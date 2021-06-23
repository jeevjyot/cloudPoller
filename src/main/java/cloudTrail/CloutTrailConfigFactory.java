package cloudTrail;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudtrail.CloudTrailAsyncClient;

public class CloutTrailConfigFactory {

    private CloutTrailConfigFactory() {

    }

    public static CloudTrailAsyncClient getCloudTrailAsyncClient() {
        Region region = Region.US_EAST_1;
        return CloudTrailAsyncClient.builder()
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .region(region)
                .build();
    }
}
