package alien4cloud.paas.cloudify3.util;

import alien4cloud.application.ApplicationService;
import alien4cloud.dao.ElasticSearchDAO;
import alien4cloud.model.application.Application;
import alien4cloud.paas.cloudify3.AbstractTest;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.utils.FileUtil;
import alien4cloud.utils.MapUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.catalog.ArchiveParser;
import org.alien4cloud.tosca.model.templates.Topology;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component("cfy3-applicationUtil")
@Slf4j
public class ApplicationUtil {

    @Resource
    private ApplicationService applicationService;

    @Resource
    protected ElasticSearchDAO alienDAO;

    @Resource
    private ArchiveParser parser;

    public boolean isTopologyExistForLocation(String topologyFileName, String locationName) {
        return Files.exists(Paths.get("src/test/resources/topologies/" + locationName + "/" + topologyFileName + ".yaml"));
    }

    @SneakyThrows
    public Topology createAlienApplication(String applicationName, String topologyFileName, String locationName) {
        Application application = alienDAO.customFind(Application.class, QueryBuilders.termQuery("name", applicationName));
        if (application != null) {
            applicationService.delete(application.getId());
        }
        ArchiveRoot archiveRoot = parseYamlTopology(topologyFileName, locationName);
        String applicationId = applicationService.create("alien", applicationName, null);
        // TODO validate this works
        archiveRoot.getArchive().setDelegateId(applicationId);
        archiveRoot.getArchive().setDelegateType(Application.class.getSimpleName().toLowerCase());
        alienDAO.save(archiveRoot.getArchive());
        alienDAO.save(archiveRoot.getTopology());
        return archiveRoot.getTopology();
    }

    private ArchiveRoot parseYamlTopology(String topologyFileName, String locationName) throws IOException, ParsingException {
        Path zipPath = Files.createTempFile("csar", ".zip");
        Path realTopologyPath = Files.createTempFile(topologyFileName, ".yaml");
        VelocityUtil.generate(Paths.get("src/test/resources/topologies/" + locationName + "/" + topologyFileName + ".yaml"), realTopologyPath,
                MapUtil.newHashMap(new String[] { "projectVersion" }, new String[] { AbstractTest.VERSION }));
        FileUtil.zip(realTopologyPath, zipPath);
        ParsingResult<ArchiveRoot> parsingResult = parser.parse(zipPath);
        return parsingResult.getResult();
    }
}
