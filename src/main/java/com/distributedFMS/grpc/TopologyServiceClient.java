package com.distributedFMS.grpc;

import com.distributedFMS.grpc.TopologyServiceGrpc;
import com.distributedFMS.grpc.FMSProto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * gRPC Client for the Python Topology Learning Service
 * Calls the GNN-based topology learning microservice
 */
public class TopologyServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(TopologyServiceClient.class);
    
    private final ManagedChannel channel;
    private final TopologyServiceGrpc.TopologyServiceBlockingStub blockingStub;
    
    /**
     * Construct client for accessing TopologyService server
     */
    public TopologyServiceClient(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.blockingStub = TopologyServiceGrpc.newBlockingStub(channel);
        logger.info("TopologyServiceClient connected to {}:{}", host, port);
    }
    
    /**
     * Train the topology model
     */
    public TrainTopologyResponse trainTopology(int alarmHistoryHours, int numEpochs) {
        logger.info("Requesting topology training: {} hours, {} epochs", 
            alarmHistoryHours, numEpochs);
        
        TrainTopologyRequest request = TrainTopologyRequest.newBuilder()
                .setAlarmHistoryHours(alarmHistoryHours)
                .setNumEpochs(numEpochs)
                .build();
        
        try {
            TrainTopologyResponse response = blockingStub.trainTopology(request);
            
            if (response.getSuccess()) {
                logger.info("✅ Training succeeded: {} nodes, {} edges, loss: {}", 
                    response.getNumNodes(), 
                    response.getNumEdges(),
                    response.getTrainingLoss());
            } else {
                logger.error("❌ Training failed: {}", response.getMessage());
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("gRPC call failed: {}", e.getMessage());
            throw new RuntimeException("Failed to train topology", e);
        }
    }
    
    /**
     * Get the learned topology
     */
    public GetTopologyResponse getTopology(double minConfidence) {
        logger.info("Fetching topology with min confidence: {}", minConfidence);
        
        GetTopologyRequest request = GetTopologyRequest.newBuilder()
                .setMinConfidence(minConfidence)
                .build();
        
        try {
            GetTopologyResponse response = blockingStub.getTopology(request);
            
            logger.info("✅ Retrieved topology: {} nodes, {} edges", 
                response.getNodesCount(), 
                response.getEdgesCount());
            
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to get topology: {}", e.getMessage());
            throw new RuntimeException("Failed to get topology", e);
        }
    }
    
    /**
     * Predict if two devices are connected
     */
    public PredictConnectionResponse predictConnection(String deviceA, String deviceB) {
        logger.info("Predicting connection: {} <-> {}", deviceA, deviceB);
        
        PredictConnectionRequest request = PredictConnectionRequest.newBuilder()
                .setDeviceA(deviceA)
                .setDeviceB(deviceB)
                .build();
        
        try {
            PredictConnectionResponse response = blockingStub.predictConnection(request);
            
            logger.info("Connection prediction: connected={}, confidence={}, causality={}", 
                response.getConnected(),
                response.getConfidence(),
                response.getCausalityWeight());
            
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to predict connection: {}", e.getMessage());
            throw new RuntimeException("Failed to predict connection", e);
        }
    }
    
    /**
     * Get all connections for a specific device
     */
    public GetDeviceConnectionsResponse getDeviceConnections(String deviceId, double minConfidence) {
        logger.info("Fetching connections for device: {}", deviceId);
        
        GetDeviceConnectionsRequest request = GetDeviceConnectionsRequest.newBuilder()
                .setDeviceId(deviceId)
                .setMinConfidence(minConfidence)
                .build();
        
        try {
            GetDeviceConnectionsResponse response = blockingStub.getDeviceConnections(request);
            
            logger.info("✅ Device {} has {} connections", 
                deviceId, 
                response.getTotalConnections());
            
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to get device connections: {}", e.getMessage());
            throw new RuntimeException("Failed to get device connections", e);
        }
    }
    
    /**
     * Shutdown the channel
     */
    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        logger.info("TopologyServiceClient shut down");
    }
    
    /**
     * Simple test main
     */
    public static void main(String[] args) {
        TopologyServiceClient client = new TopologyServiceClient("localhost", 50052);
        
        try {
            // Test 1: Train the topology
            logger.info("=== TEST 1: Training Topology ===");
            TrainTopologyResponse trainResponse = client.trainTopology(24, 50);
            System.out.println("Training result: " + trainResponse.getMessage());
            
            // Test 2: Get the topology
            logger.info("=== TEST 2: Getting Topology ===");
            GetTopologyResponse topology = client.getTopology(0.5);
            System.out.println("Topology nodes: " + topology.getNodesCount());
            System.out.println("Topology edges: " + topology.getEdgesCount());
            
            // Print first 5 edges
            System.out.println("\nTop 5 edges:");
            topology.getEdgesList().stream()
                .limit(5)
                .forEach(edge -> {
                    System.out.printf("  %s -> %s (confidence: %.3f, causality: %.3f)\n",
                        edge.getSourceDevice(),
                        edge.getDestinationDevice(),
                        edge.getConfidence(),
                        edge.getCausalityWeight());
                });
            
            // Test 3: Predict a connection
            if (topology.getNodesCount() >= 2) {
                String device1 = topology.getNodes(0).getDeviceId();
                String device2 = topology.getNodes(1).getDeviceId();
                
                logger.info("=== TEST 3: Predicting Connection ===");
                PredictConnectionResponse prediction = client.predictConnection(device1, device2);
                System.out.printf("\nConnection %s <-> %s: %s (confidence: %.3f)\n",
                    device1, device2,
                    prediction.getConnected() ? "CONNECTED" : "NOT CONNECTED",
                    prediction.getConfidence());
            }
            
        } catch (Exception e) {
            logger.error("Test failed", e);
        } finally {
            try {
                client.shutdown();
            } catch (InterruptedException e) {
                logger.error("Shutdown interrupted", e);
            }
        }
    }
}
