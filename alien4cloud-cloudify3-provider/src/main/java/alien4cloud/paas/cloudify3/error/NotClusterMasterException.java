package alien4cloud.paas.cloudify3.error;

import java.io.IOException;

/**
 * Exception to be thrown when the cloudify instance is not a master node.
 */
public class NotClusterMasterException extends IOException {
    public NotClusterMasterException(String message, Throwable cause) {
        super(message, cause);
    }
}
