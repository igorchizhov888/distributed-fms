package com.distributedFMS;

import com.distributedFMS.core.EventConsumer;
import com.distributedFMS.core.FmsCoreServer;
import com.distributedFMS.core.config.FMSIgniteConfig;
import org.apache.ignite.Ignite;

import java.io.IOException;

import java.util.concurrent.CountDownLatch;

import java.util.logging.Logger;



public class DistributedFMSApplication {



    private static final Logger logger = Logger.getLogger(DistributedFMSApplication.class.getName());



        private volatile EventConsumer eventConsumer;



        private volatile Ignite ignite;

    private final CountDownLatch igniteLatch = new CountDownLatch(1);

    private String bootstrapServers;

    public DistributedFMSApplication() {
        this.bootstrapServers = "localhost:9092";
    }

    public DistributedFMSApplication(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public void main(String[] args) throws IOException, InterruptedException {
        boolean clientMode = true;
        String nodeName = "fms-client";

        if (args.length > 0 && "server".equalsIgnoreCase(args[0])) {
            clientMode = false;
            nodeName = (args.length > 1) ? args[1] : "fms-server";
        }

        ignite = FMSIgniteConfig.getInstance();
        igniteLatch.countDown(); // Signal that ignite is initialized

        eventConsumer = new EventConsumer(ignite, bootstrapServers);

        if (clientMode) {
            final FmsCoreServer server = new FmsCoreServer(ignite);
            server.start();
            server.blockUntilShutdown();
        } else {
            // In server mode, start the event consumer and wait indefinitely.
            new Thread(eventConsumer).start();
            Thread.currentThread().join();
        }
    }

    public void shutdown() {
        if (eventConsumer != null) {
            eventConsumer.shutdown();
        }
        if (ignite != null) {
            ignite.close();
        }
    }

    public Ignite getIgnite() throws InterruptedException {
        logger.info("getIgnite() called");
        igniteLatch.await();
        logger.info("getIgnite() returning: " + ignite);
        return ignite;
    }

    public EventConsumer getEventConsumer() {
        return eventConsumer;
    }
}

