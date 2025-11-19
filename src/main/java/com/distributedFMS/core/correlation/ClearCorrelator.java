package com.distributedFMS.core.correlation;

import com.distributedFMS.core.model.Alarm;
import com.distributedFMS.core.model.AlarmStatus;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.SqlQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.Cache;
import java.util.ArrayList;
import java.util.List;

public class ClearCorrelator {
    private static final Logger logger = LoggerFactory.getLogger(ClearCorrelator.class);
    
    private final Ignite ignite;
    private final IgniteCache<String, Alarm> alarmsCache;
    private final boolean removeOnClear;
    
    public ClearCorrelator(Ignite ignite, boolean removeOnClear) {
        logger.info("=== Initializing ClearCorrelator ===");
        logger.info("Remove on clear: {}", removeOnClear);
        this.ignite = ignite;
        this.alarmsCache = ignite.getOrCreateCache("alarms");
        this.removeOnClear = removeOnClear;
    }
    
    public boolean processClearEvent(Alarm clearAlarm) {
        logger.info(">>> CLEAR CHECK: Processing alarm {}", clearAlarm.getAlarmId());
        
        if (!isClearEvent(clearAlarm)) {
            logger.debug("Not a clear event: {}", clearAlarm.getDescription());
            return false;
        }
        
        logger.info(">>> CLEAR DETECTED: {} from device {}", 
            clearAlarm.getDescription(), clearAlarm.getDeviceId());
        
        List<Alarm> matchingProblems = findMatchingProblemAlarms(clearAlarm);
        
        if (matchingProblems.isEmpty()) {
            logger.warn(">>> CLEAR: No matching problem alarm found for clear event: {}", 
                clearAlarm.getDescription());
            return true;
        }
        
        logger.info(">>> CLEAR: Found {} matching problem alarm(s)", matchingProblems.size());
        
        int clearedCount = 0;
        for (Alarm problemAlarm : matchingProblems) {
            if (clearProblemAlarm(problemAlarm, clearAlarm)) {
                clearedCount++;
            }
        }
        
        logger.info(">>> CLEAR COMPLETE: Cleared {} alarm(s)", clearedCount);
        return true;
    }
    
    private boolean isClearEvent(Alarm alarm) {
        if (ClearCorrelationConfig.isClearEventType(alarm.getEventType())) {
            logger.debug("Clear event detected by eventType: {}", alarm.getEventType());
            return true;
        }
        
        if (ClearCorrelationConfig.isClearDescription(alarm.getDescription())) {
            logger.debug("Clear event detected by description: {}", alarm.getDescription());
            return true;
        }
        
        String severity = alarm.getSeverity().toUpperCase();
        if (severity.contains("CLEAR") || severity.equals("NORMAL") || 
            severity.equals("OK") || severity.equals("INFO")) {
            logger.debug("Clear event detected by severity: {}", alarm.getSeverity());
            return true;
        }
        
        return false;
    }
    
    private List<Alarm> findMatchingProblemAlarms(Alarm clearAlarm) {
        List<Alarm> matches = new ArrayList<>();
        String deviceId = clearAlarm.getDeviceId();
        
        String problemEventType = ClearCorrelationConfig.getProblemEventType(clearAlarm.getEventType());
        if (problemEventType != null) {
            matches.addAll(findByDeviceAndEventType(deviceId, problemEventType));
        }
        
        if (matches.isEmpty()) {
            String problemPattern = ClearCorrelationConfig.getProblemDescriptionPattern(
                clearAlarm.getDescription());
            if (problemPattern != null) {
                matches.addAll(findByDeviceAndDescriptionPattern(deviceId, problemPattern));
            }
        }
        
        return matches;
    }
    
    private List<Alarm> findByDeviceAndEventType(String deviceId, String eventType) {
        List<Alarm> results = new ArrayList<>();
        String sql = "SELECT * FROM Alarm WHERE deviceId = ? AND eventType = ? AND status != ?";
        SqlQuery<String, Alarm> query = new SqlQuery<>(Alarm.class, sql);
        query.setArgs(deviceId, eventType, AlarmStatus.CLEARED.name());
        
        try (var cursor = alarmsCache.query(query)) {
            for (Cache.Entry<String, Alarm> entry : cursor) {
                results.add(entry.getValue());
            }
        }
        
        return results;
    }
    
    private List<Alarm> findByDeviceAndDescriptionPattern(String deviceId, String pattern) {
        List<Alarm> results = new ArrayList<>();
        String sql = "SELECT * FROM Alarm WHERE deviceId = ? AND description LIKE ? AND status != ?";
        SqlQuery<String, Alarm> query = new SqlQuery<>(Alarm.class, sql);
        query.setArgs(deviceId, "%" + pattern + "%", AlarmStatus.CLEARED.name());
        
        try (var cursor = alarmsCache.query(query)) {
            for (Cache.Entry<String, Alarm> entry : cursor) {
                results.add(entry.getValue());
            }
        }
        
        return results;
    }
    
    private boolean clearProblemAlarm(Alarm problemAlarm, Alarm clearAlarm) {
        try {
            logger.info(">>> CLEARING ALARM: {} - {}", 
                problemAlarm.getAlarmId(), problemAlarm.getDescription());
            
            if (removeOnClear) {
                alarmsCache.remove(problemAlarm.getAlarmId());
                logger.info(">>> CLEARED: Removed alarm {} from active cache", 
                    problemAlarm.getAlarmId());
            } else {
                problemAlarm.setStatus(AlarmStatus.CLEARED);
                problemAlarm.setSeverity("CLEARED");
                problemAlarm.setLastOccurrence(clearAlarm.getTimestamp());
                
                if (problemAlarm.getCorrelationId() == null) {
                    problemAlarm.setCorrelationId(clearAlarm.getAlarmId());
                }
                
                alarmsCache.put(problemAlarm.getAlarmId(), problemAlarm);
                logger.info(">>> CLEARED: Updated alarm {} to CLEARED status", 
                    problemAlarm.getAlarmId());
            }
            
            return true;
        } catch (Exception e) {
            logger.error(">>> CLEAR ERROR: Failed to clear alarm {}", 
                problemAlarm.getAlarmId(), e);
            return false;
        }
    }
}
