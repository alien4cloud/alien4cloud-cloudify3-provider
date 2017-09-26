package alien4cloud.paas.cloudify3;

import alien4cloud.orchestrators.plugin.ILocationConfiguratorPlugin;
import alien4cloud.paas.cloudify3.location.AmazonLocationConfigurator;
import alien4cloud.paas.cloudify3.location.ByonLocationConfigurator;
import alien4cloud.paas.cloudify3.location.OpenstackLocationConfigurator;
import alien4cloud.paas.cloudify3.model.DeploymentPropertiesNames;
import alien4cloud.paas.cloudify3.util.DeploymentLauncher;
import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-context.xml")
@Slf4j
@Ignore
/**
 * This is not a test, it's more an utility class to rapidly bring up a deployment
 */
public class TestDeploymentService extends AbstractTest {

    @Inject
    private AmazonLocationConfigurator amazonLocationConfigurator;

    @Resource
    private DeploymentLauncher deploymentLauncher;

    @Getter
    private Map<String, ILocationConfiguratorPlugin> locationsConfigurators = Maps.newHashMap();

    @PostConstruct
    public void postConstruct() {
        LOCATIONS.add("amazon");
        locationsConfigurators.put("amazon", amazonLocationConfigurator);
    }

    @org.junit.Test
    public void deploySingleCompute() throws Exception {
        deploymentLauncher.launch(SINGLE_COMPUTE_TOPOLOGY);
    }

    @Test
    public void deployLamp() throws Exception {
        Map<String, String> deploymentProps = Maps.newHashMap();
        deploymentProps.put(DeploymentPropertiesNames.AUTO_HEAL, "true");
        deploymentLauncher.launch(LAMP_TOPOLOGY, deploymentProps);
    }

    @Test
    public void deployBlockStorage() throws Exception {
        deploymentLauncher.launch(STORAGE_TOPOLOGY);
    }

    /*
     * Many cloud images are not configured to automatically bring up all network cards that are available. They will usually only have a single network card
     * configured. To correctly set up a host in the cloud with multiple network cards, log on to the machine and bring up the additional interfaces.
     * 
     * On an Ubuntu Image, this usually looks like this:
     * echo $'auto eth1\niface eth1 inet dhcp' | sudo tee /etc/network/interfaces.d/eth1.cfg > /dev/null
     * sudo ifup eth1
     */
    @org.junit.Test
    public void deployNetwork() throws Exception {
        deploymentLauncher.launch(NETWORK_TOPOLOGY);
    }

    @org.junit.Test
    public void deployTomcat() throws Exception {
        deploymentLauncher.launch(TOMCAT_TOPOLOGY);
    }

    @org.junit.Test
    public void deployArtifactTest() throws Exception {
        deploymentLauncher.launch(ARTIFACT_TEST_TOPOLOGY);
    }

}
