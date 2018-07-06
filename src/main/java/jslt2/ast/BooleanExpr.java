/*
 * see license.txt
 */
package jslt2.ast;

/**
 * @author Tony
 *
 */
public class BooleanExpr extends Expr {

    private boolean bool;
    
    /**
     * 
     */
    public BooleanExpr(boolean bool) {
        this.bool = bool;
    }
    
    public boolean getBoolean() {
        return this.bool;
    }
    
    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }

}
