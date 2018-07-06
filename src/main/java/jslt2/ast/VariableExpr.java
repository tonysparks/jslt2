/*
 * see license.txt
 */
package jslt2.ast;

/**
 * @author Tony
 *
 */
public class VariableExpr extends Expr {

    private String variable;
    
    /**
     * 
     */
    public VariableExpr(String variable) {
        this.variable = variable;
    }
    
    /**
     * @return the variable
     */
    public String getVariable() {
        return variable;
    }

    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }

}
