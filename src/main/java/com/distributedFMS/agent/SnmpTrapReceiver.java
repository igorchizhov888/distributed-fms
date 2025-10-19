package com.distributedFMS.agent;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.snmp4j.*;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

public class SnmpTrapReceiver implements CommandResponder {

    private static final Logger logger = Logger.getLogger(SnmpTrapReceiver.class.getName());
    private static final String KAFKA_TOPIC = "fms-events";
    private static final String BOOTSTRAP_SERVERS = "kafka:29092";

    private KafkaProducer<String, String> kafkaProducer;
    private Snmp snmp;

    public static void main(String[] args) {
        SnmpTrapReceiver receiver = new SnmpTrapReceiver();
        try {
            receiver.listen();

            // Block main thread to keep listener running
            synchronized (receiver) {
                receiver.wait();
            }
        } catch (IOException | InterruptedException e) {
            logger.severe("Error in SNMP trap listener: " + e.getMessage());
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    public void listen() throws IOException {
        // Initialize Kafka Producer with retry logic
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
                break; // Success, exit the loop
            } catch (Exception e) {
                logger.warning("Failed to connect to Kafka: " + e.getMessage());
                if (i < maxRetries - 1) {
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    throw new IOException("Could not connect to Kafka after " + maxRetries + " attempts.", e);
                }
            }
        }

        // Initialize SNMP Listener
        TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping(new UdpAddress("0.0.0.0/10162"));
        this.snmp = new Snmp(transport);
        snmp.addCommandResponder(this);

        // Add a shutdown hook to close resources gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down SNMP Trap Receiver...");
            try {
                snmp.close();
            } catch (IOException e) {
                logger.severe("Error closing Snmp: " + e.getMessage());
            }
            kafkaProducer.close();
            logger.info("Shutdown complete.");
        }));

        logger.info("SNMP Trap Receiver started, listening on UDP port 10162...");
        transport.listen();
    }

    @Override
    public synchronized void processPdu(CommandResponderEvent event) {
        PDU pdu = event.getPDU();
        if (pdu == null || pdu.getType() != PDU.TRAP) {
            logger.warning("Received non-TRAP PDU. Ignoring.");
            return;
        }

        String sourceIpWithPort = event.getPeerAddress().toString();
        String sourceIp = extractIpAddress(sourceIpWithPort);
        logger.info("SNMP TRAP RECEIVED from " + sourceIpWithPort + ", extracted IP: " + sourceIp);

        // Extract eventType from the PDU
        Variable eventTypeVar = pdu.getVariable(SnmpConstants.snmpTrapOID);
        String eventType = "Unknown";
        if (eventTypeVar != null) {
            eventType = eventTypeVar.toString();
        }

        // Format the PDU into a standard JSON object
        JsonObject trapJson = new JsonObject();
        trapJson.addProperty("sourceIp", sourceIp);
        trapJson.addProperty("community", new String(event.getSecurityName()));
        trapJson.addProperty("timestamp", System.currentTimeMillis());
        trapJson.addProperty("eventType", eventType);

        JsonArray varBinds = new JsonArray();
        for (VariableBinding vb : pdu.getVariableBindings()) {
            JsonObject varBindJson = new JsonObject();
            varBindJson.addProperty("oid", vb.getOid().toString());
            varBindJson.addProperty("value", vb.getVariable().toString());
            varBinds.add(varBindJson);
        }
        trapJson.add("variableBindings", varBinds);
        trapJson.addProperty("description", varBinds.toString());

        String eventJson = trapJson.toString();
        ProducerRecord<String, String> record = new ProducerRecord<>(KAFKA_TOPIC, sourceIp, eventJson);

        // Send the JSON to the Kafka topic
        kafkaProducer.send(record, (metadata, exception) -> {
            if (exception != null) {
                logger.severe("Failed to send event to Kafka: " + exception.getMessage());
            } else {
                logger.info(String.format("Published event to Kafka topic '%s' (partition %d): %s",
                        metadata.topic(), metadata.partition(), eventJson));
            }
        });
    }

    private String extractIpAddress(String fullAddress) {
        String ip = fullAddress;
        if (ip.startsWith("/")) {
            ip = ip.substring(1);
        }
        int portSeparatorIndex = ip.lastIndexOf(':');
        if (portSeparatorIndex != -1) {
            ip = ip.substring(0, portSeparatorIndex);
        }
        portSeparatorIndex = ip.lastIndexOf('/');
        if (portSeparatorIndex != -1) {
            ip = ip.substring(0, portSeparatorIndex);
        }
        return ip;
    }
}