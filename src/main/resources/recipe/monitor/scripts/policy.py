import sys
from cloudify_rest_client import CloudifyClient
from influxdb.influxdb08 import InfluxDBClient
from influxdb.influxdb08.client import InfluxDBClientError
from cloudify import utils

from os import getpid

import json
import datetime

# check against influxdb which nodes are available CPUtotal
# change status of missing nodes comparing to the node_instances that are taken from cloudify
# do it only for compute nodes

def log(message, level='INFO'):
    print "{date} [{level}] {message}".format(date=str(datetime.datetime.now()), level=level, message=message)


def check_liveness(nodes_to_monitor,depl_id):
    c = CloudifyClient(host=utils.get_manager_ip(),
                       port=utils.get_manager_rest_service_port(),
                       protocol='https',
                       cert=utils.get_local_rest_certificate(),
                       token= utils.get_rest_token(),
                       tenant= utils.get_tenant_name())

    c_influx = InfluxDBClient(host='localhost', port=8086, database='cloudify')
    log ('nodes_to_monitor: {0}'.format(nodes_to_monitor))

    # compare influx data (monitoring) to cloudify desired state

    for node_name in nodes_to_monitor:
        instances=c.node_instances.list(depl_id,node_name)
        for instance in instances:
            q_string='SELECT MEAN(value) FROM /' + depl_id + '\.' + node_name + '\.' + instance.id + '\.cpu_total_system/ GROUP BY time(10s) '\
                   'WHERE  time > now() - 40s'
            log ('query string is {0}'.format(q_string))
            try:
               result=c_influx.query(q_string)
               log ('result is {0}'.format(result))
               if not result:
                  executions=c.executions.list(depl_id)
                  has_pending_execution = False
                  if executions and len(executions)>0:
                      for execution in executions:
                      # log("Execution {0} : {1}".format(execution.id, execution.status))
                          if execution.status not in execution.END_STATES:
                              has_pending_execution = True
                  if not has_pending_execution:
                      log ('Setting state to error for instance {0} and its children'.format(instance.id))
                      update_nodes_tree_state(c, depl_id, instance, 'error')
                      params = {'node_instance_id': instance.id}
                      log ('Calling Auto-healing workflow for container instance {0}'.format(instance.id))
                      c.executions.start(depl_id, 'a4c_heal', params)
                  else:
                      log ('pending executions on the deployment...waiting for the end before calling heal workflow...')
            except InfluxDBClientError as ee:
                log ('DBClienterror {0}'.format(str(ee)), level='ERROR')
                log ('instance id is {0}'.format(instance), level='ERROR')
            except Exception as e:
                log (str(e), level='ERROR')


def update_nodes_tree_state(client,depl_id,instance,state):
    log ('updating instance {0} state to {1}'.format(instance.id, state))
    client.node_instances.update(instance.id, state)
    dep_inst_list = client.node_instances.list(depl_id)
    for inst in dep_inst_list:
        try:
            if inst.relationships:
                for relationship in inst.relationships:
                    target = relationship['target_name']
                    type = relationship['type']
                    if ('contained_in' in str(type)) and (target == instance.node_id):
                        update_nodes_tree_state(client,depl_id,inst,state)
        except Exception as e:
            log(str(e), level='ERROR')


def main(argv):
    log ("argv={0}".format(argv))
    depl_id=argv[2]
    monitoring_dir=argv[3]
    of = open(monitoring_dir+'/pid_file', 'w')
    of.write('%i' % getpid())
    of.close()

    nodes_to_monitor=json.loads(argv[1].replace("'", '"'))
    check_liveness(nodes_to_monitor, depl_id)

if __name__ == '__main__':
    main(sys.argv)
