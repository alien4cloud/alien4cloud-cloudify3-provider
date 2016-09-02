from cloudify.workflows import tasks as workflow_tasks
from workflow import PersistentResourceEvent
from workflow import WfEvent
from workflow import build_pre_event
from cloudify import logs
from cloudify import utils
from cloudify import constants
from cloudify.logs import _send_event
from cloudify.workflows.workflow_context import task_config

def _wait_for_host_to_start(ctx, host_node_instance):
    task = host_node_instance.execute_operation(
        'cloudify.interfaces.host.get_state')

    # handler returns True if if get_state returns False,
    # this means, that get_state will be re-executed until
    # get_state returns True
    def node_get_state_handler(tsk):
        host_started = tsk.async_result.get()
        if host_started:
            return workflow_tasks.HandlerResult.cont()
        else:
            return workflow_tasks.HandlerResult.retry(
                ignore_total_retries=True)
    if not task.is_nop():
        task.on_success = node_get_state_handler
    return task


def prepare_running_agent(host_node_instance):
    tasks = []
    install_method = utils.internal.get_install_method(
        host_node_instance.node.properties)

    plugins_to_install = filter(lambda plugin: plugin['install'],
                                host_node_instance.node.plugins_to_install)
    if (plugins_to_install and
            install_method != constants.AGENT_INSTALL_METHOD_NONE):
        node_operations = host_node_instance.node.operations
        tasks += [host_node_instance.send_event('Installing plugins')]
        if 'cloudify.interfaces.plugin_installer.install' in \
                node_operations:
            # 3.2 Compute Node
            tasks += [host_node_instance.execute_operation(
                'cloudify.interfaces.plugin_installer.install',
                kwargs={'plugins': plugins_to_install})
            ]
        else:
            tasks += [host_node_instance.execute_operation(
                'cloudify.interfaces.cloudify_agent.install_plugins',
                kwargs={'plugins': plugins_to_install})
            ]

        if install_method in constants.AGENT_INSTALL_METHODS_SCRIPTS:
            # this option is only available since 3.3 so no need to
            # handle 3.2 version here.
            tasks += [
                host_node_instance.send_event('Restarting Agent via AMQP'),
                host_node_instance.execute_operation(
                    'cloudify.interfaces.cloudify_agent.restart_amqp',
                    send_task_events=False)
            ]
        else:
            tasks += [host_node_instance.send_event(
                'Restarting Agent')]
            if 'cloudify.interfaces.worker_installer.restart' in \
                    node_operations:
                # 3.2 Compute Node
                tasks += [host_node_instance.execute_operation(
                    'cloudify.interfaces.worker_installer.restart',
                    send_task_events=False)]
            else:
                tasks += [host_node_instance.execute_operation(
                    'cloudify.interfaces.cloudify_agent.restart',
                    send_task_events=False)]

    tasks += [
        host_node_instance.execute_operation(
            'cloudify.interfaces.monitoring_agent.install'),
        host_node_instance.execute_operation(
            'cloudify.interfaces.monitoring_agent.start'),
    ]
    return tasks


def host_post_start(ctx, host_node_instance):
    install_method = utils.internal.get_install_method(
        host_node_instance.node.properties)
    tasks = [_wait_for_host_to_start(ctx, host_node_instance)]
    if install_method != constants.AGENT_INSTALL_METHOD_NONE:
        node_operations = host_node_instance.node.operations
        if 'cloudify.interfaces.worker_installer.install' in node_operations:
            # 3.2 Compute Node
            tasks += [
                host_node_instance.send_event('Installing Agent'),
                host_node_instance.execute_operation(
                    'cloudify.interfaces.worker_installer.install'),
                host_node_instance.send_event('Starting Agent'),
                host_node_instance.execute_operation(
                    'cloudify.interfaces.worker_installer.start')
            ]
        else:
            tasks += [
                host_node_instance.send_event('Creating Agent'),
                host_node_instance.execute_operation(
                    'cloudify.interfaces.cloudify_agent.create'),
                host_node_instance.send_event('Configuring Agent'),
                host_node_instance.execute_operation(
                    'cloudify.interfaces.cloudify_agent.configure'),
                host_node_instance.send_event('Starting Agent'),
                host_node_instance.execute_operation(
                    'cloudify.interfaces.cloudify_agent.start')
            ]

    tasks.extend(prepare_running_agent(host_node_instance))
    return tasks


def host_pre_stop(host_node_instance):
    install_method = utils.internal.get_install_method(
        host_node_instance.node.properties)
    tasks = []
    tasks += [
        host_node_instance.execute_operation(
            'cloudify.interfaces.monitoring_agent.stop'),
        host_node_instance.execute_operation(
            'cloudify.interfaces.monitoring_agent.uninstall'),
    ]
    if install_method != constants.AGENT_INSTALL_METHOD_NONE:
        tasks.append(host_node_instance.send_event('Stopping agent'))
        if install_method in constants.AGENT_INSTALL_METHODS_SCRIPTS:
            # this option is only available since 3.3 so no need to
            # handle 3.2 version here.
            tasks += [
                host_node_instance.execute_operation(
                    'cloudify.interfaces.cloudify_agent.stop_amqp'),
                host_node_instance.send_event('Deleting agent'),
                host_node_instance.execute_operation(
                    'cloudify.interfaces.cloudify_agent.delete')
            ]
        else:
            node_operations = host_node_instance.node.operations
            if 'cloudify.interfaces.worker_installer.stop' in node_operations:
                tasks += [
                    host_node_instance.execute_operation(
                        'cloudify.interfaces.worker_installer.stop'),
                    host_node_instance.send_event('Deleting agent'),
                    host_node_instance.execute_operation(
                        'cloudify.interfaces.worker_installer.uninstall')
                ]
            else:
                tasks += [
                    host_node_instance.execute_operation(
                        'cloudify.interfaces.cloudify_agent.stop'),
                    host_node_instance.send_event('Deleting agent'),
                    host_node_instance.execute_operation(
                        'cloudify.interfaces.cloudify_agent.delete')
                ]
    for task in tasks:
        if task.is_remote():
            _set_send_node_event_on_error_handler(
                task, host_node_instance,
                'Ignoring task {0} failure'.format(task.name))
    return tasks


def _set_send_node_event_on_error_handler(task, node_instance, error_message):
    def send_node_event_error_handler(tsk):
        node_instance.send_event(error_message)
        return workflow_tasks.HandlerResult.ignore()
    task.on_failure = send_node_event_error_handler


def _set_send_node_evt_on_failed_unlink_handlers(instance, tasks_with_targets):
    for unlink_task, target_id in tasks_with_targets:
        _set_send_node_event_on_error_handler(
            unlink_task,
            instance,
            "Error occurred while unlinking node from node {0} - "
            "ignoring...".format(target_id)
        )


def build_persistent_event_tasks(instance):
    persistent_property = instance.node.properties.get('_a4c_persistent_resources', None)
    if persistent_property != None:
        # send event to send resource id to alien
        tasks = []
        @task_config(send_task_events=False)
        def send_event_task(message):
            _send_event(instance, 'workflow_node', 'a4c_persistent_event', message, None, None, None)

        for key, value in persistent_property.iteritems():
            persistent_cloudify_attribute = key
            persistent_alien_attribute = value
            kwargs={'message':build_pre_event(PersistentResourceEvent(persistent_cloudify_attribute, persistent_alien_attribute))}
            tasks.append(instance.ctx.local_task(local_task=send_event_task, node=instance, info=kwargs.get('message', ''), kwargs=kwargs))
        return tasks
    else:
        return None


def build_wf_event_task(instance, step_id, stage):
    event_msg = build_pre_event(WfEvent(stage, step_id))

    @task_config(send_task_events=False)
    def send_wf_event_task():
        _send_event(instance, 'workflow_node', 'a4c_workflow_event', event_msg, None, None, None)

    return instance.ctx.local_task(local_task=send_wf_event_task, node=instance, info=event_msg)
