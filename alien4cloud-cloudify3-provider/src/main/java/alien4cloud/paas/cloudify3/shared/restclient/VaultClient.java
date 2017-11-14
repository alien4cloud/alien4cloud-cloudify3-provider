package alien4cloud.paas.cloudify3.shared.restclient;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;

import alien4cloud.paas.cloudify3.model.SecretResponse;
import alien4cloud.paas.cloudify3.util.FutureUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VaultClient {

    private final ApiHttpClient client;

    public VaultClient(ApiHttpClient apiHttpClient) {
        this.client = apiHttpClient;
    }

    @SneakyThrows
    public ListenableFuture<SecretResponse> getSecret(String secretPath) {
        return FutureUtil.unwrapRestResponse(client.getForEntity(client.buildRequestUrl(buildUrl(secretPath)), SecretResponse.class));
    }

    @SneakyThrows
    public ListenableFuture<SecretResponse> putSecret(String secretPath, String secretValue) {
        Map<String, Object> request = Maps.newHashMap();
        request.put("value", secretValue);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return FutureUtil.unwrapRestResponse(
                client.exchange(client.buildRequestUrl(buildUrl(secretPath)), HttpMethod.PUT, client.createHttpEntity(request, headers), SecretResponse.class));
    }

    @SneakyThrows
    public ListenableFuture<?> deleteSecret(String secretPath) {
        return FutureUtil.toGuavaFuture(client.delete(client.buildRequestUrl(buildUrl(secretPath))));
    }

    private String buildUrl(String secretPath) {
        return "/api/v3/secrets/" + secretPath;
    }

}
