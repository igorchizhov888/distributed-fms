package com.distributedFMS.core.processor;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.SqlQuery;
import com.distributedFMS.core.model.Alarm;
import com.distributedFMS.core.model.AlarmSeverity;
import com.distributedFMS.core.model.AlarmStatus;
import com.distributedFMS.core.config.FMSIgniteConfig;

import javax.cache.Cache;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FMSEventProcessor {
    
    private final Ignite ignite;
    private final IgniteCache<String, Alarm> alarmCache;
    
    public FMSEventProcessor(Ignite ignite) {
        this.ignite = ignite;
        this.alarmCache = ignite.cache(FMSIgniteConfig.getAlarmsCacheName());
        
        if (this.alarmCache == null) {
            throw new IllegalStateException("Alarms cache not found. Make sure server nodes are running.");
        }
    }
    
    public CompletableFuture<String> processAlarm(Alarm alarm) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                alarmCache.put(alarm.getAlarmId(), alarm);
                
                System.out.printf("[%s] Processed alarm: %s%n", 
                    ignite.cluster().localNode().consistentId(), alarm);
                
                return alarm.getAlarmId();
                
            } catch (Exception e) {
                System.err.printf("Failed to process alarm: %s%n", e.getMessage());
                throw new RuntimeException("Alarm processing failed", e);
            }
        });
    }
    
    public List<Alarm> getActiveAlarmsForRegion(String region) {
        SqlQuery<String, Alarm> query = new SqlQuery<>(Alarm.class,
            "geographicRegion = ? AND status = ?");
        
        List<Alarm> alarms = new ArrayList<>();
        try (var cursor = alarmCache.query(query.setArgs(region, AlarmStatus.ACTIVE))) {
            for (Cache.Entry<String, Alarm> entry : cursor) {
                alarms.add(entry.getValue());
            }
        }
        
        return alarms;
    }
    
    public List<Alarm> getHighSeverityAlarms() {
        SqlQuery<String, Alarm> query = new SqlQuery<>(Alarm.class,
            "severity = ? AND status = ?");
        
        List<Alarm> alarms = new ArrayList<>();
        try (var cursor = alarmCache.query(query.setArgs(AlarmSeverity.HIGH, AlarmStatus.ACTIVE))) {
            for (Cache.Entry<String, Alarm> entry : cursor) {
                alarms.add(entry.getValue());
            }
        }
        
        return alarms;
    }
    
    public void printClusterInfo() {
        System.out.println("\n=== Cluster Information ===");
        System.out.printf("Cluster Size: %d nodes%n", ignite.cluster().nodes().size());
        System.out.printf("Local Node: %s%n", ignite.cluster().localNode().consistentId());
        System.out.printf("Total Alarms: %d%n", alarmCache.size());
        
        System.out.println("\nAlarms by Region:");
        String[] regions = {"us-east", "us-west", "eu-central", "asia-pacific"};
        for (String region : regions) {
            List<Alarm> regionAlarms = getActiveAlarmsForRegion(region);
            System.out.printf("  %s: %d alarms%n", region, regionAlarms.size());
        }
    }
}
