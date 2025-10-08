# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
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