package com.distributedFMS.core.correlation;

import com.distributedFMS.core.model.Alarm;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Deduplication correlator that tracks alarms and increments tally count
 * for duplicate alarms based on their ID.
 */
public class DeduplicationCorrelator {
    private static final Logger logger = LoggerFactory.getLogger(DeduplicationCorrelator.class);
    
    private final Ignite ignite;
    private IgniteCache<String, Alarm> deduplicationCache;
    private final ConcurrentHashMap<String, Integer> localTallyMap;

    public DeduplicationCorrelator(Ignite ignite) {
        logger.info("=== Initializing DeduplicationCorrelator ===");
        this.ignite = ignite;
        this.localTallyMap = new ConcurrentHashMap<>();
        
        try {
            // Use a separate cache for deduplication tracking
            this.deduplicationCache = ignite.getOrCreateCache("deduplication");
            logger.info("Successfully initialized deduplication cache");
        } catch (Exception e) {
            logger.error("CRITICAL: Failed to initialize deduplication cache", e);
            throw new RuntimeException("Cannot initialize DeduplicationCorrelator", e);
        }
    }

    /**
     * Correlates an alarm by checking if it's a duplicate.
     * 
     * @param alarm The incoming alarm
     * @return The alarm with updated tally if it's new or first occurrence,
     *         null if it's a duplicate (already processed)
     */
    public Alarm correlate(Alarm alarm) {
        if (alarm == null) {
            logger.warn("Received null alarm in correlate()");
            return null;
        }
        
        String alarmId = alarm.getAlarmId();
        logger.info(">>> DEDUP START: Processing alarm {}", alarmId);
        logger.debug("Alarm details: severity={}, source={}, description={}", 
            alarm.getSeverity(), alarm.getDeviceId(), alarm.getDescription());
        
        try {
            // Check if this alarm has been seen before
            Integer currentTally = localTallyMap.get(alarmId);
            
            if (currentTally == null) {
                // First occurrence of this alarm
                logger.info(">>> DEDUP: First occurrence of alarm {}", alarmId);
                alarm.setTallyCount(1);
                localTallyMap.put(alarmId, 1);
                
                // Store in deduplication cache
                deduplicationCache.put(alarmId, alarm);
                logger.info(">>> DEDUP RESULT: NEW alarm {} stored with tally=1", alarmId);
                
                return alarm; // Return the new alarm to be stored in main cache
                
            } else {
                // This is a duplicate - increment tally
                int newTally = currentTally + 1;
                alarm.setTallyCount(newTally);
                localTallyMap.put(alarmId, newTally);
                
                logger.info(">>> DEDUP: Duplicate alarm {} detected, incrementing tally: {} -> {}", 
                    alarmId, currentTally, newTally);
                
                // Update the alarm in deduplication cache
                deduplicationCache.put(alarmId, alarm);
                logger.info(">>> DEDUP RESULT: DUPLICATE alarm {} updated with tally={}", alarmId, newTally);
                
                // Return the updated alarm so it can be stored in main cache with new tally
                return alarm;
            }
            
        } catch (Exception e) {
            logger.error(">>> DEDUP ERROR: Exception while processing alarm {}", alarmId, e);
            // On error, treat as new alarm to avoid losing it
            alarm.setTallyCount(1);
            return alarm;
        }
    }

    /**
     * Get current tally for an alarm ID
     */
    public int getTally(String alarmId) {
        Integer tally = localTallyMap.get(alarmId);
        return tally != null ? tally : 0;
    }

    /**
     * Reset deduplication state (useful for testing)
     */
    public void reset() {
        logger.info("Resetting deduplication state");
        localTallyMap.clear();
        if (deduplicationCache != null) {
            deduplicationCache.clear();
        }
    }

    /**
     * Get statistics about deduplication
     */
    public void logStats() {
        logger.info("=== Deduplication Statistics ===");
        logger.info("Unique alarms tracked: {}", localTallyMap.size());
        localTallyMap.forEach((id, tally) -> 
            logger.info("  Alarm {}: tally={}", id, tally)
        );
    }
}
