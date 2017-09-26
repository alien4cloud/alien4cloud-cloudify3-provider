1. Bootstrap 3.3.1 :

  - Clone the git repository https://fastconnect.org/gitlab/cloudify/cloudify-cli-docker-image.git
  - follow the steps to create the docker container

  ==== Before bootstrapping ====

  - File /opt/cfy/env/lib/python2.7/site-packages/cloudify_rest_client/plugins.py:

    replace with src/test/resources/plugins.py
    rm /opt/cfy/env/lib/python2.7/site-packages/cloudify_rest_client/plugins.pyc

  - File /opt/cfy/cloudify-manager-blueprints-commercial/openstack-manager-blueprint.yaml
    File /opt/cfy/cloudify-manager-blueprints-commercial/aws-ec2-manager-blueprint.yaml
      * find agents_security_group definition
      * replace everithing with:
          agents_security_group:
            type: cloudify.openstack.nodes.SecurityGroup
            properties:
              resource_id: { get_input: agents_security_group_name }
              use_external_resource: true
              openstack_config: *openstack_configuration
      * Do the same with management_security_group:
          management_security_group:
            type: cloudify.openstack.nodes.SecurityGroup
            properties:
              resource_id: { get_input: manager_security_group_name }
              use_external_resource: true
              openstack_config: *openstack_configuration

  - in the inputs file:
    Change the blueprint inputs so that It uses existing security group openbar instead of creating new ones
    set manager_security_group_name and agents_security_group_name to "openbar"

  - Bootstrap the manager.

On the bootstrapped manager:
  - Install python devel and gcc
    sudo yum -y install gcc
    sudo yum -y install python-devel
2. Bootstrap 3.3.1 GA with security:
