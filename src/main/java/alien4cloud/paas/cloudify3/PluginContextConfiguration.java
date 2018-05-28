package alien4cloud.paas.cloudify3;

import org.springframework.boot.actuate.autoconfigure.ManagementServerProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

@EnableGlobalMethodSecurity(prePostEnabled = true)
@Order(ManagementServerProperties.ACCESS_OVERRIDE_ORDER)
@Configuration
@ComponentScan(basePackages = { "alien4cloud.paas.cloudify3" }, excludeFilters = {
        @Filter(type = FilterType.REGEX, pattern = "alien4cloud\\.paas\\.cloudify3\\.shared\\..*"),
        @Filter(type = FilterType.REGEX, pattern = "alien4cloud\\.paas\\.cloudify3\\.PluginFactoryConfiguration")})
@ImportResource("classpath:cloudify3-plugin-properties-config.xml")
public class PluginContextConfiguration {
}