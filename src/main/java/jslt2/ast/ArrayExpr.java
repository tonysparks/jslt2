/*
 * see license.txt
 */
package jslt2.ast;

import java.util.List;

/**
 * @author Tony
 *
 */
public class ArrayExpr extends Expr {
        
    private ForArrayExpr forExpr;
    private List<Expr> elements;
    
    /**
     * 
     */
    public ArrayExpr(ForArrayExpr forExpr, List<Expr> elements) {
        this.forExpr = forExpr;
        this.elements = elements;
    }
    
    
    /**
     * @return the forExpr
     */
    public ForArrayExpr getForExpr() {
        return forExpr;
    }
    
    /**
     * @return the elements
     */
    public List<Expr> getElements() {
        return elements;
    }

    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }

}
