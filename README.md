# Distributed Fault Management System

A geo-distributed, high-performance fault management system for telecommunications networks, built on Apache Ignite.

## Problem Statement

Traditional centralized fault management systems cannot handle modern network event volumes and geographic distribution requirements. This project implements distributed, edge-based fault management using in-memory data grids.

## Key Features

- **Geographic Distribution**: Process events at network edge locations using custom affinity functions
- **Real-time Processing**: Handle thousands of events per second per node with sub-millisecond response times
- **Active-Active Clustering**: Automatic failover with zero data loss across distributed nodes
- **SQL Querying**: Complex alarm correlation across distributed datasets
- **Universal Network Support**: Monitor any network type by adding appropriate software adapters

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+

### Build the Project
```bash
mvn package
```

### Run the Application

To start a server node, use the `start-server.sh` script:
```bash
./start-server.sh <node-name>
```
For example:
```bash
./start-server.sh node-1
```

To start a client node, use the `start-client.sh` script:
```bash
./start-client.sh <node-name>
```
For example:
```bash
./start-client.sh client-1
```
The scripts handle the complex JVM arguments required to run the application.

## High Availability and Failover

The Distributed FMS is designed for high availability. You can test the failover mechanism by following these steps:

1.  **Start two server nodes:**
    Open two separate terminals and run the following commands, one in each terminal:
    ```bash
    # In terminal 1
    ./start-server.sh node-1
    ```
    ```bash
    # In terminal 2
    ./start-server.sh node-2
    ```

2.  **Start a client:**
    Open a third terminal and start the client:
    ```bash
    ./start-client.sh client-1
    ```

3.  **Verify the cluster:**
    In the logs of any of the three nodes, you should see a topology snapshot indicating a cluster of 2 servers and 1 client:
    `Topology snapshot [..., servers=2, clients=1, ...]`

4.  **Simulate a failover:**
    Stop one of the server nodes by closing its terminal window or pressing `Ctrl+C`.

5.  **Verify the failover:**
    Check the logs of the remaining server and the client. You should see a new topology snapshot indicating that one server has left, but the client is still connected:
    `Topology snapshot [..., servers=1, clients=1, ...]`
    You can also verify that the 'Total Alarms' count in the client's log remains the same, which proves that no data was lost.

This demonstrates the system's ability to handle node failures without interrupting the client.
