package app.auth.service;

import app.auth.proto.cognito.CognitoServiceGrpc;
import app.auth.proto.cognito.ProcessAuthCodeRequest;
import app.auth.proto.cognito.ProcessAuthCodeResponse;
import io.grpc.stub.StreamObserver;

public class CognitoService extends CognitoServiceGrpc.CognitoServiceImplBase {
    @Override
    public void processAuthCode(ProcessAuthCodeRequest request, StreamObserver<ProcessAuthCodeResponse> responseObserver) {
        responseObserver.onNext(ProcessAuthCodeResponse.newBuilder().build());
        responseObserver.onCompleted();
    }
}