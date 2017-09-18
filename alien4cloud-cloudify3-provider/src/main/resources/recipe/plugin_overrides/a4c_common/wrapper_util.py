#from os.path import expanduser
from cloudify import ctx
from cloudify.decorators import operation

import os
import fcntl

# Constant for 'use_external_resource' key
USE_EXTERNAL_RESOURCE_KEY = 'use_external_resource'

# Constant to get the real resource id key from Cloudify
A4C_RESOURCE_ID_KEY = '_a4c_resource_id_key'

class Lock:
  def __init__(self, filename):
    self.filename = filename
    self.handle = open(filename, 'w')

  def acquire(self):
    fcntl.flock(self.handle, fcntl.LOCK_EX)

  def release(self):
    fcntl.flock(self.handle, fcntl.LOCK_UN)

  def __del__(self):
    self.handle.close()

def handle_external_resource():
  if ('scale' in ctx.workflow_id):
    # If this is a scaling workflow, force the creation of a volume by setting the 'use_external_resource' to false
    ctx.instance.runtime_properties[USE_EXTERNAL_RESOURCE_KEY] = False
    resource_id_key = ctx.node.properties[A4C_RESOURCE_ID_KEY]
    ctx.instance.runtime_properties[resource_id_key] = None
    ctx.logger.info("[A4C_VOLUME] Scale workflow detected, ignore use_external_resource")
  else:
    # If this is not a scaling workflow, use the default value of the 'use_external_resource' property
    ctx.instance.runtime_properties[USE_EXTERNAL_RESOURCE_KEY] = ctx.node.properties[USE_EXTERNAL_RESOURCE_KEY]

def handle_resource_ids():
  try:
    lock = Lock("/tmp/lock_name_{}_volume.tmp".format(ctx.deployment.id))
    lock.acquire()

    volume_filepath = _get_volume_filepath()

    # Get the last instance resource index
    instance_index = _get_index(volume_filepath)

    # Retrieve the IaaS resource id key
    resource_id_key = ctx.node.properties[A4C_RESOURCE_ID_KEY]

    # Retrieve the IaaS resource id values (list of ids seperated by comma)
    resource_ids = ctx.node.properties[resource_id_key]

    ids_array = resource_ids.split(",")

    if(instance_index < len(ids_array) ):
      # Save the resource id in the runtime properties for the instance
      ctx.instance.runtime_properties[resource_id_key] = ids_array[instance_index]

      ctx.logger.info("[A4C_VOLUME] Set existing resource (id={}) for volume (id={})".format(ctx.instance.runtime_properties[resource_id_key], ctx.instance.id))
      # Increment the instance index
      _write_index(volume_filepath, (instance_index + 1))
    else:
      ctx.logger.warning("[A4C_VOLUME] Create new volume for instance {} as there isn't enough existing volume ids on the list {}".format(ctx.instance.id, resource_ids))
      ctx.instance.runtime_properties[resource_id_key] = None
      ctx.instance.runtime_properties[USE_EXTERNAL_RESOURCE_KEY] = False
  finally:
    lock.release()

def _get_index(filepath):
  if(os.path.isfile(filepath)):
    with open(filepath, 'r') as f:
      line = f.read()
      if(line):
        return int(line)
  return 0

def _write_index(filepath, index):
  ctx.logger.debug("[A4C_VOLUME] writing {} into {}".format(index, filepath))
  with open(filepath, 'w') as f:
    f.write(str(index))

def _get_working_dir():
  return "./deployments/{}/{}".format(ctx.tenant_name, ctx.deployment.id)

def _get_volume_filepath():
  return "{}/volume_index_{}.dat".format(_get_working_dir(), ctx.node.id)
