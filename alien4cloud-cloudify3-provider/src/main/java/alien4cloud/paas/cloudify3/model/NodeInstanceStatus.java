package alien4cloud.paas.cloudify3.model;

import alien4cloud.paas.model.InstanceStatus;

public class NodeInstanceStatus {

    public static final String UNINITIALIZED = "uninitialized";

    public static final String CREATING = "creating";

    public static final String CREATED = "created";

    public static final String CONFIGURING = "configuring";

    public static final String CONFIGURED = "configured";

    public static final String STARTING = "starting";

    public static final String STARTED = "started";

    public static final String STOPPING = "stopping";

    public static final String STOPPED = "stopped";

    public static final String DELETING = "deleting";

    public static final String DELETED = "deleted";

    public static InstanceStatus getInstanceStatusFromState(String state) {
        switch (state) {
            case NodeInstanceStatus.STARTED:
                return InstanceStatus.SUCCESS;
            case NodeInstanceStatus.UNINITIALIZED:
            case NodeInstanceStatus.STOPPING:
            case NodeInstanceStatus.STOPPED:
            case NodeInstanceStatus.STARTING:
            case NodeInstanceStatus.CONFIGURING:
            case NodeInstanceStatus.CONFIGURED:
            case NodeInstanceStatus.CREATING:
            case NodeInstanceStatus.CREATED:
            case NodeInstanceStatus.DELETING:
                return InstanceStatus.PROCESSING;
            case NodeInstanceStatus.DELETED:
                return null;
            default:
                return InstanceStatus.FAILURE;
        }
    }
}
