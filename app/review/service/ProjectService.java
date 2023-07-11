package review.service;

import static app.review.proto.project.ProjectServiceGrpc.ProjectServiceImplBase;

import app.review.proto.project.ListProjectsRequest;
import app.review.proto.project.ListProjectsResponse;
import dev.logos.stack.service.storage.exceptions.EntityReadException;
import io.grpc.stub.StreamObserver;
import javax.inject.Inject;
import review.storage.ProjectStorage;

public class ProjectService extends ProjectServiceImplBase {

    private final ProjectStorage projectManager;

    @Inject
    ProjectService(ProjectStorage projectManager) {
        this.projectManager = projectManager;
    }

    @Override
    public void listProjects(
        ListProjectsRequest request,
        StreamObserver<ListProjectsResponse> responseObserver
    ) {
        try {
            responseObserver.onNext(
                ListProjectsResponse.newBuilder()
                                    .addAllProjects(projectManager.list().toList())
                                    .build());
            responseObserver.onCompleted();
        } catch (EntityReadException e) {
            e.printStackTrace();
            responseObserver.onError(e);
        }
    }
}