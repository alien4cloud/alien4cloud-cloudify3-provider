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
    echo "Installation JAVA"

    JAVA_URL="https://fastconnect.org/owncloud/public.php?service=files&t=121703a2a53853b0ca32bfbc903c6208&download"

    mkdir $workdir/java
    cd $workdir/java

    echo "Downloading jdk"
    curl --insecure --silent -o $workdir/java.tar.gz -O $JAVA_URL

    echo "Extracting java"
    tar xfz $workdir/java.tar.gz -C $workdir/java
    mv $workdir/java/*/* $workdir/java
    echo "Using java executable in ${workdir}/java/bin"

    _java=$workdir/java/bin/java
    rm -f $workdir/java.tar.gz
fi


nohup $_java -Djava.security.egd=file:/dev/./urandom -jar alien4cloud-cfy-logs-${project.version}.war &
echo $! > $workdir/nohup.pid
