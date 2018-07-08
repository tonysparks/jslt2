/*
 * see license.txt 
 */
package jslt2;

import com.fasterxml.jackson.databind.JsonNode;

import jslt2.vm.Bytecode;

/**
 * @author Tony
 *
 */
public class Template {

    private Jslt2 runtime;
    private Bytecode bytecode;
    
    /**
     * @param runtime
     * @param bytecode
     */
    public Template(Jslt2 runtime, Bytecode bytecode) {
        this.runtime = runtime;
        this.bytecode = bytecode;
    }
    
    /**
     * Evaluates the template with the supplied input
     * 
     * @param input
     * @return the {@link JsonNode} result
     */
    public JsonNode eval(JsonNode input) {
        return this.runtime.eval(this.bytecode, input);
    }

}
