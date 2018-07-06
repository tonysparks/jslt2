/*
 * see license.txt
 */
package jslt2.ast;

/**
 * @author Tony
 *
 */
public class DotExpr extends Expr {

    private Expr field;
    
    /**
     * 
     */
    public DotExpr(Expr field) {
        this.field = field;
    }

    /**
     * @return the field
     */
    public Expr getField() {
        return field;
    }
    
    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }

}
