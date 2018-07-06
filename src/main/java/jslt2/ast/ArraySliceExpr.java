/*
 * see license.txt
 */
package jslt2.ast;

/**
 * @author Tony
 *
 */
public class ArraySliceExpr extends Expr {

    private Expr array;
    private Expr startExpr;
    private Expr endExpr;
    
    /**
     * 
     */
    public ArraySliceExpr(Expr array, Expr startExpr, Expr endExpr) {
        this.array = array;
        this.startExpr = startExpr;
        this.endExpr = endExpr;
    }
    
    /**
     * @return the array
     */
    public Expr getArray() {
        return array;
    }
    
    /**
     * @return the startExpr
     */
    public Expr getStartExpr() {
        return startExpr;
    }
    
    /**
     * @return the endExpr
     */
    public Expr getEndExpr() {
        return endExpr;
    }

    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }

}
