package com.distributedFMS;

import com.distributedFMS.core.EventConsumer;
import com.distributedFMS.core.config.FMSIgniteConfig;
import com.distributedFMS.core.model.Alarm;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.awaitility.Awaitility.await;


@Testcontainers
public class EventProcessingTest {

    @Container
    public static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    private static Ignite ignite;
    private static EventConsumer eventConsumer;
    private static Thread consumerThread;

    @BeforeAll
    public static void setUp() throws Exception {
        // Start Kafka and wait for it to be ready
        kafka.start();

        // Programmatically create the topic to avoid race conditions
        try (AdminClient adminClient = AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()))) {
            NewTopic newTopic = new NewTopic("fms-events", 1, (short) 1);
            adminClient.createTopics(Collections.singleton(newTopic)).all().get(30, TimeUnit.SECONDS);
        }

        // Use the singleton instance for Ignite
        ignite = FMSIgniteConfig.getInstance();

        // Start the consumer
        eventConsumer = new EventConsumer(ignite, kafka.getBootstrapServers());
        consumerThread = new Thread(eventConsumer);
        consumerThread.start();

        // Wait for the consumer to be ready before sending messages
        eventConsumer.awaitReady();
    }

    @AfterAll
    public static void tearDown() {
        if (eventConsumer != null) {
            eventConsumer.shutdown();
        }
        if (consumerThread != null) {
            consumerThread.interrupt();
        }
        // Stop the singleton instance
        FMSIgniteConfig.stopInstance();
    }

    @Test
    public void testEventProcessing() {
        // Define the test event and expected key
        String eventJson = "{\"sourceIp\":\"192.168.1.1\",\"eventType\":\"link down\",\"description\":\"Link to router is down\"}";
        String expectedKey = "192.168.1.1_link down";

        // Send the event to Kafka
        Properties props = new Properties();
        props.put("bootstrap.servers", kafka.getBootstrapServers());
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>("fms-events", eventJson));
        }

        // Use Awaitility to poll the cache until the alarm appears
        IgniteCache<String, Alarm> alarmCache = ignite.cache(FMSIgniteConfig.getAlarmsCacheName());
        
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Alarm alarm = alarmCache.get(expectedKey);
            assertNotNull(alarm, "Alarm should be present in the cache");
            assertEquals("192.168.1.1", alarm.getSourceDevice());
            assertEquals("link down", alarm.getEventType());
            assertEquals(1, alarm.getTallyCount());
        });
    }
}
