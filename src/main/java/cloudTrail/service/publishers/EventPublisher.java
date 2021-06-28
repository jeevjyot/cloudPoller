package cloudTrail.service.publishers;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.cloudtrail.model.Event;

public interface EventPublisher {

    /**
     * This is responsible for publishing the event to the stream, which could be
     * 1. Kafka Stream
     * 2. Standard output stream.
     *
     * @return The current event
     */
    Mono<Event> publishEventBase(Event event);
}
