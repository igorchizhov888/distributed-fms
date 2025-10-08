package com.distributedFMS.core.model;

public enum AlarmPriority {
    CLEAR(0, "Indicates the alarm condition has been resolved", "White"),
    CRITICAL(1, "Severe service-affecting condition requiring immediate attention", "Red"),
    MAJOR(2, "Significant degradation affecting service quality", "Orange"),
    MINOR(3, "Non-service-affecting condition that should be addressed", "Yellow"),
    WARNING(4, "Early indication of potential issues", "Light Blue/Cyan"),
    SURVEILLANCE(5, "Surveillance/Information-only events", "Green");

    private final int level;
    private final String description;
    private final String color;

    AlarmPriority(int level, String description, String color) {
        this.level = level;
        this.description = description;
        this.color = color;
    }

    public int getLevel() {
        return level;
    }

    public String getDescription() {
        return description;
    }

    public String getColor() {
        return color;
    }
}
