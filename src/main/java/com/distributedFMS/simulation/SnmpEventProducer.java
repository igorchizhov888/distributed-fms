package com.distributedFMS.simulation;

import com.google.gson.JsonObject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class SnmpEventProducer {

    private static final Logger logger = Logger.getLogger(SnmpEventProducer.class.getName());
    private static final String TOPIC = "fms-events";
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private final KafkaProducer<String, String> producer;
    private final Random random = new Random();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public SnmpEventProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        this.producer = new KafkaProducer<>(props);
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::sendEvent, 0, 5, TimeUnit.SECONDS);
        logger.info("SNMP Event Producer started. Sending events to topic: " + TOPIC);
    }

    private void sendEvent() {
        String deviceId = "device-" + random.nextInt(100);
        JsonObject event = new JsonObject();
        event.addProperty("deviceId", deviceId);
        event.addProperty("timestamp", System.currentTimeMillis());
        event.addProperty("severity", "CRITICAL");
        event.addProperty("message", "CPU utilization exceeds 90%");
        event.addProperty("eventType", "SimulatedSnmpEvent");

        String eventJson = event.toString();
        ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, deviceId, eventJson);

        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                logger.severe("Failed to send event: " + exception.getMessage());
            } else {
                logger.info(String.format("Sent event to partition %d with offset %d: %s",
                        metadata.partition(), metadata.offset(), eventJson));
            }
        });
    }

    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
        producer.close();
        logger.info("SNMP Event Producer stopped.");
    }

    public static void main(String[] args) {
        SnmpEventProducer producer = new SnmpEventProducer();
        producer.start();

        // Keep the producer running for a while
        try {
            Thread.sleep(60000); // Run for 60 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            producer.stop();
        }
    }
}
