# Phase 2: Real gNMI Server Implementation

## Overview
Implement a production-ready gNMI server following OpenConfig standards and industry best practices.

## Implementation Strategy

### 1. Proto File Setup ✅ (Already have gnmi.proto)
- [ ] Use official OpenConfig `gnmi.proto` and `gnmi_ext.proto`
- [ ] Add version field to track API evolution
- [ ] Define custom extensions for FMS metadata (IID, correlation info)

### 2. Multiple Encodings Support
- [ ] JSON encoding (already partially working)
- [ ] Protobuf encoding for TypedValue
- [ ] Binary YANG data support via protobuf_bytes

### 3. Core gNMI RPCs
- [ ] `Capabilities()` - report supported models, version, encoding
- [ ] `Get()` - read current state
- [ ] `Set()` - modify configuration (may be limited for FMS)
- [ ] `Subscribe()` - telemetry streaming (PRIMARY for FMS)

### 4. Subscription Modes
- [ ] `ONCE` - single snapshot
- [ ] `POLL` - on-demand polling
- [ ] `STREAM` - continuous with sub-modes:
  - [ ] `ON_CHANGE` - send only on value change
  - [ ] `SAMPLE` - periodic sampling
  - [ ] `TARGET_DEFINED` - server decides

### 5. Error Handling
- [ ] Standard gRPC error codes
- [ ] Structured error responses with context
- [ ] Proper handling of invalid paths, encoding errors

### 6. Security (TLS)
- [ ] gRPC TLS support
- [ ] Optional client certificate validation
- [ ] Authentication hooks

### 7. Testing & Interoperability
- [ ] Unit tests for each RPC
- [ ] Integration tests with mock clients
- [ ] Validation against OpenConfig reference implementations

### 8. Build & Deployment
- [ ] Proto code generation in build pipeline
- [ ] Version management with Git tags
- [ ] Docker container for gnmi-adapter service

## Architecture
```
GnmiServerImpl (gRPC service)
    ├── CapabilitiesHandler
    ├── GetHandler
    ├── SetHandler
    └── SubscribeHandler
         ├── OnceSubscriber
         ├── PollSubscriber
         └── StreamSubscriber (ON_CHANGE, SAMPLE, TARGET_DEFINED)

FMS Integration Layer
    ├── AlarmCache (Ignite)
    ├── CorrelationEngine
    └── Kafka Consumer (for real-time updates)

Encoding Layer
    ├── JSONEncoder
    └── ProtobufEncoder
```

## Deliverables

1. **gnmi_service.proto** - Complete gNMI service definition
2. **GnmiServerImpl.java** - Main gRPC service implementation
3. **SubscriptionManager.java** - Handle ONCE/POLL/STREAM modes
4. **EncodingFactory.java** - JSON/Protobuf encoding support
5. **GnmiTestClient.java** - Test client for interoperability
6. **Dockerfile.gnmi-adapter** - Production container
7. **Integration tests** - Verify all RPCs work correctly

## Timeline Estimate
- Proto definition: 1 session
- Core RPCs (Get, Capabilities): 1 session
- Subscribe implementation: 2 sessions
- Encoding & error handling: 1 session
- Testing & validation: 1 session

## Success Criteria
- ✅ All 4 main RPCs implemented
- ✅ Subscribe works with STREAM mode
- ✅ Supports JSON and Protobuf encoding
- ✅ Proper error handling and gRPC codes
- ✅ TLS support enabled
- ✅ Passes interoperability tests
- ✅ Fully containerized in Docker
