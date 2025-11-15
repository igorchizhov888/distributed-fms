# gNMI Adapter Design Specification

## Overview
Standalone gNMI target server that receives OpenConfig YANG alarm subscriptions from 5G EMS and publishes to Kafka.

## Architecture Pattern
Similar to `SnmpTrapReceiver` but for gNMI protocol:
- Listens on port 9830 (gNMI standard port)
- Receives YANG notifications from 5G EMS
- Normalizes to JSON
- Publishes to Kafka topic `fms-events`

## Deployment
- Docker container: `gnmi-adapter`
- Language: Java (consistent with project)
- Docker image: `distributed-fms-gnmi-adapter`

## Key Components

### 1. gNMI Server Implementation
- Listen on port 9830
- Accept gRPC connections from 5G EMS
- Handle Subscribe RPC calls
- Stream YANG notifications back

### 2. OpenConfig YANG Parser
- Parse OpenConfig alarm models
- Extract alarm fields:
  - alarm-id
  - alarm-type-id
  - severity
  - timestamp
  - description
  - device-id

### 3. Kafka Producer
- Normalize YANG data to JSON
- Publish to `fms-events` topic
- Include source: "gnmi"

### 4. Configuration
- Kafka broker address
- Listen port (9830)
- TLS certificates (optional)
- YANG model paths

## Throughput Target
- Thousands of alarms/sec
- Batch publishing to Kafka (optimize for high throughput)

## Status
- [ ] Phase 1: Design (In Progress)
- [ ] Phase 2: Implementation
- [ ] Phase 3: Testing
- [ ] Phase 4: Docker Integration
- [ ] Phase 5: Documentation

