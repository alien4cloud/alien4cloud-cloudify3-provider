package alien4cloud.paas.cloudify3;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;

import com.google.common.collect.Maps;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.io.Closeables;

import alien4cloud.component.repository.ArtifactLocalRepository;
import alien4cloud.component.repository.ArtifactRepositoryConstants;
import alien4cloud.model.components.DeploymentArtifact;
import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.components.IValue;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.orchestrators.plugin.ILocationConfiguratorPlugin;
import alien4cloud.paas.cloudify3.location.AmazonLocationConfigurator;
import alien4cloud.paas.cloudify3.location.ByonLocationConfigurator;
import alien4cloud.paas.cloudify3.location.OpenstackLocationConfigurator;
import alien4cloud.paas.cloudify3.service.PropertyEvaluatorService;
import alien4cloud.paas.cloudify3.util.ApplicationUtil;
import alien4cloud.paas.cloudify3.util.CSARUtil;
import alien4cloud.paas.cloudify3.util.DeploymentLauncher;
import alien4cloud.paas.model.PaaSTopologyDeploymentContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-context.xml")
@Ignore
public class TestBlueprintService extends AbstractTestBlueprint {

    @Inject
    private ArtifactLocalRepository artifactRepository;

    @Inject
    private ApplicationContext applicationContext;

    @Resource
    private DeploymentLauncher deploymentLauncher;

    @Resource
    private ApplicationUtil applicationUtil;

    @Resource
    private PropertyEvaluatorService propertyEvaluatorService;

    /**
     * Set true to this boolean when the blueprint has changed and you want to re-register
     */
    @Getter
    protected boolean record = false;

    /**
     * Set true to this boolean so the blueprint will be uploaded to the manager to verify
     */
    @Getter
    protected boolean verifyBlueprintUpload = false;

    @Getter
    private Map<String, ILocationConfiguratorPlugin> locationsConfigurators = Maps.newHashMap();

    @Resource
    private CSARUtil csarUtil;

    @PostConstruct
    public void postConstruct() {
        LOCATIONS.add("openstack");
        locationsConfigurators.put("openstack", applicationContext.getBean(OpenstackLocationConfigurator.class));
        LOCATIONS.add("amazon");
        locationsConfigurators.put("amazon", applicationContext.getBean(AmazonLocationConfigurator.class));
        LOCATIONS.add("byon");
        locationsConfigurators.put("byon", applicationContext.getBean(ByonLocationConfigurator.class));
    }

    @Override
    public void before() throws Exception {
        super.before();
        csarUtil.uploadCSAR(Paths.get("./src/test/resources/components/artifact-test"));
        csarUtil.uploadCSAR(Paths.get("./src/test/resources/components/support-hss"));
    }

    @Test
    public void testGenerateSingleCompute() {
        testGeneratedBlueprintFile(SINGLE_COMPUTE_TOPOLOGY);
    }

    @Test
    public void testGenerateNewScalableCompute() {
        testGeneratedBlueprintFile(NEW_SCALABLE_COMPUTE_TOPOLOGY);
    }

    @Test
    public void testGenerateSingleScalableCompute() {
        testGeneratedBlueprintFile(SINGLE_SCALABLE_COMPUTE_TOPOLOGY);
    }

    @Test
    public void testGenerateSingleWindowsCompute() {
        testGeneratedBlueprintFile(SINGLE_WINDOWS_COMPUTE_TOPOLOGY);
    }

    @Test
    public void testGenerateNetwork() {
        testGeneratedBlueprintFile(NETWORK_TOPOLOGY);
    }

    @Test
    public void testGenerateLamp() {
        testGeneratedBlueprintFile(LAMP_TOPOLOGY);
    }

    @Test
    public void testGenerateBlockStorage() {
        testGeneratedBlueprintFile(STORAGE_TOPOLOGY);
    }

    @Test
    public void testGenerateTomcat() {
        testGeneratedBlueprintFile(TOMCAT_TOPOLOGY);
    }

    @Test
    public void testGenerateArtifactsTest() {
        testGeneratedBlueprintFile(ARTIFACT_TEST_TOPOLOGY);
    }

    private void overrideArtifact(PaaSTopologyDeploymentContext deploymentContext, String nodeName, String artifactId, Path newArtifactContent)
            throws IOException {
        DeploymentArtifact artifact = deploymentContext.getPaaSTopology().getAllNodes().get(nodeName).getTemplate().getArtifacts().get(artifactId);
        if (ArtifactRepositoryConstants.ALIEN_ARTIFACT_REPOSITORY.equals(artifact.getArtifactRepository())) {
            artifactRepository.deleteFile(artifact.getArtifactRef());
        }
        InputStream artifactStream = Files.newInputStream(newArtifactContent);
        try {
            String artifactFileId = artifactRepository.storeFile(artifactStream);
            artifact.setArtifactName(newArtifactContent.getFileName().toString());
            artifact.setArtifactRef(artifactFileId);
            artifact.setArtifactRepository(ArtifactRepositoryConstants.ALIEN_ARTIFACT_REPOSITORY);
        } finally {
            Closeables.close(artifactStream, true);
        }
    }

    @Test
    public void testGenerateOverriddenArtifactsTest() {
        for (String location : LOCATIONS) {
            testGeneratedBlueprintFile(ARTIFACT_TEST_TOPOLOGY, location, ARTIFACT_TEST_TOPOLOGY + "Overridden", "testGenerateOverridenArtifactsTest",
                    new DeploymentContextVisitor() {
                        @Override
                        public void visitDeploymentContext(PaaSTopologyDeploymentContext context) throws Exception {
                            overrideArtifact(context, "War", "war_file", Paths.get("src/test/resources/data/war-examples/helloWorld.war"));
                        }
                    });
        }
    }

    @Test
    public void testGetComplexProperty() {
        String topology = CUSTOM_APACHE_TOPOLOGY;
        String locationName = "openstack";

        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        if (!applicationUtil.isTopologyExistForLocation(topology, locationName)) {
            log.warn("Topology {} do not exist for location {}", topology, locationName);
            return;
        }

        PaaSTopologyDeploymentContext context = deploymentLauncher.buildPaaSDeploymentContext(stackTraceElements[2].getMethodName(), topology, locationName);

        // check the function of get_property on property
        IValue value = context.getPaaSTopology().getNonNatives().get(0).getInterfaces().get("tosca.interfaces.node.lifecycle.Standard").getOperations()
                .get("create").getInputParameters().get("DOC_ROOT");
        FunctionPropertyValue functionValue = (FunctionPropertyValue) value;
        assertEquals(functionValue.getFunction(), "get_property");
        assertEquals(functionValue.getParameters().get(0), "SELF");
        assertEquals(functionValue.getParameters().get(1), "floatingip");

        // check the function of get_property on capability
        IValue capabilityValue = context.getPaaSTopology().getNonNatives().get(0).getInterfaces().get("tosca.interfaces.node.lifecycle.Standard")
                .getOperations().get("create").getInputParameters().get("GET_PROPERTY_CAPABILITY");
        FunctionPropertyValue capabilityFunctionValue = (FunctionPropertyValue) capabilityValue;
        assertEquals(capabilityFunctionValue.getFunction(), "get_property");
        assertEquals(capabilityFunctionValue.getParameters().get(0), "SELF");
        assertEquals(capabilityFunctionValue.getParameters().get(1), "host");
        assertEquals(capabilityFunctionValue.getParameters().get(2), "floatingip_capability");

        // check the function of get_property on relationship
        IValue relationshipValue = context.getPaaSTopology().getNonNatives().get(0).getRelationshipTemplates().get(0).getInterfaces()
                .get("tosca.interfaces.relationship.Configure").getOperations().get("pre_configure_source").getInputParameters()
                .get("GET_PROPERTY_RELATIONSHIP");
        FunctionPropertyValue relationshipFunctionValue = (FunctionPropertyValue) relationshipValue;
        assertEquals(relationshipFunctionValue.getFunction(), "get_property");
        assertEquals(relationshipFunctionValue.getParameters().get(0), "SELF");
        assertEquals(relationshipFunctionValue.getParameters().get(1), "floatingip_relationship");

        propertyEvaluatorService.processGetPropertyFunction(context);

        // check the value of get_property on property
        value = context.getPaaSTopology().getNonNatives().get(0).getInterfaces().get("tosca.interfaces.node.lifecycle.Standard").getOperations().get("create")
                .getInputParameters().get("DOC_ROOT");
        ScalarPropertyValue scalarValue = (ScalarPropertyValue) value;
        assertEquals(scalarValue.getValue(), "{\n  \"floating_network_name\" : \"test\"\n}");

        // check the value of get_property on capability
        capabilityValue = context.getPaaSTopology().getNonNatives().get(0).getInterfaces().get("tosca.interfaces.node.lifecycle.Standard").getOperations()
                .get("create").getInputParameters().get("GET_PROPERTY_CAPABILITY");
        ScalarPropertyValue capabilityScalarValue = (ScalarPropertyValue) capabilityValue;
        assertEquals(capabilityScalarValue.getValue(), "{\n  \"floating_network_name\" : \"test2\"\n}");

        // check the value of get_property on relationship
        relationshipValue = context.getPaaSTopology().getNonNatives().get(0).getRelationshipTemplates().get(0).getInterfaces()
                .get("tosca.interfaces.relationship.Configure").getOperations().get("pre_configure_source").getInputParameters()
                .get("GET_PROPERTY_RELATIONSHIP");
        ScalarPropertyValue relationshipScalarValue = (ScalarPropertyValue) relationshipValue;
        assertEquals(relationshipScalarValue.getValue(), "{\n  \"floating_network_name\" : \"test3\"\n}");
    }
}
