package com.distributedFMS;

import com.distributedFMS.core.FmsCoreServer;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;

import java.io.IOException;

public class DistributedFMSApplication {

    public static void main(String[] args) throws IOException, InterruptedException {
        try (Ignite ignite = Ignition.start()) {
            final FmsCoreServer server = new FmsCoreServer(ignite);
            server.start();
            server.blockUntilShutdown();
        }
    }
}
