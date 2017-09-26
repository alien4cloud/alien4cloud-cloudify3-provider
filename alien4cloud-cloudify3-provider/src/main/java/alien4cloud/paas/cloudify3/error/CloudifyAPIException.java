package alien4cloud.paas.cloudify3.error;

import java.io.IOException;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * Exception to wrap cloudify api call errors
 */
@Getter
public class CloudifyAPIException extends IOException {

    private HttpStatus statusCode;

    public CloudifyAPIException(HttpStatus statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }
}
