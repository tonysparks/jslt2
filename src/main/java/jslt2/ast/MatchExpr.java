/*
 * see license.txt
 */
package jslt2.ast;

import java.util.List;

/**
 * @author Tony
 *
 */
public class MatchExpr extends Expr {

    private List<Expr> fields;
    
    /**
     * 
     */
    public MatchExpr(List<Expr> fields) {
        this.fields = fields;
    }

    /**
     * @return the fields
     */
    public List<Expr> getFields() {
        return fields;
    }
    
    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }

}
