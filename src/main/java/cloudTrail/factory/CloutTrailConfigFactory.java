package cloudTrail.factory;

import cloudTrail.client.CTAsyncClient;
import cloudTrail.pipeline.CloudTrailPipeline;
import cloudTrail.service.publishers.handlers.CloudTrailEventHandler;
import cloudTrail.service.publishers.handlers.EventHandler;
import cloudTrail.service.publishers.publisher.StandardOutputPublisher;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudtrail.CloudTrailAsyncClient;

public class CloutTrailConfigFactory {

    public static CloudTrailAsyncClient getCloudTrailAsyncClient() {
        Region region = Region.US_EAST_1;
        return CloudTrailAsyncClient.builder()
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .region(region)
                .build();
    }

    public static CTAsyncClient getCtAsyncClient() {
        return new CTAsyncClient(getCloudTrailAsyncClient());
    }

    public static EventHandler getCloudTrailEventHandler() {
        return new CloudTrailEventHandler(getStandardOutputPublisher());
    }

    public static StandardOutputPublisher getStandardOutputPublisher() {
        return new StandardOutputPublisher();
    }

    public static CloudTrailPipeline getCloudTrailPipelineFactory() {
        return new CloudTrailPipeline(getCtAsyncClient(), getCloudTrailEventHandler());
    }
}
