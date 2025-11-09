package com.distributedFMS.correlation.rules;

import com.distributedFMS.core.model.Alarm;
import com.distributedFMS.correlation.model.CorrelationType;

/**
 * Correlates alarms that occur within a specified time window
 */
public class TimeBasedRule implements CorrelationRule {
    
    private final long timeWindowMs;
    private final int priority;
    
    public TimeBasedRule() {
        this(60000, 50);
    }
    
    public TimeBasedRule(long timeWindowMs, int priority) {
        this.timeWindowMs = timeWindowMs;
        this.priority = priority;
    }
    
    @Override
    public boolean canCorrelate(Alarm alarm1, Alarm alarm2) {
        long timeDiff = Math.abs(alarm1.getTimestamp() - alarm2.getTimestamp());
        return timeDiff <= timeWindowMs;
    }
    
    @Override
    public double getConfidenceScore(Alarm alarm1, Alarm alarm2) {
        long timeDiff = Math.abs(alarm1.getTimestamp() - alarm2.getTimestamp());
        double score = 1.0 - (0.5 * ((double) timeDiff / timeWindowMs));
        return Math.max(0.5, Math.min(1.0, score));
    }
    
    @Override
    public CorrelationType getType() {
        return CorrelationType.TIME;
    }
    
    @Override
    public int getPriority() {
        return priority;
    }
    
    @Override
    public String getName() {
        return "TimeBasedRule";
    }
    
    public long getTimeWindowMs() {
        return timeWindowMs;
    }
}
