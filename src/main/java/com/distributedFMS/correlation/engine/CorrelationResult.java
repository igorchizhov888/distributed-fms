package com.distributedFMS.correlation.engine;

import com.distributedFMS.correlation.model.CorrelatedAlarm;

/**
 * Result of a correlation operation
 */
public class CorrelationResult {
    private final boolean correlated;
    private final CorrelatedAlarm correlatedAlarm;
    private final String message;
    
    public CorrelationResult(boolean correlated, CorrelatedAlarm correlatedAlarm, String message) {
        this.correlated = correlated;
        this.correlatedAlarm = correlatedAlarm;
        this.message = message;
    }
    
    public boolean isCorrelated() {
        return correlated;
    }
    
    public CorrelatedAlarm getCorrelatedAlarm() {
        return correlatedAlarm;
    }
    
    public String getMessage() {
        return message;
    }
    
    @Override
    public String toString() {
        return String.format("CorrelationResult{correlated=%s, message='%s'}", 
            correlated, message);
    }
}
