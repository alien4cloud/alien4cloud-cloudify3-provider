package alien4cloud.paas.cloudify3.eventpolling;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.paas.cloudify3.event.LiverPollerStarted;
import alien4cloud.paas.cloudify3.model.Event;
import alien4cloud.paas.cloudify3.util.DateUtil;
import alien4cloud.paas.cloudify3.util.SyspropConfig;
import alien4cloud.paas.model.PaaSDeploymentLog;
import alien4cloud.rest.model.BasicSearchRequest;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.RangeFilterBuilder;
import org.slf4j.Logger;
import org.springframework.context.event.*;
import org.springframework.context.event.EventListener;

import javax.annotation.Resource;
import javax.xml.bind.DatatypeConverter;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This poller is responsible of polling events that have been missed when the system was down.
 */
@Slf4j
@Setter
public class RecoveryPoller extends AbstractPoller {

    /**
     * This is the max recovery period we will use if no event is found in the system.
     */
    private static final Period MAX_HISTORY_PERIOD = Period.ofDays(SyspropConfig.getInt(SyspropConfig.RECOVERYPOLLER_MAX_HISTORY_PERIOD_IN_DAYS, 10));

    /**
     * This is the max recovery period we will use if no event is found in the system.
     */
    private static final Duration RECOVERY_INTERVAL = Duration.ofSeconds(SyspropConfig.getLong(SyspropConfig.RECOVERYPOLLER_RECOVERY_INTERVAL_IN_SECONDS, 300));

    private static final boolean ACTIVATED = SyspropConfig.getBoolean(SyspropConfig.RECOVERYPOLLER_ACTIVATED, true);

    @Override
    public String getPollerNature() {
        return "Recovery stream";
    }

    @Resource(name = "alien-monitor-es-dao")
    private IGenericSearchDAO alienMonitorDao;

    /**
     * Used to submit the recovery poll task.
     */
    private ScheduledExecutorService scheduler;

    /**
     * This cache is used to deduplicate revcovered events.
     */
    private final Set<String> recoveryCache = Sets.newHashSet();

    private AtomicBoolean livepollerStarted = new AtomicBoolean(false);

    /**
     * This poller will always request the same period, defined at startup.
     */
    private Instant fromDate;

    private Instant toDate;

    @EventListener
    public void signalLivePollerStarted(LiverPollerStarted liverPollerStarted) {
        this.livepollerStarted.set(true);
        logInfo("Live poller started polling. I will let him do its job now !");
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    /**
     * Poll historical events since the last known event date (stored in the system A4C) to now.
     * By this way all events that have been missed while the poller was offline will be polled.
     */
    @Override
    public void start() {
        if (!ACTIVATED) {
            log.info("Recovery is desactivated");
            return;
        }
        scheduler.submit(() -> {
            try {
                recovery();
            } catch (Exception e) {
                log.error("Fatal error occurred: ", e);
            }
        });
    }

    private void recovery() {
        logInfo("Starting recovery polling");
        // poll events until :
        // now
        // + LivePoller.POLL_PERIOD (the live poller will take in charge the live event stream after sleeping the POLL_PERIOD).
        toDate = Instant.now().plus(LivePoller.POLL_INTERVAL);

        PaaSDeploymentLog lastEvent = null;
        try {
            logDebug("Searching for last event stored in the system");
            lastEvent = alienMonitorDao.buildQuery(PaaSDeploymentLog.class).prepareSearch().setFieldSort("timestamp", true).find();
            if (log.isDebugEnabled()) {
                logDebug("The last event found in the system date from {}", (lastEvent == null) ? "(no event found)" : DateUtil
                        .logDate(lastEvent.getTimestamp()));
            }
        } catch (Exception e) {
            log.warn("Not able to find last known event timestamp ({})", e.getMessage());
        }

        fromDate = null;
        if (lastEvent == null) {
            fromDate = Instant.now().minus(MAX_HISTORY_PERIOD);
        } else {
            fromDate = lastEvent.getTimestamp().toInstant().minus(RECOVERY_INTERVAL);
            // we have a lastEvent, we build a recovery cache by requesting ES
            prepareRecoveryCache(loadLocalLogs(Date.from(fromDate), Date.from(toDate)));
        }

        int occurenceCount = 0;
        // we loop polling the same period until the LivePoller starts polling.
        // by this way, we are sure we don't miss events that occur during startup
        while (!livepollerStarted.get()) {
            occurenceCount++;
            logInfo("Will poll historical epoch {} -> {}, occurence {}", DateUtil.logDate(fromDate), DateUtil.logDate(toDate), occurenceCount);
            doRecover();
            try {
                // just to avoid high frequency looping
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // nothing to do here
            }
        }
    }

    private void doRecover() {
        try {
            long _startTime = System.currentTimeMillis();
            PollResult pollResult = pollEpoch(fromDate, toDate, events -> {
                List<Event> filteredEvents = Lists.newLinkedList();
                events.stream().forEach(event -> {
                    // filter the event using the recovery cache to avoid event duplication
                    String key = buildRecoveryDeduplicatingKey(event);
                    if (recoveryCache.contains(key)) {
                        if (log.isTraceEnabled()) {
                            logTrace("The event with timestamp {} and deploymentId {} is ignored since it exists in recovery cache with key : {}", event.getTimestamp(), event.getContext().getDeploymentId(), key);
                        }
                    } else {
                        filteredEvents.add(event);
                    }
                });
                if (log.isDebugEnabled()) {
                    logDebug("After filtering using recovery cache, event size is now {}/{}", filteredEvents.size(), events.size());
                }
                return filteredEvents;
            });
            String duration = DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - _startTime);
            logInfo("Recovery polling terminated: {} events polled in {} batches, {} dispatched (took {})", pollResult.eventPolledCount, pollResult.bactchCount, pollResult.eventDispatchedCount, duration);
        } catch (PollingException e) {
            logError("Giving up polling after several retries", e);
            return;
        }
    }

    /**
     * Get the locally stored logs in order to de-duplicate recovered events.
     * @param from
     * @param to
     * @return
     */
    private PaaSDeploymentLog[] loadLocalLogs(Date from, Date to) {
        if (log.isDebugEnabled()) {
            logDebug("Query elasticseach for PaaSDeploymentLog between {} and {}", DateUtil.logDate(from), DateUtil.logDate(to));
        }
        RangeFilterBuilder dateRangeBuilder = FilterBuilders.rangeFilter("timestamp");
        dateRangeBuilder = dateRangeBuilder.from(from).to(to);

        Map<String, String[]> filters = Maps.newHashMap();
        GetMultipleDataResult<PaaSDeploymentLog> result = alienMonitorDao.buildQuery(PaaSDeploymentLog.class).prepareSearch()
                .setFilters(filters, dateRangeBuilder)
                .search(0, Integer.MAX_VALUE);

        if (log.isDebugEnabled()) {
            logDebug("{} local logs fetch from database ({})", result.getTotalResults(), result.getData().length);
        }
        return result.getData();
    }

    /**
     * Using the logs retrieved from local DB, prepare the recovery cache to de-duplicate events polled by recovery.
     */
    private void prepareRecoveryCache(PaaSDeploymentLog[] logs) {
        Arrays.stream(logs).forEach(paaSDeploymentLog -> {
            String key = buildRecoveryDeduplicatingKey(paaSDeploymentLog);
            if (log.isTraceEnabled()) {
                logTrace("The local paaSDeploymentLog with timestamp {} and deploymentPaaSId {} is added to the cache with key: {}", paaSDeploymentLog.getTimestamp().getTime(), paaSDeploymentLog.getDeploymentPaaSId(), key);
            }
            recoveryCache.add(key);
        });
        logDebug("Recovery cache initialized with {} entries", recoveryCache.size());
    }

    private String buildRecoveryDeduplicatingKey(PaaSDeploymentLog log) {
        return buildRecoveryDeduplicatingKey(log.getTimestamp(), log.getDeploymentPaaSId(), log.getContent());
    }

    private String buildRecoveryDeduplicatingKey(Event event) {
        Date timeStamp = DatatypeConverter.parseDateTime(event.getTimestamp()).getTime();
        return buildRecoveryDeduplicatingKey(timeStamp, event.getContext().getDeploymentId(), event.getMessage().getText());
    }

    /**
     * FIXME: since we don't store the cfy event id in local DB we can't deduplicate on it
     * FIXME: we use a combination of timestamp & deploymentId to duplicate
     */
    private String buildRecoveryDeduplicatingKey(Date timestamp, String deploymentId, String content) {
        StringBuilder sb = new StringBuilder(deploymentId);
        sb.append("#").append(timestamp.getTime());
        sb.append("#").append(content.hashCode());
        return sb.toString();
    }

}
