package alien4cloud.paas.cloudify3.util.mapping;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PropertySubMapping {
    private SourceMapping sourceMapping;
    private TargetMapping targetMapping;
}
