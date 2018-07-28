/*
 * see license.txt
 */
package jslt2.ast;

import java.util.List;

/**
 * @author Tony
 *
 */
public class ModuleExpr extends Expr {

    private List<ImportExpr> imports;
    private List<LetExpr> lets;
    private List<DefExpr> defs;
    

    /**
     * @param imports
     * @param lets
     * @param defs
     */
    public ModuleExpr(List<ImportExpr> imports, List<LetExpr> lets, List<DefExpr> defs) {
        this.imports = imports;
        this.lets = lets;
        this.defs = defs;
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

    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }

}
