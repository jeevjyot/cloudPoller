package poller;

import cloudTrail.client.LookupEvents;
import cloudTrail.poller.CloudTrailPoller;
import cloudTrail.poller.OnMessageReceived;
import org.junit.After;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.cloudtrail.model.Event;
import software.amazon.awssdk.services.cloudtrail.model.LookupEventsResponse;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CloudTrailerPollerTests {
    private Queue<LookupEventsResponse> events;
    private CloudTrailPoller polling;

    @After
    public void after() {
        polling.terminate();
    }

    @Test
    public void shouldReceiveMessage() throws InterruptedException {
        // prepare
        events = new ConcurrentLinkedQueue<>();
        CountDownLatch cdl = new CountDownLatch(1);
        events.add(LookupEventsResponse.builder()
                .events(Event.builder()
                        .eventName("test-event")
                        .eventSource("test-source")
                        .build()).build());
        OnMessageReceived onMessageReceived = event -> {
            assertThat(event.eventName(), is("test-event"));
            cdl.countDown();
        };

        // execute
        polling = new CloudTrailPoller(onMessageReceived, eventReceiver());
        polling.runAsync();
        polling.request(1L);

        // assert
        cdl.await(1L, SECONDS);
        assertThat(cdl.getCount(), is(0L));
    }

    private LookupEvents eventReceiver() {
        return (next, limit) ->
                Mono.just(events.poll());
    }

}
