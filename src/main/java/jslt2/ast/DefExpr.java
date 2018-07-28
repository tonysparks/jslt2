/*
 * see license.txt
 */
package jslt2.ast;

import java.util.List;

/**
 * @author Tony
 *
 */
public class DefExpr extends Expr {

    private String identifier;
    private List<String> parameters;
    private List<LetExpr> lets;
    private Expr expr;
    
    /**
     * 
     */
    public DefExpr(String identifier, List<String> parameters, List<LetExpr> lets, Expr expr) {
        this.identifier = identifier;
        this.parameters = parameters;
        this.lets = becomeParentOf(lets);
        this.expr = becomeParentOf(expr);
    }
    
    /**
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }
    
    /**
     * @return the parameters
     */
    public List<String> getParameters() {
        return parameters;
    }
    
    /**
     * @return the lets
     */
    public List<LetExpr> getLets() {
        return lets;
    }
    
    /**
     * @return the expr
     */
    public Expr getExpr() {
        return expr;
    }

    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }

}
