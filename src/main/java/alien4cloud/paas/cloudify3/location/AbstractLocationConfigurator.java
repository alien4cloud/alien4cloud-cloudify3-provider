package alien4cloud.paas.cloudify3.location;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.deployment.matching.services.nodes.MatchingConfigurationsParser;
import alien4cloud.model.deployment.matching.MatchingConfiguration;
import alien4cloud.orchestrators.plugin.model.PluginArchive;
import alien4cloud.paas.cloudify3.service.PluginArchiveService;
import alien4cloud.plugin.model.ManagedPlugin;
import alien4cloud.tosca.parser.ParsingException;

public abstract class AbstractLocationConfigurator implements ITypeAwareLocationConfigurator {
    @Inject
    private ManagedPlugin selfContext;
    @Inject
    private PluginArchiveService archiveService;
    @Inject
    private MatchingConfigurationsParser matchingConfigurationsParser;

    protected List<PluginArchive> archives;
    protected Map<String, MatchingConfiguration> matchingConfigurations;

    private void parseLocationArchives(String[] paths) {
        this.archives = Lists.newArrayList();
        for (String path : paths) {
            this.archives.add(archiveService.parsePluginArchives(path));
        }
    }

    public List<String> getAllResourcesTypes() {
        List<String> resourcesTypes = Lists.newArrayList();
        for (PluginArchive pluginArchive : this.pluginArchives()) {
            for (String nodeType : pluginArchive.getArchive().getNodeTypes().keySet()) {
                resourcesTypes.add(nodeType);
            }
        }
        return resourcesTypes;
    }

    public Map<String, MatchingConfiguration> getMatchingConfigurations(String matchingConfigRelativePath) {
        if (matchingConfigurations == null) {
            Path matchingConfigPath = selfContext.getPluginPath().resolve(matchingConfigRelativePath);
            try {
                this.matchingConfigurations = matchingConfigurationsParser.parseFile(matchingConfigPath).getResult().getMatchingConfigurations();
            } catch (ParsingException e) {
                return Maps.newHashMap();
            }
        }
        return matchingConfigurations;
    }

    @Override
    public synchronized List<PluginArchive> pluginArchives() {
        if (this.archives == null) {
            parseLocationArchives(getLocationArchivePaths());
        }
        return this.archives;
    }

    protected abstract String[] getLocationArchivePaths();

}
