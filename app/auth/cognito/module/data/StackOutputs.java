package app.auth.cognito.module.data;

import com.google.gson.annotations.SerializedName;

public record StackOutputs(
        @SerializedName("logos-app-auth-cognito-stack") CognitoStackOutputs cognitoStackOutputs
) {
}
