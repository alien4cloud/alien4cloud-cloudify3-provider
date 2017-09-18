package alien4cloud.paas.cloudify3.shared.restclient;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.google.common.util.concurrent.ListenableFuture;

import alien4cloud.paas.cloudify3.model.Blueprint;
import alien4cloud.paas.cloudify3.model.ListBlueprintResponse;
import alien4cloud.paas.cloudify3.util.FutureUtil;
import alien4cloud.utils.FileUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BlueprintClient {
    private static final String BLUEPRINT_PATH = "/api/v3/blueprints";
    private static final String ID_BLUEPRINT_PATH = BLUEPRINT_PATH + "/{id}";
    private final ApiHttpClient client;

    public BlueprintClient(ApiHttpClient apiHttpClient) {
        this.client = apiHttpClient;
    }

    public ListenableFuture<ListBlueprintResponse> asyncList(int offset, int size) {
        if (log.isDebugEnabled()) {
            log.debug("List blueprint");
        }
        return FutureUtil
                .unwrapRestResponse(client.getForEntity(client.buildRequestUrl(BLUEPRINT_PATH, "_offset", "_size"), ListBlueprintResponse.class, offset, size));
    }

    @SneakyThrows
    public ListBlueprintResponse list(int offset, int size) {
        return asyncList(offset, size).get();
    }

    @SneakyThrows
    public long count() {
        return client.getForEntity(client.buildRequestUrl(BLUEPRINT_PATH, "_offset", "_size"), ListBlueprintResponse.class, 0, 0).get().getBody().getMetaData()
                .getPagination().getTotal();
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
            return FutureUtil.unwrapRestResponse(client.exchange(client.buildRequestUrl(ID_BLUEPRINT_PATH, "application_file_name"), HttpMethod.PUT,
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
        return FutureUtil.unwrapRestResponse(client.getForEntity(client.buildRequestUrl(ID_BLUEPRINT_PATH), Blueprint.class, id));
    }

    @SneakyThrows
    public Blueprint read(String id) {
        return asyncRead(id).get();
    }

    public ListenableFuture<?> asyncDelete(String id) {
        if (log.isDebugEnabled()) {
            log.debug("Delete blueprint {}", id);
        }
        return FutureUtil.toGuavaFuture(client.delete(client.buildRequestUrl(ID_BLUEPRINT_PATH), id));
    }

    @SneakyThrows
    public void delete(String id) {
        asyncDelete(id).get();
    }
}
