package models;

import lombok.Builder;
import lombok.Value;
import software.amazon.awssdk.services.cloudtrail.model.LookupEventsResponse;

@Value
@Builder
public class LookupEventsResponsesContext {

    LookupEventsResponse lookupEventsResponse;
}
