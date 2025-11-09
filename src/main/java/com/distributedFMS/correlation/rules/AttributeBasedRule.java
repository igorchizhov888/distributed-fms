package com.distributedFMS.correlation.rules;

import com.distributedFMS.core.model.Alarm;
import com.distributedFMS.correlation.model.CorrelationType;

/**
 * Correlates alarms based on shared attributes
 */
public class AttributeBasedRule implements CorrelationRule {
    
    private final int priority;
    
    public AttributeBasedRule() {
        this(60);
    }
    
    public AttributeBasedRule(int priority) {
        this.priority = priority;
    }
    
    @Override
    public boolean canCorrelate(Alarm alarm1, Alarm alarm2) {
        if (hasSameDevice(alarm1, alarm2)) {
            return true;
        }
        if (hasSameRegionAndSeverity(alarm1, alarm2)) {
            return true;
        }
        return false;
    }
    
    @Override
    public double getConfidenceScore(Alarm alarm1, Alarm alarm2) {
        double score = 0.5;
        
        if (hasSameDevice(alarm1, alarm2)) {
            score = 0.95;
            if (hasSameEventType(alarm1, alarm2)) {
                score = 1.0;
            }
        }
        else if (hasSameRegionAndSeverity(alarm1, alarm2)) {
            score = 0.7;
        }
        
        return score;
    }
    
    @Override
    public CorrelationType getType() {
        return CorrelationType.ATTRIBUTE;
    }
    
    @Override
    public int getPriority() {
        return priority;
    }
    
    @Override
    public String getName() {
        return "AttributeBasedRule";
    }
    
    private boolean hasSameDevice(Alarm alarm1, Alarm alarm2) {
        return alarm1.getDeviceId() != null && 
               alarm1.getDeviceId().equals(alarm2.getDeviceId());
    }
    
    private boolean hasSameEventType(Alarm alarm1, Alarm alarm2) {
        return alarm1.getEventType() != null && 
               alarm1.getEventType().equals(alarm2.getEventType());
    }
    
    private boolean hasSameRegionAndSeverity(Alarm alarm1, Alarm alarm2) {
        return alarm1.getGeographicRegion() != null &&
               alarm1.getGeographicRegion().equals(alarm2.getGeographicRegion()) &&
               alarm1.getSeverity() != null &&
               alarm1.getSeverity().equals(alarm2.getSeverity());
    }
}
