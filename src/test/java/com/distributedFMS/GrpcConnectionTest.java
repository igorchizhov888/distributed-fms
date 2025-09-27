package com.distributedFMS;

import com.distributedFMS.grpc.ManagementServiceGrpc;
import com.distributedFMS.grpc.SubmitEventRequest;
import com.distributedFMS.grpc.SubmitEventResponse;
import com.distributedFMS.grpc.ManagementServiceImpl;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class GrpcConnectionTest {

    @Test
    public void testGrpcConnection() throws IOException, InterruptedException {
        System.out.println("Starting gRPC connection test...");
        final CountDownLatch serverStarted = new CountDownLatch(1);

        // Start the server in a new thread
        new Thread(() -> {
            try {
                System.out.println("Building gRPC server...");
                Server server = ServerBuilder.forPort(50051)
                        .addService(new ManagementServiceImpl())
                        .build();

                server.start();
                System.out.println("gRPC server started on port 50051");
                serverStarted.countDown(); // Signal that the server has started
                server.awaitTermination();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        // Wait for the server to start
        if (!serverStarted.await(10, TimeUnit.SECONDS)) {
            throw new RuntimeException("Server did not start in time");
        }

        // Create a client and connect to the server
        System.out.println("Creating gRPC client...");
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();

        System.out.println("Creating gRPC stub...");
        ManagementServiceGrpc.ManagementServiceBlockingStub stub =
                ManagementServiceGrpc.newBlockingStub(channel);

        System.out.println("Sending gRPC request...");
        SubmitEventResponse response = stub.submitEvent(SubmitEventRequest.newBuilder()
                .setEventType("HIGH_CPU")
                .setDeviceId("router-1")
                .setEventTime(System.currentTimeMillis())
                .setDescription("CPU utilization is high")
                .build());

        System.out.println("Response from server: " + response.getEventId());

        channel.shutdown();
    }
}
