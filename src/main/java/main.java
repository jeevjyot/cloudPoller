import cloudTrail.CloutTrailConfigFactory;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.cloudtrail.CloudTrailAsyncClient;
import software.amazon.awssdk.services.cloudtrail.model.Event;
import software.amazon.awssdk.services.cloudtrail.model.LookupEventsRequest;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class main {

    public static void main(String args[]) {
        try {
            CloudTrailAsyncClient cloudTrailClient = CloutTrailConfigFactory.getCloudTrailAsyncClient();
            lookupAllEvents(cloudTrailClient);
            cloudTrailClient.close();

        } catch (Exception exception) {
            System.out.println("exception");
            System.out.println(exception.getMessage());
        }

    }

    public static void lookupAllEvents(CloudTrailAsyncClient cloudTrailClientClient) throws Exception {
        AtomicInteger atomicInteger = new AtomicInteger();
        LookupEventsRequest eventsRequest = LookupEventsRequest.builder()
                .maxResults(5)
                .build();
        Mono.fromCompletionStage(cloudTrailClientClient.lookupEvents(eventsRequest))
                .expandDeep(lookupEventsResponse -> Mono.justOrEmpty(lookupEventsResponse.nextToken())
                        .flatMap(s -> {
                            LookupEventsRequest lookupEventsRequest = LookupEventsRequest.builder()
                                    .maxResults(5)
                                    .nextToken(s)
                                    .build();
                            System.out.println("Iteration = " + atomicInteger.getAndIncrement());
                            return Mono.fromCompletionStage(cloudTrailClientClient.lookupEvents(lookupEventsRequest));
                        })
                        .doOnNext(lookupEventsResponse1 -> {
                            var events = lookupEventsResponse1.events();

                            for (Event event : events) {
                                System.out.println("Event name is : " + event.eventName());
                                System.out.println("The event source is : " + event.eventSource());
                            }
                        }))
                .collectList()
                .toFuture()
                .get();
    }
}
