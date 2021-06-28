package cloudTrail.service.publishers;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.cloudtrail.model.Event;

public class KafkaPublisher extends EventPublisherSpecification {
    @Override
    protected Mono<Event> publish(Event setting) {
        return Mono.empty();
    }
}
