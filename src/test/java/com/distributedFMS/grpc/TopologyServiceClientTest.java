package com.distributedFMS.grpc;

import com.distributedFMS.grpc.TopologyServiceGrpc;
import com.distributedFMS.grpc.FMSProto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * Standalone test for TopologyServiceClient (no Ignite required)
 */
public class TopologyServiceClientTest {
    
    public static void main(String[] args) {
        System.out.println("=== Testing Topology gRPC Service ===\n");
        
        // Create gRPC channel
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 50052)
                .usePlaintext()
                .build();
        
        TopologyServiceGrpc.TopologyServiceBlockingStub stub = 
            TopologyServiceGrpc.newBlockingStub(channel);
        
        try {
            // Test 1: Train the topology
            System.out.println("TEST 1: Training Topology Model");
            System.out.println("-".repeat(50));
            
            TrainTopologyRequest trainRequest = TrainTopologyRequest.newBuilder()
                    .setAlarmHistoryHours(24)
                    .setNumEpochs(50)
                    .build();
            
            TrainTopologyResponse trainResponse = stub.trainTopology(trainRequest);
            
            System.out.println("Success: " + trainResponse.getSuccess());
            System.out.println("Message: " + trainResponse.getMessage());
            System.out.println("Nodes: " + trainResponse.getNumNodes());
            System.out.println("Edges: " + trainResponse.getNumEdges());
            System.out.println("Loss: " + trainResponse.getTrainingLoss());
            System.out.println();
            
            // Test 2: Get the topology
            System.out.println("TEST 2: Getting Learned Topology");
            System.out.println("-".repeat(50));
            
            GetTopologyRequest getRequest = GetTopologyRequest.newBuilder()
                    .setMinConfidence(0.6)
                    .build();
            
            GetTopologyResponse topology = stub.getTopology(getRequest);
            
            System.out.println("Total Nodes: " + topology.getTotalNodes());
            System.out.println("Total Edges: " + topology.getTotalEdges());
            System.out.println("Timestamp: " + topology.getTopologyTimestamp());
            System.out.println();
            
            // Print top 10 edges
            System.out.println("Top 10 Learned Edges:");
            topology.getEdgesList().stream()
                .limit(10)
                .forEach(edge -> {
                    System.out.printf("  %s -> %s | Confidence: %.3f | Causality: %.3f | Count: %d\n",
                        edge.getSourceDevice(),
                        edge.getDestinationDevice(),
                        edge.getConfidence(),
                        edge.getCausalityWeight(),
                        edge.getCoOccurrenceCount());
                });
            System.out.println();
            
            // Test 3: Predict a connection
            if (topology.getNodesCount() >= 2) {
                System.out.println("TEST 3: Predicting Connection");
                System.out.println("-".repeat(50));
                
                String device1 = topology.getNodes(0).getDeviceId();
                String device2 = topology.getNodes(1).getDeviceId();
                
                PredictConnectionRequest predRequest = PredictConnectionRequest.newBuilder()
                        .setDeviceA(device1)
                        .setDeviceB(device2)
                        .build();
                
                PredictConnectionResponse prediction = stub.predictConnection(predRequest);
                
                System.out.printf("Connection: %s <-> %s\n", device1, device2);
                System.out.println("Connected: " + prediction.getConnected());
                System.out.println("Confidence: " + prediction.getConfidence());
                System.out.println("Causality: " + prediction.getCausalityWeight());
                System.out.println("Explanation: " + prediction.getExplanation());
            }
            
            System.out.println("\n✅ All tests completed successfully!");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            channel.shutdown();
        }
    }
}
