package cloudTrail.service.publishers.handlers;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.cloudtrail.model.Event;

public interface EventHandler {

    public Mono<Event> handle(Event event);
}
