package com.distributedFMS.core;

import com.distributedFMS.grpc.ManagementServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.apache.ignite.Ignite;

import java.io.IOException;
import java.util.logging.Logger;

public class FmsCoreServer {

    private static final Logger logger = Logger.getLogger(FmsCoreServer.class.getName());

    private final Ignite ignite;
    private Server grpcServer;
    private EventConsumer kafkaConsumer;
    private Thread consumerThread;

    public FmsCoreServer(Ignite ignite) {
        this.ignite = ignite;
    }

    public void start() throws IOException {
        // Start gRPC Server
        int port = 50051;
        grpcServer = ServerBuilder.forPort(port)
                .addService(new ManagementServiceImpl())
                .build()
                .start();
        logger.info("gRPC Server started, listening on " + port);

        // Start Kafka Consumer
        kafkaConsumer = new EventConsumer(ignite);
        consumerThread = new Thread(kafkaConsumer);
        consumerThread.start();

        // Add Shutdown Hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("*** shutting down FMS Core since JVM is shutting down");
            FmsCoreServer.this.stop();
            System.err.println("*** FMS Core shut down");
        }));
    }

    public void stop() {
        if (kafkaConsumer != null) {
            kafkaConsumer.shutdown();
        }
        if (grpcServer != null) {
            grpcServer.shutdown();
        }
        try {
            if (consumerThread != null) {
                consumerThread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (grpcServer != null) {
            grpcServer.awaitTermination();
        }
    }
}
