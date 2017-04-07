package alien4cloud.paas.cloudify3.model;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CloudifyLifeCycle {

    private CloudifyLifeCycle() {
    }

    public static final String CREATE = "cloudify.interfaces.lifecycle.create";

    public static final String CONFIGURE = "cloudify.interfaces.lifecycle.configure";

    public static final String START = "cloudify.interfaces.lifecycle.start";

    public static final String STOP = "cloudify.interfaces.lifecycle.stop";

    public static final String DELETE = "cloudify.interfaces.lifecycle.delete";

    public static String getSucceededInstanceState(String lifeCycle) {
        if (lifeCycle == null) {
            return null;
        }
        switch (lifeCycle) {
        case CREATE:
            return NodeInstanceStatus.CREATED;
        case CONFIGURE:
            return NodeInstanceStatus.CONFIGURED;
        case START:
            return NodeInstanceStatus.STARTED;
        case STOP:
            return NodeInstanceStatus.STOPPED;
        case DELETE:
            return NodeInstanceStatus.DELETED;
        default:
            log.debug("Unknown lifeCycle: <{}>", lifeCycle);
            return null;
        }
    }
}
