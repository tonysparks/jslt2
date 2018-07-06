/*
 * see license.txt
 */
package jslt2.ast;

import java.util.List;

/**
 * @author Tony
 *
 */
public class ForObjectExpr extends Expr {

    private Expr condition;
    private List<LetExpr> lets;
    private Expr keyExpr;
    private Expr valueExpr;
    
    

    /**
     * @param condition
     * @param lets
     * @param keyExpr
     * @param valueExpr
     */
    public ForObjectExpr(Expr condition, List<LetExpr> lets, Expr keyExpr, Expr valueExpr) {
        this.condition = condition;
        this.lets = lets;
        this.keyExpr = keyExpr;
        this.valueExpr = valueExpr;
    }

    /**
     * @return the condition
     */
    public Expr getCondition() {
        return condition;
    }
    
    /**
     * @return the keyExpr
     */
    public Expr getKeyExpr() {
        return keyExpr;
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
