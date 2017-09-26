from graph import Task


def _wait_for_host_to_start(ctx, host_node_instance):
    # !!!!!!!!!!!!!!!!!!!
    return Task("wait_{0}".format(host_node_instance.id))


def host_post_start(ctx, host_node_instance):
    # !!!!!!!!!!!!!!!!!!!
    plugins_to_install = "all"

    tasks = [_wait_for_host_to_start(ctx, host_node_instance)]
    if host_node_instance.node.properties.get('install_agent', False) is True:
        tasks += [
            host_node_instance.send_event('Installing worker'),
            host_node_instance.execute_operation(
                'cloudify.interfaces.worker_installer.install'),
            host_node_instance.execute_operation(
                'cloudify.interfaces.worker_installer.start'),
        ]
        if plugins_to_install:
            tasks += [
                host_node_instance.send_event('Installing host plugins'),
                host_node_instance.execute_operation(
                    'cloudify.interfaces.plugin_installer.install',
                    kwargs={
                        'plugins': plugins_to_install}),
                host_node_instance.execute_operation(
                    'cloudify.interfaces.worker_installer.restart',
                    send_task_events=False)
            ]
    tasks += [
        host_node_instance.execute_operation(
            'cloudify.interfaces.monitoring_agent.install'),
        host_node_instance.execute_operation(
            'cloudify.interfaces.monitoring_agent.start'),
    ]
    return tasks


def _set_send_node_event_on_error_handler(task, node_instance, error_message):
    # !!!!!!!!!!!!!!!!!!!!!!!!!
    print("")


def _set_send_node_evt_on_failed_unlink_handlers(instance, tasks_with_targets):
    print("")


def host_pre_stop(host_node_instance):
    tasks = []
    tasks += [
        host_node_instance.execute_operation(
            'cloudify.interfaces.monitoring_agent.stop'),
        host_node_instance.execute_operation(
            'cloudify.interfaces.monitoring_agent.uninstall'),
    ]
    if host_node_instance.node.properties.get('install_agent', False) is True:
        tasks += [
            host_node_instance.send_event('Uninstalling worker'),
            host_node_instance.execute_operation(
                'cloudify.interfaces.worker_installer.stop'),
            host_node_instance.execute_operation(
                'cloudify.interfaces.worker_installer.uninstall')
        ]
    return tasks


def build_persistent_event_task(instance):
    return None


def build_wf_event_task(instance, step_id, stage):
    event_msg = build_pre_event(WfEvent(stage, step_id))
    return instance.send_event(event_msg)
