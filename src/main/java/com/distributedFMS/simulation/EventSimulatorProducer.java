package com.distributedFMS.simulation;

import com.google.gson.Gson;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.logging.Logger;

public class EventSimulatorProducer {

    private static final Logger logger = Logger.getLogger(EventSimulatorProducer.class.getName());
    private static final String TOPIC = "fms-events";
    private static final String BOOTSTRAP_SERVERS = "kafka:29092";

    private final KafkaProducer<String, String> producer;
    private final Gson gson = new Gson();

    public EventSimulatorProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        this.producer = new KafkaProducer<>(props);
    }

    public void sendEvent(String eventType, String deviceId, String description) {
        logger.info("Sending event to Kafka topic: " + TOPIC);
        SimulatedEvent event = new SimulatedEvent(eventType, deviceId, System.currentTimeMillis(), description);
        String eventJson = gson.toJson(event);

        ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, deviceId, eventJson);

        producer.send(record, (metadata, exception) -> {
            if (exception == null) {
                logger.info("Event sent successfully to partition " + metadata.partition() + " with offset " + metadata.offset());
            } else {
                logger.severe("Failed to send event: " + exception.getMessage());
            }
        });
    }

    public void close() {
        producer.flush();
        producer.close();
    }

    public static void main(String[] args) {
        EventSimulatorProducer producer = new EventSimulatorProducer();
        producer.sendEvent("HIGH_CPU", "router-1", "CPU utilization is at 95%");
        producer.close();
    }

    // Inner class to represent the event structure
    private static class SimulatedEvent {
        private final String eventType;
        private final String deviceId;
        private final long eventTime;
        private final String description;

        public SimulatedEvent(String eventType, String deviceId, long eventTime, String description) {
            this.eventType = eventType;
            this.deviceId = deviceId;
            this.eventTime = eventTime;
            this.description = description;
        }
    }
}