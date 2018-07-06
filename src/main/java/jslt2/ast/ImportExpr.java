/*
 * see license.txt
 */
package jslt2.ast;

/**
 * @author Tony
 *
 */
public class ImportExpr extends Expr {

    private String library;
    private String alias;
    
    /**
     * 
     */
    public ImportExpr(String library, String alias) {
        this.library = library;
        this.alias = alias;
    }
    /**
     * @return the library
     */
    public String getLibrary() {
        return library;
    }
    
    /**
     * @return the alias
     */
    public String getAlias() {
        return alias;
    }

    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }

}
