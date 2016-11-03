from cloudify import ctx
from cloudify.decorators import operation

from a4c_common.wrapper_util import (A4C_RESOURCE_ID_KEY,USE_EXTERNAL_RESOURCE_KEY,handle_external_resource,handle_resource_ids)

from ec2.instance import run_instances
from ec2.ebs import create


@operation
def overrided_run_instance(**_):
  if 'parameters' in ctx.node.properties and 'placement' in ctx.node.properties['parameters']:
    if ('scale' in ctx.workflow_id):
      # Ignore the placement property when scale
      ctx.instance.runtime_properties['placement'] = None
    else:
      ctx.instance.runtime_properties['placement'] = ctx.node.properties['parameters']['placement']
  run_instances(**_)


@operation
def overrided_create_volume(args, **_):
  ctx.instance.runtime_properties[ctx.node.properties[A4C_RESOURCE_ID_KEY]]= None
  handle_external_resource()
  if(ctx.instance.runtime_properties[USE_EXTERNAL_RESOURCE_KEY]):
    handle_resource_ids()
  create(args, **_)