package alien4cloud.paas.cloudify3.restclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestExecution;
import org.springframework.http.client.AsyncClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.io.IOException;
import java.util.UUID;

/**
 * An interceptor that logs requests only in trace mode.
 */
@Slf4j
public class AsyncClientHttpRequestLogger implements AsyncClientHttpRequestInterceptor {

    @Override
    public ListenableFuture<ClientHttpResponse> intercept(HttpRequest request, byte[] body, AsyncClientHttpRequestExecution execution) throws IOException {
        if (!log.isTraceEnabled()) {
            // prevent useless computations if trace are not enabled
            return execution.executeAsync(request, body);
        }
        long requestStartTime = System.currentTimeMillis();
        String requestUUID = UUID.randomUUID().toString();
        log.trace("REST Query [{}] : {} {}", requestUUID, request.getMethod(), request.getURI());
        ListenableFuture<ClientHttpResponse> response = execution.executeAsync(request, body);
        response.addCallback(new ListenableFutureCallback<ClientHttpResponse>() {
            @Override
            public void onFailure(Throwable ex) {
                log.trace("REST Response failed to [{}] : {} {} ({}) (took {} ms)", requestUUID, request.getMethod(), request.getURI(), ex.getMessage(), System.currentTimeMillis() - requestStartTime);
            }

            @Override
            public void onSuccess(ClientHttpResponse result) {
                try {
                    log.trace("REST Response to [{}] : {} {} is {} - {} (took {} ms)", requestUUID, request.getMethod(), request.getURI(), result.getStatusCode(), result.getStatusText(), System.currentTimeMillis() - requestStartTime);
                } catch (IOException e) {
                    // nothing to do here, we are in trace log context !
                }
            }
        });
        return response;
    }

}
