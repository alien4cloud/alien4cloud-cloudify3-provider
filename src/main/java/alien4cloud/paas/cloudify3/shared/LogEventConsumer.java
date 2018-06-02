package alien4cloud.paas.cloudify3.shared;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.paas.cloudify3.model.Event;
import alien4cloud.paas.cloudify3.shared.model.CloudifyEvent;
import alien4cloud.paas.model.PaaSDeploymentLog;
import alien4cloud.paas.model.PaaSDeploymentLogLevel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Consumes a log event to store it in alien4cloud.
 */
@Slf4j
@Service
public class LogEventConsumer implements IEventConsumer {
    public static final String LOG_EVENT_CONSUMER_ID = "ALIEN_LOG_EVENT_CONSUMER";
    @Resource(name = "alien-monitor-es-dao")
    private IGenericSearchDAO alienMonitorDao;

    @Override
    public String getAlienDeploymentId(Event event) {
        return null;
    }

    @Override
    public boolean receiveUnknownEvents() {
        return true;
    }

    private Date lastAckDate = null;

    @Override
    public Date lastAcknowledgedDate() {
        if (lastAckDate == null) {
            // sort by filed date DESC the first one is the one with the latest date
            PaaSDeploymentLog lastEvent = alienMonitorDao.buildQuery(PaaSDeploymentLog.class).prepareSearch().setFieldSort("timestamp", true).find();
            if (lastEvent != null) {
                log.info("Recovering events from the last in elasticsearch {} of type {}", lastEvent.getTimestamp(), lastEvent.getClass().getName());
                lastAckDate = lastEvent.getTimestamp();
            }
        }
        return lastAckDate;
    }

    @Override
    public void accept(CloudifyEvent[] events) {
        PaaSDeploymentLog[] logs = toLogs(events);

        if (logs.length > 0) {
            alienMonitorDao.save(logs);
            for (PaaSDeploymentLog log : logs) {
                if (lastAckDate == null || log.getTimestamp().compareTo(lastAckDate) > 0) {
                    lastAckDate = log.getTimestamp();
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("Received {} log entries from orchestrator", logs.length);
            }
        }
    }

    private PaaSDeploymentLog[] toLogs(CloudifyEvent[] events) {
        if (events != null) {
            List<PaaSDeploymentLog> logs = new ArrayList<>(events.length);
            for (CloudifyEvent event : events) {
                PaaSDeploymentLog log = toLog(event);
                if (log != null) {
                    logs.add(log);
                }
            }
            return logs.toArray(new PaaSDeploymentLog[logs.size()]);
        } else {
            return new PaaSDeploymentLog[0];
        }
    }

    private PaaSDeploymentLog toLog(CloudifyEvent cloudifyEvent) {
        if (cloudifyEvent.getAlienDeploymentId() == null) {
            return null;
        }
        Event event = cloudifyEvent.getEvent();
        PaaSDeploymentLog deploymentLog = new PaaSDeploymentLog();
        deploymentLog.setContent(event.getMessage().getText());
        deploymentLog.setDeploymentPaaSId(event.getContext().getDeploymentId());
        deploymentLog.setDeploymentId(cloudifyEvent.getAlienDeploymentId());
        deploymentLog.setExecutionId(event.getContext().getExecutionId());
        deploymentLog.setInstanceId(event.getContext().getNodeId());
        deploymentLog.setNodeId(event.getContext().getNodeName());
        PaaSDeploymentLogLevel level = PaaSDeploymentLogLevel.fromLevel(event.getLevel());
        if (level == null) {
            level = PaaSDeploymentLogLevel.INFO;
        }
        deploymentLog.setLevel(level);
        String cloudifyOperationName = event.getContext().getOperation();
        if (StringUtils.isNotBlank(cloudifyOperationName)) {
            int indexOfPoint = cloudifyOperationName.lastIndexOf('.');
            if (indexOfPoint >= 0) {
                String cloudifyInterfaceName = cloudifyOperationName.substring(0, indexOfPoint);
                String cloudifyOperationShortName = cloudifyOperationName.substring(indexOfPoint + 1);
                if ("cloudify.interfaces.lifecycle".equals(cloudifyInterfaceName)) {
                    deploymentLog.setInterfaceName("Standard");
                } else {
                    deploymentLog.setInterfaceName(cloudifyInterfaceName);
                }
                deploymentLog.setOperationName(cloudifyOperationShortName);
            } else {
                deploymentLog.setOperationName(cloudifyOperationName);
            }
        }
        deploymentLog.setTimestamp(cloudifyEvent.getTimestamp().getTime());
        deploymentLog.setType(event.getEventType());
        deploymentLog.setWorkflowId(event.getContext().getWorkflowId());
        return deploymentLog;
    }
}