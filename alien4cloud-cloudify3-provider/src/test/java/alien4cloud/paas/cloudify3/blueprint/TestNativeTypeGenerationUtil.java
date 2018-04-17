package alien4cloud.paas.cloudify3.blueprint;

import alien4cloud.paas.cloudify3.configuration.OpenstackConfig;
import com.google.common.collect.Lists;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TestNativeTypeGenerationUtil {

    NativeTypeGenerationUtil util = new NativeTypeGenerationUtil(null, null, null, null);

    @Test
    public void formatObjectProperties() {
        OpenstackConfig config = new OpenstackConfig();
        config.setUsername("username");
        config.setPassword("mypassword");
        config.setAuth_url("http://localhost");
        String output = util.formatObjectProperties(3, config, Lists.newArrayList("password"));
        StringBuilder expected = new StringBuilder();
        expected.append("      username: username\n");
        expected.append("      auth_url: http://localhost\n");
        Assertions.assertThat(output).isEqualTo(expected.toString());
    }
}
