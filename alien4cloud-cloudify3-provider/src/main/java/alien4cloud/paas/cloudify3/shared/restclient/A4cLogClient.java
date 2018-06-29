package alien4cloud.paas.cloudify3.shared.restclient;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.AsyncRestTemplate;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.paas.cloudify3.shared.model.LogBatch;
import alien4cloud.paas.cloudify3.shared.model.LogClientRegistration;
import alien4cloud.paas.cloudify3.shared.model.LogRegistrationResponse;
import alien4cloud.paas.cloudify3.util.FutureUtil;
import lombok.SneakyThrows;

/**
 * Client for the alien4cloud log server.
 */
public class A4cLogClient {
    private static final String REGISTRATION_PATH = "/api/v1/registration";
    private static final String LOG_PATH = "/api/v1/logs";
    private static final String GET_LOG_PATH = LOG_PATH + "/{registrationId}";
    private static final String ACK_LOG_PATH = LOG_PATH + "/{registrationId}/ack/{batchId}";

    private AsyncRestTemplate restTemplate;
    /** Url of the log server for which this instance is a client. */
    private String logServerUrl;
    private String registrationId;

    private String getLogUrl;
    private String ackLogUrl;

    public A4cLogClient(AsyncRestTemplate restTemplate, IGenericSearchDAO cfyEsDao, String logServerUrl) {
        this.restTemplate = restTemplate;
        this.logServerUrl = logServerUrl;
        this.getLogUrl = logServerUrl + GET_LOG_PATH;
        this.ackLogUrl = logServerUrl + ACK_LOG_PATH;

        LogClientRegistration registration = cfyEsDao.findById(LogClientRegistration.class, logServerUrl);
        if (registration == null) {
            registration = register();
            cfyEsDao.save(registration);
        }
        this.registrationId = registration.getRegistrationId();
    }

    // perform sync registration
    @SneakyThrows
    private LogClientRegistration register() {
        this.registrationId = restTemplate
                .exchange(logServerUrl + REGISTRATION_PATH, HttpMethod.POST, createPostHttpEntity(Maps.newHashMap()), LogRegistrationResponse.class).get()
                .getBody().getId();
        return new LogClientRegistration(logServerUrl, this.registrationId);
    }

    public ListenableFuture<LogBatch> asyncGet() {
        // Query the log server for events
        return FutureUtil.unwrapRestResponse(restTemplate.exchange(getLogUrl, HttpMethod.GET, createHttpEntity(), LogBatch.class, registrationId));
    }

    public ListenableFuture<Object> asyncGetCorruptedLog() {
       return FutureUtil.unwrapRestResponse(restTemplate.getForEntity(getLogUrl, Object.class, registrationId));
    }

    public ListenableFuture<Void> asyncAck(long batchId) {
        // Query the log server for events
        return FutureUtil.unwrapRestResponse(restTemplate.exchange(ackLogUrl, HttpMethod.DELETE, createHttpEntity(), Void.class, registrationId, batchId));
    }

    private HttpEntity<?> createPostHttpEntity(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<?> createHttpEntity() {
        return new HttpEntity<>(new HttpHeaders());
    }
}