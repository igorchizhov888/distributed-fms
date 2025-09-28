#!/bin/bash
# This script starts a server node for the Distributed Fault Management System.
# Usage: ./start-server.sh <node-name>
# Example: ./start-server.sh node-1

# Default node name if not provided
NODE_NAME=${1:-node-1}

java -Djava.util.logging.config.file=config/java.util.logging.properties \
     --add-opens=java.base/java.nio=ALL-UNNAMED \
     --add-opens=java.base/java.lang=ALL-UNNAMED \
     --add-opens=java.base/java.util=ALL-UNNAMED \
     -cp target/distributed-fms-0.1.0-SNAPSHOT.jar \
     com.distributedFMS.DistributedFMSApplication server "$NODE_NAME"
