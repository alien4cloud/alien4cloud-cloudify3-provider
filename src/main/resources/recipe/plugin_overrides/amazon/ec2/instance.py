########
# Copyright (c) 2015 GigaSpaces Technologies Ltd. All rights reserved
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
#    * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    * See the License for the specific language governing permissions and
#    * limitations under the License.


# Third-party Imports
import boto.exception

# Cloudify imports
from ec2 import utils
from ec2 import constants
from ec2 import connection
from cloudify import ctx
from cloudify import compute
from cloudify.exceptions import NonRecoverableError
from cloudify.decorators import operation


@operation
def run_instances(**_):
    ec2_client = connection.EC2ConnectionClient().client()

    for property_name in constants.INSTANCE_REQUIRED_PROPERTIES:
        utils.validate_node_property(property_name, ctx.node.properties)

    if _create_external_instance():
        return

    instance_parameters = _get_instance_parameters()

    ctx.logger.info(
        'Attempting to create EC2 Instance with these API parameters: {0}.'
        .format(instance_parameters))

    instance_id = _run_instances_if_needed(ec2_client, instance_parameters)

    instance = _get_instance_from_id(instance_id)

    if instance is None:
        return ctx.operation.retry(
            message='Waiting to verify that instance {0} '
            'has been added to your account.'.format(instance_id))

    utils.set_external_resource_id(
        instance_id, ctx.instance, external=False)
    _instance_created_assign_runtime_properties()


def _assign_runtime_properties_to_instance(runtime_properties):

    for property_name in runtime_properties:
        if 'ip' is property_name:
            ctx.instance.runtime_properties[property_name] = \
                _get_instance_attribute('private_ip_address')
        elif 'public_ip_address' is property_name:
            ctx.instance.runtime_properties[property_name] = \
                _get_instance_attribute('ip_address')
        else:
            ctx.instance.runtime_properties[property_name] = \
                _get_instance_attribute(property_name)


def _instance_created_assign_runtime_properties():
    _assign_runtime_properties_to_instance(
        runtime_properties=constants.INSTANCE_INTERNAL_ATTRIBUTES_POST_CREATE)


def _run_instances_if_needed(ec2_client, instance_parameters):

    if ctx.operation.retry_number == 0:

        try:
            reservation = ec2_client.run_instances(**instance_parameters)
        except (boto.exception.EC2ResponseError,
                boto.exception.BotoServerError) as e:
            raise NonRecoverableError('{0}'.format(str(e)))
        ctx.instance.runtime_properties['reservation_id'] = reservation.id
        return reservation.instances[0].id

    elif constants.EXTERNAL_RESOURCE_ID not in ctx.instance.runtime_properties:

        instances = _get_instances_from_reservation_id(ec2_client)

        if not instances:
            raise NonRecoverableError(
                'Instance failed for an unknown reason. Node ID: {0}. '
                .format(ctx.instance.id))
        elif len(instances) != 1:
            raise NonRecoverableError(
                'More than one instance was created by the install workflow. '
                'Unable to handle request.')

        return instances[0].id

    return ctx.instance.runtime_properties[constants.EXTERNAL_RESOURCE_ID]


def _handle_userdata(parameters):

    existing_userdata = parameters.get('user_data')
    install_agent_userdata = ctx.agent.init_script()

    if not (existing_userdata or install_agent_userdata):
        return parameters

    if not existing_userdata:
        final_userdata = install_agent_userdata
    elif not install_agent_userdata:
        final_userdata = existing_userdata
    else:
        final_userdata = compute.create_multi_mimetype_userdata(
            [existing_userdata, install_agent_userdata])

    parameters['user_data'] = final_userdata

    return parameters


def _get_instances_from_reservation_id(ec2_client):

    try:
        reservations = ec2_client.get_all_instances(
            filters={
                'reservation-id':
                    ctx.instance.runtime_properties['reservation_id']
            })
    except (boto.exception.EC2ResponseError,
            boto.exception.BotoServerError) as e:
        raise NonRecoverableError('{0}'.format(str(e)))

    if len(reservations) < 1:
        return None

    return reservations[0].instances


def _create_external_instance():

    if not utils.use_external_resource(ctx.node.properties):
        return False

    instance_id = ctx.node.properties['resource_id']
    instance = _get_instance_from_id(instance_id)
    if instance is None:
        raise NonRecoverableError(
            'Cannot use_external_resource because instance_id {0} '
            'is not in this account.'.format(instance_id))
    utils.set_external_resource_id(instance.id, ctx.instance)
    return True


def _get_all_instances(list_of_instance_ids=None):
    """Returns a list of instance objects for a list of instance IDs.

    :returns a list of instance objects.
    :raises NonRecoverableError: If Boto errors.
    """

    ec2_client = connection.EC2ConnectionClient().client()

    try:
        reservations = ec2_client.get_all_reservations(list_of_instance_ids)
    except boto.exception.EC2ResponseError as e:
        if 'InvalidInstanceID.NotFound' in e:
            instances = [instance for res in ec2_client.get_all_reservations()
                         for instance in res.instances]
            utils.log_available_resources(instances)
        return None
    except boto.exception.BotoServerError as e:
        raise NonRecoverableError('{0}'.format(str(e)))

    instances = []

    for reservation in reservations:
        for instance in reservation.instances:
            instances.append(instance)

    return instances


def _get_instance_from_id(instance_id):
    """Gets the instance ID of a EC2 Instance

    :param instance_id: The ID of an EC2 Instance
    :returns an ID of a an EC2 Instance or None.
    """

    instance = _get_all_instances(list_of_instance_ids=instance_id)

    return instance[0] if instance else instance


def _get_instance_attribute(attribute):
    """Gets an attribute from a boto object that represents an EC2 Instance.

    :param attribute: The named python attribute of a boto object.
    :returns python attribute of a boto object representing an EC2 instance.
    :raises NonRecoverableError if constants.EXTERNAL_RESOURCE_ID not set
    :raises NonRecoverableError if no instance is found.
    """

    if constants.EXTERNAL_RESOURCE_ID not in ctx.instance.runtime_properties:
        raise NonRecoverableError(
            'Unable to get instance attibute {0}, because {1} is not set.'
            .format(attribute, constants.EXTERNAL_RESOURCE_ID))

    instance_id = \
        ctx.instance.runtime_properties[constants.EXTERNAL_RESOURCE_ID]
    instance_object = _get_instance_from_id(instance_id)

    if not instance_object:
        if not ctx.node.properties['use_external_resource']:
            ec2_client = connection.EC2ConnectionClient().client()
            instances = _get_instances_from_reservation_id(ec2_client)
            if not instances:
                raise NonRecoverableError(
                    'Unable to get instance attibute {0}, because '
                    'no instance with id {1} exists in this account.'
                    .format(attribute, instance_id))
            elif len(instances) != 1:
                raise NonRecoverableError(
                    'Unable to get instance attibute {0}, because more '
                    'than one instance with id {1} exists in this account.'
                    .format(attribute, instance_id))
            instance_object = instances[0]
        else:
            raise NonRecoverableError(
                'External resource, but the supplied '
                'instance id {0} is not in the account.'.format(instance_id))

    attribute = getattr(instance_object, attribute)
    return attribute


def _get_instance_parameters():
    """The parameters to the run_instance boto call.

    :returns parameters dictionary
    """

    provider_variables = utils.get_provider_variables()

    attached_group_ids = \
        utils.get_target_external_resource_ids(
            constants.INSTANCE_SECURITY_GROUP_RELATIONSHIP,
            ctx.instance)

    if provider_variables.get(constants.AGENTS_SECURITY_GROUP):
        attached_group_ids.append(
            provider_variables[constants.AGENTS_SECURITY_GROUP])

    parameters = \
        provider_variables.get(constants.AGENTS_AWS_INSTANCE_PARAMETERS)
    parameters.update({
        'image_id': ctx.node.properties['image_id'],
        'instance_type': ctx.node.properties['instance_type'],
        'security_group_ids': attached_group_ids,
        'key_name': _get_instance_keypair(provider_variables),
        'subnet_id': _get_instance_subnet(provider_variables)
    })

    parameters.update(ctx.node.properties['parameters'])
    parameters = _handle_userdata(parameters)

    # [a4c_override] read the property 'placement' from runtime properties
    if 'placement' in parameters:
      if 'placement' in ctx.instance.runtime_properties:
        parameters['placement'] = ctx.instance.runtime_properties['placement']
      else:
        parameters.pop('placement', None)
    ctx.logger.debug("[OVERRIDE] parameters={}".format(parameters))
    # end [a4c_override]

    return parameters


def _get_instance_keypair(provider_variables):
    """Gets the instance key pair. If more or less than one is provided,
    this will raise an error.

    """
    list_of_keypairs = \
        utils.get_target_external_resource_ids(
            constants.INSTANCE_KEYPAIR_RELATIONSHIP, ctx.instance)

    if not list_of_keypairs and \
            provider_variables.get(constants.AGENTS_KEYPAIR):
        list_of_keypairs.append(provider_variables[constants.AGENTS_KEYPAIR])
    elif len(list_of_keypairs) > 1:
        raise NonRecoverableError(
            'Only one keypair may be attached to an instance.')

    return list_of_keypairs[0] if list_of_keypairs else None


def _get_instance_subnet(provider_variables):

    list_of_subnets = \
        utils.get_target_external_resource_ids(
            constants.INSTANCE_SUBNET_RELATIONSHIP, ctx.instance
        )

    if not list_of_subnets and provider_variables.get(constants.SUBNET):
        list_of_subnets.append(provider_variables[constants.SUBNET])
    elif len(list_of_subnets) > 1:
        raise NonRecoverableError(
            'instance may only be attached to one subnet')

    return list_of_subnets[0] if list_of_subnets else None
