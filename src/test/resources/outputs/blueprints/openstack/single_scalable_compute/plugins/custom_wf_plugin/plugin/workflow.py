import json


# class Activity(object):
#
#     def __init__(self, name):
#         self.name = name
#
#
# class SetActivity(Activity):
#
#     def __init__(self, stat_name):
#         Activity.__init__(self, "set")
#         self.stat_name = stat_name
#
#
# class CallActivity(Activity):
#
#     def __init__(self, fqn):
#         Activity.__init__(self, "call")
#         self.fqn = fqn
#
#
# class Step(object):
#
#     def __init__(self, name, node_id, host_id, activity):
#         self.name = name
#         self.node_id = node_id
#         self.host_id = host_id
#         self.activity = json.dumps(activity.__dict__)

class WfStartEvent(object):

    def __init__(self, workflow_name, subworkflow=None):
        self.workflow_name = workflow_name
        self.subworkflow = subworkflow


class WfEvent(object):

    def __init__(self, stage, step_id):
        self.stage = stage
        self.step_id = step_id


class PersistentResourceEvent(object):

    def __init__(self, persistent_resource_id, persistent_alien_attribute):
        self.persistent_resource_id = persistent_resource_id
        self.persistent_alien_attribute = persistent_alien_attribute

def build_pre_event(event):
    return json.dumps(event.__dict__)


# def _build_wf_event(wf_event):
    # return "wfe:" + json.dumps(wf_event.__dict__).replace("\\", "").replace("\"{", "{").replace("}\"", "}")
