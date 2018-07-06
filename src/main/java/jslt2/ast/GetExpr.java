/*
 * see license.txt
 */
package jslt2.ast;

/**
 * @author Tony
 *
 */
public class GetExpr extends Expr {

    private Expr object;
    private String identifier;
    
    /**
     * 
     */
    public GetExpr(Expr object, String identifier) {
        this.object = object;
        this.identifier = identifier;
    }

    /**
     * @return the object
     */
    public Expr getObject() {
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
