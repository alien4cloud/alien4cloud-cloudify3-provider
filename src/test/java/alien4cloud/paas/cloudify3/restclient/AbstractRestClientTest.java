package alien4cloud.paas.cloudify3.restclient;

import org.springframework.web.client.AsyncRestTemplate;

import com.google.common.collect.Lists;

import alien4cloud.paas.cloudify3.PluginFactoryConfiguration;
import alien4cloud.paas.cloudify3.configuration.CfyConnectionManager;
import alien4cloud.paas.cloudify3.shared.restclient.ApiClient;
import alien4cloud.paas.cloudify3.shared.restclient.ApiHttpClient;
import alien4cloud.paas.cloudify3.shared.restclient.auth.AuthenticationInterceptor;

public abstract class AbstractRestClientTest {
    private static final String CLOUDIFY_MANAGER_URL_ENV_VAR = "CLOUDIFY_MANAGER_URL";

    private static final String MANAGER_URL = "http://34.249.69.84";
    private static final String MANAGER_USER = "admin";
    private static final String MANAGER_USER_PWD = "ad1min";

    private static AuthenticationInterceptor authenticationInterceptor;

    private static AsyncRestTemplate asyncRestTemplate;

    private static CfyConnectionManager cloudConfigurationHolder;

    private static ApiClient apiClient;

    public static void initializeContext() {
        String cloudifyManagerURL = System.getenv(CLOUDIFY_MANAGER_URL_ENV_VAR);
        if (cloudifyManagerURL == null || cloudifyManagerURL.isEmpty()) {
            cloudifyManagerURL = MANAGER_URL;
        }

        asyncRestTemplate = new PluginFactoryConfiguration().asyncRestTemplate();

        authenticationInterceptor = new AuthenticationInterceptor();
        authenticationInterceptor.setUserName(MANAGER_USER);
        authenticationInterceptor.setPassword(MANAGER_USER_PWD);

        ApiHttpClient apiHttpClient = new ApiHttpClient(asyncRestTemplate, Lists.newArrayList(cloudifyManagerURL), authenticationInterceptor, null, null);
        apiClient = new ApiClient(apiHttpClient);
    }

    public static ApiClient getApiClient() {
        return apiClient;
    }

    public static void sleep() {
        try {
            Thread.sleep(2000L);
        } catch (InterruptedException ignored) {
        }
    }
}
