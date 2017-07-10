package alien4cloud.paas.cloudify3.shared.restclient;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestClientException;

import alien4cloud.paas.cloudify3.error.NotClusterMasterException;
import alien4cloud.paas.cloudify3.shared.restclient.auth.AuthenticationInterceptor;
import lombok.experimental.Builder;

public class ApiHttpClientTest {

    private int numberOfCall;

    @Before
    public void clearCounter() {
        numberOfCall = 0;
    }

    @Test
    public void when_ha_is_disable_then_no_retry() throws Exception {
        ApiHttpClient apiHttpClient = ApiHttpClientBuilder.builder().highAvailabilityConfig(false).failOverRetry(10).failOverDelay(0).build().client();
        ListenableFuture<Object> future = apiHttpClient.retryHttpClient(apiHttpClient.new RetryCounter(), this::alwaysFails);

        try {
            future.get();
        } catch (Exception e) {
            // catch all
        }

        Assertions.assertThat(numberOfCall).isEqualTo(1);
    }

    @Test
    public void when_no_issue_no_retry_should_be_made() throws Exception {
        ApiHttpClient apiHttpClient = ApiHttpClientBuilder.builder().highAvailabilityConfig(true).failOverRetry(10).failOverDelay(0).build().client();
        apiHttpClient.retryHttpClient(apiHttpClient.new RetryCounter(), this::alwaysOK).get();

        Assertions.assertThat(numberOfCall).isEqualTo(1);
    }

    @Ignore
    @Test(timeout = 500)
    // Sadly, this is not true, if the connection failed then the sleep will be done in the calling thread
    public void async_should_not_block() throws Exception {
        ApiHttpClient apiHttpClient = ApiHttpClientBuilder.builder().highAvailabilityConfig(true).failOverRetry(10).failOverDelay(100).build().client();
        apiHttpClient.retryHttpClient(apiHttpClient.new RetryCounter(), this::alwaysFails);
    }

    @Test
    public void when_failure_in_ha_mode_with_retry_to_0_then_no_sleep_and_all_urls_should_be_called_once() throws Exception {
        ApiHttpClient apiHttpClient = ApiHttpClientBuilder.builder().highAvailabilityConfig(true).failOverRetry(0).failOverDelay(1000).build().client();
        ListenableFuture<Object> future = apiHttpClient.retryHttpClient(apiHttpClient.new RetryCounter(), this::alwaysFails);

        try {
            future.get();
        } catch (Exception e) {
            // catch all
        }

        Assertions.assertThat(numberOfCall).isEqualTo(2);
    }

    @Test
    public void when_ha_is_enable_then_follow_retry_config() throws Exception {
        ApiHttpClient apiHttpClient = ApiHttpClientBuilder.builder().highAvailabilityConfig(true).failOverRetry(10).failOverDelay(0).build().client();
        ListenableFuture<Object> future = apiHttpClient.retryHttpClient(apiHttpClient.new RetryCounter(), this::alwaysFails);

        try {
            future.get();
        } catch (Exception e) {
            // catch all
        }

        Assertions.assertThat(numberOfCall).isEqualTo(2 * 10 + 2);
    }

    private ListenableFuture<Object> alwaysFails() throws RestClientException {
        numberOfCall++;
        SettableListenableFuture<Object> listenableFuture = new SettableListenableFuture<Object>();
        listenableFuture.setException(new NotClusterMasterException("", null));
        return listenableFuture;
    }

    private ListenableFuture<Object> alwaysOK() throws RestClientException {
        numberOfCall++;
        SettableListenableFuture<Object> listenableFuture = new SettableListenableFuture<Object>();
        listenableFuture.set("ok");
        return listenableFuture;
    }

    @Builder
    private static class ApiHttpClientBuilder {
        private boolean highAvailabilityConfig;
        private Integer failOverRetry;
        private Integer failOverDelay;

        public ApiHttpClient client() {
            List<String> urls;
            if (highAvailabilityConfig) {
                urls = Arrays.asList("http://localhost:2000", "http://localhost:3000");
            } else {
                urls = Arrays.asList("http://localhost:2000");
            }
            return new ApiHttpClient(Mockito.mock(AsyncRestTemplate.class), urls, Mockito.mock(AuthenticationInterceptor.class), failOverRetry, failOverDelay);
        }
    }

}