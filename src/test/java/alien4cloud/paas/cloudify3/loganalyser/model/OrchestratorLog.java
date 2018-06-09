package alien4cloud.paas.cloudify3.loganalyser.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrchestratorLog {

    // FIXME : don't know why the id is not deserialized
    //    private String id;
    public long timestamp;
    public String content;
    public String pattern;

    /**
     * We consider that logs are same if their pattern are same.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OrchestratorLog that = (OrchestratorLog) o;

        return content != null ? content.equals(that.content) : that.content == null;
    }

    /**
     * We consider that logs are same if their pattern are same.
     */
    @Override
    public int hashCode() {
        return content != null ? content.hashCode() : 0;
    }
}
