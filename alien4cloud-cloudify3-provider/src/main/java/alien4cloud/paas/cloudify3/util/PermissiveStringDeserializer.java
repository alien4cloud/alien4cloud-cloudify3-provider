package alien4cloud.paas.cloudify3.util;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * Allow parsing of elements that should be string but are sometimes not correctly formatted (happens in internal cfy logs).
 */
public class PermissiveStringDeserializer extends JsonDeserializer<String> {
    @Override
    public String deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        try {
            JsonNode jsonNode = jsonParser.readValueAs(JsonNode.class);
            if (jsonNode instanceof TextNode) {
                return ((TextNode) jsonNode).textValue();
            }
        } catch (JsonMappingException jme) {
        }
        return null;
    }
}
