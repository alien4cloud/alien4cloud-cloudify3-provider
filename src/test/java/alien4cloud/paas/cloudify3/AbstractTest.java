package alien4cloud.paas.cloudify3;

import alien4cloud.model.components.CSARSource;
import alien4cloud.orchestrators.plugin.ILocationConfiguratorPlugin;
import alien4cloud.orchestrators.plugin.model.PluginArchive;
import alien4cloud.paas.cloudify3.configuration.CloudConfigurationHolder;
import alien4cloud.paas.cloudify3.util.CSARUtil;
import alien4cloud.tosca.ArchiveIndexer;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.utils.FileUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.inject.Inject;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

public abstract class AbstractTest {

    public static final String NEW_SCALABLE_COMPUTE_TOPOLOGY = "new_scalable_compute";

    public static final String SINGLE_SCALABLE_COMPUTE_TOPOLOGY = "single_scalable_compute";

    public static final String SINGLE_COMPUTE_TOPOLOGY = "single_compute";

    public static final String SINGLE_WINDOWS_COMPUTE_TOPOLOGY = "single_windows_compute";

    public static final String NETWORK_TOPOLOGY = "network";

    public static final String STORAGE_TOPOLOGY = "storage";

    public static final String LAMP_TOPOLOGY = "lamp";

    public static final String TOMCAT_TOPOLOGY = "tomcat";

    public static final String ARTIFACT_TEST_TOPOLOGY = "artifact_test";

    public static final String CUSTOM_APACHE_TOPOLOGY = "compute_apache_prop_complex";

    public static final String VERSION;

    protected static final Set<String> LOCATIONS = Sets.newHashSet();

    @Value("${cloudify3.externalNetworkName}")
    private String externalNetworkName;

    @Value("${cloudify3.imageId}")
    private String imageId;

    @Value("${directories.alien}/${directories.csar_repository}")
    private String repositoryCsarDirectory;

    private static boolean isInitialized = false;

    private static boolean forceReloadCSARs = false;

    @Inject
    private CSARUtil csarUtil;

    @Inject
    private ArchiveIndexer archiveIndexer;

    @Inject
    private CloudifyOrchestrator cloudifyOrchestrator;

    @Inject
    protected CloudConfigurationHolder cloudConfigurationHolder;

    public static final Path tempPluginDataPath = Paths.get("target/alien/plugin");

    static {
        YamlPropertiesFactoryBean propertiesFactoryBean = new YamlPropertiesFactoryBean();
        propertiesFactoryBean.setResources(new org.springframework.core.io.Resource[] { new ClassPathResource("version.yml") });
        Properties properties = propertiesFactoryBean.getObject();
        VERSION = properties.getProperty("version");
    }

    @BeforeClass
    public static void cleanup() throws IOException {
        if (forceReloadCSARs) {
            FileUtil.delete(CSARUtil.ARTIFACTS_DIRECTORY);
            FileUtils.deleteDirectory(CSARUtil.ARTIFACTS_DIRECTORY.toFile());
        }
        FileUtil.delete(tempPluginDataPath);
        // this is a hack for the yml resources changes to be taken into account without having to build the project
        // we should filter before copying files

        // FileUtil.copy(Paths.get("src/main/resources"), tempPluginDataPath);
        for (Path cloudify3Path : Files.newDirectoryStream(Paths.get("target/"))) {
            if (Files.isDirectory(cloudify3Path) && cloudify3Path.toString().replaceAll("\\\\", "/").startsWith("target/alien4cloud-cloudify3-provider")) {
                FileUtil.copy(cloudify3Path, tempPluginDataPath);
            }
        }
    }

    @Before
    public void before() throws Exception {
        if (!isInitialized) {
            isInitialized = true;
        } else {
            return;
        }

        // Map<String, ILocationConfiguratorPlugin> locationConfigurators = Maps.newHashMap();
        // locationConfigurators.put("openstack", openstackLocationConfigurator);
        // locationConfigurators.put("amazon", amazonLocationConfigurator);
        // locationConfigurators.put("byon", byonLocationConfigurator);

        if (forceReloadCSARs || !Files.isDirectory(CSARUtil.ARTIFACTS_DIRECTORY)) {
            FileUtil.delete(Paths.get(repositoryCsarDirectory));
            csarUtil.uploadAll();
        }

        // Reload in order to be sure that the archive is constructed once all dependencies have been uploaded
        List<ParsingError> parsingErrors = Lists.newArrayList();
        for (PluginArchive pluginArchive : cloudifyOrchestrator.pluginArchives()) {
            // index the archive in alien catalog
            archiveIndexer.importArchive(pluginArchive.getArchive(), CSARSource.OTHER, pluginArchive.getArchiveFilePath(), parsingErrors);
        }

        // index archives of provided locations in alien catalog
        Map<String, ILocationConfiguratorPlugin> locationConfigurators = getLocationsConfigurators();
        if (MapUtils.isNotEmpty(locationConfigurators)) {
            for (String location : LOCATIONS) {
                ILocationConfiguratorPlugin locationConfigurator = locationConfigurators.get(location);
                if (locationConfigurator != null) {
                    for (PluginArchive pluginArchive : locationConfigurator.pluginArchives()) {
                        // index the archive in alien catalog
                        archiveIndexer.importArchive(pluginArchive.getArchive(), CSARSource.OTHER, pluginArchive.getArchiveFilePath(), parsingErrors);
                    }
                }
            }
        }
        cloudConfigurationHolder.setConfiguration(new CloudifyOrchestratorFactory().getDefaultConfiguration());
    }

    protected abstract Map<String, ILocationConfiguratorPlugin> getLocationsConfigurators();
}
