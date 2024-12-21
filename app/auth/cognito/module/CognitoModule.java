package app.auth.cognito.module;

import app.auth.cognito.interceptor.cognito.CognitoServerInterceptor;
import app.auth.cognito.interceptor.cookie.CookieServerInterceptor;
import app.auth.cognito.module.data.CognitoClientCredentialsSecret;
import app.auth.cognito.module.data.CognitoStackOutputs;
import app.auth.cognito.module.data.StackOutputs;
import app.auth.cognito.service.CognitoService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.multibindings.OptionalBinder;
import dev.logos.app.AppModule;
import dev.logos.app.register.registerModule;
import dev.logos.stack.aws.module.EksModule.EksStack;
import dev.logos.stack.aws.module.annotation.StackOutputsJson;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.cognito.*;
import software.amazon.awscdk.services.iam.Policy;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static dev.logos.stack.aws.module.EksModule.EksStack.RPC_SERVICE_ACCOUNT_ROLE_ARN_OUTPUT;

@registerModule
public class CognitoModule extends AppModule {
    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CognitoAllowedCallbackUrl {
    }

    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CognitoSignInRedirectUrl {
    }

    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CognitoAllowedLogoutUrl {
    }

    @Override
    protected void configure() {
        stack(CognitoStack.class);
        services(CognitoService.class);
        interceptors(CookieServerInterceptor.class, CognitoServerInterceptor.class);

        OptionalBinder.newOptionalBinder(binder(), CognitoDomainOptions.Builder.class).setDefault().toInstance(
                CognitoDomainOptions.builder().domainPrefix("logos"));
    }

    @Provides
    SecretsManagerClient secretsManagerClient() {
        return SecretsManagerClient.create();
    }

    @Provides
    CognitoClientCredentialsSecret getCognitoClientCredentialsSecret(
            CognitoStackOutputs cognitoStackOutputs,
            SecretsManagerClient secretsManagerClient
    ) throws SecretsManagerException {
        return new Gson().fromJson(
                secretsManagerClient.getSecretValue(
                        GetSecretValueRequest.builder()
                                .secretId(cognitoStackOutputs.cognitoClientCredentialsSecretArn())
                                .build()
                ).secretString(),
                CognitoClientCredentialsSecret.class
        );
    }

    @Provides
    UserPoolProps.Builder userPoolPropsBuilder() {
        return UserPoolProps.builder()
                .signInCaseSensitive(false)
                .selfSignUpEnabled(true)
                .signInAliases(
                        SignInAliases.builder()
                                .email(true)
                                .phone(true)
                                .build())
                .keepOriginal(
                        KeepOriginalAttrs.builder()
                                .email(true)
                                .phone(true)
                                .build())
                .mfaSecondFactor(
                        MfaSecondFactor.builder()
                                .sms(true)
                                .otp(true)
                                .build());
    }

    @Provides
    UserPoolDomainProps.Builder userPoolDomainPropsBuilder(CognitoDomainOptions.Builder cognitoDomainOptionsBuilder) {
        return UserPoolDomainProps.builder().cognitoDomain(cognitoDomainOptionsBuilder.build());
    }

    @Provides
    UserPoolClientProps.Builder userPoolClientPropsBuilder(
            @CognitoAllowedCallbackUrl Set<String> callbackUrls,
            @CognitoAllowedLogoutUrl Set<String> logoutUrls
    ) {
        return UserPoolClientProps.builder()
                .generateSecret(true)
                .accessTokenValidity(Duration.hours(8))
                .idTokenValidity(Duration.hours(8))
                .oAuth(OAuthSettings.builder()
                        .callbackUrls(callbackUrls.stream().toList())
                        .logoutUrls(logoutUrls.stream().toList())
                        .flows(OAuthFlows.builder()
                                .authorizationCodeGrant(true)
                                .build())
                        .scopes(List.of(OAuthScope.EMAIL,
                                OAuthScope.OPENID,
                                OAuthScope.PROFILE))
                        .build());
    }

    @Provides
    SignInUrlOptions.Builder signInUrlOptionsBuilder(@CognitoSignInRedirectUrl String redirectUrl) {
        return SignInUrlOptions.builder()
                .redirectUri(redirectUrl);
    }

    @Provides
    @CognitoStackOutputsJson
    InputStream cognitoStackOutputsJsonProvider(@StackOutputsJson String stackOutputsJson) {
        return Objects.requireNonNull(getClass().getResourceAsStream(stackOutputsJson));
    }

    @Provides
    CognitoStackOutputs cognitoStackOutputsProvider(
            @CognitoStackOutputsJson InputStream cognitoServerConfigsJson
    ) {
        return new Gson().<StackOutputs>fromJson(
                new InputStreamReader(cognitoServerConfigsJson),
                new TypeToken<StackOutputs>() {
                }.getType()
        ).cognitoStackOutputs();
    }

    @BindingAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CognitoStackOutputsJson {
    }

    public static class CognitoStack extends Stack {
        @Inject
        public CognitoStack(
                App app,
                StackProps props,
                UserPoolProps.Builder userPoolPropsBuilder,
                UserPoolDomainProps.Builder userPoolDomainPropsBuilder,
                UserPoolClientProps.Builder userPoolClientPropsBuilder,
                SignInUrlOptions.Builder signInUrlOptionsBuilder,
                EksStack eksStack
        ) {
            super(app, "logos-app-auth-cognito-stack", props);
            this.addDependency(eksStack);

            String id = this.getArtifactId();

            String userPoolId = id + "-user-pool";
            UserPool userPool = new UserPool(
                    this,
                    userPoolId,
                    userPoolPropsBuilder.userPoolName(userPoolId).build());

            UserPoolDomain userPoolDomain = new UserPoolDomain(
                    this,
                    id + "-user-pool-domain",
                    userPoolDomainPropsBuilder.userPool(userPool).build());

            UserPoolClient userPoolClient = new UserPoolClient(
                    this,
                    id + "-user-pool-client",
                    userPoolClientPropsBuilder.userPool(userPool).build());

            SignInUrlOptions signInUrlOptions = signInUrlOptionsBuilder.build();

            Secret clientCredentialsSecret =
                    Secret.Builder.create(this, id + "-client-credentials")
                            .secretName(id + "-client-credentials")
                            .secretObjectValue(Map.of(
                                    "clientId",
                                    SecretValue.Builder.create(userPoolClient.getUserPoolClientId()).build(),
                                    "clientSecret",
                                    SecretValue.Builder.create(userPoolClient.getUserPoolClientSecret()).build()
                            ))
                            .build();

            String clientCredentialsSecretArn = clientCredentialsSecret.getSecretArn();

            Role.fromRoleArn(
                    this,
                    id + "-service-account-role",
                    Fn.importValue(RPC_SERVICE_ACCOUNT_ROLE_ARN_OUTPUT)
            ).attachInlinePolicy(
                    Policy.Builder.create(this, id + "-secret-access-policy")
                            .statements(
                                    List.of(PolicyStatement.Builder.create()
                                            .actions(List.of("secretsmanager:GetSecretValue"))
                                            .resources(List.of(clientCredentialsSecretArn))
                                            .build()))
                            .build());

            new CfnOutput(this, "CognitoClientCredentialsSecretArn", CfnOutputProps.builder()
                    .value(clientCredentialsSecretArn)
                    .build());

            new CfnOutput(this, "CognitoUserPoolId", CfnOutputProps.builder()
                    .value(userPool.getUserPoolId())
                    .build());

            new CfnOutput(this, "CognitoUserPoolDomainSignInUrl", CfnOutputProps.builder()
                    .value(userPoolDomain.signInUrl(userPoolClient, signInUrlOptionsBuilder.build()))
                    .build());

            new CfnOutput(this, "CognitoUserPoolDomainBaseUrl", CfnOutputProps.builder()
                    .value(userPoolDomain.baseUrl())
                    .build());

            new CfnOutput(this, "CognitoUserPoolDomainRedirectUrl", CfnOutputProps.builder()
                    .value(signInUrlOptions.getRedirectUri())
                    .build());
        }
    }
}
