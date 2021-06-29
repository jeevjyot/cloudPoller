package CTClientTests;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudtrail.CloudTrailAsyncClient;
import software.amazon.awssdk.services.cloudtrail.model.LookupEventsRequest;
import software.amazon.awssdk.services.cloudtrail.model.LookupEventsResponse;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Slf4j
public class TestCTClientTests {

    @Test
    public void testIntegrationWithCloudTrail() throws Exception {
        Region region = Region.US_EAST_1;
        CloudTrailAsyncClient client = CloudTrailAsyncClient.builder()
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .region(region)
                .build();

        LookupEventsResponse lookupEventsResponse1 = client.lookupEvents(LookupEventsRequest.builder()
                .maxResults(1)
                .build()).get();

        assertThat(lookupEventsResponse1.events().size(), is(1));
    }
}
