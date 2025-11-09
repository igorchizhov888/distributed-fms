package com.distributedFMS.correlation.engine;

import com.distributedFMS.core.model.Alarm;
import com.distributedFMS.correlation.model.RootCauseScore;
import com.distributedFMS.correlation.rules.TopologyBasedRule;
import java.util.*;

/**
 * Analyzes a group of correlated alarms to determine the root cause
 */
public class RootCauseAnalyzer {
    
    private final TopologyBasedRule topologyRule;
    
    public RootCauseAnalyzer(TopologyBasedRule topologyRule) {
        this.topologyRule = topologyRule;
    }
    
    public RootCauseScore analyzeRootCause(List<Alarm> alarms) {
        if (alarms == null || alarms.isEmpty()) {
            return null;
        }
        
        if (alarms.size() == 1) {
            return new RootCauseScore(alarms.get(0).getAlarmId(), 1.0, "Only alarm in correlation");
        }
        
        Map<String, RootCauseScore> scores = new HashMap<>();
        for (Alarm alarm : alarms) {
            double score = calculateRootCauseScore(alarm, alarms);
            String reasoning = buildReasoning(alarm, alarms);
            scores.put(alarm.getAlarmId(), new RootCauseScore(alarm.getAlarmId(), score, reasoning));
        }
        
        return scores.values().stream()
            .max(Comparator.comparingDouble(RootCauseScore::getScore))
            .orElse(null);
    }
    
    private double calculateRootCauseScore(Alarm candidate, List<Alarm> allAlarms) {
        double score = 0.0;
        
        double topologyScore = calculateTopologyScore(candidate, allAlarms);
        score += topologyScore * 0.4;
        
        double timestampScore = calculateTimestampScore(candidate, allAlarms);
        score += timestampScore * 0.3;
        
        double severityScore = calculateSeverityScore(candidate);
        score += severityScore * 0.2;
        
        double eventTypeScore = calculateEventTypeScore(candidate);
        score += eventTypeScore * 0.1;
        
        return score;
    }
    
    private double calculateTopologyScore(Alarm candidate, List<Alarm> allAlarms) {
        if (topologyRule == null) {
            return 0.0;
        }
        
        int downstreamCount = 0;
        for (Alarm other : allAlarms) {
            if (!candidate.getAlarmId().equals(other.getAlarmId())) {
                if (topologyRule.isUpstreamOf(candidate.getDeviceId(), other.getDeviceId())) {
                    downstreamCount++;
                }
            }
        }
        
        return (double) downstreamCount / (allAlarms.size() - 1);
    }
    
    private double calculateTimestampScore(Alarm candidate, List<Alarm> allAlarms) {
        long earliestTimestamp = allAlarms.stream()
            .mapToLong(Alarm::getTimestamp)
            .min()
            .orElse(candidate.getTimestamp());
        
        long latestTimestamp = allAlarms.stream()
            .mapToLong(Alarm::getTimestamp)
            .max()
            .orElse(candidate.getTimestamp());
        
        if (earliestTimestamp == latestTimestamp) {
            return 1.0;
        }
        
        double position = (double) (candidate.getTimestamp() - earliestTimestamp) / 
                         (latestTimestamp - earliestTimestamp);
        return 1.0 - position;
    }
    
    private double calculateSeverityScore(Alarm alarm) {
        String severity = alarm.getSeverity();
        if (severity == null) {
            return 0.5;
        }
        
        switch (severity.toUpperCase()) {
            case "CRITICAL":
            case "HIGH":
                return 1.0;
            case "MEDIUM":
            case "MODERATE":
                return 0.7;
            case "LOW":
            case "INFO":
                return 0.4;
            default:
                return 0.5;
        }
    }
    
    private double calculateEventTypeScore(Alarm alarm) {
        String eventType = alarm.getEventType();
        if (eventType == null) {
            return 0.5;
        }
        
        String eventUpper = eventType.toUpperCase();
        if (eventUpper.contains("DOWN") || eventUpper.contains("FAILURE") || 
            eventUpper.contains("POWER")) {
            return 1.0;
        } else if (eventUpper.contains("DEGRADED") || eventUpper.contains("LINK")) {
            return 0.7;
        } else if (eventUpper.contains("WARNING") || eventUpper.contains("THRESHOLD")) {
            return 0.5;
        }
        
        return 0.6;
    }
    
    private String buildReasoning(Alarm candidate, List<Alarm> allAlarms) {
        StringBuilder reasoning = new StringBuilder();
        
        long earliestTimestamp = allAlarms.stream()
            .mapToLong(Alarm::getTimestamp)
            .min()
            .orElse(candidate.getTimestamp());
        
        if (candidate.getTimestamp() == earliestTimestamp) {
            reasoning.append("First alarm in sequence. ");
        }
        
        if (topologyRule != null) {
            int downstreamCount = 0;
            for (Alarm other : allAlarms) {
                if (!candidate.getAlarmId().equals(other.getAlarmId())) {
                    if (topologyRule.isUpstreamOf(candidate.getDeviceId(), other.getDeviceId())) {
                        downstreamCount++;
                    }
                }
            }
            if (downstreamCount > 0) {
                reasoning.append(String.format("Upstream of %d devices. ", downstreamCount));
            }
        }
        
        if ("CRITICAL".equalsIgnoreCase(candidate.getSeverity()) || 
            "HIGH".equalsIgnoreCase(candidate.getSeverity())) {
            reasoning.append("High severity. ");
        }
        
        if (reasoning.length() == 0) {
            reasoning.append("Default selection.");
        }
        
        return reasoning.toString().trim();
    }
}
