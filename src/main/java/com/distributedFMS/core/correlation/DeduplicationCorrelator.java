package com.distributedFMS.core.correlation;

import com.distributedFMS.core.config.FMSIgniteConfig;
import com.distributedFMS.core.model.Alarm;
import com.distributedFMS.core.model.AlarmStatus;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.SqlQuery;

import javax.cache.Cache;
import java.time.Instant;
import java.util.logging.Logger;

public class DeduplicationCorrelator {

    private static final Logger logger = Logger.getLogger(DeduplicationCorrelator.class.getName());
    private final Ignite ignite;
    private final IgniteCache<String, Alarm> alarmCache;

    public DeduplicationCorrelator(Ignite ignite, String cacheName) {
        this.ignite = ignite;
        this.alarmCache = ignite.cache(cacheName);
    }

    /**
     * Apply deduplication correlation rule
     * Criteria: Node (sourceDevice), AlertGroup (eventType), AlertKey (description hash)
     * Action: Increment tally and update last occurrence timestamp
     */
    public void deduplicate(Alarm alarm) {
        String key = generateCorrelationKey(alarm);
        logger.info("DeduplicationCorrelator: Processing alarm with key: " + key);
        // Attempt to retrieve an existing alarm with the same key
        Alarm existingAlarm = this.alarmCache.get(key);

        if (existingAlarm != null) {
            logger.info("DeduplicationCorrelator: Found existing alarm with key: " + key);
            int oldTally = existingAlarm.getTallyCount();
            // If an alarm with the same key exists, update the tally and last occurrence time
            existingAlarm.setTallyCount(oldTally + 1);
            existingAlarm.setLastOccurrence(System.currentTimeMillis());
            this.alarmCache.put(key, existingAlarm); // Update the existing alarm in the cache
            logger.info("DeduplicationCorrelator: Updated existing alarm: " + key + ". Tally incremented from " + oldTally + " to " + existingAlarm.getTallyCount());
        } else {
            // If no such alarm exists, create a new one
            logger.info("DeduplicationCorrelator: No existing alarm found, creating new alarm with key: " + key);
            alarm.setFirstOccurrence(System.currentTimeMillis());
            alarm.setLastOccurrence(System.currentTimeMillis());
            alarm.setTallyCount(1);
            this.alarmCache.put(key, alarm);
        }
    }

    /**
     * Generate correlation key from Alarm object
     */
    private String generateCorrelationKey(Alarm alarm) {
        return generateCorrelationKey(alarm.getSourceDevice(), alarm.getEventType());
    }

    /**
     * Generate correlation key from Node + AlertGroup + AlertKey
     */
    private String generateCorrelationKey(String node, String alertGroup) {
        return String.format("%s_%s",
            node != null ? node : "UNKNOWN",
            alertGroup != null ? alertGroup : "UNKNOWN"
        );
    }
}
