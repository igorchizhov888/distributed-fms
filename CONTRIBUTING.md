**docker/docker-compose.yml** (basic setup):
```yaml
version: '3.8'
services:
  fms-node1:
    build: .
    ports:
      - "8080:8080"
      - "47500:47500"
    environment:
      - IGNITE_NODE_NAME=node1
      - GEOGRAPHIC_REGION=us-east
    # More configuration coming soon
