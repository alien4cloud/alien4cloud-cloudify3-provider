#!/bin/bash
set -e

echo "**** BEGIN RELATIONSHIP SUPPORT TO VOLUME"

device=$TARGET_BLOCKSTORAGE_DEVICE

path="/tmp/support_hss/"
file="${path}devices"

mkdir -p $path

echo $device >> $file
echo "\n" >> $file

echo "**** END RELATIONSHIP SUPPORT TO VOLUME"

exit 0 
