/*
 * see license.txt
 */
package jslt2.ast;

/**
 * @author Tony
 *
 */
public class IdentifierExpr extends Expr {

    private String identifier;
    
    /**
     * 
     */
    public IdentifierExpr(String identifier) {
        this.identifier = identifier;
    }
    
    /**
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }

}
