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


# subworkflow 'install' for host 'Server'
def install_host_server(ctx, graph, custom_context):
    custom_context.add_customized_wf_node('Java')
    custom_context.add_customized_wf_node('Java')
    custom_context.add_customized_wf_node('Artifact_Directory_Test')
    custom_context.add_customized_wf_node('War')
    custom_context.add_customized_wf_node('War')
    custom_context.add_customized_wf_node('War')
    custom_context.add_customized_wf_node('Java')
    custom_context.add_customized_wf_node('War')
    custom_context.add_customized_wf_node('Tomcat')
    custom_context.add_customized_wf_node('Tomcat')
    custom_context.add_customized_wf_node('Java')
    custom_context.add_customized_wf_node('Tomcat')
    custom_context.add_customized_wf_node('War')
    custom_context.add_customized_wf_node('Tomcat')
    custom_context.add_customized_wf_node('Tomcat')
    custom_context.add_customized_wf_node('Tomcat')
    custom_context.add_customized_wf_node('Artifact_Directory_Test')
    custom_context.add_customized_wf_node('Artifact_Directory_Test')
    custom_context.add_customized_wf_node('Artifact_Directory_Test')
    custom_context.add_customized_wf_node('Artifact_Directory_Test')
    custom_context.add_customized_wf_node('War')
    custom_context.add_customized_wf_node('Java')
    custom_context.add_customized_wf_node('Tomcat')
    custom_context.add_customized_wf_node('War')
    custom_context.add_customized_wf_node('Artifact_Directory_Test')
    custom_context.add_customized_wf_node('Java')
    custom_context.add_customized_wf_node('Java')
    custom_context.add_customized_wf_node('Artifact_Directory_Test')
    set_state_task(ctx, graph, 'Java', 'started', 'Java_started', custom_context)
    set_state_task(ctx, graph, 'Java', 'creating', 'Java_creating', custom_context)
    operation_task(ctx, graph, 'Tomcat', 'cloudify.interfaces.lifecycle.create', 'create_Tomcat', custom_context)
    set_state_task(ctx, graph, 'Artifact_Directory_Test', 'configured', 'Artifact_Directory_Test_configured', custom_context)
    operation_task(ctx, graph, 'Artifact_Directory_Test', 'cloudify.interfaces.lifecycle.start', 'start_Artifact_Directory_Test', custom_context)
    set_state_task(ctx, graph, 'War', 'configured', 'War_configured', custom_context)
    operation_task(ctx, graph, 'Artifact_Directory_Test', 'cloudify.interfaces.lifecycle.create', 'create_Artifact_Directory_Test', custom_context)
    set_state_task(ctx, graph, 'War', 'starting', 'War_starting', custom_context)
    set_state_task(ctx, graph, 'War', 'started', 'War_started', custom_context)
    set_state_task(ctx, graph, 'Java', 'created', 'Java_created', custom_context)
    set_state_task(ctx, graph, 'War', 'configuring', 'War_configuring', custom_context)
    set_state_task(ctx, graph, 'Tomcat', 'starting', 'Tomcat_starting', custom_context)
    operation_task(ctx, graph, 'War', 'cloudify.interfaces.lifecycle.configure', 'configure_War', custom_context)
    set_state_task(ctx, graph, 'Tomcat', 'creating', 'Tomcat_creating', custom_context)
    set_state_task(ctx, graph, 'Java', 'starting', 'Java_starting', custom_context)
    set_state_task(ctx, graph, 'Tomcat', 'created', 'Tomcat_created', custom_context)
    set_state_task(ctx, graph, 'War', 'created', 'War_created', custom_context)
    operation_task(ctx, graph, 'Java', 'cloudify.interfaces.lifecycle.start', 'start_Java', custom_context)
    set_state_task(ctx, graph, 'Tomcat', 'initial', 'Tomcat_initial', custom_context)
    set_state_task(ctx, graph, 'Tomcat', 'configuring', 'Tomcat_configuring', custom_context)
    operation_task(ctx, graph, 'Artifact_Directory_Test', 'cloudify.interfaces.lifecycle.configure', 'configure_Artifact_Directory_Test', custom_context)
    set_state_task(ctx, graph, 'Tomcat', 'started', 'Tomcat_started', custom_context)
    set_state_task(ctx, graph, 'Artifact_Directory_Test', 'creating', 'Artifact_Directory_Test_creating', custom_context)
    operation_task(ctx, graph, 'Java', 'cloudify.interfaces.lifecycle.configure', 'configure_Java', custom_context)
    set_state_task(ctx, graph, 'Artifact_Directory_Test', 'created', 'Artifact_Directory_Test_created', custom_context)
    operation_task(ctx, graph, 'Java', 'cloudify.interfaces.lifecycle.create', 'create_Java', custom_context)
    operation_task(ctx, graph, 'War', 'cloudify.interfaces.lifecycle.create', 'create_War', custom_context)
    set_state_task(ctx, graph, 'Artifact_Directory_Test', 'started', 'Artifact_Directory_Test_started', custom_context)
    set_state_task(ctx, graph, 'Artifact_Directory_Test', 'starting', 'Artifact_Directory_Test_starting', custom_context)
    custom_context.register_native_delegate_wf_step('Server', 'Server_install')
    set_state_task(ctx, graph, 'War', 'creating', 'War_creating', custom_context)
    operation_task(ctx, graph, 'War', 'cloudify.interfaces.lifecycle.start', 'start_War', custom_context)
    set_state_task(ctx, graph, 'Java', 'configured', 'Java_configured', custom_context)
    set_state_task(ctx, graph, 'Tomcat', 'configured', 'Tomcat_configured', custom_context)
    operation_task(ctx, graph, 'Tomcat', 'cloudify.interfaces.lifecycle.configure', 'configure_Tomcat', custom_context)
    set_state_task(ctx, graph, 'War', 'initial', 'War_initial', custom_context)
    set_state_task(ctx, graph, 'Artifact_Directory_Test', 'configuring', 'Artifact_Directory_Test_configuring', custom_context)
    operation_task(ctx, graph, 'Tomcat', 'cloudify.interfaces.lifecycle.start', 'start_Tomcat', custom_context)
    set_state_task(ctx, graph, 'Java', 'configuring', 'Java_configuring', custom_context)
    set_state_task(ctx, graph, 'Java', 'initial', 'Java_initial', custom_context)
    set_state_task(ctx, graph, 'Artifact_Directory_Test', 'initial', 'Artifact_Directory_Test_initial', custom_context)
    generate_native_node_workflows(ctx, graph, custom_context, 'install')
    link_tasks(graph, 'Tomcat_configuring', 'Java_started', custom_context)
    link_tasks(graph, 'create_Java', 'Java_creating', custom_context)
    link_tasks(graph, 'Tomcat_created', 'create_Tomcat', custom_context)
    link_tasks(graph, 'Artifact_Directory_Test_starting', 'Artifact_Directory_Test_configured', custom_context)
    link_tasks(graph, 'Artifact_Directory_Test_started', 'start_Artifact_Directory_Test', custom_context)
    link_tasks(graph, 'War_starting', 'War_configured', custom_context)
    link_tasks(graph, 'Artifact_Directory_Test_created', 'create_Artifact_Directory_Test', custom_context)
    link_tasks(graph, 'start_War', 'War_starting', custom_context)
    link_tasks(graph, 'Java_configuring', 'Java_created', custom_context)
    link_tasks(graph, 'configure_War', 'War_configuring', custom_context)
    link_tasks(graph, 'start_Tomcat', 'Tomcat_starting', custom_context)
    link_tasks(graph, 'War_configured', 'configure_War', custom_context)
    link_tasks(graph, 'create_Tomcat', 'Tomcat_creating', custom_context)
    link_tasks(graph, 'start_Java', 'Java_starting', custom_context)
    link_tasks(graph, 'Tomcat_configuring', 'Tomcat_created', custom_context)
    link_tasks(graph, 'Java_configuring', 'Tomcat_created', custom_context)
    link_tasks(graph, 'War_configuring', 'War_created', custom_context)
    link_tasks(graph, 'Java_started', 'start_Java', custom_context)
    link_tasks(graph, 'Tomcat_creating', 'Tomcat_initial', custom_context)
    link_tasks(graph, 'configure_Tomcat', 'Tomcat_configuring', custom_context)
    link_tasks(graph, 'Artifact_Directory_Test_configured', 'configure_Artifact_Directory_Test', custom_context)
    link_tasks(graph, 'War_initial', 'Tomcat_started', custom_context)
    link_tasks(graph, 'create_Artifact_Directory_Test', 'Artifact_Directory_Test_creating', custom_context)
    link_tasks(graph, 'Java_configured', 'configure_Java', custom_context)
    link_tasks(graph, 'Artifact_Directory_Test_configuring', 'Artifact_Directory_Test_created', custom_context)
    link_tasks(graph, 'Java_created', 'create_Java', custom_context)
    link_tasks(graph, 'War_created', 'create_War', custom_context)
    link_tasks(graph, 'start_Artifact_Directory_Test', 'Artifact_Directory_Test_starting', custom_context)
    link_tasks(graph, 'Tomcat_initial', 'Server_install', custom_context)
    link_tasks(graph, 'Artifact_Directory_Test_initial', 'Server_install', custom_context)
    link_tasks(graph, 'Java_initial', 'Server_install', custom_context)
    link_tasks(graph, 'create_War', 'War_creating', custom_context)
    link_tasks(graph, 'War_started', 'start_War', custom_context)
    link_tasks(graph, 'Java_starting', 'Java_configured', custom_context)
    link_tasks(graph, 'Tomcat_starting', 'Tomcat_configured', custom_context)
    link_tasks(graph, 'Tomcat_configured', 'configure_Tomcat', custom_context)
    link_tasks(graph, 'War_creating', 'War_initial', custom_context)
    link_tasks(graph, 'configure_Artifact_Directory_Test', 'Artifact_Directory_Test_configuring', custom_context)
    link_tasks(graph, 'Tomcat_started', 'start_Tomcat', custom_context)
    link_tasks(graph, 'configure_Java', 'Java_configuring', custom_context)
    link_tasks(graph, 'Java_creating', 'Java_initial', custom_context)
    link_tasks(graph, 'Artifact_Directory_Test_creating', 'Artifact_Directory_Test_initial', custom_context)


# subworkflow 'uninstall' for host 'Server'
def uninstall_host_server(ctx, graph, custom_context):
    custom_context.add_customized_wf_node('War')
    custom_context.add_customized_wf_node('Java')
    custom_context.add_customized_wf_node('Tomcat')
    custom_context.add_customized_wf_node('Artifact_Directory_Test')
    custom_context.add_customized_wf_node('War')
    custom_context.add_customized_wf_node('Artifact_Directory_Test')
    custom_context.add_customized_wf_node('Java')
    custom_context.add_customized_wf_node('Tomcat')
    custom_context.add_customized_wf_node('War')
    custom_context.add_customized_wf_node('Tomcat')
    custom_context.add_customized_wf_node('Tomcat')
    custom_context.add_customized_wf_node('Artifact_Directory_Test')
    custom_context.add_customized_wf_node('Java')
    custom_context.add_customized_wf_node('War')
    custom_context.add_customized_wf_node('Java')
    custom_context.add_customized_wf_node('Artifact_Directory_Test')
    set_state_task(ctx, graph, 'War', 'deleting', 'War_deleting', custom_context)
    set_state_task(ctx, graph, 'Java', 'stopping', 'Java_stopping', custom_context)
    set_state_task(ctx, graph, 'Tomcat', 'deleted', 'Tomcat_deleted', custom_context)
    set_state_task(ctx, graph, 'Artifact_Directory_Test', 'stopped', 'Artifact_Directory_Test_stopped', custom_context)
    set_state_task(ctx, graph, 'War', 'stopping', 'War_stopping', custom_context)
    operation_task(ctx, graph, 'Tomcat', 'cloudify.interfaces.lifecycle.stop', 'stop_Tomcat', custom_context)
    set_state_task(ctx, graph, 'Artifact_Directory_Test', 'stopping', 'Artifact_Directory_Test_stopping', custom_context)
    set_state_task(ctx, graph, 'Java', 'deleting', 'Java_deleting', custom_context)
    set_state_task(ctx, graph, 'Tomcat', 'stopped', 'Tomcat_stopped', custom_context)
    custom_context.register_native_delegate_wf_step('Server', 'Server_uninstall')
    set_state_task(ctx, graph, 'War', 'stopped', 'War_stopped', custom_context)
    set_state_task(ctx, graph, 'Tomcat', 'stopping', 'Tomcat_stopping', custom_context)
    set_state_task(ctx, graph, 'Tomcat', 'deleting', 'Tomcat_deleting', custom_context)
    set_state_task(ctx, graph, 'Artifact_Directory_Test', 'deleting', 'Artifact_Directory_Test_deleting', custom_context)
    set_state_task(ctx, graph, 'Java', 'stopped', 'Java_stopped', custom_context)
    set_state_task(ctx, graph, 'War', 'deleted', 'War_deleted', custom_context)
    set_state_task(ctx, graph, 'Java', 'deleted', 'Java_deleted', custom_context)
    set_state_task(ctx, graph, 'Artifact_Directory_Test', 'deleted', 'Artifact_Directory_Test_deleted', custom_context)
    generate_native_node_workflows(ctx, graph, custom_context, 'uninstall')
    link_tasks(graph, 'War_deleted', 'War_deleting', custom_context)
    link_tasks(graph, 'Java_stopped', 'Java_stopping', custom_context)
    link_tasks(graph, 'Server_uninstall', 'Tomcat_deleted', custom_context)
    link_tasks(graph, 'Artifact_Directory_Test_deleting', 'Artifact_Directory_Test_stopped', custom_context)
    link_tasks(graph, 'War_stopped', 'War_stopping', custom_context)
    link_tasks(graph, 'Tomcat_stopped', 'stop_Tomcat', custom_context)
    link_tasks(graph, 'Artifact_Directory_Test_stopped', 'Artifact_Directory_Test_stopping', custom_context)
    link_tasks(graph, 'Java_deleted', 'Java_deleting', custom_context)
    link_tasks(graph, 'Tomcat_deleting', 'Tomcat_stopped', custom_context)
    link_tasks(graph, 'War_deleting', 'War_stopped', custom_context)
    link_tasks(graph, 'stop_Tomcat', 'Tomcat_stopping', custom_context)
    link_tasks(graph, 'Tomcat_deleted', 'Tomcat_deleting', custom_context)
    link_tasks(graph, 'Artifact_Directory_Test_deleted', 'Artifact_Directory_Test_deleting', custom_context)
    link_tasks(graph, 'Java_deleting', 'Java_stopped', custom_context)
    link_tasks(graph, 'Tomcat_stopping', 'War_deleted', custom_context)
    link_tasks(graph, 'Server_uninstall', 'Java_deleted', custom_context)
    link_tasks(graph, 'Server_uninstall', 'Artifact_Directory_Test_deleted', custom_context)


def install_host(ctx, graph, custom_context, compute):
    options = {}
    options['Server'] = install_host_server
    options[compute](ctx, graph, custom_context)


def uninstall_host(ctx, graph, custom_context, compute):
    options = {}
    options['Server'] = uninstall_host_server
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
    custom_context.add_customized_wf_node('Java')
    custom_context.add_customized_wf_node('Java')
    custom_context.add_customized_wf_node('Artifact_Directory_Test')
    custom_context.add_customized_wf_node('War')
    custom_context.add_customized_wf_node('War')
    custom_context.add_customized_wf_node('War')
    custom_context.add_customized_wf_node('Java')
    custom_context.add_customized_wf_node('War')
    custom_context.add_customized_wf_node('Tomcat')
    custom_context.add_customized_wf_node('Tomcat')
    custom_context.add_customized_wf_node('Java')
    custom_context.add_customized_wf_node('Tomcat')
    custom_context.add_customized_wf_node('War')
    custom_context.add_customized_wf_node('Tomcat')
    custom_context.add_customized_wf_node('Tomcat')
    custom_context.add_customized_wf_node('Tomcat')
    custom_context.add_customized_wf_node('Artifact_Directory_Test')
    custom_context.add_customized_wf_node('Artifact_Directory_Test')
    custom_context.add_customized_wf_node('Artifact_Directory_Test')
    custom_context.add_customized_wf_node('Artifact_Directory_Test')
    custom_context.add_customized_wf_node('War')
    custom_context.add_customized_wf_node('Java')
    custom_context.add_customized_wf_node('Tomcat')
    custom_context.add_customized_wf_node('War')
    custom_context.add_customized_wf_node('Artifact_Directory_Test')
    custom_context.add_customized_wf_node('Java')
    custom_context.add_customized_wf_node('Java')
    custom_context.add_customized_wf_node('Artifact_Directory_Test')
    set_state_task(ctx, graph, 'Java', 'started', 'Java_started', custom_context)
    set_state_task(ctx, graph, 'Java', 'creating', 'Java_creating', custom_context)
    operation_task(ctx, graph, 'Tomcat', 'cloudify.interfaces.lifecycle.create', 'create_Tomcat', custom_context)
    set_state_task(ctx, graph, 'Artifact_Directory_Test', 'configured', 'Artifact_Directory_Test_configured', custom_context)
    operation_task(ctx, graph, 'Artifact_Directory_Test', 'cloudify.interfaces.lifecycle.start', 'start_Artifact_Directory_Test', custom_context)
    set_state_task(ctx, graph, 'War', 'configured', 'War_configured', custom_context)
    operation_task(ctx, graph, 'Artifact_Directory_Test', 'cloudify.interfaces.lifecycle.create', 'create_Artifact_Directory_Test', custom_context)
    set_state_task(ctx, graph, 'War', 'starting', 'War_starting', custom_context)
    set_state_task(ctx, graph, 'War', 'started', 'War_started', custom_context)
    set_state_task(ctx, graph, 'Java', 'created', 'Java_created', custom_context)
    set_state_task(ctx, graph, 'War', 'configuring', 'War_configuring', custom_context)
    set_state_task(ctx, graph, 'Tomcat', 'starting', 'Tomcat_starting', custom_context)
    operation_task(ctx, graph, 'War', 'cloudify.interfaces.lifecycle.configure', 'configure_War', custom_context)
    set_state_task(ctx, graph, 'Tomcat', 'creating', 'Tomcat_creating', custom_context)
    set_state_task(ctx, graph, 'Java', 'starting', 'Java_starting', custom_context)
    set_state_task(ctx, graph, 'Tomcat', 'created', 'Tomcat_created', custom_context)
    set_state_task(ctx, graph, 'War', 'created', 'War_created', custom_context)
    operation_task(ctx, graph, 'Java', 'cloudify.interfaces.lifecycle.start', 'start_Java', custom_context)
    set_state_task(ctx, graph, 'Tomcat', 'initial', 'Tomcat_initial', custom_context)
    set_state_task(ctx, graph, 'Tomcat', 'configuring', 'Tomcat_configuring', custom_context)
    operation_task(ctx, graph, 'Artifact_Directory_Test', 'cloudify.interfaces.lifecycle.configure', 'configure_Artifact_Directory_Test', custom_context)
    set_state_task(ctx, graph, 'Tomcat', 'started', 'Tomcat_started', custom_context)
    custom_context.register_native_delegate_wf_step('NetPub', 'NetPub_install')
    set_state_task(ctx, graph, 'Artifact_Directory_Test', 'creating', 'Artifact_Directory_Test_creating', custom_context)
    operation_task(ctx, graph, 'Java', 'cloudify.interfaces.lifecycle.configure', 'configure_Java', custom_context)
    set_state_task(ctx, graph, 'Artifact_Directory_Test', 'created', 'Artifact_Directory_Test_created', custom_context)
    operation_task(ctx, graph, 'Java', 'cloudify.interfaces.lifecycle.create', 'create_Java', custom_context)
    operation_task(ctx, graph, 'War', 'cloudify.interfaces.lifecycle.create', 'create_War', custom_context)
    set_state_task(ctx, graph, 'Artifact_Directory_Test', 'started', 'Artifact_Directory_Test_started', custom_context)
    set_state_task(ctx, graph, 'Artifact_Directory_Test', 'starting', 'Artifact_Directory_Test_starting', custom_context)
    custom_context.register_native_delegate_wf_step('Server', 'Server_install')
    set_state_task(ctx, graph, 'War', 'creating', 'War_creating', custom_context)
    operation_task(ctx, graph, 'War', 'cloudify.interfaces.lifecycle.start', 'start_War', custom_context)
    set_state_task(ctx, graph, 'Java', 'configured', 'Java_configured', custom_context)
    set_state_task(ctx, graph, 'Tomcat', 'configured', 'Tomcat_configured', custom_context)
    operation_task(ctx, graph, 'Tomcat', 'cloudify.interfaces.lifecycle.configure', 'configure_Tomcat', custom_context)
    set_state_task(ctx, graph, 'War', 'initial', 'War_initial', custom_context)
    set_state_task(ctx, graph, 'Artifact_Directory_Test', 'configuring', 'Artifact_Directory_Test_configuring', custom_context)
    operation_task(ctx, graph, 'Tomcat', 'cloudify.interfaces.lifecycle.start', 'start_Tomcat', custom_context)
    set_state_task(ctx, graph, 'Java', 'configuring', 'Java_configuring', custom_context)
    set_state_task(ctx, graph, 'Java', 'initial', 'Java_initial', custom_context)
    set_state_task(ctx, graph, 'Artifact_Directory_Test', 'initial', 'Artifact_Directory_Test_initial', custom_context)
    generate_native_node_workflows(ctx, graph, custom_context, 'install')
    link_tasks(graph, 'Java_started', 'start_Java', custom_context)
    link_tasks(graph, 'Java_creating', 'Java_initial', custom_context)
    link_tasks(graph, 'create_Tomcat', 'Tomcat_creating', custom_context)
    link_tasks(graph, 'Artifact_Directory_Test_configured', 'configure_Artifact_Directory_Test', custom_context)
    link_tasks(graph, 'start_Artifact_Directory_Test', 'Artifact_Directory_Test_starting', custom_context)
    link_tasks(graph, 'War_configured', 'configure_War', custom_context)
    link_tasks(graph, 'create_Artifact_Directory_Test', 'Artifact_Directory_Test_creating', custom_context)
    link_tasks(graph, 'War_starting', 'War_configured', custom_context)
    link_tasks(graph, 'War_started', 'start_War', custom_context)
    link_tasks(graph, 'Java_created', 'create_Java', custom_context)
    link_tasks(graph, 'War_configuring', 'War_created', custom_context)
    link_tasks(graph, 'Tomcat_starting', 'Tomcat_configured', custom_context)
    link_tasks(graph, 'configure_War', 'War_configuring', custom_context)
    link_tasks(graph, 'Tomcat_creating', 'Tomcat_initial', custom_context)
    link_tasks(graph, 'Java_starting', 'Java_configured', custom_context)
    link_tasks(graph, 'Tomcat_created', 'create_Tomcat', custom_context)
    link_tasks(graph, 'War_created', 'create_War', custom_context)
    link_tasks(graph, 'start_Java', 'Java_starting', custom_context)
    link_tasks(graph, 'Tomcat_initial', 'Server_install', custom_context)
    link_tasks(graph, 'Tomcat_configuring', 'Java_started', custom_context)
    link_tasks(graph, 'Tomcat_configuring', 'Tomcat_created', custom_context)
    link_tasks(graph, 'configure_Artifact_Directory_Test', 'Artifact_Directory_Test_configuring', custom_context)
    link_tasks(graph, 'Tomcat_started', 'start_Tomcat', custom_context)
    link_tasks(graph, 'Artifact_Directory_Test_creating', 'Artifact_Directory_Test_initial', custom_context)
    link_tasks(graph, 'configure_Java', 'Java_configuring', custom_context)
    link_tasks(graph, 'Artifact_Directory_Test_created', 'create_Artifact_Directory_Test', custom_context)
    link_tasks(graph, 'create_Java', 'Java_creating', custom_context)
    link_tasks(graph, 'create_War', 'War_creating', custom_context)
    link_tasks(graph, 'Artifact_Directory_Test_started', 'start_Artifact_Directory_Test', custom_context)
    link_tasks(graph, 'Artifact_Directory_Test_starting', 'Artifact_Directory_Test_configured', custom_context)
    link_tasks(graph, 'War_creating', 'War_initial', custom_context)
    link_tasks(graph, 'start_War', 'War_starting', custom_context)
    link_tasks(graph, 'Java_configured', 'configure_Java', custom_context)
    link_tasks(graph, 'Tomcat_configured', 'configure_Tomcat', custom_context)
    link_tasks(graph, 'configure_Tomcat', 'Tomcat_configuring', custom_context)
    link_tasks(graph, 'War_initial', 'Tomcat_started', custom_context)
    link_tasks(graph, 'Artifact_Directory_Test_configuring', 'Artifact_Directory_Test_created', custom_context)
    link_tasks(graph, 'start_Tomcat', 'Tomcat_starting', custom_context)
    link_tasks(graph, 'Java_configuring', 'Tomcat_created', custom_context)
    link_tasks(graph, 'Java_configuring', 'Java_created', custom_context)
    link_tasks(graph, 'Java_initial', 'Server_install', custom_context)
    link_tasks(graph, 'Artifact_Directory_Test_initial', 'Server_install', custom_context)
def _a4c_uninstall(ctx, graph, custom_context):
    #  following code can be pasted in src/test/python/workflows/tasks.py for simulation
    custom_context.add_customized_wf_node('War')
    custom_context.add_customized_wf_node('Java')
    custom_context.add_customized_wf_node('Tomcat')
    custom_context.add_customized_wf_node('Artifact_Directory_Test')
    custom_context.add_customized_wf_node('War')
    custom_context.add_customized_wf_node('Artifact_Directory_Test')
    custom_context.add_customized_wf_node('Java')
    custom_context.add_customized_wf_node('Tomcat')
    custom_context.add_customized_wf_node('War')
    custom_context.add_customized_wf_node('Tomcat')
    custom_context.add_customized_wf_node('Tomcat')
    custom_context.add_customized_wf_node('Artifact_Directory_Test')
    custom_context.add_customized_wf_node('Java')
    custom_context.add_customized_wf_node('War')
    custom_context.add_customized_wf_node('Java')
    custom_context.add_customized_wf_node('Artifact_Directory_Test')
    set_state_task(ctx, graph, 'War', 'deleting', 'War_deleting', custom_context)
    set_state_task(ctx, graph, 'Java', 'stopping', 'Java_stopping', custom_context)
    set_state_task(ctx, graph, 'Tomcat', 'deleted', 'Tomcat_deleted', custom_context)
    set_state_task(ctx, graph, 'Artifact_Directory_Test', 'stopped', 'Artifact_Directory_Test_stopped', custom_context)
    set_state_task(ctx, graph, 'War', 'stopping', 'War_stopping', custom_context)
    operation_task(ctx, graph, 'Tomcat', 'cloudify.interfaces.lifecycle.stop', 'stop_Tomcat', custom_context)
    set_state_task(ctx, graph, 'Artifact_Directory_Test', 'stopping', 'Artifact_Directory_Test_stopping', custom_context)
    custom_context.register_native_delegate_wf_step('NetPub', 'NetPub_uninstall')
    set_state_task(ctx, graph, 'Java', 'deleting', 'Java_deleting', custom_context)
    set_state_task(ctx, graph, 'Tomcat', 'stopped', 'Tomcat_stopped', custom_context)
    custom_context.register_native_delegate_wf_step('Server', 'Server_uninstall')
    set_state_task(ctx, graph, 'War', 'stopped', 'War_stopped', custom_context)
    set_state_task(ctx, graph, 'Tomcat', 'stopping', 'Tomcat_stopping', custom_context)
    set_state_task(ctx, graph, 'Tomcat', 'deleting', 'Tomcat_deleting', custom_context)
    set_state_task(ctx, graph, 'Artifact_Directory_Test', 'deleting', 'Artifact_Directory_Test_deleting', custom_context)
    set_state_task(ctx, graph, 'Java', 'stopped', 'Java_stopped', custom_context)
    set_state_task(ctx, graph, 'War', 'deleted', 'War_deleted', custom_context)
    set_state_task(ctx, graph, 'Java', 'deleted', 'Java_deleted', custom_context)
    set_state_task(ctx, graph, 'Artifact_Directory_Test', 'deleted', 'Artifact_Directory_Test_deleted', custom_context)
    generate_native_node_workflows(ctx, graph, custom_context, 'uninstall')
    link_tasks(graph, 'War_deleting', 'War_stopped', custom_context)
    link_tasks(graph, 'Tomcat_deleted', 'Tomcat_deleting', custom_context)
    link_tasks(graph, 'Artifact_Directory_Test_stopped', 'Artifact_Directory_Test_stopping', custom_context)
    link_tasks(graph, 'stop_Tomcat', 'Tomcat_stopping', custom_context)
    link_tasks(graph, 'Java_deleting', 'Java_stopped', custom_context)
    link_tasks(graph, 'Tomcat_stopped', 'stop_Tomcat', custom_context)
    link_tasks(graph, 'Server_uninstall', 'Tomcat_deleted', custom_context)
    link_tasks(graph, 'Server_uninstall', 'Java_deleted', custom_context)
    link_tasks(graph, 'Server_uninstall', 'Artifact_Directory_Test_deleted', custom_context)
    link_tasks(graph, 'War_stopped', 'War_stopping', custom_context)
    link_tasks(graph, 'Tomcat_stopping', 'War_deleted', custom_context)
    link_tasks(graph, 'Tomcat_deleting', 'Tomcat_stopped', custom_context)
    link_tasks(graph, 'Artifact_Directory_Test_deleting', 'Artifact_Directory_Test_stopped', custom_context)
    link_tasks(graph, 'Java_stopped', 'Java_stopping', custom_context)
    link_tasks(graph, 'War_deleted', 'War_deleting', custom_context)
    link_tasks(graph, 'Java_deleted', 'Java_deleting', custom_context)
    link_tasks(graph, 'Artifact_Directory_Test_deleted', 'Artifact_Directory_Test_deleting', custom_context)

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
    #types.append('alien.nodes.Java')
    #types.append('tosca.nodes.SoftwareComponent')
    #types.append('tosca.nodes.Root')
    #node_Java = _build_node(ctx, 'Java', types, 1)
    #types = []
    #types.append('alien.nodes.TestArtifactDirectory')
    #types.append('tosca.nodes.SoftwareComponent')
    #types.append('tosca.nodes.Root')
    #node_Artifact_Directory_Test = _build_node(ctx, 'Artifact_Directory_Test', types, 1)
    #types = []
    #types.append('alien.nodes.Tomcat')
    #types.append('tosca.nodes.WebServer')
    #types.append('tosca.nodes.SoftwareComponent')
    #types.append('tosca.nodes.Root')
    #node_Tomcat = _build_node(ctx, 'Tomcat', types, 1)
    #types = []
    #types.append('alien.cloudify.aws.nodes.Compute')
    #types.append('tosca.nodes.Compute')
    #types.append('tosca.nodes.Root')
    #node_Server = _build_node(ctx, 'Server', types, 1)
    #types = []
    #types.append('alien.nodes.WarWithLifeCycleScript')
    #types.append('alien.nodes.War')
    #types.append('alien.nodes.LoadBalancedWebApplication')
    #types.append('tosca.nodes.Root')
    #node_War = _build_node(ctx, 'War', types, 1)
    #types = []
    #types.append('alien.nodes.aws.PublicNetwork')
    #types.append('alien.nodes.PublicNetwork')
    #types.append('tosca.nodes.Network')
    #types.append('tosca.nodes.Root')
    #node_NetPub = _build_node(ctx, 'NetPub', types, 1)
    #_add_relationship(node_Java, node_Server)
    #_add_relationship(node_Artifact_Directory_Test, node_Server)
    #_add_relationship(node_Tomcat, node_Server)
    #_add_relationship(node_Tomcat, node_Java)
    #_add_relationship(node_Server, node_NetPub)
    #_add_relationship(node_War, node_Tomcat)