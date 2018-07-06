/*
 * see license.txt
 */
package jslt2.ast;

/**
 * @author Tony
 *
 */
public class IfExpr extends Expr {

    private Expr condition;
    private Expr thenExpr;
    private Expr elseExpr;
    
    /**
     * 
     */
    public IfExpr(Expr condition, Expr thenExpr, Expr elseExpr) {
        this.condition = condition;
        this.thenExpr = thenExpr;
        this.elseExpr = elseExpr;
    }
    
    /**
     * @return the condition
     */
    public Expr getCondition() {
        return condition;
    }
    
    /**
     * @return the thenExpr
     */
    public Expr getThenExpr() {
        return thenExpr;
    }
    
    /**
     * @return the elseExpr
     */
    public Expr getElseExpr() {
        return elseExpr;
    }
    
    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }

}
