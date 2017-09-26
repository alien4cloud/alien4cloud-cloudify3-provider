package alien4cloud.paas.cloudify3.restclient;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import alien4cloud.paas.cloudify3.model.Blueprint;
import alien4cloud.paas.cloudify3.shared.restclient.BlueprintClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestBlueprintClient extends AbstractRestClientTest {

    private static BlueprintClient blueprintClient;

    @BeforeClass
    public static void before() {
        initializeContext();
        blueprintClient = getApiClient().getBlueprintClient();
    }

    @Test
    public void test() {
        String blueprintId = "testRest";

        Blueprint[] blueprints = blueprintClient.list(0, 1000).getItems();
        try {
            Assert.assertEquals(0, blueprints.length);
            blueprintClient.create(blueprintId, "src/test/resources/restclient/blueprint/blueprint.yaml");
            blueprints = blueprintClient.list(0, 1000).getItems();
            Assert.assertEquals(1, blueprints.length);
            for (Blueprint blueprint : blueprints) {
                log.info(blueprint.getId() + " = " + blueprint);
            }
            Blueprint blueprint = blueprintClient.read(blueprintId);
            Assert.assertNotNull(blueprint);
        } finally {
            blueprintClient.delete(blueprintId);
            blueprints = blueprintClient.list(0, 1000).getItems();
            Assert.assertEquals(0, blueprints.length);
        }
    }
}
