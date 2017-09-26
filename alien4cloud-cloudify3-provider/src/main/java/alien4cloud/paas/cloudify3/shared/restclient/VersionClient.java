package alien4cloud.paas.cloudify3.shared.restclient;

import lombok.SneakyThrows;

import alien4cloud.paas.cloudify3.model.Version;
import alien4cloud.paas.cloudify3.util.FutureUtil;

import com.google.common.util.concurrent.ListenableFuture;

public class VersionClient {
    private static final String VERSION_PATH = "/api/v3/version";
    private final ApiHttpClient client;

    public VersionClient(ApiHttpClient apiHttpClient) {
        this.client = apiHttpClient;
    }

    public ListenableFuture<Version> asyncRead() {
        return FutureUtil.unwrapRestResponse(client.getForEntity(client.buildRequestUrl(VERSION_PATH), Version.class));
    }

    @SneakyThrows
    public Version read() {
        return asyncRead().get();
    }
}
