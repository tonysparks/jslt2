/*
 * see license.txt 
 */
package jslt2;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A user defined function
 * 
 * @author Tony
 *
 */
public interface Jslt2Function {

    /**
     * Executes the user defined function
     * 
     * @param input the input
     * @param args
     * @return the resulting {@link JsonNode} expression
     * @throws Jslt2Exception
     */
    public JsonNode execute(JsonNode input, JsonNode ... args) 
            throws Jslt2Exception;
}
