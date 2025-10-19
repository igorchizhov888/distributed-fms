#!/bin/bash
LOG_FILE="client.log"
if [ "$1" = "server" ]; then
    LOG_FILE="server.log"
fi

sleep 5

echo "Loading version from pom.xml..."
export APP_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
echo "Running version: $APP_VERSION"

java --add-opens=java.base/java.nio=ALL-UNNAMED \
     --add-opens=java.base/java.util=ALL-UNNAMED \
     --add-opens=java.base/java.lang=ALL-UNNAMED \
     --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
     --add-opens=java.base/java.io=ALL-UNNAMED \
     --add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
     --add-opens=java.base/sun.misc=ALL-UNNAMED \
     -jar target/distributed-fms-${APP_VERSION}.jar "$@" > "$LOG_FILE" 2>&1 &
