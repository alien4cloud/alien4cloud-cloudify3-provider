package alien4cloud.paas.cloudify3.error;

import java.io.IOException;
import java.util.StringJoiner;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpStatusCodeException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CloudifyResponseErrorHandler extends DefaultResponseErrorHandler {

    private ObjectMapper objectMapper = new ObjectMapper();

    public CloudifyResponseErrorHandler() {
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        try {
            super.handleError(response);
        } catch (HttpStatusCodeException exception) {
            String formattedError = exception.getResponseBodyAsString();

            if (HttpStatus.BAD_REQUEST.equals(exception.getStatusCode()) && formattedError.contains("not_cluster_master")) {
                throw new NotClusterMasterException("Tried to connect to a non master node in a cloudify cluster", exception);
            } else if (HttpStatus.NOT_FOUND.equals(exception.getStatusCode())) {
                if (!formattedError.contains("not_found_error")) {
                    // This is a server unavailable error (not an api error)
                    throw new NotClusterMasterException("Failed to connect to server.", exception);
                }
            } else {
                // Let's log the error
                try {
                    formattedError = objectMapper.writeValueAsString(objectMapper.readTree(formattedError));
                    log.error("Rest error with body \n{}", formattedError);
                } catch (Exception e) {
                    // Ignore if we cannot indent error
                }
            }
            StringJoiner joiner = new StringJoiner("\n");
            joiner.add(exception.getMessage() + " With error body: ").add(formattedError);
            throw new CloudifyAPIException(exception.getStatusCode(), joiner.toString(), exception);
        }
    }
}