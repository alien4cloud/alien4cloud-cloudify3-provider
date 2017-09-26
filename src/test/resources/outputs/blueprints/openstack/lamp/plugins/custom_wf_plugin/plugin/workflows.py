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
from utils import is_kubernetes_node
from utils import relationship_operation_task
from workflow import WfStartEvent
from workflow import build_pre_event


# subworkflow 'install' for host 'Server'
def install_host_server(ctx, graph, custom_context):
    custom_context.add_customized_wf_node('php')
    custom_context.add_customized_wf_node('wordpress')
    custom_context.add_customized_wf_node('php')
    custom_context.add_customized_wf_node('php')
    custom_context.add_customized_wf_node('apache')
    custom_context.add_customized_wf_node('php')
    custom_context.add_customized_wf_node('apache')
    custom_context.add_customized_wf_node('apache')
    custom_context.add_customized_wf_node('apache')
    custom_context.add_customized_wf_node('php')
    custom_context.add_customized_wf_node('wordpress')
    custom_context.add_customized_wf_node('apache')
    custom_context.add_customized_wf_node('wordpress')
    custom_context.add_customized_wf_node('wordpress')
    custom_context.add_customized_wf_node('apache')
    custom_context.add_customized_wf_node('wordpress')
    custom_context.add_customized_wf_node('wordpress')
    custom_context.add_customized_wf_node('php')
    custom_context.add_customized_wf_node('php')
    custom_context.add_customized_wf_node('apache')
    custom_context.add_customized_wf_node('wordpress')
    operation_task(ctx, graph, 'apache', 'org.alien4cloud.interfaces.cfy.lifecycle.NodeInit', '_a4c_init_apache', custom_context)
    set_state_task(ctx, graph, 'php', 'starting', 'php_starting', custom_context)
    operation_task(ctx, graph, 'php', 'org.alien4cloud.interfaces.cfy.lifecycle.NodeInit', '_a4c_init_php', custom_context)
    operation_task(ctx, graph, 'php', 'cloudify.interfaces.lifecycle.configure', 'php_configure', custom_context)
    set_state_task(ctx, graph, 'wordpress', 'creating', 'wordpress_creating', custom_context)
    set_state_task(ctx, graph, 'php', 'creating', 'php_creating', custom_context)
    operation_task(ctx, graph, 'php', 'cloudify.interfaces.lifecycle.start', 'php_start', custom_context)
    set_state_task(ctx, graph, 'php', 'created', 'php_created', custom_context)
    operation_task(ctx, graph, 'apache', 'cloudify.interfaces.lifecycle.configure', 'apache_configure', custom_context)
    set_state_task(ctx, graph, 'apache', 'configuring', 'apache_configuring', custom_context)
    set_state_task(ctx, graph, 'php', 'configured', 'php_configured', custom_context)
    operation_task(ctx, graph, 'wordpress', 'cloudify.interfaces.lifecycle.create', 'wordpress_create', custom_context)
    set_state_task(ctx, graph, 'apache', 'created', 'apache_created', custom_context)
    set_state_task(ctx, graph, 'apache', 'configured', 'apache_configured', custom_context)
    operation_task(ctx, graph, 'apache', 'cloudify.interfaces.lifecycle.start', 'apache_start', custom_context)
    operation_task(ctx, graph, 'wordpress', 'org.alien4cloud.interfaces.cfy.lifecycle.NodeInit', '_a4c_init_wordpress', custom_context)
    set_state_task(ctx, graph, 'apache', 'creating', 'apache_creating', custom_context)
    set_state_task(ctx, graph, 'php', 'configuring', 'php_configuring', custom_context)
    set_state_task(ctx, graph, 'wordpress', 'started', 'wordpress_started', custom_context)
    set_state_task(ctx, graph, 'apache', 'starting', 'apache_starting', custom_context)
    set_state_task(ctx, graph, 'wordpress', 'initial', 'wordpress_initial', custom_context)
    set_state_task(ctx, graph, 'wordpress', 'configured', 'wordpress_configured', custom_context)
    operation_task(ctx, graph, 'php', 'cloudify.interfaces.lifecycle.create', 'php_create', custom_context)
    operation_task(ctx, graph, 'apache', 'cloudify.interfaces.lifecycle.create', 'apache_create', custom_context)
    custom_context.register_native_delegate_wf_step('Server', 'Server_install')
    set_state_task(ctx, graph, 'apache', 'initial', 'apache_initial', custom_context)
    set_state_task(ctx, graph, 'wordpress', 'created', 'wordpress_created', custom_context)
    set_state_task(ctx, graph, 'wordpress', 'starting', 'wordpress_starting', custom_context)
    set_state_task(ctx, graph, 'php', 'started', 'php_started', custom_context)
    set_state_task(ctx, graph, 'php', 'initial', 'php_initial', custom_context)
    set_state_task(ctx, graph, 'apache', 'started', 'apache_started', custom_context)
    set_state_task(ctx, graph, 'wordpress', 'configuring', 'wordpress_configuring', custom_context)
    operation_task(ctx, graph, 'wordpress', 'cloudify.interfaces.lifecycle.configure', 'wordpress_configure', custom_context)
    operation_task(ctx, graph, 'wordpress', 'cloudify.interfaces.lifecycle.start', 'wordpress_start', custom_context)
    generate_native_node_workflows(ctx, graph, custom_context, 'install')
    link_tasks(graph, 'apache_create', '_a4c_init_apache', custom_context)
    link_tasks(graph, 'php_start', 'php_starting', custom_context)
    link_tasks(graph, 'php_create', '_a4c_init_php', custom_context)
    link_tasks(graph, 'php_configured', 'php_configure', custom_context)
    link_tasks(graph, '_a4c_init_wordpress', 'wordpress_creating', custom_context)
    link_tasks(graph, '_a4c_init_php', 'php_creating', custom_context)
    link_tasks(graph, 'php_started', 'php_start', custom_context)
    link_tasks(graph, 'php_configuring', 'php_created', custom_context)
    link_tasks(graph, 'apache_configured', 'apache_configure', custom_context)
    link_tasks(graph, 'apache_configure', 'apache_configuring', custom_context)
    link_tasks(graph, 'php_starting', 'php_configured', custom_context)
    link_tasks(graph, 'wordpress_created', 'wordpress_create', custom_context)
    link_tasks(graph, 'apache_configuring', 'apache_created', custom_context)
    link_tasks(graph, 'apache_starting', 'apache_configured', custom_context)
    link_tasks(graph, 'apache_started', 'apache_start', custom_context)
    link_tasks(graph, 'wordpress_create', '_a4c_init_wordpress', custom_context)
    link_tasks(graph, '_a4c_init_apache', 'apache_creating', custom_context)
    link_tasks(graph, 'php_configure', 'php_configuring', custom_context)
    link_tasks(graph, 'apache_start', 'apache_starting', custom_context)
    link_tasks(graph, 'wordpress_creating', 'wordpress_initial', custom_context)
    link_tasks(graph, 'wordpress_starting', 'wordpress_configured', custom_context)
    link_tasks(graph, 'php_created', 'php_create', custom_context)
    link_tasks(graph, 'apache_created', 'apache_create', custom_context)
    link_tasks(graph, 'apache_creating', 'Server_install', custom_context)
    link_tasks(graph, 'php_creating', 'Server_install', custom_context)
    link_tasks(graph, 'apache_creating', 'apache_initial', custom_context)
    link_tasks(graph, 'php_configuring', 'wordpress_created', custom_context)
    link_tasks(graph, 'wordpress_configuring', 'wordpress_created', custom_context)
    link_tasks(graph, 'wordpress_start', 'wordpress_starting', custom_context)
    link_tasks(graph, 'wordpress_configuring', 'php_started', custom_context)
    link_tasks(graph, 'php_creating', 'php_initial', custom_context)
    link_tasks(graph, 'wordpress_creating', 'apache_started', custom_context)
    link_tasks(graph, 'wordpress_configure', 'wordpress_configuring', custom_context)
    link_tasks(graph, 'wordpress_configured', 'wordpress_configure', custom_context)
    link_tasks(graph, 'wordpress_started', 'wordpress_start', custom_context)


# subworkflow 'install' for host 'DataBase'
def install_host_database(ctx, graph, custom_context):
    custom_context.add_customized_wf_node('mysql')
    custom_context.add_customized_wf_node('mysql')
    custom_context.add_customized_wf_node('mysql')
    custom_context.add_customized_wf_node('mysql')
    custom_context.add_customized_wf_node('mysql')
    custom_context.add_customized_wf_node('mysql')
    custom_context.add_customized_wf_node('mysql')
    operation_task(ctx, graph, 'mysql', 'cloudify.interfaces.lifecycle.start', 'mysql_start', custom_context)
    operation_task(ctx, graph, 'mysql', 'org.alien4cloud.interfaces.cfy.lifecycle.NodeInit', '_a4c_init_mysql', custom_context)
    custom_context.register_native_delegate_wf_step('DataBase', 'DataBase_install')
    set_state_task(ctx, graph, 'mysql', 'started', 'mysql_started', custom_context)
    set_state_task(ctx, graph, 'mysql', 'configuring', 'mysql_configuring', custom_context)
    set_state_task(ctx, graph, 'mysql', 'starting', 'mysql_starting', custom_context)
    set_state_task(ctx, graph, 'mysql', 'creating', 'mysql_creating', custom_context)
    set_state_task(ctx, graph, 'mysql', 'configured', 'mysql_configured', custom_context)
    set_state_task(ctx, graph, 'mysql', 'initial', 'mysql_initial', custom_context)
    operation_task(ctx, graph, 'mysql', 'cloudify.interfaces.lifecycle.configure', 'mysql_configure', custom_context)
    set_state_task(ctx, graph, 'mysql', 'created', 'mysql_created', custom_context)
    operation_task(ctx, graph, 'mysql', 'cloudify.interfaces.lifecycle.create', 'mysql_create', custom_context)
    generate_native_node_workflows(ctx, graph, custom_context, 'install')
    link_tasks(graph, 'mysql_started', 'mysql_start', custom_context)
    link_tasks(graph, 'mysql_create', '_a4c_init_mysql', custom_context)
    link_tasks(graph, 'mysql_creating', 'DataBase_install', custom_context)
    link_tasks(graph, 'mysql_configure', 'mysql_configuring', custom_context)
    link_tasks(graph, 'mysql_start', 'mysql_starting', custom_context)
    link_tasks(graph, '_a4c_init_mysql', 'mysql_creating', custom_context)
    link_tasks(graph, 'mysql_starting', 'mysql_configured', custom_context)
    link_tasks(graph, 'mysql_creating', 'mysql_initial', custom_context)
    link_tasks(graph, 'mysql_configured', 'mysql_configure', custom_context)
    link_tasks(graph, 'mysql_configuring', 'mysql_created', custom_context)
    link_tasks(graph, 'mysql_created', 'mysql_create', custom_context)


# subworkflow 'uninstall' for host 'Server'
def uninstall_host_server(ctx, graph, custom_context):
    custom_context.add_customized_wf_node('wordpress')
    custom_context.add_customized_wf_node('apache')
    custom_context.add_customized_wf_node('apache')
    custom_context.add_customized_wf_node('php')
    custom_context.add_customized_wf_node('php')
    custom_context.add_customized_wf_node('php')
    custom_context.add_customized_wf_node('apache')
    custom_context.add_customized_wf_node('apache')
    custom_context.add_customized_wf_node('php')
    custom_context.add_customized_wf_node('wordpress')
    custom_context.add_customized_wf_node('wordpress')
    custom_context.add_customized_wf_node('wordpress')
    set_state_task(ctx, graph, 'wordpress', 'stopping', 'wordpress_stopping', custom_context)
    set_state_task(ctx, graph, 'apache', 'deleted', 'apache_deleted', custom_context)
    set_state_task(ctx, graph, 'apache', 'stopping', 'apache_stopping', custom_context)
    set_state_task(ctx, graph, 'php', 'deleting', 'php_deleting', custom_context)
    operation_task(ctx, graph, 'apache', 'cloudify.interfaces.lifecycle.stop', 'apache_stop', custom_context)
    set_state_task(ctx, graph, 'php', 'stopped', 'php_stopped', custom_context)
    operation_task(ctx, graph, 'wordpress', 'cloudify.interfaces.lifecycle.delete', 'wordpress_delete', custom_context)
    operation_task(ctx, graph, 'apache', 'cloudify.interfaces.lifecycle.delete', 'apache_delete', custom_context)
    operation_task(ctx, graph, 'php', 'cloudify.interfaces.lifecycle.delete', 'php_delete', custom_context)
    set_state_task(ctx, graph, 'php', 'deleted', 'php_deleted', custom_context)
    set_state_task(ctx, graph, 'apache', 'deleting', 'apache_deleting', custom_context)
    set_state_task(ctx, graph, 'apache', 'stopped', 'apache_stopped', custom_context)
    operation_task(ctx, graph, 'wordpress', 'cloudify.interfaces.lifecycle.stop', 'wordpress_stop', custom_context)
    custom_context.register_native_delegate_wf_step('Server', 'Server_uninstall')
    set_state_task(ctx, graph, 'php', 'stopping', 'php_stopping', custom_context)
    operation_task(ctx, graph, 'php', 'cloudify.interfaces.lifecycle.stop', 'php_stop', custom_context)
    set_state_task(ctx, graph, 'wordpress', 'stopped', 'wordpress_stopped', custom_context)
    set_state_task(ctx, graph, 'wordpress', 'deleting', 'wordpress_deleting', custom_context)
    set_state_task(ctx, graph, 'wordpress', 'deleted', 'wordpress_deleted', custom_context)
    generate_native_node_workflows(ctx, graph, custom_context, 'uninstall')
    link_tasks(graph, 'wordpress_stop', 'wordpress_stopping', custom_context)
    link_tasks(graph, 'Server_uninstall', 'apache_deleted', custom_context)
    link_tasks(graph, 'apache_stop', 'apache_stopping', custom_context)
    link_tasks(graph, 'php_delete', 'php_deleting', custom_context)
    link_tasks(graph, 'apache_stopped', 'apache_stop', custom_context)
    link_tasks(graph, 'php_deleting', 'php_stopped', custom_context)
    link_tasks(graph, 'wordpress_deleted', 'wordpress_delete', custom_context)
    link_tasks(graph, 'apache_deleted', 'apache_delete', custom_context)
    link_tasks(graph, 'php_deleted', 'php_delete', custom_context)
    link_tasks(graph, 'Server_uninstall', 'php_deleted', custom_context)
    link_tasks(graph, 'apache_delete', 'apache_deleting', custom_context)
    link_tasks(graph, 'apache_deleting', 'apache_stopped', custom_context)
    link_tasks(graph, 'wordpress_stopped', 'wordpress_stop', custom_context)
    link_tasks(graph, 'php_stop', 'php_stopping', custom_context)
    link_tasks(graph, 'php_stopped', 'php_stop', custom_context)
    link_tasks(graph, 'php_stopping', 'wordpress_stopped', custom_context)
    link_tasks(graph, 'wordpress_deleting', 'wordpress_stopped', custom_context)
    link_tasks(graph, 'wordpress_delete', 'wordpress_deleting', custom_context)
    link_tasks(graph, 'apache_stopping', 'wordpress_deleted', custom_context)


# subworkflow 'uninstall' for host 'DataBase'
def uninstall_host_database(ctx, graph, custom_context):
    custom_context.add_customized_wf_node('mysql')
    custom_context.add_customized_wf_node('mysql')
    custom_context.add_customized_wf_node('mysql')
    custom_context.add_customized_wf_node('mysql')
    operation_task(ctx, graph, 'mysql', 'cloudify.interfaces.lifecycle.stop', 'mysql_stop', custom_context)
    operation_task(ctx, graph, 'mysql', 'cloudify.interfaces.lifecycle.delete', 'mysql_delete', custom_context)
    set_state_task(ctx, graph, 'mysql', 'stopping', 'mysql_stopping', custom_context)
    custom_context.register_native_delegate_wf_step('DataBase', 'DataBase_uninstall')
    set_state_task(ctx, graph, 'mysql', 'stopped', 'mysql_stopped', custom_context)
    set_state_task(ctx, graph, 'mysql', 'deleting', 'mysql_deleting', custom_context)
    set_state_task(ctx, graph, 'mysql', 'deleted', 'mysql_deleted', custom_context)
    generate_native_node_workflows(ctx, graph, custom_context, 'uninstall')
    link_tasks(graph, 'mysql_stopped', 'mysql_stop', custom_context)
    link_tasks(graph, 'mysql_deleted', 'mysql_delete', custom_context)
    link_tasks(graph, 'mysql_stop', 'mysql_stopping', custom_context)
    link_tasks(graph, 'mysql_deleting', 'mysql_stopped', custom_context)
    link_tasks(graph, 'mysql_delete', 'mysql_deleting', custom_context)
    link_tasks(graph, 'DataBase_uninstall', 'mysql_deleted', custom_context)


def install_host(ctx, graph, custom_context, compute):
    options = {}
    options['Server'] = install_host_server
    options['DataBase'] = install_host_database
    options[compute](ctx, graph, custom_context)


def uninstall_host(ctx, graph, custom_context, compute):
    options = {}
    options['Server'] = uninstall_host_server
    options['DataBase'] = uninstall_host_database
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


@workflow
def a4c_start(**kwargs):
    graph = ctx.graph_mode()
    nodes = _get_all_nodes(ctx)
    instances = _get_all_nodes_instances(ctx)
    custom_context = CustomContext(ctx, instances, nodes)
    ctx.internal.send_workflow_event(event_type='a4c_workflow_started', message=build_pre_event(WfStartEvent('start')))
    _a4c_start(ctx, graph, custom_context)
    return graph.execute()


@workflow
def a4c_stop(**kwargs):
    graph = ctx.graph_mode()
    nodes = _get_all_nodes(ctx)
    instances = _get_all_nodes_instances(ctx)
    custom_context = CustomContext(ctx, instances, nodes)
    ctx.internal.send_workflow_event(event_type='a4c_workflow_started', message=build_pre_event(WfStartEvent('stop')))
    _a4c_stop(ctx, graph, custom_context)
    return graph.execute()


def _a4c_install(ctx, graph, custom_context):
    #  following code can be pasted in src/test/python/workflows/tasks.py for simulation
    custom_context.add_customized_wf_node('php')
    custom_context.add_customized_wf_node('wordpress')
    custom_context.add_customized_wf_node('php')
    custom_context.add_customized_wf_node('php')
    custom_context.add_customized_wf_node('apache')
    custom_context.add_customized_wf_node('php')
    custom_context.add_customized_wf_node('mysql')
    custom_context.add_customized_wf_node('apache')
    custom_context.add_customized_wf_node('mysql')
    custom_context.add_customized_wf_node('apache')
    custom_context.add_customized_wf_node('apache')
    custom_context.add_customized_wf_node('mysql')
    custom_context.add_customized_wf_node('mysql')
    custom_context.add_customized_wf_node('php')
    custom_context.add_customized_wf_node('mysql')
    custom_context.add_customized_wf_node('mysql')
    custom_context.add_customized_wf_node('wordpress')
    custom_context.add_customized_wf_node('apache')
    custom_context.add_customized_wf_node('wordpress')
    custom_context.add_customized_wf_node('wordpress')
    custom_context.add_customized_wf_node('mysql')
    custom_context.add_customized_wf_node('apache')
    custom_context.add_customized_wf_node('wordpress')
    custom_context.add_customized_wf_node('wordpress')
    custom_context.add_customized_wf_node('php')
    custom_context.add_customized_wf_node('php')
    custom_context.add_customized_wf_node('apache')
    custom_context.add_customized_wf_node('wordpress')
    relationship_operation_task(graph, 'wordpress', 'mysql', 'cloudify.interfaces.relationship_lifecycle.preconfigure', 'TARGET', 'wordpress_wordpressConnectToMysqlMysql_pre_configure_target', custom_context)
    operation_task(ctx, graph, 'apache', 'org.alien4cloud.interfaces.cfy.lifecycle.NodeInit', '_a4c_init_apache', custom_context)
    set_state_task(ctx, graph, 'php', 'starting', 'php_starting', custom_context)
    operation_task(ctx, graph, 'php', 'org.alien4cloud.interfaces.cfy.lifecycle.NodeInit', '_a4c_init_php', custom_context)
    operation_task(ctx, graph, 'mysql', 'cloudify.interfaces.lifecycle.start', 'mysql_start', custom_context)
    operation_task(ctx, graph, 'php', 'cloudify.interfaces.lifecycle.configure', 'php_configure', custom_context)
    set_state_task(ctx, graph, 'wordpress', 'creating', 'wordpress_creating', custom_context)
    relationship_operation_task(graph, 'php', 'Server', 'cloudify.interfaces.relationship_lifecycle.postconfigure', 'SOURCE', 'php_hostedOnServer_post_configure_source', custom_context)
    set_state_task(ctx, graph, 'php', 'creating', 'php_creating', custom_context)
    relationship_operation_task(graph, 'wordpress', 'apache', 'cloudify.interfaces.relationship_lifecycle.establish', 'TARGET', 'wordpress_wordpressHostedOnApacheApache_add_source', custom_context)
    relationship_operation_task(graph, 'php', 'Server', 'cloudify.interfaces.relationship_lifecycle.preconfigure', 'SOURCE', 'php_hostedOnServer_pre_configure_source', custom_context)
    relationship_operation_task(graph, 'wordpress', 'apache', 'cloudify.interfaces.relationship_lifecycle.establish', 'SOURCE', 'wordpress_wordpressHostedOnApacheApache_add_target', custom_context)
    operation_task(ctx, graph, 'mysql', 'org.alien4cloud.interfaces.cfy.lifecycle.NodeInit', '_a4c_init_mysql', custom_context)
    operation_task(ctx, graph, 'php', 'cloudify.interfaces.lifecycle.start', 'php_start', custom_context)
    set_state_task(ctx, graph, 'php', 'created', 'php_created', custom_context)
    operation_task(ctx, graph, 'apache', 'cloudify.interfaces.lifecycle.configure', 'apache_configure', custom_context)
    relationship_operation_task(graph, 'wordpress', 'apache', 'cloudify.interfaces.relationship_lifecycle.preconfigure', 'SOURCE', 'wordpress_wordpressHostedOnApacheApache_pre_configure_source', custom_context)
    set_state_task(ctx, graph, 'apache', 'configuring', 'apache_configuring', custom_context)
    set_state_task(ctx, graph, 'php', 'configured', 'php_configured', custom_context)
    relationship_operation_task(graph, 'wordpress', 'php', 'cloudify.interfaces.relationship_lifecycle.establish', 'SOURCE', 'wordpress_wordpressConnectToPHPPhp_add_target', custom_context)
    custom_context.register_native_delegate_wf_step('NetPub', 'NetPub_install')
    custom_context.register_native_delegate_wf_step('DataBase', 'DataBase_install')
    relationship_operation_task(graph, 'mysql', 'DataBase', 'cloudify.interfaces.relationship_lifecycle.preconfigure', 'SOURCE', 'mysql_hostedOnDataBase_pre_configure_source', custom_context)
    relationship_operation_task(graph, 'wordpress', 'apache', 'cloudify.interfaces.relationship_lifecycle.preconfigure', 'TARGET', 'wordpress_wordpressHostedOnApacheApache_pre_configure_target', custom_context)
    set_state_task(ctx, graph, 'mysql', 'started', 'mysql_started', custom_context)
    operation_task(ctx, graph, 'wordpress', 'cloudify.interfaces.lifecycle.create', 'wordpress_create', custom_context)
    set_state_task(ctx, graph, 'apache', 'created', 'apache_created', custom_context)
    relationship_operation_task(graph, 'apache', 'Server', 'cloudify.interfaces.relationship_lifecycle.preconfigure', 'SOURCE', 'apache_hostedOnServer_pre_configure_source', custom_context)
    relationship_operation_task(graph, 'wordpress', 'php', 'cloudify.interfaces.relationship_lifecycle.preconfigure', 'SOURCE', 'wordpress_wordpressConnectToPHPPhp_pre_configure_source', custom_context)
    set_state_task(ctx, graph, 'mysql', 'configuring', 'mysql_configuring', custom_context)
    relationship_operation_task(graph, 'wordpress', 'mysql', 'cloudify.interfaces.relationship_lifecycle.postconfigure', 'TARGET', 'wordpress_wordpressConnectToMysqlMysql_post_configure_target', custom_context)
    set_state_task(ctx, graph, 'apache', 'configured', 'apache_configured', custom_context)
    operation_task(ctx, graph, 'apache', 'cloudify.interfaces.lifecycle.start', 'apache_start', custom_context)
    operation_task(ctx, graph, 'wordpress', 'org.alien4cloud.interfaces.cfy.lifecycle.NodeInit', '_a4c_init_wordpress', custom_context)
    relationship_operation_task(graph, 'wordpress', 'php', 'cloudify.interfaces.relationship_lifecycle.preconfigure', 'TARGET', 'wordpress_wordpressConnectToPHPPhp_pre_configure_target', custom_context)
    relationship_operation_task(graph, 'wordpress', 'mysql', 'cloudify.interfaces.relationship_lifecycle.establish', 'TARGET', 'wordpress_wordpressConnectToMysqlMysql_add_source', custom_context)
    relationship_operation_task(graph, 'wordpress', 'apache', 'cloudify.interfaces.relationship_lifecycle.postconfigure', 'TARGET', 'wordpress_wordpressHostedOnApacheApache_post_configure_target', custom_context)
    set_state_task(ctx, graph, 'apache', 'creating', 'apache_creating', custom_context)
    set_state_task(ctx, graph, 'mysql', 'starting', 'mysql_starting', custom_context)
    relationship_operation_task(graph, 'wordpress', 'apache', 'cloudify.interfaces.relationship_lifecycle.postconfigure', 'SOURCE', 'wordpress_wordpressHostedOnApacheApache_post_configure_source', custom_context)
    set_state_task(ctx, graph, 'mysql', 'creating', 'mysql_creating', custom_context)
    set_state_task(ctx, graph, 'php', 'configuring', 'php_configuring', custom_context)
    set_state_task(ctx, graph, 'mysql', 'configured', 'mysql_configured', custom_context)
    set_state_task(ctx, graph, 'mysql', 'initial', 'mysql_initial', custom_context)
    relationship_operation_task(graph, 'wordpress', 'mysql', 'cloudify.interfaces.relationship_lifecycle.establish', 'SOURCE', 'wordpress_wordpressConnectToMysqlMysql_add_target', custom_context)
    relationship_operation_task(graph, 'wordpress', 'php', 'cloudify.interfaces.relationship_lifecycle.postconfigure', 'SOURCE', 'wordpress_wordpressConnectToPHPPhp_post_configure_source', custom_context)
    custom_context.register_native_delegate_wf_step('InternalNetwork', 'InternalNetwork_install')
    relationship_operation_task(graph, 'mysql', 'DataBase', 'cloudify.interfaces.relationship_lifecycle.postconfigure', 'SOURCE', 'mysql_hostedOnDataBase_post_configure_source', custom_context)
    relationship_operation_task(graph, 'php', 'Server', 'cloudify.interfaces.relationship_lifecycle.establish', 'SOURCE', 'php_hostedOnServer_add_target', custom_context)
    set_state_task(ctx, graph, 'wordpress', 'started', 'wordpress_started', custom_context)
    set_state_task(ctx, graph, 'apache', 'starting', 'apache_starting', custom_context)
    operation_task(ctx, graph, 'mysql', 'cloudify.interfaces.lifecycle.configure', 'mysql_configure', custom_context)
    relationship_operation_task(graph, 'apache', 'Server', 'cloudify.interfaces.relationship_lifecycle.postconfigure', 'SOURCE', 'apache_hostedOnServer_post_configure_source', custom_context)
    set_state_task(ctx, graph, 'wordpress', 'initial', 'wordpress_initial', custom_context)
    set_state_task(ctx, graph, 'wordpress', 'configured', 'wordpress_configured', custom_context)
    relationship_operation_task(graph, 'wordpress', 'mysql', 'cloudify.interfaces.relationship_lifecycle.postconfigure', 'SOURCE', 'wordpress_wordpressConnectToMysqlMysql_post_configure_source', custom_context)
    operation_task(ctx, graph, 'php', 'cloudify.interfaces.lifecycle.create', 'php_create', custom_context)
    operation_task(ctx, graph, 'apache', 'cloudify.interfaces.lifecycle.create', 'apache_create', custom_context)
    relationship_operation_task(graph, 'mysql', 'DataBase', 'cloudify.interfaces.relationship_lifecycle.establish', 'SOURCE', 'mysql_hostedOnDataBase_add_target', custom_context)
    set_state_task(ctx, graph, 'mysql', 'created', 'mysql_created', custom_context)
    custom_context.register_native_delegate_wf_step('Server', 'Server_install')
    set_state_task(ctx, graph, 'apache', 'initial', 'apache_initial', custom_context)
    set_state_task(ctx, graph, 'wordpress', 'created', 'wordpress_created', custom_context)
    set_state_task(ctx, graph, 'wordpress', 'starting', 'wordpress_starting', custom_context)
    set_state_task(ctx, graph, 'php', 'started', 'php_started', custom_context)
    relationship_operation_task(graph, 'wordpress', 'php', 'cloudify.interfaces.relationship_lifecycle.establish', 'TARGET', 'wordpress_wordpressConnectToPHPPhp_add_source', custom_context)
    set_state_task(ctx, graph, 'php', 'initial', 'php_initial', custom_context)
    relationship_operation_task(graph, 'wordpress', 'mysql', 'cloudify.interfaces.relationship_lifecycle.preconfigure', 'SOURCE', 'wordpress_wordpressConnectToMysqlMysql_pre_configure_source', custom_context)
    relationship_operation_task(graph, 'wordpress', 'php', 'cloudify.interfaces.relationship_lifecycle.postconfigure', 'TARGET', 'wordpress_wordpressConnectToPHPPhp_post_configure_target', custom_context)
    relationship_operation_task(graph, 'apache', 'Server', 'cloudify.interfaces.relationship_lifecycle.establish', 'SOURCE', 'apache_hostedOnServer_add_target', custom_context)
    operation_task(ctx, graph, 'mysql', 'cloudify.interfaces.lifecycle.create', 'mysql_create', custom_context)
    set_state_task(ctx, graph, 'apache', 'started', 'apache_started', custom_context)
    set_state_task(ctx, graph, 'wordpress', 'configuring', 'wordpress_configuring', custom_context)
    operation_task(ctx, graph, 'wordpress', 'cloudify.interfaces.lifecycle.configure', 'wordpress_configure', custom_context)
    operation_task(ctx, graph, 'wordpress', 'cloudify.interfaces.lifecycle.start', 'wordpress_start', custom_context)
    generate_native_node_workflows(ctx, graph, custom_context, 'install')
    link_tasks(graph, 'wordpress_wordpressConnectToMysqlMysql_pre_configure_target', 'mysql_created', custom_context)
    link_tasks(graph, '_a4c_init_apache', 'apache_creating', custom_context)
    link_tasks(graph, 'php_starting', 'php_configured', custom_context)
    link_tasks(graph, '_a4c_init_php', 'php_creating', custom_context)
    link_tasks(graph, 'mysql_start', 'mysql_hostedOnDataBase_post_configure_source', custom_context)
    link_tasks(graph, 'mysql_start', 'mysql_starting', custom_context)
    link_tasks(graph, 'mysql_start', 'wordpress_wordpressConnectToMysqlMysql_post_configure_target', custom_context)
    link_tasks(graph, 'php_configure', 'wordpress_wordpressConnectToPHPPhp_pre_configure_target', custom_context)
    link_tasks(graph, 'php_configure', 'php_configuring', custom_context)
    link_tasks(graph, 'php_configure', 'php_hostedOnServer_pre_configure_source', custom_context)
    link_tasks(graph, 'wordpress_creating', 'wordpress_initial', custom_context)
    link_tasks(graph, 'wordpress_creating', 'apache_started', custom_context)
    link_tasks(graph, 'php_hostedOnServer_post_configure_source', 'php_configured', custom_context)
    link_tasks(graph, 'php_creating', 'php_initial', custom_context)
    link_tasks(graph, 'php_creating', 'Server_install', custom_context)
    link_tasks(graph, 'wordpress_wordpressHostedOnApacheApache_add_source', 'wordpress_started', custom_context)
    link_tasks(graph, 'wordpress_wordpressHostedOnApacheApache_add_source', 'apache_started', custom_context)
    link_tasks(graph, 'php_hostedOnServer_pre_configure_source', 'php_created', custom_context)
    link_tasks(graph, 'wordpress_wordpressHostedOnApacheApache_add_target', 'wordpress_started', custom_context)
    link_tasks(graph, 'wordpress_wordpressHostedOnApacheApache_add_target', 'apache_started', custom_context)
    link_tasks(graph, '_a4c_init_mysql', 'mysql_creating', custom_context)
    link_tasks(graph, 'php_start', 'php_starting', custom_context)
    link_tasks(graph, 'php_start', 'php_hostedOnServer_post_configure_source', custom_context)
    link_tasks(graph, 'php_start', 'wordpress_wordpressConnectToPHPPhp_post_configure_target', custom_context)
    link_tasks(graph, 'php_created', 'php_create', custom_context)
    link_tasks(graph, 'apache_configure', 'wordpress_wordpressHostedOnApacheApache_pre_configure_target', custom_context)
    link_tasks(graph, 'apache_configure', 'apache_hostedOnServer_pre_configure_source', custom_context)
    link_tasks(graph, 'apache_configure', 'apache_configuring', custom_context)
    link_tasks(graph, 'wordpress_wordpressHostedOnApacheApache_pre_configure_source', 'wordpress_created', custom_context)
    link_tasks(graph, 'apache_configuring', 'apache_created', custom_context)
    link_tasks(graph, 'php_configured', 'php_configure', custom_context)
    link_tasks(graph, 'wordpress_wordpressConnectToPHPPhp_add_target', 'php_started', custom_context)
    link_tasks(graph, 'wordpress_wordpressConnectToPHPPhp_add_target', 'wordpress_started', custom_context)
    link_tasks(graph, 'mysql_hostedOnDataBase_pre_configure_source', 'mysql_created', custom_context)
    link_tasks(graph, 'wordpress_wordpressHostedOnApacheApache_pre_configure_target', 'apache_created', custom_context)
    link_tasks(graph, 'mysql_started', 'mysql_start', custom_context)
    link_tasks(graph, 'wordpress_create', '_a4c_init_wordpress', custom_context)
    link_tasks(graph, 'apache_created', 'apache_create', custom_context)
    link_tasks(graph, 'apache_hostedOnServer_pre_configure_source', 'apache_created', custom_context)
    link_tasks(graph, 'wordpress_wordpressConnectToPHPPhp_pre_configure_source', 'wordpress_created', custom_context)
    link_tasks(graph, 'mysql_configuring', 'wordpress_created', custom_context)
    link_tasks(graph, 'mysql_configuring', 'mysql_created', custom_context)
    link_tasks(graph, 'wordpress_wordpressConnectToMysqlMysql_post_configure_target', 'mysql_configured', custom_context)
    link_tasks(graph, 'apache_configured', 'apache_configure', custom_context)
    link_tasks(graph, 'apache_start', 'wordpress_wordpressHostedOnApacheApache_post_configure_target', custom_context)
    link_tasks(graph, 'apache_start', 'apache_starting', custom_context)
    link_tasks(graph, 'apache_start', 'apache_hostedOnServer_post_configure_source', custom_context)
    link_tasks(graph, '_a4c_init_wordpress', 'wordpress_creating', custom_context)
    link_tasks(graph, 'wordpress_wordpressConnectToPHPPhp_pre_configure_target', 'php_created', custom_context)
    link_tasks(graph, 'wordpress_wordpressConnectToMysqlMysql_add_source', 'mysql_started', custom_context)
    link_tasks(graph, 'wordpress_wordpressConnectToMysqlMysql_add_source', 'wordpress_started', custom_context)
    link_tasks(graph, 'wordpress_wordpressHostedOnApacheApache_post_configure_target', 'apache_configured', custom_context)
    link_tasks(graph, 'apache_creating', 'apache_initial', custom_context)
    link_tasks(graph, 'apache_creating', 'Server_install', custom_context)
    link_tasks(graph, 'mysql_starting', 'mysql_configured', custom_context)
    link_tasks(graph, 'wordpress_wordpressHostedOnApacheApache_post_configure_source', 'wordpress_configured', custom_context)
    link_tasks(graph, 'mysql_creating', 'mysql_initial', custom_context)
    link_tasks(graph, 'mysql_creating', 'DataBase_install', custom_context)
    link_tasks(graph, 'php_configuring', 'wordpress_created', custom_context)
    link_tasks(graph, 'php_configuring', 'php_created', custom_context)
    link_tasks(graph, 'mysql_configured', 'mysql_configure', custom_context)
    link_tasks(graph, 'wordpress_wordpressConnectToMysqlMysql_add_target', 'mysql_started', custom_context)
    link_tasks(graph, 'wordpress_wordpressConnectToMysqlMysql_add_target', 'wordpress_started', custom_context)
    link_tasks(graph, 'wordpress_wordpressConnectToPHPPhp_post_configure_source', 'wordpress_configured', custom_context)
    link_tasks(graph, 'mysql_hostedOnDataBase_post_configure_source', 'mysql_configured', custom_context)
    link_tasks(graph, 'php_hostedOnServer_add_target', 'php_started', custom_context)
    link_tasks(graph, 'php_hostedOnServer_add_target', 'Server_install', custom_context)
    link_tasks(graph, 'wordpress_started', 'wordpress_start', custom_context)
    link_tasks(graph, 'apache_starting', 'apache_configured', custom_context)
    link_tasks(graph, 'mysql_configure', 'wordpress_wordpressConnectToMysqlMysql_pre_configure_target', custom_context)
    link_tasks(graph, 'mysql_configure', 'mysql_hostedOnDataBase_pre_configure_source', custom_context)
    link_tasks(graph, 'mysql_configure', 'mysql_configuring', custom_context)
    link_tasks(graph, 'apache_hostedOnServer_post_configure_source', 'apache_configured', custom_context)
    link_tasks(graph, 'wordpress_configured', 'wordpress_configure', custom_context)
    link_tasks(graph, 'wordpress_wordpressConnectToMysqlMysql_post_configure_source', 'wordpress_configured', custom_context)
    link_tasks(graph, 'php_create', '_a4c_init_php', custom_context)
    link_tasks(graph, 'apache_create', '_a4c_init_apache', custom_context)
    link_tasks(graph, 'mysql_hostedOnDataBase_add_target', 'DataBase_install', custom_context)
    link_tasks(graph, 'mysql_hostedOnDataBase_add_target', 'mysql_started', custom_context)
    link_tasks(graph, 'mysql_created', 'mysql_create', custom_context)
    link_tasks(graph, 'wordpress_created', 'wordpress_create', custom_context)
    link_tasks(graph, 'wordpress_starting', 'wordpress_configured', custom_context)
    link_tasks(graph, 'php_started', 'php_start', custom_context)
    link_tasks(graph, 'wordpress_wordpressConnectToPHPPhp_add_source', 'php_started', custom_context)
    link_tasks(graph, 'wordpress_wordpressConnectToPHPPhp_add_source', 'wordpress_started', custom_context)
    link_tasks(graph, 'wordpress_wordpressConnectToMysqlMysql_pre_configure_source', 'wordpress_created', custom_context)
    link_tasks(graph, 'wordpress_wordpressConnectToPHPPhp_post_configure_target', 'php_configured', custom_context)
    link_tasks(graph, 'apache_hostedOnServer_add_target', 'apache_started', custom_context)
    link_tasks(graph, 'apache_hostedOnServer_add_target', 'Server_install', custom_context)
    link_tasks(graph, 'mysql_create', '_a4c_init_mysql', custom_context)
    link_tasks(graph, 'apache_started', 'apache_start', custom_context)
    link_tasks(graph, 'wordpress_configuring', 'wordpress_created', custom_context)
    link_tasks(graph, 'wordpress_configuring', 'mysql_started', custom_context)
    link_tasks(graph, 'wordpress_configuring', 'php_started', custom_context)
    link_tasks(graph, 'wordpress_configure', 'wordpress_wordpressConnectToMysqlMysql_pre_configure_source', custom_context)
    link_tasks(graph, 'wordpress_configure', 'wordpress_wordpressConnectToPHPPhp_pre_configure_source', custom_context)
    link_tasks(graph, 'wordpress_configure', 'wordpress_wordpressHostedOnApacheApache_pre_configure_source', custom_context)
    link_tasks(graph, 'wordpress_configure', 'wordpress_configuring', custom_context)
    link_tasks(graph, 'wordpress_start', 'wordpress_wordpressConnectToPHPPhp_post_configure_source', custom_context)
    link_tasks(graph, 'wordpress_start', 'wordpress_wordpressConnectToMysqlMysql_post_configure_source', custom_context)
    link_tasks(graph, 'wordpress_start', 'wordpress_starting', custom_context)
    link_tasks(graph, 'wordpress_start', 'wordpress_wordpressHostedOnApacheApache_post_configure_source', custom_context)
def _a4c_uninstall(ctx, graph, custom_context):
    #  following code can be pasted in src/test/python/workflows/tasks.py for simulation
    custom_context.add_customized_wf_node('wordpress')
    custom_context.add_customized_wf_node('apache')
    custom_context.add_customized_wf_node('apache')
    custom_context.add_customized_wf_node('php')
    custom_context.add_customized_wf_node('php')
    custom_context.add_customized_wf_node('php')
    custom_context.add_customized_wf_node('apache')
    custom_context.add_customized_wf_node('apache')
    custom_context.add_customized_wf_node('mysql')
    custom_context.add_customized_wf_node('php')
    custom_context.add_customized_wf_node('mysql')
    custom_context.add_customized_wf_node('wordpress')
    custom_context.add_customized_wf_node('wordpress')
    custom_context.add_customized_wf_node('mysql')
    custom_context.add_customized_wf_node('mysql')
    custom_context.add_customized_wf_node('wordpress')
    set_state_task(ctx, graph, 'wordpress', 'stopping', 'wordpress_stopping', custom_context)
    set_state_task(ctx, graph, 'apache', 'deleted', 'apache_deleted', custom_context)
    relationship_operation_task(graph, 'mysql', 'DataBase', 'cloudify.interfaces.relationship_lifecycle.unlink', 'SOURCE', 'mysql_hostedOnDataBase_remove_target', custom_context)
    custom_context.register_native_delegate_wf_step('NetPub', 'NetPub_uninstall')
    set_state_task(ctx, graph, 'apache', 'stopping', 'apache_stopping', custom_context)
    set_state_task(ctx, graph, 'php', 'deleting', 'php_deleting', custom_context)
    operation_task(ctx, graph, 'apache', 'cloudify.interfaces.lifecycle.stop', 'apache_stop', custom_context)
    custom_context.register_native_delegate_wf_step('InternalNetwork', 'InternalNetwork_uninstall')
    relationship_operation_task(graph, 'wordpress', 'apache', 'cloudify.interfaces.relationship_lifecycle.unlink', 'SOURCE', 'wordpress_wordpressHostedOnApacheApache_remove_target', custom_context)
    relationship_operation_task(graph, 'php', 'Server', 'cloudify.interfaces.relationship_lifecycle.unlink', 'SOURCE', 'php_hostedOnServer_remove_target', custom_context)
    set_state_task(ctx, graph, 'php', 'stopped', 'php_stopped', custom_context)
    operation_task(ctx, graph, 'wordpress', 'cloudify.interfaces.lifecycle.delete', 'wordpress_delete', custom_context)
    operation_task(ctx, graph, 'apache', 'cloudify.interfaces.lifecycle.delete', 'apache_delete', custom_context)
    operation_task(ctx, graph, 'php', 'cloudify.interfaces.lifecycle.delete', 'php_delete', custom_context)
    set_state_task(ctx, graph, 'php', 'deleted', 'php_deleted', custom_context)
    set_state_task(ctx, graph, 'apache', 'deleting', 'apache_deleting', custom_context)
    operation_task(ctx, graph, 'mysql', 'cloudify.interfaces.lifecycle.stop', 'mysql_stop', custom_context)
    relationship_operation_task(graph, 'wordpress', 'php', 'cloudify.interfaces.relationship_lifecycle.unlink', 'SOURCE', 'wordpress_wordpressConnectToPHPPhp_remove_target', custom_context)
    relationship_operation_task(graph, 'wordpress', 'apache', 'cloudify.interfaces.relationship_lifecycle.unlink', 'TARGET', 'wordpress_wordpressHostedOnApacheApache_remove_source', custom_context)
    relationship_operation_task(graph, 'wordpress', 'php', 'cloudify.interfaces.relationship_lifecycle.unlink', 'TARGET', 'wordpress_wordpressConnectToPHPPhp_remove_source', custom_context)
    relationship_operation_task(graph, 'apache', 'Server', 'cloudify.interfaces.relationship_lifecycle.unlink', 'SOURCE', 'apache_hostedOnServer_remove_target', custom_context)
    operation_task(ctx, graph, 'mysql', 'cloudify.interfaces.lifecycle.delete', 'mysql_delete', custom_context)
    relationship_operation_task(graph, 'wordpress', 'mysql', 'cloudify.interfaces.relationship_lifecycle.unlink', 'TARGET', 'wordpress_wordpressConnectToMysqlMysql_remove_source', custom_context)
    relationship_operation_task(graph, 'wordpress', 'mysql', 'cloudify.interfaces.relationship_lifecycle.unlink', 'SOURCE', 'wordpress_wordpressConnectToMysqlMysql_remove_target', custom_context)
    set_state_task(ctx, graph, 'apache', 'stopped', 'apache_stopped', custom_context)
    operation_task(ctx, graph, 'wordpress', 'cloudify.interfaces.lifecycle.stop', 'wordpress_stop', custom_context)
    custom_context.register_native_delegate_wf_step('Server', 'Server_uninstall')
    set_state_task(ctx, graph, 'mysql', 'stopping', 'mysql_stopping', custom_context)
    set_state_task(ctx, graph, 'php', 'stopping', 'php_stopping', custom_context)
    operation_task(ctx, graph, 'php', 'cloudify.interfaces.lifecycle.stop', 'php_stop', custom_context)
    custom_context.register_native_delegate_wf_step('DataBase', 'DataBase_uninstall')
    set_state_task(ctx, graph, 'mysql', 'stopped', 'mysql_stopped', custom_context)
    set_state_task(ctx, graph, 'wordpress', 'stopped', 'wordpress_stopped', custom_context)
    set_state_task(ctx, graph, 'wordpress', 'deleting', 'wordpress_deleting', custom_context)
    set_state_task(ctx, graph, 'mysql', 'deleting', 'mysql_deleting', custom_context)
    set_state_task(ctx, graph, 'mysql', 'deleted', 'mysql_deleted', custom_context)
    set_state_task(ctx, graph, 'wordpress', 'deleted', 'wordpress_deleted', custom_context)
    generate_native_node_workflows(ctx, graph, custom_context, 'uninstall')
    link_tasks(graph, 'apache_deleted', 'apache_delete', custom_context)
    link_tasks(graph, 'apache_stopping', 'wordpress_deleted', custom_context)
    link_tasks(graph, 'php_deleting', 'php_stopped', custom_context)
    link_tasks(graph, 'apache_stop', 'wordpress_wordpressHostedOnApacheApache_remove_source', custom_context)
    link_tasks(graph, 'apache_stop', 'wordpress_wordpressHostedOnApacheApache_remove_target', custom_context)
    link_tasks(graph, 'apache_stop', 'apache_hostedOnServer_remove_target', custom_context)
    link_tasks(graph, 'apache_stop', 'apache_stopping', custom_context)
    link_tasks(graph, 'php_stopped', 'php_stop', custom_context)
    link_tasks(graph, 'wordpress_delete', 'wordpress_deleting', custom_context)
    link_tasks(graph, 'apache_delete', 'apache_deleting', custom_context)
    link_tasks(graph, 'php_delete', 'php_deleting', custom_context)
    link_tasks(graph, 'php_deleted', 'php_delete', custom_context)
    link_tasks(graph, 'apache_deleting', 'apache_stopped', custom_context)
    link_tasks(graph, 'mysql_stop', 'mysql_stopping', custom_context)
    link_tasks(graph, 'mysql_stop', 'mysql_hostedOnDataBase_remove_target', custom_context)
    link_tasks(graph, 'mysql_stop', 'wordpress_wordpressConnectToMysqlMysql_remove_source', custom_context)
    link_tasks(graph, 'mysql_stop', 'wordpress_wordpressConnectToMysqlMysql_remove_target', custom_context)
    link_tasks(graph, 'mysql_delete', 'mysql_deleting', custom_context)
    link_tasks(graph, 'apache_stopped', 'apache_stop', custom_context)
    link_tasks(graph, 'wordpress_stop', 'wordpress_stopping', custom_context)
    link_tasks(graph, 'wordpress_stop', 'wordpress_wordpressConnectToPHPPhp_remove_target', custom_context)
    link_tasks(graph, 'wordpress_stop', 'wordpress_wordpressHostedOnApacheApache_remove_source', custom_context)
    link_tasks(graph, 'wordpress_stop', 'wordpress_wordpressHostedOnApacheApache_remove_target', custom_context)
    link_tasks(graph, 'wordpress_stop', 'wordpress_wordpressConnectToPHPPhp_remove_source', custom_context)
    link_tasks(graph, 'wordpress_stop', 'wordpress_wordpressConnectToMysqlMysql_remove_source', custom_context)
    link_tasks(graph, 'wordpress_stop', 'wordpress_wordpressConnectToMysqlMysql_remove_target', custom_context)
    link_tasks(graph, 'Server_uninstall', 'apache_deleted', custom_context)
    link_tasks(graph, 'Server_uninstall', 'php_hostedOnServer_remove_target', custom_context)
    link_tasks(graph, 'Server_uninstall', 'apache_hostedOnServer_remove_target', custom_context)
    link_tasks(graph, 'Server_uninstall', 'php_deleted', custom_context)
    link_tasks(graph, 'mysql_stopping', 'wordpress_stopped', custom_context)
    link_tasks(graph, 'php_stopping', 'wordpress_stopped', custom_context)
    link_tasks(graph, 'php_stop', 'php_stopping', custom_context)
    link_tasks(graph, 'php_stop', 'wordpress_wordpressConnectToPHPPhp_remove_target', custom_context)
    link_tasks(graph, 'php_stop', 'php_hostedOnServer_remove_target', custom_context)
    link_tasks(graph, 'php_stop', 'wordpress_wordpressConnectToPHPPhp_remove_source', custom_context)
    link_tasks(graph, 'DataBase_uninstall', 'mysql_hostedOnDataBase_remove_target', custom_context)
    link_tasks(graph, 'DataBase_uninstall', 'mysql_deleted', custom_context)
    link_tasks(graph, 'mysql_stopped', 'mysql_stop', custom_context)
    link_tasks(graph, 'wordpress_stopped', 'wordpress_stop', custom_context)
    link_tasks(graph, 'wordpress_deleting', 'wordpress_stopped', custom_context)
    link_tasks(graph, 'mysql_deleting', 'mysql_stopped', custom_context)
    link_tasks(graph, 'mysql_deleted', 'mysql_delete', custom_context)
    link_tasks(graph, 'wordpress_deleted', 'wordpress_delete', custom_context)
def _a4c_start(ctx, graph, custom_context):
    #  following code can be pasted in src/test/python/workflows/tasks.py for simulation
    custom_context.register_native_delegate_wf_step('Server', 'Server_start')
    custom_context.register_native_delegate_wf_step('DataBase', 'DataBase_start')
    custom_context.register_native_delegate_wf_step('NetPub', 'NetPub_start')
    custom_context.register_native_delegate_wf_step('InternalNetwork', 'InternalNetwork_start')
    generate_native_node_workflows(ctx, graph, custom_context, 'start')
def _a4c_stop(ctx, graph, custom_context):
    #  following code can be pasted in src/test/python/workflows/tasks.py for simulation
    custom_context.register_native_delegate_wf_step('InternalNetwork', 'InternalNetwork_stop')
    custom_context.register_native_delegate_wf_step('DataBase', 'DataBase_stop')
    custom_context.register_native_delegate_wf_step('NetPub', 'NetPub_stop')
    custom_context.register_native_delegate_wf_step('Server', 'Server_stop')
    generate_native_node_workflows(ctx, graph, custom_context, 'stop')

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
      scaled_node = ctx.get_node(node_id)
      if not scaled_node:
          raise ValueError("Node {0} doesn't exist".format(scalable_entity_name))
      if not is_host_node(scaled_node) and not is_kubernetes_node(scaled_node):
          raise ValueError("Node {0} is not a host. This workflow can only scale hosts".format(scalable_entity_name))
      if delta == 0:
          ctx.logger.info('delta parameter is 0, so no scaling will take place.')
          return
      curr_num_instances = scaled_node.number_of_instances
      planned_num_instances = curr_num_instances + delta
      scale_id = scaled_node.id
      scalable_entity_name = scale_id

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
    #types.append('alien.nodes.openstack.PrivateNetwork')
    #types.append('alien.nodes.PrivateNetwork')
    #types.append('tosca.nodes.Network')
    #types.append('tosca.nodes.Root')
    #node_InternalNetwork = _build_node(ctx, 'InternalNetwork', types, 1)
    #types = []
    #types.append('org.alien4cloud.nodes.Apache')
    #types.append('tosca.nodes.WebServer')
    #types.append('tosca.nodes.SoftwareComponent')
    #types.append('tosca.nodes.Root')
    #node_apache = _build_node(ctx, 'apache', types, 1)
    #types = []
    #types.append('alien.nodes.openstack.Compute')
    #types.append('tosca.nodes.Compute')
    #types.append('tosca.nodes.Root')
    #node_Server = _build_node(ctx, 'Server', types, 1)
    #types = []
    #types.append('org.alien4cloud.nodes.Wordpress')
    #types.append('tosca.nodes.WebApplication')
    #types.append('tosca.nodes.Root')
    #node_wordpress = _build_node(ctx, 'wordpress', types, 1)
    #types = []
    #types.append('org.alien4cloud.nodes.PHP')
    #types.append('tosca.nodes.SoftwareComponent')
    #types.append('tosca.nodes.Root')
    #node_php = _build_node(ctx, 'php', types, 1)
    #types = []
    #types.append('org.alien4cloud.nodes.Mysql')
    #types.append('org.alien4cloud.nodes.AbstractMysql')
    #types.append('tosca.nodes.Database')
    #types.append('tosca.nodes.Root')
    #node_mysql = _build_node(ctx, 'mysql', types, 1)
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
    #node_DataBase = _build_node(ctx, 'DataBase', types, 1)
    #_add_relationship(node_apache, node_Server)
    #_add_relationship(node_Server, node_NetPub)
    #_add_relationship(node_Server, node_InternalNetwork)
    #_add_relationship(node_wordpress, node_apache)
    #_add_relationship(node_wordpress, node_mysql)
    #_add_relationship(node_wordpress, node_php)
    #_add_relationship(node_php, node_Server)
    #_add_relationship(node_mysql, node_DataBase)
    #_add_relationship(node_DataBase, node_InternalNetwork)
