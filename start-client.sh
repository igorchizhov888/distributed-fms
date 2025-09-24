#!/bin/bash
# This script starts a client node for the Distributed Fault Management System.
# Usage: ./start-client.sh <node-name>
# Example: ./start-client.sh client-1

# Default node name if not provided
NODE_NAME=${1:-client-1}

java --add-opens=java.base/java.nio=ALL-UNNAMED \
     --add-opens=java.base/java.lang=ALL-UNNAMED \
     --add-opens=java.base/java.util=ALL-UNNAMED \
     -cp target/distributed-fms-0.1.0-SNAPSHOT.jar \
     com.distributedFMS.DistributedFMSApplication client "$NODE_NAME"
