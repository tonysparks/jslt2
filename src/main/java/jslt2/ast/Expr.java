/*
 * see license.txt
 */
package jslt2.ast;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.LongNode;

import jslt2.parser.tokens.TokenType;
import jslt2.util.Jslt2Util;
import jslt2.util.Tuple;
import static jslt2.ast.Decl.*;

/**
 * @author Tony
 *
 */
public abstract class Expr  {
    
    public Expr parentNode;
    public int lineNumber;
    public String sourceLine;
        
    public abstract void visit(ExprVisitor v);
    public abstract Expr optimize();
    
    public boolean isPrimitive() {
        return false;
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Expr> T as() {
        return (T)this;
    }
    
    protected <T extends Expr> T becomeParentOf(T node) {
        if(node != null) {
            node.parentNode = this;
        }
        return node;
    }
    
    protected <T extends Expr> List<T> becomeParentOf(List<T> nodes) {
        if(nodes != null) {
            for(int i = 0; i < nodes.size(); i++) {
                becomeParentOf(nodes.get(i));
            }
        }
        return nodes;
    }
    
    protected <T extends Expr, Y extends Expr> List<Tuple<T, Y>> becomeParentOfByTuples(List<Tuple<T, Y>> nodes) {
        if(nodes != null) {
            for(int i = 0; i < nodes.size(); i++) {
                Tuple<T, Y> tuple = nodes.get(i);
                becomeParentOf(tuple.getFirst());
                becomeParentOf(tuple.getSecond());
            }
        }
        return nodes;
    }
    
    private static boolean isBooleanable(Expr expr) {
        return (expr instanceof BooleanExpr) ||
               (expr instanceof NumberExpr) ||
               (expr instanceof StringExpr) ||
               (expr instanceof NullExpr);
    }
    
    private static boolean isTrue(Expr expr) {
        if(expr instanceof BooleanExpr) {
            BooleanExpr b = expr.as();
            return b.bool;
        }
        else if(expr instanceof NumberExpr) {
            NumberExpr n = expr.as();
            return Jslt2Util.isTrue(n.number);
        }
        else if(expr instanceof StringExpr) {
            StringExpr s = expr.as();
            return s.string != null && !s.string.isEmpty();
        }
        else if(expr instanceof NullExpr) {
            return false;
        }
        
        return false;
    }
    
    
    /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                                  Expressions 
      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
    
    
    public static class ArrayExpr extends Expr {
        public ForArrayExpr forExpr;
        public List<Expr> elements;
        
        public ArrayExpr(ForArrayExpr forExpr, List<Expr> elements) {
            this.forExpr = becomeParentOf(forExpr);
            this.elements = becomeParentOf(elements);
        }
    
        @Override
        public void visit(ExprVisitor v) {
            v.visit(this);
        }
        
        @Override
        public Expr optimize() {
            if(forExpr != null) {
                Expr expr = forExpr.optimize();
                if(expr instanceof ForArrayExpr) {
                    forExpr = expr.as();
                }
                else {
                    return expr;
                }
            }
            
            for(int i = 0; i < elements.size(); i++) {
                Expr e = elements.get(i);
                elements.set(i, e.optimize());
            }
            
            return this;
        }
    }
    
    public static class ArrayIndexExpr extends Expr {
        public Expr array;
        public Expr index;
           
        public ArrayIndexExpr(Expr array, Expr index) {
            this.array = becomeParentOf(array);
            this.index = becomeParentOf(index);
        }
        
        @Override
        public void visit(ExprVisitor v) {
            v.visit(this);
        }
        
        @Override
        public Expr optimize() {
            array = array.optimize();
            index = index.optimize();
            return this;
        }
        
    }
    
    public static class ArraySliceExpr extends Expr {

        public Expr array;
        public Expr startExpr;
        public Expr endExpr;

        public ArraySliceExpr(Expr array, Expr startExpr, Expr endExpr) {
            this.array = becomeParentOf(array);
            this.startExpr = becomeParentOf(startExpr);
            this.endExpr = becomeParentOf(endExpr);
        }
        
        @Override
        public void visit(ExprVisitor v) {
            v.visit(this);
        }

        @Override
        public Expr optimize() {
            array = array.optimize();
            if(startExpr != null) startExpr = startExpr.optimize();
            if(endExpr != null)   endExpr = endExpr.optimize();
            return this;
        }
    }

    public static class BinaryExpr extends Expr {

        public Expr left;
        public TokenType operator;
        public Expr right;
        
        public BinaryExpr(Expr left, TokenType operator, Expr right) {
            this.left = becomeParentOf(left);
            this.operator = operator;
            this.right = becomeParentOf(right);
        }
        
        @Override
        public void visit(ExprVisitor v) {
            v.visit(this);
        }
        
        @Override
        public Expr optimize() {
            left = left.optimize();
            right = right.optimize();
            
            if(left instanceof NumberExpr && right instanceof NumberExpr) {
                NumberExpr l = left.as();
                NumberExpr r = right.as();
                
                switch(operator) {
                    case PLUS: return new NumberExpr(l.number.isIntegralNumber() && r.number.isIntegralNumber() ?
                            new LongNode(l.number.asLong() + r.number.asLong()) : new DoubleNode(l.number.asDouble() + r.number.asDouble()));
                    case MINUS: return new NumberExpr(l.number.isIntegralNumber() && r.number.isIntegralNumber() ?
                            new LongNode(l.number.asLong() - r.number.asLong()) : new DoubleNode(l.number.asDouble() - r.number.asDouble()));
                    case STAR: return new NumberExpr(l.number.isIntegralNumber() && r.number.isIntegralNumber() ?
                            new LongNode(l.number.asLong() * r.number.asLong()) : new DoubleNode(l.number.asDouble() * r.number.asDouble()));
                    case SLASH: return new NumberExpr(l.number.isIntegralNumber() && r.number.isIntegralNumber() ?                    
                            new LongNode(l.number.asLong() / r.number.asLong()) : new DoubleNode(l.number.asDouble() / r.number.asDouble()));                    
                    case MOD: return new NumberExpr(l.number.isIntegralNumber() && r.number.isIntegralNumber() ?
                            new LongNode(l.number.asLong() % r.number.asLong()) : new DoubleNode(l.number.asDouble() % r.number.asDouble()));
                    default: {
                        /* do nothing */
                    }                    
                }
            }
            
            if( (left instanceof StringExpr || right instanceof StringExpr) && (left.isPrimitive() && right.isPrimitive())) {                                
                if(operator.equals(TokenType.PLUS)) {
                    return new StringExpr(left.toString() + right.toString());                                       
                }
            }
            
            if(isBooleanable(left) && isBooleanable(right)) {                
                if(operator.equals(TokenType.AND)) {
                    boolean leftIs = isTrue(left);
                    boolean rightIs = isTrue(right);
                    return new BooleanExpr(leftIs && rightIs);
                    
                }
                else if(operator.equals(TokenType.OR)) {
                    boolean leftIs = isTrue(left);
                    boolean rightIs = isTrue(right);
                    return new BooleanExpr(leftIs || rightIs);
                }                
            }
            
            return this;
        }
    }

    public static class BooleanExpr extends Expr {
        public final boolean bool;
        
        public BooleanExpr(boolean bool) {
            this.bool = bool;
        }
        
        @Override
        public void visit(ExprVisitor v) {
            v.visit(this);
        }
        
        @Override
        public Expr optimize() {        
            return this;
        }
        
        @Override
        public boolean isPrimitive() {        
            return true;
        }
        
        @Override
        public String toString() {        
            return bool ? "true" : "false";
        }
    }
    

    
    public static class DotExpr extends Expr {
        public Expr field;
        
        public DotExpr(Expr field) {
            this.field = becomeParentOf(field);
        }
        
        @Override
        public void visit(ExprVisitor v) {
            v.visit(this);
        }
        
        @Override
        public Expr optimize() {
            if(field != null) {
                field = field.optimize();
            }
            return this;
        }
    }
    
    public static class ElseExpr extends Expr {
        public List<LetDecl> lets;
        public Expr expr;
        
        public ElseExpr(List<LetDecl> lets, Expr expr) {
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
    
    public static class ForArrayExpr extends Expr {

        public Expr condition;
        public List<LetDecl> lets;    
        public Expr valueExpr;
        public Expr ifExpr;
        
        public ForArrayExpr(Expr condition, List<LetDecl> lets, Expr valueExpr, Expr ifExpr) {
            this.condition = becomeParentOf(condition);
            this.lets = becomeParentOf(lets);
            this.valueExpr = becomeParentOf(valueExpr);
            
            this.ifExpr = becomeParentOf(ifExpr);
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
            
            condition = condition.optimize();
            valueExpr = valueExpr.optimize();
            
            if(ifExpr != null) {
                ifExpr = ifExpr.optimize();
                
                // remove the ifExpr if its always true, conversely if its always false
                // return an empty array
                if(isBooleanable(ifExpr)) {
                    if(isTrue(ifExpr)) {
                        ifExpr = null;
                    }
                    else {
                        return new ArrayExpr(null, new ArrayList<>());
                    }
                }
            }
            
            return this;
        }
    }
    
    public static class ForObjectExpr extends Expr {
        public Expr condition;
        public List<LetDecl> lets;
        public Expr keyExpr;
        public Expr valueExpr;
        public Expr ifExpr;
        
        public ForObjectExpr(Expr condition, List<LetDecl> lets, Expr keyExpr, Expr valueExpr, Expr ifExpr) {
            this.condition = becomeParentOf(condition);
            this.lets = becomeParentOf(lets);
            this.keyExpr = becomeParentOf(keyExpr);
            this.valueExpr = becomeParentOf(valueExpr);
            
            this.ifExpr = becomeParentOf(ifExpr);
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
            
            condition = condition.optimize();
            keyExpr = keyExpr.optimize();
            valueExpr = valueExpr.optimize();
            
            if(ifExpr != null) {
                ifExpr = ifExpr.optimize();
                
                // remove the ifExpr if its always true, conversely if its always false
                // return an empty array
                if(isBooleanable(ifExpr)) {
                    if(isTrue(ifExpr)) {
                        ifExpr = null;
                    }
                    else {
                        return new ObjectExpr(lets, null, new ArrayList<>());
                    }
                }
            }
            
            return this;
        }
    }

    public static class FuncCallExpr extends Expr {
        public Expr object;
        public List<Expr> arguments;
        
        public FuncCallExpr(Expr object, List<Expr> arguments) {
            this.object = becomeParentOf(object);
            this.arguments = becomeParentOf(arguments);
        }
        
        @Override
        public void visit(ExprVisitor v) {
            v.visit(this);
        }
        
        @Override
        public Expr optimize() {
            for(int i = 0; i < arguments.size(); i++) {
                Expr a = arguments.get(i).optimize().as();
                arguments.set(i, a);
            }
            
            object = object.optimize();
            
            return this;
        }
    }

    public static class GetExpr extends Expr {

        public Expr object;
        public String identifier;
        
        public GetExpr(Expr object, String identifier) {
            this.object = becomeParentOf(object);
            this.identifier = identifier;
        }
        
        @Override
        public void visit(ExprVisitor v) {
            v.visit(this);
        }
        
        @Override
        public Expr optimize() {
            object = object.optimize();
            return this;
        }
    }

    public static class GroupExpr extends Expr {
        public Expr expr;
        
        public GroupExpr(Expr expr) {
            this.expr = becomeParentOf(expr);
        }
        
        @Override
        public void visit(ExprVisitor v) {
            v.visit(this);
        }
        
        @Override
        public Expr optimize() {
            expr = expr.optimize();
            if(expr.isPrimitive()) {
                return expr;
            }
            
            return this;
        }
    }

    public static class IdentifierExpr extends Expr {
        public final String identifier;
        
        public IdentifierExpr(String identifier) {
            this.identifier = identifier;
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
    
    public static class IfExpr extends Expr {
        public List<LetDecl> lets;
        public Expr condition;
        public Expr thenExpr;
        public ElseExpr elseExpr;

        public IfExpr(List<LetDecl> lets, Expr condition, Expr thenExpr, ElseExpr elseExpr) {
            this.lets = becomeParentOf(lets);
            this.condition = becomeParentOf(condition);
            this.thenExpr = becomeParentOf(thenExpr);
            this.elseExpr = becomeParentOf(elseExpr);
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
            
            condition = condition.optimize();
            thenExpr = thenExpr.optimize();
            
            if(elseExpr != null) {
                elseExpr = elseExpr.optimize().as();
            }
            
            if(isBooleanable(condition)) {
                if(isTrue(condition)) {
                    return thenExpr;
                }
                else {
                    if(elseExpr != null) {
                        return elseExpr;
                    }
                    
                    return new NullExpr();
                }
            }
            
            
            return this;
        }
    }

    public static class MacroCallExpr extends Expr {

        public IdentifierExpr object;
        public List<Expr> arguments;
        
        public MacroCallExpr(IdentifierExpr object, List<Expr> arguments) {
            this.object = becomeParentOf(object);
            this.arguments = becomeParentOf(arguments);
        }

        @Override
        public void visit(ExprVisitor v) {
            v.visit(this);
        }
        
        @Override
        public Expr optimize() {            
            for(int i = 0; i < arguments.size(); i++) {
                Expr a = arguments.get(i).optimize();
                arguments.set(i, a);
            }
                        
            return this;
        }
    }
    
    public static class MatchExpr extends Expr {

        public List<Expr> fields;    

        public MatchExpr(List<Expr> fields) {
            this.fields = becomeParentOf(fields);
        }
        
        @Override
        public void visit(ExprVisitor v) {
            v.visit(this);
        }
        
        @Override
        public Expr optimize() {            
            for(int i = 0; i < fields.size(); i++) {
                Expr a = fields.get(i).optimize();
                fields.set(i, a);
            }
                        
            return this;
        }
    }

    public static class ModuleExpr extends Expr {

        public List<Decl> declarations;

        public Expr expr;

        public ModuleExpr(List<Decl> declarations, Expr expr) {
            this.declarations = becomeParentOf(declarations);
            this.expr = becomeParentOf(expr);
        }
        
        @Override
        public void visit(ExprVisitor v) {
            v.visit(this);
        }
        
        @Override
        public Expr optimize() {
            for(int i = 0; i < declarations.size(); i++) {
                Decl decl = declarations.get(i).optimize().as();
                declarations.set(i, decl);
            }
              
            if(expr != null) {
                expr = expr.optimize();
            }
                        
            return this;
        }
    }
    
    public static class NullExpr extends Expr {
        public NullExpr() {
        }
        
        @Override
        public void visit(ExprVisitor v) {
            v.visit(this);
        }
        
        @Override
        public Expr optimize() {        
            return this;
        }
        
        @Override
        public boolean isPrimitive() {        
            return true;
        }
        
        @Override
        public String toString() {
            return "null";
        }        
    }
    
    public static class NumberExpr extends Expr {
        public final JsonNode number;
        
        public NumberExpr(JsonNode number) {
            this.number = number;
        }
        
        @Override
        public void visit(ExprVisitor v) {
            v.visit(this);
        }
        
        @Override
        public Expr optimize() {        
            return this;
        }
        
        @Override
        public boolean isPrimitive() {        
            return true;
        }
        
        @Override
        public String toString() {
            return number.asText();
        }
    }

    public static class ObjectExpr extends Expr {

        public List<LetDecl> lets;
        public ForObjectExpr forObjectExpr;
        public List<Tuple<Expr, Expr>> fields;
        
        public ObjectExpr(List<LetDecl> lets, ForObjectExpr forObjectExpr, List<Tuple<Expr, Expr>> fields) {
            this.lets = becomeParentOf(lets);
            this.forObjectExpr = becomeParentOf(forObjectExpr);
            this.fields = becomeParentOfByTuples(fields);
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
            
            if(forObjectExpr != null) {
                Expr expr = forObjectExpr.optimize();
                if(expr instanceof ForObjectExpr) {
                    forObjectExpr = expr.as();
                }
                else {
                    return expr;
                }
            }
            
            for(int i = 0; i < fields.size(); i++) {
                Expr key = fields.get(i).getFirst().optimize();
                Expr value = fields.get(i).getSecond().optimize();
                fields.set(i, new Tuple<>(key, value));
            }
            
            return this;
        }
    }

    public static class ProgramExpr extends ModuleExpr {
        
        public ProgramExpr(List<Decl> declarations, Expr expr) {
            super(declarations, expr);
        }

        @Override
        public void visit(ExprVisitor v) {
            v.visit(this);
        }
    }
    
    public static class StringExpr extends Expr {

        public final String string;
        
        public StringExpr(String string) {
            this.string = string;
        }

        @Override
        public void visit(ExprVisitor v) {
            v.visit(this);
        }
        
        @Override
        public Expr optimize() {        
            return this;
        }
        
        @Override
        public boolean isPrimitive() {        
            return true;
        }
        
        @Override
        public String toString() {
            return this.string;
        }
    }
    
    public static class UnaryExpr extends Expr {

        public TokenType operator;
        public Expr expr;
        
        public UnaryExpr(TokenType operator, Expr expr) {
            this.operator = operator;
            this.expr = becomeParentOf(expr);
        }
        
        @Override
        public void visit(ExprVisitor v) {
            v.visit(this);
        }
        
        @Override
        public Expr optimize() {
            expr = expr.optimize();
            
            if(expr.isPrimitive()) {
                if(expr instanceof NumberExpr) {
                    NumberExpr n = expr.as();
                    switch(operator) {
                        case MINUS: {                    
                            if(n.number.isIntegralNumber()) {
                                return new NumberExpr(new LongNode(-n.number.asLong()));
                            }
                            return new NumberExpr(new DoubleNode(-n.number.asDouble()));
                        }
                        case NOT: {
                            return new BooleanExpr(!Jslt2Util.isTrue(n.number));
                        }
                        default: {}
                    }
                }
                else if(isBooleanable(expr)) {
                    if(operator.equals(TokenType.NOT)) {
                        return new BooleanExpr(!isTrue(expr));
                    }
                }
            }
            
            return this;
        }
    }

    public static class VariableExpr extends Expr {
        public final String variable;
        
        public VariableExpr(String variable) {
            this.variable = variable;
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

}
