/*
 * see license.txt 
 */
package jslt2;

import com.fasterxml.jackson.databind.JsonNode;

import jslt2.vm.Bytecode;
import jslt2.vm.VM;

/**
 * The {@link Template#eval(JsonNode)} method is not thread-safe.  If you want to run multiple concurrent template transforms, each thread
 * must have its own {@link Template} instance.  You can easily do this by {@link Template#clone()}. 
 * 
 * @author Tony
 *
 */
public class Template {

    private Jslt2 runtime;
    private VM vm;
    private Bytecode bytecode;
    
    /**
     * @param runtime
     * @param bytecode
     */
    public Template(Jslt2 runtime, Bytecode bytecode) {
        this.runtime = runtime;
        this.bytecode = bytecode;
        
        this.vm = new VM(runtime);
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

    /**
     * Creates a clone of this {@link Template}
     */
    @Override
    public Template clone() {
        return new Template(this.runtime, this.bytecode);
    }
}
