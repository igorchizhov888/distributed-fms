# Distributed FMS - Demo Guide

This branch contains demo data and screenshots for the GNN topology learning system.

## Screenshots

### Alarm Table with Parent/Child Grouping
![Alarm Table](screenshots/alarm-table.png)

Shows the real-time alarm monitoring interface with:
- Parent alarm highlighted (blue background)
- Expandable child alarms (4 children under 1 parent)
- 15+ alarm fields displayed
- Tally count, correlation ID, root cause ID
- Professional dark theme UI

### Network Topology Visualization
![Network Topology](screenshots/network-topology.png)

Interactive force-directed graph visualization featuring:
- 30 nodes color-coded by device type
- 870 edges at 0.30 confidence threshold
- Real-time confidence slider
- Node labels with device names
- Training controls and statistics
- Zoom, pan, and click interactions

## Quick Demo

### 1. Start All Services
```bash
# Start Docker services
cd ~/Gemini_CLI/distributed-fms
docker compose up -d
sleep 30

# Start Python topology service (Terminal 1)
cd topology-service
source topology_env/bin/activate
python topology_grpc_server.py

# Start React UI (Terminal 2)
cd ui/fms-ui
docker compose stop fms-ui
npm start
```

### 2. Generate Alarms
```bash
docker compose restart gnmi-simulator
```

### 3. View System

Open http://localhost:3000

- **Alarms Tab**: View real-time table, expand parent alarms
- **Network Topology Tab**: Click "Train Model", adjust confidence slider

## Device Type Colors
- 🔴 Red: Core Routers
- 🔵 Blue: Switches  
- 🟢 Green: Routers
- 🟠 Orange: Hosts
- ⚫ Gray: Unknown

## Stopping the System
```bash
# Ctrl+C in Python/React terminals
docker compose down
```

For complete documentation, see the `main` branch.
