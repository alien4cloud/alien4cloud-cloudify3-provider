from setuptools import setup

# Replace the place holders with values for your project

setup(

    # Do not use underscores in the plugin name.
    name='a4c-overrides-openstack',

    version='2.7.1',
    author='alien4cloud',
    author_email='alien4cloud@fastconnect.fr',
    description='custom overrides',

    # This must correspond to the actual packages in the plugin.
    packages=['a4c_common','cinder_plugin'],

    license='Apache',
    zip_safe=True,
    install_requires=[
        # Requirements from cloudify-openstack-plugin 2.7.1
        'cloudify-plugins-common>=3.4.1',
        'keystoneauth1>=2.16.0,<3',
        'python-novaclient==7.0.0',
        'python-keystoneclient==3.5.0',
        'python-neutronclient==6.0.0',
        'python-cinderclient==1.9.0',
        'python-glanceclient==2.5.0',
        'IPy==0.81'
    ]
)