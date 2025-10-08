package com.distributedFMS;

import com.distributedFMS.core.EventConsumer;
import com.distributedFMS.core.FmsCoreServer;
import com.distributedFMS.core.config.FMSIgniteConfig;
import org.apache.ignite.Ignite;

import java.io.IOException;

public class DistributedFMSApplication {

    public static void main(String[] args) throws IOException, InterruptedException {
        boolean clientMode = true;
        String nodeName = "fms-client";

        if (args.length > 0 && "server".equalsIgnoreCase(args[0])) {
            clientMode = false;
            nodeName = (args.length > 1) ? args[1] : "fms-server";
        }

        try (Ignite ignite = FMSIgniteConfig.createIgniteInstance(nodeName, clientMode)) {
            if (clientMode) {
                final FmsCoreServer server = new FmsCoreServer(ignite);
                server.start();
                server.blockUntilShutdown();
            } else {
                // In server mode, start the event consumer and wait indefinitely.
                EventConsumer eventConsumer = new EventConsumer(ignite);
                new Thread(eventConsumer).start();
                Thread.currentThread().join();
            }
        }
    }
}
