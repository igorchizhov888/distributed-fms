# Distributed Fault Management System

A geo-distributed, high-performance fault management system for telecommunications networks, built on Apache Ignite and Apache Kafka.

## Problem Statement

Traditional centralized fault management systems cannot handle modern network event volumes and geographic distribution requirements. This project implements distributed, edge-based fault management using in-memory data grids and a message bus for event ingestion.

## Key Features

- **Event-Driven Architecture**: Ingests events via an Apache Kafka message bus.
- **Geographic Distribution**: Process events at network edge locations using custom affinity functions in Apache Ignite.
- **Real-time Processing**: Handle thousands of events per second per node with sub-millisecond response times.
- **Distributed Caching**: Events are consumed and stored in a distributed Apache Ignite cache.
- **Active-Active Clustering**: Automatic failover with zero data loss across distributed nodes (feature of Ignite).
- **Universal Network Support**: Monitor any network type by adding appropriate software adapters.

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+
- Docker and Docker Compose
- `snmptrap` command-line tool

### 1. Build the Project

First, build the application using Maven. This will compile the code and create a single executable JAR file with all dependencies.

```bash
mvn clean install
```

### 2. Start the Kafka Environment

The system depends on Apache Kafka and Zookeeper. A Docker Compose file is provided to easily start these services.

```bash
docker compose up -d
```
This will start the containers in the background.

### 3. Start the FMS Server and Trap Receiver

Run the FMS server and the SNMP trap receiver in the background using the provided scripts:

```bash
./run-app.sh server &
./start-trap-receiver.sh &
```

The server will start, connect to the Kafka bus, and begin listening for events. Log output is directed to `server.log` and `trap.log`.

### 4. Send Test Events

In a separate terminal, use the `snmptrap` command to send test events to the `SnmpTrapReceiver`.

```bash
snmptrap -v 2c -c public localhost:10162 '' .1.3.6.1.6.3.1.1.5.3
```

### 5. Verify Operation

Check the logs of the FMS Core Server (`server.log`). You should see messages indicating that events were consumed from Kafka and processed by the deduplication logic:

```
INFO: DeduplicationCorrelator: Processing alarm with key: ...
INFO: DeduplicationCorrelator: No existing alarm found, creating new alarm with key: ...
```

Send the same trap again and you should see the `tallyCount` increment:

```
INFO: DeduplicationCorrelator: Updated existing alarm: ... Tally incremented from 1 to 2
```

This confirms the end-to-end data pipeline and deduplication logic are working correctly.