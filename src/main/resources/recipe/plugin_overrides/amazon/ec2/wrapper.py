from cloudify import ctx
from cloudify.decorators import operation

from a4c_common.wrapper_util import (A4C_RESOURCE_ID_KEY,USE_EXTERNAL_RESOURCE_KEY,handle_external_resource,Lock,_get_index,_write_index)

from ec2.instance import run_instances
from ec2.ebs import create

A4C_INSTANCE_INDEX = "_a4c_instance_index"

@operation
def overrided_run_instance(**_):
  if 'parameters' in ctx.node.properties and 'placement' in ctx.node.properties['parameters']:
    if ('scale' in ctx.workflow_id):
      # Ignore the placement property when scale
      ctx.instance.runtime_properties['placement'] = None
    else:
      _set_placement_for_instance()
  run_instances(**_)

def _set_placement_for_instance():
  try:
    lock = Lock("/tmp/lock_name_{}_placement.tmp".format(ctx.deployment.id))
    lock.acquire()

    placement_filepath = _get_placement_filepath()

    # Get the last instance resource index
    instance_index = _get_index(placement_filepath)

    # Retrieve the IaaS resource id values (list of ids seperated by comma)
    placements = ctx.node.properties['parameters']['placement']

    placement_array = placements.split(",")

    if(instance_index < len(placement_array) ):
      # Save the resource id in the runtime properties for the instance
      ctx.instance.runtime_properties['placement'] = placement_array[instance_index]
      ctx.instance.runtime_properties[A4C_INSTANCE_INDEX]=instance_index

      ctx.logger.info("[A4C_PLACEMENT] Set placement '{}'' for compute id={} index={}".format(ctx.instance.runtime_properties['placement'], ctx.instance.id, instance_index))
      # Increment the instance index
      _write_index(placement_filepath, (instance_index + 1))
    else:
      ctx.logger.warning("[A4C_PLACEMENT] No placement for instance {} as there is no more available zones on the list {}".format(ctx.instance.id, placements))
      ctx.instance.runtime_properties['placement'] = None
  finally:
    lock.release()

def _get_placement_filepath():
  return "./deployments/{}/placement_index_{}.dat".format(ctx.deployment.id, ctx.node.id)

@operation
def overrided_create_volume(args, **_):
  ctx.instance.runtime_properties[ctx.node.properties[A4C_RESOURCE_ID_KEY]]= None
  handle_external_resource()
  if(ctx.instance.runtime_properties[USE_EXTERNAL_RESOURCE_KEY]):
    handle_aws_resource_ids()
  create(args, **_)


def handle_aws_resource_ids():

  compute_id = ctx.instance.relationships[0].target.instance.id

  instance_index = -1
  if( A4C_INSTANCE_INDEX in ctx.instance.relationships[0].target.instance.runtime_properties ):
    # Retrieve the index instance
    instance_index = ctx.instance.relationships[0].target.instance.runtime_properties[A4C_INSTANCE_INDEX]

  # Retrieve the IaaS resource id key
  resource_id_key = ctx.node.properties[A4C_RESOURCE_ID_KEY]

  # Retrieve the IaaS resource id values (list of ids seperated by comma)
  resource_ids = ctx.node.properties[resource_id_key]

  ids_array = resource_ids.split(",")

  if( instance_index != -1 and instance_index < len(ids_array) ):
    # Save the resource id in the runtime properties for the instance
    ctx.instance.runtime_properties[resource_id_key] = ids_array[instance_index]
    ctx.logger.info("[A4C_VOLUME] Set existing resource (id={}) for volume (id={}) linked to compute (id={})".format(ctx.instance.runtime_properties[resource_id_key], ctx.instance.id, compute_id))
  else:
    ctx.logger.warning("[A4C_VOLUME] Create new volume for instance {} as there no match index to get a volume id on the list {} (index={})".format(ctx.instance.id, resource_ids, instance_index))
    ctx.instance.runtime_properties[resource_id_key] = None
    ctx.instance.runtime_properties[USE_EXTERNAL_RESOURCE_KEY] = False 