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
    packages=['a4c_common','ec2'],

    license='Apache',
    zip_safe=True,
    install_requires=[
        # Requirements from cloudify-aws-plugin 1.4.1
        'cloudify-plugins-common>=3.3.1',
        'boto==2.38.0',
        'pycrypto==2.6.1'
    ]
)