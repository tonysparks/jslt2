/*
 * see license.txt 
 */
package jslt2;

import com.fasterxml.jackson.databind.JsonNode;

import jslt2.vm.Bytecode;
import jslt2.vm.VM;

/**
 * @author Tony
 *
 */
public class Template {

    private VM vm;
    private Bytecode bytecode;
    
    /**
     * @param runtime
     * @param bytecode
     */
    public Template(VM vm, Bytecode bytecode) {
        this.vm = vm;
        this.bytecode = bytecode;
    }
    
    /**
     * Evaluates the template with the supplied input
     * 
     * @param input
     * @return the {@link JsonNode} result
     */
    public JsonNode eval(JsonNode input) {
        return this.vm.execute(this.bytecode, input);
    }

}
