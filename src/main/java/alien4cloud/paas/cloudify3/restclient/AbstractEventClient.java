package alien4cloud.paas.cloudify3.restclient;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import alien4cloud.paas.cloudify3.model.Event;
import alien4cloud.paas.cloudify3.model.ListEventResponse;
import alien4cloud.paas.cloudify3.util.FutureUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class for event access
 */
@Slf4j
public abstract class AbstractEventClient extends AbstractClient {

    public static final String EVENTS_PATH = "/events";

    protected abstract Map<String, Object> createEventsQuery();

    @Override
    protected String getPath() {
        return EVENTS_PATH;
    }

    @Deprecated
    /**
     *
     * @deprecated hack due to event api limitation on filtering on event_type. Delete this when fixed!
     */
    // FIXME hack due to event api limitation on filtering on event_type. Delete this when fixed
    protected abstract Set<String> getValidEventTypes();

    @SneakyThrows
    public ListenableFuture<Event[]> asyncGetBatch(String executionId, Date fromDate, int from, int batchSize) {
        Map<String, Object> request = Maps.newLinkedHashMap();
        if (fromDate != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(fromDate);
            request.put("_range", "@timestamp," + DatatypeConverter.printDateTime(calendar) + ",");
        }
        request.put("_offset", from);
        request.put("_size", batchSize);
        request.put("_sort", "@timestamp");
        if (StringUtils.isNotBlank(executionId)) {
            request.put("execution_id", executionId);
        }
        request.putAll(createEventsQuery());
        String[] paramsNames = request.keySet().toArray(new String[request.size()]);
        Object[] paramsValues = request.values().toArray(new Object[request.size()]);
        List<String> allParamsNames = Lists.newArrayList();
        List<Object> allParamsValues = Lists.newArrayList();
        for (int i = 0; i < paramsValues.length; i++) {
            Object paramValue = paramsValues[i];
            if (paramValue instanceof Object[]) {
                Object[] paramValueArray = (Object[]) paramValue;
                for (Object paramValueComponent : paramValueArray) {
                    allParamsNames.add(paramsNames[i]);
                    allParamsValues.add(paramValueComponent);
                }
            } else {
                allParamsNames.add(paramsNames[i]);
                allParamsValues.add(paramValue);
            }
        }
        ListenableFuture<ListEventResponse> eventsResultListenableFuture = FutureUtil
                .unwrapRestResponse(getForEntity(getSuffixedUrl(null, allParamsNames.toArray(new String[allParamsNames.size()])), ListEventResponse.class,
                        allParamsValues.toArray(new Object[allParamsValues.size()])));
        // return Futures.transform(eventsResultListenableFuture, ListEventResponse::getItems);
        // FIXME:Event4.0 hack due to event api limitation on filtering on event_type. uncomment the line above when fixed
        return Futures.transform(eventsResultListenableFuture, (Function<? super ListEventResponse, ? extends Event[]>) listEventResponse -> {
            Set<String> validEventTypes = getValidEventTypes();
            Event[] events = listEventResponse.getItems();
            return (validEventTypes == null || events == null) ? events
                    : Arrays.stream(events).filter(event -> validEventTypes.contains(event.getEventType())).toArray(Event[]::new);
        });
    }

    @SneakyThrows
    public Event[] getBatch(String executionId, Date fromDate, int from, int batchSize) {
        return asyncGetBatch(executionId, fromDate, from, batchSize).get();
    }
}
