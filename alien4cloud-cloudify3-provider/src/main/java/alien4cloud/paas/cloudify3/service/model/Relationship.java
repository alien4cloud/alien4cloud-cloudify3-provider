package alien4cloud.paas.cloudify3.service.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode
@Getter
@Setter
@AllArgsConstructor
public class Relationship {
    private String id;
    private String source;
    private String target;
}
