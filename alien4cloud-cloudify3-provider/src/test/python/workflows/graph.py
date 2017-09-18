
def short_name(fqname):
    names = fqname.split(".")
    return names[len(names) - 1]


class Internal(object):

    def __init__(self):
        self.state = None

    def send_workflow_event(self, event_type, message):
        message = message.replace(" ", "_")
        message = message.replace(".", "_")
        message = message.replace("'", "")
        message = message.replace(":", "_")
        message = message.replace("{", "_")
        message = message.replace("}", "_")
        message = message.replace("\"", "_")
        message = message.replace(",", "_")
        task = Task("event_{0}_{1}".format(event_type, message))
        return task


class Context(object):

    def __init__(self):
        self.nodes = []
        self.internal = Internal()


class Node(object):

    def __init__(self, id, types=["default"]):
        self.id = id
        self.instances = []
        self.type_hierarchy = types
        self.properties = {}
        self._relationships = {}

    def add_relationship(self, relationship):
        self._relationships[relationship.target_id] = relationship

    @property
    def relationships(self):
        """The node relationships"""
        return self._relationships.itervalues()

class Instance(object):

    def __init__(self, id, node):
        self.id = id
        self.node_id = node.id
        self.node = node
        self.relationships = []

    def execute_operation(self, operation_fqn):
        task = Task("exec_{0}_{1}_{2}".format(self.node_id, self.id, str(operation_fqn).replace(".", "_")))
        return task

    def set_state(self, state_name):
        task = Task("state_{0}_{1}_{2}".format(self.node_id, self.id, state_name))
        return task

    def send_event(self, message):
        msg = message.replace(" ", "_")
        msg = msg.replace(".", "_")
        msg = msg.replace("'", "")
        msg = msg.replace(":", "_")
        msg = msg.replace("{", "_")
        msg = msg.replace("}", "_")
        msg = msg.replace("\"", "_")
        msg = msg.replace(",", "_")        
        task = Task("event_{0}_{1}".format(self.id, msg))
        return task


class RelationshipIntance(object):

    def __init__(self, instance, target):
        self.instance = instance
        # the target node
        self.target = target
        self.target_id = target.id

    def execute_source_operation(self, operation):
        task = Task("rel_src_{0}_to_{1}_{2}".format(self.instance.id, self.target_id, str(operation).replace(".", "_")))
        return task

    def execute_target_operation(self, operation):
        task = Task("rel_tgt_{0}_to_{1}_{2}".format(self.instance.id, self.target_id, str(operation).replace(".", "_")))
        return task


class Relationship(object):

    def __init__(self, target):
        # the target node
        self.target = target
        self.target_id = target.id


class Task(object):

    def __init__(self, name):
        self.name = name
        self.followers = []

    def add_follower(self, follower):
        self.followers.append(follower)


class Graph(object):

    def __init__(self):
        self.tasks = {}

    def add_task(self, task):
        self.tasks[task.name] = task

    def add_dependency(self, target, source):
        self.tasks[source.name].add_follower(target.name)


def print_graph(graph):
    print "----------------"
    print ""
    print "digraph g{"
    for task_key in graph.tasks:
        print "  " + graph.tasks[task_key].name
    for task_key in graph.tasks:
        for follower in graph.tasks[task_key].followers:
            print "  " + graph.tasks[task_key].name + " -> " + follower
    print "}"
    print ""
    print "----------------"
