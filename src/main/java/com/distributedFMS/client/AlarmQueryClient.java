package com.distributedFMS.client;

import com.distributedFMS.grpc.AlarmMessage;
import com.distributedFMS.grpc.AlarmServiceGrpc;
import com.distributedFMS.grpc.QueryAlarmsRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class AlarmQueryClient {

    private final ManagedChannel channel;
    private final AlarmServiceGrpc.AlarmServiceBlockingStub blockingStub;

    public AlarmQueryClient(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.blockingStub = AlarmServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void queryAlarms(QueryAlarmsRequest request) {
        System.out.println("Querying for alarms...");
        try {
            Iterator<AlarmMessage> alarms = blockingStub.queryAlarms(request);
            while (alarms.hasNext()) {
                AlarmMessage alarm = alarms.next();
                System.out.println("Received Alarm: " + alarm);
            }
        } catch (Exception e) {
            System.err.println("RPC failed: " + e.getMessage());
            return;
        }
        System.out.println("Finished querying alarms.");
    }

    public static void main(String[] args) throws InterruptedException {
        AlarmQueryClient client = new AlarmQueryClient("localhost", 50051);
        try {
            System.out.println("\n--- Query 1: Get all alarms ---");
            QueryAlarmsRequest allRequest = QueryAlarmsRequest.newBuilder().build();
            client.queryAlarms(allRequest);

            System.out.println("\n--- Query 2: Get alarms for device_id = device-18 ---");
            QueryAlarmsRequest byDeviceRequest = QueryAlarmsRequest.newBuilder().setDeviceId("device-18").build();
            client.queryAlarms(byDeviceRequest);

            System.out.println("\n--- Query 3: Get alarms for severity = INFO ---");
            QueryAlarmsRequest bySeverityRequest = QueryAlarmsRequest.newBuilder().setSeverity("INFO").build();
            client.queryAlarms(bySeverityRequest);

        } finally {
            client.shutdown();
        }
    }
}
