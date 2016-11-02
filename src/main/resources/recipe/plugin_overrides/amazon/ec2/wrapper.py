from cloudify import ctx
from cloudify.decorators import operation

from a4c_common.wrapper_util import (USE_EXTERNAL_RESOURCE_KEY,handle_external_resource,handle_resource_ids)

from ec2.ebs import create

@operation
def overrided_create_volume(args, **_):
  handle_external_resource()
  if(ctx.instance.runtime_properties[USE_EXTERNAL_RESOURCE_KEY]):
    handle_resource_ids()
  create(args, **_)