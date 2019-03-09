/*
 * see license.txt
 */
package jslt2.ast;

import java.util.List;

/**
 * @author Tony
 *
 */
public abstract class Decl extends Expr {

    public static class AsyncBlockDecl extends Decl {
        public List<LetDecl> lets;
        
        public AsyncBlockDecl(List<LetDecl> lets) {
            this.lets = becomeParentOf(lets);
        }
        
        @Override
        public void visit(ExprVisitor v) {
            v.visit(this);
        }
        
        @Override
        public Expr optimize() {
            for(int i = 0; i < lets.size(); i++) {
                LetDecl let = lets.get(i).optimize().as();
                lets.set(i, let);
            }
                     
            return this;
        }
    }
    
    public static class DefDecl extends Decl {

        public String identifier;
        public List<String> parameters;
        public List<LetDecl> lets;
        public Expr expr;
        
        public DefDecl(String identifier, List<String> parameters, List<LetDecl> lets, Expr expr) {
            this.identifier = identifier;
            this.parameters = parameters;
            this.lets = becomeParentOf(lets);
            this.expr = becomeParentOf(expr);
        }
        
        @Override
        public void visit(ExprVisitor v) {
            v.visit(this);
        }
        
        @Override
        public Expr optimize() {
            for(int i = 0; i < lets.size(); i++) {
                LetDecl let = lets.get(i).optimize().as();
                lets.set(i, let);
            }
            
            expr = expr.optimize();
            
            return this;
        }
    }
    
    
    public static class ImportDecl extends Decl {

        public final String library;
        public final String alias;
        
        public ImportDecl(String library, String alias) {
            this.library = library;
            this.alias = alias;
        }

        @Override
        public void visit(ExprVisitor v) {
            v.visit(this);
        }
        
        @Override
        public Expr optimize() {
            return this;
        }
        
    }
    
    public static class LetDecl extends Decl {

        public final String identifier;
        public Expr value;
        
        public LetDecl(String identifier, Expr value) {
            this.identifier = identifier;
            this.value = becomeParentOf(value);
        }

        @Override
        public void visit(ExprVisitor v) {
            v.visit(this);
        }
        
        @Override
        public Expr optimize() {
            value = value.optimize();
            return this;
        }
    }

}
