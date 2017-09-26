package alien4cloud.paas.cloudify3.shared.model;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.mapping.IndexType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Keeps information on a alien to log server connexion.
 */
@NoArgsConstructor
@AllArgsConstructor
@ESObject(all = false)
@Getter
@Setter
public class LogClientRegistration {
    @Id
    private String logServerUrl;
    @StringField(indexType = IndexType.no)
    private String registrationId;
}
