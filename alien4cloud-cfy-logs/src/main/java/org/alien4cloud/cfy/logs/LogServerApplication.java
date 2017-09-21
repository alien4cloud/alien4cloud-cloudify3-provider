package org.alien4cloud.cfy.logs;

import java.io.IOException;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import alien4cloud.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = { "org.alien4cloud.cfy" })
@Slf4j
public class LogServerApplication {

    @Value("${server.http.port:#{8080}}")
    private int httpPort;

    private static final String[] SEARCH_LOCATIONS = new String[] { "file:./config/", "file:./", "classpath:/config/", "classpath:/" };

    public static void main(String[] args) throws Exception {
        System.setProperty("spring.config.name", "alien4cloud-cfy-logs-config");
        SpringApplication.run(LogServerApplication.class, args);
    }

    @Bean(name = "config")
    public static YamlPropertiesFactoryBean config(ResourceLoader resourceLoader) throws IOException {
        for (String searchLocation : SEARCH_LOCATIONS) {
            Resource resource = resourceLoader.getResource(searchLocation + "alien4cloud-cfy-logs-config.yml");
            if (resource != null && resource.exists()) {
                log.info("Loading Alien 4 Cloud configuration from {}", resource.getDescription());
                YamlPropertiesFactoryBean config = new YamlPropertiesFactoryBean();
                config.setResources(resource);
                return config;
            }
        }
        throw new NotFoundException("Configuration file for alien post deployment web app cannot be found");
    }

    /**
     * Add a connector to serve both https and http protocol.
     *
     * Only if the property <b>server.ssl.enabled</b> is set to <b>true</b> in the config file
     * 
     * @return
     */
    @Bean
    @ConditionalOnProperty("server.ssl.enabled")
    public EmbeddedServletContainerCustomizer containerCustomizer() {
        return new EmbeddedServletContainerCustomizer() {
            @Override
            public void customize(ConfigurableEmbeddedServletContainer container) {
                if (container instanceof JettyEmbeddedServletContainerFactory) {
                    JettyEmbeddedServletContainerFactory containerFactory = (JettyEmbeddedServletContainerFactory) container;
                    containerFactory.addServerCustomizers(new JettyServerCustomizer() {
                        @Override
                        public void customize(Server server) {
                            ServerConnector connector = new ServerConnector(server);
                            connector.setPort(httpPort);
                            server.addConnector(connector);
                        }
                    });
                }
            }

        };
    }

}