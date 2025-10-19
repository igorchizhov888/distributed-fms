#!/bin/bash

echo "Starting trap receiver..."

export APP_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

java -cp target/distributed-fms-${APP_VERSION}.jar com.distributedFMS.agent.SnmpTrapReceiver &
