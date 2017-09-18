package org.alien4cloud.cfy.logs;

import java.io.IOException;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
    private static final String[] SEARCH_LOCATIONS = new String[] { "file:./config/", "file:./", "classpath:/config/", "classpath:/" };

    public static void main(String[] args) throws Exception {
        System.setProperty("spring.config.name", "alien4cloud-cfy-logs-config");
        SpringApplication.run(LogServerApplication.class, args);
    }

    @Bean
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
}