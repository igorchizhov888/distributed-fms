package com.distributedFMS.correlation.service;

import com.distributedFMS.core.model.Alarm;
import com.distributedFMS.correlation.model.CorrelatedAlarm;
import com.distributedFMS.correlation.model.CorrelationStatus;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.SqlQuery;
import javax.cache.Cache;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for querying correlated alarms
 */
public class CorrelationService {
    
    private final IgniteCache<String, Alarm> alarmsCache;
    private final IgniteCache<String, CorrelatedAlarm> correlatedAlarmsCache;
    
    public CorrelationService(Ignite ignite, String alarmsCacheName, String correlatedAlarmsCacheName) {
        this.alarmsCache = ignite.cache(alarmsCacheName);
        this.correlatedAlarmsCache = ignite.cache(correlatedAlarmsCacheName);
    }
    
    public List<CorrelatedAlarm> getActiveCorrelations() {
        SqlQuery<String, CorrelatedAlarm> query = new SqlQuery<>(CorrelatedAlarm.class, "status = ?");
        List<CorrelatedAlarm> correlations = new ArrayList<>();
        try (var cursor = correlatedAlarmsCache.query(query.setArgs(CorrelationStatus.ACTIVE))) {
            for (Cache.Entry<String, CorrelatedAlarm> entry : cursor) {
                correlations.add(entry.getValue());
            }
        }
        return correlations;
    }
    
    public List<CorrelatedAlarm> getCorrelationsForRegion(String region) {
        SqlQuery<String, CorrelatedAlarm> query = new SqlQuery<>(CorrelatedAlarm.class,
            "geographicRegion = ? AND status = ?");
        List<CorrelatedAlarm> correlations = new ArrayList<>();
        try (var cursor = correlatedAlarmsCache.query(query.setArgs(region, CorrelationStatus.ACTIVE))) {
            for (Cache.Entry<String, CorrelatedAlarm> entry : cursor) {
                correlations.add(entry.getValue());
            }
        }
        return correlations;
    }
    
    public CorrelatedAlarmDetail getCorrelationDetail(String correlationId) {
        CorrelatedAlarm correlation = correlatedAlarmsCache.get(correlationId);
        if (correlation == null) {
            return null;
        }
        
        List<Alarm> childAlarms = correlation.getChildAlarmIds().stream()
            .map(alarmsCache::get)
            .filter(alarm -> alarm != null)
            .collect(Collectors.toList());
        
        return new CorrelatedAlarmDetail(correlation, childAlarms);
    }
    
    public Alarm getRootCauseAlarm(String correlationId) {
        CorrelatedAlarm correlation = correlatedAlarmsCache.get(correlationId);
        if (correlation == null || correlation.getRootCauseAlarmId() == null) {
            return null;
        }
        return alarmsCache.get(correlation.getRootCauseAlarmId());
    }
    
    public static class CorrelatedAlarmDetail {
        private final CorrelatedAlarm correlation;
        private final List<Alarm> childAlarms;
        
        public CorrelatedAlarmDetail(CorrelatedAlarm correlation, List<Alarm> childAlarms) {
            this.correlation = correlation;
            this.childAlarms = childAlarms;
        }
        
        public CorrelatedAlarm getCorrelation() { return correlation; }
        public List<Alarm> getChildAlarms() { return childAlarms; }
    }
}
