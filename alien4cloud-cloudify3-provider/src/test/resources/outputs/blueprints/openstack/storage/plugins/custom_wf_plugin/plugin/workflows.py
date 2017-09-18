from cloudify.decorators import workflow
from cloudify.workflows import ctx
from cloudify.workflows import tasks as workflow_tasks
from utils import set_state_task
from utils import operation_task
from utils import link_tasks
from utils import CustomContext
from utils import generate_native_node_workflows
from utils import _get_all_nodes
from utils import _get_all_nodes_instances
from utils import _get_all_modified_node_instances
from utils import is_host_node
from workflow import WfStartEvent
from workflow import build_pre_event


# subworkflow 'install' for host 'Compute'
def install_host_compute(ctx, graph, custom_context):
    custom_context.add_customized_wf_node('LinuxFileSystem_2')
    custom_context.add_customized_wf_node('LinuxFileSystem_4')
    custom_context.add_customized_wf_node('LinuxFileSystem_3')
    custom_context.add_customized_wf_node('LinuxFileSystem_1')
    custom_context.add_customized_wf_node('LinuxFileSystem_3')
    custom_context.add_customized_wf_node('LinuxFileSystem_3')
    custom_context.add_customized_wf_node('LinuxFileSystem_4')
    custom_context.add_customized_wf_node('LinuxFileSystem_3')
    custom_context.add_customized_wf_node('LinuxFileSystem_4')
    custom_context.add_customized_wf_node('LinuxFileSystem_2')
    custom_context.add_customized_wf_node('LinuxFileSystem_3')
    custom_context.add_customized_wf_node('LinuxFileSystem_2')
    custom_context.add_customized_wf_node('LinuxFileSystem_3')
    custom_context.add_customized_wf_node('LinuxFileSystem_2')
    custom_context.add_customized_wf_node('LinuxFileSystem_2')
    custom_context.add_customized_wf_node('LinuxFileSystem_4')
    custom_context.add_customized_wf_node('LinuxFileSystem_1')
    custom_context.add_customized_wf_node('LinuxFileSystem_1')
    custom_context.add_customized_wf_node('LinuxFileSystem_1')
    custom_context.add_customized_wf_node('LinuxFileSystem_2')
    custom_context.add_customized_wf_node('LinuxFileSystem_3')
    custom_context.add_customized_wf_node('LinuxFileSystem_2')
    custom_context.add_customized_wf_node('LinuxFileSystem_1')
    custom_context.add_customized_wf_node('LinuxFileSystem_4')
    custom_context.add_customized_wf_node('LinuxFileSystem_1')
    custom_context.add_customized_wf_node('LinuxFileSystem_4')
    custom_context.add_customized_wf_node('LinuxFileSystem_4')
    custom_context.add_customized_wf_node('LinuxFileSystem_1')
    set_state_task(ctx, graph, 'LinuxFileSystem_2', 'configuring', 'LinuxFileSystem_2_configuring', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_4', 'configuring', 'LinuxFileSystem_4_configuring', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_3', 'starting', 'LinuxFileSystem_3_starting', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_1', 'configuring', 'LinuxFileSystem_1_configuring', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_3', 'creating', 'LinuxFileSystem_3_creating', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_3', 'configuring', 'LinuxFileSystem_3_configuring', custom_context)
    custom_context.register_native_delegate_wf_step('Compute', 'Compute_install')
    set_state_task(ctx, graph, 'LinuxFileSystem_4', 'started', 'LinuxFileSystem_4_started', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_3', 'started', 'LinuxFileSystem_3_started', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_4', 'created', 'LinuxFileSystem_4_created', custom_context)
    operation_task(ctx, graph, 'LinuxFileSystem_1', 'cloudify.interfaces.lifecycle.configure', 'configure_LinuxFileSystem_1', custom_context)
    operation_task(ctx, graph, 'LinuxFileSystem_3', 'cloudify.interfaces.lifecycle.start', 'start_LinuxFileSystem_3', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_2', 'created', 'LinuxFileSystem_2_created', custom_context)
    operation_task(ctx, graph, 'LinuxFileSystem_2', 'cloudify.interfaces.lifecycle.configure', 'configure_LinuxFileSystem_2', custom_context)
    operation_task(ctx, graph, 'LinuxFileSystem_4', 'cloudify.interfaces.lifecycle.start', 'start_LinuxFileSystem_4', custom_context)
    operation_task(ctx, graph, 'LinuxFileSystem_1', 'cloudify.interfaces.lifecycle.start', 'start_LinuxFileSystem_1', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_3', 'created', 'LinuxFileSystem_3_created', custom_context)
    operation_task(ctx, graph, 'LinuxFileSystem_3', 'cloudify.interfaces.lifecycle.configure', 'configure_LinuxFileSystem_3', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_2', 'configured', 'LinuxFileSystem_2_configured', custom_context)
    operation_task(ctx, graph, 'LinuxFileSystem_2', 'cloudify.interfaces.lifecycle.start', 'start_LinuxFileSystem_2', custom_context)
    operation_task(ctx, graph, 'LinuxFileSystem_4', 'cloudify.interfaces.lifecycle.configure', 'configure_LinuxFileSystem_4', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_3', 'initial', 'LinuxFileSystem_3_initial', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_2', 'initial', 'LinuxFileSystem_2_initial', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_2', 'creating', 'LinuxFileSystem_2_creating', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_4', 'initial', 'LinuxFileSystem_4_initial', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_1', 'initial', 'LinuxFileSystem_1_initial', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_1', 'created', 'LinuxFileSystem_1_created', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_1', 'started', 'LinuxFileSystem_1_started', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_2', 'started', 'LinuxFileSystem_2_started', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_3', 'configured', 'LinuxFileSystem_3_configured', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_2', 'starting', 'LinuxFileSystem_2_starting', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_1', 'configured', 'LinuxFileSystem_1_configured', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_4', 'creating', 'LinuxFileSystem_4_creating', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_1', 'starting', 'LinuxFileSystem_1_starting', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_4', 'starting', 'LinuxFileSystem_4_starting', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_4', 'configured', 'LinuxFileSystem_4_configured', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_1', 'creating', 'LinuxFileSystem_1_creating', custom_context)
    custom_context.register_native_delegate_wf_step('CBS4', 'CBS4_install')
    custom_context.register_native_delegate_wf_step('CBS3', 'CBS3_install')
    custom_context.register_native_delegate_wf_step('CBS1', 'CBS1_install')
    custom_context.register_native_delegate_wf_step('CBS2', 'CBS2_install')
    generate_native_node_workflows(ctx, graph, custom_context, 'install')
    link_tasks(graph, 'configure_LinuxFileSystem_2', 'LinuxFileSystem_2_configuring', custom_context)
    link_tasks(graph, 'configure_LinuxFileSystem_4', 'LinuxFileSystem_4_configuring', custom_context)
    link_tasks(graph, 'start_LinuxFileSystem_3', 'LinuxFileSystem_3_starting', custom_context)
    link_tasks(graph, 'configure_LinuxFileSystem_1', 'LinuxFileSystem_1_configuring', custom_context)
    link_tasks(graph, 'LinuxFileSystem_3_created', 'LinuxFileSystem_3_creating', custom_context)
    link_tasks(graph, 'configure_LinuxFileSystem_3', 'LinuxFileSystem_3_configuring', custom_context)
    link_tasks(graph, 'LinuxFileSystem_3_initial', 'Compute_install', custom_context)
    link_tasks(graph, 'LinuxFileSystem_2_initial', 'Compute_install', custom_context)
    link_tasks(graph, 'LinuxFileSystem_4_initial', 'Compute_install', custom_context)
    link_tasks(graph, 'LinuxFileSystem_1_initial', 'Compute_install', custom_context)
    link_tasks(graph, 'LinuxFileSystem_4_configuring', 'LinuxFileSystem_4_created', custom_context)
    link_tasks(graph, 'LinuxFileSystem_1_configured', 'configure_LinuxFileSystem_1', custom_context)
    link_tasks(graph, 'LinuxFileSystem_3_started', 'start_LinuxFileSystem_3', custom_context)
    link_tasks(graph, 'LinuxFileSystem_2_configuring', 'LinuxFileSystem_2_created', custom_context)
    link_tasks(graph, 'LinuxFileSystem_2_configured', 'configure_LinuxFileSystem_2', custom_context)
    link_tasks(graph, 'LinuxFileSystem_4_started', 'start_LinuxFileSystem_4', custom_context)
    link_tasks(graph, 'LinuxFileSystem_1_started', 'start_LinuxFileSystem_1', custom_context)
    link_tasks(graph, 'LinuxFileSystem_3_configuring', 'LinuxFileSystem_3_created', custom_context)
    link_tasks(graph, 'LinuxFileSystem_3_configured', 'configure_LinuxFileSystem_3', custom_context)
    link_tasks(graph, 'LinuxFileSystem_2_starting', 'LinuxFileSystem_2_configured', custom_context)
    link_tasks(graph, 'LinuxFileSystem_2_started', 'start_LinuxFileSystem_2', custom_context)
    link_tasks(graph, 'LinuxFileSystem_4_configured', 'configure_LinuxFileSystem_4', custom_context)
    link_tasks(graph, 'LinuxFileSystem_3_creating', 'LinuxFileSystem_3_initial', custom_context)
    link_tasks(graph, 'LinuxFileSystem_2_creating', 'LinuxFileSystem_2_initial', custom_context)
    link_tasks(graph, 'LinuxFileSystem_2_created', 'LinuxFileSystem_2_creating', custom_context)
    link_tasks(graph, 'LinuxFileSystem_4_creating', 'LinuxFileSystem_4_initial', custom_context)
    link_tasks(graph, 'LinuxFileSystem_1_creating', 'LinuxFileSystem_1_initial', custom_context)
    link_tasks(graph, 'LinuxFileSystem_1_configuring', 'LinuxFileSystem_1_created', custom_context)
    link_tasks(graph, 'LinuxFileSystem_3_starting', 'LinuxFileSystem_3_configured', custom_context)
    link_tasks(graph, 'start_LinuxFileSystem_2', 'LinuxFileSystem_2_starting', custom_context)
    link_tasks(graph, 'LinuxFileSystem_1_starting', 'LinuxFileSystem_1_configured', custom_context)
    link_tasks(graph, 'LinuxFileSystem_4_created', 'LinuxFileSystem_4_creating', custom_context)
    link_tasks(graph, 'start_LinuxFileSystem_1', 'LinuxFileSystem_1_starting', custom_context)
    link_tasks(graph, 'start_LinuxFileSystem_4', 'LinuxFileSystem_4_starting', custom_context)
    link_tasks(graph, 'LinuxFileSystem_4_starting', 'LinuxFileSystem_4_configured', custom_context)
    link_tasks(graph, 'LinuxFileSystem_1_created', 'LinuxFileSystem_1_creating', custom_context)
    link_tasks(graph, 'LinuxFileSystem_4_initial', 'CBS4_install', custom_context)
    link_tasks(graph, 'LinuxFileSystem_3_initial', 'CBS3_install', custom_context)
    link_tasks(graph, 'LinuxFileSystem_1_initial', 'CBS1_install', custom_context)
    link_tasks(graph, 'LinuxFileSystem_2_initial', 'CBS2_install', custom_context)


# subworkflow 'uninstall' for host 'Compute'
def uninstall_host_compute(ctx, graph, custom_context):
    custom_context.add_customized_wf_node('LinuxFileSystem_4')
    custom_context.add_customized_wf_node('LinuxFileSystem_2')
    custom_context.add_customized_wf_node('LinuxFileSystem_3')
    custom_context.add_customized_wf_node('LinuxFileSystem_2')
    custom_context.add_customized_wf_node('LinuxFileSystem_1')
    custom_context.add_customized_wf_node('LinuxFileSystem_3')
    custom_context.add_customized_wf_node('LinuxFileSystem_1')
    custom_context.add_customized_wf_node('LinuxFileSystem_4')
    custom_context.add_customized_wf_node('LinuxFileSystem_1')
    custom_context.add_customized_wf_node('LinuxFileSystem_2')
    custom_context.add_customized_wf_node('LinuxFileSystem_2')
    custom_context.add_customized_wf_node('LinuxFileSystem_3')
    custom_context.add_customized_wf_node('LinuxFileSystem_4')
    custom_context.add_customized_wf_node('LinuxFileSystem_4')
    custom_context.add_customized_wf_node('LinuxFileSystem_3')
    custom_context.add_customized_wf_node('LinuxFileSystem_1')
    custom_context.register_native_delegate_wf_step('Compute', 'Compute_uninstall')
    set_state_task(ctx, graph, 'LinuxFileSystem_4', 'stopping', 'LinuxFileSystem_4_stopping', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_2', 'deleting', 'LinuxFileSystem_2_deleting', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_3', 'stopping', 'LinuxFileSystem_3_stopping', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_2', 'deleted', 'LinuxFileSystem_2_deleted', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_1', 'deleted', 'LinuxFileSystem_1_deleted', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_3', 'deleting', 'LinuxFileSystem_3_deleting', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_1', 'stopped', 'LinuxFileSystem_1_stopped', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_4', 'deleting', 'LinuxFileSystem_4_deleting', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_1', 'deleting', 'LinuxFileSystem_1_deleting', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_2', 'stopped', 'LinuxFileSystem_2_stopped', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_2', 'stopping', 'LinuxFileSystem_2_stopping', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_3', 'stopped', 'LinuxFileSystem_3_stopped', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_4', 'deleted', 'LinuxFileSystem_4_deleted', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_4', 'stopped', 'LinuxFileSystem_4_stopped', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_3', 'deleted', 'LinuxFileSystem_3_deleted', custom_context)
    operation_task(ctx, graph, 'LinuxFileSystem_1', 'cloudify.interfaces.lifecycle.stop', 'stop_LinuxFileSystem_1', custom_context)
    operation_task(ctx, graph, 'LinuxFileSystem_2', 'cloudify.interfaces.lifecycle.stop', 'stop_LinuxFileSystem_2', custom_context)
    operation_task(ctx, graph, 'LinuxFileSystem_3', 'cloudify.interfaces.lifecycle.stop', 'stop_LinuxFileSystem_3', custom_context)
    operation_task(ctx, graph, 'LinuxFileSystem_4', 'cloudify.interfaces.lifecycle.stop', 'stop_LinuxFileSystem_4', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_1', 'stopping', 'LinuxFileSystem_1_stopping', custom_context)
    custom_context.register_native_delegate_wf_step('CBS2', 'CBS2_uninstall')
    custom_context.register_native_delegate_wf_step('CBS4', 'CBS4_uninstall')
    custom_context.register_native_delegate_wf_step('CBS1', 'CBS1_uninstall')
    custom_context.register_native_delegate_wf_step('CBS3', 'CBS3_uninstall')
    generate_native_node_workflows(ctx, graph, custom_context, 'uninstall')
    link_tasks(graph, 'stop_LinuxFileSystem_4', 'LinuxFileSystem_4_stopping', custom_context)
    link_tasks(graph, 'LinuxFileSystem_2_deleted', 'LinuxFileSystem_2_deleting', custom_context)
    link_tasks(graph, 'stop_LinuxFileSystem_3', 'LinuxFileSystem_3_stopping', custom_context)
    link_tasks(graph, 'Compute_uninstall', 'LinuxFileSystem_2_deleted', custom_context)
    link_tasks(graph, 'CBS2_uninstall', 'LinuxFileSystem_2_deleted', custom_context)
    link_tasks(graph, 'Compute_uninstall', 'LinuxFileSystem_1_deleted', custom_context)
    link_tasks(graph, 'CBS1_uninstall', 'LinuxFileSystem_1_deleted', custom_context)
    link_tasks(graph, 'LinuxFileSystem_3_deleted', 'LinuxFileSystem_3_deleting', custom_context)
    link_tasks(graph, 'LinuxFileSystem_1_deleting', 'LinuxFileSystem_1_stopped', custom_context)
    link_tasks(graph, 'LinuxFileSystem_4_deleted', 'LinuxFileSystem_4_deleting', custom_context)
    link_tasks(graph, 'LinuxFileSystem_1_deleted', 'LinuxFileSystem_1_deleting', custom_context)
    link_tasks(graph, 'LinuxFileSystem_2_deleting', 'LinuxFileSystem_2_stopped', custom_context)
    link_tasks(graph, 'stop_LinuxFileSystem_2', 'LinuxFileSystem_2_stopping', custom_context)
    link_tasks(graph, 'LinuxFileSystem_3_deleting', 'LinuxFileSystem_3_stopped', custom_context)
    link_tasks(graph, 'Compute_uninstall', 'LinuxFileSystem_4_deleted', custom_context)
    link_tasks(graph, 'CBS4_uninstall', 'LinuxFileSystem_4_deleted', custom_context)
    link_tasks(graph, 'LinuxFileSystem_4_deleting', 'LinuxFileSystem_4_stopped', custom_context)
    link_tasks(graph, 'CBS3_uninstall', 'LinuxFileSystem_3_deleted', custom_context)
    link_tasks(graph, 'Compute_uninstall', 'LinuxFileSystem_3_deleted', custom_context)
    link_tasks(graph, 'LinuxFileSystem_1_stopped', 'stop_LinuxFileSystem_1', custom_context)
    link_tasks(graph, 'LinuxFileSystem_2_stopped', 'stop_LinuxFileSystem_2', custom_context)
    link_tasks(graph, 'LinuxFileSystem_3_stopped', 'stop_LinuxFileSystem_3', custom_context)
    link_tasks(graph, 'LinuxFileSystem_4_stopped', 'stop_LinuxFileSystem_4', custom_context)
    link_tasks(graph, 'stop_LinuxFileSystem_1', 'LinuxFileSystem_1_stopping', custom_context)


def install_host(ctx, graph, custom_context, compute):
    options = {}
    options['Compute'] = install_host_compute
    options[compute](ctx, graph, custom_context)


def uninstall_host(ctx, graph, custom_context, compute):
    options = {}
    options['Compute'] = uninstall_host_compute
    options[compute](ctx, graph, custom_context)


@workflow
def a4c_install(**kwargs):
    graph = ctx.graph_mode()
    nodes = _get_all_nodes(ctx)
    instances = _get_all_nodes_instances(ctx)
    custom_context = CustomContext(ctx, instances, nodes)
    ctx.internal.send_workflow_event(event_type='a4c_workflow_started', message=build_pre_event(WfStartEvent('install')))
    _a4c_install(ctx, graph, custom_context)
    return graph.execute()


@workflow
def a4c_uninstall(**kwargs):
    graph = ctx.graph_mode()
    nodes = _get_all_nodes(ctx)
    instances = _get_all_nodes_instances(ctx)
    custom_context = CustomContext(ctx, instances, nodes)
    ctx.internal.send_workflow_event(event_type='a4c_workflow_started', message=build_pre_event(WfStartEvent('uninstall')))
    _a4c_uninstall(ctx, graph, custom_context)
    return graph.execute()


def _a4c_install(ctx, graph, custom_context):
    #  following code can be pasted in src/test/python/workflows/tasks.py for simulation
    custom_context.add_customized_wf_node('LinuxFileSystem_2')
    custom_context.add_customized_wf_node('LinuxFileSystem_4')
    custom_context.add_customized_wf_node('LinuxFileSystem_3')
    custom_context.add_customized_wf_node('LinuxFileSystem_1')
    custom_context.add_customized_wf_node('LinuxFileSystem_3')
    custom_context.add_customized_wf_node('LinuxFileSystem_3')
    custom_context.add_customized_wf_node('LinuxFileSystem_4')
    custom_context.add_customized_wf_node('LinuxFileSystem_3')
    custom_context.add_customized_wf_node('LinuxFileSystem_4')
    custom_context.add_customized_wf_node('LinuxFileSystem_2')
    custom_context.add_customized_wf_node('LinuxFileSystem_3')
    custom_context.add_customized_wf_node('LinuxFileSystem_2')
    custom_context.add_customized_wf_node('LinuxFileSystem_3')
    custom_context.add_customized_wf_node('LinuxFileSystem_2')
    custom_context.add_customized_wf_node('LinuxFileSystem_2')
    custom_context.add_customized_wf_node('LinuxFileSystem_4')
    custom_context.add_customized_wf_node('LinuxFileSystem_1')
    custom_context.add_customized_wf_node('LinuxFileSystem_1')
    custom_context.add_customized_wf_node('LinuxFileSystem_1')
    custom_context.add_customized_wf_node('LinuxFileSystem_2')
    custom_context.add_customized_wf_node('LinuxFileSystem_3')
    custom_context.add_customized_wf_node('LinuxFileSystem_2')
    custom_context.add_customized_wf_node('LinuxFileSystem_1')
    custom_context.add_customized_wf_node('LinuxFileSystem_4')
    custom_context.add_customized_wf_node('LinuxFileSystem_1')
    custom_context.add_customized_wf_node('LinuxFileSystem_4')
    custom_context.add_customized_wf_node('LinuxFileSystem_4')
    custom_context.add_customized_wf_node('LinuxFileSystem_1')
    set_state_task(ctx, graph, 'LinuxFileSystem_2', 'configuring', 'LinuxFileSystem_2_configuring', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_4', 'configuring', 'LinuxFileSystem_4_configuring', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_3', 'starting', 'LinuxFileSystem_3_starting', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_1', 'configuring', 'LinuxFileSystem_1_configuring', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_3', 'creating', 'LinuxFileSystem_3_creating', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_3', 'configuring', 'LinuxFileSystem_3_configuring', custom_context)
    custom_context.register_native_delegate_wf_step('Compute', 'Compute_install')
    set_state_task(ctx, graph, 'LinuxFileSystem_4', 'started', 'LinuxFileSystem_4_started', custom_context)
    custom_context.register_native_delegate_wf_step('CBS4', 'CBS4_install')
    set_state_task(ctx, graph, 'LinuxFileSystem_3', 'started', 'LinuxFileSystem_3_started', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_4', 'created', 'LinuxFileSystem_4_created', custom_context)
    custom_context.register_native_delegate_wf_step('CBS3', 'CBS3_install')
    operation_task(ctx, graph, 'LinuxFileSystem_1', 'cloudify.interfaces.lifecycle.configure', 'configure_LinuxFileSystem_1', custom_context)
    operation_task(ctx, graph, 'LinuxFileSystem_3', 'cloudify.interfaces.lifecycle.start', 'start_LinuxFileSystem_3', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_2', 'created', 'LinuxFileSystem_2_created', custom_context)
    operation_task(ctx, graph, 'LinuxFileSystem_2', 'cloudify.interfaces.lifecycle.configure', 'configure_LinuxFileSystem_2', custom_context)
    operation_task(ctx, graph, 'LinuxFileSystem_4', 'cloudify.interfaces.lifecycle.start', 'start_LinuxFileSystem_4', custom_context)
    operation_task(ctx, graph, 'LinuxFileSystem_1', 'cloudify.interfaces.lifecycle.start', 'start_LinuxFileSystem_1', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_3', 'created', 'LinuxFileSystem_3_created', custom_context)
    operation_task(ctx, graph, 'LinuxFileSystem_3', 'cloudify.interfaces.lifecycle.configure', 'configure_LinuxFileSystem_3', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_2', 'configured', 'LinuxFileSystem_2_configured', custom_context)
    operation_task(ctx, graph, 'LinuxFileSystem_2', 'cloudify.interfaces.lifecycle.start', 'start_LinuxFileSystem_2', custom_context)
    operation_task(ctx, graph, 'LinuxFileSystem_4', 'cloudify.interfaces.lifecycle.configure', 'configure_LinuxFileSystem_4', custom_context)
    custom_context.register_native_delegate_wf_step('PublicNetwork', 'PublicNetwork_install')
    set_state_task(ctx, graph, 'LinuxFileSystem_3', 'initial', 'LinuxFileSystem_3_initial', custom_context)
    custom_context.register_native_delegate_wf_step('CBS1', 'CBS1_install')
    set_state_task(ctx, graph, 'LinuxFileSystem_2', 'initial', 'LinuxFileSystem_2_initial', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_2', 'creating', 'LinuxFileSystem_2_creating', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_4', 'initial', 'LinuxFileSystem_4_initial', custom_context)
    custom_context.register_native_delegate_wf_step('CBS2', 'CBS2_install')
    set_state_task(ctx, graph, 'LinuxFileSystem_1', 'initial', 'LinuxFileSystem_1_initial', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_1', 'created', 'LinuxFileSystem_1_created', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_1', 'started', 'LinuxFileSystem_1_started', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_2', 'started', 'LinuxFileSystem_2_started', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_3', 'configured', 'LinuxFileSystem_3_configured', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_2', 'starting', 'LinuxFileSystem_2_starting', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_1', 'configured', 'LinuxFileSystem_1_configured', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_4', 'creating', 'LinuxFileSystem_4_creating', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_1', 'starting', 'LinuxFileSystem_1_starting', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_4', 'starting', 'LinuxFileSystem_4_starting', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_4', 'configured', 'LinuxFileSystem_4_configured', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_1', 'creating', 'LinuxFileSystem_1_creating', custom_context)
    generate_native_node_workflows(ctx, graph, custom_context, 'install')
    link_tasks(graph, 'LinuxFileSystem_2_configuring', 'LinuxFileSystem_2_created', custom_context)
    link_tasks(graph, 'LinuxFileSystem_4_configuring', 'LinuxFileSystem_4_created', custom_context)
    link_tasks(graph, 'LinuxFileSystem_3_starting', 'LinuxFileSystem_3_configured', custom_context)
    link_tasks(graph, 'LinuxFileSystem_1_configuring', 'LinuxFileSystem_1_created', custom_context)
    link_tasks(graph, 'LinuxFileSystem_3_creating', 'LinuxFileSystem_3_initial', custom_context)
    link_tasks(graph, 'LinuxFileSystem_3_configuring', 'LinuxFileSystem_3_created', custom_context)
    link_tasks(graph, 'LinuxFileSystem_4_started', 'start_LinuxFileSystem_4', custom_context)
    link_tasks(graph, 'LinuxFileSystem_3_started', 'start_LinuxFileSystem_3', custom_context)
    link_tasks(graph, 'LinuxFileSystem_4_created', 'LinuxFileSystem_4_creating', custom_context)
    link_tasks(graph, 'configure_LinuxFileSystem_1', 'LinuxFileSystem_1_configuring', custom_context)
    link_tasks(graph, 'start_LinuxFileSystem_3', 'LinuxFileSystem_3_starting', custom_context)
    link_tasks(graph, 'LinuxFileSystem_2_created', 'LinuxFileSystem_2_creating', custom_context)
    link_tasks(graph, 'configure_LinuxFileSystem_2', 'LinuxFileSystem_2_configuring', custom_context)
    link_tasks(graph, 'start_LinuxFileSystem_4', 'LinuxFileSystem_4_starting', custom_context)
    link_tasks(graph, 'start_LinuxFileSystem_1', 'LinuxFileSystem_1_starting', custom_context)
    link_tasks(graph, 'LinuxFileSystem_3_created', 'LinuxFileSystem_3_creating', custom_context)
    link_tasks(graph, 'configure_LinuxFileSystem_3', 'LinuxFileSystem_3_configuring', custom_context)
    link_tasks(graph, 'LinuxFileSystem_2_configured', 'configure_LinuxFileSystem_2', custom_context)
    link_tasks(graph, 'start_LinuxFileSystem_2', 'LinuxFileSystem_2_starting', custom_context)
    link_tasks(graph, 'configure_LinuxFileSystem_4', 'LinuxFileSystem_4_configuring', custom_context)
    link_tasks(graph, 'LinuxFileSystem_3_initial', 'Compute_install', custom_context)
    link_tasks(graph, 'LinuxFileSystem_3_initial', 'CBS3_install', custom_context)
    link_tasks(graph, 'LinuxFileSystem_2_initial', 'Compute_install', custom_context)
    link_tasks(graph, 'LinuxFileSystem_2_initial', 'CBS2_install', custom_context)
    link_tasks(graph, 'LinuxFileSystem_2_creating', 'LinuxFileSystem_2_initial', custom_context)
    link_tasks(graph, 'LinuxFileSystem_4_initial', 'Compute_install', custom_context)
    link_tasks(graph, 'LinuxFileSystem_4_initial', 'CBS4_install', custom_context)
    link_tasks(graph, 'LinuxFileSystem_1_initial', 'Compute_install', custom_context)
    link_tasks(graph, 'LinuxFileSystem_1_initial', 'CBS1_install', custom_context)
    link_tasks(graph, 'LinuxFileSystem_1_created', 'LinuxFileSystem_1_creating', custom_context)
    link_tasks(graph, 'LinuxFileSystem_1_started', 'start_LinuxFileSystem_1', custom_context)
    link_tasks(graph, 'LinuxFileSystem_2_started', 'start_LinuxFileSystem_2', custom_context)
    link_tasks(graph, 'LinuxFileSystem_3_configured', 'configure_LinuxFileSystem_3', custom_context)
    link_tasks(graph, 'LinuxFileSystem_2_starting', 'LinuxFileSystem_2_configured', custom_context)
    link_tasks(graph, 'LinuxFileSystem_1_configured', 'configure_LinuxFileSystem_1', custom_context)
    link_tasks(graph, 'LinuxFileSystem_4_creating', 'LinuxFileSystem_4_initial', custom_context)
    link_tasks(graph, 'LinuxFileSystem_1_starting', 'LinuxFileSystem_1_configured', custom_context)
    link_tasks(graph, 'LinuxFileSystem_4_starting', 'LinuxFileSystem_4_configured', custom_context)
    link_tasks(graph, 'LinuxFileSystem_4_configured', 'configure_LinuxFileSystem_4', custom_context)
    link_tasks(graph, 'LinuxFileSystem_1_creating', 'LinuxFileSystem_1_initial', custom_context)
def _a4c_uninstall(ctx, graph, custom_context):
    #  following code can be pasted in src/test/python/workflows/tasks.py for simulation
    custom_context.add_customized_wf_node('LinuxFileSystem_4')
    custom_context.add_customized_wf_node('LinuxFileSystem_2')
    custom_context.add_customized_wf_node('LinuxFileSystem_3')
    custom_context.add_customized_wf_node('LinuxFileSystem_2')
    custom_context.add_customized_wf_node('LinuxFileSystem_1')
    custom_context.add_customized_wf_node('LinuxFileSystem_3')
    custom_context.add_customized_wf_node('LinuxFileSystem_1')
    custom_context.add_customized_wf_node('LinuxFileSystem_4')
    custom_context.add_customized_wf_node('LinuxFileSystem_1')
    custom_context.add_customized_wf_node('LinuxFileSystem_2')
    custom_context.add_customized_wf_node('LinuxFileSystem_2')
    custom_context.add_customized_wf_node('LinuxFileSystem_3')
    custom_context.add_customized_wf_node('LinuxFileSystem_4')
    custom_context.add_customized_wf_node('LinuxFileSystem_4')
    custom_context.add_customized_wf_node('LinuxFileSystem_3')
    custom_context.add_customized_wf_node('LinuxFileSystem_1')
    custom_context.register_native_delegate_wf_step('Compute', 'Compute_uninstall')
    custom_context.register_native_delegate_wf_step('PublicNetwork', 'PublicNetwork_uninstall')
    set_state_task(ctx, graph, 'LinuxFileSystem_4', 'stopping', 'LinuxFileSystem_4_stopping', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_2', 'deleting', 'LinuxFileSystem_2_deleting', custom_context)
    custom_context.register_native_delegate_wf_step('CBS2', 'CBS2_uninstall')
    set_state_task(ctx, graph, 'LinuxFileSystem_3', 'stopping', 'LinuxFileSystem_3_stopping', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_2', 'deleted', 'LinuxFileSystem_2_deleted', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_1', 'deleted', 'LinuxFileSystem_1_deleted', custom_context)
    custom_context.register_native_delegate_wf_step('CBS4', 'CBS4_uninstall')
    set_state_task(ctx, graph, 'LinuxFileSystem_3', 'deleting', 'LinuxFileSystem_3_deleting', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_1', 'stopped', 'LinuxFileSystem_1_stopped', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_4', 'deleting', 'LinuxFileSystem_4_deleting', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_1', 'deleting', 'LinuxFileSystem_1_deleting', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_2', 'stopped', 'LinuxFileSystem_2_stopped', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_2', 'stopping', 'LinuxFileSystem_2_stopping', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_3', 'stopped', 'LinuxFileSystem_3_stopped', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_4', 'deleted', 'LinuxFileSystem_4_deleted', custom_context)
    custom_context.register_native_delegate_wf_step('CBS1', 'CBS1_uninstall')
    set_state_task(ctx, graph, 'LinuxFileSystem_4', 'stopped', 'LinuxFileSystem_4_stopped', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_3', 'deleted', 'LinuxFileSystem_3_deleted', custom_context)
    custom_context.register_native_delegate_wf_step('CBS3', 'CBS3_uninstall')
    operation_task(ctx, graph, 'LinuxFileSystem_1', 'cloudify.interfaces.lifecycle.stop', 'stop_LinuxFileSystem_1', custom_context)
    operation_task(ctx, graph, 'LinuxFileSystem_2', 'cloudify.interfaces.lifecycle.stop', 'stop_LinuxFileSystem_2', custom_context)
    operation_task(ctx, graph, 'LinuxFileSystem_3', 'cloudify.interfaces.lifecycle.stop', 'stop_LinuxFileSystem_3', custom_context)
    operation_task(ctx, graph, 'LinuxFileSystem_4', 'cloudify.interfaces.lifecycle.stop', 'stop_LinuxFileSystem_4', custom_context)
    set_state_task(ctx, graph, 'LinuxFileSystem_1', 'stopping', 'LinuxFileSystem_1_stopping', custom_context)
    generate_native_node_workflows(ctx, graph, custom_context, 'uninstall')
    link_tasks(graph, 'Compute_uninstall', 'LinuxFileSystem_2_deleted', custom_context)
    link_tasks(graph, 'Compute_uninstall', 'LinuxFileSystem_1_deleted', custom_context)
    link_tasks(graph, 'Compute_uninstall', 'LinuxFileSystem_4_deleted', custom_context)
    link_tasks(graph, 'Compute_uninstall', 'LinuxFileSystem_3_deleted', custom_context)
    link_tasks(graph, 'LinuxFileSystem_2_deleting', 'LinuxFileSystem_2_stopped', custom_context)
    link_tasks(graph, 'CBS2_uninstall', 'LinuxFileSystem_2_deleted', custom_context)
    link_tasks(graph, 'LinuxFileSystem_2_deleted', 'LinuxFileSystem_2_deleting', custom_context)
    link_tasks(graph, 'LinuxFileSystem_1_deleted', 'LinuxFileSystem_1_deleting', custom_context)
    link_tasks(graph, 'CBS4_uninstall', 'LinuxFileSystem_4_deleted', custom_context)
    link_tasks(graph, 'LinuxFileSystem_3_deleting', 'LinuxFileSystem_3_stopped', custom_context)
    link_tasks(graph, 'LinuxFileSystem_1_stopped', 'stop_LinuxFileSystem_1', custom_context)
    link_tasks(graph, 'LinuxFileSystem_4_deleting', 'LinuxFileSystem_4_stopped', custom_context)
    link_tasks(graph, 'LinuxFileSystem_1_deleting', 'LinuxFileSystem_1_stopped', custom_context)
    link_tasks(graph, 'LinuxFileSystem_2_stopped', 'stop_LinuxFileSystem_2', custom_context)
    link_tasks(graph, 'LinuxFileSystem_3_stopped', 'stop_LinuxFileSystem_3', custom_context)
    link_tasks(graph, 'LinuxFileSystem_4_deleted', 'LinuxFileSystem_4_deleting', custom_context)
    link_tasks(graph, 'CBS1_uninstall', 'LinuxFileSystem_1_deleted', custom_context)
    link_tasks(graph, 'LinuxFileSystem_4_stopped', 'stop_LinuxFileSystem_4', custom_context)
    link_tasks(graph, 'LinuxFileSystem_3_deleted', 'LinuxFileSystem_3_deleting', custom_context)
    link_tasks(graph, 'CBS3_uninstall', 'LinuxFileSystem_3_deleted', custom_context)
    link_tasks(graph, 'stop_LinuxFileSystem_1', 'LinuxFileSystem_1_stopping', custom_context)
    link_tasks(graph, 'stop_LinuxFileSystem_2', 'LinuxFileSystem_2_stopping', custom_context)
    link_tasks(graph, 'stop_LinuxFileSystem_3', 'LinuxFileSystem_3_stopping', custom_context)
    link_tasks(graph, 'stop_LinuxFileSystem_4', 'LinuxFileSystem_4_stopping', custom_context)

def _get_scaling_group_name_from_node_id(ctx, node_id):
    scaling_groups=ctx.deployment.scaling_groups
    for group_name, scaling_group in ctx.deployment.scaling_groups.iteritems():
        for member in scaling_group['members']:
            if member == node_id:
                ctx.logger.info("Node {} found in scaling group {}".format(node_id, group_name))
                return group_name
    return None

@workflow
def a4c_scale(ctx, node_id, delta, scale_compute, **kwargs):
    delta = int(delta)
    scalable_entity_name = _get_scaling_group_name_from_node_id(ctx, node_id)
    scaling_group = ctx.deployment.scaling_groups.get(scalable_entity_name)
    if scalable_entity_name:
        curr_num_instances = scaling_group['properties']['current_instances']
        planned_num_instances = curr_num_instances + delta
        scale_id = scalable_entity_name
    else:
      scaled_node = ctx.get_node(scalable_entity_name)
      if not scaled_node:
          raise ValueError("Node {0} doesn't exist".format(scalable_entity_name))
      if not is_host_node(scaled_node):
          raise ValueError("Node {0} is not a host. This workflow can only scale hosts".format(scalable_entity_name))
      if delta == 0:
          ctx.logger.info('delta parameter is 0, so no scaling will take place.')
          return
      curr_num_instances = scaled_node.number_of_instances
      planned_num_instances = curr_num_instances + delta
      scale_id = scaled_node.id

    if planned_num_instances < 1:
        raise ValueError('Provided delta: {0} is illegal. current number of'
                         'instances of node/group {1} is {2}'
                         .format(delta, scalable_entity_name, curr_num_instances))

    modification = ctx.deployment.start_modification({
        scale_id: {
            'instances': planned_num_instances
        }
    })
    ctx.logger.info('Deployment modification started. [modification_id={0} : {1}]'.format(modification.id, dir(modification)))
    
    try:
        if delta > 0:
            ctx.logger.info('Scaling host/group {0} adding {1} instances'.format(scalable_entity_name, delta))
            added_and_related = _get_all_nodes(modification.added)
            added = _get_all_modified_node_instances(added_and_related, 'added')
            graph = ctx.graph_mode()
            ctx.internal.send_workflow_event(event_type='a4c_workflow_started',
                                             message=build_pre_event(WfStartEvent('scale', 'install')))
            custom_context = CustomContext(ctx, added, added_and_related)
            install_host(ctx, graph, custom_context, node_id)
            try:
                graph.execute()
            except:
                ctx.logger.error('Scale failed. Uninstalling node/group {0}'.format(scalable_entity_name))
                graph = ctx.internal.task_graph
                for task in graph.tasks_iter():
                    graph.remove_task(task)
                try:
                    custom_context = CustomContext(ctx, added, added_and_related)
                    uninstall_host(ctx, graph, custom_context, scalable_entity_name)
                    graph.execute()
                except:
                    ctx.logger.error('Node {0} uninstallation following scale failure has failed'.format(scalable_entity_name))
                raise
        else:
            ctx.logger.info('Unscaling host/group {0} removing {1} instances'.format(scalable_entity_name, delta))
            removed_and_related = _get_all_nodes(modification.removed)
            removed = _get_all_modified_node_instances(removed_and_related, 'removed')
            graph = ctx.graph_mode()
            ctx.internal.send_workflow_event(event_type='a4c_workflow_started',
                                             message=build_pre_event(WfStartEvent('scale', 'uninstall')))
            custom_context = CustomContext(ctx, removed, removed_and_related)
            uninstall_host(ctx, graph, custom_context, node_id)
            try:
                graph.execute()
            except:
                ctx.logger.error('Unscale failed.')
                raise
    except:
        ctx.logger.warn('Rolling back deployment modification. [modification_id={0}]'.format(modification.id))
        try:
            modification.rollback()
        except:
            ctx.logger.warn('Deployment modification rollback failed. The '
                            'deployment model is most likely in some corrupted'
                            ' state.'
                            '[modification_id={0}]'.format(modification.id))
            raise
        raise
    else:
        try:
            modification.finish()
        except:
            ctx.logger.warn('Deployment modification finish failed. The '
                            'deployment model is most likely in some corrupted'
                            ' state.'
                            '[modification_id={0}]'.format(modification.id))
            raise


@workflow
def a4c_heal(
        ctx,
        node_instance_id,
        diagnose_value='Not provided',
        **kwargs):
    """Reinstalls the whole subgraph of the system topology

    The subgraph consists of all the nodes that are hosted in the
    failing node's compute and the compute itself.
    Additionally it unlinks and establishes appropriate relationships

    :param ctx: cloudify context
    :param node_id: failing node's id
    :param diagnose_value: diagnosed reason of failure
    """

    ctx.logger.info("Starting 'heal' workflow on {0}, Diagnosis: {1}"
                    .format(node_instance_id, diagnose_value))
    failing_node = ctx.get_node_instance(node_instance_id)
    host_instance_id = failing_node._node_instance.host_id
    failing_node_host = ctx.get_node_instance(host_instance_id)
    node_id = failing_node_host.node_id
    subgraph_node_instances = failing_node_host.get_contained_subgraph()
    added_and_related = _get_all_nodes(ctx)
    try:
      graph = ctx.graph_mode()
      ctx.internal.send_workflow_event(event_type='a4c_workflow_started',
                                               message=build_pre_event(WfStartEvent('heal', 'uninstall')))
      custom_context = CustomContext(ctx, subgraph_node_instances, added_and_related)
      uninstall_host(ctx, graph, custom_context, node_id)
      graph.execute()
    except:
      ctx.logger.error('Uninstall while healing failed.')
    graph = ctx.internal.task_graph
    for task in graph.tasks_iter():
      graph.remove_task(task)
    ctx.internal.send_workflow_event(event_type='a4c_workflow_started',
                                             message=build_pre_event(WfStartEvent('heal', 'install')))
    custom_context = CustomContext(ctx, subgraph_node_instances, added_and_related)
    install_host(ctx, graph, custom_context, node_id)
    graph.execute()

#following code can be pasted in src/test/python/workflows/context.py for simulation
#def _build_nodes(ctx):
    #types = []
    #types.append('alien.cloudify.openstack.nodes.Volume')
    #types.append('alien.cloudify.openstack.nodes.DeletableVolume')
    #types.append('tosca.nodes.BlockStorage')
    #types.append('tosca.nodes.Root')
    #node_CBS3 = _build_node(ctx, 'CBS3', types, 1)
    #types = []
    #types.append('alien.cloudify.openstack.nodes.Volume')
    #types.append('alien.cloudify.openstack.nodes.DeletableVolume')
    #types.append('tosca.nodes.BlockStorage')
    #types.append('tosca.nodes.Root')
    #node_CBS2 = _build_node(ctx, 'CBS2', types, 1)
    #types = []
    #types.append('alien.nodes.LinuxFileSystem')
    #types.append('tosca.nodes.SoftwareComponent')
    #types.append('tosca.nodes.Root')
    #node_LinuxFileSystem_1 = _build_node(ctx, 'LinuxFileSystem_1', types, 1)
    #types = []
    #types.append('alien.cloudify.openstack.nodes.Volume')
    #types.append('alien.cloudify.openstack.nodes.DeletableVolume')
    #types.append('tosca.nodes.BlockStorage')
    #types.append('tosca.nodes.Root')
    #node_CBS1 = _build_node(ctx, 'CBS1', types, 1)
    #types = []
    #types.append('alien.nodes.LinuxFileSystem')
    #types.append('tosca.nodes.SoftwareComponent')
    #types.append('tosca.nodes.Root')
    #node_LinuxFileSystem_2 = _build_node(ctx, 'LinuxFileSystem_2', types, 1)
    #types = []
    #types.append('alien.nodes.LinuxFileSystem')
    #types.append('tosca.nodes.SoftwareComponent')
    #types.append('tosca.nodes.Root')
    #node_LinuxFileSystem_3 = _build_node(ctx, 'LinuxFileSystem_3', types, 1)
    #types = []
    #types.append('alien.nodes.LinuxFileSystem')
    #types.append('tosca.nodes.SoftwareComponent')
    #types.append('tosca.nodes.Root')
    #node_LinuxFileSystem_4 = _build_node(ctx, 'LinuxFileSystem_4', types, 1)
    #types = []
    #types.append('alien.nodes.openstack.Compute')
    #types.append('tosca.nodes.Compute')
    #types.append('tosca.nodes.Root')
    #node_Compute = _build_node(ctx, 'Compute', types, 1)
    #types = []
    #types.append('alien.nodes.openstack.PublicNetwork')
    #types.append('alien.nodes.PublicNetwork')
    #types.append('tosca.nodes.Network')
    #types.append('tosca.nodes.Root')
    #node_PublicNetwork = _build_node(ctx, 'PublicNetwork', types, 1)
    #types = []
    #types.append('alien.cloudify.openstack.nodes.Volume')
    #types.append('alien.cloudify.openstack.nodes.DeletableVolume')
    #types.append('tosca.nodes.BlockStorage')
    #types.append('tosca.nodes.Root')
    #node_CBS4 = _build_node(ctx, 'CBS4', types, 1)
    #_add_relationship(node_CBS3, node_Compute)
    #_add_relationship(node_CBS2, node_Compute)
    #_add_relationship(node_LinuxFileSystem_1, node_Compute)
    #_add_relationship(node_LinuxFileSystem_1, node_CBS1)
    #_add_relationship(node_CBS1, node_Compute)
    #_add_relationship(node_LinuxFileSystem_2, node_Compute)
    #_add_relationship(node_LinuxFileSystem_2, node_CBS2)
    #_add_relationship(node_LinuxFileSystem_3, node_Compute)
    #_add_relationship(node_LinuxFileSystem_3, node_CBS3)
    #_add_relationship(node_LinuxFileSystem_4, node_Compute)
    #_add_relationship(node_LinuxFileSystem_4, node_CBS4)
    #_add_relationship(node_Compute, node_PublicNetwork)
    #_add_relationship(node_CBS4, node_Compute)
