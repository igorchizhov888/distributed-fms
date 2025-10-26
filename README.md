# Distributed Fault Management System
A geo-distributed, high-performance fault management system for telecommunications networks, built on Apache Ignite and Apache Kafka, with a modern React web UI.

## Problem Statement
Traditional centralized fault management systems cannot handle modern network event volumes and geographic distribution requirements. This project implements distributed, edge-based fault management using in-memory data grids and a message bus for event ingestion, exposed through a web-based dashboard.

## Key Features
- **Event-Driven Architecture**: Ingests events via an Apache Kafka message bus.
- **Geographic Distribution**: Process events at network edge locations using custom affinity functions in Apache Ignite.
- **Real-time Processing**: Handle thousands of events per second per node with sub-millisecond response times.
- **Distributed Caching**: Events are consumed and stored in a distributed Apache Ignite cache.
- **Active-Active Clustering**: Automatic failover with zero data loss across distributed nodes (feature of Ignite).
- **Universal Network Support**: Monitor any network type by adding appropriate software adapters.
- **Modern Web UI**: React-based dashboard for viewing alarms in real-time.
- **gRPC & gRPC-Web**: Full-duplex streaming support for real-time data updates.
- **Containerized**: Complete Docker containerization for easy deployment.

## Architecture
```
SNMP Traps → SnmpTrapReceiver → Kafka → FMS Server → Ignite Cache
                                                ↓
                                        gRPC Service
                                        (via Envoy Proxy)
                                                ↓
                                        React UI
                                      (gRPC-Web)
```

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+
- Docker and Docker Compose
- Node.js 18+ (for UI development)
- `snmptrap` command-line tool

### Fastest Way: Automated Script (Recommended)
Start everything with one command:

```bash
./start-fms.sh
```

This script will:
1. Stop any running containers
2. Build all Docker images
3. Start all services (Kafka, Zookeeper, FMS Server, Envoy Proxy, React UI)
4. Generate sample SNMP traps
5. Open the web UI in your browser (http://localhost:3000)

### Manual Setup

#### 1. Build the Project
```bash
mvn clean install
```

#### 2. Start All Services with Docker Compose
```bash
export APP_VERSION=0.1.0-SNAPSHOT
docker compose up -d
```

This starts:
- Zookeeper (port 2181)
- Kafka (port 9092)
- FMS Server (port 50051 - gRPC)
- SNMP Trap Receiver (port 10162 - UDP)
- Envoy Proxy (port 8080 - gRPC-Web)
- React UI (port 3000 - HTTP)

#### 3. Send SNMP Traps
```bash
snmptrap -v 2c -c public 127.0.0.1:10162 '' 1.3.6.1.6.3.1.1.5.4 1.3.6.1.4.1.8072.2.3.0.1 s "Test Alarm"
```

#### 4. View the UI
Open http://localhost:3000 in your browser to see the alarms table update in real-time.

## Accessing the System

| Service | URL/Port | Purpose |
|---------|----------|---------|
| React UI | http://localhost:3000 | Web dashboard for viewing alarms |
| Envoy Admin | http://localhost:9901 | Envoy proxy administration |
| gRPC Server | localhost:50051 | Backend gRPC service (internal) |
| Kafka | localhost:9092 | Message broker (internal) |

## Alarm Fields

The FMS displays the following alarm information in the real-time dashboard:

| Field | Description |
|-------|-------------|
| Alarm ID | Unique identifier for the alarm |
| Device ID | Source device that triggered the alarm |
| Node Alias | Network node identifier |
| Severity | Alarm severity level (1=INFO, 2=WARNING, 3=CRITICAL) |
| Alarm Group | Category or group classification |
| Probable Cause | Root cause analysis |
| Summary | Brief alarm summary |
| Description | Detailed alarm description |
| Status | Current alarm status (ACTIVE, CLEARED, etc.) |
| Event Type | Type of network event |
| Geographic Region | Geographic location of source |
| Tally Count | Number of duplicate occurrences |
| First Occurrence | Timestamp of first occurrence |
| Last Occurrence | Timestamp of most recent occurrence |
| IID | Ignite Internal ID (cache key) |

## Stopping the System
```bash
docker compose down
```

## Technology Stack
- **Backend**: Java 17, Apache Ignite, Apache Kafka, gRPC
- **Frontend**: React 18, JavaScript, CSS3
- **Proxy**: Envoy (for gRPC-Web support)
- **Infrastructure**: Docker, Docker Compose
- **Protocol**: gRPC with gRPC-Web (Envoy-mediated)

## Project Structure
```
distributed-fms/
├── src/                          # Java backend source code
│   ├── main/
│   │   ├── java/               # Backend implementation
│   │   ├── proto/              # Protocol Buffer definitions
│   │   └── resources/
│   └── test/                   # Integration tests
├── ui/                          # React frontend
│   └── fms-ui/
│       ├── src/
│       │   ├── App.js          # Main React component
│       │   ├── App.css         # Styling
│       │   ├── AlarmClient.js  # gRPC-Web client
│       │   └── generated/      # Auto-generated gRPC stubs
│       └── package.json
├── Dockerfile.*                 # Docker images for each service
├── docker-compose.yml           # Docker Compose configuration
├── envoy.yaml                   # Envoy proxy configuration
└── start-fms.sh                 # Automation script
```

## Development

### Building the Backend
```bash
mvn clean install
```

### Building the Frontend
```bash
cd ui/fms-ui
npm install
npm run build
```

### Running Tests
```bash
mvn test
```

## Troubleshooting

### Alarms not appearing in UI?
1. Check that SNMP traps are being received: `docker logs trap-receiver`
2. Verify Kafka is running: `docker logs kafka`
3. Check FMS Server logs: `docker logs fms-server`
4. Open browser console (F12) to see gRPC connection errors

### Port already in use?
If ports are already in use, modify `docker-compose.yml` or stop the conflicting service:
```bash
docker compose down
```

## Contributing
Feel free to submit issues, fork the repository, and create pull requests for any improvements.

## License
This project is open source and available under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
