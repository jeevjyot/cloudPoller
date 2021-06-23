package cloudTrail.client;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.cloudtrail.model.LookupEventsResponse;

@FunctionalInterface
public interface LookupEvents {

    Mono<LookupEventsResponse> lookupEvents(String next, int limit);
}
