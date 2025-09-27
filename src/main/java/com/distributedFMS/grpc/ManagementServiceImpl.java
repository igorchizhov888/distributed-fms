package com.distributedFMS.grpc;

import com.distributedFMS.grpc.ManagementServiceGrpc;
import com.distributedFMS.grpc.SubmitEventRequest;
import com.distributedFMS.grpc.SubmitEventResponse;
import io.grpc.stub.StreamObserver;

public class ManagementServiceImpl extends ManagementServiceGrpc.ManagementServiceImplBase {

    @Override
    public void submitEvent(SubmitEventRequest request, StreamObserver<SubmitEventResponse> responseObserver) {
        System.out.println("Received event: " + request.getDescription());

        SubmitEventResponse response = SubmitEventResponse.newBuilder()
                .setEventId("event-123")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
