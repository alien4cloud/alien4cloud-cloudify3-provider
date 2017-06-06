package alien4cloud.paas.cloudify3.shared;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.xml.bind.DatatypeConverter;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Component;

import alien4cloud.paas.cloudify3.restclient.AbstractEventClient;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EventClient extends AbstractEventClient {
    @Override
    protected QueryBuilder createEventsQuery(Date fromDate, Date toDate) {
        BoolQueryBuilder eventsQuery = QueryBuilders.boolQuery();
        if (fromDate != null) {
            Calendar fromCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            fromCalendar.setTime(fromDate);
            if (toDate == null) {
                eventsQuery.must(QueryBuilders.rangeQuery("@timestamp").gte(DatatypeConverter.printDateTime(fromCalendar)));
            } else {
                Calendar toCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                toCalendar.setTime(toDate);
                eventsQuery.must(QueryBuilders.rangeQuery("@timestamp").gte(DatatypeConverter.printDateTime(fromCalendar))
                        .lt(DatatypeConverter.printDateTime(toCalendar)));
            }
        }
        return QueryBuilders.filteredQuery(eventsQuery, FilterBuilders.existsFilter("context.deployment_id"));
    }
}