/*
 * see license.txt
 */
package jslt2.ast;

import java.util.List;

/**
 * @author Tony
 *
 */
public class ForArrayExpr extends Expr {

    private Expr condition;
    private List<LetExpr> lets;    
    private Expr valueExpr;
    
    

    /**
     * @param condition
     * @param lets
     * @param valueExpr
     */
    public ForArrayExpr(Expr condition, List<LetExpr> lets, Expr valueExpr) {
        this.condition = condition;
        this.lets = lets;
        this.valueExpr = valueExpr;
    }

    /**
     * @return the condition
     */
    public Expr getCondition() {
        return condition;
    }
    
    /**
     * @return the valueExpr
     */
    public Expr getValueExpr() {
        return valueExpr;
    }
    
    /**
     * @return the lets
     */
    public List<LetExpr> getLets() {
        return lets;
    }


    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }

}
