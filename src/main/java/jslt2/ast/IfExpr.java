/*
 * see license.txt
 */
package jslt2.ast;

import java.util.List;

/**
 * @author Tony
 *
 */
public class IfExpr extends Expr {

    private List<LetExpr> lets;
    private Expr condition;
    private Expr thenExpr;
    private ElseExpr elseExpr;
    
    /**
     * 
     */
    public IfExpr(List<LetExpr> lets, Expr condition, Expr thenExpr, ElseExpr elseExpr) {
        this.lets = lets;
        this.condition = condition;
        this.thenExpr = thenExpr;
        this.elseExpr = elseExpr;
    }
    
    /**
     * @return the lets
     */
    public List<LetExpr> getLets() {
        return lets;
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
    public ElseExpr getElseExpr() {
        return elseExpr;
    }
    
    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }

}
