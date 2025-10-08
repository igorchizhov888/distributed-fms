package com.distributedFMS.core.priority;

import com.distributedFMS.core.model.AlarmPriority;

public class AlarmPrioritizationEngine {

    public static AlarmPriority getPriority(String eventType) {
        if (eventType == null) {
            return AlarmPriority.MINOR;
        }

        switch (eventType) {
            case "1.3.6.1.6.3.1.1.5.3":
                return AlarmPriority.CRITICAL;
            default:
                return AlarmPriority.MINOR;
        }
    }
}
