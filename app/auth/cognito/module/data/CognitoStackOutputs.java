package app.auth.cognito.module.data;

import com.google.gson.annotations.SerializedName;

import static java.util.Objects.requireNonNull;

public record CognitoStackOutputs(
        @SerializedName("CognitoUserPoolId") String cognitoUserPoolId,
        @SerializedName("CognitoUserPoolDomainRedirectUrl") String cognitoUserPoolDomainRedirectUrl,
        @SerializedName("CognitoClientCredentialsSecretArn") String cognitoClientCredentialsSecretArn,
        @SerializedName("CognitoUserPoolDomainBaseUrl") String cognitoUserPoolDomainBaseUrl,
        @SerializedName("CognitoUserPoolDomainSignInUrl") String cognitoUserPoolDomainSignInUrl
) {
    public CognitoStackOutputs {
        requireNonNull(cognitoClientCredentialsSecretArn);
        requireNonNull(cognitoUserPoolId);
        requireNonNull(cognitoUserPoolDomainSignInUrl);
        requireNonNull(cognitoUserPoolDomainBaseUrl);
        requireNonNull(cognitoUserPoolDomainRedirectUrl);
    }
}
