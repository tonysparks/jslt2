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
    private Expr ifExpr;
    

    /**
     * @param condition
     * @param lets
     * @param keyExpr
     * @param valueExpr
     */
    public ForObjectExpr(Expr condition, List<LetExpr> lets, Expr keyExpr, Expr valueExpr, Expr ifExpr) {
        this.condition = becomeParentOf(condition);
        this.lets = becomeParentOf(lets);
        this.keyExpr = becomeParentOf(keyExpr);
        this.valueExpr = becomeParentOf(valueExpr);
        
        this.ifExpr = becomeParentOf(ifExpr);
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

    /**
     * @return the ifExpr
     */
    public Expr getIfExpr() {
        return ifExpr;
    }
    

    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }

}
