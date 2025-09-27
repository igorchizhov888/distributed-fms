package com.distributedFMS.client;

import com.distributedFMS.grpc.ManagementServiceGrpc;
import com.distributedFMS.grpc.SubmitEventRequest;
import com.distributedFMS.grpc.SubmitEventResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class FMSClient {

    public static void main(String[] args) throws InterruptedException {
        // Wait for the server to start
        Thread.sleep(5000);

        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();

        ManagementServiceGrpc.ManagementServiceBlockingStub stub =
                ManagementServiceGrpc.newBlockingStub(channel);

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
