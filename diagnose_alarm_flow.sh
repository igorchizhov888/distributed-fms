#!/bin/bash

# FMS gNMI Alarm Flow Diagnostic Script
# This script helps identify where the alarm processing pipeline breaks

echo "============================================"
echo "FMS Alarm Flow Diagnostics"
echo "============================================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to check container status
check_container() {
    local container_name=$1
    if docker ps --format '{{.Names}}' | grep -q "^${container_name}$"; then
        echo -e "${GREEN}✓${NC} Container ${container_name} is running"
        return 0
    else
        echo -e "${RED}✗${NC} Container ${container_name} is NOT running"
        return 1
    fi
}

# Function to check logs for specific patterns
check_logs() {
    local container=$1
    local pattern=$2
    local description=$3
    
    echo -n "  Checking: ${description}... "
    if docker logs ${container} 2>&1 | grep -q "${pattern}"; then
        echo -e "${GREEN}FOUND${NC}"
        return 0
    else
        echo -e "${RED}NOT FOUND${NC}"
        return 1
    fi
}

# Function to count occurrences in logs
count_in_logs() {
    local container=$1
    local pattern=$2
    local count=$(docker logs ${container} 2>&1 | grep -c "${pattern}")
    echo $count
}

echo "1. Container Status Check"
echo "-------------------------"
check_container "fms-core"
check_container "gnmi-simulator"
check_container "kafka"
check_container "zookeeper"
echo ""

echo "2. Kafka Message Flow"
echo "---------------------"
FMS_CONTAINER="fms-core"

# Check if EventConsumer is initialized
check_logs $FMS_CONTAINER "Initializing EventConsumer" "EventConsumer initialization"

# Check if subscribed to Kafka topic
check_logs $FMS_CONTAINER "Subscribed to topic: fms-events" "Kafka topic subscription"

# Count events received
event_count=$(count_in_logs $FMS_CONTAINER "Received event JSON")
echo "  Events received by EventConsumer: ${event_count}"

# Count events parsed
parsed_count=$(count_in_logs $FMS_CONTAINER "Successfully parsed alarm")
echo "  Events successfully parsed: ${parsed_count}"

# Count events sent to deduplication
dedup_count=$(count_in_logs $FMS_CONTAINER "Sending to DeduplicationCorrelator")
echo "  Events sent to deduplication: ${dedup_count}"

# Count deduplication results
dedup_result_count=$(count_in_logs $FMS_CONTAINER "Deduplication completed")
echo "  Deduplication completed: ${dedup_result_count}"

# Count cache store attempts
cache_store_count=$(count_in_logs $FMS_CONTAINER "CACHE STORE START")
echo "  Cache store attempts: ${cache_store_count}"

# Count successful cache stores
cache_success_count=$(count_in_logs $FMS_CONTAINER "CACHE STORE SUCCESS")
echo "  Cache store successes: ${cache_success_count}"

# Count cache verification
cache_verify_count=$(count_in_logs $FMS_CONTAINER "CACHE VERIFY: Successfully retrieved")
echo "  Cache verifications: ${cache_verify_count}"

echo ""

echo "3. Deduplication Analysis"
echo "-------------------------"
first_occurrence=$(count_in_logs $FMS_CONTAINER "First occurrence of alarm")
echo "  First occurrences (new alarms): ${first_occurrence}"

duplicate_count=$(count_in_logs $FMS_CONTAINER "Duplicate alarm .* detected")
echo "  Duplicates detected: ${duplicate_count}"

echo ""

echo "4. Error Detection"
echo "------------------"
json_errors=$(count_in_logs $FMS_CONTAINER "JSON parsing error")
echo "  JSON parsing errors: ${json_errors}"

cache_errors=$(count_in_logs $FMS_CONTAINER "CACHE STORE ERROR")
echo "  Cache storage errors: ${cache_errors}"

cache_null=$(count_in_logs $FMS_CONTAINER "alarmsCache is NULL")
echo "  Cache NULL errors: ${cache_null}"

general_errors=$(count_in_logs $FMS_CONTAINER "ERROR")
echo "  General ERROR messages: ${general_errors}"

fatal_errors=$(count_in_logs $FMS_CONTAINER "FATAL")
echo "  FATAL error messages: ${fatal_errors}"

echo ""

echo "5. Pipeline Flow Summary"
echo "------------------------"
echo "  Kafka → EventConsumer:      ${event_count} events"
echo "  EventConsumer → Parse:      ${parsed_count} parsed"
echo "  Parse → Deduplication:      ${dedup_count} sent"
echo "  Deduplication → Result:     ${dedup_result_count} processed"
echo "  Result → Cache Store:       ${cache_store_count} attempts"
echo "  Cache Store → Success:      ${cache_success_count} stored"
echo "  Cache Success → Verified:   ${cache_verify_count} verified"

echo ""
echo "6. Problem Detection"
echo "--------------------"

# Calculate losses at each stage
parse_loss=$((event_count - parsed_count))
dedup_loss=$((parsed_count - dedup_count))
result_loss=$((dedup_count - dedup_result_count))
store_loss=$((dedup_result_count - cache_store_count))
success_loss=$((cache_store_count - cache_success_count))

if [ $parse_loss -gt 0 ]; then
    echo -e "${RED}⚠${NC} Lost ${parse_loss} events during JSON parsing"
fi

if [ $dedup_loss -gt 0 ]; then
    echo -e "${RED}⚠${NC} Lost ${dedup_loss} events before deduplication"
fi

if [ $result_loss -gt 0 ]; then
    echo -e "${RED}⚠${NC} Lost ${result_loss} events during deduplication"
fi

if [ $store_loss -gt 0 ]; then
    echo -e "${RED}⚠${NC} Lost ${store_loss} events before cache storage"
fi

if [ $success_loss -gt 0 ]; then
    echo -e "${RED}⚠${NC} Lost ${success_loss} events during cache storage"
fi

if [ $cache_success_count -eq 0 ] && [ $event_count -gt 0 ]; then
    echo -e "${RED}✗ CRITICAL: No alarms are being stored in cache!${NC}"
fi

if [ $cache_success_count -gt 0 ] && [ $cache_success_count -eq $cache_verify_count ]; then
    echo -e "${GREEN}✓ Cache storage working correctly${NC}"
fi

echo ""
echo "7. Recent Error Messages"
echo "------------------------"
echo "Last 10 ERROR messages:"
docker logs $FMS_CONTAINER 2>&1 | grep "ERROR" | tail -10

echo ""
echo "8. Recent Event Processing"
echo "--------------------------"
echo "Last 5 event processing sequences:"
docker logs $FMS_CONTAINER 2>&1 | grep "Processing event #" | tail -5

echo ""
echo "9. Cache Status"
echo "---------------"
docker logs $FMS_CONTAINER 2>&1 | grep "CACHE STATUS" | tail -5

echo ""
echo "============================================"
echo "Diagnostic Complete"
echo "============================================"

# Provide recommendations
echo ""
echo "Recommendations:"
echo "----------------"

if [ $event_count -eq 0 ]; then
    echo -e "${YELLOW}→${NC} No events received. Check if gNMI simulator is sending events."
    echo "  Run: docker logs gnmi-simulator | grep 'Publishing alarm'"
fi

if [ $parse_loss -gt 0 ]; then
    echo -e "${YELLOW}→${NC} JSON parsing issues detected. Check alarm format from simulator."
fi

if [ $cache_success_count -eq 0 ] && [ $dedup_result_count -gt 0 ]; then
    echo -e "${YELLOW}→${NC} Deduplication works but cache storage fails."
    echo "  Check Ignite cache initialization and permissions."
fi

if [ $general_errors -gt 0 ]; then
    echo -e "${YELLOW}→${NC} Errors detected. Check full logs with:"
    echo "  docker logs fms-core 2>&1 | grep -A 5 ERROR"
fi
