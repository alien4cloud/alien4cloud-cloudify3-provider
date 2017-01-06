package alien4cloud.paas.cloudify3.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListResponse<T> {

    private T[] items;

    private MetaData metaData;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class MetaData {
        private Pagination pagination;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Pagination {
        private long total;
        private long offset;
        private long size;
    }
}
