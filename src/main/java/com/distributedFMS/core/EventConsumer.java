package com.distributedFMS.core;

import com.distributedFMS.core.config.FMSIgniteConfig;
import com.distributedFMS.core.correlation.DeduplicationCorrelator;
import com.distributedFMS.core.model.Alarm;
import com.distributedFMS.core.model.AlarmSeverity;
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

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.logging.Logger;

public class EventConsumer implements Runnable {

    private static final Logger logger = Logger.getLogger(EventConsumer.class.getName());
    private static final String TOPIC = "fms-events";
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String EVENTS_CACHE_NAME = "fms-events-cache";

    private final KafkaConsumer<String, String> consumer;
    private final Ignite ignite;
    private final Gson gson = new Gson();
    private final DeduplicationCorrelator deduplicationCorrelator;

    public EventConsumer(Ignite ignite) {
        this.ignite = ignite;
        ignite.getOrCreateCache(FMSIgniteConfig.getAlarmsCacheName()).getConfiguration(CacheConfiguration.class).setIndexedTypes(String.class, Alarm.class);
        this.deduplicationCorrelator = new DeduplicationCorrelator(ignite, FMSIgniteConfig.getAlarmsCacheName());
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "fms-core-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        this.consumer = new KafkaConsumer<>(props);
        this.consumer.subscribe(Collections.singletonList(TOPIC));
    }

    @Override
    public void run() {
        System.out.println("Kafka consumer started, listening for events on topic: " + TOPIC);
        IgniteCache<String, String> eventsCache = ignite.getOrCreateCache(EVENTS_CACHE_NAME);

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    String eventJson = record.value();
                    System.out.println("Received event JSON: " + eventJson);
                    System.out.println(String.format("Consumed event from partition %d with offset %d: %s",
                            record.partition(), record.offset(), eventJson));

                    JsonObject jsonObject = gson.fromJson(eventJson, JsonObject.class);
                    String sourceIp = jsonObject.get("sourceIp").getAsString();

                    // Put the event into the Ignite cache
                    eventsCache.put(sourceIp, eventJson);
                    logger.info("Put event from source '" + sourceIp + "' into cache '" + EVENTS_CACHE_NAME + "'");

                    if (!jsonObject.has("eventType")) {
                        logger.warning("Skipping event with missing 'eventType' field: " + eventJson);
                        continue;
                    }

                    // Create an Alarm from the event
                    Alarm alarm = new Alarm(
                            sourceIp,
                            AlarmSeverity.INFO, // Default severity
                            jsonObject.get("eventType").getAsString(),
                            jsonObject.get("description").getAsString(),
                            "UNKNOWN" // Default region
                    );

                    // Process the alarm through the deduplication correlator
                    deduplicationCorrelator.deduplicate(alarm);
                }
            }
        } finally {
            consumer.close();
        }
    }

    public void shutdown() {
        consumer.wakeup();
    }
}
