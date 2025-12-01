# Changelog

All notable changes to the Distributed FMS project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.2.0-topology] - 2025-11-27

### Added
- **GNN Topology Learning Service** - Python-based topology learning using Graph Attention Networks (GAT)
  - Learns network topology from alarm co-occurrence patterns
  - Supports 30 nodes, 870 edges with confidence scoring (0.0-1.0)
  - Temporal, spatial, and causal relationship detection
  - gRPC server for training and topology retrieval
  - PyTorch Geometric implementation with 2 GAT layers, 8 attention heads

- **Interactive Network Topology Visualization**
  - React-based force-directed graph visualization
  - Tab-based UI navigation (Alarms / Network Topology)
  - Confidence slider for edge filtering
  - Color-coded nodes by device type (core/switch/router/host)
  - Node labels with readable fonts (size 3)
  - Interactive controls: Train Model, Refresh, zoom, pan, node click
  - Real-time statistics (nodes, edges, confidence threshold)
  - Directional particles showing network flow
  - Tooltips with confidence/causality/co-occurrence data

- **Multi-Service Architecture**
  - Java FMS Server ↔ Python Topology Service via gRPC
  - React UI ↔ Python Service via gRPC-web through Envoy proxy
  - Envoy smart routing (port 50051 for FMS, 50052 for topology)

- **New Components**
  - `topology-service/` - Complete Python GNN service
  - `TopologyView.js` - React visualization component
  - `TopologyServiceClient.java` - Java client for topology service
  - `AlarmHistoryExporter.java` - Export alarms for ML training

- **Documentation**
  - `CLEAR_CORRELATION_SUMMARY.md` - Clear event handling guide
  - `topology-service/TOPOLOGY_QUICKSTART.md` - Quick start guide
  - `topology-service/gnn_topology_guide.md` - GNN implementation details
  - `docs/DEMO.md` - Demo branch documentation

### Changed
- Updated `FMS.proto` with TopologyService RPC definitions
  - `TrainTopology` - Train GNN on historical alarms
  - `TrainTopologyWithAlarms` - Train with provided alarm data
  - `GetTopology` - Retrieve learned topology with filtering

- Enhanced Envoy configuration with path-based routing
  - `/com.distributedFMS.TopologyService` routes to topology service (port 50052)
  - Preserves existing FMS server routes (port 50051)

- Updated UI dependencies
  - Added `react-force-graph-2d@1.25` for graph visualization
  - Regenerated gRPC-web stubs for TopologyService

### Fixed
- **Critical**: Alarm severity bug - Changed default from INFO to MAJOR
  - Root cause: ClearCorrelator treated INFO alarms as clear events
  - Impact: Alarms weren't being stored in Ignite cache
  - Result: All alarms now properly stored and displayed (5 alarms visible)

- Removed obsolete docker-compose version attribute (eliminated warnings)
- Fixed Docker Compose APP_VERSION configuration via .env file
- Fixed Envoy routing for multi-service architecture

### Removed
- Legacy shell scripts replaced by Docker Compose workflow
  - `build-and-run.sh`, `run.sh`, `start-*.sh`, `test-*.sh`
  - Improves consistency and reduces maintenance burden

### Infrastructure
- Added `.env` file for environment variables (APP_VERSION)
- Enhanced `.gitignore` for Python/Node artifacts
- Docker Compose orchestration for 7 services
- Envoy proxy for gRPC-web gateway

### Technical Metrics
- **Lines of Code Added**: 16,000+
  - Python topology service: ~800 lines
  - React UI components: ~700 lines
  - Java integration: ~500 lines
  - Proto definitions: ~200 lines
  - Tests and documentation: ~1,000+ lines

- **Performance**
  - Topology training: ~5-10 seconds (50 epochs, 30 nodes)
  - Graph rendering: <1 second (870 edges)
  - Alarm processing: <1 second latency
  - UI responsiveness: Real-time slider updates

### Dependencies
- Python: PyTorch 2.1, PyTorch Geometric 2.4, grpcio 1.60
- React: react-force-graph-2d 1.25, grpc-web 1.4
- Infrastructure: Envoy 1.28, Kafka 3.7, Ignite 2.17

  - Identifies a "root cause" alarm for each correlated group.
- **Correlation Fields**: Added `correlation_id` and `root_cause_alarm_id` to the alarm model (`FMS.proto`, `Alarm.java`) to track relationships.
- **Parent/Child UI Grouping**: The React UI now groups alarms by `correlationId`.
  - Parent alarms are highlighted and display a toggle (▶/▼) to show/hide children.
  - Child alarms are nested visually under their parent.
  - A badge indicates the number of child alarms in a group.
- **New UI Columns**: Added "Correlation ID" and "Root Cause ID" columns to the UI table.

### Changed
- **UI Logic**: `App.js` was completely refactored to handle the new grouping, expand/collapse state, and rendering logic.
- **Deduplication Logic**: `DeduplicationCorrelator` now uses the `alarm.getAlarmId()` as the cache key to prevent stale data issues.
- **Correlation Engine**: The engine was updated to ensure all alarms within a correlation group (including the first one) are correctly updated in the cache with the proper IDs.
- **Styling**: `App.css` was updated to provide distinct visual styles for parent, child, and standalone alarms.

### Fixed
- **Critical Bug: First Alarm Missing IDs**: Fixed a race condition where the first alarm in a sequence would not receive its correlation IDs. All alarms in a group are now correctly updated.
- **Tally Count Initialization**: The `tallyCount` in `Alarm.java` is now correctly initialized to `1` on creation.
- **Deduplication Return Value**: `DeduplicationCorrelator.deduplicate()` now correctly returns the processed alarm, ensuring the correlation engine receives the most up-to-date data.

## [1.1.0] - 2025-10-25
### Added
- **Extended Alarm Fields**: Added 5 new fields to alarm model:
  - Node Alias: Network node identifier
  - Probable Cause: Root cause analysis
  - Alarm Group: Alarm categorization
  - Summary: Alarm summary text
  - IID: Ignite Internal ID (cache key)
- **Horizontal Scrolling Table**: All 15 alarm columns now visible with smooth horizontal scrolling.
- **Severity Level Mapping**: Severity converted to numeric levels (1=INFO, 2=WARNING, 3=CRITICAL).
- **Smart Description Parsing**: Extracts readable message from SNMP OID data.
- **Sticky Table Headers**: Headers remain visible while scrolling.

### Changed
- Updated proto definitions (FMS.proto) with new alarm fields
- Enhanced Alarm.java model with new field definitions
- Updated AlarmServiceImpl to map IID to cache key
- Improved React table with 15 columns instead of 4
- Enhanced App.css with scrolling and responsive design

### Fixed
- Description field now shows readable message instead of raw OID JSON
- Severity field now shows numeric values (1, 2, 3) instead of text
# Changelog
All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-10-24
### Added
- **React Web UI**: Implemented a modern React-based dashboard for viewing alarms in real-time.
  - Live alarm table with sorting and filtering capabilities
  - Real-time data streaming via gRPC-Web
  - Professional styling and responsive design
- **gRPC-Web Support**: Full gRPC-Web implementation enabling browser-based clients to connect to backend services.
  - Auto-generated gRPC-Web stubs using protoc-gen-grpc-web
  - Support for server-streaming responses
- **Envoy Proxy**: Integrated Envoy proxy for gRPC-Web protocol translation and CORS support.
  - HTTP/2 to gRPC-Web conversion
  - Automatic CORS header management
  - Load balancing and health checks
- **Complete Docker Containerization**: Containerized all services for seamless deployment.
  - Dockerfile for FMS Server (Java)
  - Dockerfile for SNMP Trap Receiver
  - Dockerfile for React UI (multi-stage build with Nginx)
  - Dockerfile for Envoy Proxy
  - docker-compose.yml for orchestration
- **Automation Script**: Created `start-fms.sh` for one-command deployment of the entire system.
  - Automated container building and starting
  - SNMP trap generation for testing
  - Automatic browser launch
  - Service health verification
- **AlarmClient.js**: gRPC-Web client library for React components to subscribe to alarm streams.

### Changed
- **README.md**: Completely revamped with Docker, UI, and gRPC-Web information.
- **Architecture**: Evolved from CLI-only backend to full-stack system with web UI.
  - Added Envoy proxy layer for gRPC-Web translation
  - React UI as primary user interface
  - Maintained backward compatibility with existing gRPC services

### Fixed
- **gRPC Stubs Generation**: Regenerated all gRPC and gRPC-Web stubs to match current proto definitions.
- **CORS Configuration**: Fixed Envoy CORS settings for proper browser-based gRPC-Web connectivity.
- **React Component Rendering**: Fixed CSS styling issues preventing alarm table display.
  - Updated App.css to properly display table with correct layout
  - Fixed header height issues that were hiding content
- **Docker Port Mapping**: Corrected network configuration for host mode Docker containers.

### Technical Details
- **Frontend Stack**: React 18, JavaScript ES6+, CSS3, gRPC-Web protocol
- **Backend Stack**: Java 17, Apache Ignite, Apache Kafka, gRPC
- **Infrastructure**: Docker, Docker Compose, Envoy v1.28
- **Build Tools**: Maven, npm, protoc with gRPC-Web plugin

### Testing
- End-to-end pipeline verified: SNMP → Kafka → Ignite → gRPC → Envoy → gRPC-Web → React UI
- Tested with multiple SNMP traps and verified real-time UI updates
- Verified alarm deduplication and tally counting in UI

## [0.2.0] - 2025-10-14
### Added
- **gRPC Alarm Service:** Implemented a new gRPC service (`AlarmService`) to query for alarms stored in the Ignite cache. The service supports streaming responses.
- **Query Filtering:** The `AlarmService` supports filtering alarms by `deviceId`, `severity`, and `eventType`.
- **gRPC Test Client:** Created a new client (`AlarmQueryClient`) to demonstrate and test the `AlarmService`.
- **Integration Testing:** Created a stable, end-to-end integration test (`EventProcessingTest`) for the full event pipeline using Testcontainers for Kafka.

### Changed
- **Refactored `deviceId`:** Renamed `sourceDevice` to `deviceId` across the codebase for clarity and consistency.
- **Refactored Severity to String:** Changed the `severity` field in the `Alarm` model from an enum to a `String` to ensure reliable SQL querying in Ignite.

### Fixed
- **Application Startup Logic:** Corrected a major bug in `DistributedFMSApplication` that prevented the gRPC server from starting in server mode.
- **gRPC Server Stability:** Fixed multiple bugs that caused the gRPC server to crash, including type mismatches in the query logic and a non-static main method.
- **Integration Test Stability:** Resolved several race conditions in the integration test related to Kafka topic creation and consumer offset policies.
- **Event Deduplication:** Implemented and verified end-to-end event deduplication logic. The `DeduplicationCorrelator` now correctly identifies duplicate events based on a correlation key and updates a tally count in the `Alarm` object, rather than creating new alarms for each duplicate event.

## [0.1.0] - 2025-09-30
### Added
- **Kafka Integration:** Implemented an event-driven pipeline using Apache Kafka for message bus communication.
- **Ignite Cache:** Integrated Apache Ignite as a distributed cache (`fms-events-cache`) to store events consumed from Kafka.
- **Event Simulation:** Created `SnmpEventProducer` to simulate SNMP events and publish them to the Kafka `fms-events` topic.
- **Java 17 Compatibility:** Introduced `run.sh` script with necessary `--add-opens` flags to ensure compatibility with Java 17+ and resolve reflection errors from Apache Ignite.
- **Docker Environment:** Added a `docker-compose.yml` file to manage local Kafka and Zookeeper services.
- Initial implementation of Distributed Fault Management System (FMS) core components.
- Apache Ignite cluster configuration for FMS.
- Event processing logic (`FMSEventProcessor`).
- Core `Alarm` data model.
- Demonstration of distributed alarm processing with geographic affinity.

### Fixed
- **Kafka Connectivity:** Resolved persistent connection failures between the application and the Kafka container by resetting the Docker environment to clear stale Zookeeper data (`NodeExistsException`).
- **Logging Configuration:** Fixed an issue where consumer logs were not appearing by ensuring the `java.util.logging.properties` file is correctly loaded by the JVM at runtime.
- Compilation errors related to `AlarmSeverity` and `AlarmStatus` enum visibility by refactoring them into separate public files.
