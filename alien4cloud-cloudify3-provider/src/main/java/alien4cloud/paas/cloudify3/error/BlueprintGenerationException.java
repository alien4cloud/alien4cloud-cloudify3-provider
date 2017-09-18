package alien4cloud.paas.cloudify3.error;

/**
 * Exception to be thrown in case of an error during blueprint generation. Message will be sent back to user logs.
 */
public class BlueprintGenerationException extends RuntimeException {
    public BlueprintGenerationException(String message) {
        super(message);
    }
}
