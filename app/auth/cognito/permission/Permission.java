package app.auth.cognito.permission;

public interface Permission {
    default String getIdentifier() {
        return this.getClass().getCanonicalName();
    }
}

enum UserPermission implements Permission {
    UPDATE_PROFILE
}