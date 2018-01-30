package alien4cloud.paas.cloudify3.error;

/**
 * It's an exception for managing the deployment runtime failure
 */
public class DeploymentRuntimeException extends RuntimeException {

    public DeploymentRuntimeException(String message) {
        super(message);
    }
}
