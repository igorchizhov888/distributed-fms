package com.distributedFMS.correlation.model;

/**
 * Enumeration of correlation types for grouping related alarms
 */
public enum CorrelationType {
    TIME,
    ATTRIBUTE,
    TOPOLOGY,
    COMPOSITE,
    ML_PREDICTED
}
