package com.distributedFMS.correlation.rules;

import com.distributedFMS.core.model.Alarm;
import com.distributedFMS.correlation.model.CorrelationType;
import java.util.*;

/**
 * Correlates alarms based on network topology
 */
public class TopologyBasedRule implements CorrelationRule {
    
    private final Map<String, List<String>> deviceTopology;
    private final long maxTimeDiffMs;
    private final int priority;
    
    public TopologyBasedRule() {
        this(new HashMap<>(), 30000, 100);
    }
    
    public TopologyBasedRule(Map<String, List<String>> deviceTopology, 
                             long maxTimeDiffMs, 
                             int priority) {
        this.deviceTopology = deviceTopology;
        this.maxTimeDiffMs = maxTimeDiffMs;
        this.priority = priority;
    }
    
    @Override
    public boolean canCorrelate(Alarm alarm1, Alarm alarm2) {
        long timeDiff = Math.abs(alarm1.getTimestamp() - alarm2.getTimestamp());
        if (timeDiff > maxTimeDiffMs) {
            return false;
        }
        
        return isUpstreamOf(alarm1.getDeviceId(), alarm2.getDeviceId()) ||
               isUpstreamOf(alarm2.getDeviceId(), alarm1.getDeviceId());
    }
    
    @Override
    public double getConfidenceScore(Alarm alarm1, Alarm alarm2) {
        if (!canCorrelate(alarm1, alarm2)) {
            return 0.0;
        }
        
        double baseScore = 0.9;
        long timeDiff = Math.abs(alarm1.getTimestamp() - alarm2.getTimestamp());
        double timeDecay = 1.0 - ((double) timeDiff / maxTimeDiffMs) * 0.2;
        
        return baseScore * timeDecay;
    }
    
    @Override
    public CorrelationType getType() {
        return CorrelationType.TOPOLOGY;
    }
    
    @Override
    public int getPriority() {
        return priority;
    }
    
    @Override
    public String getName() {
        return "TopologyBasedRule";
    }
    
    public boolean isUpstreamOf(String device1, String device2) {
        if (device1 == null || device2 == null) {
            return false;
        }
        
        List<String> downstreamDevices = deviceTopology.get(device1);
        if (downstreamDevices == null) {
            return false;
        }
        
        if (downstreamDevices.contains(device2)) {
            return true;
        }
        
        for (String downstream : downstreamDevices) {
            if (isUpstreamOf(downstream, device2)) {
                return true;
            }
        }
        
        return false;
    }
    
    public void loadTopology(Map<String, List<String>> topology) {
        this.deviceTopology.clear();
        this.deviceTopology.putAll(topology);
    }
    
    public String getMostUpstreamDevice(String device1, String device2) {
        if (isUpstreamOf(device1, device2)) {
            return device1;
        } else if (isUpstreamOf(device2, device1)) {
            return device2;
        }
        return null;
    }
}
