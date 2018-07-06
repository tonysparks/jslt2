/*
 * see license.txt
 */
package jslt2.ast;

/**
 * @author Tony
 *
 */
public class LetExpr extends Expr {

    private String identifier;
    private Expr value;
    
    /**
     * 
     */
    public LetExpr(String identifier, Expr value) {
        this.identifier = identifier;
        this.value = value;
    }
    
    /**
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }
    
    /**
     * @return the value
     */
    public Expr getValue() {
        return value;
    }

    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }

}
