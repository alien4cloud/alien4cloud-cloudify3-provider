package alien4cloud.paas.cloudify3.model;

import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Lists;

@Getter
@Setter
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.UnusedPrivateField")
public class GetEventsResult extends AbstractCloudifyModel {

    private GetEventsHits hits;

    @Getter
    @Setter
    @EqualsAndHashCode
    @JsonIgnoreProperties(ignoreUnknown = true)
    @SuppressWarnings("PMD.UnusedPrivateField")
    private static class GetEventsHits extends AbstractCloudifyModel {
        private GetEventsHit[] hits;
    }

    @Getter
    @Setter
    @EqualsAndHashCode
    @JsonIgnoreProperties(ignoreUnknown = true)
    @SuppressWarnings("PMD.UnusedPrivateField")
    private static class GetEventsHit extends AbstractCloudifyModel {
        private Event __source;
        private String __id;
    }

    @JsonIgnore
    public Event[] getEvents() {
        if (hits == null) {
            return new Event[0];
        }
        GetEventsHit[] internalHits = hits.getHits();
        if (internalHits == null || internalHits.length == 0) {
            return new Event[0];
        }
        List<Event> events = Lists.newArrayList();
        for (GetEventsHit internalHit : internalHits) {
            if (internalHit != null && internalHit.get__source() != null) {
                Event event = internalHit.get__source();
                event.setId(internalHit.__id);
                events.add(internalHit.get__source());
            }
        }
        return events.toArray(new Event[events.size()]);
    }
}
