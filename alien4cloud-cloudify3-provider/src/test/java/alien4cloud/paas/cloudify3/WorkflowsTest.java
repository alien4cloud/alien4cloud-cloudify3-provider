package alien4cloud.paas.cloudify3;

import static org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants.INSTALL;
import static org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants.UNINSTALL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.workflow.NodeWorkflowStep;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;
import org.apache.commons.collections4.MapUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.model.common.Tag;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.paas.cloudify3.service.CloudifyDeploymentBuilderService;
import alien4cloud.paas.cloudify3.service.model.HostWorkflow;
import alien4cloud.paas.cloudify3.service.model.StandardWorkflow;
import alien4cloud.paas.cloudify3.service.model.Workflows;
import alien4cloud.paas.cloudify3.util.mapping.IPropertyMapping;
import alien4cloud.paas.cloudify3.util.mapping.PropertiesMappingUtil;
import alien4cloud.paas.model.PaaSTopologyDeploymentContext;
import alien4cloud.paas.wf.util.WorkflowUtils;

public class WorkflowsTest {

    private Map<String, Workflow> wfs = Maps.newHashMap();

    @Before
    public void before() {
        addWorkflow(true, INSTALL);
        addWorkflow(true, UNINSTALL);
    }

    @Test
    public void testPropertyMapping() {
        NodeType nodeType = new NodeType();
        Map<String, PropertyDefinition> properties = new HashMap<>();
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType("string");
        properties.put("size", propertyDefinition);
        nodeType.setProperties(properties);
        List<Tag> tags = new ArrayList<>();
        Tag tag = new Tag();
        tag.setName(PropertiesMappingUtil.PROP_MAPPING_TAG_KEY);
        tag.setValue(
                "{\"size\": [{\"path\": \"volume.size\", \"unit\": \"GiB\"}, \"toto\" ], \"volume_id\": \"resource_id\", \"snapshot_id\": \"volume.snapshot_id\", \"device\": \"device_name\"}");
        tags.add(tag);
        nodeType.setTags(tags);
        Map<String, List<IPropertyMapping>> mappings = PropertiesMappingUtil.loadPropertyMapping(PropertiesMappingUtil.PROP_MAPPING_TAG_KEY, nodeType);
        mappings.get("size");
        Assert.assertEquals(4, mappings.size()); // did not understand what submappings were about so far. will try.
        Assert.assertEquals(2, mappings.get("size").size());
    }

    /**
     * <pre>
     *
     *    | - a -- a1 -- a2 -- a3
     *            /
     * | - b --  b1
     * |          \
     * |           b2
     *               \
     *            | - orphan1
     *                   |
     *                   |
     *            | - orphan2
     *
     * </pre>
     */
    @Test
    public void test() {
        Workflow installWf = wfs.get(INSTALL);
        String host1 = "host1";
        String host2 = "host2";
        installWf.setHosts(Sets.newHashSet(host1, host2));
        WorkflowStep a = addStep(installWf, host1, "a");
        WorkflowStep a1 = addStep(installWf, host1, "a1");
        WorkflowStep a2 = addStep(installWf, host1, "a2");
        WorkflowStep a3 = addStep(installWf, host1, "a3");

        WorkflowStep b = addStep(installWf, host2, "b");
        WorkflowStep b1 = addStep(installWf, host2, "b1");
        WorkflowStep b2 = addStep(installWf, host2, "b2");

        WorkflowStep orphan1 = addStep(installWf, null, "orphan1");
        WorkflowStep orphan2 = addStep(installWf, null, "orphan2");

        WorkflowUtils.linkSteps(a, a1);
        WorkflowUtils.linkSteps(a1, a2);
        WorkflowUtils.linkSteps(a2, a3);

        WorkflowUtils.linkSteps(b, b1);
        WorkflowUtils.linkSteps(b1, b2);
        WorkflowUtils.linkSteps(b1, a2);
        WorkflowUtils.linkSteps(b2, orphan1);

        WorkflowUtils.linkSteps(orphan1, orphan2);

        CloudifyDeploymentBuilderService builder = new CloudifyDeploymentBuilderService();
        PaaSTopologyDeploymentContext paaSTopologyDeploymentContext = new PaaSTopologyDeploymentContext();
        paaSTopologyDeploymentContext.setDeploymentTopology(new DeploymentTopology());
        paaSTopologyDeploymentContext.getDeploymentTopology().setWorkflows(wfs);
        Workflows builtWorkflow = builder.buildWorkflowsForDeployment(paaSTopologyDeploymentContext);
        Map<String, HostWorkflow> installHostWFs = builtWorkflow.getInstallHostWorkflows();
        assertNotEmpty(installHostWFs);
        Assert.assertEquals(installWf.getHosts().size(), installHostWFs.size());
        assertHostWorkflow(installHostWFs, host1, 4, 3, 0);
        assertHostWorkflow(installHostWFs, host2, 4, 3, 2); // additional step b2 -> orphan1 considered as internal link

        assertNotEmpty(builtWorkflow.getStandardWorkflows());
        StandardWorkflow installSdf = builtWorkflow.getStandardWorkflows().get(INSTALL);
        assertStandardWorkFlow(installSdf, 2, 1);

    }

    private void assertStandardWorkFlow(StandardWorkflow standardWf, int expectedOrphans, int expectedLinks) {
        Assert.assertEquals(expectedOrphans, standardWf.getOrphanSteps().size());
        Assert.assertEquals(expectedLinks, standardWf.getLinks().size());

    }

    private void assertHostWorkflow(Map<String, HostWorkflow> hostWfs, String hostName, int expectedSteps, int expectedInternalLinks,
            int expectedExternalLinks) {
        HostWorkflow hwf = hostWfs.get(hostName);
        Assert.assertNotNull(hwf);
        Assert.assertEquals(expectedSteps, hwf.getSteps().size());
        Assert.assertEquals(expectedInternalLinks, hwf.getInternalLinks().size());
        Assert.assertEquals(expectedExternalLinks, hwf.getExternalLinks().size());
    }

    private void assertNotEmpty(Map<?, ?> map) {
        Assert.assertFalse(MapUtils.isEmpty(map));

    }

    private WorkflowStep addStep(Workflow installWf, String host1, String name) {
        NodeWorkflowStep step = new NodeWorkflowStep();
        step.setName(name);
        step.setHostId(host1);
        installWf.addStep(step);
        return step;
    }

    private void addWorkflow(boolean standard, String name) {
        Workflow wf = new Workflow();
        wf.setStandard(standard);
        wf.setName(name);
        wfs.put(name, wf);
    }

}
