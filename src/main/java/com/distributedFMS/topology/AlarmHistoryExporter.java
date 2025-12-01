package com.distributedFMS.topology;

import com.distributedFMS.core.model.Alarm;
import com.distributedFMS.core.config.FMSIgniteConfig;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.SqlQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.Cache;
import java.util.ArrayList;
import java.util.List;

/**
 * Exports alarm history from Ignite cache for topology learning
 */
public class AlarmHistoryExporter {
    private static final Logger logger = LoggerFactory.getLogger(AlarmHistoryExporter.class);
    
    private final Ignite ignite;
    private final IgniteCache<String, Alarm> alarmsCache;
    
    public AlarmHistoryExporter(Ignite ignite) {
        this.ignite = ignite;
        this.alarmsCache = ignite.getOrCreateCache(FMSIgniteConfig.getAlarmsCacheName());
        logger.info("AlarmHistoryExporter initialized");
    }
    
    /**
     * Get all alarms from the last N hours
     */
    public List<Alarm> getRecentAlarms(int hours) {
        List<Alarm> alarms = new ArrayList<>();
        
        long cutoffTime = System.currentTimeMillis() - (hours * 60 * 60 * 1000L);
        
        String sql = "SELECT * FROM Alarm WHERE timestamp >= ? ORDER BY timestamp ASC";
        SqlQuery<String, Alarm> query = new SqlQuery<>(Alarm.class, sql);
        query.setArgs(cutoffTime);
        
        try (var cursor = alarmsCache.query(query)) {
            for (Cache.Entry<String, Alarm> entry : cursor) {
                alarms.add(entry.getValue());
            }
        }
        
        logger.info("Fetched {} alarms from last {} hours", alarms.size(), hours);
        return alarms;
    }
    
    /**
     * Get all unique device IDs from alarms
     */
    public List<String> getAllDeviceIds() {
        List<String> devices = new ArrayList<>();
        
        String sql = "SELECT DISTINCT deviceId FROM Alarm";
        SqlQuery<String, Alarm> query = new SqlQuery<>(Alarm.class, sql);
        
        try (var cursor = alarmsCache.query(query)) {
            for (Cache.Entry<String, Alarm> entry : cursor) {
                String deviceId = entry.getValue().getDeviceId();
                if (!devices.contains(deviceId)) {
                    devices.add(deviceId);
                }
            }
        }
        
        logger.info("Found {} unique devices", devices.size());
        return devices;
    }
    
    /**
     * Convert Alarm to JSON format for Python
     */
    public String alarmToJson(Alarm alarm) {
        return String.format(
            "{\"device_id\":\"%s\",\"timestamp\":%d,\"severity\":\"%s\",\"event_type\":\"%s\",\"description\":\"%s\"}",
            alarm.getDeviceId(),
            alarm.getTimestamp(),
            alarm.getSeverity(),
            alarm.getEventType(),
            alarm.getDescription()
        );
    }
}
