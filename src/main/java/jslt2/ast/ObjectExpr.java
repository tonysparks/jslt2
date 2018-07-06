/*
 * see license.txt
 */
package jslt2.ast;

import java.util.List;

import jslt2.util.Tuple;

/**
 * @author Tony
 *
 */
public class ObjectExpr extends Expr {

    private List<LetExpr> lets;
    private ForObjectExpr forObjectExpr;
    private List<Tuple<Expr, Expr>> fields;
    
    /**
     * 
     */
    public ObjectExpr(List<LetExpr> lets, ForObjectExpr forObjectExpr, List<Tuple<Expr, Expr>> fields) {
        this.lets = lets;
        this.forObjectExpr = forObjectExpr;
        this.fields = fields;
    }
    
    /**
     * @return the lets
     */
    public List<LetExpr> getLets() {
        return lets;
    }
    
    /**
     * @return the forObjectExpr
     */
    public ForObjectExpr getForObjectExpr() {
        return forObjectExpr;
    }
    
    /**
     * @return the fields
     */
    public List<Tuple<Expr, Expr>> getFields() {
        return fields;
    }

    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }

}
