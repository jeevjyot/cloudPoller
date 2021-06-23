package cloudTrail.poller;


import software.amazon.awssdk.services.cloudtrail.model.Event;
import software.amazon.awssdk.services.cloudtrail.model.LookupEventsResponse;

/**
 * Function that will receive an event from {@link CloudTrailPoller}.
 *
 * <p>This function is called for each event received. Blocking this operation will block
 * all the requested messages in the {@link CloudTrailPoller}.
 */
@FunctionalInterface
public interface OnMessageReceived {

    /**
     * @param event {@link Event} received from cloudtrail
     */
    void receive(Event event);
}
