package cloudTrail.service.publishers.publisher;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.cloudtrail.model.Event;

@Slf4j
public class StandardOutputPublisher extends EventPublisherSpecification {
    @Override
    protected Mono<Event> publish(Event event) {
        return Mono.just(event)
                .doOnEach(eventSignal -> log.info("Event name is : " + event.eventName() + " " + "From Source " + event.eventSource()))
                .map(event1 -> event1);
    }
}
