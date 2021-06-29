package cloudTrail.service.publishers.publisher;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.cloudtrail.model.Event;


@Slf4j
public abstract class EventPublisherSpecification implements EventPublisher {
    @Override
    public Mono<Event> processAndPublishEvent(Event event) {
        return publish(event)
                .doOnError(throwable -> log.error("Something went wrong", throwable));
    }

    /**
     * Provides a way to configure different emitter if required
     * @param event
     * @return
     */
    protected abstract Mono<Event> publish(Event event);
}
