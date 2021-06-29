package cloudTrail.pipeline;

import cloudTrail.client.LookupEvents;
import cloudTrail.pipeline.exceptions.EventHandlerRetryableException;
import cloudTrail.poller.CloudTrailPoller;
import cloudTrail.service.publishers.handlers.EventHandler;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.cloudtrail.model.Event;

import java.util.function.Supplier;

/***
 * Main reactive pipeline for CloudTrailer events
 *
 * This listener builds the following pipeline
 * Event Received - calls handler - emit emiited
 *
 * Pipeline polls the events from {@link CloudTrailPoller}
 */
@Slf4j
public class CloudTrailPipeline implements Supplier<Flux<Event>> {

    private final LookupEvents lookupEvents;
    private final EventHandler cloudTrailEventHandler;

    public CloudTrailPipeline(LookupEvents ctAsyncClient, EventHandler eventHandler) {
        this.lookupEvents = ctAsyncClient;
        this.cloudTrailEventHandler = eventHandler;
    }

    @Override
    public Flux<Event> get() {
        return Flux.<Event>create(eventFluxSink -> {
            CloudTrailPoller cloudTrailPoller = new CloudTrailPoller(
                    eventFluxSink::next,
                    lookupEvents
            );
            eventFluxSink.onRequest(cloudTrailPoller::request);
            eventFluxSink.onCancel(cloudTrailPoller::terminate);
            cloudTrailPoller.runAsync();
        })
                .flatMap(this::safelyCallHandler)
                .onErrorResume(error -> {
                    log.error("An unexpected error was captured in the pipeline.", error);
                    return Mono.just(Event.builder().build());
                });
    }

    private Mono<Event> safelyCallHandler(Event event) {
        return cloudTrailEventHandler.handle(event)
                .onErrorResume(EventHandlerRetryableException.class, throwable -> Mono.just(event))
                .doOnError(throwable -> log.error("Something went wrong", throwable));
    }
}
