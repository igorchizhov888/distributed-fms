package com.distributedFMS.correlation.model;

import com.distributedFMS.core.model.Alarm;
import java.util.*;

/**
 * Context object for a correlation session
 */
public class CorrelationContext {
    private Alarm currentAlarm;
    private List<Alarm> candidateAlarms;
    private List<CorrelatedAlarm> activeCorrelations;
    private Map<String, Object> ruleData;
    private long windowStartTime;
    private long windowEndTime;
    
    public CorrelationContext(Alarm currentAlarm) {
        this.currentAlarm = currentAlarm;
        this.candidateAlarms = new ArrayList<>();
        this.activeCorrelations = new ArrayList<>();
        this.ruleData = new HashMap<>();
        this.windowEndTime = currentAlarm.getTimestamp();
        this.windowStartTime = this.windowEndTime - 60000;
    }
    
    public void addCandidateAlarm(Alarm alarm) { candidateAlarms.add(alarm); }
    public void addActiveCorrelation(CorrelatedAlarm correlation) { activeCorrelations.add(correlation); }
    public void putRuleData(String key, Object value) { ruleData.put(key, value); }
    public Object getRuleData(String key) { return ruleData.get(key); }
    
    public Alarm getCurrentAlarm() { return currentAlarm; }
    public void setCurrentAlarm(Alarm currentAlarm) { this.currentAlarm = currentAlarm; }
    public List<Alarm> getCandidateAlarms() { return candidateAlarms; }
    public void setCandidateAlarms(List<Alarm> candidateAlarms) { this.candidateAlarms = candidateAlarms; }
    public List<CorrelatedAlarm> getActiveCorrelations() { return activeCorrelations; }
    public void setActiveCorrelations(List<CorrelatedAlarm> activeCorrelations) { this.activeCorrelations = activeCorrelations; }
    public Map<String, Object> getRuleData() { return ruleData; }
    public void setRuleData(Map<String, Object> ruleData) { this.ruleData = ruleData; }
    public long getWindowStartTime() { return windowStartTime; }
    public void setWindowStartTime(long windowStartTime) { this.windowStartTime = windowStartTime; }
    public long getWindowEndTime() { return windowEndTime; }
    public void setWindowEndTime(long windowEndTime) { this.windowEndTime = windowEndTime; }
}
