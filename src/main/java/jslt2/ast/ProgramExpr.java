/*
 * see license.txt
 */
package jslt2.ast;

import java.util.List;

/**
 * @author Tony
 *
 */
public class ProgramExpr extends Expr {

    private List<ImportExpr> imports;
    private List<LetExpr> lets;
    private List<DefExpr> defs;
    private Expr expr;

    

    /**
     * @param imports
     * @param lets
     * @param defs
     * @param expr
     */
    public ProgramExpr(List<ImportExpr> imports, List<LetExpr> lets, List<DefExpr> defs, Expr expr) {
        this.imports = imports;
        this.lets = lets;
        this.defs = defs;
        this.expr = expr;
    }

    /**
     * @return the imports
     */
    public List<ImportExpr> getImports() {
        return imports;
    }

    /**
     * @return the lets
     */
    public List<LetExpr> getLets() {
        return lets;
    }

    /**
     * @return the defs
     */
    public List<DefExpr> getDefs() {
        return defs;
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
