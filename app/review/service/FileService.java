package review.service;

import static app.review.proto.file.FileServiceGrpc.FileServiceImplBase;

import app.review.proto.file.File;
import app.review.proto.file.GetFileRequest;
import app.review.proto.file.ListFilesRequest;
import app.review.proto.file.ListFilesResponse;
import dev.logos.stack.service.storage.exceptions.EntityReadException;
import io.grpc.stub.StreamObserver;
import review.storage.FileStorage;

public class FileService extends FileServiceImplBase {

    @Override
    public void getFile(
        GetFileRequest request,
        StreamObserver<File> responseObserver) {
        // TODO(trinque): Inject this with Guice
        FileStorage fileManager = new FileStorage();

        try {
            responseObserver.onNext(fileManager.get(request.getName()));
            responseObserver.onCompleted();
        } catch (EntityReadException e) {
            e.printStackTrace();
            responseObserver.onError(e);
        }
    }

    @Override
    public void listFiles(
        ListFilesRequest request,
        StreamObserver<ListFilesResponse> responseObserver
    ) {
        // TODO(trinque): Inject this with Guice
        FileStorage fileManager = new FileStorage();

        try {
            responseObserver.onNext(
                ListFilesResponse.newBuilder()
                                 .addAllFiles(
                                     fileManager.list(request.getParent())
                                 )
                                 .build());
            responseObserver.onCompleted();
        } catch (EntityReadException e) {
            e.printStackTrace();
            responseObserver.onError(e);
        }
    }
}
