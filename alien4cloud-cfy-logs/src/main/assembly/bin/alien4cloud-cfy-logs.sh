#!/bin/sh -e

cd `dirname $0`
workdir=$PWD
cd $OLDPWD

if type -p java; then
    echo "found java executable in PATH"
    _java=java
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
    echo "found java executable in ${JAVA_HOME}"
    _java="$JAVA_HOME/bin/java"
elif [ ! -d "$workdir/java" ]; then
    echo "JAVA is required for the log application to run"
fi


nohup $_java -Djava.security.egd=file:/dev/./urandom -jar alien4cloud-cfy-logs-${project.version}.war &
echo $! > $workdir/nohup.pid
