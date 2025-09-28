package com.distributedFMS;

import com.distributedFMS.core.FmsCoreServer;

import java.io.IOException;

public class DistributedFMSApplication {

    public static void main(String[] args) throws IOException, InterruptedException {
        final FmsCoreServer server = new FmsCoreServer();
        server.start();
        server.blockUntilShutdown();
    }
}
