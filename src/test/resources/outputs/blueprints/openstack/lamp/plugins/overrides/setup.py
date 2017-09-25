from setuptools import setup

# Replace the place holders with values for your project

setup(

    # Do not use underscores in the plugin name.
    name='a4c-overrides',

    version='0.1',
    author='alien',
    author_email='alien@fastconnect.fr',
    description='custom overrides',

    # This must correspond to the actual packages in the plugin.
    packages=['a4c_common','openstack'],

    license='Apache',
    zip_safe=True,
    install_requires=[
        # Requirements from cloudify-openstack-plugin 1.3.1
        'cloudify-plugins-common>=3.3.1',
        'python-novaclient==2.26.0',
        'python-keystoneclient==1.6.0',
        'python-neutronclient==2.6.0',
        'python-cinderclient==1.2.2',
        'IPy==0.81'
    ]
)