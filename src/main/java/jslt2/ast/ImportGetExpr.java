/*
 * see license.txt
 */
package jslt2.ast;

/**
 * @author Tony
 *
 */
public class ImportGetExpr extends Expr {

    private IdentifierExpr object;
    private String identifier;
    
    /**
     * 
     */
    public ImportGetExpr(IdentifierExpr object, String identifier) {
        this.object = object;
        this.identifier = identifier;
    }

    /**
     * @return the object
     */
    public IdentifierExpr getObject() {
        return object;
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
