#!/bin/sh
if [ -z "$confs_directory" ]; then
    echo "confs_directory is not set"
    exit 1
else
    echo "confs_directory is ${confs_directory}"
fi
if [ -z "$REQUIREMENT_PROPERTY" ]; then
    echo "REQUIREMENT_PROPERTY is not set"
    exit 1
else
    echo "CAPABILITY_PROPERTY is ${REQUIREMENT_PROPERTY}"
fi
if [ -z "$CAPABILITY_PROPERTY" ]; then
    echo "CAPABILITY_PROPERTY is not set"
    exit 1
else
    echo "CAPABILITY_PROPERTY is ${CAPABILITY_PROPERTY}"
fi
if [ -f "$confs_directory/log.properties" ]; then
    echo "confs_directory/log.properties is copied"
else
    echo "confs_directory/log.properties is not copied"
    exit 1
fi

if [ -f "$confs_directory/settings.properties" ]; then
    echo "confs_directory/settings.properties is copied"
else
    echo "confs_directory/settings.properties is not copied"
    exit 1
fi

if [ -f "$confs_directory/test/nestedDirTest.txt" ]; then
    echo "confs_directory/test/nestedDirTest.txt is copied"
else
    echo "confs_directory/test/nestedDirTest.txt is not copied"
    exit 1
fi