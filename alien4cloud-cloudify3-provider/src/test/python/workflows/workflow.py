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


class WfEvent(object):

    def __init__(self, instance_id, stage, step_id):
        self.instance_id = instance_id
        self.stage = stage
        self.step_id = step_id


def build_wf_event(wf_event):
    return "wfe:" + json.dumps(wf_event.__dict__)


# def _build_wf_event(wf_event):
    # return "wfe:" + json.dumps(wf_event.__dict__).replace("\\", "").replace("\"{", "{").replace("}\"", "}")