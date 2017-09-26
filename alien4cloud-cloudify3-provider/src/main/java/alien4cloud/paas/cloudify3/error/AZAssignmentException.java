package alien4cloud.paas.cloudify3.error;

/**
 * Exception to be thrown in case there is an error while enforcing an HA policy through availability zones.
 */
public class AZAssignmentException extends RuntimeException {
    public AZAssignmentException(String message) {
        super(message);
    }
}
