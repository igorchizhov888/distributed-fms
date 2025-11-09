package com.distributedFMS.correlation.rules;

import com.distributedFMS.core.model.Alarm;
import com.distributedFMS.correlation.model.CorrelationType;

/**
 * Interface for all correlation rules
 */
public interface CorrelationRule {
    
    boolean canCorrelate(Alarm alarm1, Alarm alarm2);
    
    double getConfidenceScore(Alarm alarm1, Alarm alarm2);
    
    CorrelationType getType();
    
    int getPriority();
    
    String getName();
}
