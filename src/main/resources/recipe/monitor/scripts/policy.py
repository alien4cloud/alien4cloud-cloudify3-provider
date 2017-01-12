import sys
from cloudify_rest_client import CloudifyClient
from influxdb.influxdb08 import InfluxDBClient
from influxdb.influxdb08.client import InfluxDBClientError

from os import utime
from os import getpid
from os import path

import json
import time
import datetime
import base64

# check against influxdb which nodes are available CPUtotal
# change status of missing nodes comparing to the node_instances that are taken from cloudify
# do it only for compute nodes

rest_conf_file = '/opt/manager/rest-security.conf'

CLOUDIFY_USER_KEY = 'cloudify_user'
CLOUDIFY_PASSWORD_KEY = 'cloudify_password'


def _read_rest_conf():
    if not path.isfile(rest_conf_file):
        return None
    with open(rest_conf_file) as config:
        # The rest_conf_file is not a valide json format.
        # We should read it with a yaml.loads() but the yaml library is not include in the mgmworker env
        # So in order not to bother with yaml library, we just do this little trick which will suffice at the moment
        data = config.read().replace("'",'"').replace('True', '"True"').replace('False','"False"')
        rest_conf = json.loads(data)
        return rest_conf

def is_ssl_enabled():
    # Since this script is executed from the manager machine, it searches directly into the rest-security configuration file to find out if the rest is secured.
    rest_conf = _read_rest_conf()
    if 'ssl' in rest_conf and 'enabled' in rest_conf['ssl'] and rest_conf['ssl']['enabled'] == True:
        return True
    return False


def get_cloudify_credentials():
    rest_conf = _read_rest_conf()
    if 'enabled' in rest_conf and rest_conf['enabled']:
        return { CLOUDIFY_USER_KEY : rest_conf['admin_username'], CLOUDIFY_PASSWORD_KEY : rest_conf['admin_password'] }
    return None


def log(message, level='INFO'):
    print "{date} [{level}] {message}".format(date=str(datetime.datetime.now()), level=level, message=message)


def check_liveness(nodes_to_monitor,depl_id):
    cloudify_credentials = get_cloudify_credentials()
    if cloudify_credentials:
        headers = {'Authorization': 'Basic ' + base64.b64encode(cloudify_credentials[CLOUDIFY_USER_KEY] + ':' + cloudify_credentials[CLOUDIFY_PASSWORD_KEY])}
    if is_ssl_enabled():
        c = CloudifyClient(host='localhost', port=8101, protocol='https', trust_all=True, headers=headers)
    else:
        c = CloudifyClient(host='localhost', headers=headers)

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
                      if cloudify_credentials:
                          cloudify_token = c.tokens.get();
                          params['cloudify_token'] = cloudify_token['value']
                      log ('Calling Auto-healing workflow for container instance {0}'.format(instance.id))
                      c.executions.start(depl_id, 'a4c_heal', params)
                  else:
                      log ('pendding executions on the deployment...waiting for their end before calling heal workfllow...')
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
