from cloudify import ctx
from cloudify.exceptions import NonRecoverableError
from cloudify.state import ctx_parameters as inputs
import subprocess
import os
import re
import sys
import time
import threading
import platform
from StringIO import StringIO
from cloudify_rest_client import CloudifyClient
from cloudify import utils

if 'MANAGER_REST_PROTOCOL' in os.environ and os.environ['MANAGER_REST_PROTOCOL'] == "https":
  client = CloudifyClient(host=utils.get_manager_ip(), port=utils.get_manager_rest_service_port(), protocol='https', trust_all=True)
else:
  client = CloudifyClient(host=utils.get_manager_ip(), port=utils.get_manager_rest_service_port())

def convert_env_value_to_string(envDict):
    for key, value in envDict.items():
        envDict[str(key)] = str(envDict.pop(key))

def get_attribute_user(ctx):
    if get_attribute(ctx, 'user'):
        return get_attribute(ctx, 'user')
    else:
        return get_attribute(ctx, 'cloudify_agent')['user']

def get_attribute_key(ctx):
    if get_attribute(ctx, 'key'):
        return get_attribute(ctx, 'key')
    else:
        return get_attribute(ctx, 'cloudify_agent')['key']

def get_host(entity):
    if entity.instance.relationships:
        for relationship in entity.instance.relationships:
            if 'cloudify.relationships.contained_in' in relationship.type_hierarchy:
                return relationship.target
    return None


def has_attribute_mapping(entity, attribute_name):
    ctx.logger.info('Check if it exists mapping for attribute {0} in {1}'.format(attribute_name, entity.node.properties))
    mapping_configuration = entity.node.properties.get('_a4c_att_' + attribute_name, None)
    if mapping_configuration is not None:
        if mapping_configuration['parameters'][0] == 'SELF' and mapping_configuration['parameters'][1] == attribute_name:
            return False
        else:
            return True
    return False


def process_attribute_mapping(entity, attribute_name, data_retriever_function):
    # This is where attribute mapping is defined in the cloudify type
    mapping_configuration = entity.node.properties['_a4c_att_' + attribute_name]
    ctx.logger.info('Mapping configuration found for attribute {0} is {1}'.format(attribute_name, mapping_configuration))
    # If the mapping configuration exist and if it concerns SELF then just get attribute of the mapped attribute name
    # Else if it concerns TARGET then follow the relationship and retrieved the mapped attribute name from the TARGET
    if mapping_configuration['parameters'][0] == 'SELF':
        return data_retriever_function(entity, mapping_configuration['parameters'][1])
    elif mapping_configuration['parameters'][0] == 'TARGET' and entity.instance.relationships:
        for relationship in entity.instance.relationships:
            if mapping_configuration['parameters'][1] in relationship.type_hierarchy:
                return data_retriever_function(relationship.target, mapping_configuration['parameters'][2])
    return ""


def get_nested_attribute(entity, attribute_names):
    deep_properties = get_attribute(entity, attribute_names[0])
    attribute_names_iter = iter(attribute_names)
    next(attribute_names_iter)
    for attribute_name in attribute_names_iter:
        if deep_properties is None:
            return ""
        else:
            deep_properties = deep_properties.get(attribute_name, None)
    return deep_properties


def _all_instances_get_nested_attribute(entity, attribute_names):
    return None


def get_attribute(entity, attribute_name):
    if has_attribute_mapping(entity, attribute_name):
        # First check if any mapping exist for attribute
        mapped_value = process_attribute_mapping(entity, attribute_name, get_attribute)
        ctx.logger.info('Mapping exists for attribute {0} with value {1}'.format(attribute_name, mapped_value))
        return mapped_value
    # No mapping exist, try to get directly the attribute from the entity
    attribute_value = entity.instance.runtime_properties.get(attribute_name, None)
    if attribute_value is not None:
        ctx.logger.info('Found the attribute {0} with value {1} on the node {2}'.format(attribute_name, attribute_value, entity.node.id))
        return attribute_value
    # Attribute retrieval fails, fall back to property
    property_value = entity.node.properties.get(attribute_name, None)
    if property_value is not None:
        return property_value
    # Property retrieval fails, fall back to host instance
    host = get_host(entity)
    if host is not None:
        ctx.logger.info('Attribute not found {0} go up to the parent node {1}'.format(attribute_name, host.node.id))
        return get_attribute(host, attribute_name)
    # Nothing is found
    return ""


def _all_instances_get_attribute(entity, attribute_name):
    result_map = {}
    # get all instances data using cfy rest client
    # we have to get the node using the rest client with node_instance.node_id
    # then we will have the relationships
    node = client.nodes.get(ctx.deployment.id, entity.node.id)
    all_node_instances = client.node_instances.list(ctx.deployment.id, entity.node.id)
    for node_instance in all_node_instances:
        prop_value = __recursively_get_instance_data(node, node_instance, attribute_name)
        if prop_value is not None:
            ctx.logger.info('Found the property/attribute {0} with value {1} on the node {2} instance {3}'.format(attribute_name, prop_value, entity.node.id,
                                                                                                                  node_instance.id))
            result_map[node_instance.id + '_'] = prop_value
    return result_map


def get_property(entity, property_name):
    # Try to get the property value on the node
    property_value = entity.node.properties.get(property_name, None)
    if property_value is not None:
        ctx.logger.info('Found the property {0} with value {1} on the node {2}'.format(property_name, property_value, entity.node.id))
        return property_value
    # No property found on the node, fall back to the host
    host = get_host(entity)
    if host is not None:
        ctx.logger.info('Property not found {0} go up to the parent node {1}'.format(property_name, host.node.id))
        return get_property(host, property_name)
    return ""


def get_instance_list(node_id):
    result = ''
    all_node_instances = client.node_instances.list(ctx.deployment.id, node_id)
    for node_instance in all_node_instances:
        if len(result) > 0:
            result += ','
        result += node_instance.id
    return result

def get_host_node_name(instance):
    for relationship in instance.relationships:
        if 'cloudify.relationships.contained_in' in relationship.type_hierarchy:
            return relationship.target.node.id
    return None

def __get_relationship(node, target_name, relationship_type):
    for relationship in node.relationships:
        if relationship.get('target_id') == target_name and relationship_type in relationship.get('type_hierarchy'):
            return relationship
    return None


def __has_attribute_mapping(node, attribute_name):
    ctx.logger.info('Check if it exists mapping for attribute {0} in {1}'.format(attribute_name, node.properties))
    mapping_configuration = node.properties.get('_a4c_att_' + attribute_name, None)
    if mapping_configuration is not None:
        if mapping_configuration['parameters'][0] == 'SELF' and mapping_configuration['parameters'][1] == attribute_name:
            return False
        else:
            return True
    return False


def __process_attribute_mapping(node, node_instance, attribute_name, data_retriever_function):
    # This is where attribute mapping is defined in the cloudify type
    mapping_configuration = node.properties['_a4c_att_' + attribute_name]
    ctx.logger.info('Mapping configuration found for attribute {0} is {1}'.format(attribute_name, mapping_configuration))
    # If the mapping configuration exist and if it concerns SELF then just get attribute of the mapped attribute name
    # Else if it concerns TARGET then follow the relationship and retrieved the mapped attribute name from the TARGET
    if mapping_configuration['parameters'][0] == 'SELF':
        return data_retriever_function(node, node_instance, mapping_configuration['parameters'][1])
    elif mapping_configuration['parameters'][0] == 'TARGET' and node_instance.relationships:
        for rel in node_instance.relationships:
            relationship = __get_relationship(node, rel.get('target_name'), rel.get('type'))
            if mapping_configuration['parameters'][1] in relationship.get('type_hierarchy'):
                target_instance = client.node_instances.get(rel.get('target_id'))
                target_node = client.nodes.get(ctx.deployment.id, target_instance.node_id)
                return data_retriever_function(target_node, target_instance, mapping_configuration['parameters'][2])
    return None


def __recursively_get_instance_data(node, node_instance, attribute_name):
    if __has_attribute_mapping(node, attribute_name):
        return __process_attribute_mapping(node, node_instance, attribute_name, __recursively_get_instance_data)
    attribute_value = node_instance.runtime_properties.get(attribute_name, None)
    if attribute_value is not None:
        return attribute_value
    elif node_instance.relationships:
        for rel in node_instance.relationships:
            # on rel we have target_name, target_id (instanceId), type
            relationship = __get_relationship(node, rel.get('target_name'), rel.get('type'))
            if 'cloudify.relationships.contained_in' in relationship.get('type_hierarchy'):
                parent_instance = client.node_instances.get(rel.get('target_id'))
                parent_node = client.nodes.get(ctx.deployment.id, parent_instance.node_id)
                return __recursively_get_instance_data(parent_node, parent_instance, attribute_name)
        return None
    else:
        return None
