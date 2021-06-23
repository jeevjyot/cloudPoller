package cloudTrail;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

public class EnvironmentVariablesCredentialProvider implements AwsCredentialsProvider {

    @Override
    public AwsCredentials resolveCredentials() {
        return null;
    }
}
