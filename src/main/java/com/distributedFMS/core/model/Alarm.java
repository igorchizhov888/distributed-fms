package com.distributedFMS.core.model;

import org.apache.ignite.cache.query.annotations.QuerySqlField;
import org.apache.ignite.cache.affinity.AffinityKeyMapped;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Alarm implements Serializable {
    
    @QuerySqlField(index = true)
    private String alarmId;
    
    @QuerySqlField(index = true)
    private long timestamp;
    
    @QuerySqlField(index = true)
    private String deviceId;
    
    @QuerySqlField(index = true)
    private String severity;
    
    @QuerySqlField
    private String eventType;
    
    @QuerySqlField
    private String description;
    
    @QuerySqlField(index = true)
    @AffinityKeyMapped
    private String geographicRegion;
    
    @QuerySqlField(index = true)
    private AlarmStatus status;
    
    @QuerySqlField
    private String correlationId;

    @QuerySqlField
    private String rootCauseAlarmId;

    @QuerySqlField
    private int tallyCount;

    @QuerySqlField
    private int priority;

    @QuerySqlField
    private long firstOccurrence;

    @QuerySqlField
    private long lastOccurrence;
    public Alarm() {
        this.alarmId = UUID.randomUUID().toString();
        this.timestamp = Instant.now().toEpochMilli();
        this.status = AlarmStatus.ACTIVE;
        this.tallyCount = 1;  // ADD THIS LINE if not already there
    }
    
        
    public Alarm(String deviceId, String severity, String eventType, 
                 String description, String geographicRegion) {
        this();
        this.deviceId = deviceId;
        this.severity = severity;
        this.eventType = eventType;
        this.description = description;
        this.geographicRegion = geographicRegion;
    }
    
    public String getAlarmId() { return alarmId; }
    public void setAlarmId(String alarmId) { this.alarmId = alarmId; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getGeographicRegion() { return geographicRegion; }
    public void setGeographicRegion(String geographicRegion) { this.geographicRegion = geographicRegion; }
    
    public AlarmStatus getStatus() { return status; }
    public void setStatus(AlarmStatus status) { this.status = status; }
    
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getRootCauseAlarmId() { return rootCauseAlarmId; }
    public void setRootCauseAlarmId(String rootCauseAlarmId) { this.rootCauseAlarmId = rootCauseAlarmId; }

    public int getTallyCount() { return tallyCount; }
    public void setTallyCount(int tallyCount) { this.tallyCount = tallyCount; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public long getFirstOccurrence() { return firstOccurrence; }
    public void setFirstOccurrence(long firstOccurrence) { this.firstOccurrence = firstOccurrence; }

    public long getLastOccurrence() { return lastOccurrence; }
    public void setLastOccurrence(long lastOccurrence) { this.lastOccurrence = lastOccurrence; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Alarm alarm = (Alarm) o;
        return Objects.equals(alarmId, alarm.alarmId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(alarmId);
    }
    
    @Override
    public String toString() {
        return String.format("Alarm{id='%s', deviceId='%s', severity=%s, type='%s', region='%s', status=%s}", 
                           alarmId, deviceId, severity, eventType, geographicRegion, status);
    }
}

