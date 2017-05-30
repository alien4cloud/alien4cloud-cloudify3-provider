from cloudify import utils
from cloudify_rest_client import CloudifyClient
from cloudify.state import ctx_parameters as inputs

import os

client = CloudifyClient(host=utils.get_manager_ip(),
                        port=utils.get_manager_rest_service_port(),
                        protocol='https',
                        cert=utils.get_local_rest_certificate(),
                        token= utils.get_rest_token(),
                        tenant= utils.get_tenant_name())
