## [Unreleased]

### Added
- Initial implementation of Distributed Fault Management System (FMS) core components.
- Apache Ignite cluster configuration for FMS.
- Event processing logic (`FMSEventProcessor`).
- Core `Alarm` data model.
- Demonstration of distributed alarm processing with geographic affinity.

### Fixed
- Compilation errors related to `AlarmSeverity` and `AlarmStatus` enum visibility by refactoring them into separate public files.
