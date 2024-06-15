package review.service;

import app.review.storage.review.ListProjectRequest;
import app.review.storage.review.ProjectStorageServiceBase;
import dev.logos.service.storage.pg.Select;
import dev.logos.user.User;

import static app.review.storage.Review.project;
import static app.review.storage.Review.Project.name;
import static dev.logos.service.storage.pg.Select.select;


public class ProjectService extends ProjectStorageServiceBase {
    @Override
    public <Req> boolean allow(Req request, User user) {
        return user.isAuthenticated();
    }

    @Override
    public Select.Builder query(ListProjectRequest request) {
        return select(name).from(project);
    }
}
