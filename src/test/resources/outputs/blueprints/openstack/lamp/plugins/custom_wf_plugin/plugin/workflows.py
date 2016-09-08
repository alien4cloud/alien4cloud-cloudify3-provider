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


# subworkflow 'install' for host 'DataBase'
def install_host_database(ctx, graph, custom_context):
    custom_context.add_customized_wf_node('Mysql')
    custom_context.add_customized_wf_node('Mysql')
    custom_context.add_customized_wf_node('Mysql')
    custom_context.add_customized_wf_node('Mysql')
    custom_context.add_customized_wf_node('Mysql')
    custom_context.add_customized_wf_node('Mysql')
    custom_context.add_customized_wf_node('Mysql')
    set_state_task(ctx, graph, 'Mysql', 'configuring', 'Mysql_configuring', custom_context)
    operation_task(ctx, graph, 'Mysql', 'cloudify.interfaces.lifecycle.create', 'create_Mysql', custom_context)
    operation_task(ctx, graph, 'Mysql', 'cloudify.interfaces.lifecycle.configure', 'configure_Mysql', custom_context)
    set_state_task(ctx, graph, 'Mysql', 'created', 'Mysql_created', custom_context)
    custom_context.register_native_delegate_wf_step('DataBase', 'DataBase_install')
    set_state_task(ctx, graph, 'Mysql', 'creating', 'Mysql_creating', custom_context)
    set_state_task(ctx, graph, 'Mysql', 'starting', 'Mysql_starting', custom_context)
    set_state_task(ctx, graph, 'Mysql', 'configured', 'Mysql_configured', custom_context)
    set_state_task(ctx, graph, 'Mysql', 'started', 'Mysql_started', custom_context)
    set_state_task(ctx, graph, 'Mysql', 'initial', 'Mysql_initial', custom_context)
    operation_task(ctx, graph, 'Mysql', 'cloudify.interfaces.lifecycle.start', 'start_Mysql', custom_context)
    generate_native_node_workflows(ctx, graph, custom_context, 'install')
    link_tasks(graph, 'configure_Mysql', 'Mysql_configuring', custom_context)
    link_tasks(graph, 'Mysql_created', 'create_Mysql', custom_context)
    link_tasks(graph, 'Mysql_configured', 'configure_Mysql', custom_context)
    link_tasks(graph, 'Mysql_configuring', 'Mysql_created', custom_context)
    link_tasks(graph, 'Mysql_initial', 'DataBase_install', custom_context)
    link_tasks(graph, 'create_Mysql', 'Mysql_creating', custom_context)
    link_tasks(graph, 'start_Mysql', 'Mysql_starting', custom_context)
    link_tasks(graph, 'Mysql_starting', 'Mysql_configured', custom_context)
    link_tasks(graph, 'Mysql_creating', 'Mysql_initial', custom_context)
    link_tasks(graph, 'Mysql_started', 'start_Mysql', custom_context)


# subworkflow 'install' for host 'Server'
def install_host_server(ctx, graph, custom_context):
    custom_context.add_customized_wf_node('PHP')
    custom_context.add_customized_wf_node('Wordpress')
    custom_context.add_customized_wf_node('Apache')
    custom_context.add_customized_wf_node('Wordpress')
    custom_context.add_customized_wf_node('Wordpress')
    custom_context.add_customized_wf_node('Apache')
    custom_context.add_customized_wf_node('PHP')
    custom_context.add_customized_wf_node('PHP')
    custom_context.add_customized_wf_node('Wordpress')
    custom_context.add_customized_wf_node('Apache')
    custom_context.add_customized_wf_node('PHP')
    custom_context.add_customized_wf_node('Wordpress')
    custom_context.add_customized_wf_node('PHP')
    custom_context.add_customized_wf_node('Apache')
    custom_context.add_customized_wf_node('Wordpress')
    custom_context.add_customized_wf_node('Apache')
    custom_context.add_customized_wf_node('Apache')
    custom_context.add_customized_wf_node('Wordpress')
    custom_context.add_customized_wf_node('Apache')
    custom_context.add_customized_wf_node('PHP')
    custom_context.add_customized_wf_node('PHP')
    set_state_task(ctx, graph, 'PHP', 'starting', 'PHP_starting', custom_context)
    set_state_task(ctx, graph, 'Wordpress', 'started', 'Wordpress_started', custom_context)
    set_state_task(ctx, graph, 'Apache', 'initial', 'Apache_initial', custom_context)
    set_state_task(ctx, graph, 'Wordpress', 'configured', 'Wordpress_configured', custom_context)
    set_state_task(ctx, graph, 'Wordpress', 'starting', 'Wordpress_starting', custom_context)
    operation_task(ctx, graph, 'PHP', 'cloudify.interfaces.lifecycle.start', 'start_PHP', custom_context)
    set_state_task(ctx, graph, 'Apache', 'starting', 'Apache_starting', custom_context)
    set_state_task(ctx, graph, 'PHP', 'configuring', 'PHP_configuring', custom_context)
    custom_context.register_native_delegate_wf_step('Server', 'Server_install')
    set_state_task(ctx, graph, 'PHP', 'created', 'PHP_created', custom_context)
    operation_task(ctx, graph, 'Wordpress', 'cloudify.interfaces.lifecycle.start', 'start_Wordpress', custom_context)
    operation_task(ctx, graph, 'Apache', 'cloudify.interfaces.lifecycle.configure', 'configure_Apache', custom_context)
    operation_task(ctx, graph, 'Apache', 'cloudify.interfaces.lifecycle.create', 'create_Apache', custom_context)
    set_state_task(ctx, graph, 'Wordpress', 'initial', 'Wordpress_initial', custom_context)
    set_state_task(ctx, graph, 'Apache', 'configured', 'Apache_configured', custom_context)
    set_state_task(ctx, graph, 'PHP', 'started', 'PHP_started', custom_context)
    set_state_task(ctx, graph, 'Wordpress', 'configuring', 'Wordpress_configuring', custom_context)
    set_state_task(ctx, graph, 'PHP', 'creating', 'PHP_creating', custom_context)
    operation_task(ctx, graph, 'Wordpress', 'cloudify.interfaces.lifecycle.configure', 'configure_Wordpress', custom_context)
    set_state_task(ctx, graph, 'Apache', 'creating', 'Apache_creating', custom_context)
    operation_task(ctx, graph, 'PHP', 'cloudify.interfaces.lifecycle.create', 'create_PHP', custom_context)
    operation_task(ctx, graph, 'Apache', 'cloudify.interfaces.lifecycle.start', 'start_Apache', custom_context)
    operation_task(ctx, graph, 'PHP', 'cloudify.interfaces.lifecycle.configure', 'configure_PHP', custom_context)
    operation_task(ctx, graph, 'Wordpress', 'cloudify.interfaces.lifecycle.create', 'create_Wordpress', custom_context)
    set_state_task(ctx, graph, 'Wordpress', 'creating', 'Wordpress_creating', custom_context)
    set_state_task(ctx, graph, 'Apache', 'created', 'Apache_created', custom_context)
    set_state_task(ctx, graph, 'Apache', 'started', 'Apache_started', custom_context)
    set_state_task(ctx, graph, 'Wordpress', 'created', 'Wordpress_created', custom_context)
    set_state_task(ctx, graph, 'Apache', 'configuring', 'Apache_configuring', custom_context)
    set_state_task(ctx, graph, 'PHP', 'initial', 'PHP_initial', custom_context)
    set_state_task(ctx, graph, 'PHP', 'configured', 'PHP_configured', custom_context)
    generate_native_node_workflows(ctx, graph, custom_context, 'install')
    link_tasks(graph, 'start_PHP', 'PHP_starting', custom_context)
    link_tasks(graph, 'Apache_creating', 'Apache_initial', custom_context)
    link_tasks(graph, 'Wordpress_starting', 'Wordpress_configured', custom_context)
    link_tasks(graph, 'start_Wordpress', 'Wordpress_starting', custom_context)
    link_tasks(graph, 'PHP_started', 'start_PHP', custom_context)
    link_tasks(graph, 'start_Apache', 'Apache_starting', custom_context)
    link_tasks(graph, 'configure_PHP', 'PHP_configuring', custom_context)
    link_tasks(graph, 'Apache_initial', 'Server_install', custom_context)
    link_tasks(graph, 'PHP_initial', 'Server_install', custom_context)
    link_tasks(graph, 'PHP_configuring', 'PHP_created', custom_context)
    link_tasks(graph, 'Wordpress_started', 'start_Wordpress', custom_context)
    link_tasks(graph, 'Apache_configured', 'configure_Apache', custom_context)
    link_tasks(graph, 'Apache_created', 'create_Apache', custom_context)
    link_tasks(graph, 'Wordpress_creating', 'Wordpress_initial', custom_context)
    link_tasks(graph, 'Apache_starting', 'Apache_configured', custom_context)
    link_tasks(graph, 'Wordpress_configuring', 'PHP_started', custom_context)
    link_tasks(graph, 'configure_Wordpress', 'Wordpress_configuring', custom_context)
    link_tasks(graph, 'create_PHP', 'PHP_creating', custom_context)
    link_tasks(graph, 'Wordpress_configured', 'configure_Wordpress', custom_context)
    link_tasks(graph, 'create_Apache', 'Apache_creating', custom_context)
    link_tasks(graph, 'PHP_created', 'create_PHP', custom_context)
    link_tasks(graph, 'Apache_started', 'start_Apache', custom_context)
    link_tasks(graph, 'PHP_configured', 'configure_PHP', custom_context)
    link_tasks(graph, 'Wordpress_created', 'create_Wordpress', custom_context)
    link_tasks(graph, 'create_Wordpress', 'Wordpress_creating', custom_context)
    link_tasks(graph, 'Apache_configuring', 'Apache_created', custom_context)
    link_tasks(graph, 'Wordpress_initial', 'Apache_started', custom_context)
    link_tasks(graph, 'PHP_configuring', 'Wordpress_created', custom_context)
    link_tasks(graph, 'Wordpress_configuring', 'Wordpress_created', custom_context)
    link_tasks(graph, 'configure_Apache', 'Apache_configuring', custom_context)
    link_tasks(graph, 'PHP_creating', 'PHP_initial', custom_context)
    link_tasks(graph, 'PHP_starting', 'PHP_configured', custom_context)


# subworkflow 'uninstall' for host 'DataBase'
def uninstall_host_database(ctx, graph, custom_context):
    custom_context.add_customized_wf_node('Mysql')
    custom_context.add_customized_wf_node('Mysql')
    custom_context.add_customized_wf_node('Mysql')
    custom_context.add_customized_wf_node('Mysql')
    set_state_task(ctx, graph, 'Mysql', 'deleted', 'Mysql_deleted', custom_context)
    set_state_task(ctx, graph, 'Mysql', 'deleting', 'Mysql_deleting', custom_context)
    custom_context.register_native_delegate_wf_step('DataBase', 'DataBase_uninstall')
    set_state_task(ctx, graph, 'Mysql', 'stopped', 'Mysql_stopped', custom_context)
    set_state_task(ctx, graph, 'Mysql', 'stopping', 'Mysql_stopping', custom_context)
    generate_native_node_workflows(ctx, graph, custom_context, 'uninstall')
    link_tasks(graph, 'DataBase_uninstall', 'Mysql_deleted', custom_context)
    link_tasks(graph, 'Mysql_deleted', 'Mysql_deleting', custom_context)
    link_tasks(graph, 'Mysql_deleting', 'Mysql_stopped', custom_context)
    link_tasks(graph, 'Mysql_stopped', 'Mysql_stopping', custom_context)


# subworkflow 'uninstall' for host 'Server'
def uninstall_host_server(ctx, graph, custom_context):
    custom_context.add_customized_wf_node('PHP')
    custom_context.add_customized_wf_node('Wordpress')
    custom_context.add_customized_wf_node('Apache')
    custom_context.add_customized_wf_node('Apache')
    custom_context.add_customized_wf_node('Wordpress')
    custom_context.add_customized_wf_node('Apache')
    custom_context.add_customized_wf_node('Wordpress')
    custom_context.add_customized_wf_node('Apache')
    custom_context.add_customized_wf_node('PHP')
    custom_context.add_customized_wf_node('PHP')
    custom_context.add_customized_wf_node('Wordpress')
    custom_context.add_customized_wf_node('PHP')
    set_state_task(ctx, graph, 'PHP', 'stopping', 'PHP_stopping', custom_context)
    set_state_task(ctx, graph, 'Wordpress', 'deleting', 'Wordpress_deleting', custom_context)
    set_state_task(ctx, graph, 'Apache', 'stopped', 'Apache_stopped', custom_context)
    set_state_task(ctx, graph, 'Apache', 'deleted', 'Apache_deleted', custom_context)
    set_state_task(ctx, graph, 'Wordpress', 'deleted', 'Wordpress_deleted', custom_context)
    set_state_task(ctx, graph, 'Apache', 'deleting', 'Apache_deleting', custom_context)
    set_state_task(ctx, graph, 'Wordpress', 'stopped', 'Wordpress_stopped', custom_context)
    set_state_task(ctx, graph, 'Apache', 'stopping', 'Apache_stopping', custom_context)
    set_state_task(ctx, graph, 'PHP', 'deleting', 'PHP_deleting', custom_context)
    custom_context.register_native_delegate_wf_step('Server', 'Server_uninstall')
    set_state_task(ctx, graph, 'PHP', 'stopped', 'PHP_stopped', custom_context)
    set_state_task(ctx, graph, 'Wordpress', 'stopping', 'Wordpress_stopping', custom_context)
    set_state_task(ctx, graph, 'PHP', 'deleted', 'PHP_deleted', custom_context)
    generate_native_node_workflows(ctx, graph, custom_context, 'uninstall')
    link_tasks(graph, 'PHP_stopped', 'PHP_stopping', custom_context)
    link_tasks(graph, 'Wordpress_deleted', 'Wordpress_deleting', custom_context)
    link_tasks(graph, 'Apache_deleting', 'Apache_stopped', custom_context)
    link_tasks(graph, 'Server_uninstall', 'Apache_deleted', custom_context)
    link_tasks(graph, 'Apache_stopping', 'Wordpress_deleted', custom_context)
    link_tasks(graph, 'Apache_deleted', 'Apache_deleting', custom_context)
    link_tasks(graph, 'Wordpress_deleting', 'Wordpress_stopped', custom_context)
    link_tasks(graph, 'Apache_stopped', 'Apache_stopping', custom_context)
    link_tasks(graph, 'PHP_deleted', 'PHP_deleting', custom_context)
    link_tasks(graph, 'PHP_deleting', 'PHP_stopped', custom_context)
    link_tasks(graph, 'Wordpress_stopped', 'Wordpress_stopping', custom_context)
    link_tasks(graph, 'Server_uninstall', 'PHP_deleted', custom_context)


def install_host(ctx, graph, custom_context, compute):
    options = {}
    options['DataBase'] = install_host_database
    options['Server'] = install_host_server
    options[compute](ctx, graph, custom_context)


def uninstall_host(ctx, graph, custom_context, compute):
    options = {}
    options['DataBase'] = uninstall_host_database
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
    custom_context.add_customized_wf_node('PHP')
    custom_context.add_customized_wf_node('Wordpress')
    custom_context.add_customized_wf_node('Mysql')
    custom_context.add_customized_wf_node('Apache')
    custom_context.add_customized_wf_node('Wordpress')
    custom_context.add_customized_wf_node('Wordpress')
    custom_context.add_customized_wf_node('Mysql')
    custom_context.add_customized_wf_node('Apache')
    custom_context.add_customized_wf_node('PHP')
    custom_context.add_customized_wf_node('PHP')
    custom_context.add_customized_wf_node('Mysql')
    custom_context.add_customized_wf_node('Wordpress')
    custom_context.add_customized_wf_node('Apache')
    custom_context.add_customized_wf_node('PHP')
    custom_context.add_customized_wf_node('Wordpress')
    custom_context.add_customized_wf_node('PHP')
    custom_context.add_customized_wf_node('Mysql')
    custom_context.add_customized_wf_node('Mysql')
    custom_context.add_customized_wf_node('Apache')
    custom_context.add_customized_wf_node('Mysql')
    custom_context.add_customized_wf_node('Wordpress')
    custom_context.add_customized_wf_node('Mysql')
    custom_context.add_customized_wf_node('Apache')
    custom_context.add_customized_wf_node('Apache')
    custom_context.add_customized_wf_node('Wordpress')
    custom_context.add_customized_wf_node('Apache')
    custom_context.add_customized_wf_node('PHP')
    custom_context.add_customized_wf_node('PHP')
    set_state_task(ctx, graph, 'PHP', 'starting', 'PHP_starting', custom_context)
    set_state_task(ctx, graph, 'Wordpress', 'started', 'Wordpress_started', custom_context)
    set_state_task(ctx, graph, 'Mysql', 'configuring', 'Mysql_configuring', custom_context)
    operation_task(ctx, graph, 'Mysql', 'cloudify.interfaces.lifecycle.create', 'create_Mysql', custom_context)
    set_state_task(ctx, graph, 'Apache', 'initial', 'Apache_initial', custom_context)
    operation_task(ctx, graph, 'Mysql', 'cloudify.interfaces.lifecycle.configure', 'configure_Mysql', custom_context)
    set_state_task(ctx, graph, 'Wordpress', 'configured', 'Wordpress_configured', custom_context)
    set_state_task(ctx, graph, 'Wordpress', 'starting', 'Wordpress_starting', custom_context)
    set_state_task(ctx, graph, 'Mysql', 'created', 'Mysql_created', custom_context)
    custom_context.register_native_delegate_wf_step('DataBase', 'DataBase_install')
    operation_task(ctx, graph, 'PHP', 'cloudify.interfaces.lifecycle.start', 'start_PHP', custom_context)
    set_state_task(ctx, graph, 'Apache', 'starting', 'Apache_starting', custom_context)
    set_state_task(ctx, graph, 'PHP', 'configuring', 'PHP_configuring', custom_context)
    custom_context.register_native_delegate_wf_step('Server', 'Server_install')
    set_state_task(ctx, graph, 'PHP', 'created', 'PHP_created', custom_context)
    operation_task(ctx, graph, 'Wordpress', 'cloudify.interfaces.lifecycle.start', 'start_Wordpress', custom_context)
    operation_task(ctx, graph, 'Apache', 'cloudify.interfaces.lifecycle.configure', 'configure_Apache', custom_context)
    set_state_task(ctx, graph, 'Mysql', 'creating', 'Mysql_creating', custom_context)
    operation_task(ctx, graph, 'Apache', 'cloudify.interfaces.lifecycle.create', 'create_Apache', custom_context)
    custom_context.register_native_delegate_wf_step('InternalNetwork', 'InternalNetwork_install')
    set_state_task(ctx, graph, 'Wordpress', 'initial', 'Wordpress_initial', custom_context)
    set_state_task(ctx, graph, 'Apache', 'configured', 'Apache_configured', custom_context)
    set_state_task(ctx, graph, 'PHP', 'started', 'PHP_started', custom_context)
    set_state_task(ctx, graph, 'Wordpress', 'configuring', 'Wordpress_configuring', custom_context)
    set_state_task(ctx, graph, 'PHP', 'creating', 'PHP_creating', custom_context)
    operation_task(ctx, graph, 'Wordpress', 'cloudify.interfaces.lifecycle.configure', 'configure_Wordpress', custom_context)
    set_state_task(ctx, graph, 'Mysql', 'starting', 'Mysql_starting', custom_context)
    set_state_task(ctx, graph, 'Mysql', 'configured', 'Mysql_configured', custom_context)
    custom_context.register_native_delegate_wf_step('NetPub', 'NetPub_install')
    set_state_task(ctx, graph, 'Apache', 'creating', 'Apache_creating', custom_context)
    operation_task(ctx, graph, 'PHP', 'cloudify.interfaces.lifecycle.create', 'create_PHP', custom_context)
    operation_task(ctx, graph, 'Apache', 'cloudify.interfaces.lifecycle.start', 'start_Apache', custom_context)
    operation_task(ctx, graph, 'PHP', 'cloudify.interfaces.lifecycle.configure', 'configure_PHP', custom_context)
    set_state_task(ctx, graph, 'Mysql', 'started', 'Mysql_started', custom_context)
    operation_task(ctx, graph, 'Wordpress', 'cloudify.interfaces.lifecycle.create', 'create_Wordpress', custom_context)
    set_state_task(ctx, graph, 'Wordpress', 'creating', 'Wordpress_creating', custom_context)
    set_state_task(ctx, graph, 'Mysql', 'initial', 'Mysql_initial', custom_context)
    set_state_task(ctx, graph, 'Apache', 'created', 'Apache_created', custom_context)
    set_state_task(ctx, graph, 'Apache', 'started', 'Apache_started', custom_context)
    set_state_task(ctx, graph, 'Wordpress', 'created', 'Wordpress_created', custom_context)
    set_state_task(ctx, graph, 'Apache', 'configuring', 'Apache_configuring', custom_context)
    operation_task(ctx, graph, 'Mysql', 'cloudify.interfaces.lifecycle.start', 'start_Mysql', custom_context)
    set_state_task(ctx, graph, 'PHP', 'initial', 'PHP_initial', custom_context)
    set_state_task(ctx, graph, 'PHP', 'configured', 'PHP_configured', custom_context)
    generate_native_node_workflows(ctx, graph, custom_context, 'install')
    link_tasks(graph, 'PHP_starting', 'PHP_configured', custom_context)
    link_tasks(graph, 'Wordpress_started', 'start_Wordpress', custom_context)
    link_tasks(graph, 'Mysql_configuring', 'Wordpress_created', custom_context)
    link_tasks(graph, 'Mysql_configuring', 'Mysql_created', custom_context)
    link_tasks(graph, 'create_Mysql', 'Mysql_creating', custom_context)
    link_tasks(graph, 'Apache_initial', 'Server_install', custom_context)
    link_tasks(graph, 'configure_Mysql', 'Mysql_configuring', custom_context)
    link_tasks(graph, 'Wordpress_configured', 'configure_Wordpress', custom_context)
    link_tasks(graph, 'Wordpress_starting', 'Wordpress_configured', custom_context)
    link_tasks(graph, 'Mysql_created', 'create_Mysql', custom_context)
    link_tasks(graph, 'start_PHP', 'PHP_starting', custom_context)
    link_tasks(graph, 'Apache_starting', 'Apache_configured', custom_context)
    link_tasks(graph, 'PHP_configuring', 'PHP_created', custom_context)
    link_tasks(graph, 'PHP_configuring', 'Wordpress_created', custom_context)
    link_tasks(graph, 'PHP_created', 'create_PHP', custom_context)
    link_tasks(graph, 'start_Wordpress', 'Wordpress_starting', custom_context)
    link_tasks(graph, 'configure_Apache', 'Apache_configuring', custom_context)
    link_tasks(graph, 'Mysql_creating', 'Mysql_initial', custom_context)
    link_tasks(graph, 'create_Apache', 'Apache_creating', custom_context)
    link_tasks(graph, 'Wordpress_initial', 'Apache_started', custom_context)
    link_tasks(graph, 'Apache_configured', 'configure_Apache', custom_context)
    link_tasks(graph, 'PHP_started', 'start_PHP', custom_context)
    link_tasks(graph, 'Wordpress_configuring', 'Mysql_started', custom_context)
    link_tasks(graph, 'Wordpress_configuring', 'PHP_started', custom_context)
    link_tasks(graph, 'Wordpress_configuring', 'Wordpress_created', custom_context)
    link_tasks(graph, 'PHP_creating', 'PHP_initial', custom_context)
    link_tasks(graph, 'configure_Wordpress', 'Wordpress_configuring', custom_context)
    link_tasks(graph, 'Mysql_starting', 'Mysql_configured', custom_context)
    link_tasks(graph, 'Mysql_configured', 'configure_Mysql', custom_context)
    link_tasks(graph, 'Apache_creating', 'Apache_initial', custom_context)
    link_tasks(graph, 'create_PHP', 'PHP_creating', custom_context)
    link_tasks(graph, 'start_Apache', 'Apache_starting', custom_context)
    link_tasks(graph, 'configure_PHP', 'PHP_configuring', custom_context)
    link_tasks(graph, 'Mysql_started', 'start_Mysql', custom_context)
    link_tasks(graph, 'create_Wordpress', 'Wordpress_creating', custom_context)
    link_tasks(graph, 'Wordpress_creating', 'Wordpress_initial', custom_context)
    link_tasks(graph, 'Mysql_initial', 'DataBase_install', custom_context)
    link_tasks(graph, 'Apache_created', 'create_Apache', custom_context)
    link_tasks(graph, 'Apache_started', 'start_Apache', custom_context)
    link_tasks(graph, 'Wordpress_created', 'create_Wordpress', custom_context)
    link_tasks(graph, 'Apache_configuring', 'Apache_created', custom_context)
    link_tasks(graph, 'start_Mysql', 'Mysql_starting', custom_context)
    link_tasks(graph, 'PHP_initial', 'Server_install', custom_context)
    link_tasks(graph, 'PHP_configured', 'configure_PHP', custom_context)
def _a4c_uninstall(ctx, graph, custom_context):
    #  following code can be pasted in src/test/python/workflows/tasks.py for simulation
    custom_context.add_customized_wf_node('PHP')
    custom_context.add_customized_wf_node('Wordpress')
    custom_context.add_customized_wf_node('Apache')
    custom_context.add_customized_wf_node('Mysql')
    custom_context.add_customized_wf_node('Apache')
    custom_context.add_customized_wf_node('Wordpress')
    custom_context.add_customized_wf_node('Mysql')
    custom_context.add_customized_wf_node('Apache')
    custom_context.add_customized_wf_node('Wordpress')
    custom_context.add_customized_wf_node('Apache')
    custom_context.add_customized_wf_node('PHP')
    custom_context.add_customized_wf_node('PHP')
    custom_context.add_customized_wf_node('Mysql')
    custom_context.add_customized_wf_node('Wordpress')
    custom_context.add_customized_wf_node('PHP')
    custom_context.add_customized_wf_node('Mysql')
    set_state_task(ctx, graph, 'PHP', 'stopping', 'PHP_stopping', custom_context)
    set_state_task(ctx, graph, 'Wordpress', 'deleting', 'Wordpress_deleting', custom_context)
    set_state_task(ctx, graph, 'Apache', 'stopped', 'Apache_stopped', custom_context)
    set_state_task(ctx, graph, 'Mysql', 'deleted', 'Mysql_deleted', custom_context)
    set_state_task(ctx, graph, 'Apache', 'deleted', 'Apache_deleted', custom_context)
    set_state_task(ctx, graph, 'Wordpress', 'deleted', 'Wordpress_deleted', custom_context)
    custom_context.register_native_delegate_wf_step('InternalNetwork', 'InternalNetwork_uninstall')
    set_state_task(ctx, graph, 'Mysql', 'deleting', 'Mysql_deleting', custom_context)
    custom_context.register_native_delegate_wf_step('NetPub', 'NetPub_uninstall')
    custom_context.register_native_delegate_wf_step('DataBase', 'DataBase_uninstall')
    set_state_task(ctx, graph, 'Apache', 'deleting', 'Apache_deleting', custom_context)
    set_state_task(ctx, graph, 'Wordpress', 'stopped', 'Wordpress_stopped', custom_context)
    set_state_task(ctx, graph, 'Apache', 'stopping', 'Apache_stopping', custom_context)
    set_state_task(ctx, graph, 'PHP', 'deleting', 'PHP_deleting', custom_context)
    custom_context.register_native_delegate_wf_step('Server', 'Server_uninstall')
    set_state_task(ctx, graph, 'PHP', 'stopped', 'PHP_stopped', custom_context)
    set_state_task(ctx, graph, 'Mysql', 'stopped', 'Mysql_stopped', custom_context)
    set_state_task(ctx, graph, 'Wordpress', 'stopping', 'Wordpress_stopping', custom_context)
    set_state_task(ctx, graph, 'PHP', 'deleted', 'PHP_deleted', custom_context)
    set_state_task(ctx, graph, 'Mysql', 'stopping', 'Mysql_stopping', custom_context)
    generate_native_node_workflows(ctx, graph, custom_context, 'uninstall')
    link_tasks(graph, 'Wordpress_deleting', 'Wordpress_stopped', custom_context)
    link_tasks(graph, 'Apache_stopped', 'Apache_stopping', custom_context)
    link_tasks(graph, 'Mysql_deleted', 'Mysql_deleting', custom_context)
    link_tasks(graph, 'Apache_deleted', 'Apache_deleting', custom_context)
    link_tasks(graph, 'Wordpress_deleted', 'Wordpress_deleting', custom_context)
    link_tasks(graph, 'Mysql_deleting', 'Mysql_stopped', custom_context)
    link_tasks(graph, 'DataBase_uninstall', 'Mysql_deleted', custom_context)
    link_tasks(graph, 'Apache_deleting', 'Apache_stopped', custom_context)
    link_tasks(graph, 'Wordpress_stopped', 'Wordpress_stopping', custom_context)
    link_tasks(graph, 'Apache_stopping', 'Wordpress_deleted', custom_context)
    link_tasks(graph, 'PHP_deleting', 'PHP_stopped', custom_context)
    link_tasks(graph, 'Server_uninstall', 'Apache_deleted', custom_context)
    link_tasks(graph, 'Server_uninstall', 'PHP_deleted', custom_context)
    link_tasks(graph, 'PHP_stopped', 'PHP_stopping', custom_context)
    link_tasks(graph, 'Mysql_stopped', 'Mysql_stopping', custom_context)
    link_tasks(graph, 'PHP_deleted', 'PHP_deleting', custom_context)

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
    #types.append('alien.nodes.Apache')
    #types.append('tosca.nodes.WebServer')
    #types.append('tosca.nodes.SoftwareComponent')
    #types.append('tosca.nodes.Root')
    #node_Apache = _build_node(ctx, 'Apache', types, 1)
    #types = []
    #types.append('alien.nodes.openstack.Compute')
    #types.append('tosca.nodes.Compute')
    #types.append('tosca.nodes.Root')
    #node_DataBase = _build_node(ctx, 'DataBase', types, 1)
    #types = []
    #types.append('alien.nodes.PHP')
    #types.append('tosca.nodes.SoftwareComponent')
    #types.append('tosca.nodes.Root')
    #node_PHP = _build_node(ctx, 'PHP', types, 1)
    #types = []
    #types.append('alien.nodes.Wordpress')
    #types.append('tosca.nodes.WebApplication')
    #types.append('tosca.nodes.Root')
    #node_Wordpress = _build_node(ctx, 'Wordpress', types, 1)
    #types = []
    #types.append('alien.nodes.openstack.PrivateNetwork')
    #types.append('alien.nodes.PrivateNetwork')
    #types.append('tosca.nodes.Network')
    #types.append('tosca.nodes.Root')
    #node_InternalNetwork = _build_node(ctx, 'InternalNetwork', types, 1)
    #types = []
    #types.append('alien.nodes.openstack.PublicNetwork')
    #types.append('alien.nodes.PublicNetwork')
    #types.append('tosca.nodes.Network')
    #types.append('tosca.nodes.Root')
    #node_NetPub = _build_node(ctx, 'NetPub', types, 1)
    #types = []
    #types.append('alien.nodes.openstack.Compute')
    #types.append('tosca.nodes.Compute')
    #types.append('tosca.nodes.Root')
    #node_Server = _build_node(ctx, 'Server', types, 1)
    #types = []
    #types.append('alien.nodes.Mysql')
    #types.append('tosca.nodes.Database')
    #types.append('tosca.nodes.Root')
    #node_Mysql = _build_node(ctx, 'Mysql', types, 1)
    #_add_relationship(node_Apache, node_Server)
    #_add_relationship(node_DataBase, node_InternalNetwork)
    #_add_relationship(node_PHP, node_Server)
    #_add_relationship(node_Wordpress, node_PHP)
    #_add_relationship(node_Wordpress, node_Apache)
    #_add_relationship(node_Wordpress, node_Mysql)
    #_add_relationship(node_Server, node_NetPub)
    #_add_relationship(node_Server, node_InternalNetwork)
    #_add_relationship(node_Mysql, node_DataBase)
