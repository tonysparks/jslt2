/*
 * see license.txt
 */
package jslt2.ast;

import jslt2.parser.tokens.TokenType;

/**
 * @author Tony
 *
 */
public class BinaryExpr extends Expr {

    private Expr left;
    private TokenType operator;
    private Expr right;
    
    /**
     * 
     */
    public BinaryExpr(Expr left, TokenType operator, Expr right) {
        this.left = becomeParentOf(left);
        this.operator = operator;
        this.right = becomeParentOf(right);
    }
    
    /**
     * @return the left
     */
    public Expr getLeft() {
        return left;
    }
    
    /**
     * @return the operator
     */
    public TokenType getOperator() {
        return operator;
    }
    
    /**
     * @return the right
     */
    public Expr getRight() {
        return right;
    }

    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }

}
