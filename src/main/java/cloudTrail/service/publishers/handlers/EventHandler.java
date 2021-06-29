package cloudTrail.service.publishers.handlers;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.cloudtrail.model.Event;

public interface EventHandler {

    /***
     * Handles the event recieved from the cloudTrail event
     * @param event see {@link Event}
     * @return
     */
    public Mono<Event> handle(Event event);
}
