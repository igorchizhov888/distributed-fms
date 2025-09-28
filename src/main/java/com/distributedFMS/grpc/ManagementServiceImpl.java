package com.distributedFMS.grpc;

import com.distributedFMS.grpc.ManagementServiceGrpc.ManagementServiceImplBase;
import com.distributedFMS.grpc.SubmitEventRequest;
import com.distributedFMS.grpc.SubmitEventResponse;
import io.grpc.stub.StreamObserver;

import java.util.UUID;
import java.util.logging.Logger;

public class ManagementServiceImpl extends ManagementServiceImplBase {

    private static final Logger logger = Logger.getLogger(ManagementServiceImpl.class.getName());

    @Override
    public void submitEvent(SubmitEventRequest req, StreamObserver<SubmitEventResponse> responseObserver) {
        logger.info("Received event: " + req.getDescription());
        String eventId = "event-" + UUID.randomUUID().toString();
        SubmitEventResponse response = SubmitEventResponse.newBuilder().setEventId(eventId).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        logger.info("Processed event and responded with ID: " + eventId);
    }
}