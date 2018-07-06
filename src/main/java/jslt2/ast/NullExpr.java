/*
 * see license.txt
 */
package jslt2.ast;

/**
 * @author Tony
 *
 */
public class NullExpr extends Expr {
    public NullExpr() {
    }
    
    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }
}
