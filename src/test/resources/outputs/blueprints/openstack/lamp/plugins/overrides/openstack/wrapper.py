from cloudify import ctx
from cloudify.decorators import operation

from a4c_common.wrapper_util import (USE_EXTERNAL_RESOURCE_KEY,handle_external_resource,handle_resource_ids)

from openstack import with_cinder_client
from openstack.volume import create

@operation
@with_cinder_client
def overrided_create_volume(cinder_client, args, **_):
  handle_external_resource()
  if(ctx.instance.runtime_properties[USE_EXTERNAL_RESOURCE_KEY]):
    handle_resource_ids()
  create(cinder_client, args, **_)
