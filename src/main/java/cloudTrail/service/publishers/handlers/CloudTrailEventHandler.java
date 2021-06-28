package cloudTrail.service.publishers.handlers;

import cloudTrail.service.publishers.EventPublisherSpecification;
import cloudTrail.service.publishers.StandardOutputPublisher;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.cloudtrail.model.Event;

@Slf4j
public class CloudTrailEventHandler implements EventHandler {

    private final StandardOutputPublisher standardOutputPublisher;

    public CloudTrailEventHandler(StandardOutputPublisher standardOutputPublisher) {
        this.standardOutputPublisher = standardOutputPublisher;
    }

    public Mono<Event> handle(Event event) {
        return standardOutputPublisher.publishEventBase(event);
    }
}
