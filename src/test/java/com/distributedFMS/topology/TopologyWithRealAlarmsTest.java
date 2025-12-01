package com.distributedFMS.topology;

import com.distributedFMS.core.model.Alarm;
import com.distributedFMS.core.config.FMSIgniteConfig;
import com.distributedFMS.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.SqlQuery;

import javax.cache.Cache;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TopologyWithRealAlarmsTest {
    
    public static void main(String[] args) {
        System.out.println("=== Testing Topology with REAL Alarms ===\n");
        
        Ignite ignite = null;
        ManagedChannel channel = null;
        
        try {
            System.out.println("Connecting to Ignite...");
            ignite = FMSIgniteConfig.getInstance();
            
            IgniteCache<String, Alarm> alarmsCache = ignite.getOrCreateCache(FMSIgniteConfig.getAlarmsCacheName());
            
            System.out.println("Fetching alarms...");
            List<Alarm> alarms = new ArrayList<>();
            Set<String> devices = new HashSet<>();
            
            SqlQuery<String, Alarm> query = new SqlQuery<>(Alarm.class, "SELECT * FROM Alarm ORDER BY timestamp ASC");
            
            try (var cursor = alarmsCache.query(query)) {
                for (Cache.Entry<String, Alarm> entry : cursor) {
                    Alarm alarm = entry.getValue();
                    alarms.add(alarm);
                    devices.add(alarm.getDeviceId());
                }
            }
            
            System.out.println("Found " + alarms.size() + " alarms from " + devices.size() + " devices");
            
            if (alarms.isEmpty()) {
                System.out.println("❌ No alarms! Generate some first.");
                return;
            }
            
            channel = ManagedChannelBuilder.forAddress("localhost", 50052).usePlaintext().build();
            TopologyServiceGrpc.TopologyServiceBlockingStub stub = TopologyServiceGrpc.newBlockingStub(channel);
            
            System.out.println("\nConverting to protobuf...");
            List<AlarmData> alarmDataList = new ArrayList<>();
            for (Alarm alarm : alarms) {
                alarmDataList.add(AlarmData.newBuilder()
                        .setDeviceId(alarm.getDeviceId())
                        .setTimestamp(alarm.getTimestamp())
                        .setSeverity(alarm.getSeverity())
                        .setEventType(alarm.getEventType())
                        .setDescription(alarm.getDescription())
                        .build());
            }
            
            System.out.println("\nTraining with REAL alarms...");
            System.out.println("-".repeat(60));
            
            TrainTopologyRequestWithAlarms trainRequest = 
                TrainTopologyRequestWithAlarms.newBuilder()
                    .addAllAlarms(alarmDataList)
                    .addAllDeviceIds(devices)
                    .setNumEpochs(50)
                    .build();
            
            TrainTopologyResponse trainResponse = stub.trainTopologyWithAlarms(trainRequest);
            
            System.out.println("Success: " + trainResponse.getSuccess());
            System.out.println("Message: " + trainResponse.getMessage());
            System.out.println("Nodes: " + trainResponse.getNumNodes());
            System.out.println("Edges: " + trainResponse.getNumEdges());
            System.out.println();
            
            System.out.println("Getting topology...");
            GetTopologyRequest getRequest = GetTopologyRequest.newBuilder().setMinConfidence(0.6).build();
            GetTopologyResponse topology = stub.getTopology(getRequest);
            
            System.out.println("Nodes: " + topology.getTotalNodes());
            System.out.println("Edges: " + topology.getTotalEdges());
            System.out.println();
            
            System.out.println("Top 10 Edges:");
            topology.getEdgesList().stream().limit(10).forEach(edge -> 
                System.out.printf("  %s -> %s | Conf: %.3f | Caus: %.3f\n",
                    edge.getSourceDevice(), edge.getDestinationDevice(),
                    edge.getConfidence(), edge.getCausalityWeight())
            );
            
            System.out.println("\n✅ Success!");
            
        } catch (Exception e) {
            System.err.println("❌ Failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (channel != null) channel.shutdown();
            if (ignite != null) ignite.close();
        }
    }
}
