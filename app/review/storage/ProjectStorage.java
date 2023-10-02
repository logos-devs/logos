package review.storage;

import app.review.proto.project.Project;
import dev.logos.stack.service.storage.TableStorage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static dev.logos.stack.service.storage.pg.meta.Review.project;


public class ProjectStorage extends TableStorage<Project, UUID> {

    public ProjectStorage() {
        super(project, Project.class, UUID.class);
    }

    public Project storageToEntity(ResultSet resultSet) throws SQLException {
        return Project.newBuilder()
                      .setDisplayName(resultSet.getString("display_name"))
                      .setFetchUrl(resultSet.getString("fetch_url"))
                      .build();
    }
}