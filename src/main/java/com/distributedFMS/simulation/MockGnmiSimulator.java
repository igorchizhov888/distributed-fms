package com.distributedFMS.simulation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.logging.Logger;

public class MockGnmiSimulator {

    private static final Logger logger = Logger.getLogger(MockGnmiSimulator.class.getName());
    private static final String KAFKA_TOPIC = "fms-events";
    private static final String BOOTSTRAP_SERVERS = "kafka:29092";

    private KafkaProducer<String, String> kafkaProducer;

    public static void main(String[] args) {
        MockGnmiSimulator simulator = new MockGnmiSimulator();
        try {
            simulator.initialize();
            simulator.simulateAlarmStream();
        } catch (Exception e) {
            logger.severe("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initialize() throws Exception {
        int maxRetries = 5;
        for (int i = 0; i < maxRetries; i++) {
            try {
                logger.info("Connecting to Kafka (attempt " + (i + 1) + "/" + maxRetries + ")...");
                Properties props = new Properties();
                props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
                props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                this.kafkaProducer = new KafkaProducer<>(props);
                logger.info("Connected to Kafka successfully");
                break;
            } catch (Exception e) {
                if (i < maxRetries - 1) {
                    Thread.sleep(5000);
                } else {
                    throw e;
                }
            }
        }
    }

    private void simulateAlarmStream() throws InterruptedException {
        logger.info("Starting mock gNMI alarm stream simulation...");
        logger.info("Simulating realistic scenario: 1 parent + 4 children alarms");

        // Send parent alarm first (root cause)
        logger.info("Sending parent alarm: Service Down");
        JsonObject parentAlarm = createParentAlarm();
        sendAlarmToKafka(parentAlarm);
        Thread.sleep(1000);

        // Send 4 child alarms (consequences)
        String[] childDescriptions = {
            "Connection Lost",
            "Timeout Detected",
            "Resource Exhausted",
            "Circuit Failure"
        };

        for (int i = 0; i < childDescriptions.length; i++) {
            logger.info("Sending child alarm " + (i + 1) + ": " + childDescriptions[i]);
            JsonObject childAlarm = createChildAlarm(i, childDescriptions[i]);
            sendAlarmToKafka(childAlarm);
            Thread.sleep(1000);
        }

        kafkaProducer.close();
        logger.info("Simulation complete");
        logger.info("Expected result: 1 parent (tally 1) + 4 children (tally 1 each) = total parent tally 5");
    }

    private JsonObject createParentAlarm() {
        JsonObject alarm = new JsonObject();
        alarm.addProperty("sourceIp", "10.0.0.1");  // Different source for realism
        alarm.addProperty("community", "public");
        alarm.addProperty("timestamp", System.currentTimeMillis());
        alarm.addProperty("eventType", "1.3.6.1.6.3.1.1.5.3");  // Linkdown trap
        alarm.addProperty("description", "[GNMI_5G_PARENT] Service Down - Power Unit Failure");

        JsonArray vbArray = new JsonArray();

        // OID 1: sysUpTime
        JsonObject vb1 = new JsonObject();
        vb1.addProperty("oid", "1.3.6.1.2.1.1.3.0");
        vb1.addProperty("value", String.valueOf(System.currentTimeMillis() / 1000));
        vbArray.add(vb1);

        // OID 2: snmpTrapOID
        JsonObject vb2 = new JsonObject();
        vb2.addProperty("oid", "1.3.6.1.6.3.1.1.4.1.0");
        vb2.addProperty("value", "1.3.6.1.6.3.1.1.5.3");
        vbArray.add(vb2);

        // OID 3: description
        JsonObject vb3 = new JsonObject();
        vb3.addProperty("oid", "1.3.6.1.4.1.8072.2.3.0.1");
        vb3.addProperty("value", "Service Down - Power Unit Failure on Router A");
        vbArray.add(vb3);

        alarm.add("variableBindings", vbArray);

        return alarm;
    }

    private JsonObject createChildAlarm(int index, String description) {
        JsonObject alarm = new JsonObject();
        alarm.addProperty("sourceIp", "10.0.0.1");  // Same source as parent
        alarm.addProperty("community", "public");
        alarm.addProperty("timestamp", System.currentTimeMillis());
        alarm.addProperty("eventType", "1.3.6.1.6.3.1.1.5.3");  // Different trap type (consequence)
        alarm.addProperty("description", "[GNMI_5G_CHILD] " + description);

        JsonArray vbArray = new JsonArray();

        // OID 1: sysUpTime
        JsonObject vb1 = new JsonObject();
        vb1.addProperty("oid", "1.3.6.1.2.1.1.3.0");
        vb1.addProperty("value", String.valueOf(System.currentTimeMillis() / 1000));
        vbArray.add(vb1);

        // OID 2: snmpTrapOID
        JsonObject vb2 = new JsonObject();
        vb2.addProperty("oid", "1.3.6.1.6.3.1.1.4.1.0");
        vb2.addProperty("value", "1.3.6.1.6.3.1.1.5.3");
        vbArray.add(vb2);

        // OID 3: description
        JsonObject vb3 = new JsonObject();
        vb3.addProperty("oid", "1.3.6.1.4.1.8072.2.3.0.1");
        vb3.addProperty("value", description + " (Port " + (index + 1) + ")");
        vbArray.add(vb3);

        alarm.add("variableBindings", vbArray);

        return alarm;
    }

    private void sendAlarmToKafka(JsonObject alarm) {
        String alarmJson = alarm.toString();
        kafkaProducer.send(new ProducerRecord<>(KAFKA_TOPIC, "GNMI_5G_" + System.currentTimeMillis(), alarmJson),
                (metadata, exception) -> {
                    if (exception == null) {
                        logger.info("Published: " + alarm.get("description").getAsString());
                    } else {
                        logger.severe("Failed: " + exception.getMessage());
                    }
                });
    }
}
