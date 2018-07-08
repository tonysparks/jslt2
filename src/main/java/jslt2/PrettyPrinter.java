/*
 * see license.txt
 */
package jslt2;

import java.util.List;

import jslt2.ast.ArrayExpr;
import jslt2.ast.ArrayIndexExpr;
import jslt2.ast.ArraySliceExpr;
import jslt2.ast.BinaryExpr;
import jslt2.ast.BooleanExpr;
import jslt2.ast.DefExpr;
import jslt2.ast.DotExpr;
import jslt2.ast.ElseExpr;
import jslt2.ast.Expr;
import jslt2.ast.ForArrayExpr;
import jslt2.ast.ForObjectExpr;
import jslt2.ast.FuncCallExpr;
import jslt2.ast.GetExpr;
import jslt2.ast.IdentifierExpr;
import jslt2.ast.IfExpr;
import jslt2.ast.ImportExpr;
import jslt2.ast.ImportGetExpr;
import jslt2.ast.LetExpr;
import jslt2.ast.MatchExpr;
import jslt2.ast.VariableExpr;
import jslt2.ast.NodeVisitor;
import jslt2.ast.NullExpr;
import jslt2.ast.NumberExpr;
import jslt2.ast.ObjectExpr;
import jslt2.ast.ProgramExpr;
import jslt2.ast.StringExpr;
import jslt2.ast.UnaryExpr;
import jslt2.util.Tuple;

/**
 * @author Tony
 *
 */
public class PrettyPrinter implements NodeVisitor {

    private int ident;
    private StringBuilder line;
    
    /**
     * 
     */
    public PrettyPrinter() {
        this.line = new StringBuilder();
        
        printIndent();
    }

    private void printIndent() {
        for(int i = 0; i < this.ident; i++) {
            this.line.append("   ");
        }
    }
    
    private void print(Object ... args) {
        for(int i = 0; i < args.length; i++) {
            if(i > 0) {
                this.line.append(" ");
            }
            this.line.append(args[i]);
        }        
    }
    
    private void println(Object ... args) {
        print(args); 
        System.out.println(this.line);
        this.line.delete(0, this.line.length());
        printIndent();
    }

    private void indent() {
        this.ident++;
    }
    
    private void unindent() {
        this.ident--;
    }
    
    private void printBlockStart(String str) {
        println(str);
        indent();
        printIndent();
    }
    
    private void printBlockEnd(String str) {
        unindent();
        println();
        print(str);
        //println();
    }
    
    @Override
    public void visit(NullExpr expr) {
        print("null");
    }

    @Override
    public void visit(BooleanExpr expr) {
        print(expr.getBoolean() ? "true" : "false");
    }

    @Override
    public void visit(NumberExpr expr) {
        print(expr.getNumber());
    }
    
    @Override
    public void visit(StringExpr expr) {
        print("\"" + expr.getString() + "\"");
    }

    @Override
    public void visit(ObjectExpr expr) {
        printBlockStart("{");
        
        List<LetExpr> lets = expr.getLets();
        for(LetExpr let : lets) {
            print(let.getIdentifier(), " = ");
            let.getValue().visit(this);
            println();
        }
        println();
        
        ForObjectExpr forExpr = expr.getForObjectExpr();
        if(forExpr != null) {
            forExpr.visit(this);
        }
        else {        
            List<Tuple<Expr, Expr>> fields = expr.getFields();
            for(int i = 0; i < fields.size(); i++) {            
                Tuple<Expr, Expr> t = fields.get(i);
                t.getFirst().visit(this);
                print(" : ");
                t.getSecond().visit(this);
                
                if(i < fields.size() - 1) {
                    print(",");
                }
                println();
            }
        }
        printBlockEnd("}");
    }

    @Override
    public void visit(ArrayExpr expr) {
        print("[");
        
        ForArrayExpr forArray = expr.getForExpr();
        if(forArray != null) {
            forArray.visit(this);
        }
        else {
            List<Expr> elements = expr.getElements();
            for(int i = 0; i < elements.size(); i++) {
                if(i > 0) {
                    print(", ");
                }
                
                elements.get(i).visit(this);
            }
        }        
        print("]");
    }

    @Override
    public void visit(IfExpr expr) {
        print("if (");
        expr.getCondition().visit(this);
        print(")");
        indent();
        println();
        expr.getThenExpr().visit(this);
        unindent();
                
        ElseExpr elseExpr = expr.getElseExpr();
        if(elseExpr != null) {
            elseExpr.visit(this);
        }

    }
    
    @Override
    public void visit(ElseExpr expr) {
        Expr elseExpr = expr.getExpr();
        if(elseExpr != null) {
            println();
            print("else");
            indent();
            println();
            elseExpr.visit(this);
            unindent();
        }
    }

    @Override
    public void visit(ForObjectExpr expr) {
        print("for (");
        expr.getCondition().visit(this);
        print(")");
        print(" ");
        
        expr.getKeyExpr().visit(this);
        print(" : ");       
        expr.getValueExpr().visit(this);
    }

    @Override
    public void visit(ForArrayExpr expr) {
        print("for", "(");
        expr.getCondition().visit(this);
        print(") ");
        expr.getValueExpr().visit(this);
    }

    @Override
    public void visit(LetExpr expr) {
        print("let", expr.getIdentifier(), "= ");
        expr.getValue().visit(this);
        println();
    }

    @Override
    public void visit(DefExpr expr) {
        print("def", expr.getIdentifier(), "(");
        List<String> params = expr.getParameters();
        for(int i = 0; i < params.size(); i++) {
            if(i > 0) {
                print(", ");
            }
            
            print(params.get(i));
        }
        print(")");
        printBlockStart("");
        expr.getExpr().visit(this);
        printBlockEnd("");

    }

    @Override
    public void visit(FuncCallExpr expr) {
        expr.getObject().visit(this);
        print("(");
        List<Expr> args = expr.getArguments();
        for(int i = 0; i < args.size(); i++) {
            if(i > 0) {
                print(",");
            }
            
            args.get(i).visit(this);
        }
        print(")");
    }

    @Override
    public void visit(IdentifierExpr expr) {
        print(expr.getIdentifier());
    }

    @Override
    public void visit(VariableExpr expr) {
        print(expr.getVariable());
    }

    @Override
    public void visit(ArraySliceExpr expr) {
        expr.getArray().visit(this);
        print("[");
        expr.getStartExpr().visit(this);
        Expr endExpr = expr.getEndExpr();
        if(endExpr != null) {
            print(" : ");
            endExpr.visit(this);
        }
        print("]");
    }
    
    @Override
    public void visit(ArrayIndexExpr expr) {
        expr.getArray().visit(this);
        print("[");
        expr.getIndex().visit(this);
        print("]");
    }

    @Override
    public void visit(GetExpr expr) {
        expr.getObject().visit(this);
        print("." + expr.getIdentifier());        
    }

    /* (non-Javadoc)
     * @see jslt2.ast.NodeVisitor#visit(jslt2.ast.ImportGetExpr)
     */
    @Override
    public void visit(ImportGetExpr expr) {
        // TODO Auto-generated method stub

    }

    @Override
    public void visit(ImportExpr expr) {
        print("import", expr.getLibrary());
        String alias = expr.getAlias();
        if(alias != null) {
            print(" as", alias);
        }
        
        println();
    }

    @Override
    public void visit(ProgramExpr expr) {
        expr.getImports().forEach(imp -> imp.visit(this));
        expr.getLets().forEach(let -> let.visit(this));
        expr.getDefs().forEach(def -> def.visit(this));
        
        Expr e = expr.getExpr();
        if(e != null) {
            e.visit(this);
        }
        
        println();
    }

    @Override
    public void visit(UnaryExpr expr) {
        print(expr.getOperator().getText());
        expr.getExpr().visit(this);
    }

    @Override
    public void visit(BinaryExpr expr) {
        expr.getLeft().visit(this);
        print(" " + expr.getOperator().getText() + " ");
        expr.getRight().visit(this);
    }
        
    @Override
    public void visit(DotExpr expr) {
        print(".");
        Expr field = expr.getField();
        if(field != null) {
            field.visit(this);
        }        
    }
    
    @Override
    public void visit(MatchExpr expr) {
        print("*");
        List<Expr> fields = expr.getFields();
        for(int i = 0; i < fields.size(); i++) {
            if(i == 0) {
                print(" - ");
            }
            
            if(i > 0) {
                print(", ");
            }
            
            fields.get(i).visit(this);
        }
        
    }

}
