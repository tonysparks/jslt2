/*
 * see license.txt 
 */
package jslt2.ast;

import java.util.List;

/**
 * @author Tony
 *
 */
public class ElseExpr extends Expr {

    private List<LetExpr> lets;
    private Expr expr;
    
    /**
     * 
     */
    public ElseExpr(List<LetExpr> lets, Expr expr) {
        this.lets = lets;
        this.expr = expr;
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
