package com.distributedFMS.core;

import com.distributedFMS.core.config.FMSIgniteConfig;
import com.distributedFMS.core.correlation.DeduplicationCorrelator;
import com.distributedFMS.core.correlation.ClearCorrelator;
import com.distributedFMS.core.model.Alarm;
import com.distributedFMS.core.model.AlarmPriority;
import com.distributedFMS.core.priority.AlarmPrioritizationEngine;
import org.apache.ignite.configuration.CacheConfiguration;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import com.distributedFMS.correlation.engine.CorrelationEngine;
import com.distributedFMS.correlation.config.CorrelationConfig;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.logging.Logger;

import java.util.concurrent.CountDownLatch;

public class EventConsumer implements Runnable {

    private static final Logger logger = Logger.getLogger(EventConsumer.class.getName());
    private static final String TOPIC = "fms-events";
    private static final String BOOTSTRAP_SERVERS = "kafka:29092";
    private static final String EVENTS_CACHE_NAME = "fms-events-cache";

    private final KafkaConsumer<String, String> consumer;
    private final Ignite ignite;
    private final Gson gson = new Gson();
    private final DeduplicationCorrelator deduplicationCorrelator;
    private final CorrelationEngine correlationEngine;
    private final ClearCorrelator clearCorrelator;
    private final CountDownLatch readyLatch = new CountDownLatch(1);

    public EventConsumer(Ignite ignite) {
        this(ignite, BOOTSTRAP_SERVERS);
    }

    public EventConsumer(Ignite ignite, String bootstrapServers) {
        this.ignite = ignite;
        ignite.getOrCreateCache(FMSIgniteConfig.getAlarmsCacheName()).getConfiguration(CacheConfiguration.class).setIndexedTypes(String.class, Alarm.class);
        this.deduplicationCorrelator = new DeduplicationCorrelator(ignite);
        this.clearCorrelator = new ClearCorrelator(ignite, false);
        this.correlationEngine = new CorrelationEngine(
            ignite,
            FMSIgniteConfig.getAlarmsCacheName(),
            CorrelationConfig.getCorrelatedAlarmsCacheName(),
            60000,
            100
        );
        logger.info("Correlation engine initialized");
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "fms-core-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        this.consumer = new KafkaConsumer<>(props);
        this.consumer.subscribe(Collections.singletonList(TOPIC));
        readyLatch.countDown();
    }

    public void awaitReady() throws InterruptedException {
        readyLatch.await();
    }

    @Override
    public void run() {
        System.out.println("Kafka consumer started, listening for events on topic: " + TOPIC);
        IgniteCache<String, String> eventsCache = ignite.getOrCreateCache(EVENTS_CACHE_NAME);

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    System.out.println("[DEBUG] Processing record...");
                    try {
                        String eventJson = record.value();
                        System.out.println("Received event JSON: " + eventJson);
                        System.out.println(String.format("Consumed event from partition %d with offset %d: %s",
                                record.partition(), record.offset(), eventJson));

                        JsonObject jsonObject = gson.fromJson(eventJson, JsonObject.class);
                        String deviceId = jsonObject.get("sourceIp").getAsString();

                        eventsCache.put(deviceId, eventJson);
                        logger.info("Put event from source '" + deviceId + "' into cache '" + EVENTS_CACHE_NAME + "'");

                        if (!jsonObject.has("eventType")) {
                            logger.warning("Skipping event with missing 'eventType' field: " + eventJson);
                            continue;
                        }

                        System.out.println("[DEBUG] About to create Alarm object...");
                        Alarm alarm = new Alarm(
                                deviceId,
                                com.distributedFMS.core.model.AlarmSeverity.MAJOR.name(),
                                jsonObject.get("eventType").getAsString(),
                                jsonObject.get("description").getAsString(),
                                "UNKNOWN"
                        );

                        // NEW: Check if this is a clear event BEFORE deduplication
                        System.out.println("[DEBUG] Checking for clear event...");
                        boolean isClearEvent = clearCorrelator.processClearEvent(alarm);
                        
                        if (isClearEvent) {
                            System.out.println("[DEBUG] Clear event processed, skipping normal alarm flow");
                            logger.info("CLEAR EVENT processed: " + alarm.getDescription());
                            continue; // Don't store clear events as new alarms
                        }

                        // Process the alarm through the deduplication correlator
                        System.out.println("[DEBUG] About to deduplicate alarm...");
                        Alarm processedAlarm = deduplicationCorrelator.correlate(alarm);
                        System.out.println("[DEBUG] After deduplication, alarmId=" + processedAlarm.getAlarmId());
                        logger.info("Deduplicated alarm: " + processedAlarm.getAlarmId());
                        
                        // Store deduplicated alarm immediately so gRPC can see it
                        try {
                            IgniteCache<String, Alarm> alarmsCache = ignite.getOrCreateCache(FMSIgniteConfig.getAlarmsCacheName());
                            if (alarmsCache != null) {
                                alarmsCache.put(processedAlarm.getAlarmId(), processedAlarm);
                                logger.info("✓ Stored alarm in alarms cache: " + processedAlarm.getAlarmId() + " (tally: " + processedAlarm.getTallyCount() + ")");
                            } else {
                                logger.severe("ERROR: alarms cache is NULL!");
                            }
                        } catch (Exception cacheEx) {
                            logger.severe("EXCEPTION storing alarm in cache: " + cacheEx.getMessage());
                            cacheEx.printStackTrace();
                        }

                        // Trigger correlation (async, non-blocking)
                        correlationEngine.correlateAsync(processedAlarm)
                            .exceptionally(ex -> {
                                logger.warning("Correlation failed for alarm " + processedAlarm.getAlarmId() + ": " + ex.getMessage());
                                return null;
                            });
                    } catch (Exception recordEx) {
                        logger.severe("ERROR processing record: " + recordEx.getMessage());
                        recordEx.printStackTrace();
                    }
                }
            }
        } catch (Exception mainEx) {
            logger.severe("ERROR in EventConsumer.run(): " + mainEx.getMessage());
            mainEx.printStackTrace();
        } finally {
            consumer.close();
        }
    }

    public void shutdown() {
        consumer.wakeup();
    }
}
