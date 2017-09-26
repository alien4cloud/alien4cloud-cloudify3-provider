package alien4cloud.paas.cloudify3.configuration;

public interface ICloudConfigurationChangeListener {

    void onConfigurationChange(CloudConfiguration newConfiguration) throws Exception;
}
