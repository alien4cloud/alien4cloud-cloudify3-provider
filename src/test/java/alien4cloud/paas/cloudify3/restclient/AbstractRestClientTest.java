package alien4cloud.paas.cloudify3.restclient;

import org.junit.Assert;
import org.mockito.Mockito;
import org.springframework.web.client.AsyncRestTemplate;

import alien4cloud.paas.cloudify3.CloudifyOrchestratorFactory;
import alien4cloud.paas.cloudify3.PluginContextConfiguration;
import alien4cloud.paas.cloudify3.configuration.CloudConfiguration;
import alien4cloud.paas.cloudify3.configuration.CloudConfigurationHolder;
import alien4cloud.paas.cloudify3.restclient.auth.AuthenticationInterceptor;
import alien4cloud.paas.cloudify3.restclient.auth.SSLContextManager;
import alien4cloud.paas.exception.PluginConfigurationException;

public abstract class AbstractRestClientTest {

    private static final String CLOUDIFY_MANAGER_URL_ENV_VAR = "CLOUDIFY_MANAGER_URL";

    static AuthenticationInterceptor authenticationInterceptor;

    static AsyncRestTemplate asyncRestTemplate;

    static CloudConfigurationHolder cloudConfigurationHolder;

    static void initializeContext() {
        // String cloudifyManagerURL = System.getenv(CLOUDIFY_MANAGER_URL_ENV_VAR);
        String cloudifyManagerURL = "http://34.249.69.84";
        // Assert.assertTrue(CLOUDIFY_MANAGER_URL_ENV_VAR + " must be configured for the test", StringUtils.isNotBlank(cloudifyManagerURL));
        PluginContextConfiguration fake = Mockito.mock(PluginContextConfiguration.class);
        Mockito.when(fake.asyncRestTemplate()).thenCallRealMethod();
        Mockito.when(fake.restTemplate()).thenCallRealMethod();
        asyncRestTemplate = fake.asyncRestTemplate();
        CloudConfiguration defaultConfiguration = new CloudifyOrchestratorFactory().getDefaultConfiguration();
        defaultConfiguration.setUrl(cloudifyManagerURL);
        defaultConfiguration.setUserName("admin");
        defaultConfiguration.setPassword("ad1min");
        cloudConfigurationHolder = new CloudConfigurationHolder();
        authenticationInterceptor = new AuthenticationInterceptor();

        VersionClient versionClient = configureClient(new VersionClient());
        BlueprintClient blueprintClient = configureClient(new BlueprintClient());
        DeploymentClient deploymentClient = configureClient(new DeploymentClient());

        cloudConfigurationHolder.setVersionClient(versionClient);
        cloudConfigurationHolder.setBlueprintClient(blueprintClient);
        cloudConfigurationHolder.setDeploymentClient(deploymentClient);
        cloudConfigurationHolder.setAuthenticationInterceptor(authenticationInterceptor);
        cloudConfigurationHolder.setSslContextManager(new SSLContextManager());
        versionClient.setConfigurationHolder(cloudConfigurationHolder);
        try {
            cloudConfigurationHolder.setConfigurationAndNotifyListeners(defaultConfiguration);
        } catch (PluginConfigurationException e) {
            Assert.fail("Configuration is incorrect");
        }
    }

    static <T extends AbstractClient> T configureClient(T client) {
        client.setRestTemplate(asyncRestTemplate);
        client.setAuthenticationInterceptor(authenticationInterceptor);
        client.setConfigurationHolder(cloudConfigurationHolder);
        return client;
    }

    static void sleep() {
        try {
            Thread.sleep(2000L);
        } catch (InterruptedException ignored) {
        }
    }
}
