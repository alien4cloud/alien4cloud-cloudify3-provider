package alien4cloud.paas.cloudify3.shared.restclient;

import lombok.Getter;

/**
 * Manage the creation of a cloudify Api Client
 */
public class ApiClient {
    @Getter
    private final BlueprintClient blueprintClient;
    @Getter
    private final DeploymentClient deploymentClient;
    @Getter
    private final EventClient eventClient;
    @Getter
    private final DeploymentUpdateClient deploymentUpdateClient;
    @Getter
    private final ExecutionClient executionClient;
    @Getter
    private final NodeClient nodeClient;
    @Getter
    private final NodeInstanceClient nodeInstanceClient;
    @Getter
    private final TokenClient tokenClient;
    @Getter
    private final VersionClient versionClient;

    public ApiClient(ApiHttpClient httpClient) {
        blueprintClient = new BlueprintClient(httpClient);
        deploymentClient = new DeploymentClient(httpClient);
        eventClient = new EventClient(httpClient);
        deploymentUpdateClient = new DeploymentUpdateClient(httpClient);
        executionClient = new ExecutionClient(httpClient);
        nodeClient = new NodeClient(httpClient);
        nodeInstanceClient = new NodeInstanceClient(httpClient);
        tokenClient = new TokenClient(httpClient);
        versionClient = new VersionClient(httpClient);
    }
}