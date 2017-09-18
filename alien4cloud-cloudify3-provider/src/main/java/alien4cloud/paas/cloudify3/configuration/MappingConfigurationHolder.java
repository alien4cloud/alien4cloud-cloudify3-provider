package alien4cloud.paas.cloudify3.configuration;

import java.io.BufferedInputStream;
import java.nio.file.Files;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import alien4cloud.plugin.model.ManagedPlugin;
import alien4cloud.utils.YamlParserUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("cloudify-mapping-configuration-holder")
public class MappingConfigurationHolder {

    @Resource
    private ManagedPlugin pluginContext;

    private ObjectMapper yamlObjectMapper = YamlParserUtil.createYamlObjectMapper();

    @Getter
    private MappingConfiguration mappingConfiguration;

    @PostConstruct
    public void postConstruct() throws Exception {
        mappingConfiguration = yamlObjectMapper.readValue(
                new BufferedInputStream(Files.newInputStream(pluginContext.getPluginPath().resolve("mapping/mapping.yaml"))), MappingConfiguration.class);
    }
}
