#########
# Copyright (c) 2014 GigaSpaces Technologies Ltd. All rights reserved
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
#  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  * See the License for the specific language governing permissions and
#  * limitations under the License.

#
# https://github.com/cloudify-cosmo/cloudify-openstack-plugin/blob/1.3.1/cinder_plugin/volume.py
#
# [a4c_override]: 
#  - Replaced import 'openstack_plugin_common' with 'openstack'
#  - Removed annotations on create() method
#
import time

from cloudify import ctx
from cloudify.decorators import operation
from cloudify import exceptions as cfy_exc

#[a4c_override]
from openstack import (with_cinder_client,
                                     get_resource_id,
                                     transform_resource_name,
                                     use_external_resource,
                                     OPENSTACK_ID_PROPERTY,
                                     OPENSTACK_TYPE_PROPERTY,
                                     OPENSTACK_NAME_PROPERTY)

VOLUME_STATUS_AVAILABLE = 'available'
VOLUME_STATUS_ERROR = 'error'
VOLUME_STATUS_ERROR_DELETING = 'error_deleting'
VOLUME_ERROR_STATUSES = (VOLUME_STATUS_ERROR, VOLUME_STATUS_ERROR_DELETING)

VOLUME_OPENSTACK_TYPE = 'volume'

#[a4c_override] removed annotations
def create(cinder_client, args, **kwargs):
    if use_external_resource(ctx, cinder_client, VOLUME_OPENSTACK_TYPE,
                             'display_name'):
        return

    name = get_resource_id(ctx, VOLUME_OPENSTACK_TYPE)
    volume_dict = {'display_name': name}
    volume_dict.update(ctx.node.properties['volume'], **args)
    volume_dict['display_name'] = transform_resource_name(
        ctx, volume_dict['display_name'])

    v = cinder_client.volumes.create(**volume_dict)

    ctx.instance.runtime_properties[OPENSTACK_ID_PROPERTY] = v.id
    ctx.instance.runtime_properties[OPENSTACK_TYPE_PROPERTY] = \
        VOLUME_OPENSTACK_TYPE
    ctx.instance.runtime_properties[OPENSTACK_NAME_PROPERTY] = \
        volume_dict['display_name']
    wait_until_status(cinder_client=cinder_client,
                      volume_id=v.id,
                      status=VOLUME_STATUS_AVAILABLE)


def wait_until_status(cinder_client, volume_id, status, num_tries=10,
                      timeout=2):
    for _ in range(num_tries):
        volume = cinder_client.volumes.get(volume_id)

        if volume.status in VOLUME_ERROR_STATUSES:
            raise cfy_exc.NonRecoverableError(
                "Volume {0} is in error state".format(volume_id))

        if volume.status == status:
            return volume, True
        time.sleep(timeout)

    ctx.logger.warning("Volume {0} current state: '{1}', "
                       "expected state: '{2}'".format(volume_id,
                                                      volume.status,
                                                      status))
    return volume, False
