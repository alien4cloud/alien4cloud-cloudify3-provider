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

