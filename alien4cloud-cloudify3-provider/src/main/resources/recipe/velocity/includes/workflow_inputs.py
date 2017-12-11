def process_custom_workflow_inputs(inputs, env_map):
    if inputs.get('process', None) is not None and inputs['process'].get('env', None) is not None:
        ctx.logger.info('Operation is executed with environment variable {0}'.format(inputs['process']['env']))
        for input_env_key, input_env_raw_value in inputs['process']['env'].items():
            if input_env_raw_value.startswith('{') and input_env_raw_value.endswith('}'):
                try:
                    input_env_value = json.loads(input_env_raw_value)
                except:
                    env_map[input_env_key] = input_env_raw_value
                    continue
                function_type = input_env_value.get('function')
                function_parameters = input_env_value.get('parameters')
                if function_type is None or function_parameters is None or len(function_parameters) == 0:
                    env_map[input_env_key] = input_env_raw_value
                    continue
                if function_type == 'get_secret':
                    env_map[input_env_key] = get_secret(function_parameters[0])
                elif function_type == 'get_property':
                    env_map[input_env_key] = get_property(ctx, function_parameters[len(function_parameters) - 1])
                elif function_type == 'get_attribute':
                    env_map[input_env_key] = get_attribute(ctx, function_parameters[len(function_parameters) - 1])
                elif function_type == 'get_operation_output':
                    if function_parameters < 4:
                        ctx.logger.info('Function get_operation_output needs 4 parameters [entity, interface, operation, output_name] {0}'.format(input_env_raw_value))
                        env_map[input_env_key] = input_env_raw_value
                        continue
                    interface_name = function_parameters[1]
                    if interface_name.lower() == 'standard':
                        interface_name = 'tosca.interfaces.node.lifecycle.Standard'
                    elif interface_name.lower() == 'configure':
                        interface_name = 'tosca.interfaces.relationship.Configure'
                    operation_name = function_parameters[2]
                    output_name = function_parameters[3]
                    env_map[input_env_key] = get_attribute(ctx, '_a4c_OO:' + interface_name + ':' + operation_name + ':' + output_name)
                else:
                    # It must be a complex input
                    env_map[input_env_key] = input_env_raw_value
            else:
                env_map[input_env_key] = input_env_raw_value
