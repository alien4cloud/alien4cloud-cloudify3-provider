from setuptools import setup

# Replace the place holders with values for your project

setup(

    # Do not use underscores in the plugin name.
    name='custom-wf-plugin',

    version='0.1',
    author='alien',
    author_email='alien@fastconnect.fr',
    description='custom generated workflows',

    # This must correspond to the actual packages in the plugin.
    packages=['plugin'],

    license='Apache',
    zip_safe=True,
    install_requires=[
        # Necessary dependency for developing plugins, do not remove!
        "cloudify-plugins-common>=3.2"
    ],
    test_requires=[
        "cloudify-dsl-parser>=3.2"
        "nose"
    ]
)