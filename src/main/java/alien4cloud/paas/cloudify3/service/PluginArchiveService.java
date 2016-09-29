package alien4cloud.paas.cloudify3.service;

import java.nio.file.Path;

import javax.inject.Inject;

import alien4cloud.common.AlienConstants;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import alien4cloud.orchestrators.plugin.model.PluginArchive;
import alien4cloud.paas.cloudify3.error.BadConfigurationException;
import alien4cloud.plugin.model.ManagedPlugin;
import org.alien4cloud.tosca.catalog.ArchiveParser;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;

@Component("plugin-archive-service")
@Slf4j
public class PluginArchiveService {

    @Inject
    private ManagedPlugin selfContext;

    @Inject
    private ArchiveParser archiveParser;

    public PluginArchive parsePluginArchives(String archiveRelativePath) {
        // Parse the archives
        ParsingResult<ArchiveRoot> result;
        Path archivePath = selfContext.getPluginPath().resolve(archiveRelativePath);
        try {
            result = this.archiveParser.parseDir(archivePath, AlienConstants.GLOBAL_WORKSPACE_ID);
        } catch (ParsingException e) {
            log.error("Failed to parse archive, plugin won't work", e);
            throw new BadConfigurationException("Failed to parse archive, plugin won't work", e);
        }
        if (result.getContext().getParsingErrors() != null && !result.getContext().getParsingErrors().isEmpty()) {
            log.error("Parsing errors for " + archiveRelativePath);
            for (ParsingError parsingError : result.getContext().getParsingErrors()) {
                log.error(parsingError.toString());
            }
            throw new BadConfigurationException("Plugin archive is invalid, plugin won't work");
        }
        return new PluginArchive(result.getResult(), archivePath);
    }
}
