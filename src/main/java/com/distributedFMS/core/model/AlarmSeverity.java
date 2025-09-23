package com.distributedFMS.core.model;

public enum AlarmSeverity {
    HIGH(1), MEDIUM(2), LOW(3);
    
    private final int level;
    AlarmSeverity(int level) { this.level = level; }
    public int getLevel() { return level; }
}