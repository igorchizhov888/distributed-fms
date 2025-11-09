package com.distributedFMS.correlation.rules;

import com.distributedFMS.core.model.Alarm;
import com.distributedFMS.correlation.model.CorrelationType;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Executes correlation rules and determines if alarms should be correlated
 */
public class RuleEngine {
    
    private final List<CorrelationRule> rules;
    
    public RuleEngine() {
        this.rules = new ArrayList<>();
    }
    
    public void registerRule(CorrelationRule rule) {
        rules.add(rule);
        rules.sort((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()));
    }
    
    public Optional<CorrelationRule> evaluate(Alarm alarm1, Alarm alarm2) {
        for (CorrelationRule rule : rules) {
            if (rule.canCorrelate(alarm1, alarm2)) {
                return Optional.of(rule);
            }
        }
        return Optional.empty();
    }
    
    public List<CorrelationRule> evaluateAll(Alarm alarm1, Alarm alarm2) {
        return rules.stream()
            .filter(rule -> rule.canCorrelate(alarm1, alarm2))
            .collect(Collectors.toList());
    }
    
    public double getHighestConfidence(Alarm alarm1, Alarm alarm2) {
        return rules.stream()
            .filter(rule -> rule.canCorrelate(alarm1, alarm2))
            .mapToDouble(rule -> rule.getConfidenceScore(alarm1, alarm2))
            .max()
            .orElse(0.0);
    }
    
    public CorrelationType getCorrelationType(Alarm alarm1, Alarm alarm2) {
        Optional<CorrelationRule> rule = evaluate(alarm1, alarm2);
        if (rule.isPresent()) {
            List<CorrelationRule> allMatches = evaluateAll(alarm1, alarm2);
            if (allMatches.size() > 1) {
                return CorrelationType.COMPOSITE;
            }
            return rule.get().getType();
        }
        return null;
    }
    
    public List<CorrelationRule> getRules() {
        return new ArrayList<>(rules);
    }
    
    public void clearRules() {
        rules.clear();
    }
}
