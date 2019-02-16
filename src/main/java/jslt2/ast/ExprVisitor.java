/*
 * see license.txt
 */
package jslt2.ast;

import jslt2.ast.Expr.*;
import jslt2.ast.Decl.*;

/**
 * @author Tony
 *
 */
public interface ExprVisitor {

    void visit(AsyncBlockDecl expr);
    
    void visit(NullExpr expr);
    void visit(BooleanExpr expr);
    void visit(NumberExpr expr);
    void visit(StringExpr expr);
    void visit(ObjectExpr expr);
    void visit(ArrayExpr expr);
    
    void visit(IfExpr expr);
    void visit(ElseExpr expr);
    void visit(GroupExpr expr);
    void visit(ForObjectExpr expr);
    void visit(ForArrayExpr expr);
    void visit(LetDecl expr);
    void visit(DefDecl expr);
    void visit(FuncCallExpr expr);
    void visit(MacroCallExpr expr);
    
    void visit(IdentifierExpr expr);
    void visit(VariableExpr expr);
    
    void visit(ArraySliceExpr expr);
    void visit(ArrayIndexExpr expr);
    void visit(GetExpr expr);
    
    void visit(ImportDecl expr);
    void visit(ProgramExpr expr);
    void visit(ModuleExpr expr);
    
    void visit(UnaryExpr expr);
    void visit(BinaryExpr expr);
    void visit(DotExpr expr);
    void visit(MatchExpr expr);
}
