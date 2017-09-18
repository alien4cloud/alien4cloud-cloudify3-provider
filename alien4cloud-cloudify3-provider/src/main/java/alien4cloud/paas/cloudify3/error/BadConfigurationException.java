package alien4cloud.paas.cloudify3.error;

import alien4cloud.paas.exception.PaaSTechnicalException;

/**
 * The PaaS is not well configured
 *
 * @author Minh Khang VU
 */
public class BadConfigurationException extends PaaSTechnicalException {

    public BadConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadConfigurationException(String message) {
        super(message);
    }
}
