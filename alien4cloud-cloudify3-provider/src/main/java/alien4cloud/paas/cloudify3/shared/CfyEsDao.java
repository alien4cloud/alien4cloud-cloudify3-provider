package alien4cloud.paas.cloudify3.shared;

import java.beans.IntrospectionException;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import alien4cloud.dao.ESGenericSearchDAO;
import alien4cloud.dao.ElasticSearchMapper;
import alien4cloud.exception.IndexingServiceException;
import alien4cloud.paas.cloudify3.shared.model.LogClientRegistration;
import lombok.extern.slf4j.Slf4j;

/**
 * Dao to store event registration information.
 */
@Slf4j
@Component("cfy-es-dao")
public class CfyEsDao extends ESGenericSearchDAO {
    @PostConstruct
    public void initEnvironment() {
        // init ES annotation scanning
        try {
            getMappingBuilder().initialize("alien4cloud.paas.cloudify3.shared.model");
        } catch (IntrospectionException | IOException e) {
            throw new IndexingServiceException("Could not initialize elastic search mapping builder", e);
        }

        // init indices and mapped classes
        setJsonMapper(ElasticSearchMapper.getInstance());

        initIndice(LogClientRegistration.class);
    }

    private void initIndice(Class<?> clazz) {
        initIndices(clazz.getSimpleName().toLowerCase(), null, clazz);
    }

}
