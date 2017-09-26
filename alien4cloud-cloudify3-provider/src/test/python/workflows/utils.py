from handlers import host_post_start
from handlers import host_pre_stop
from handlers import _set_send_node_event_on_error_handler
from workflow import WfEvent
from workflow import build_wf_event


def _get_nodes_instances(ctx, node_id):
    instances = []
    for node in ctx.nodes:
        for instance in node.instances:
            if instance.node_id == node_id:
                instances.append(instance)
    return instances


def _get_all_nodes_instances(ctx):
    node_instances = set()
    for node in ctx.nodes:
        for instance in node.instances:
            node_instances.add(instance)
    return node_instances


def set_state_task(ctx, graph, node_id, state_name, step_id, custom_context):
    sequence = _set_state_task(ctx, graph, node_id, state_name, step_id)
    if sequence is not None:
        sequence.name = step_id
        # start = ctx.internal.send_workflow_event(event_type='custom_workflow', message=build_wf_event(WfEvent(step_id, "in")))
        # sequence.set_head(start)
        # end = ctx.internal.send_workflow_event(event_type='custom_workflow', message=build_wf_event(WfEvent(step_id, "ok")))
        # sequence.add(end)
        custom_context.tasks[step_id] = sequence


def _set_state_task(ctx, graph, node_id, state_name, step_id):
    sequence = None
    instances = _get_nodes_instances(ctx, node_id)
    instance_count = len(instances)
    if instance_count == 1:
        instance = instances[0]
        sequence = set_state_task_for_instance(graph, node_id, instance, state_name, step_id)
    elif instance_count > 1:
        fork = ForkjoinWrapper(graph)
        for instance in instances:
            fork.add(set_state_task_for_instance(graph, node_id, instance, state_name, step_id))
        msg = "state {0} on all {1} node instances".format(state_name, node_id)
        sequence = forkjoin_sequence(graph, fork, instances[0], msg)
    return sequence


def set_state_task_for_instance(graph, node_id, instance, state_name, step_id):
    task = TaskSequenceWrapper(graph)
    msg = build_wf_event(WfEvent(instance.id, "in", step_id))
    task.add(instance.send_event(msg))
    task.add(instance.set_state(state_name))
    msg = build_wf_event(WfEvent(instance.id, "ok", step_id))
    task.add(instance.send_event(msg))
    return task


def operation_task(ctx, graph, node_id, operation_fqname, step_id, custom_context):
    sequence = _operation_task(ctx, graph, node_id, operation_fqname, step_id, custom_context)
    if sequence is not None:
        sequence.name = step_id
        # start = ctx.internal.send_workflow_event(event_type='custom_workflow', message=build_wf_event(WfEvent(step_id, "in")))
        # sequence.set_head(start)
        # end = ctx.internal.send_workflow_event(event_type='custom_workflow', message=build_wf_event(WfEvent(step_id, "ok")))
        # sequence.add(end)
        custom_context.tasks[step_id] = sequence


def _operation_task(ctx, graph, node_id, operation_fqname, step_id, custom_context):
    sequence = None
    instances = _get_nodes_instances(ctx, node_id)
    first_instance = None
    instance_count = len(instances)
    if instance_count == 1:
        instance = instances[0]
        first_instance = instance
        sequence = operation_task_for_instance(ctx, graph, node_id, instance, operation_fqname, step_id, custom_context)
    elif instance_count > 1:
        fork = ForkjoinWrapper(graph)
        for instance in instances:
            instance_task = operation_task_for_instance(ctx, graph, node_id, instance, operation_fqname, step_id, custom_context)
            fork.add(instance_task)
        msg = "operation {0} on all {1} node instances".format(operation_fqname, node_id)
        first_instance = instances[0]
        sequence = forkjoin_sequence(graph, fork, first_instance, msg)
    return sequence


def count_relationships(instance):
    relationship_count = 0
    for relationship in instance.relationships:
        relationship_count += 1
    return relationship_count

def operation_task_for_instance(ctx, graph, node_id, instance, operation_fqname, step_id, custom_context):
    sequence = TaskSequenceWrapper(graph)
    msg = build_wf_event(WfEvent(instance.id, "in", step_id))
    sequence.add(instance.send_event(msg))
    relationship_count = count_relationships(instance)
    if operation_fqname == 'cloudify.interfaces.lifecycle.start':
        sequence.add(instance.execute_operation(operation_fqname))
        if _is_host_node(instance):
            sequence.add(*host_post_start(ctx, instance))
        fork = ForkjoinWrapper(graph)
        fork.add(instance.execute_operation('cloudify.interfaces.monitoring.start'))
        if relationship_count > 0:
            for relationship in instance.relationships:
                fork.add(relationship.execute_source_operation('cloudify.interfaces.relationship_lifecycle.establish'))
                fork.add(relationship.execute_target_operation('cloudify.interfaces.relationship_lifecycle.establish'))
        sequence.add(
            instance.send_event("Start monitoring on node '{0}' instance '{1}'".format(node_id, instance.id)),
            forkjoin_sequence(graph, fork, instance, "establish")
        )
    elif operation_fqname == 'cloudify.interfaces.lifecycle.configure':
        as_target_relationships = custom_context.relationship_targets.get(instance.id, set())
        if relationship_count > 0 or len(as_target_relationships) > 0:
            preconfigure_tasks = ForkjoinWrapper(graph)
            for relationship in instance.relationships:
                preconfigure_tasks.add(relationship.execute_source_operation('cloudify.interfaces.relationship_lifecycle.preconfigure'))
            for relationship in as_target_relationships:
                preconfigure_tasks.add(relationship.execute_target_operation('cloudify.interfaces.relationship_lifecycle.preconfigure'))
            sequence.add(forkjoin_sequence(graph, preconfigure_tasks, instance, "preconf for {0}".format(instance.id)))
        sequence.add(instance.execute_operation(operation_fqname))
        if relationship_count > 0 or len(as_target_relationships) > 0:
            postconfigure_tasks = ForkjoinWrapper(graph)
            for relationship in instance.relationships:
                postconfigure_tasks.add(relationship.execute_source_operation('cloudify.interfaces.relationship_lifecycle.postconfigure'))
            for relationship in as_target_relationships:
                task = relationship.execute_target_operation('cloudify.interfaces.relationship_lifecycle.postconfigure')
                _set_send_node_event_on_error_handler(task, instance, "Error occurred while postconfiguring node as target for relationship {0} - ignoring...".format(relationship))
                postconfigure_tasks.add(task)
            msg = "postconf for {0}".format(instance.id)
            sequence.add(forkjoin_sequence(graph, postconfigure_tasks, instance, msg))
    elif operation_fqname == 'cloudify.interfaces.lifecycle.stop':
        if _is_host_node(instance):
            sequence.add(*host_pre_stop(instance))
        task = instance.execute_operation(operation_fqname)
        _set_send_node_event_on_error_handler(task, instance, "Error occurred while stopping node - ignoring...")
        sequence.add(task)
        # now call unlink onto relations' target
        if relationship_count > 0:
            fork = ForkjoinWrapper(graph)
            for relationship in instance.relationships:
                unlink_task_source = relationship.execute_source_operation('cloudify.interfaces.relationship_lifecycle.unlink')
                _set_send_node_event_on_error_handler(unlink_task_source, instance, "Error occurred while unlinking node from target {0} - ignoring...".format(relationship.target_id))
                fork.add(unlink_task_source)
                unlink_task_target = relationship.execute_target_operation('cloudify.interfaces.relationship_lifecycle.unlink')
                _set_send_node_event_on_error_handler(unlink_task_target, instance, "Error occurred while unlinking node from target {0} - ignoring...".format(relationship.target_id))
                fork.add(unlink_task_target)
            sequence.add(forkjoin_sequence(graph, fork, instance, "unlink"))
    elif operation_fqname == 'cloudify.interfaces.lifecycle.delete':
        task = instance.execute_operation(operation_fqname)
        _set_send_node_event_on_error_handler(task, instance, "Error occurred while deleting node - ignoring...")
        sequence.add(task)
    else:
        # the default behavior : just do the job
        sequence.add(instance.execute_operation(operation_fqname))
    msg = build_wf_event(WfEvent(instance.id, "ok", step_id))
    sequence.add(instance.send_event(msg))
    return sequence


def forkjoin_sequence(graph, forkjoin_wrapper, instance, label):
    sequence = TaskSequenceWrapper(graph)
    sequence.add(instance.send_event("forking: {0} instance '{1}'".format(label, instance.id)))
    sequence.add(forkjoin_wrapper)
    sequence.add(instance.send_event("joining: {0} instance '{1}'".format(label, instance.id)))
    return sequence


def link_tasks(graph, source_id, target_id, custom_context):
    sources = custom_context.tasks.get(source_id, None)
    targets = custom_context.tasks.get(target_id, None)
    _link_tasks(graph, sources, targets)


def _link_tasks(graph, sources, targets):
    if sources is None:
        return
    if isinstance(sources, TaskSequenceWrapper) or isinstance(sources, ForkjoinWrapper):
        sources = sources.first_tasks
    else:
        sources = [sources]
    if targets is None:
        return
    if isinstance(targets, TaskSequenceWrapper) or isinstance(targets, ForkjoinWrapper):
        targets = targets.last_tasks
    else:
        targets = [targets]
    for source in sources:
        for target in targets:
            graph.add_dependency(source, target)


def _is_host_node(node_instance):
    return 'cloudify.nodes.Compute' in node_instance.node.type_hierarchy


# def _relationship_operations(node_instance, operation):
#     tasks_with_targets = _relationship_operations_with_targets(
#         node_instance, operation)
#     return [task for task, _ in tasks_with_targets]
#
#
# def _relationship_operations_with_targets(node_instance, operation):
#     tasks = []
#     for relationship in node_instance.relationships:
#         tasks += _relationship_operations_with_target(relationship, operation)
#     return tasks
#
#
# def _relationship_operations_with_target(relationship, operation):
#     return [
#         (relationship.execute_source_operation(operation),
#          relationship.target_id)
#     ]

def generate_native_node_workflows(ctx, graph, custom_context, stage):
    native_nodes = custom_context.get_native_nodes(ctx)
    # for each native node we build a sequence of operations
    native_sequences = {}
    for node in native_nodes:
        sequence = _generate_native_node_sequence(ctx, graph, node, stage, custom_context)
        if sequence is not None:
            native_sequences[node.id] = sequence
    # we explore the relations between native nodes to orchestrate tasks 'a la' cloudify
    for node in native_nodes:
        sequence = native_sequences.get(node.id, None)
        if sequence is not None:
            for relationship in node.relationships:
                target_id = relationship.target_id
                target_sequence = native_sequences.get(target_id, None)
                if target_sequence is not None:
                    if stage == 'install':
                        _link_tasks(graph, sequence, target_sequence)
                    elif stage == 'uninstall':
                        _link_tasks(graph, target_sequence, sequence)
    # when posible, associate the native sequences with the corresponding delegate workflow step
    for node in native_nodes:
        sequence = native_sequences.get(node.id, None)
        if sequence is not None:
            delegate_wf_step = custom_context.delegate_wf_steps.get(node.id, None)
            if delegate_wf_step is not None:
                # the delegate wf step can be associated to a native sequence
                # let's register it in the custom context to make it available for non native tasks links
                custom_context.tasks[delegate_wf_step] = sequence
                # and remove it from the original map
                del custom_context.delegate_wf_steps[node.id]
                # this sequence is now associated with a delegate wf step, just remove it from the map
                del native_sequences[node.id]
    # iterate through remaining delegate_wf_steps
    # the remaining ones are those that are not associated with a native sequence
    # at this stage, we are not able to associate these remaining delegate wf steps (we don't have
    # a bridge between java world model and python world model (cfy blueprint) )
    # so: we fork all remaining sequences and we associate the fork-join to all remaining delegate step
    if len(custom_context.delegate_wf_steps) > 0 and len(native_sequences) > 0:
        # let's create a fork join with remaining sequences
        fork = ForkjoinWrapper(graph)
        for sequence in native_sequences.itervalues():
            fork.add(sequence)
        for stepId in custom_context.delegate_wf_steps.itervalues():
            # we register this fork using the delegate wf step id
            # so it can be referenced later to link non native tasks
            custom_context.tasks[stepId] = fork


def _generate_native_node_sequence(ctx, graph, node, stage, custom_context):
    if stage == 'install':
        return _generate_native_node_sequence_install(ctx, graph, node, custom_context)
    elif stage == 'uninstall':
        return _generate_native_node_sequence_uninstall(ctx, graph, node, custom_context)
    else:
        return None


def _generate_native_node_sequence_install(ctx, graph, node, custom_context):
    sequence = TaskSequenceWrapper(graph)
    sequence.add(_set_state_task(ctx, graph, node.id, 'initial', '_{0}_initial'.format(node.id)))
    sequence.add(_set_state_task(ctx, graph, node.id, 'creating', '_{0}_creating'.format(node.id)))
    sequence.add(_operation_task(ctx, graph, node.id, 'cloudify.interfaces.lifecycle.create', '_create_{0}'.format(node.id), custom_context))
    sequence.add(_set_state_task(ctx, graph, node.id, 'created', '_{0}_created'.format(node.id)))
    sequence.add(_set_state_task(ctx, graph, node.id, 'configuring', '_{0}_configuring'.format(node.id)))
    sequence.add(_operation_task(ctx, graph, node.id, 'cloudify.interfaces.lifecycle.configure', '_configure_{0}'.format(node.id), custom_context))
    sequence.add(_set_state_task(ctx, graph, node.id, 'configured', '_{0}_configured'.format(node.id)))
    sequence.add(_set_state_task(ctx, graph, node.id, 'starting', '_{0}_starting'.format(node.id)))
    sequence.add(_operation_task(ctx, graph, node.id, 'cloudify.interfaces.lifecycle.start', '_start_{0}'.format(node.id), custom_context))
    sequence.add(_set_state_task(ctx, graph, node.id, 'started', '_{0}_started'.format(node.id)))
    return sequence


def _generate_native_node_sequence_uninstall(ctx, graph, node, custom_context):
    sequence = TaskSequenceWrapper(graph)
    sequence.add(_set_state_task(ctx, graph, node.id, 'stopping', '_{0}_stopping'.format(node.id)))
    sequence.add(_operation_task(ctx, graph, node.id, 'cloudify.interfaces.lifecycle.stop', '_stop_{0}'.format(node.id), custom_context))
    sequence.add(_set_state_task(ctx, graph, node.id, 'stopped', '_{0}_stopped'.format(node.id)))
    sequence.add(_set_state_task(ctx, graph, node.id, 'deleting', '_{0}_deleting'.format(node.id)))
    sequence.add(_operation_task(ctx, graph, node.id, 'cloudify.interfaces.lifecycle.delete', '_delete_{0}'.format(node.id), custom_context))
    sequence.add(_set_state_task(ctx, graph, node.id, 'deleted', '_{0}_deleted'.format(node.id)))
    return sequence


class ForkjoinWrapper(object):

    def __init__(self, graph, name=""):
        self.graph = graph
        self.first_tasks = []
        self.last_tasks = []
        self.name = name

    def add(self, *tasks):
        for element in tasks:
            if isinstance(element, ForkjoinWrapper):
                self.first_tasks.extend(element.first_tasks)
                self.last_tasks.extend(element.last_tasks)
            elif isinstance(element, TaskSequenceWrapper):
                self.first_tasks.extend(element.first_tasks)
                self.last_tasks.extend(element.last_tasks)
            else:
                self.first_tasks.append(element)
                self.last_tasks.append(element)
                self.graph.add_task(element)



class TaskSequenceWrapper(object):

    def __init__(self, graph, name=""):
        self.graph = graph
        self.first_tasks = None
        self.last_tasks = None
        self.name = name

    def set_head(self, task):
        if self.first_tasks is None:
            self.add(task)
        else:
            self.graph.add_task(task)
            for next_task in self.first_tasks:
                self.graph.add_dependency(next_task, task)
            self.first_tasks = [task]

    def add(self, *tasks):
        for element in tasks:
            tasks_head = None
            tasks_queue = None
            if isinstance(element, ForkjoinWrapper):
                tasks_head = element.first_tasks
                tasks_queue = element.last_tasks
            elif isinstance(element, TaskSequenceWrapper):
                tasks_head = element.first_tasks
                tasks_queue = element.last_tasks
            else:
                tasks_head = [element]
                tasks_queue = tasks_head
                self.graph.add_task(element)
            for task in tasks_head:
                if self.last_tasks is not None:
                    for last_task in self.last_tasks:
                        self.graph.add_dependency(task, last_task)
            if tasks_head is not None:
                if self.first_tasks is None:
                    self.first_tasks = tasks_head
            if tasks_queue is not None:
                self.last_tasks = tasks_queue


class CustomContext(object):
    def __init__(self, ctx):
        self.tasks = {}
        self.relationship_targets = {}
        # a set of nodeId for which wf is customized (designed using a4c)
        self.customized_wf_nodes = set()
        # a dict of nodeId -> stepId : nodes for which we need to manage the wf ourself
        self.delegate_wf_steps = {}
        self.__build_relationship_targets(ctx)

    '''
    Build a map containing all the relationships that target a given node instance :
    - key is target_id (a node instance id)
    - value is a set of relationships (all relationship that target this node)
    '''
    def __build_relationship_targets(self, ctx):
        node_instances = _get_all_nodes_instances(ctx)
        for node_instance in node_instances:
            ctx.internal.send_workflow_event(
                    event_type='other',
                    message="found an instance of {0} : {1}".format(node_instance.node_id, node_instance.id))
            for relationship in node_instance.relationships:
                target_relationships = self.relationship_targets.get(relationship.target_id, None)
                if target_relationships is None:
                    target_relationships = set()
                    self.relationship_targets[relationship.target_id] = target_relationships
                ctx.internal.send_workflow_event(
                        event_type='other',
                        message="found a relationship that targets {0} : {1}".format(relationship.target_id, relationship))
                target_relationships.add(relationship)

    def add_customized_wf_node(self, nodeId):
        self.customized_wf_nodes.add(nodeId)

    # the native node are those for which workflow is not managed by a4c
    def get_native_nodes(self, ctx):
        native_nodes = set()
        for node in ctx.nodes:
            if node.id not in self.customized_wf_nodes:
                native_nodes.add(node)
        return native_nodes

    def register_native_delegate_wf_step(self, nodeId, stepId):
        self.delegate_wf_steps[nodeId] = stepId
