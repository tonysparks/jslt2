/*
 * see license.txt
 */
package jslt2.ast;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Tony
 *
 */
public class NumberExpr extends Expr {

    private JsonNode number;
    
    /**
     * 
     */
    public NumberExpr(JsonNode number) {
        this.number = number;
    }
    
    /**
     * @return the number
     */
    public JsonNode getNumber() {
        return number;
    }

    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }
}
