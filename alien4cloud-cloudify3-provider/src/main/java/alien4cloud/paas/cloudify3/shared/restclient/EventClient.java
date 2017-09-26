package alien4cloud.paas.cloudify3.shared.restclient;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.bind.DatatypeConverter;

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
 * Note, we for now use api v2.1 for events, since v3 is constantly changing. when stabilized, we should use it
 */
@Slf4j
public class EventClient {
    public static final String EVENTS_PATH = "/api/v2.1/events";
    private final ApiHttpClient client;

    public EventClient(ApiHttpClient apiHttpClient) {
        this.client = apiHttpClient;
    }

    @SneakyThrows
    public ListenableFuture<Event[]> asyncGetBatch(Date fromDate, Date toDate, int from, int batchSize) {
        Map<String, Object> request = Maps.newLinkedHashMap();
        if (fromDate != null) {
            Calendar fromCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            fromCalendar.setTime(fromDate);
            if (toDate == null) {
                request.put("_range", "@timestamp," + DatatypeConverter.printDateTime(fromCalendar) + ",");
            } else {
                Calendar toCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                toCalendar.setTime(toDate);
                // we need toDate to be exclusive so decrement a little
                toCalendar.add(Calendar.MILLISECOND, -1);
                request.put("_range", "@timestamp," + DatatypeConverter.printDateTime(fromCalendar) + "," + DatatypeConverter.printDateTime(toCalendar));
            }
        }
        request.put("_offset", from);
        request.put("_size", batchSize);
        request.put("_sort", "@timestamp");

        // FIXME filter results on context.deployment_id
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
                .unwrapRestResponse(client.getForEntity(client.buildRequestUrl(EVENTS_PATH, allParamsNames.toArray(new String[allParamsNames.size()])),
                        ListEventResponse.class, allParamsValues.toArray(new Object[allParamsValues.size()])));
        return Futures.transform(eventsResultListenableFuture, ListEventResponse::getItems);
    }
}
