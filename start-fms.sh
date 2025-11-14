#!/bin/bash
set -e
echo "=========================================="
echo "FMS - Distributed Fault Management System"
echo "=========================================="
echo ""
# Change to script directory
cd "$(dirname "$0")"
echo "[1/6] Stopping any running containers..."
docker compose down 2>/dev/null || true
sleep 2
echo "[2/6] Building all containers..."
export APP_VERSION=0.1.0-SNAPSHOT
docker compose build
sleep 2
echo "[3/6] Starting all services..."
docker compose up -d
echo "Waiting for services to start..."
sleep 15
echo "[4/6] Verifying services are running..."
docker compose ps
sleep 3
echo "[5/6] Generating SNMP traps..."
# Test alarms (send within 10 seconds!)
snmptrap -v 2c -c public 127.0.0.1:10162 '' 1.3.6.1.6.3.1.1.5.3 \
  1.3.6.1.4.1.8072.2.3.0.1 s "TEST-1 CPU"
sleep 2
snmptrap -v 2c -c public 127.0.0.1:10162 '' 1.3.6.1.6.3.1.1.5.4 \
  1.3.6.1.4.1.8072.2.3.0.1 s "TEST-1 Memory"
sleep 2
snmptrap -v 2c -c public 127.0.0.1:10162 '' 1.3.6.1.6.3.1.1.5.5 \
  1.3.6.1.4.1.8072.2.3.0.1 s "TEST-1 Disk"
echo ""
echo "Checking correlation logs..."
sleep 2
docker logs fms-server 2>&1 | grep "CORRELATION" | tail -20
echo "[6/6] Opening FMS UI in browser..."
sleep 3
# Detect OS and open browser accordingly
if command -v xdg-open &> /dev/null; then
  xdg-open http://localhost:3000
elif command -v open &> /dev/null; then
  open http://localhost:3000
else
  echo "Please manually open http://localhost:3000 in your browser"
fi
echo ""
echo "=========================================="
echo "✓ FMS is running!"
echo "=========================================="
echo ""
echo "Services:"
echo "  - React UI:        http://localhost:3000"
echo "  - Envoy Admin:     http://localhost:9901"
echo "  - gRPC Server:     localhost:50051"
echo ""
echo "To stop all services, run: docker compose down"
echo ""
