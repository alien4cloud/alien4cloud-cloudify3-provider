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
  outputs = execute(map_script_path, new_script_process, outputNames=None, command_prefix=command_prefix)
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
ctx.logger.debug("[MAPPING] volume: {0}".format(volume))

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
  ctx.logger.info("[MAPPING] volume: {0} updated".format(volume))
else:
  ctx.logger.info("[MAPPING] No mapping for {0}".format(volume_instance_id))
