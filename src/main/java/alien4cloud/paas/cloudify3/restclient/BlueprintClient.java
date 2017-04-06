package alien4cloud.paas.cloudify3.restclient;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.inject.Inject;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.google.common.util.concurrent.ListenableFuture;

import alien4cloud.paas.cloudify3.configuration.CloudConfigurationHolder;
import alien4cloud.paas.cloudify3.model.Blueprint;
import alien4cloud.paas.cloudify3.util.FutureUtil;
import alien4cloud.utils.FileUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class BlueprintClient extends AbstractClient {
    public static final String BLUEPRINT_PATH = "/blueprints";

    @Inject
    private CloudConfigurationHolder configurationHolder;

    @Override
    protected String getPath() {
        return BLUEPRINT_PATH;
    }

    public ListenableFuture<Blueprint[]> asyncList() {
        if (log.isDebugEnabled()) {
            log.debug("List blueprint");
        }
        return FutureUtil.unwrapRestResponse(getForEntity(getBaseUrl(getManagerUrl()), Blueprint[].class));
    }

    @SneakyThrows
    public Blueprint[] list() {
        return asyncList().get();
    }

    @SneakyThrows
    public ListenableFuture<Blueprint> asyncCreate(String id, String path) {
        if (log.isDebugEnabled()) {
            log.debug("Create blueprint {} with path {}", id, path);
        }
        Path sourcePath = Paths.get(path);
        String sourceName = sourcePath.getFileName().toString();
        File destination = File.createTempFile(id, ".tar.gz");
        // Tar gz the parent directory
        FileUtil.tar(sourcePath.getParent(), destination.toPath(), true, false);
        if (log.isDebugEnabled()) {
            log.debug("Created temporary archive file at {} for blueprint {}", destination, id);
        }
        try {
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
            return FutureUtil.unwrapRestResponse(exchange(getSuffixedUrl(getManagerUrl(), "/{id}", "application_file_name"), HttpMethod.PUT,
                    new HttpEntity<>(Files.readAllBytes(destination.toPath()), headers), Blueprint.class, id, sourceName));
        } finally {
            destination.delete();
        }
    }

    @SneakyThrows
    public Blueprint create(String id, String path) {
        return asyncCreate(id, path).get();
    }

    public ListenableFuture<Blueprint> asyncRead(String id) {
        if (log.isDebugEnabled()) {
            log.debug("Read blueprint {}", id);
        }
        return FutureUtil.unwrapRestResponse(getForEntity(getSuffixedUrl(getManagerUrl(), "/{id}"), Blueprint.class, id));
    }

    @SneakyThrows
    public Blueprint read(String id) {
        return asyncRead(id).get();
    }

    public ListenableFuture<?> asyncDelete(String id) {
        if (log.isDebugEnabled()) {
            log.debug("Delete blueprint {}", id);
        }
        return FutureUtil.toGuavaFuture(delete(getSuffixedUrl(getManagerUrl(), "/{id}"), id));
    }

    @SneakyThrows
    public void delete(String id) {
        asyncDelete(id).get();
    }

    private String getManagerUrl() {
        return configurationHolder.getConfiguration().getUrl();
    }
}
