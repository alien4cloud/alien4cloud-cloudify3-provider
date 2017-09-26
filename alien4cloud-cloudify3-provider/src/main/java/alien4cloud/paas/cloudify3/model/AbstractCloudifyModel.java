package alien4cloud.paas.cloudify3.model;

import lombok.extern.slf4j.Slf4j;
import alien4cloud.rest.utils.JsonUtil;

import com.fasterxml.jackson.core.JsonProcessingException;

@Slf4j
public class AbstractCloudifyModel {

    public String toString() {
        try {
            return JsonUtil.toString(this);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize instance of " + this.getClass().getSimpleName(), e);
            return "Failed to serialize instance of " + this.getClass().getSimpleName();
        }
    }
}
