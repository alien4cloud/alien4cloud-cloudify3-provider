package alien4cloud.paas.cloudify3.shared.restclient;

import java.net.ConnectException;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import alien4cloud.paas.cloudify3.error.NotClusterMasterException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import alien4cloud.paas.cloudify3.shared.restclient.auth.AuthenticationInterceptor;
import lombok.extern.slf4j.Slf4j;

/**
 * Client for the rest api.
 */
@Slf4j
public final class ApiHttpClient {
    private final int maxRetry;
    private final long retrySleep;
    private AsyncRestTemplate restTemplate;
    /** The authentication manager to use. */
    private AuthenticationInterceptor authenticationInterceptor;
    /** Url of the current master manager. */
    private String currentManagerUrl;
    private List<String> managerUrls;

    /**
     * Create a new api client for the given urls.
     *
     * @param restTemplate The spring rest template to leverage for requests.
     * @param managerUrls The urls of the manager(s) part of the cluster to connect to.
     * @param authenticationInterceptor The authentication interceptor to use to add the authentication headers.
     */
    public ApiHttpClient(AsyncRestTemplate restTemplate, List<String> managerUrls, AuthenticationInterceptor authenticationInterceptor, Integer failOverRetry,
            Integer failOverDelay) {
        log.info("Creating api client for managers {}", managerUrls);
        this.restTemplate = restTemplate;
        this.managerUrls = managerUrls;
        this.currentManagerUrl = managerUrls.get(0);
        this.authenticationInterceptor = authenticationInterceptor;

        this.maxRetry = failOverRetry == null ? 60 : failOverRetry;
        this.retrySleep = failOverDelay == null ? 1000 : failOverDelay;
    }

    public <T> ListenableFuture<ResponseEntity<T>> getForEntity(final RequestUrlBuilder requestUrlBuilder, final Class<T> responseType,
            final Object... uriVariables) {
        ListenableFuture<ResponseEntity<T>> future = retryHttpClient(new RetryCounter(),
                () -> restTemplate.exchange(requestUrlBuilder.getUrl(), HttpMethod.GET, createHttpEntity(), responseType, uriVariables));
        return future;
    }

    public ListenableFuture<?> delete(final RequestUrlBuilder requestUrlBuilder, final Object... urlVariables) {
        ListenableFuture<?> future = retryHttpClient(new RetryCounter(),
                () -> restTemplate.exchange(requestUrlBuilder.getUrl(), HttpMethod.DELETE, createHttpEntity(), (Class<?>) null, urlVariables));
        return future;
    }

    public <T> ListenableFuture<ResponseEntity<T>> exchange(final RequestUrlBuilder requestUrlBuilder, HttpMethod method, HttpEntity<?> requestEntity,
            Class<T> responseType, Object... uriVariables) {
        final HttpEntity<?> securedRequestEntity;
        if (authenticationInterceptor != null) {
            securedRequestEntity = authenticationInterceptor.addAuthenticationHeader(requestEntity);
        } else {
            securedRequestEntity = requestEntity;
        }
        ListenableFuture<ResponseEntity<T>> future = retryHttpClient(new RetryCounter(),
                () -> restTemplate.exchange(requestUrlBuilder.getUrl(), method, securedRequestEntity, responseType, uriVariables));
        return future;
    }

    public <T> HttpEntity<T> createHttpEntity(T body, HttpHeaders httpHeaders) {
        if (authenticationInterceptor != null) {
            return authenticationInterceptor.addAuthenticationHeader(new HttpEntity<>(body, httpHeaders));
        }
        return new HttpEntity<>(body, httpHeaders);
    }

    /**
     * Get request url
     * 
     * @param parameterNames all parameters' name
     * @return the url
     */
    public RequestUrlBuilder buildRequestUrl(String requestPath, String... parameterNames) {
        return new RequestUrlBuilder(requestPath, parameterNames);
    }

    private class RequestUrlBuilder {
        private String requestPath;
        private String[] parameterNames;

        private RequestUrlBuilder(String requestPath, String... parameterNames) {
            this.requestPath = requestPath;
            this.parameterNames = parameterNames;
        }

        private String getUrl() {
            String requestUrl = currentManagerUrl + requestPath;
            if (parameterNames != null && parameterNames.length > 0) {
                UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(requestUrl);
                for (String parameterName : parameterNames) {
                    uriComponentsBuilder.queryParam(parameterName, "{" + parameterName + "}");
                }
                return uriComponentsBuilder.build().toUriString();
            } else {
                return requestUrl;
            }
        }
    }

    private HttpEntity<?> createHttpEntity() {
        return createHttpEntity(new HttpHeaders());
    }

    private HttpEntity<?> createHttpEntity(HttpHeaders httpHeaders) {
        HttpEntity<?> httpEntity = new HttpEntity(httpHeaders);
        if (authenticationInterceptor == null) {
            return httpEntity;
        }
        return authenticationInterceptor.addAuthenticationHeader(httpEntity);
    }

    /**
     * Retry mechanism will allow the function to fail, and will retry with all provided managerUrls.
     *
     * If no url works then it will sleep and then try again all urls as long as max retry has not been reached.
     *
     * @param retryCounter A retry counter to stop the recursive retry.
     * @param function The function to execute.
     * @param <T> The type the function returns in it's future.
     * @return The listener that takes retry in account.
     */
    private <T> ListenableFuture<T> retryHttpClient(final RetryCounter retryCounter, Supplier<ListenableFuture<T>> function) {
        final SettableListenableFuture<T> retryFuture = new SettableListenableFuture<T>();

        function.get().addCallback(t -> {
            retryFuture.set(t);
        }, throwable -> {
            if (throwable instanceof NotClusterMasterException || throwable instanceof ConnectException) {
                retryCounter.increment();
                if (retryCounter.loopedRetry < maxRetry) {
                    // Select next url to try out.
                    this.currentManagerUrl = this.managerUrls.get(retryCounter.modulo());
                    log.warn("Unable to communicate with manager, unit retry {} / all managers retry {} on {}, with url {}", retryCounter.unitRetry,
                            retryCounter.loopedRetry, maxRetry, currentManagerUrl);
                    if (retryCounter.unitRetry % this.managerUrls.size() == 0) {
                        log.info("Sleeping {} milliseconds before next retry.", retrySleep);
                        try {
                            Thread.sleep(retrySleep);
                        } catch (InterruptedException e) {
                            retryFuture.setException(throwable);
                        }
                    }
                    // Execute on the next url in recursive way.
                    ListenableFuture<T> retriedFuture = retryHttpClient(retryCounter, function);
                    retriedFuture.addCallback(t -> retryFuture.set(t), retryThrowable -> retryFuture.setException(retryThrowable));
                } else {
                    retryFuture.setException(throwable);
                }
            } else {
                retryFuture.setException(throwable);
            }
        });

        return retryFuture;
    }

    private class RetryCounter {
        // This counter increment at each attempt.
        private int unitRetry = 0;
        // This counter increment when all url have been retried
        private int loopedRetry = 0;

        public void increment() {
            unitRetry++;
            log.info("Sleeping {} milliseconds before next retry.", retrySleep);
            if (modulo() == 0) {
                loopedRetry++;
            }
        }

        public int modulo() {
            return unitRetry % managerUrls.size();
        }
    }
}