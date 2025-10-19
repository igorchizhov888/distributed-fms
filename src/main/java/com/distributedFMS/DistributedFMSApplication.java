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
        this.bootstrapServers = "kafka:29092";
    }

    public DistributedFMSApplication(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        DistributedFMSApplication app = new DistributedFMSApplication();
        app.run(args);
    }

    public void run(String[] args) throws IOException, InterruptedException {
        // Default to server mode if no arguments are provided, or if the first argument is 'server'.
        if (args.length == 0 || "server".equalsIgnoreCase(args[0])) {
            ignite = FMSIgniteConfig.getInstance();
            igniteLatch.countDown(); // Signal that ignite is initialized

            final FmsCoreServer server = new FmsCoreServer(ignite);
            server.start();
            server.blockUntilShutdown();
        } else {
            // Handle other modes if necessary, for now, just log.
            logger.warning("Unknown application mode specified. Exiting.");
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

