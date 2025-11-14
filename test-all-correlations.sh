#!/bin/bash

echo "=========================================="
echo "FMS Correlation Comprehensive Test Suite"
echo "=========================================="
echo ""

# Test 1: Basic correlation (3 related alarms)
echo "TEST 1: Basic Correlation (CPU, Memory, Disk)"
echo "-------------------------------------------"
snmptrap -v 2c -c public 127.0.0.1:10162 '' 1.3.6.1.6.3.1.1.5.3 1.3.6.1.4.1.8072.2.3.0.1 s "[TEST1] CPU High"
sleep 2
snmptrap -v 2c -c public 127.0.0.1:10162 '' 1.3.6.1.6.3.1.1.5.4 1.3.6.1.4.1.8072.2.3.0.1 s "[TEST1] Memory High"
sleep 2
snmptrap -v 2c -c public 127.0.0.1:10162 '' 1.3.6.1.6.3.1.1.5.5 1.3.6.1.4.1.8072.2.3.0.1 s "[TEST1] Disk Full"
sleep 3
echo "✓ Sent 3 alarms (should be grouped)"
echo ""

# Test 2: Deduplication (send same alarm twice)
echo "TEST 2: Deduplication"
echo "-------------------------------------------"
snmptrap -v 2c -c public 127.0.0.1:10162 '' 1.3.6.1.6.3.1.1.5.3 1.3.6.1.4.1.8072.2.3.0.1 s "[TEST2] Duplicate Test"
sleep 2
snmptrap -v 2c -c public 127.0.0.1:10162 '' 1.3.6.1.6.3.1.1.5.3 1.3.6.1.4.1.8072.2.3.0.1 s "[TEST2] Duplicate Test"
sleep 2
snmptrap -v 2c -c public 127.0.0.1:10162 '' 1.3.6.1.6.3.1.1.5.3 1.3.6.1.4.1.8072.2.3.0.1 s "[TEST2] Duplicate Test"
sleep 3
echo "✓ Sent same alarm 3 times (tally should be 3)"
echo ""

# Test 3: Root cause identification
echo "TEST 3: Root Cause Identification"
echo "-------------------------------------------"
snmptrap -v 2c -c public 127.0.0.1:10162 '' 1.3.6.1.6.3.1.1.5.3 1.3.6.1.4.1.8072.2.3.0.1 s "[TEST3] Root Cause"
sleep 2
snmptrap -v 2c -c public 127.0.0.1:10162 '' 1.3.6.1.6.3.1.1.5.4 1.3.6.1.4.1.8072.2.3.0.1 s "[TEST3] Consequence 1"
sleep 2
snmptrap -v 2c -c public 127.0.0.1:10162 '' 1.3.6.1.6.3.1.1.5.5 1.3.6.1.4.1.8072.2.3.0.1 s "[TEST3] Consequence 2"
sleep 3
echo "✓ Sent 3 alarms (first should be root cause)"
echo ""

echo "=========================================="
echo "Test Complete!"
echo "=========================================="
echo ""
echo "Check UI at http://localhost:3000"
echo ""
echo "Recent correlations:"
docker logs fms-server 2>&1 | grep "\[CORRELATION\] Created" | tail -3
