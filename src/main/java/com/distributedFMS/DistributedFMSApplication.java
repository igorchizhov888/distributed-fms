package com.distributedFMS;

import com.distributedFMS.core.config.FMSIgniteConfig;
import com.distributedFMS.core.model.Alarm;
import com.distributedFMS.core.model.AlarmSeverity;
import com.distributedFMS.core.processor.FMSEventProcessor;
import com.distributedFMS.grpc.ManagementServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.apache.ignite.Ignite;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class DistributedFMSApplication {

    public static void main(String[] args) throws IOException, InterruptedException {
        String nodeType = args.length > 0 ? args[0] : "server";
        String nodeName = args.length > 1 ? args[1] : "node-1";

        boolean clientMode = "client".equalsIgnoreCase(nodeType);

        System.out.printf("Starting %s node: %s%n", nodeType, nodeName);

        try {
            Ignite ignite = FMSIgniteConfig.createIgniteInstance(nodeName, clientMode);

            System.out.printf("Node %s started successfully%n", nodeName);
            System.out.printf("Client mode: %b%n", clientMode);

            ignite.cluster().active(true);
            Thread.sleep(2000);

            FMSEventProcessor processor = new FMSEventProcessor(ignite);

            if (clientMode) {
                runClientDemo(processor);
            } else {
                startGrpcServer();
                runServerNode(processor);
            }

        } catch (Exception e) {
            System.err.printf("Error starting node %s: %s%n", nodeName, e.getMessage());
            e.printStackTrace();
        }
    }

    private static void startGrpcServer() {
        new Thread(() -> {
            try {
                System.out.println("Building gRPC server...");
                Server server = ServerBuilder.forPort(50051)
                        .addService(new ManagementServiceImpl())
                        .build();

                server.start();
                System.out.println("gRPC server started on port 50051");
                try {
                    server.awaitTermination();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void runClientDemo(FMSEventProcessor processor) throws InterruptedException {
        System.out.println("\n=== Running Client Demo ===");

        String[] regions = {"us-east", "us-west", "eu-central", "asia-pacific"};
        String[] devices = {"router-", "switch-", "gateway-", "basestation-"};
        String[] eventTypes = {"INTERFACE_DOWN", "HIGH_CPU", "MEMORY_FULL", "CONNECTION_TIMEOUT"};
        AlarmSeverity[] severities = {AlarmSeverity.HIGH, AlarmSeverity.MEDIUM, AlarmSeverity.LOW};

        Random random = new Random();

        for (int i = 0; i < 20; i++) {
            String region = regions[random.nextInt(regions.length)];
            String device = devices[random.nextInt(devices.length)] + region + "-" + (i % 5 + 1);
            String eventType = eventTypes[random.nextInt(eventTypes.length)];
            AlarmSeverity severity = severities[random.nextInt(severities.length)];
            String description = String.format("%s detected on %s", eventType, device);

            Alarm alarm = new Alarm(device, severity, eventType, description, region);

            CompletableFuture<String> result = processor.processAlarm(alarm);
            result.thenAccept(alarmId ->
                System.out.printf("✓ Alarm processed: %s%n", alarmId));

            Thread.sleep(100);
        }

        Thread.sleep(2000);

        processor.printClusterInfo();

        System.out.println("\n=== High Severity Alarms ===");
        processor.getHighSeverityAlarms().forEach(alarm ->
            System.out.printf("HIGH: %s - %s%n", alarm.getSourceDevice(), alarm.getDescription()));

        System.out.println("\nDemo completed. Press Ctrl+C to exit...");
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            System.out.println("Client shutting down...");
        }
    }

    private static void runServerNode(FMSEventProcessor processor) throws InterruptedException {
        System.out.println("\n=== Server Node Running ===");
        System.out.println("Waiting for events...");

        while (true) {
            Thread.sleep(30000);
            processor.printClusterInfo();
        }
    }
}
