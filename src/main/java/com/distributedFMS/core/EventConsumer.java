package com.distributedFMS.core;

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

    private final KafkaConsumer<String, String> consumer;

    public EventConsumer() {
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
        logger.info("Kafka consumer started, listening for events on topic: " + TOPIC);
        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    logger.info(String.format("Consumed event from partition %d with offset %d: %s",
                            record.partition(), record.offset(), record.value()));
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
