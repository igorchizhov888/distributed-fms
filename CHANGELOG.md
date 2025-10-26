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
