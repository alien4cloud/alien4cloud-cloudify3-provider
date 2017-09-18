package alien4cloud.paas.cloudify3.error;

/**
 * Exception in case of a failure during property mapping.
 */
public class PropertyValueMappingException extends RuntimeException {
    /**
     * New exception instance from a custom message.
     * 
     * @param message
     *            The message.
     */
    public PropertyValueMappingException(String message) {
        super(message);
    }
}
