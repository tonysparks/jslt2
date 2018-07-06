/*
 * see license.txt
 */
package jslt2.ast;

/**
 * @author Tony
 *
 */
public interface NodeVisitor {

    void visit(NullExpr expr);
    void visit(BooleanExpr expr);
    void visit(NumberExpr expr);
    void visit(StringExpr expr);
    void visit(ObjectExpr expr);
    void visit(ArrayExpr expr);
    
    void visit(IfExpr expr);
    void visit(ForObjectExpr expr);
    void visit(ForArrayExpr expr);
    void visit(LetExpr expr);
    void visit(DefExpr expr);
    void visit(FuncCallExpr expr);
    
    void visit(IdentifierExpr expr);
    void visit(VariableExpr expr);
    
    void visit(ArraySliceExpr expr);
    void visit(GetExpr expr);
    void visit(ImportGetExpr expr);
    
    void visit(ImportExpr expr);
    void visit(ProgramExpr expr);
    
    void visit(UnaryExpr expr);
    void visit(BinaryExpr expr);
    void visit(DotExpr expr);
    void visit(MatchExpr expr);
}
