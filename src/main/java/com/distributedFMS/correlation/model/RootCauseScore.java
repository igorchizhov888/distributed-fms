package com.distributedFMS.correlation.model;

import java.io.Serializable;

/**
 * Represents the confidence score for a root cause determination
 */
public class RootCauseScore implements Serializable {
    private String alarmId;
    private double score;
    private String reasoning;
    
    public RootCauseScore(String alarmId, double score, String reasoning) {
        this.alarmId = alarmId;
        this.score = score;
        this.reasoning = reasoning;
    }
    
    public String getAlarmId() { return alarmId; }
    public void setAlarmId(String alarmId) { this.alarmId = alarmId; }
    
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    
    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }
    
    @Override
    public String toString() {
        return String.format("RootCauseScore{alarmId='%s', score=%.2f, reasoning='%s'}", 
            alarmId, score, reasoning);
    }
}
