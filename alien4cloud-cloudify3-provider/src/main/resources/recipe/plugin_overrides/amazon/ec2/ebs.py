#########
# https://github.com/cloudify-cosmo/cloudify-aws-plugin/blob/1.4.1/ec2/ebs.py 
#
# [a4c_override]:
# - Replaced ctx.node.properties by ctx.instance.runtime_properties when accessing 'use_external_resource' and 'resource_id'
#
#########
# Third-party Imports
import boto.exception

# Cloudify imports
from ec2 import utils
from ec2 import constants
from ec2 import connection
from cloudify import ctx
from cloudify.exceptions import NonRecoverableError
from cloudify.decorators import operation

@operation
def create(args, **_):
    """Creates an EBS volume.
    """

    ec2_client = connection.EC2ConnectionClient().client()

    for property_name in constants.VOLUME_REQUIRED_PROPERTIES:
        utils.validate_node_property(property_name, ctx.node.properties)

    if _create_external_volume():
        return

    ctx.logger.debug('Creating EBS volume')

    create_volume_args = dict(
        size=ctx.node.properties['size'],
        zone=ctx.node.properties[constants.ZONE]
    )

    create_volume_args.update(args)

    try:
        new_volume = ec2_client.create_volume(**create_volume_args)
    except (boto.exception.EC2ResponseError,
            boto.exception.BotoServerError) as e:
        raise NonRecoverableError('{0}'.format(str(e)))

    ctx.instance.runtime_properties[constants.ZONE] = new_volume.zone

    utils.set_external_resource_id(
        new_volume.id, ctx.instance, external=False)


def _create_external_volume():
    """If use_external_resource is True, this will set the runtime_properties,
    and then exit.
    :return False: Cloudify resource. Continue operation.
    :return True: External resource. Set runtime_properties. Ignore operation.
    """

    # [a4c_override]
    if not utils.use_external_resource(ctx.instance.runtime_properties):
        return False

    #[a4c_override]
    volume_id = ctx.instance.runtime_properties['resource_id']

    volume = _get_volumes_from_id(volume_id)
    if not volume:
        raise NonRecoverableError(
            'External EBS volume was indicated, but the '
            'volume id does not exist.')
    utils.set_external_resource_id(volume.id, ctx.instance)
    return True


def _get_volumes_from_id(volume_id):
    """Returns the EBS Volume object for a given EBS Volume id.
    :param volume_id: The ID of an EBS Volume.
    :returns The boto EBS volume object.
    """

    volumes = _get_volumes(list_of_volume_ids=volume_id)

    return volumes[0] if volumes else volumes


def _get_volumes(list_of_volume_ids):
    """Returns a list of EBS Volumes for a given list of volume IDs.
    :param list_of_volume_ids: A list of EBS volume IDs.
    :returns A list of EBS objects.
    :raises NonRecoverableError: If Boto errors.
    """

    ec2_client = connection.EC2ConnectionClient().client()

    try:
        volumes = ec2_client.get_all_volumes(
            volume_ids=list_of_volume_ids)
    except boto.exception.EC2ResponseError as e:
        if 'InvalidVolume.NotFound' in e:
            all_volumes = ec2_client.get_all_volumes()
            utils.log_available_resources(all_volumes)
        return None
    except boto.exception.BotoServerError as e:
        raise NonRecoverableError('{0}'.format(str(e)))

    return volumes