#!/bin/bash

echo "FMS Correlation Test - SNMP Traps"
echo "=================================="
echo ""
echo "Sending 3 related alarms to trigger correlation..."
echo ""

# Alarm 1: CPU High
echo "1. Sending CPU_HIGH alarm..."
snmptrap -v 2c -c public 127.0.0.1:10162 '' 1.3.6.1.6.3.1.1.5.3 1.3.6.1.4.1.8072.2.3.0.1 s "[TEST_CORR] CPU High"
sleep 2

# Alarm 2: Memory High
echo "2. Sending MEMORY_HIGH alarm..."
snmptrap -v 2c -c public 127.0.0.1:10162 '' 1.3.6.1.6.3.1.1.5.4 1.3.6.1.4.1.8072.2.3.0.1 s "[TEST_CORR] Memory High"
sleep 2

# Alarm 3: Disk Full
echo "3. Sending DISK_FULL alarm..."
snmptrap -v 2c -c public 127.0.0.1:10162 '' 1.3.6.1.6.3.1.1.5.5 1.3.6.1.4.1.8072.2.3.0.1 s "[TEST_CORR] Disk Full"

echo ""
echo "Waiting for correlation to be created..."
sleep 3

echo ""
echo "Recent correlation logs:"
docker logs fms-server 2>&1 | grep "\[CORRELATION\]" | tail -3

echo ""
echo "✓ Check UI at http://localhost:3000"
echo "  You should see 3 alarms grouped together with a parent alarm"
