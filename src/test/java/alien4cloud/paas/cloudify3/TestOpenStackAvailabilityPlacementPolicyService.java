package alien4cloud.paas.cloudify3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.*;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;

import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.ComplexPropertyValue;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import alien4cloud.paas.cloudify3.service.OpenStackAvailabilityZonePlacementPolicyService;
import alien4cloud.paas.model.PaaSNodeTemplate;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import com.google.common.collect.Maps;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.orchestrators.plugin.ILocationConfiguratorPlugin;
import alien4cloud.paas.cloudify3.location.OpenstackLocationConfigurator;
import alien4cloud.paas.cloudify3.util.DeploymentLauncher;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-context.xml")
public class TestOpenStackAvailabilityPlacementPolicyService extends AbstractTest {

    private static final String GROUP_NAME = "A4C-AH";

    @Resource
    private DeploymentLauncher deploymentLauncher;

    @Inject
    private OpenStackAvailabilityZonePlacementPolicyService osAzPPolicyService;

    @Inject
    private ApplicationContext applicationContext;

    @Getter
    private Map<String, ILocationConfiguratorPlugin> locationsConfigurators = Maps.newHashMap();

    @PostConstruct
    public void postConstruct() {
        LOCATIONS.add("openstack");
        locationsConfigurators.put("openstack", applicationContext.getBean(OpenstackLocationConfigurator.class));
    }

    @Override
    public void before() throws Exception {
        // void
    }

    private NodeTemplate buildFakeNodeTemplate(String name, String zone) {
        NodeTemplate fakeTemplate = Mockito.mock(NodeTemplate.class);
        HashMap<String, AbstractPropertyValue> properties = new HashMap<String, AbstractPropertyValue>();
        properties.put("id", new ScalarPropertyValue(zone));
        Mockito.when(fakeTemplate.getProperties()).thenReturn(properties);
        Mockito.when(fakeTemplate.getName()).thenReturn(name);
        return fakeTemplate;
    }

    private LocationResourceTemplate buildFakeLocationResourceTemplate(NodeTemplate template) {
        LocationResourceTemplate locationResourceTemplate = Mockito.mock(LocationResourceTemplate.class);
        Mockito.when(locationResourceTemplate.getTemplate()).thenReturn(template);
        return locationResourceTemplate;
    }

    @Test
    public void testProcessTopologyOnOneCompute() {
        Map<String, PaaSNodeTemplate> allNodes = Maps.<String, PaaSNodeTemplate> newHashMap();
        List<LocationResourceTemplate> availabilityZones = Lists.<LocationResourceTemplate> newArrayList();
        Set<String> members = new HashSet<>();
        String computeName = "Compute 1";

        NodeTemplate fakeTemplate = buildFakeNodeTemplate(computeName,"A4C-zone");
        members.add(computeName);
        allNodes.put(computeName, new PaaSNodeTemplate("fake-id", fakeTemplate));
        availabilityZones.add(buildFakeLocationResourceTemplate(fakeTemplate));

        ComplexPropertyValue propertyValue = new ComplexPropertyValue();
        propertyValue.setValue(new HashMap<String, Object>());
        propertyValue.getValue().put(OpenStackAvailabilityZonePlacementPolicyService.AZ_KEY, "A4C-zone");

        assertNotEquals(allNodes.get(computeName).getTemplate().getProperties().get(OpenStackAvailabilityZonePlacementPolicyService.SERVER_PROPERTY), propertyValue);
        osAzPPolicyService.setAZForAllNodes("fake-id", allNodes, availabilityZones, GROUP_NAME, members);
        assertEquals(allNodes.get(computeName).getTemplate().getProperties().get(OpenStackAvailabilityZonePlacementPolicyService.SERVER_PROPERTY), propertyValue);
    }


    @Test
    public void testProcessTopologyOnFourCompute() {
        Map<String, PaaSNodeTemplate> allNodes = Maps.<String, PaaSNodeTemplate> newHashMap();
        List<LocationResourceTemplate> availabilityZones = Lists.<LocationResourceTemplate> newArrayList();
        Set<String> members = new HashSet<>();
        members.add("Compute 1");
        members.add("Compute 2");
        members.add("Compute 3");
        members.add("Compute 4");

        for (String computeName : members) {
            NodeTemplate fakeTemplate = buildFakeNodeTemplate(computeName, "");
            allNodes.put(computeName, new PaaSNodeTemplate("fake-id", fakeTemplate));
        }

        availabilityZones.add(buildFakeLocationResourceTemplate(buildFakeNodeTemplate("a", "Fastconnect")));
        availabilityZones.add(buildFakeLocationResourceTemplate(buildFakeNodeTemplate("b", "A4C-zone")));

        osAzPPolicyService.setAZForAllNodes("fake-id", allNodes, availabilityZones, GROUP_NAME, members);

        Map<String, Integer> countAZ = new HashMap<String, Integer>();
        countAZ.put("Fastconnect", 0);
        countAZ.put("A4C-zone", 0);

        for (String computeName : members) {
            ComplexPropertyValue propertyValue = (ComplexPropertyValue) allNodes.get(computeName).getTemplate().getProperties().get(OpenStackAvailabilityZonePlacementPolicyService.SERVER_PROPERTY);
            String AZ = (String) propertyValue.getValue().get(OpenStackAvailabilityZonePlacementPolicyService.AZ_KEY);
            countAZ.put(AZ, countAZ.get(AZ) + 1);
        }

        for (Map.Entry<String, Integer> entry : countAZ.entrySet()) {
            assertEquals(entry.getValue(), new Integer(2));
        }
    }



}
