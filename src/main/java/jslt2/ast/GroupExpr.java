/*
 * see license.txt
 */
package jslt2.ast;

/**
 * @author Tony
 *
 */
public class GroupExpr extends Expr {

    private Expr expr;
    
    /**
     * 
     */
    public GroupExpr(Expr expr) {
        this.expr = expr;
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
