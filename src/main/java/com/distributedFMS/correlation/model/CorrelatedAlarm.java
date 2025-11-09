package com.distributedFMS.correlation.model;

import org.apache.ignite.cache.query.annotations.QuerySqlField;
import org.apache.ignite.cache.affinity.AffinityKeyMapped;
import java.io.Serializable;
import java.time.Instant;
import java.util.*;

/**
 * Represents a meta-alarm that groups multiple related alarms together
 */
public class CorrelatedAlarm implements Serializable {
    
    @QuerySqlField(index = true)
    private String correlationId;
    
    @QuerySqlField(index = true)
    private CorrelationType correlationType;
    
    @QuerySqlField
    private List<String> childAlarmIds;
    
    @QuerySqlField(index = true)
    private String rootCauseAlarmId;
    
    @QuerySqlField
    private double confidenceScore;
    
    @QuerySqlField(index = true)
    private long firstCorrelatedAt;
    
    @QuerySqlField(index = true)
    private long lastCorrelatedAt;
    
    @QuerySqlField(index = true)
    private CorrelationStatus status;
    
    @QuerySqlField(index = true)
    @AffinityKeyMapped
    private String geographicRegion;
    
    @QuerySqlField
    private Map<String, Object> metadata;
    
    @QuerySqlField
    private int childCount;
    
    @QuerySqlField
    private String description;
    
    public CorrelatedAlarm() {
        this.correlationId = UUID.randomUUID().toString();
        this.firstCorrelatedAt = Instant.now().toEpochMilli();
        this.lastCorrelatedAt = this.firstCorrelatedAt;
        this.status = CorrelationStatus.ACTIVE;
        this.childAlarmIds = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.childCount = 0;
    }
    
    public CorrelatedAlarm(CorrelationType correlationType, String geographicRegion) {
        this();
        this.correlationType = correlationType;
        this.geographicRegion = geographicRegion;
    }
    
    public void addChildAlarm(String alarmId) {
        if (!childAlarmIds.contains(alarmId)) {
            childAlarmIds.add(alarmId);
            childCount = childAlarmIds.size();
            lastCorrelatedAt = Instant.now().toEpochMilli();
        }
    }
    
    public void removeChildAlarm(String alarmId) {
        if (childAlarmIds.remove(alarmId)) {
            childCount = childAlarmIds.size();
            lastCorrelatedAt = Instant.now().toEpochMilli();
        }
    }
    
    public boolean isStale(long staleThresholdMs) {
        long now = Instant.now().toEpochMilli();
        return (now - lastCorrelatedAt) > staleThresholdMs;
    }
    
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    
    public CorrelationType getCorrelationType() { return correlationType; }
    public void setCorrelationType(CorrelationType correlationType) { this.correlationType = correlationType; }
    
    public List<String> getChildAlarmIds() { return childAlarmIds; }
    public void setChildAlarmIds(List<String> childAlarmIds) { 
        this.childAlarmIds = childAlarmIds;
        this.childCount = childAlarmIds.size();
    }
    
    public String getRootCauseAlarmId() { return rootCauseAlarmId; }
    public void setRootCauseAlarmId(String rootCauseAlarmId) { this.rootCauseAlarmId = rootCauseAlarmId; }
    
    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }
    
    public long getFirstCorrelatedAt() { return firstCorrelatedAt; }
    public void setFirstCorrelatedAt(long firstCorrelatedAt) { this.firstCorrelatedAt = firstCorrelatedAt; }
    
    public long getLastCorrelatedAt() { return lastCorrelatedAt; }
    public void setLastCorrelatedAt(long lastCorrelatedAt) { this.lastCorrelatedAt = lastCorrelatedAt; }
    
    public CorrelationStatus getStatus() { return status; }
    public void setStatus(CorrelationStatus status) { this.status = status; }
    
    public String getGeographicRegion() { return geographicRegion; }
    public void setGeographicRegion(String geographicRegion) { this.geographicRegion = geographicRegion; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public int getChildCount() { return childCount; }
    public void setChildCount(int childCount) { this.childCount = childCount; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CorrelatedAlarm that = (CorrelatedAlarm) o;
        return Objects.equals(correlationId, that.correlationId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(correlationId);
    }
    
    @Override
    public String toString() {
        return String.format("CorrelatedAlarm{id='%s', type=%s, children=%d, rootCause='%s', " +
            "confidence=%.2f, status=%s, region='%s'}",
            correlationId, correlationType, childCount, rootCauseAlarmId, 
            confidenceScore, status, geographicRegion);
    }
}
