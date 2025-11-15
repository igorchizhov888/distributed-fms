# gNMI Implementation Plan - Phase-Based Approach

## Current Status
- ✅ Mock gNMI Simulator created (JSON-based, compiles)
- ✅ GnmiAdapter skeleton created
- ✅ Architecture updated to multi-protocol support
- ❌ Not yet containerized
- ❌ Not yet tested end-to-end

## Phase 1: Docker Containerization & E2E Testing (NEXT)
**Goal:** Prove full pipeline works with mock gNMI data

### Tasks:
1. Create `Dockerfile.gnmi-simulator`
   - Containerize MockGnmiSimulator
   - Run inside Docker network
   - Can access `kafka:29092`

2. Update `docker-compose.yml`
   - Add `gnmi-simulator` service
   - Add `gnmi-adapter` service (placeholder)
   - Ensure network connectivity

3. Test Full Pipeline
```
   gnmi-simulator (Docker)
        ↓
     Kafka (Docker)
        ↓
   EventConsumer (Docker)
        ↓
   Ignite Cache (Docker)
        ↓
   React UI (Browser)
```

4. Verify in UI
   - Alarms appear with `source: "gnmi"`
   - Correlation works
   - Parent/child grouping works

## Phase 2: Real gNMI Server Implementation (FUTURE)
**Goal:** Production-ready gNMI with protobuf

### Tasks:
1. Implement gNMI server in GnmiAdapter
   - Listen on port 9830
   - Accept gRPC connections
   - Handle Subscribe RPC
   - Stream YANG notifications

2. Use real OpenConfig YANG models
   - 3GPP TS 28.622 (5G NRM)
   - Protocol Buffer definitions

3. Create Dockerfile.gnmi (final)

4. Test with actual 5G EMS client (when available)

## Architecture Principle (CRITICAL)
**ALL adapters and components run in Docker containers**
- No host machine execution
- Use Docker internal DNS
- Consistent deployment model

## Key Files to Create
- `Dockerfile.gnmi-simulator`
- `docker-compose.yml` (update)
- `GNMI_IMPLEMENTATION_PLAN.md` (this file)

## Success Criteria
- ✅ All containers start successfully
- ✅ Mock alarms flow through pipeline
- ✅ UI displays gNMI alarms correctly
- ✅ Correlation works cross-protocol (SNMP + gNMI)
