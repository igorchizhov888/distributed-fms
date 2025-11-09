package com.distributedFMS.correlation.engine;

import com.distributedFMS.correlation.model.CorrelatedAlarm;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages time-based windows for efficient correlation lookups
 */
public class CorrelationWindowManager {
    
    private final Map<String, NavigableMap<Long, Set<String>>> regionWindows;
    private final long windowSizeMs;
    private final long staleThresholdMs;
    
    public CorrelationWindowManager() {
        this(300000, 600000);
    }
    
    public CorrelationWindowManager(long windowSizeMs, long staleThresholdMs) {
        this.regionWindows = new ConcurrentHashMap<>();
        this.windowSizeMs = windowSizeMs;
        this.staleThresholdMs = staleThresholdMs;
    }
    
    public void addToWindow(String region, CorrelatedAlarm correlation) {
        if (region == null || correlation == null) {
            return;
        }
        
        regionWindows.computeIfAbsent(region, k -> new TreeMap<>())
            .computeIfAbsent(correlation.getLastCorrelatedAt(), k -> new HashSet<>())
            .add(correlation.getCorrelationId());
    }
    
    public Set<String> getActiveCorrelations(String region, long startTime, long endTime) {
        Set<String> activeCorrelations = new HashSet<>();
        
        NavigableMap<Long, Set<String>> window = regionWindows.get(region);
        if (window == null) {
            return activeCorrelations;
        }
        
        NavigableMap<Long, Set<String>> subMap = window.subMap(startTime, true, endTime, true);
        for (Set<String> correlations : subMap.values()) {
            activeCorrelations.addAll(correlations);
        }
        
        return activeCorrelations;
    }
    
    public Set<String> getActiveCorrelations(String region) {
        long now = Instant.now().toEpochMilli();
        long windowStart = now - windowSizeMs;
        return getActiveCorrelations(region, windowStart, now);
    }
    
    public void removeFromWindow(String region, String correlationId, long timestamp) {
        NavigableMap<Long, Set<String>> window = regionWindows.get(region);
        if (window != null) {
            Set<String> correlations = window.get(timestamp);
            if (correlations != null) {
                correlations.remove(correlationId);
                if (correlations.isEmpty()) {
                    window.remove(timestamp);
                }
            }
        }
    }
    
    public int evictStaleWindows() {
        long cutoffTime = Instant.now().toEpochMilli() - staleThresholdMs;
        int evictedCount = 0;
        
        for (Map.Entry<String, NavigableMap<Long, Set<String>>> entry : regionWindows.entrySet()) {
            NavigableMap<Long, Set<String>> window = entry.getValue();
            NavigableMap<Long, Set<String>> staleEntries = window.headMap(cutoffTime, true);
            
            for (Set<String> correlations : staleEntries.values()) {
                evictedCount += correlations.size();
            }
            
            staleEntries.clear();
        }
        
        return evictedCount;
    }
    
    public int getTotalActiveCorrelations() {
        return regionWindows.values().stream()
            .flatMap(window -> window.values().stream())
            .mapToInt(Set::size)
            .sum();
    }
    
    public Map<String, Integer> getWindowStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        
        for (Map.Entry<String, NavigableMap<Long, Set<String>>> entry : regionWindows.entrySet()) {
            int count = entry.getValue().values().stream()
                .mapToInt(Set::size)
                .sum();
            stats.put(entry.getKey(), count);
        }
        
        return stats;
    }
    
    public void clearAll() {
        regionWindows.clear();
    }
}
