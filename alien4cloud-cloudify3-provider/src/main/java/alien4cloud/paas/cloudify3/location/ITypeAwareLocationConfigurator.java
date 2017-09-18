package alien4cloud.paas.cloudify3.location;

import java.util.Set;

import alien4cloud.orchestrators.plugin.ILocationConfiguratorPlugin;

public interface ITypeAwareLocationConfigurator extends ILocationConfiguratorPlugin {

    Set<String> getManagedLocationTypes();
}
