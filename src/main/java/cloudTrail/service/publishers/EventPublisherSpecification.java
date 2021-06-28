package cloudTrail.service.publishers;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.cloudtrail.model.Event;


@Slf4j
public abstract class EventPublisherSpecification implements EventPublisher {
    @Override
    public Mono<Event> publishEventBase(Event event) {
        return publish(event)
                .doOnError(throwable -> log.error("Something went wrong", throwable));
    }

    protected abstract Mono<Event> publish(Event event);
}
