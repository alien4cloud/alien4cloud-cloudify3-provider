package alien4cloud.paas.cloudify3;

import org.mockito.Mockito;
import org.springframework.web.client.AsyncRestTemplate;

import alien4cloud.paas.cloudify3.configuration.CloudConfiguration;
import alien4cloud.paas.cloudify3.configuration.CloudConfigurationHolder;
import alien4cloud.paas.cloudify3.model.Execution;
import alien4cloud.paas.cloudify3.restclient.ExecutionClient;
import alien4cloud.paas.cloudify3.restclient.VersionClient;
import alien4cloud.paas.cloudify3.restclient.auth.AuthenticationInterceptor;
import alien4cloud.paas.exception.PluginConfigurationException;

public class RestAPITest {

    public static void main(String[] args) {
        AuthenticationInterceptor authenticationInterceptor = new AuthenticationInterceptor();
        PluginFactoryConfiguration fake = Mockito.mock(PluginFactoryConfiguration.class);
        Mockito.when(fake.asyncRestTemplate()).thenCallRealMethod();
        Mockito.when(fake.restTemplate()).thenCallRealMethod();
        AsyncRestTemplate asyncRestTemplate = fake.asyncRestTemplate();
        ExecutionClient executionClient = new ExecutionClient();
        VersionClient versionClient = new VersionClient();
        versionClient.setRestTemplate(asyncRestTemplate);
        executionClient.setRestTemplate(asyncRestTemplate);
        CloudConfiguration defaultConfiguration = new CloudifyOrchestratorFactory().getDefaultConfiguration();
        defaultConfiguration.setUrl("https://129.185.67.33");
        defaultConfiguration.setUserName("admin");
        defaultConfiguration.setPassword("admin");
        CloudConfigurationHolder cloudConfigurationHolder = new CloudConfigurationHolder();
        cloudConfigurationHolder.setVersionClient(versionClient);
        cloudConfigurationHolder.setAuthenticationInterceptor(authenticationInterceptor);
        versionClient.setConfigurationHolder(cloudConfigurationHolder);
        executionClient.setConfigurationHolder(cloudConfigurationHolder);
        try {
            cloudConfigurationHolder.setConfigurationAndNotifyListeners(defaultConfiguration);
            Execution[] executions = executionClient.list("artifact-test-cfy3-Environment", true);
            for (Execution execution : executions) {
                System.out.println(execution.getWorkflowId() + " = " + execution.getIsSystemWorkflow());
            }
        } catch (PluginConfigurationException e) {
        }
    }
}
