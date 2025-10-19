#!/bin/bash
# This script starts a server node for the Distributed Fault Management System.
# Usage: ./start-server.sh <node-name>
# Example: ./start-server.sh node-1

# Default node name if not provided
NODE_NAME=${1:-node-1}

export APP_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

java -Djava.util.logging.config.file=config/java.util.logging.properties \
     --add-opens=java.base/java.nio=ALL-UNNAMED \
     --add-opens=java.base/java.lang=ALL-UNNAMED \
     --add-opens=java.base/java.util=ALL-UNNAMED \
     --add-opens=java.base/java.util.concurrent=ALL-UNNAMED \
     --add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
     --add-opens=java.base/sun.util.calendar=ALL-UNNAMED \
     -jar target/distributed-fms-${APP_VERSION}.jar "server" "$NODE_NAME"
