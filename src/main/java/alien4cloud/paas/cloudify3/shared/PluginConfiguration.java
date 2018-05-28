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
@FormProperties({ "delayBetweenLogPolling", "delayedPollerPanic", "cloudifyAsyncThreadpoolCoreSize", "cloudifyAsyncThreadpoolMaxSize", "delayedAsyncThreadpoolCoreSize", "delayedAsyncThreadpoolMaxSize" })
public class PluginConfiguration {
    @FormLabel("polling delay")
    @FormPropertyDefinition(description = "Delay between two events polling in seconds.", defaultValue = "5", type = "integer")
    private Integer delayBetweenLogPolling = 5;

    @FormLabel("Delayed poller Panic")
    @FormPropertyDefinition(description = "Switch delayed poller to panic mode", defaultValue = "false", type = "boolean")
    private Boolean delayedPollerPanic = false;

    @FormLabel("Main REST threadpool coreSize")
    @FormPropertyDefinition(description = "Number of thread by default for the main REST threadpool (cloudify-async-thread-pool)", defaultValue = "5", type = "integer")
    private Integer cloudifyAsyncThreadpoolCoreSize = 5;

    @FormLabel("Main REST threadpool maxSize")
    @FormPropertyDefinition(description = "Maximum number of thread for the main REST threadpool (cloudify-async-thread-pool)", defaultValue = "50", type = "integer")
    private Integer cloudifyAsyncThreadpoolMaxSize = 50;

    @FormLabel("Delayed REST threadpool coreSize")
    @FormPropertyDefinition(description = "Number of thread for the delayed scheduler threadpool (delayed-async-thread-pool)", defaultValue = "2", type = "integer")
    private Integer delayedAsyncThreadpoolCoreSize = 2;

    @FormLabel("Delayed REST threadpool maxSize")
    @FormPropertyDefinition(description = "Maximum number of thread for the delayed REST threadpool (delayed-async-thread-pool)", defaultValue = "2", type = "integer")
    private Integer delayedAsyncThreadpoolMaxSize = 2;

}