#!/bin/bash

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=========================================="
echo "FMS Alarm Correlation Integration Tests"
echo "=========================================="
echo ""

# Function to send alarm
send_alarm() {
    local event_type=$1
    local description=$2
    snmptrap -v 2c -c public 127.0.0.1:10162 '' "$event_type" \
        1.3.6.1.4.1.8072.2.3.0.1 s "$description" > /dev/null 2>&1
}

# Function to check correlation in logs
check_correlation() {
    local test_name=$1
    local expected_alarms=$2
    
    sleep 3
    
    local correlation_log=$(docker logs fms-server 2>&1 | grep "\[$test_name\]" | grep "CORRELATION" | tail -1)
    
    if echo "$correlation_log" | grep -q "alarms=$expected_alarms"; then
        echo -e "${GREEN}✓ PASS${NC}: $test_name - Correlation created with $expected_alarms alarms"
        return 0
    else
        echo -e "${RED}✗ FAIL${NC}: $test_name - Expected $expected_alarms alarms in correlation"
        echo "  Log: $correlation_log"
        return 1
    fi
}

# Function to verify IDs in UI (via gRPC query simulation)
verify_ids() {
    local test_name=$1
    echo -e "${YELLOW}→${NC} Verifying IDs for $test_name..."
    
    # Check if alarms have correlation IDs in cache
    local missing_ids=$(docker logs fms-server 2>&1 | grep "$test_name" | grep -c "correlationId=null")
    
    if [ "$missing_ids" -eq 0 ]; then
        echo -e "${GREEN}✓ PASS${NC}: All alarms have Correlation IDs"
        return 0
    else
        echo -e "${RED}✗ FAIL${NC}: $missing_ids alarms missing Correlation IDs"
        return 1
    fi
}

# Clean up old test data
echo "Cleaning up old test data..."
docker compose down > /dev/null 2>&1
docker compose up -d > /dev/null 2>&1
sleep 10
echo -e "${GREEN}✓${NC} System ready"
echo ""

PASS_COUNT=0
FAIL_COUNT=0

# ==========================================
# TEST 1: Basic 3-Alarm Correlation
# ==========================================
echo "TEST 1: Basic 3-alarm correlation (CPU → Memory → Disk)"
echo "----------------------------------------------"

send_alarm "1.3.6.1.6.3.1.1.5.3" "[TEST1] CPU High Load"
sleep 2
send_alarm "1.3.6.1.6.3.1.1.5.4" "[TEST1] Memory High Usage"
sleep 2
send_alarm "1.3.6.1.6.3.1.1.5.5" "[TEST1] Disk Full"

if check_correlation "TEST1" 3; then
    ((PASS_COUNT++))
else
    ((FAIL_COUNT++))
fi
echo ""

# ==========================================
# TEST 2: Deduplication with Correlation
# ==========================================
echo "TEST 2: Deduplication - Same alarms sent twice"
echo "----------------------------------------------"

send_alarm "1.3.6.1.6.3.1.1.5.3" "[TEST2] CPU Alert"
sleep 1
send_alarm "1.3.6.1.6.3.1.1.5.4" "[TEST2] Memory Alert"
sleep 1
send_alarm "1.3.6.1.6.3.1.1.5.3" "[TEST2] CPU Alert"  # Duplicate
sleep 2

# Check if tally increased
local tally=$(docker logs fms-server 2>&1 | grep "TEST2.*CPU" | grep -o "Tally incremented from [0-9]* to [0-9]*" | tail -1)
if echo "$tally" | grep -q "to 2"; then
    echo -e "${GREEN}✓ PASS${NC}: TEST2 - Deduplication working (tally=2)"
    ((PASS_COUNT++))
else
    echo -e "${RED}✗ FAIL${NC}: TEST2 - Deduplication failed"
    ((FAIL_COUNT++))
fi
echo ""

# ==========================================
# TEST 3: Time Window Test (60 seconds)
# ==========================================
echo "TEST 3: Time window - Alarms within 10 seconds"
echo "----------------------------------------------"

send_alarm "1.3.6.1.6.3.1.1.5.3" "[TEST3] Router A Down"
sleep 2
send_alarm "1.3.6.1.6.3.1.1.5.4" "[TEST3] Router B Down"
sleep 2
send_alarm "1.3.6.1.6.3.1.1.5.5" "[TEST3] Router C Down"

if check_correlation "TEST3" 3; then
    ((PASS_COUNT++))
else
    ((FAIL_COUNT++))
fi
echo ""

# ==========================================
# TEST 4: Root Cause Identification
# ==========================================
echo "TEST 4: Root cause identification (First alarm = root)"
echo "----------------------------------------------"

send_alarm "1.3.6.1.6.3.1.1.5.3" "[TEST4] Core Switch Failure"
sleep 2
send_alarm "1.3.6.1.6.3.1.1.5.4" "[TEST4] Access Switch 1 Down"
sleep 2
send_alarm "1.3.6.1.6.3.1.1.5.5" "[TEST4] Access Switch 2 Down"

sleep 3
local root_cause=$(docker logs fms-server 2>&1 | grep "\[TEST4\]" | grep "CORRELATION.*Created" | grep -o "root=[a-f0-9-]*")
if [ ! -z "$root_cause" ]; then
    echo -e "${GREEN}✓ PASS${NC}: TEST4 - Root cause identified: $root_cause"
    ((PASS_COUNT++))
else
    echo -e "${RED}✗ FAIL${NC}: TEST4 - Root cause not identified"
    ((FAIL_COUNT++))
fi
echo ""

# ==========================================
# TEST 5: Different Regions (No Correlation)
# ==========================================
echo "TEST 5: Different regions should NOT correlate"
echo "----------------------------------------------"
echo -e "${YELLOW}NOTE:${NC} This test requires region-aware alarm creation (not yet implemented)"
echo -e "${YELLOW}→${NC} SKIPPED"
echo ""

# ==========================================
# SUMMARY
# ==========================================
echo "=========================================="
echo "TEST SUMMARY"
echo "=========================================="
echo -e "Tests Passed: ${GREEN}$PASS_COUNT${NC}"
echo -e "Tests Failed: ${RED}$FAIL_COUNT${NC}"
echo ""

if [ $FAIL_COUNT -eq 0 ]; then
    echo -e "${GREEN}✓ ALL TESTS PASSED!${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Open http://localhost:3000 to verify UI display"
    echo "2. Check that all alarms have Correlation IDs and Root Cause IDs"
    echo "3. Test expand/collapse functionality"
    exit 0
else
    echo -e "${RED}✗ SOME TESTS FAILED${NC}"
    echo ""
    echo "Check docker logs for details:"
    echo "  docker logs fms-server 2>&1 | grep CORRELATION | tail -20"
    exit 1
fi
