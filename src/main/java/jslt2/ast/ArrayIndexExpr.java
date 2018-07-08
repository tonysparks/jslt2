/*
 * see license.txt
 */
package jslt2.ast;

/**
 * @author Tony
 *
 */
public class ArrayIndexExpr extends Expr {

    private Expr array;
    private Expr index;
        
    /**
     * 
     */
    public ArrayIndexExpr(Expr array, Expr index) {
        this.array = array;
        this.index = index;
    }
    
    /**
     * @return the array
     */
    public Expr getArray() {
        return array;
    }
    
    /**
     * @return the index
     */
    public Expr getIndex() {
        return index;
    }
    
    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }

}
