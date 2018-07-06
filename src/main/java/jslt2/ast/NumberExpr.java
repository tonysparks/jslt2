/*
 * see license.txt
 */
package jslt2.ast;

/**
 * @author Tony
 *
 */
public class NumberExpr extends Expr {

    private double number;
    
    /**
     * 
     */
    public NumberExpr(double number) {
        this.number = number;
    }
    
    /**
     * @return the number
     */
    public double getNumber() {
        return number;
    }

    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }
}
