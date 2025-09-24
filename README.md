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
