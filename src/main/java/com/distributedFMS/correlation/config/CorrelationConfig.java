package com.distributedFMS.correlation.config;

import com.distributedFMS.correlation.model.CorrelatedAlarm;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;

/**
 * Ignite cache configuration for correlated alarms
 */
public class CorrelationConfig {
    
    public static final String CORRELATED_ALARMS_CACHE_NAME = "correlated-alarms";
    
    public static CacheConfiguration<String, CorrelatedAlarm> correlatedAlarmsCacheConfig() {
        CacheConfiguration<String, CorrelatedAlarm> cacheConfig = 
            new CacheConfiguration<>(CORRELATED_ALARMS_CACHE_NAME);
        
        cacheConfig.setCacheMode(CacheMode.PARTITIONED);
        cacheConfig.setIndexedTypes(String.class, CorrelatedAlarm.class);
        cacheConfig.setSqlSchema("PUBLIC");
        cacheConfig.setBackups(1);
        cacheConfig.setStatisticsEnabled(true);
        
        return cacheConfig;
    }
    
    public static String getCorrelatedAlarmsCacheName() {
        return CORRELATED_ALARMS_CACHE_NAME;
    }
}
