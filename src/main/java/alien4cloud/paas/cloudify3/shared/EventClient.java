package alien4cloud.paas.cloudify3.shared;

import java.util.Calendar;
import java.util.Date;

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
    protected QueryBuilder createEventsQuery(Date timestamp) {
        BoolQueryBuilder eventsQuery = QueryBuilders.boolQuery();
        if (timestamp != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(timestamp);
            eventsQuery.must(QueryBuilders.rangeQuery("@timestamp").gte(DatatypeConverter.printDateTime(calendar)));
        }
        return QueryBuilders.filteredQuery(eventsQuery, FilterBuilders.existsFilter("context.deployment_id"));
    }
}
