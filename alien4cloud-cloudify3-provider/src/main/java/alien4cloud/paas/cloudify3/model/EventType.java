package alien4cloud.paas.cloudify3.model;

import java.util.Collection;

import com.google.common.collect.Lists;

public class EventType {
    private EventType() {
    }

    // WORKFLOW_STAGE and WORKFLOW_NODE_EVENT are not used in Alien
    public static final String WORKFLOW_STAGE = "workflow_stage";
    public static final String WORKFLOW_NODE_EVENT = "workflow_node_event";

    // WORKFLOW_STARTED, WORKFLOW_FAILED and WORKFLOW_SUCCEEDED are used to determine DeploymentStatus
    public static final String WORKFLOW_STARTED = "workflow_started";
    public static final String WORKFLOW_SUCCEEDED = "workflow_succeeded";
    public static final String WORKFLOW_FAILED = "workflow_failed";

    // SENDING_TASK and TASK_STARTED are not used in Alien
    public static final String SENDING_TASK = "sending_task";
    public static final String TASK_STARTED = "task_started";

    // TASK_SUCCEEDED and TASK_FAILED are used for instance state information
    public static final String TASK_SUCCEEDED = "task_succeeded";
    public static final String TASK_FAILED = "task_failed";

    public static final String A4C_PERSISTENT_EVENT = "a4c_persistent_event";
    public static final String A4C_WORKFLOW_EVENT = "a4c_workflow_event";
    public static final String A4C_WORKFLOW_STARTED = "a4c_workflow_started";

    public static final Collection<String> ALL = Lists.newArrayList(WORKFLOW_STARTED, WORKFLOW_SUCCEEDED, WORKFLOW_FAILED, TASK_SUCCEEDED);
}