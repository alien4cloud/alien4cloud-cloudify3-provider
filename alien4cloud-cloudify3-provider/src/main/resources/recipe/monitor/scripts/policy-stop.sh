#! /bin/bash
DPLID=$(ctx deployment id)
ctx logger info "disabling monitoring for deployment ${DPLID}"
MONITORING_DIR="$BASE_DIR/${DPLID}"

read PID < $MONITORING_DIR/pid_file
sudo kill -9 $PID

(crontab -l ; cat $MONITORING_DIR/policycron) 2>&1 | grep -v "no crontab" | grep -v $MONITORING_DIR/log |  sort | uniq | crontab -

ctx logger info "monitoring cron job for deployment ${DPLID}, nodes ${NTM} removed"

rm -rf $MONITORING_DIR
