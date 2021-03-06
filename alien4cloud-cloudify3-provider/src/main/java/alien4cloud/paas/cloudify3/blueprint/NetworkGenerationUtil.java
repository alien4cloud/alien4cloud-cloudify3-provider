package alien4cloud.paas.cloudify3.blueprint;

import java.nio.file.Path;
import java.util.List;

import org.alien4cloud.tosca.normative.ToscaNormativeUtil;

import com.google.common.collect.Lists;

import alien4cloud.paas.cloudify3.configuration.MappingConfiguration;
import alien4cloud.paas.cloudify3.service.PropertyEvaluatorService;
import alien4cloud.paas.cloudify3.service.model.CloudifyDeployment;
import alien4cloud.paas.model.PaaSNodeTemplate;
import org.alien4cloud.tosca.normative.ToscaNormativeUtil;

import com.google.common.collect.Lists;
import org.alien4cloud.tosca.utils.ToscaTypeUtils;

public class NetworkGenerationUtil extends NativeTypeGenerationUtil {

    public NetworkGenerationUtil(MappingConfiguration mappingConfiguration, CloudifyDeployment alienDeployment, Path recipePath,
            PropertyEvaluatorService propertyEvaluatorService) {
        super(mappingConfiguration, alienDeployment, recipePath, propertyEvaluatorService);
    }

    private List<PaaSNodeTemplate> getNetworksOfType(PaaSNodeTemplate compute, String networkType) {
        List<PaaSNodeTemplate> computeNetworks = compute.getNetworkNodes();
        List<PaaSNodeTemplate> selectedNetworks = Lists.newArrayList();
        for (PaaSNodeTemplate computeNetwork : computeNetworks) {
            if (ToscaTypeUtils.isOfType(computeNetwork.getIndexedToscaElement(), networkType)) {
                selectedNetworks.add(computeNetwork);
            }
        }
        return selectedNetworks;
    }

    public List<PaaSNodeTemplate> getExternalNetworks(PaaSNodeTemplate compute) {
        return getNetworksOfType(compute, "alien.nodes.PublicNetwork");
    }

    public List<PaaSNodeTemplate> getInternalNetworks(PaaSNodeTemplate compute) {
        return getNetworksOfType(compute, "alien.nodes.PrivateNetwork");
    }

    public String generateFloatingIpNodeName(String computeId, String networkId) {
        return String.format("%s_floating_ip_%s_on_%s", mappingConfiguration.getGeneratedNodePrefix(), computeId, networkId);
    }
}
