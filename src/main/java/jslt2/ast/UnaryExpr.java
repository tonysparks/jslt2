/*
 * see license.txt
 */
package jslt2.ast;

import jslt2.parser.tokens.TokenType;

/**
 * @author Tony
 *
 */
public class UnaryExpr extends Expr {

    private TokenType operator;
    private Expr expr;
    
    /**
     * 
     */
    public UnaryExpr(TokenType operator, Expr expr) {
        this.operator = operator;
        this.expr = expr;
    }
    
    /**
     * @return the operator
     */
    public TokenType getOperator() {
        return operator;
    }
    
    /**
     * @return the expr
     */
    public Expr getExpr() {
        return expr;
    }

    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }

}
