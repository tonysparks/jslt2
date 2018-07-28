/*
 * see license.txt
 */
package jslt2.ast;

import java.util.List;

/**
 * @author Tony
 *
 */
public class ProgramExpr extends ModuleExpr {

    /**
     * @param imports
     * @param lets
     * @param defs
     * @param expr
     */
    public ProgramExpr(List<ImportExpr> imports, List<LetExpr> lets, List<DefExpr> defs, Expr expr) {
        super(imports, lets, defs, expr);
    }

    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }

}
