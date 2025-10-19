package com.distributedFMS.simulation;

import com.google.gson.JsonObject;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Collections;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class SnmpEventProducer {

    private static final Logger logger = Logger.getLogger(SnmpEventProducer.class.getName());
    private static final String TOPIC = "fms-events";
    private static final String BOOTSTRAP_SERVERS = "kafka:29092";
    private final KafkaProducer<String, String> producer;
    private final Random random = new Random();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public SnmpEventProducer() {
        // Ensure the topic exists before creating the producer
        createTopic();

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        this.producer = new KafkaProducer<>(props);
    }

    private void createTopic() {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        try (AdminClient adminClient = AdminClient.create(props)) {
            // Check if the topic already exists
            if (!adminClient.listTopics().names().get().contains(TOPIC)) {
                logger.info("Topic '" + TOPIC + "' does not exist. Creating it now.");
                NewTopic newTopic = new NewTopic(TOPIC, 1, (short) 1);
                adminClient.createTopics(Collections.singleton(newTopic)).all().get(30, TimeUnit.SECONDS);
            } else {
                logger.info("Topic '" + TOPIC + "' already exists.");
            }
        } catch (Exception e) {
            logger.severe("Failed to create or verify Kafka topic: " + e.getMessage());
            // Optionally, rethrow as a runtime exception to prevent the producer from starting
            throw new RuntimeException("Failed to initialize Kafka topic", e);
        }
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::sendEvent, 0, 5, TimeUnit.SECONDS);
        logger.info("SNMP Event Producer started. Sending events to topic: " + TOPIC);
    }

    private void sendEvent() {
        String deviceId = "device-" + random.nextInt(100);
        JsonObject event = new JsonObject();
        event.addProperty("device_id", deviceId);
        event.addProperty("timestamp", System.currentTimeMillis());
        event.addProperty("severity", "CRITICAL");
        event.addProperty("eventType", "SimulatedSnmpEvent");
        event.addProperty("description", "CPU utilization exceeds 90% for device " + deviceId);

        String eventJson = event.toString();
        ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, deviceId, eventJson);

        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                logger.severe("Failed to send event: " + exception.getMessage());
            } else {
                logger.info(String.format("Sent event to partition %d with offset %d", metadata.partition(), metadata.offset()));
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
        try {
            SnmpEventProducer producer = new SnmpEventProducer();
            producer.start();

            // Keep the producer running for a while to send a few events
            Thread.sleep(20000); // Run for 20 seconds

            producer.stop();
        } catch (Exception e) {
            logger.severe("Application failed to start: " + e.getMessage());
        }
    }
}
