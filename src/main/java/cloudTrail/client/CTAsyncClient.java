package cloudTrail.client;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.cloudtrail.CloudTrailAsyncClient;
import software.amazon.awssdk.services.cloudtrail.model.LookupEventsRequest;
import software.amazon.awssdk.services.cloudtrail.model.LookupEventsResponse;

@Slf4j
public class CTAsyncClient implements LookupEvents {

    private final CloudTrailAsyncClient cloudTrailAsyncClient;

    public CTAsyncClient(CloudTrailAsyncClient cloudTrailAsyncClient) {
        this.cloudTrailAsyncClient = cloudTrailAsyncClient;
    }

    public Mono<LookupEventsResponse> lookupEvents(String next, int limit) {
        LookupEventsRequest request = LookupEventsRequest.builder()
                .nextToken(next)
                .maxResults(limit)
                .build();
        return Mono.fromCompletionStage(cloudTrailAsyncClient.lookupEvents(request))
                .onErrorResume(throwable -> {
                    log.warn("Something went wrong", throwable);
                    return Mono.just(LookupEventsResponse.builder().build());
                });
    }
}
