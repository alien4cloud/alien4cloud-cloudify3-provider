#! /bin/bash
#exit
pip install influxdb
if [ $? -gt 0 ]; then
  ctx logger info "Aborting ... "
  exit
fi

ctx logger info "Retrieving nodes_to_monitor and deployment_id"

NTM="$(ctx node properties nodes_to_monitor)"
ctx logger info "nodes_to_monitor = ${NTM}"
NTM=$(echo ${NTM} | sed "s/u'/'/g")
DPLID=$(ctx deployment id)
ctx logger info "deployment_id = ${DPLID}"
ctx logger info "checking interval: ${MONITORING_INTERVAL}"
MONITORING_DIR="$BASE_DIR/${DPLID}"
LOC=$(ctx download-resource monitor/scripts/policy.py)

mkdir -p $MONITORING_DIR

CRON_FILE=$MONITORING_DIR/policycron

rm $CRON_FILE
rm $MONITORING_DIR/log

COMMAND="/opt/mgmtworker/env/bin/python ${LOC} \"${NTM}\" ${DPLID} $MONITORING_DIR >> $MONITORING_DIR/log 2>&1"
echo "*/$MONITORING_INTERVAL * * * * $COMMAND" >> $CRON_FILE

(crontab -l ; cat $CRON_FILE) 2>&1 | grep -v "no crontab" | sort | uniq | crontab -

ctx logger info "monitoring cron job added for deployment ${DPLID}, nodes ${NTM}"
