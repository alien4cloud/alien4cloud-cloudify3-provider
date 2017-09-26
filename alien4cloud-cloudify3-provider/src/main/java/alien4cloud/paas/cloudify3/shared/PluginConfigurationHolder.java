package alien4cloud.paas.cloudify3.shared;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import alien4cloud.plugin.IPluginConfigurator;
import alien4cloud.plugin.exception.PluginConfigurationException;
import lombok.Getter;

/**
 * Manage all configuration that is not orchestrator instance specific.
 */
@Service
public class PluginConfigurationHolder implements IPluginConfigurator<PluginConfiguration> {
    private List<Consumer<PluginConfiguration>> listeners = Lists.newArrayList();
    @Getter
    private PluginConfiguration pluginConfiguration;

    @PostConstruct
    private void init() {
        pluginConfiguration = getDefaultConfiguration();
    }

    @Override
    public PluginConfiguration getDefaultConfiguration() {
        return new PluginConfiguration();
    }

    @Override
    public synchronized void setConfiguration(PluginConfiguration pluginConfiguration) throws PluginConfigurationException {
        this.pluginConfiguration = pluginConfiguration;

        for (Consumer<PluginConfiguration> consumer : listeners) {
            consumer.accept(pluginConfiguration);
        }
    }

    /**
     * Register a listener for configuration change..
     *
     * @param configurationConsumer The listener for configuration change.
     */
    public synchronized void register(Consumer<PluginConfiguration> configurationConsumer) {
        this.listeners.add(configurationConsumer);
    }
}
