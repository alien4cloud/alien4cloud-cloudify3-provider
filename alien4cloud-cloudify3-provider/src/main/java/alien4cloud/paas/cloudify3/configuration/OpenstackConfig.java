package alien4cloud.paas.cloudify3.configuration;

import alien4cloud.ui.form.annotation.FormLabel;
import alien4cloud.ui.form.annotation.FormProperties;
import alien4cloud.ui.form.annotation.FormPropertyDefinition;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@FormProperties({ "username", "password", "auth_url", "region", "tenant_name", "project_id", "project_name", "user_domain_name", "project_domain_name",
        "insecure", "ca_cert", "nova_url", "neutron_url" })
public class OpenstackConfig {

    @FormLabel("username")
    @FormPropertyDefinition(description = "User to authenticate to KeyStone with.", type = "string", isRequired = true)
    private String username;

    @FormLabel("password secret key")
    @FormPropertyDefinition(description = "The key stored in Cloudify's secret vault for the password to authenticate to KeyStone with.", type = "string", isRequired = true)
    private String password;

    @FormLabel("auth_url")
    @FormPropertyDefinition(description = "Keystone's URL (used for authentication).", type = "string", isRequired = true)
    private String auth_url;

    @FormLabel("region")
    @FormPropertyDefinition(description = "The region's name (optional if only one region exists).", type = "string")
    private String region;

    @FormLabel("tenant_name")
    @FormPropertyDefinition(description = "Name of tenant.", type = "string")
    private String tenant_name;

    @FormLabel("project id")
    @FormPropertyDefinition(description = "ID of project to operate on.", type = "string")
    private String project_id;

    @FormLabel("project name")
    @FormPropertyDefinition(description = "Name of project to operate on.", type = "string")
    private String project_name;

    @FormLabel("user domain name")
    @FormPropertyDefinition(description = "Domain name to operate on.", type = "string")
    private String user_domain_name;

    @FormLabel("project domain name")
    @FormPropertyDefinition(description = "Project domain name to operate on.", type = "string")
    private String project_domain_name;

    @FormLabel("insecure")
    @FormPropertyDefinition(description = "If true, SSL validation is skipped.", type = "boolean")
    private Boolean insecure;

    @FormLabel("ca cert")
    @FormPropertyDefinition(description = "Path to CA certificate to validate OpenStack's endpoint with.", type = "string")
    private String ca_cert;

    @FormLabel("nova url")
    @FormPropertyDefinition(description = "DEPRECATED - use 'custom_configuration' and 'bypass_url' instead.", type = "string")
    private String nova_url;

    @FormLabel("neutron url")
    @FormPropertyDefinition(description = "DEPRECATED - use 'custom_configuration' and 'endpoint_url' instead.", type = "string")
    private String neutron_url;
}
