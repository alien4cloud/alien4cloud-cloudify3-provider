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
# https://github.com/cloudify-cosmo/cloudify-openstack-plugin/blob/2.7.1/cinder_plugin/volume.py
#
# [a4c_override]: 
#  - Replaced import 'openstack_plugin_common' with 'cinder_plugin'
#  - Removed annotations on create() method
#

import time

from cloudify import ctx
from cloudify import exceptions as cfy_exc

#[a4c_override]
from cinder_plugin import (with_cinder_client,
                         use_external_resource,
                         create_object_dict,
                         COMMON_RUNTIME_PROPERTIES_KEYS,
                         OPENSTACK_AZ_PROPERTY,
                         OPENSTACK_ID_PROPERTY,
                         OPENSTACK_TYPE_PROPERTY,
                         OPENSTACK_NAME_PROPERTY)

VOLUME_STATUS_AVAILABLE = 'available'
VOLUME_STATUS_ERROR = 'error'
VOLUME_STATUS_ERROR_DELETING = 'error_deleting'
VOLUME_ERROR_STATUSES = (VOLUME_STATUS_ERROR, VOLUME_STATUS_ERROR_DELETING)

VOLUME_OPENSTACK_TYPE = 'volume'

#[a4c_override] removed annotations
def create(cinder_client, status_attempts, status_timeout, args, **kwargs):

    if use_external_resource(ctx, cinder_client, VOLUME_OPENSTACK_TYPE,
                             'name'):
        return

    volume_dict = create_object_dict(ctx, VOLUME_OPENSTACK_TYPE, args, {})
    handle_image_from_relationship(volume_dict, 'imageRef', ctx)

    v = cinder_client.volumes.create(**volume_dict)

    ctx.instance.runtime_properties[OPENSTACK_ID_PROPERTY] = v.id
    ctx.instance.runtime_properties[OPENSTACK_TYPE_PROPERTY] = \
        VOLUME_OPENSTACK_TYPE
    ctx.instance.runtime_properties[OPENSTACK_NAME_PROPERTY] = \
        volume_dict['name']
    wait_until_status(cinder_client=cinder_client,
                      volume_id=v.id,
                      status=VOLUME_STATUS_AVAILABLE,
                      num_tries=status_attempts,
                      timeout=status_timeout,
                      )
    ctx.instance.runtime_properties[OPENSTACK_AZ_PROPERTY] = \
        v.availability_zone


def wait_until_status(cinder_client, volume_id, status, num_tries,
                      timeout):
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


#
# [a4c_override]
# The function handle_image_from_relationship() is copied from glance plugin, source can be found here:
#  - https://github.com/cloudify-cosmo/cloudify-openstack-plugin/blob/2.7.1/glance_plugin/volume.py
#  - Watch out the import of get_openstack_ids_of_connected_nodes_by_openstack_type which is included from our local cinder_plugin/__init__.py
#
IMAGE_OPENSTACK_TYPE = 'image'
from cinder_plugin import get_openstack_ids_of_connected_nodes_by_openstack_type

def handle_image_from_relationship(obj_dict, property_name_to_put, ctx):
    images = get_openstack_ids_of_connected_nodes_by_openstack_type(
        ctx, IMAGE_OPENSTACK_TYPE)
    if images:
        obj_dict.update({property_name_to_put: images[0]})
