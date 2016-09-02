#!/bin/bash

# This script suppose that lsblk is available on the target machine.
#
# Tips: The command below show how to retrieve the root device
# lsblk -ao PKNAME,NAME,TYPE,MOUNTPOINT | grep / | cut -d ' ' -f-1
#

CFY_DEVICE=$DEVICE_NAME
MATCHED_DEVICE=""

retry_sleep=20
max_retry=5

#
# Usually, devices like /dev/sdb are changed to /dev/xvdb or /dev/hdb
# The device is renamed but the trailing letter remains the same.
# The following loop try to find an existing device on the machine with the same trailing letter coming from the given device.
#
# Limitations: http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/device_naming.html
#  - This script do not handle the devices finishing with a digit (i.e. /dev/sd[f-p][1-6])
#  - This script do not handle the case where the device name trailing letter is incremented (i.e. /dev/sdb becomes /dev/xvdf)
#
mapDevice() {
  DEVICE_TO_MAP=$1
  for device in $(sudo lsblk -no KNAME,TYPE | grep disk | cut -d ' ' -f-1) ; do
    #echo mapping device $device
    if [ "${DEVICE_TO_MAP: -1}" = "${device: -1}" ] ; then
      MATCHED_DEVICE=/dev/$device
      break
    fi
  done

  echo $MATCHED_DEVICE
}

MATCHED_DEVICE=$(mapDevice $CFY_DEVICE)

retry_count=1
while [ -z "$MATCHED_DEVICE" ] && [ $retry_count -le $max_retry ] ; do
  echo "No device matched for '$CFY_DEVICE'. Waiting $retry_sleep seconds before retry ($retry_count/$max_retry)..."
  sleep $retry_sleep
  retry_count=$((retry_count+1))
  MATCHED_DEVICE=$(mapDevice $CFY_DEVICE)
done

if [ -z "$MATCHED_DEVICE" ] ; then
  all_devices=$(sudo lsblk -npo KNAME,TYPE | grep disk | cut -d ' ' -f-1)
  echo "Unable to match '$CFY_DEVICE' with an existing device on the machine ($(echo $all_devices))"
  exit 1
fi

echo Device $CFY_DEVICE mapped to $MATCHED_DEVICE
echo $MATCHED_DEVICE
exit 0
