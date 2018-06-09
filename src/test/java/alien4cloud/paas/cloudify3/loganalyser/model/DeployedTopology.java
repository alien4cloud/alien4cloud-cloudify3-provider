package alien4cloud.paas.cloudify3.loganalyser.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DeployedTopology {

    public String id;
    public String archiveName;
    public String environmentId;

}
