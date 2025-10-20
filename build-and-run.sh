#!/bin/bash

# Load the version from the "state file" into an environment variable
export APP_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

# Tell the world what's happening
echo "Building and running version: $APP_VERSION"

# Run your build. Docker Compose will automatically read $APP_VERSION
# and pass it to the Dockerfile.
docker compose up --build -d
