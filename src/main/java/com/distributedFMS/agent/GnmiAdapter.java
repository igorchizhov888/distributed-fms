package com.distributedFMS.agent;

import com.google.gson.JsonObject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.logging.Logger;

public class GnmiAdapter {

    private static final Logger logger = Logger.getLogger(GnmiAdapter.class.getName());
    private static final String KAFKA_TOPIC = "fms-events";
    private static final String BOOTSTRAP_SERVERS = "kafka:29092";
    private static final int GNMI_PORT = 9830;

    private KafkaProducer<String, String> kafkaProducer;

    public static void main(String[] args) {
        GnmiAdapter adapter = new GnmiAdapter();
        try {
            adapter.listen();
            synchronized (adapter) {
                adapter.wait();
            }
        } catch (Exception e) {
            logger.severe("Error in gNMI adapter: " + e.getMessage());
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    public void listen() throws Exception {
        // Initialize Kafka Producer
        initializeKafkaProducer();
        
        // TODO: Initialize gNMI server on port 9830
        logger.info("gNMI Adapter started, listening on port " + GNMI_PORT + "...");
    }

    private void initializeKafkaProducer() throws Exception {
        int maxRetries = 5;
        long retryDelayMs = 5000;
        
        for (int i = 0; i < maxRetries; i++) {
            try {
                logger.info("Attempting to connect to Kafka (attempt " + (i + 1) + "/" + maxRetries + ")...");
                Properties props = new Properties();
                props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
                props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                this.kafkaProducer = new KafkaProducer<>(props);
                logger.info("Successfully connected to Kafka.");
                break;
            } catch (Exception e) {
                logger.warning("Failed to connect to Kafka: " + e.getMessage());
                if (i < maxRetries - 1) {
                    Thread.sleep(retryDelayMs);
                } else {
                    throw new Exception("Could not connect to Kafka after " + maxRetries + " attempts.");
                }
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down gNMI Adapter...");
            kafkaProducer.close();
            logger.info("Shutdown complete.");
        }));
    }

    protected void publishAlarmToKafka(JsonObject alarm) {
        String alarmJson = alarm.toString();
        kafkaProducer.send(new ProducerRecord<>(KAFKA_TOPIC, alarm.get("alarm_id").getAsString(), alarmJson));
        logger.info("Published alarm to Kafka: " + alarm.get("alarm_id"));
    }
}
