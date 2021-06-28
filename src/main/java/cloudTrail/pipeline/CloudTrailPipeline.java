package cloudTrail.pipeline;

import cloudTrail.client.CTAsyncClient;
import cloudTrail.pipeline.exceptions.EventHandlerRetryableException;
import cloudTrail.poller.CloudTrailPoller;
import cloudTrail.service.publishers.handlers.EventHandler;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.cloudtrail.model.Event;

import java.util.function.Supplier;

@Slf4j
public class CloudTrailPipeline implements Supplier<Flux<Event>> {

    private final CTAsyncClient ctAsyncClient;
    private final EventHandler cloudTrailEventHandler;

    public CloudTrailPipeline(CTAsyncClient ctAsyncClient, EventHandler eventHandler) {
        this.ctAsyncClient = ctAsyncClient;
        this.cloudTrailEventHandler = eventHandler;
    }

    @Override
    public Flux<Event> get() {
        return Flux.<Event>create(eventFluxSink -> {
            CloudTrailPoller cloudTrailPoller = new CloudTrailPoller(
                    eventFluxSink::next,
                    ctAsyncClient
            );
            eventFluxSink.onRequest(numOfMessages -> cloudTrailPoller.request(numOfMessages));
            eventFluxSink.onCancel(cloudTrailPoller::terminate);
            cloudTrailPoller.runAsync();
        })
                .flatMap(event -> safelyCallHandler(event))
                .onErrorResume(error -> {
                    log.error("An unexpected error was captured in the pipeline.", error);
                    return Mono.just(Event.builder().build());
                });
    }

    private Mono<Event> safelyCallHandler(Event event) {
        return cloudTrailEventHandler.handle(event)
                .onErrorResume(EventHandlerRetryableException.class, throwable -> {
                    return Mono.just(event);
                })
                .doOnError(throwable -> log.error("Something went wrong", throwable));
    }
}
