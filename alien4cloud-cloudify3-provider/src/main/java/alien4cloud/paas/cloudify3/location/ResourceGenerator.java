package alien4cloud.paas.cloudify3.location;

import java.util.List;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.orchestrators.locations.services.LocationResourceGeneratorService;
import alien4cloud.orchestrators.locations.services.LocationResourceGeneratorService.ComputeContext;
import alien4cloud.orchestrators.locations.services.LocationResourceGeneratorService.ImageFlavorContext;
import alien4cloud.orchestrators.plugin.ILocationResourceAccessor;

import com.google.common.collect.Lists;

@Component
@Slf4j
public class ResourceGenerator {

    @Inject
    private LocationResourceGeneratorService resourceGeneratorService;

    public List<LocationResourceTemplate> generateComputes(String computeType, String imageType, String flavorType, String imageIdProperty,
            String flavorIdProperty, ILocationResourceAccessor resourceAccessor) {
        return generateComputes(computeType, null, imageType, flavorType, imageIdProperty, flavorIdProperty, resourceAccessor);
    }

    public List<LocationResourceTemplate> generateComputes(String linuxComputeType, String windowsComputeType, String imageType, String flavorType,
            String imageIdProperty, String flavorIdProperty, ILocationResourceAccessor resourceAccessor) {
        ImageFlavorContext imageContext = resourceGeneratorService.buildContext(imageType, "id", resourceAccessor);
        ImageFlavorContext flavorContext = resourceGeneratorService.buildContext(flavorType, "id", resourceAccessor);
        boolean canProceed = true;
        if (CollectionUtils.isEmpty(imageContext.getTemplates())) {
            log.warn("At least one configured image resource is required for the auto-configuration");
            canProceed = false;
        }
        if (CollectionUtils.isEmpty(flavorContext.getTemplates())) {
            log.warn("At least one configured flavor resource is required for the auto-configuration");
            canProceed = false;
        }
        if (!canProceed) {
            log.warn("Skipping auto configuration");
            return Lists.newArrayList();
        }

        ComputeContext linuxComputeContext = resourceGeneratorService.buildComputeContext(linuxComputeType, null, imageIdProperty, flavorIdProperty,
                resourceAccessor);
        ComputeContext windowsComputeContext = windowsComputeType == null ? null
                : getWinComputeContext(windowsComputeType, imageIdProperty, flavorIdProperty, resourceAccessor);

        return resourceGeneratorService.generateComputeFromImageAndFlavor(imageContext, flavorContext, linuxComputeContext, windowsComputeContext,
                resourceAccessor);
    }

    private ComputeContext getWinComputeContext(String windowsComputeType, String imageIdProperty, String flavorIdProperty,
            ILocationResourceAccessor resourceAccessor) {
        ComputeContext computeContext = resourceGeneratorService.buildComputeContext(windowsComputeType, null, imageIdProperty, flavorIdProperty,
                resourceAccessor);
        if (computeContext.getNodeTypes().size() == 0) {
            return null;
        }
        return computeContext;
    }
}
