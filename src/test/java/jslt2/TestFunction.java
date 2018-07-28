/*
 * see license.txt 
 */
package jslt2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;

/**
 * @author Tony
 *
 */
public class TestFunction implements Jslt2Function {
    
    @Override
    public String name() {
        return "test";
    }
    
    @Override
    public JsonNode execute(JsonNode input, JsonNode... args) throws Jslt2Exception {
        return IntNode.valueOf(42);
    }

}
