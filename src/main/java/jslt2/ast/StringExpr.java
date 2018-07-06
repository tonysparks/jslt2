/*
 * see license.txt
 */
package jslt2.ast;

/**
 * @author Tony
 *
 */
public class StringExpr extends Expr {

    private String string;
    
    /**
     * 
     */
    public StringExpr(String string) {
        this.string = string;
    }

    /**
     * @return the string
     */
    public String getString() {
        return string;
    }
    
    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }
}
