/*
 * see license.txt
 */
package jslt2.ast;

import java.util.List;

/**
 * @author Tony
 *
 */
public class MacroCallExpr extends Expr {

    private IdentifierExpr object;
    private List<Expr> arguments;
    
    /**
     * 
     */
    public MacroCallExpr(IdentifierExpr object, List<Expr> arguments) {
        this.object = becomeParentOf(object);
        this.arguments = becomeParentOf(arguments);
    }
    
    /**
     * @return the object
     */
    public IdentifierExpr getObject() {
        return object;
    }
    
    /**
     * @return the arguments
     */
    public List<Expr> getArguments() {
        return arguments;
    }

    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }

}
