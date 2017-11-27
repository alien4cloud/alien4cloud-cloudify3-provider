from cloudify import ctx
from cloudify import utils
from cloudify.exceptions import NonRecoverableError
from StringIO import StringIO

import base64
import os
import platform
import re
import subprocess
import sys
import time
import threading
import platform
import json

def convert_env_value_to_string(envDict):
    for key, value in envDict.items():
        envDict[str(key)] = str(envDict.pop(key))

def get_attribute_user(ctx):
    if get_attribute_from_top_host(ctx, 'user'):
        return get_attribute_from_top_host(ctx, 'user')
    if get_attribute(ctx, 'cloudify_agent'):
        return get_attribute(ctx, 'cloudify_agent').get('user', None)
    if get_attribute(ctx, 'agent_config'):
        return get_attribute(ctx, 'agent_config').get('user', None)
    return None

def get_attribute_key(ctx):
    if get_attribute_from_top_host(ctx, 'key'):
        return get_attribute_from_top_host(ctx, 'key')
    if get_attribute(ctx, 'cloudify_agent'):
        return get_attribute(ctx, 'cloudify_agent').get('key', None)
    if get_attribute(ctx, 'agent_config'):
        return get_attribute(ctx, 'agent_config').get('key', None)
    return None

def get_host(entity):
    if entity.instance.relationships:
        for relationship in entity.instance.relationships:
            if 'cloudify.relationships.contained_in' in relationship.type_hierarchy:
                return relationship.target
    return None


def has_attribute_mapping(entity, attribute_name):
    # ctx.logger.debug('Check if it exists mapping for attribute {0} in {1}'.format(attribute_name,json.dumps(entity.node.properties)))
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
    # ctx.logger.debug('Mapping configuration found for attribute {0} is {1}'.format(attribute_name, json.dumps(mapping_configuration)))
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
        # ctx.logger.debug('Mapping exists for attribute {0} with value {1}'.format(attribute_name, json.dumps(mapped_value)))
        return mapped_value
    # No mapping exist, try to get directly the attribute from the entity
    attribute_value = entity.instance.runtime_properties.get(attribute_name, None)
    if attribute_value is not None:
        # ctx.logger.debug('Found the attribute {0} with value {1} on the node {2}'.format(attribute_name, json.dumps(attribute_value), entity.node.id))
        return attribute_value
    # Attribute retrieval fails, fall back to property
    property_value = entity.node.properties.get(attribute_name, None)
    if property_value is not None:
        return property_value
    # Property retrieval fails, fall back to host instance
    host = get_host(entity)
    if host is not None:
        # ctx.logger.debug('Attribute not found {0} go up to the parent node {1}'.format(attribute_name, host.node.id))
        return get_attribute(host, attribute_name)
    # Nothing is found
    return ""

def get_target_capa_or_node_attribute(entity, capability_attribute_name, attribute_name):
    attribute_value = entity.instance.runtime_properties.get(capability_attribute_name, None)
    if attribute_value is not None:
        # ctx.logger.debug('Found the capability attribute {0} with value {1} on the node {2}'.format(attribute_name, json.dumps(attribute_value), entity.node.id))
        return attribute_value
    return get_attribute(entity, attribute_name)

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
            # ctx.logger.debug('Found the property/attribute {0} with value {1} on the node {2} instance {3}'.format(attribute_name, json.dumps(prop_value), entity.node.id,
            #   node_instance.id))
            result_map[node_instance.id + '_'] = prop_value
    return result_map

# Same as previous method but will first try to find the attribute on the capability.
def _all_instances_get_target_capa_or_node_attribute(entity, capability_attribute_name, attribute_name):
    result_map = {}
    node = client.nodes.get(ctx.deployment.id, entity.node.id)
    all_node_instances = client.node_instances.list(ctx.deployment.id, entity.node.id)
    for node_instance in all_node_instances:
        attribute_value = node_instance.runtime_properties.get(capability_attribute_name, None)
        if attribute_value is not None:
            prop_value = attribute_value
        else:
            prop_value = __recursively_get_instance_data(node, node_instance, attribute_name)
        if prop_value is not None:
            # ctx.logger.debug('Found the property/attribute {0} with value {1} on the node {2} instance {3}'.format(attribute_name, json.dumps(prop_value), entity.node.id,
            #    node_instance.id))
            result_map[node_instance.id + '_'] = prop_value
    return result_map

def get_property(entity, property_name):
    # Try to get the property value on the node
    property_value = entity.node.properties.get(property_name, None)
    if property_value is not None:
        # ctx.logger.debug('Found the property {0} with value {1} on the node {2}'.format(property_name, json.dumps(property_value), entity.node.id))
        return property_value
    # No property found on the node, fall back to the host
    host = get_host(entity)
    if host is not None:
        # ctx.logger.debug('Property not found {0} go up to the parent node {1}'.format(property_name, host.node.id))
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
    # ctx.logger.debug('Check if it exists mapping for attribute {0} in {1}'.format(attribute_name, json.dumps(node.properties)))
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
    # ctx.logger.debug('Mapping configuration found for attribute {0} is {1}'.format(attribute_name, json.dumps(mapping_configuration)))
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

def get_public_or_private_ip(entity):
    public_ip = get_attribute(entity, 'public_ip_address')
    if not public_ip:
        return get_attribute(entity, 'ip_address')
    return public_ip

def get_attribute_from_top_host(entity, attribute_name):
    host = get_host(entity)
    while host is not None:
        entity = host
        host = get_host(entity)
    return get_attribute(entity, attribute_name)

from cloudify import utils
from cloudify_rest_client import CloudifyClient
from cloudify.state import ctx_parameters as inputs

import os

client = CloudifyClient(host=utils.get_manager_ip(),
                        port=utils.get_manager_rest_service_port(),
                        protocol='https',
                        cert=utils.get_local_rest_certificate(),
                        token= utils.get_rest_token(),
                        tenant= utils.get_tenant_name())

def convert_env_value_to_string(envDict):
    for key, value in envDict.items():
        envDict[str(key)] = str(envDict.pop(key))


def parse_output(output):
    # by convention, the last output is the result of the operation
    last_output = None
    outputs = {}
    pattern = re.compile('EXPECTED_OUTPUT_(\w+)=(.*)')
    for line in output.splitlines():
        match = pattern.match(line)
        if match is None:
            last_output = line
        else:
            output_name = match.group(1)
            output_value = match.group(2)
            outputs[output_name] = output_value
    return {'last_output': last_output, 'outputs': outputs}


def execute(script_path, process, outputNames, command_prefix=None, cwd=None, raiseException=True):
    os.chmod(script_path, 0755)
    on_posix = 'posix' in sys.builtin_module_names

    env = os.environ.copy()
    process_env = process.get('env', {})
    env.update(process_env)

    if outputNames is not None:
        env['EXPECTED_OUTPUTS'] = outputNames
        if platform.system() == 'Windows':
            wrapper_path = ctx.download_resource("scriptWrapper.bat")
        else:
            wrapper_path = ctx.download_resource("scriptWrapper.sh")
        os.chmod(wrapper_path, 0755)
        command = '{0} {1}'.format(wrapper_path, script_path)
    else:
        command = script_path

    if command_prefix is not None:
        command = "{0} {1}".format(command_prefix, command)

    ctx.logger.info('Executing: {0} in env {1}'.format(command, env))

    process = subprocess.Popen(command,
                               shell=True,
                               stdout=subprocess.PIPE,
                               stderr=subprocess.PIPE,
                               env=env,
                               cwd=cwd,
                               bufsize=1,
                               close_fds=on_posix)

    return_code = None

    stdout_consumer = OutputConsumer(process.stdout)
    stderr_consumer = OutputConsumer(process.stderr)

    while True:
        return_code = process.poll()
        if return_code is not None:
            break
        time.sleep(0.1)

    stdout_consumer.join()
    stderr_consumer.join()

    parsed_output = parse_output(stdout_consumer.buffer.getvalue())
    if outputNames is not None:
        outputNameList = outputNames.split(';')
        for outputName in outputNameList:
            ctx.logger.info('Ouput name: {0} value : {1}'.format(outputName, parsed_output['outputs'].get(outputName, None)))

    if return_code != 0:
        error_message = "Script {0} encountered error with return code {1} and standard output {2}, error output {3}".format(command, return_code,
                                                                                                                             stdout_consumer.buffer.getvalue(),
                                                                                                                             stderr_consumer.buffer.getvalue())
        error_message = str(unicode(error_message, errors='ignore'))
        ctx.logger.error(error_message)

        if raiseException:
            ctx.logger.debug("Script {0} will raise an exception".format(command))
            raise NonRecoverableError(error_message)
    else:
        ok_message = "Script {0} executed normally with standard output {1} and error output {2}".format(command, stdout_consumer.buffer.getvalue(),
                                                                                                         stderr_consumer.buffer.getvalue())
        ok_message = str(unicode(ok_message, errors='ignore'))
        ctx.logger.info(ok_message)

    return parsed_output

def executePy(script_path, tosca_env_map):
    tosca_params={'tosca': {'inputs': tosca_env_map, 'outputs': {}}}
    execfile(script_path, globals().copy(), tosca_params)
    return tosca_params['tosca'];

class OutputConsumer(object):
    def __init__(self, out):
        self.out = out
        self.buffer = StringIO()
        self.consumer = threading.Thread(target=self.consume_output)
        self.consumer.daemon = True
        self.consumer.start()

    def consume_output(self):
        for line in iter(self.out.readline, b''):
            self.buffer.write(line)
        self.out.close()

    def join(self):
        self.consumer.join()

# Check the inputs
mandatories = ['iaas', 'os_mapping', 'volume_instance_id', 'device_key']
for param in mandatories:
    if param not in inputs:
      raise SystemExit("The parameter '{0}' is missing".format(param))


# Method which actually call the script corresponding to the IaaS and the OS that do the mapping
def do_mapping(current_os, iaas, device_name):
    map_script_path = None
    ctx.logger.info("inside current os: '{0}'".format(current_os))
    command_prefix = None
    if 'windows' == current_os:
        ctx.logger.info('[MAPPING] windows')
        map_script_path = ctx.download_resource("device-mapping-scripts/{0}/mapDevice.ps1".format(iaas))
        command_prefix="C:\\Windows\\Sysnative\\WindowsPowerShell\\v1.0\\powershell.exe -executionpolicy bypass -File"
    else:
        ctx.logger.info("[MAPPING] linux")
        map_script_path = ctx.download_resource("device-mapping-scripts/{0}/mapDevice.sh".format(iaas))
    env_map = {'DEVICE_NAME' : device_name}
    new_script_process = {'env': env_map}
    convert_env_value_to_string(new_script_process['env'])
    outputs = execute(map_script_path, new_script_process, outputNames=None, command_prefix=command_prefix, raiseException=True)
    return outputs['last_output']


# Method will do the device mapping if the OS needs a mapping for the device
def map_device_name(iaas, os_mapping, device_name):
    new_device_name = None
    current_os = platform.system().lower()
    ctx.logger.info("current os: '{0}'".format(current_os))
    if current_os in os_mapping:
        new_device_name = do_mapping(current_os, iaas, device_name)
    return new_device_name

# Retrieve requiert parameters
volume_instance_id = inputs['volume_instance_id']
iaas = inputs['iaas'] # correspond to the folder where to find the mapping scripts
os_mapping = inputs['os_mapping'] # values: windows or/and linux. it means that the specified os will need a mapping
device_key = inputs['device_key'] # the attribute name of the volume node which contains the device value

# Retrieve the current device_name from the attributes of the volume node
volume = client.node_instances.get(volume_instance_id)
ctx.logger.debug("[MAPPING] volume: {0}".format(json.dumps(volume)))

saved_device_key = "cfy_{0}_saved".format(device_key)
if saved_device_key in volume.runtime_properties:
    device_name = volume.runtime_properties[saved_device_key]
elif device_key in volume.runtime_properties:
    device_name = volume.runtime_properties[device_key]
else:
    ctx.logger.warning("No '{0}' keyname in runtime properties, retrieve the value from the properties of the node '{1}'".format(device_key, volume.node_id))
    volume_node = client.nodes.get(volume.deployment_id, volume.node_id)
    device_name = volume_node.properties[device_key]

# Do the mapping
mapped_device = map_device_name(iaas, os_mapping, device_name)

# Update the device_name attributes if needed
if mapped_device is not None:
    if saved_device_key not in volume.runtime_properties:
        volume.runtime_properties[saved_device_key] = device_name
    volume.runtime_properties[device_key] = mapped_device
    client.node_instances.update(volume_instance_id, None, volume.runtime_properties, volume.version)
    ctx.logger.info("[MAPPING] volume: {0} updated".format(json.dumps(volume)))
else:
    ctx.logger.info("[MAPPING] No mapping for {0}".format(volume_instance_id))
