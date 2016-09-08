
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


def execute(script_path, process, outputNames, command_prefix=None, cwd=None):
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
        raise NonRecoverableError(error_message)
    else:
        ok_message = "Script {0} executed normally with standard output {1} and error output {2}".format(command, stdout_consumer.buffer.getvalue(),
                                                                                                         stderr_consumer.buffer.getvalue())
        ok_message = str(unicode(ok_message, errors='ignore'))
        ctx.logger.info(ok_message)

    return parsed_output


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

def download(child_rel_path, child_abs_path, download_dir):
    artifact_downloaded_path = ctx.download_resource(child_abs_path)
    new_file = os.path.join(download_dir, child_rel_path)
    new_file_dir = os.path.dirname(new_file)
    if not os.path.exists(new_file_dir):
        os.makedirs(new_file_dir)
    os.rename(artifact_downloaded_path, new_file)
    ctx.logger.info('Downloaded artifact from path ' + child_abs_path + ', it\'s available now at ' + new_file)
    return new_file


def download_artifacts(artifacts, download_dir):
    downloaded_artifacts = {}
    os.makedirs(download_dir)
    for artifact_name, artifact_ref in artifacts.items():
        ctx.logger.info('Download artifact ' + artifact_name)
        if isinstance(artifact_ref, basestring):
            downloaded_artifacts[artifact_name] = download(os.path.basename(artifact_ref), artifact_ref, download_dir)
        else:
            child_download_dir = os.path.join(download_dir, artifact_name)
            for child_path in artifact_ref:
                download(child_path['relative_path'], child_path['absolute_path'], child_download_dir)
            downloaded_artifacts[artifact_name] = child_download_dir
    return downloaded_artifacts

env_map = {}
env_map['NODE'] = ctx.node.id
env_map['INSTANCE'] = ctx.instance.id
env_map['INSTANCES'] = get_instance_list(ctx.node.id)
env_map['HOST'] = get_host_node_name(ctx.instance)
new_script_process = {'env': env_map}

node_artifacts = {
    "scripts": [
        {
            "relative_path": "configureSupp_hss.sh",
            "absolute_path": "artifacts/support_hss_types/scripts/configureSupp_hss.sh"
        }
        ,
        {
            "relative_path": "deleteSupp_hss.sh",
            "absolute_path": "artifacts/support_hss_types/scripts/deleteSupp_hss.sh"
        }
        ,
        {
            "relative_path": "relationships/supp_to_volume.sh",
            "absolute_path": "artifacts/support_hss_types/scripts/relationships/supp_to_volume.sh"
        }
        ,
        {
            "relative_path": "stopSupp_hss.sh",
            "absolute_path": "artifacts/support_hss_types/scripts/stopSupp_hss.sh"
        }
        ,
        {
            "relative_path": "createSupp_hss.sh",
            "absolute_path": "artifacts/support_hss_types/scripts/createSupp_hss.sh"
        }
        ,
        {
            "relative_path": "startSupp_hss.sh",
            "absolute_path": "artifacts/support_hss_types/scripts/startSupp_hss.sh"
        }
    ]
}

relationship_artifacts = {
}

artifacts = node_artifacts.copy()
artifacts.update(relationship_artifacts)

download_dir = os.path.join(os.path.dirname(os.path.realpath(__file__)), 'downloads')
new_script_process['env'].update(download_artifacts(artifacts, download_dir))

ctx.logger.info('Operation is executed with inputs {0}'.format(inputs))
if inputs.get('process', None) is not None and inputs['process'].get('env', None) is not None:
    ctx.logger.info('Operation is executed with environment variable {0}'.format(inputs['process']['env']))
    new_script_process['env'].update(inputs['process']['env'])

operationOutputNames = None
convert_env_value_to_string(new_script_process['env'])
parsed_output = execute(ctx.download_resource('artifacts/support_hss_types/scripts/stopSupp_hss.sh'), new_script_process, operationOutputNames)
for k,v in parsed_output['outputs'].items():
    ctx.logger.info('Output name: {0} value: {1}'.format(k, v))
    ctx.instance.runtime_properties['_a4c_OO:tosca.interfaces.node.lifecycle.Standard:stop:{0}'.format(k)] = v


ctx.instance.update()
