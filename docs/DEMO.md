# Distributed FMS - Demo Guide

This branch contains demo data and screenshots for the GNN topology learning system.

## Quick Demo

1. Start all services:
```bash
docker compose up -d
cd topology-service && source topology_env/bin/activate && python topology_grpc_server.py &
cd ui/fms-ui && npm start
```

2. Generate alarms:
```bash
docker compose restart gnmi-simulator
```

3. Open browser: http://localhost:3000
   - View alarms in "Alarms" tab
   - Click "Network Topology" → "Train Model"
   - Adjust confidence slider to see different edge densities

## Screenshots

See `docs/screenshots/` for:
- Alarm table with parent/child grouping
- Topology with 870 edges (low confidence)
- Topology with 0 edges (high confidence)
- Training controls and statistics

## Sample Data

- `topology-service/synthetic_alarms.json` - Generated alarm data
- `topology-service/learned_topology.json` - Pre-trained topology
