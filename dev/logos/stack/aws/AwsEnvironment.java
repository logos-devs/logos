package dev.logos.stack.aws;

import dev.logos.config.infra.InfrastructureProvider;

/**
 * Centralizes feature-flag style checks for AWS-specific behavior so that alternative deployments can
 * disable AWS integrations. The value is supplied via the //dev/logos/config/infra:provider build setting.
 */
public final class AwsEnvironment {
    private static final String AWS = "aws";

    private AwsEnvironment() {}

    public static boolean isEnabled() {
        return AWS.equalsIgnoreCase(InfrastructureProvider.VALUE);
    }
}
