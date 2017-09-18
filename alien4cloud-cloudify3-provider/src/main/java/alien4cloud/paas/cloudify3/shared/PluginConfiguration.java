package alien4cloud.paas.cloudify3.shared;

import alien4cloud.ui.form.annotation.FormLabel;
import alien4cloud.ui.form.annotation.FormProperties;
import alien4cloud.ui.form.annotation.FormPropertyDefinition;
import lombok.Getter;
import lombok.Setter;

/**
 * Manage plugin configuration.
 */
@Getter
@Setter
@FormProperties({ "delayBetweenLogPolling" })
public class PluginConfiguration {
    @FormLabel("polling delay")
    @FormPropertyDefinition(description = "Delay between two events polling in seconds.", defaultValue = "5", type = "integer")
    private Integer delayBetweenLogPolling = 5;
}