/*
 * see license.txt
 */
package jslt2.ast;

import java.util.List;

/**
 * @author Tony
 *
 */
public class FuncCallExpr extends Expr {

    private Expr object;
    private List<Expr> arguments;
    
    /**
     * 
     */
    public FuncCallExpr(Expr object, List<Expr> arguments) {
        this.object = object;
        this.arguments = arguments;
    }
    
    /**
     * @return the object
     */
    public Expr getObject() {
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
