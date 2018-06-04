package alien4cloud.paas.cloudify3.eventpolling;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.paas.cloudify3.model.Event;
import alien4cloud.paas.cloudify3.shared.model.CloudifyEvent;
import alien4cloud.paas.cloudify3.util.DateUtil;
import alien4cloud.paas.model.PaaSDeploymentLog;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DurationFormatUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Instant;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This poller is responsible of polling events that have been missed when the system was down.
 */
@Slf4j
@Setter
public class RecoveryPoller extends AbstractPoller {

    /**
     * This is the max recovery period we will use if no event is found in the system.
     */
    // FIXME : why can't I set this in months ?
    private static final Period MAX_HISTORY_PERIOD = Period.ofDays(1);

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
     * Poll historical events since the last known event date (stored in the system A4C) to now.
     * By this way all events that have been missed while the poller was offline will be polled.
     */
    @Override
    public void start() {
        scheduler.submit(() -> {
            logInfo("Starting recovery polling");
            // poll events until now (the live poller will take in charge the live event stream).
            Instant toDate = Instant.now();

            PaaSDeploymentLog lastEvent = null;
            try {
                logDebug("Searching for last event stored in the system");
                lastEvent = alienMonitorDao.buildQuery(PaaSDeploymentLog.class).prepareSearch().setFieldSort("timestamp", true).find();
                if (log.isDebugEnabled()) {
                    logDebug("The last event found in the system date from {}", (lastEvent == null) ? "(no event found)" : DateUtil.logDate(lastEvent.getTimestamp()));
                }
                if (lastEvent != null) {
                    // we don't want this event to be re-polled
                    getEventCache().blackList(lastEvent.getId());
                }
            } catch (Exception e) {
                log.warn("Not able to find last known event timestamp ({})", e.getMessage());
            }
            final Instant fromDate = (lastEvent == null) ? Instant.now().minus(MAX_HISTORY_PERIOD) : lastEvent.getTimestamp().toInstant();
            logInfo("Will poll historical epoch {} -> {}", DateUtil.logDate(fromDate), DateUtil.logDate(toDate));
            try {
                pollEpoch(fromDate, toDate);
                String duration = DurationFormatUtils.formatDurationHMS(toDate.until(Instant.now(), ChronoUnit.MILLIS));
                logInfo("Recovery polling terminated ^^ took {}", duration);
            } catch (ExecutionException | InterruptedException e) {
                // TODO: handle correctly this exception
                log.error("TODO: handle correctly this exception", e);
            }
        });
    }

}
