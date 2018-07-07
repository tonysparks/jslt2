/*
 * see license.txt 
 */
package jslt2;

import com.fasterxml.jackson.databind.JsonNode;

import jslt2.ast.ProgramExpr;

/**
 * @author Tony
 *
 */
public class Template {

    private Jslt2 runtime;
    private ProgramExpr expr;
    
    /**
     * @param runtime
     * @param expr
     */
    public Template(Jslt2 runtime, ProgramExpr expr) {
        this.runtime = runtime;
        this.expr = expr;
    }
    
    /**
     * Evaluates the template with the supplied input
     * 
     * @param input
     * @return the {@link JsonNode} result
     */
    public JsonNode eval(JsonNode input) {
        return this.runtime.eval(this.expr, input);
    }

}
