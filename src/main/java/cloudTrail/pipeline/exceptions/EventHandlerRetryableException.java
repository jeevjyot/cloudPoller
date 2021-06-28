package cloudTrail.pipeline.exceptions;

public class EventHandlerRetryableException extends RuntimeException {

    public EventHandlerRetryableException(String message) {
        super(message);
    }
}
