package alien4cloud.paas.cloudify3.restclient;

import alien4cloud.paas.cloudify3.configuration.CloudConfigurationHolder;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import org.springframework.stereotype.Component;

import alien4cloud.paas.cloudify3.model.Version;
import alien4cloud.paas.cloudify3.util.FutureUtil;

import com.google.common.util.concurrent.ListenableFuture;

import javax.inject.Inject;

@Component
public class VersionClient extends AbstractClient {
    public static final String VERSION_PATH = "/version";

    @Inject
    @Getter
    @Setter
    private CloudConfigurationHolder configurationHolder;

    @Override
    protected String getPath() {
        return VERSION_PATH;
    }

    public ListenableFuture<Version> asyncRead() {
        return FutureUtil.unwrapRestResponse(getForEntity(getBaseUrl(getManagerUrl()), Version.class));
    }

    @SneakyThrows
    public Version read() {
        return asyncRead().get();
    }

    private String getManagerUrl() {
        return configurationHolder.getConfiguration().getUrl();
    }
}
