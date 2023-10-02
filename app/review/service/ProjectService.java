package review.service;

import app.review.proto.project.ListProjectsRequest;
import app.review.proto.project.ListProjectsResponse;
import app.review.proto.project.Project;
import dev.logos.stack.service.storage.exceptions.EntityReadException;
import dev.logos.stack.service.storage.pg.Select;
import io.grpc.stub.StreamObserver;
import review.storage.ProjectStorage;

import javax.inject.Inject;
import java.util.stream.Stream;

import static app.review.proto.project.ProjectServiceGrpc.ProjectServiceImplBase;
import static dev.logos.stack.service.storage.pg.meta.Review.project;

public class ProjectService extends ProjectServiceImplBase {

    private final ProjectStorage projectStorage;

    @Inject
    ProjectService(ProjectStorage projectStorage) {
        this.projectStorage = projectStorage;
    }

    @Override
    public void listProjects(
        ListProjectsRequest request,
        StreamObserver<ListProjectsResponse> responseObserver
    ) {
        try (Stream<Project> projectListStream = projectStorage.query(
                Select.builder().from(project)
        )) {
            responseObserver.onNext(
                ListProjectsResponse.newBuilder()
                                    .addAllProjects(projectListStream.toList())
                                    .build());
            responseObserver.onCompleted();
        } catch (EntityReadException e) {
            responseObserver.onError(e);
        }
    }
}