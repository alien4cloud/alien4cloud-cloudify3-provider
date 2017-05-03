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

import alien4cloud.paas.cloudify3.util.FutureUtil;
import alien4cloud.utils.FileUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by xdegenne on 06/03/2017.
 */
@Slf4j
public class DeploymentUpdateClient {
    private static final String DEPLOYMENT_UPDATE_PATH = "/api/v3/deployment-updates/{id}/update/initiate";

    // FIXME: actually we can not query anything but backend REST API
    private final ApiHttpClient client;

    public DeploymentUpdateClient(ApiHttpClient apiHttpClient) {
        this.client = apiHttpClient;
    }

    @SneakyThrows
    public ListenableFuture<Void> asyncUpdate(String deploymentId, String path) {
        if (log.isDebugEnabled()) {
            log.debug("Update deployment {} with path {}", deploymentId, path);
        }
        Path sourcePath = Paths.get(path);
        String sourceName = sourcePath.getFileName().toString();
        File destination = File.createTempFile(deploymentId, ".tar.gz");
        // Tar gz the parent directory
        FileUtil.tar(sourcePath.getParent(), destination.toPath(), true, false);
        if (log.isDebugEnabled()) {
            log.debug("Created temporary archive file at {} for deployment update {}", destination, deploymentId);
        }
        try {
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
            return FutureUtil.unwrapRestResponse(client.exchange(client.buildRequestUrl(DEPLOYMENT_UPDATE_PATH, "application_file_name"), HttpMethod.POST,
                    new HttpEntity<>(Files.readAllBytes(destination.toPath()), headers), Void.class, deploymentId, sourceName));
        } finally {
            destination.delete();
        }
    }

    @SneakyThrows
    public Void update(String deploymentId, String path) {
        return asyncUpdate(deploymentId, path).get();
    }

}
