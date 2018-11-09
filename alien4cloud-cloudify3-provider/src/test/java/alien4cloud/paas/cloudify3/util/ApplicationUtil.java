package alien4cloud.paas.cloudify3.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.Resource;

import org.alien4cloud.tosca.model.Csar;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Component;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.application.ApplicationVersionService;
import alien4cloud.dao.ElasticSearchDAO;
import alien4cloud.deployment.DeploymentTopologyDTOBuilder;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.paas.cloudify3.AbstractTest;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.utils.MapUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Component("cfy3-applicationUtil")
@Slf4j
public class ApplicationUtil {

    @Resource
    private ApplicationService applicationService;

    @Resource
    protected ElasticSearchDAO alienDAO;

    @Resource
    protected ApplicationVersionService applicationVersionService;

    @Resource
    protected ApplicationEnvironmentService applicationEnvironmentService;

    @Resource
    private DeploymentTopologyDTOBuilder deploymentTopologyDTOBuilder;

    @Resource
    private TopologyServiceCore topologyServiceCore;

    @Resource
    private CSARUtil csarUtil;

    public boolean isTopologyExistForLocation(String topologyFileName, String locationName) {
        return Files.exists(Paths.get("src/test/resources/topologies/" + locationName + "/" + topologyFileName + ".yaml"));
    }

    @SneakyThrows
    public DeploymentTopology createAlienApplication(String applicationName, String topologyFileName, String locationName) {
        Application application = alienDAO.customFind(Application.class, QueryBuilders.termQuery("name", applicationName));
        if (application != null) {
            applicationService.delete(application.getId());
        }
        Csar csar = parseYamlTopology(topologyFileName, locationName);
        String applicationId = applicationService.create("alien", applicationName, applicationName, null, null);
        // TODO validate this works
        ApplicationVersion version = applicationVersionService.createInitialVersion(applicationId, csar.getId());
        ApplicationEnvironment applicationEnvironment = applicationEnvironmentService.createApplicationEnvironment("alien", applicationId,
                version.getTopologyVersions().keySet().iterator().next());
        return deploymentTopologyDTOBuilder.prepareDeployment(topologyServiceCore.getOrFail(csar.getId()), application, applicationEnvironment).getTopology();
    }

    private Csar parseYamlTopology(String topologyFileName, String locationName) throws Exception {
        Path realTopologyPath = Files.createTempFile(topologyFileName, ".yaml");
        VelocityUtil.generate(Paths.get("src/test/resources/topologies/" + locationName + "/" + topologyFileName + ".yaml"), realTopologyPath,
                MapUtil.newHashMap(new String[] { "projectVersion" }, new String[] { AbstractTest.VERSION }));
        return csarUtil.uploadCSAR(realTopologyPath).getResult();
    }
}
