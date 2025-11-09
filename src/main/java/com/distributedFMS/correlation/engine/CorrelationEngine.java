package com.distributedFMS.correlation.engine;

import com.distributedFMS.core.model.Alarm;
import com.distributedFMS.correlation.model.*;
import com.distributedFMS.correlation.rules.*;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.SqlQuery;
import javax.cache.Cache;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Main correlation engine that orchestrates alarm correlation
 */
public class CorrelationEngine {
    
    private final Ignite ignite;
    private final IgniteCache<String, Alarm> alarmsCache;
    private final IgniteCache<String, CorrelatedAlarm> correlatedAlarmsCache;
    private final RuleEngine ruleEngine;
    private final RootCauseAnalyzer rootCauseAnalyzer;
    private final CorrelationWindowManager windowManager;
    private final long timeWindowMs;
    private final int maxGroupSize;
    
    public CorrelationEngine(Ignite ignite, String alarmsCacheName, String correlatedAlarmsCacheName) {
        this(ignite, alarmsCacheName, correlatedAlarmsCacheName, 60000, 100);
    }
    
    public CorrelationEngine(Ignite ignite, String alarmsCacheName, String correlatedAlarmsCacheName,
                            long timeWindowMs, int maxGroupSize) {
        this.ignite = ignite;
        this.alarmsCache = ignite.cache(alarmsCacheName);
        this.correlatedAlarmsCache = ignite.cache(correlatedAlarmsCacheName);
        this.timeWindowMs = timeWindowMs;
        this.maxGroupSize = maxGroupSize;
        
        this.ruleEngine = new RuleEngine();
        TopologyBasedRule topologyRule = new TopologyBasedRule();
        this.ruleEngine.registerRule(topologyRule);
        this.ruleEngine.registerRule(new AttributeBasedRule());
        this.ruleEngine.registerRule(new TimeBasedRule(timeWindowMs, 50));
        this.rootCauseAnalyzer = new RootCauseAnalyzer(topologyRule);
        this.windowManager = new CorrelationWindowManager(300000, 600000);
        
        if (this.alarmsCache == null || this.correlatedAlarmsCache == null) {
            throw new IllegalStateException("Required caches not found");
        }
    }
    
    public CompletableFuture<CorrelationResult> correlateAsync(Alarm alarm) {
        return CompletableFuture.supplyAsync(() -> correlate(alarm));
    }
    
    public CorrelationResult correlate(Alarm alarm) {
        try {
            List<CorrelatedAlarm> activeCorrelations = findActiveCorrelationsForRegion(alarm.getGeographicRegion());
            
            for (CorrelatedAlarm existingCorrelation : activeCorrelations) {
                if (belongsToCorrelation(alarm, existingCorrelation)) {
                    updateExistingCorrelation(existingCorrelation, alarm);
                    return new CorrelationResult(true, existingCorrelation, "Added to existing correlation");
                }
            }
            
            List<Alarm> candidateAlarms = findCandidateAlarms(alarm);
            if (!candidateAlarms.isEmpty()) {
                CorrelatedAlarm newCorrelation = createNewCorrelation(alarm, candidateAlarms);
                return new CorrelationResult(true, newCorrelation, "Created new correlation");
            }
            
            return new CorrelationResult(false, null, "No correlation found");
        } catch (Exception e) {
            System.err.printf("[CORRELATION] Error: %s%n", e.getMessage());
            return new CorrelationResult(false, null, "Error: " + e.getMessage());
        }
    }
    
    private List<CorrelatedAlarm> findActiveCorrelationsForRegion(String region) {
        SqlQuery<String, CorrelatedAlarm> query = new SqlQuery<>(CorrelatedAlarm.class,
            "geographicRegion = ? AND status = ?");
        List<CorrelatedAlarm> correlations = new ArrayList<>();
        try (var cursor = correlatedAlarmsCache.query(query.setArgs(region, CorrelationStatus.ACTIVE))) {
            for (Cache.Entry<String, CorrelatedAlarm> entry : cursor) {
                CorrelatedAlarm correlation = entry.getValue();
                if (!correlation.isStale(timeWindowMs)) {
                    correlations.add(correlation);
                }
            }
        }
        return correlations;
    }
    
    private boolean belongsToCorrelation(Alarm alarm, CorrelatedAlarm correlation) {
        if (correlation.getChildCount() >= maxGroupSize || correlation.getChildAlarmIds().isEmpty()) {
            return false;
        }
        String childAlarmId = correlation.getChildAlarmIds().get(0);
        Alarm childAlarm = alarmsCache.get(childAlarmId);
        return childAlarm != null && ruleEngine.evaluate(alarm, childAlarm).isPresent();
    }
    
    private void updateExistingCorrelation(CorrelatedAlarm correlation, Alarm newAlarm) {
        correlation.addChildAlarm(newAlarm.getAlarmId());
        newAlarm.setCorrelationId(correlation.getCorrelationId());
        alarmsCache.put(newAlarm.getAlarmId(), newAlarm);
        
        List<Alarm> allAlarms = getAlarmsForCorrelation(correlation);
        RootCauseScore rootCause = rootCauseAnalyzer.analyzeRootCause(allAlarms);
        if (rootCause != null) {
            correlation.setRootCauseAlarmId(rootCause.getAlarmId());
            correlation.setConfidenceScore(rootCause.getScore());
            correlation.setDescription(String.format("Correlation of %d alarms. Root cause: %s",
                correlation.getChildCount(), rootCause.getReasoning()));
            
            // Update rootCauseAlarmId on ALL alarms in correlation (root cause may have changed)
            for (Alarm alarm : allAlarms) {
                alarm.setRootCauseAlarmId(rootCause.getAlarmId());
                alarmsCache.put(alarm.getAlarmId(), alarm);
            }
        }
        
        correlatedAlarmsCache.put(correlation.getCorrelationId(), correlation);
        windowManager.addToWindow(correlation.getGeographicRegion(), correlation);
        
        System.out.printf("[CORRELATION] Updated %s: added %s (total: %d)%n",
            correlation.getCorrelationId(), newAlarm.getAlarmId(), correlation.getChildCount());
    }
    
    private List<Alarm> findCandidateAlarms(Alarm targetAlarm) {
        long windowStart = targetAlarm.getTimestamp() - timeWindowMs;
        long windowEnd = targetAlarm.getTimestamp();
        SqlQuery<String, Alarm> query = new SqlQuery<>(Alarm.class,
            "geographicRegion = ? AND timestamp >= ? AND timestamp <= ? AND alarmId != ?");
        
        List<Alarm> candidates = new ArrayList<>();
        try (var cursor = alarmsCache.query(query.setArgs(
                targetAlarm.getGeographicRegion(), windowStart, windowEnd, targetAlarm.getAlarmId()))) {
            for (Cache.Entry<String, Alarm> entry : cursor) {
                Alarm candidate = entry.getValue();
                if (candidate.getCorrelationId() == null && 
                    ruleEngine.evaluate(targetAlarm, candidate).isPresent()) {
                    candidates.add(candidate);
                }
            }
        }
        return candidates;
    }
    
    private CorrelatedAlarm createNewCorrelation(Alarm triggerAlarm, List<Alarm> relatedAlarms) {
        CorrelationType correlationType = relatedAlarms.isEmpty() ? CorrelationType.TIME :
            ruleEngine.getCorrelationType(triggerAlarm, relatedAlarms.get(0));
        
        CorrelatedAlarm correlation = new CorrelatedAlarm(correlationType, triggerAlarm.getGeographicRegion());
        correlation.addChildAlarm(triggerAlarm.getAlarmId());
        relatedAlarms.forEach(alarm -> correlation.addChildAlarm(alarm.getAlarmId()));
        
        List<Alarm> allAlarms = new ArrayList<>();
        allAlarms.add(triggerAlarm);
        allAlarms.addAll(relatedAlarms);
        
        RootCauseScore rootCause = rootCauseAnalyzer.analyzeRootCause(allAlarms);
        if (rootCause != null) {
            correlation.setRootCauseAlarmId(rootCause.getAlarmId());
            correlation.setConfidenceScore(rootCause.getScore());
            correlation.setDescription(String.format("Correlation of %d alarms. Root cause: %s",
                correlation.getChildCount(), rootCause.getReasoning()));
        }
        
        triggerAlarm.setCorrelationId(correlation.getCorrelationId());
        triggerAlarm.setRootCauseAlarmId(correlation.getRootCauseAlarmId());
        alarmsCache.put(triggerAlarm.getAlarmId(), triggerAlarm);
        relatedAlarms.forEach(alarm -> {
            alarm.setCorrelationId(correlation.getCorrelationId());
            alarm.setRootCauseAlarmId(correlation.getRootCauseAlarmId());
            alarmsCache.put(alarm.getAlarmId(), alarm);
        });
        
        correlatedAlarmsCache.put(correlation.getCorrelationId(), correlation);
        windowManager.addToWindow(correlation.getGeographicRegion(), correlation);
        
        System.out.printf("[CORRELATION] Created %s: type=%s, alarms=%d, root=%s, conf=%.2f%n",
            correlation.getCorrelationId(), correlation.getCorrelationType(), 
            correlation.getChildCount(), correlation.getRootCauseAlarmId(), correlation.getConfidenceScore());
        
        return correlation;
    }
    
    private List<Alarm> getAlarmsForCorrelation(CorrelatedAlarm correlation) {
        return correlation.getChildAlarmIds().stream()
            .map(alarmsCache::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    public void registerRule(CorrelationRule rule) {
        ruleEngine.registerRule(rule);
    }
    
    public RuleEngine getRuleEngine() {
        return ruleEngine;
    }
    
    public CorrelationWindowManager getWindowManager() {
        return windowManager;
    }
    
    public int cleanupStaleCorrelations() {
        int cleanedCount = 0;
        SqlQuery<String, CorrelatedAlarm> query = new SqlQuery<>(CorrelatedAlarm.class, "status = ?");
        try (var cursor = correlatedAlarmsCache.query(query.setArgs(CorrelationStatus.ACTIVE))) {
            for (Cache.Entry<String, CorrelatedAlarm> entry : cursor) {
                CorrelatedAlarm correlation = entry.getValue();
                if (correlation.isStale(timeWindowMs * 2)) {
                    correlation.setStatus(CorrelationStatus.STALE);
                    correlatedAlarmsCache.put(correlation.getCorrelationId(), correlation);
                    cleanedCount++;
                }
            }
        }
        cleanedCount += windowManager.evictStaleWindows();
        if (cleanedCount > 0) {
            System.out.printf("[CORRELATION] Cleaned up %d stale correlations%n", cleanedCount);
        }
        return cleanedCount;
    }
}
