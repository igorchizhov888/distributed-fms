package com.distributedFMS.core.correlation;

import java.util.HashMap;
import java.util.Map;

public class ClearCorrelationConfig {
    
    private static final Map<String, String> EVENT_TYPE_PAIRS = new HashMap<>();
    
    static {
        // SNMP Trap OID pairs: problem -> clear
        EVENT_TYPE_PAIRS.put("1.3.6.1.6.3.1.1.5.3", "1.3.6.1.6.3.1.1.5.4"); // linkDown -> linkUp
    }
    
    public static boolean isClearEventType(String eventType) {
        return EVENT_TYPE_PAIRS.containsValue(eventType);
    }
    
    public static String getProblemEventType(String clearEventType) {
        for (Map.Entry<String, String> entry : EVENT_TYPE_PAIRS.entrySet()) {
            if (entry.getValue().equals(clearEventType)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    // DISABLED: Only use event type matching for now
    public static boolean isClearDescription(String description) {
        return false;
    }
    
    public static String getProblemDescriptionPattern(String clearDescription) {
        return null;
    }
}
