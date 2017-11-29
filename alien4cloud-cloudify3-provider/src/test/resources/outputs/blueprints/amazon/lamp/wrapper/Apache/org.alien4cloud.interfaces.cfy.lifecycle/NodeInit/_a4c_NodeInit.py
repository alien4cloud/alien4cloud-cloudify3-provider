
from cloudify import ctx
from cloudify import utils
from cloudify.exceptions import NonRecoverableError
from StringIO import StringIO

import base64
import os
import platform
import re
import subprocess
import sys
import time
import threading
import platform
import json

def convert_env_value_to_string(envDict):
    for key, value in envDict.items():
        envDict[str(key)] = str(envDict.pop(key))

def get_attribute_user(ctx):
    if get_attribute_from_top_host(ctx, 'user'):
        return get_attribute_from_top_host(ctx, 'user')
    if get_attribute(ctx, 'cloudify_agent'):
        return get_attribute(ctx, 'cloudify_agent').get('user', None)
    if get_attribute(ctx, 'agent_config'):
        return get_attribute(ctx, 'agent_config').get('user', None)
    return None

def get_attribute_key(ctx):
    if get_attribute_from_top_host(ctx, 'key'):
        return get_attribute_from_top_host(ctx, 'key')
    if get_attribute(ctx, 'cloudify_agent'):
        return get_attribute(ctx, 'cloudify_agent').get('key', None)
    if get_attribute(ctx, 'agent_config'):
        return get_attribute(ctx, 'agent_config').get('key', None)
    return None

def get_host(entity):
    if entity.instance.relationships:
        for relationship in entity.instance.relationships:
            if 'cloudify.relationships.contained_in' in relationship.type_hierarchy:
                return relationship.target
    return None


def has_attribute_mapping(entity, attribute_name):
    # ctx.logger.debug('Check if it exists mapping for attribute {0} in {1}'.format(attribute_name,json.dumps(entity.node.properties)))
    mapping_configuration = entity.node.properties.get('_a4c_att_' + attribute_name, None)
    if mapping_configuration is not None:
        if mapping_configuration['parameters'][0] == 'SELF' and mapping_configuration['parameters'][1] == attribute_name:
            return False
        else:
            return True
    return False


def process_attribute_mapping(entity, attribute_name, data_retriever_function):
    # This is where attribute mapping is defined in the cloudify type
    mapping_configuration = entity.node.properties['_a4c_att_' + attribute_name]
    # ctx.logger.debug('Mapping configuration found for attribute {0} is {1}'.format(attribute_name, json.dumps(mapping_configuration)))
    # If the mapping configuration exist and if it concerns SELF then just get attribute of the mapped attribute name
    # Else if it concerns TARGET then follow the relationship and retrieved the mapped attribute name from the TARGET
    if mapping_configuration['parameters'][0] == 'SELF':
        return data_retriever_function(entity, mapping_configuration['parameters'][1])
    elif mapping_configuration['parameters'][0] == 'TARGET' and entity.instance.relationships:
        for relationship in entity.instance.relationships:
            if mapping_configuration['parameters'][1] in relationship.type_hierarchy:
                return data_retriever_function(relationship.target, mapping_configuration['parameters'][2])
    return ""


def get_nested_attribute(entity, attribute_names):
    deep_properties = get_attribute(entity, attribute_names[0])
    attribute_names_iter = iter(attribute_names)
    next(attribute_names_iter)
    for attribute_name in attribute_names_iter:
        if deep_properties is None:
            return ""
        else:
            deep_properties = deep_properties.get(attribute_name, None)
    return deep_properties


def _all_instances_get_nested_attribute(entity, attribute_names):
    return None


def get_attribute(entity, attribute_name):
    if has_attribute_mapping(entity, attribute_name):
        # First check if any mapping exist for attribute
        mapped_value = process_attribute_mapping(entity, attribute_name, get_attribute)
        # ctx.logger.debug('Mapping exists for attribute {0} with value {1}'.format(attribute_name, json.dumps(mapped_value)))
        return mapped_value
    # No mapping exist, try to get directly the attribute from the entity
    attribute_value = entity.instance.runtime_properties.get(attribute_name, None)
    if attribute_value is not None:
        # ctx.logger.debug('Found the attribute {0} with value {1} on the node {2}'.format(attribute_name, json.dumps(attribute_value), entity.node.id))
        return attribute_value
    # Attribute retrieval fails, fall back to property
    property_value = entity.node.properties.get(attribute_name, None)
    if property_value is not None:
        return property_value
    # Property retrieval fails, fall back to host instance
    host = get_host(entity)
    if host is not None:
        # ctx.logger.debug('Attribute not found {0} go up to the parent node {1}'.format(attribute_name, host.node.id))
        return get_attribute(host, attribute_name)
    # Nothing is found
    return ""

def get_target_capa_or_node_attribute(entity, capability_attribute_name, attribute_name):
    attribute_value = entity.instance.runtime_properties.get(capability_attribute_name, None)
    if attribute_value is not None:
        # ctx.logger.debug('Found the capability attribute {0} with value {1} on the node {2}'.format(attribute_name, json.dumps(attribute_value), entity.node.id))
        return attribute_value
    return get_attribute(entity, attribute_name)

def _all_instances_get_attribute(entity, attribute_name):
    result_map = {}
    # get all instances data using cfy rest client
    # we have to get the node using the rest client with node_instance.node_id
    # then we will have the relationships
    node = client.nodes.get(ctx.deployment.id, entity.node.id)
    all_node_instances = client.node_instances.list(ctx.deployment.id, entity.node.id)
    for node_instance in all_node_instances:
        prop_value = __recursively_get_instance_data(node, node_instance, attribute_name)
        if prop_value is not None:
            # ctx.logger.debug('Found the property/attribute {0} with value {1} on the node {2} instance {3}'.format(attribute_name, json.dumps(prop_value), entity.node.id,
            #   node_instance.id))
            result_map[node_instance.id + '_'] = prop_value
    return result_map

# Same as previous method but will first try to find the attribute on the capability.
def _all_instances_get_target_capa_or_node_attribute(entity, capability_attribute_name, attribute_name):
    result_map = {}
    node = client.nodes.get(ctx.deployment.id, entity.node.id)
    all_node_instances = client.node_instances.list(ctx.deployment.id, entity.node.id)
    for node_instance in all_node_instances:
        attribute_value = node_instance.runtime_properties.get(capability_attribute_name, None)
        if attribute_value is not None:
            prop_value = attribute_value
        else:
            prop_value = __recursively_get_instance_data(node, node_instance, attribute_name)
        if prop_value is not None:
            # ctx.logger.debug('Found the property/attribute {0} with value {1} on the node {2} instance {3}'.format(attribute_name, json.dumps(prop_value), entity.node.id,
            #    node_instance.id))
            result_map[node_instance.id + '_'] = prop_value
    return result_map

def get_property(entity, property_name):
    # Try to get the property value on the node
    property_value = entity.node.properties.get(property_name, None)
    if property_value is not None:
        # ctx.logger.debug('Found the property {0} with value {1} on the node {2}'.format(property_name, json.dumps(property_value), entity.node.id))
        return property_value
    # No property found on the node, fall back to the host
    host = get_host(entity)
    if host is not None:
        # ctx.logger.debug('Property not found {0} go up to the parent node {1}'.format(property_name, host.node.id))
        return get_property(host, property_name)
    return ""


def get_instance_list(node_id):
    result = ''
    all_node_instances = client.node_instances.list(ctx.deployment.id, node_id)
    for node_instance in all_node_instances:
        if len(result) > 0:
            result += ','
        result += node_instance.id
    return result

def get_host_node_name(instance):
    for relationship in instance.relationships:
        if 'cloudify.relationships.contained_in' in relationship.type_hierarchy:
            return relationship.target.node.id
    return None

def __get_relationship(node, target_name, relationship_type):
    for relationship in node.relationships:
        if relationship.get('target_id') == target_name and relationship_type in relationship.get('type_hierarchy'):
            return relationship
    return None


def __has_attribute_mapping(node, attribute_name):
    # ctx.logger.debug('Check if it exists mapping for attribute {0} in {1}'.format(attribute_name, json.dumps(node.properties)))
    mapping_configuration = node.properties.get('_a4c_att_' + attribute_name, None)
    if mapping_configuration is not None:
        if mapping_configuration['parameters'][0] == 'SELF' and mapping_configuration['parameters'][1] == attribute_name:
            return False
        else:
            return True
    return False

def __process_attribute_mapping(node, node_instance, attribute_name, data_retriever_function):
    # This is where attribute mapping is defined in the cloudify type
    mapping_configuration = node.properties['_a4c_att_' + attribute_name]
    # ctx.logger.debug('Mapping configuration found for attribute {0} is {1}'.format(attribute_name, json.dumps(mapping_configuration)))
    # If the mapping configuration exist and if it concerns SELF then just get attribute of the mapped attribute name
    # Else if it concerns TARGET then follow the relationship and retrieved the mapped attribute name from the TARGET
    if mapping_configuration['parameters'][0] == 'SELF':
        return data_retriever_function(node, node_instance, mapping_configuration['parameters'][1])
    elif mapping_configuration['parameters'][0] == 'TARGET' and node_instance.relationships:
        for rel in node_instance.relationships:
            relationship = __get_relationship(node, rel.get('target_name'), rel.get('type'))
            if mapping_configuration['parameters'][1] in relationship.get('type_hierarchy'):
                target_instance = client.node_instances.get(rel.get('target_id'))
                target_node = client.nodes.get(ctx.deployment.id, target_instance.node_id)
                return data_retriever_function(target_node, target_instance, mapping_configuration['parameters'][2])
    return None

def __recursively_get_instance_data(node, node_instance, attribute_name):
    if __has_attribute_mapping(node, attribute_name):
        return __process_attribute_mapping(node, node_instance, attribute_name, __recursively_get_instance_data)
    attribute_value = node_instance.runtime_properties.get(attribute_name, None)
    if attribute_value is not None:
        return attribute_value
    elif node_instance.relationships:
        for rel in node_instance.relationships:
            # on rel we have target_name, target_id (instanceId), type
            relationship = __get_relationship(node, rel.get('target_name'), rel.get('type'))
            if 'cloudify.relationships.contained_in' in relationship.get('type_hierarchy'):
                parent_instance = client.node_instances.get(rel.get('target_id'))
                parent_node = client.nodes.get(ctx.deployment.id, parent_instance.node_id)
                return __recursively_get_instance_data(parent_node, parent_instance, attribute_name)
        return None
    else:
        return None

def get_public_or_private_ip(entity):
    public_ip = get_attribute(entity, 'public_ip_address')
    if not public_ip:
        return get_attribute(entity, 'ip_address')
    return public_ip

def get_attribute_from_top_host(entity, attribute_name):
    host = get_host(entity)
    while host is not None:
        entity = host
        host = get_host(entity)
    return get_attribute(entity, attribute_name)

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

from __future__ import unicode_literals

import json

try:
    import hcl
    has_hcl_parser = True
except ImportError:
    has_hcl_parser = False
import requests

try:
    from urlparse import urljoin
except ImportError:
    from urllib.parse import urljoin

class VaultError(Exception):
    def __init__(self, message=None, errors=None):
        if errors:
            message = ', '.join(errors)

        self.errors = errors

        super(VaultError, self).__init__(message)

class InvalidRequest(VaultError):
    pass

class Unauthorized(VaultError):
    pass

class Forbidden(VaultError):
    pass

class InvalidPath(VaultError):
    pass

class RateLimitExceeded(VaultError):
    pass

class InternalServerError(VaultError):
    pass

class VaultNotInitialized(VaultError):
    pass

class VaultDown(VaultError):
    pass

class UnexpectedError(VaultError):
    pass

class HashiCorpVaultClient(object):
    def __init__(self, url='http://localhost:8200', token=None,
                 cert=None, verify=True, timeout=30, proxies=None,
                 allow_redirects=True, session=None):

        if not session:
            session = requests.Session()

        self.allow_redirects = allow_redirects
        self.session = session
        self.token = token

        self._url = url
        self._kwargs = {
            'cert': cert,
            'verify': verify,
            'timeout': timeout,
            'proxies': proxies,
        }

    def read(self, path, wrap_ttl=None):
        """
        GET /<path>
        """
        try:
            return self._get('/v1/{0}'.format(path), wrap_ttl=wrap_ttl).json()
        except InvalidPath:
            return None

    def list(self, path):
        """
        GET /<path>?list=true
        """
        try:
            payload = {
                'list': True
            }
            return self._get('/v1/{}'.format(path), params=payload).json()
        except InvalidPath:
            return None

    def write(self, path, wrap_ttl=None, **kwargs):
        """
        PUT /<path>
        """
        response = self._put('/v1/{0}'.format(path), json=kwargs, wrap_ttl=wrap_ttl)

        if response.status_code == 200:
            return response.json()

    def delete(self, path):
        """
        DELETE /<path>
        """
        self._delete('/v1/{0}'.format(path))

    def unwrap(self, token):
        """
        GET /cubbyhole/response
        X-Vault-Token: <token>
        """
        path = "cubbyhole/response"
        _token = self.token
        try:
            self.token = token
            return json.loads(self.read(path)['data']['response'])
        finally:
            self.token = _token

    def is_initialized(self):
        """
        GET /sys/init
        """
        return self._get('/v1/sys/init').json()['initialized']

    def initialize(self, secret_shares=5, secret_threshold=3, pgp_keys=None):
        """
        PUT /sys/init
        """
        params = {
            'secret_shares': secret_shares,
            'secret_threshold': secret_threshold,
        }

        if pgp_keys:
            if len(pgp_keys) != secret_shares:
                raise ValueError('Length of pgp_keys must equal secret shares')

            params['pgp_keys'] = pgp_keys

        return self._put('/v1/sys/init', json=params).json()

    @property
    def seal_status(self):
        """
        GET /sys/seal-status
        """
        return self._get('/v1/sys/seal-status').json()

    def is_sealed(self):
        return self.seal_status['sealed']

    def seal(self):
        """
        PUT /sys/seal
        """
        self._put('/v1/sys/seal')

    def unseal_reset(self):
        """
        PUT /sys/unseal
        """
        params = {
            'reset': True,
        }
        return self._put('/v1/sys/unseal', json=params).json()

    def unseal(self, key):
       """
        PUT /sys/unseal
        """
       params = {
           'key': key,
       }

       return self._put('/v1/sys/unseal', json=params).json()

    def unseal_multi(self, keys):
        result = None

        for key in keys:
            result = self.unseal(key)
            if not result['sealed']:
                break

        return result

    @property
    def key_status(self):
        """
        GET /sys/key-status
        """
        return self._get('/v1/sys/key-status').json()

    def rotate(self):
        """
        PUT /sys/rotate
        """
        self._put('/v1/sys/rotate')

    @property
    def rekey_status(self):
        """
        GET /sys/rekey/init
        """
        return self._get('/v1/sys/rekey/init').json()

    def start_rekey(self, secret_shares=5, secret_threshold=3, pgp_keys=None,
                    backup=False):
        """
        PUT /sys/rekey/init
        """
        params = {
            'secret_shares': secret_shares,
            'secret_threshold': secret_threshold,
        }

        if pgp_keys:
            if len(pgp_keys) != secret_shares:
                raise ValueError('Length of pgp_keys must equal secret shares')

            params['pgp_keys'] = pgp_keys
            params['backup'] = backup

        resp = self._put('/v1/sys/rekey/init', json=params)
        if resp.text:
            return resp.json()

    def cancel_rekey(self):
        """
        DELETE /sys/rekey/init
        """
        self._delete('/v1/sys/rekey/init')

    def rekey(self, key, nonce=None):
        """
        PUT /sys/rekey/update
        """
        params = {
            'key': key,
        }

        if nonce:
            params['nonce'] = nonce

        return self._put('/v1/sys/rekey/update', json=params).json()

    def rekey_multi(self, keys, nonce=None):
        result = None

        for key in keys:
            result = self.rekey(key, nonce=nonce)
            if 'complete' in result and result['complete']:
                break

        return result

    def get_backed_up_keys(self):
        """
        GET /sys/rekey/backup
        """
        return self._get('/v1/sys/rekey/backup').json()

    @property
    def ha_status(self):
        """
        GET /sys/leader
        """
        return self._get('/v1/sys/leader').json()

    def renew_secret(self, lease_id, increment=None):
        """
        PUT /sys/leases/renew
        """
        params = {
            'lease_id': lease_id,
            'increment': increment,
        }
        return self._put('/v1/sys/leases/renew', json=params).json()

    def revoke_secret(self, lease_id):
        """
        PUT /sys/revoke/<lease id>
        """
        self._put('/v1/sys/revoke/{0}'.format(lease_id))

    def revoke_secret_prefix(self, path_prefix):
        """
        PUT /sys/revoke-prefix/<path prefix>
        """
        self._put('/v1/sys/revoke-prefix/{0}'.format(path_prefix))

    def revoke_self_token(self):
        """
        PUT /auth/token/revoke-self
        """
        self._put('/v1/auth/token/revoke-self')

    def list_secret_backends(self):
        """
        GET /sys/mounts
        """
        return self._get('/v1/sys/mounts').json()

    def enable_secret_backend(self, backend_type, description=None, mount_point=None, config=None):
        """
        POST /sys/auth/<mount point>
        """
        if not mount_point:
            mount_point = backend_type

        params = {
            'type': backend_type,
            'description': description,
            'config': config,
        }

        self._post('/v1/sys/mounts/{0}'.format(mount_point), json=params)

    def tune_secret_backend(self, backend_type, mount_point=None, default_lease_ttl=None, max_lease_ttl=None):
        """
        POST /sys/mounts/<mount point>/tune
        """

        if not mount_point:
            mount_point = backend_type

        params = {
            'default_lease_ttl': default_lease_ttl,
            'max_lease_ttl': max_lease_ttl
        }

        self._post('/v1/sys/mounts/{0}/tune'.format(mount_point), json=params)

    def get_secret_backend_tuning(self, backend_type, mount_point=None):
        """
        GET /sys/mounts/<mount point>/tune
        """
        if not mount_point:
            mount_point = backend_type

        return self._get('/v1/sys/mounts/{0}/tune'.format(mount_point)).json()

    def disable_secret_backend(self, mount_point):
        """
        DELETE /sys/mounts/<mount point>
        """
        self._delete('/v1/sys/mounts/{0}'.format(mount_point))

    def remount_secret_backend(self, from_mount_point, to_mount_point):
        """
        POST /sys/remount
        """
        params = {
            'from': from_mount_point,
            'to': to_mount_point,
        }

        self._post('/v1/sys/remount', json=params)

    def list_policies(self):
        """
        GET /sys/policy
        """
        return self._get('/v1/sys/policy').json()['policies']

    def get_policy(self, name, parse=False):
        """
        GET /sys/policy/<name>
        """
        try:
            policy = self._get('/v1/sys/policy/{0}'.format(name)).json()['rules']
            if parse:
                if not has_hcl_parser:
                    raise ImportError('pyhcl is required for policy parsing')

                policy = hcl.loads(policy)

            return policy
        except InvalidPath:
            return None

    def set_policy(self, name, rules):
        """
        PUT /sys/policy/<name>
        """

        if isinstance(rules, dict):
            rules = json.dumps(rules)

        params = {
            'rules': rules,
        }

        self._put('/v1/sys/policy/{0}'.format(name), json=params)

    def delete_policy(self, name):
        """
        DELETE /sys/policy/<name>
        """
        self._delete('/v1/sys/policy/{0}'.format(name))

    def list_audit_backends(self):
        """
        GET /sys/audit
        """
        return self._get('/v1/sys/audit').json()

    def enable_audit_backend(self, backend_type, description=None, options=None, name=None):
        """
        POST /sys/audit/<name>
        """
        if not name:
            name = backend_type

        params = {
            'type': backend_type,
            'description': description,
            'options': options,
        }

        self._post('/v1/sys/audit/{0}'.format(name), json=params)

    def disable_audit_backend(self, name):
        """
        DELETE /sys/audit/<name>
        """
        self._delete('/v1/sys/audit/{0}'.format(name))

    def audit_hash(self, name, input):
        """
        POST /sys/audit-hash
        """
        params = {
            'input': input,
        }
        return self._post('/v1/sys/audit-hash/{0}'.format(name), json=params).json()

    def create_token(self, role=None, token_id=None, policies=None, meta=None,
                     no_parent=False, lease=None, display_name=None,
                     num_uses=None, no_default_policy=False,
                     ttl=None, orphan=False, wrap_ttl=None, renewable=None,
                     explicit_max_ttl=None):
        """
        POST /auth/token/create
        POST /auth/token/create/<role>
        POST /auth/token/create-orphan
        """
        params = {
            'id': token_id,
            'policies': policies,
            'meta': meta,
            'no_parent': no_parent,
            'display_name': display_name,
            'num_uses': num_uses,
            'no_default_policy': no_default_policy,
            'renewable': renewable
        }

        if lease:
            params['lease'] = lease
        else:
            params['ttl'] = ttl
            params['explicit_max_ttl'] = explicit_max_ttl

        if explicit_max_ttl:
            params['explicit_max_ttl'] = explicit_max_ttl

        if orphan:
            return self._post('/v1/auth/token/create-orphan', json=params, wrap_ttl=wrap_ttl).json()
        elif role:
            return self._post('/v1/auth/token/create/{0}'.format(role), json=params, wrap_ttl=wrap_ttl).json()
        else:
            return self._post('/v1/auth/token/create', json=params, wrap_ttl=wrap_ttl).json()

    def lookup_token(self, token=None, accessor=False, wrap_ttl=None):
        """
        GET /auth/token/lookup/<token>
        GET /auth/token/lookup-accessor/<token-accessor>
        GET /auth/token/lookup-self
        """
        if token:
            if accessor:
                path = '/v1/auth/token/lookup-accessor/{0}'.format(token)
                return self._post(path, wrap_ttl=wrap_ttl).json()
            else:
                return self._get('/v1/auth/token/lookup/{0}'.format(token)).json()
        else:
            return self._get('/v1/auth/token/lookup-self', wrap_ttl=wrap_ttl).json()

    def revoke_token(self, token, orphan=False, accessor=False):
        """
        POST /auth/token/revoke/<token>
        POST /auth/token/revoke-orphan/<token>
        POST /auth/token/revoke-accessor/<token-accessor>
        """
        if accessor and orphan:
            msg = "revoke_token does not support 'orphan' and 'accessor' flags together"
            raise InvalidRequest(msg)
        elif accessor:
            self._post('/v1/auth/token/revoke-accessor/{0}'.format(token))
        elif orphan:
            self._post('/v1/auth/token/revoke-orphan/{0}'.format(token))
        else:
            self._post('/v1/auth/token/revoke/{0}'.format(token))

    def revoke_token_prefix(self, prefix):
        """
        POST /auth/token/revoke-prefix/<prefix>
        """
        self._post('/v1/auth/token/revoke-prefix/{0}'.format(prefix))

    def renew_token(self, token=None, increment=None, wrap_ttl=None):
        """
        POST /auth/token/renew/<token>
        POST /auth/token/renew-self
        """
        params = {
            'increment': increment,
        }

        if token:
            path = '/v1/auth/token/renew/{0}'.format(token)
            return self._post(path, json=params, wrap_ttl=wrap_ttl).json()
        else:
            return self._post('/v1/auth/token/renew-self', json=params, wrap_ttl=wrap_ttl).json()

    def create_token_role(self, role,
                          allowed_policies=None, orphan=None, period=None,
                          renewable=None, path_suffix=None, explicit_max_ttl=None):
        """
        POST /auth/token/roles/<role>
        """
        params = {
            'allowed_policies': allowed_policies,
            'orphan': orphan,
            'period': period,
            'renewable': renewable,
            'path_suffix': path_suffix,
            'explicit_max_ttl': explicit_max_ttl
        }
        return self._post('/v1/auth/token/roles/{0}'.format(role), json=params)

    def token_role(self, role):
        """
        Returns the named token role.
        """
        return self.read('auth/token/roles/{0}'.format(role))

    def delete_token_role(self, role):
        """
        Deletes the named token role.
        """
        return self.delete('auth/token/roles/{0}'.format(role))

    def list_token_roles(self):
        """
        GET /auth/token/roles?list=true
        """
        return self.list('auth/token/roles')

    def logout(self, revoke_token=False):
        """
        Clears the token used for authentication, optionally revoking it before doing so
        """
        if revoke_token:
            self.revoke_self_token()

        self.token = None

    def is_authenticated(self):
        """
        Helper method which returns the authentication status of the client
        """
        if not self.token:
            return False

        try:
            self.lookup_token()
            return True
        except Forbidden:
            return False
        except InvalidPath:
            return False
        except InvalidRequest:
            return False

    def auth_app_id(self, app_id, user_id, mount_point='app-id', use_token=True):
        """
        POST /auth/<mount point>/login
        """
        params = {
            'app_id': app_id,
            'user_id': user_id,
        }

        return self.auth('/v1/auth/{0}/login'.format(mount_point), json=params, use_token=use_token)

    def auth_tls(self, mount_point='cert', use_token=True):
        """
        POST /auth/<mount point>/login
        """
        return self.auth('/v1/auth/{0}/login'.format(mount_point), use_token=use_token)

    def auth_userpass(self, username, password, mount_point='userpass', use_token=True, **kwargs):
        """
        POST /auth/<mount point>/login/<username>
        """
        params = {
            'password': password,
        }

        params.update(kwargs)

        return self.auth('/v1/auth/{0}/login/{1}'.format(mount_point, username), json=params, use_token=use_token)

    def auth_ec2(self, pkcs7, nonce=None, role=None, use_token=True):
        """
        POST /auth/aws-ec2/login
        """
        params = {'pkcs7': pkcs7}
        if nonce:
            params['nonce'] = nonce
        if role:
            params['role'] = role

        return self.auth('/v1/auth/aws-ec2/login', json=params, use_token=use_token).json()

    def create_userpass(self, username, password, policies, mount_point='userpass', **kwargs):
        """
        POST /auth/<mount point>/users/<username>
        """

        # Users can have more than 1 policy. It is easier for the user to pass in the
        # policies as a list so if they do, we need to convert to a , delimited string.
        if isinstance(policies, (list, set, tuple)):
            policies = ','.join(policies)

        params = {
            'password': password,
            'policies': policies
        }
        params.update(kwargs)

        return self._post('/v1/auth/{}/users/{}'.format(mount_point, username), json=params)

    def delete_userpass(self, username, mount_point='userpass'):
        """
        DELETE /auth/<mount point>/users/<username>
        """
        return self._delete('/v1/auth/{}/users/{}'.format(mount_point, username))

    def create_app_id(self, app_id, policies, display_name=None, mount_point='app-id', **kwargs):
        """
        POST /auth/<mount point>/map/app-id/<app_id>
        """

        # app-id can have more than 1 policy. It is easier for the user to pass in the
        # policies as a list so if they do, we need to convert to a , delimited string.
        if isinstance(policies, (list, set, tuple)):
            policies = ','.join(policies)

        params = {
            'value': policies
        }

        # Only use the display_name if it has a value. Made it a named param for user
        # convienence instead of leaving it as part of the kwargs
        if display_name:
            params['display_name'] = display_name

        params.update(kwargs)

        return self._post('/v1/auth/{}/map/app-id/{}'.format(mount_point, app_id), json=params)

    def get_app_id(self, app_id, mount_point='app-id', wrap_ttl=None):
        """
        GET /auth/<mount_point>/map/app-id/<app_id>
        """
        path = '/v1/auth/{0}/map/app-id/{1}'.format(mount_point, app_id)
        return self._get(path, wrap_ttl=wrap_ttl).json()

    def delete_app_id(self, app_id, mount_point='app-id'):
        """
        DELETE /auth/<mount_point>/map/app-id/<app_id>
        """
        return self._delete('/v1/auth/{0}/map/app-id/{1}'.format(mount_point, app_id))

    def create_user_id(self, user_id, app_id, cidr_block=None, mount_point='app-id', **kwargs):
        """
        POST /auth/<mount point>/map/user-id/<user_id>
        """

        # user-id can be associated to more than 1 app-id (aka policy). It is easier for the user to
        # pass in the policies as a list so if they do, we need to convert to a , delimited string.
        if isinstance(app_id, (list, set, tuple)):
            app_id = ','.join(app_id)

        params = {
            'value': app_id
        }

        # Only use the cidr_block if it has a value. Made it a named param for user
        # convienence instead of leaving it as part of the kwargs
        if cidr_block:
            params['cidr_block'] = cidr_block

        params.update(kwargs)

        return self._post('/v1/auth/{}/map/user-id/{}'.format(mount_point, user_id), json=params)

    def get_user_id(self, user_id, mount_point='app-id', wrap_ttl=None):
        """
        GET /auth/<mount_point>/map/user-id/<user_id>
        """
        path = '/v1/auth/{0}/map/user-id/{1}'.format(mount_point, user_id)
        return self._get(path, wrap_ttl=wrap_ttl).json()

    def delete_user_id(self, user_id, mount_point='app-id'):
        """
        DELETE /auth/<mount_point>/map/user-id/<user_id>
        """
        return self._delete('/v1/auth/{0}/map/user-id/{1}'.format(mount_point, user_id))

    def create_vault_ec2_client_configuration(self, access_key, secret_key, endpoint=None):
        """
        POST /auth/aws-ec2/config/client
        """
        params = {
            'access_key': access_key,
            'secret_key': secret_key
        }
        if endpoint is not None:
            params['endpoint'] = endpoint

        return self._post('/v1/auth/aws-ec2/config/client', json=params)

    def get_vault_ec2_client_configuration(self):
        """
        GET /auth/aws-ec2/config/client
        """
        return self._get('/v1/auth/aws-ec2/config/client').json()

    def delete_vault_ec2_client_configuration(self):
        """
        DELETE /auth/aws-ec2/config/client
        """
        return self._delete('/v1/auth/aws-ec2/config/client')

    def create_vault_ec2_certificate_configuration(self, cert_name, aws_public_cert):
        """
        POST /auth/aws-ec2/config/certificate/<cert_name>
        """
        params = {
            'cert_name': cert_name,
            'aws_public_cert': aws_public_cert
        }
        return self._post('/v1/auth/aws-ec2/config/certificate/{0}'.format(cert_name), json=params)

    def get_vault_ec2_certificate_configuration(self, cert_name):
        """
        GET /auth/aws-ec2/config/certificate/<cert_name>
        """
        return self._get('/v1/auth/aws-ec2/config/certificate/{0}'.format(cert_name)).json()

    def list_vault_ec2_certificate_configurations(self):
        """
        GET /auth/aws-ec2/config/certificates?list=true
        """
        params = {'list': True}
        return self._get('/v1/auth/aws-ec2/config/certificates', params=params).json()

    def create_ec2_role(self, role, bound_ami_id=None, bound_account_id=None, bound_iam_role_arn=None,
                        bound_iam_instance_profile_arn=None, role_tag=None, max_ttl=None, policies=None,
                        allow_instance_migration=False, disallow_reauthentication=False, **kwargs):
        """
        POST /auth/aws-ec2/role/<role>
        """
        params = {
            'role': role,
            'disallow_reauthentication': disallow_reauthentication,
            'allow_instance_migration': allow_instance_migration
        }
        if bound_ami_id is not None:
            params['bound_ami_id'] = bound_ami_id
        if bound_account_id is not None:
            params['bound_account_id'] = bound_account_id
        if bound_iam_role_arn is not None:
            params['bound_iam_role_arn'] = bound_iam_role_arn
        if bound_iam_instance_profile_arn is not None:
            params['bound_iam_instance_profile_arn'] = bound_iam_instance_profile_arn
        if role_tag is not None:
            params['role_tag'] = role_tag
        if max_ttl is not None:
            params['max_ttl'] = max_ttl
        if policies is not None:
            params['policies'] = policies
        params.update(**kwargs)
        return self._post('/v1/auth/aws-ec2/role/{0}'.format(role), json=params)

    def get_ec2_role(self, role):
        """
        GET /auth/aws-ec2/role/<role>
        """
        return self._get('/v1/auth/aws-ec2/role/{0}'.format(role)).json()

    def delete_ec2_role(self, role):
        """
        DELETE /auth/aws-ec2/role/<role>
        """
        return self._delete('/v1/auth/aws-ec2/role/{0}'.format(role))

    def list_ec2_roles(self):
        """
        GET /auth/aws-ec2/roles?list=true
        """
        try:
            return self._get('/v1/auth/aws-ec2/roles', params={'list': True}).json()
        except InvalidPath:
            return None

    def create_ec2_role_tag(self, role, policies=None, max_ttl=None, instance_id=None,
                            disallow_reauthentication=False, allow_instance_migration=False):
        """
        POST /auth/aws-ec2/role/<role>/tag
        """
        params = {
            'role': role,
            'disallow_reauthentication': disallow_reauthentication,
            'allow_instance_migration': allow_instance_migration
        }
        if max_ttl is not None:
            params['max_ttl'] = max_ttl
        if policies is not None:
            params['policies'] = policies
        if instance_id is not None:
            params['instance_id'] = instance_id
        return self._post('/v1/auth/aws-ec2/role/{0}/tag'.format(role), json=params).json()

    def auth_ldap(self, username, password, mount_point='ldap', use_token=True, **kwargs):
        """
        POST /auth/<mount point>/login/<username>
        """
        params = {
            'password': password,
        }

        params.update(kwargs)

        return self.auth('/v1/auth/{0}/login/{1}'.format(mount_point, username), json=params, use_token=use_token)

    def auth_github(self, token, mount_point='github', use_token=True):
        """
        POST /auth/<mount point>/login
        """
        params = {
            'token': token,
        }

        return self.auth('/v1/auth/{0}/login'.format(mount_point), json=params, use_token=use_token)

    def auth(self, url, use_token=True, **kwargs):
        response = self._post(url, **kwargs).json()

        if use_token:
            self.token = response['auth']['client_token']

        return response

    def list_auth_backends(self):
        """
        GET /sys/auth
        """
        return self._get('/v1/sys/auth').json()

    def enable_auth_backend(self, backend_type, description=None, mount_point=None):
        """
        POST /sys/auth/<mount point>
        """
        if not mount_point:
            mount_point = backend_type

        params = {
            'type': backend_type,
            'description': description,
        }

        self._post('/v1/sys/auth/{0}'.format(mount_point), json=params)

    def disable_auth_backend(self, mount_point):
        """
        DELETE /sys/auth/<mount point>
        """
        self._delete('/v1/sys/auth/{0}'.format(mount_point))

    def create_role(self, role_name, **kwargs):
        """
        POST /auth/approle/role/<role name>
        """

        self._post('/v1/auth/approle/role/{0}'.format(role_name), json=kwargs)

    def list_roles(self):
        """
        GET /auth/approle/role
        """

        return self._get('/v1/auth/approle/role?list=true').json()

    def get_role_id(self, role_name):
        """
        GET /auth/approle/role/<role name>/role-id
        """

        url = '/v1/auth/approle/role/{0}/role-id'.format(role_name)
        return self._get(url).json()['data']['role_id']

    def set_role_id(self, role_name, role_id):
        """
        POST /auth/approle/role/<role name>/role-id
        """

        url = '/v1/auth/approle/role/{0}/role-id'.format(role_name)
        params = {
            'role_id': role_id
        }
        self._post(url, json=params)


    def get_role(self, role_name):
        """
        GET /auth/approle/role/<role name>
        """
        return self._get('/v1/auth/approle/role/{0}'.format(role_name)).json()

    def create_role_secret_id(self, role_name, meta=None):
        """
        POST /auth/approle/role/<role name>/secret-id
        """

        url = '/v1/auth/approle/role/{0}/secret-id'.format(role_name)
        params = {}
        if meta is not None:
            params['metadata'] = json.dumps(meta)

        return self._post(url, json=params).json()

    def get_role_secret_id(self, role_name, secret_id):
        """
        POST /auth/approle/role/<role name>/secret-id/lookup
        """
        url = '/v1/auth/approle/role/{0}/secret-id/lookup'.format(role_name)
        params = {
            'secret_id': secret_id
        }
        return self._post(url, json=params).json()

    def list_role_secrets(self, role_name):
        """
        GET /auth/approle/role/<role name>/secret-id?list=true
        """
        url = '/v1/auth/approle/role/{0}/secret-id?list=true'.format(role_name)
        return self._get(url).json()

    def get_role_secret_id_accessor(self, role_name, secret_id_accessor):
        """
        GET /auth/approle/role/<role name>/secret-id-accessor/<secret_id_accessor>
        """
        url = '/v1/auth/approle/role/{0}/secret-id-accessor/{1}'.format(role_name, secret_id_accessor)
        return self._get(url).json()

    def delete_role_secret_id(self, role_name, secret_id):
        """
        POST /auth/approle/role/<role name>/secret-id/destroy
        """
        url = '/v1/auth/approle/role/{0}/secret-id/destroy'.format(role_name)
        params = {
            'secret_id': secret_id
        }
        self._post(url, json=params)

    def delete_role_secret_id_accessor(self, role_name, secret_id_accessor):
        """
        DELETE /auth/approle/role/<role name>/secret-id/<secret_id_accessor>
        """
        url = '/v1/auth/approle/role/{0}/secret-id-accessor/{1}'.format(role_name, secret_id_accessor)
        self._delete(url)

    def create_role_custom_secret_id(self, role_name, secret_id, meta=None):
        """
        POST /auth/approle/role/<role name>/custom-secret-id
        """
        url = '/v1/auth/approle/role/{0}/custom-secret-id'.format(role_name)
        params = {
            'secret_id': secret_id
        }
        if meta is not None:
            params['meta'] = meta
        return self._post(url, json=params).json()

    def auth_approle(self, role_id, secret_id=None, mount_point='approle', use_token=True):
        """
        POST /auth/approle/login
        """
        params = {
            'role_id': role_id
        }
        if secret_id is not None:
            params['secret_id'] = secret_id

        return self.auth('/v1/auth/{0}/login'.format(mount_point), json=params, use_token=use_token)

    def close(self):
        """
        Close the underlying Requests session
        """
        self.session.close()

    def _get(self, url, **kwargs):
        return self.__request('get', url, **kwargs)

    def _post(self, url, **kwargs):
        return self.__request('post', url, **kwargs)

    def _put(self, url, **kwargs):
        return self.__request('put', url, **kwargs)

    def _delete(self, url, **kwargs):
        return self.__request('delete', url, **kwargs)

    def __request(self, method, url, headers=None, **kwargs):
        url = urljoin(self._url, url)

        if not headers:
            headers = {}

        if self.token:
            headers['X-Vault-Token'] = self.token

        wrap_ttl = kwargs.pop('wrap_ttl', None)
        if wrap_ttl:
            headers['X-Vault-Wrap-TTL'] = str(wrap_ttl)

        _kwargs = self._kwargs.copy()
        _kwargs.update(kwargs)

        response = self.session.request(method, url, headers=headers,
                                        allow_redirects=False, **_kwargs)

        # NOTE(ianunruh): workaround for https://github.com/ianunruh/hvac/issues/51
        while response.is_redirect and self.allow_redirects:
            url = urljoin(self._url, response.headers['Location'])
            response = self.session.request(method, url, headers=headers,
                                            allow_redirects=False, **_kwargs)

        if response.status_code >= 400 and response.status_code < 600:
            text = errors = None
            if response.headers.get('Content-Type') == 'application/json':
                errors = response.json().get('errors')
            if errors is None:
                text = response.text
            self.__raise_error(response.status_code, text, errors=errors)

        return response

    def __raise_error(self, status_code, message=None, errors=None):
        if status_code == 400:
            raise InvalidRequest(message, errors=errors)
        elif status_code == 401:
            raise Unauthorized(message, errors=errors)
        elif status_code == 403:
            raise Forbidden(message, errors=errors)
        elif status_code == 404:
            raise InvalidPath(message, errors=errors)
        elif status_code == 429:
            raise RateLimitExceeded(message, errors=errors)
        elif status_code == 500:
            raise InternalServerError(message, errors=errors)
        elif status_code == 501:
            raise VaultNotInitialized(message, errors=errors)
        elif status_code == 503:
            raise VaultDown(message, errors=errors)
        else:
            raise UnexpectedError(message)

def connect_to_vault_by_token(url, token):
    return HashiCorpVaultClient(url=url, token=token)

def connect_to_vault_by_ldap(url, user, password):
    client = HashiCorpVaultClient(url=url)
    client.auth_ldap(user, password)
    return client

credentials = json.loads(client.secrets.get('vault-credentials-testGenerateLamp'))
vault_client = connect_to_vault_by_ldap(url = 'https://localhost', user = credentials.user, password = credentials.password)

def get_secret(secret_path):
    return vault_client.read(secret_path)


ctx.instance.runtime_properties['tosca_id'] = ctx.instance.id
ctx.instance.runtime_properties['tosca_name'] = ctx.node.id

ctx.instance.runtime_properties['component_version'] = r'2.4'
ctx.instance.runtime_properties['port'] = r'80'
ctx.instance.runtime_properties['document_root'] = r'/var/www'

ctx.instance.runtime_properties['capabilities.data_endpoint.protocol'] = r'tcp'
ctx.instance.runtime_properties['capabilities.data_endpoint.secure'] = r'false'
ctx.instance.runtime_properties['capabilities.data_endpoint.network_name'] = r'PRIVATE'
ctx.instance.runtime_properties['capabilities.data_endpoint.initiator'] = r'source'
ctx.instance.runtime_properties['capabilities.data_endpoint.ip_address'] = get_attribute(ctx, 'ip_address')
ctx.instance.runtime_properties['capabilities.admin_endpoint.secure'] = r'true'
ctx.instance.runtime_properties['capabilities.admin_endpoint.protocol'] = r'tcp'
ctx.instance.runtime_properties['capabilities.admin_endpoint.network_name'] = r'PRIVATE'
ctx.instance.runtime_properties['capabilities.admin_endpoint.initiator'] = r'source'
ctx.instance.runtime_properties['capabilities.admin_endpoint.ip_address'] = get_attribute(ctx, 'ip_address')

