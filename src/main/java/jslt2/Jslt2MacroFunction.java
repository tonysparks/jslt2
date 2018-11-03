/*
 * see license.txt 
 */
package jslt2;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import jslt2.vm.Bytecode;
import jslt2.vm.VM;

/**
 * A Macro function
 * 
 * @author Tony
 *
 */
public interface Jslt2MacroFunction {

    default public String name() {
        return "<unknown>";
    }
    
    /**
     * Executes the macro function
     * 
     * @param input the input
     * @param args
     * @return the resulting {@link JsonNode} expression
     * @throws Jslt2Exception
     */
    public JsonNode execute(VM vm, JsonNode input, List<Bytecode> args) 
            throws Jslt2Exception;
}
