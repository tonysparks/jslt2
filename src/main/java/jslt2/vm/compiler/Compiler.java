/*
 * see license.txt
 */
package jslt2.vm.compiler;

import java.util.ArrayList;
import java.util.List;

import jslt2.Jslt2;
import jslt2.Jslt2Exception;
import jslt2.ast.Decl.*;
import jslt2.ast.Expr;
import jslt2.ast.Expr.*;
import jslt2.ast.ExprVisitor;
import jslt2.parser.ErrorCode;
import jslt2.parser.Parser;
import jslt2.parser.Scanner;
import jslt2.parser.Source;
import jslt2.parser.tokens.TokenType;
import jslt2.util.Stack;
import jslt2.util.Tuple;
import jslt2.vm.Bytecode;

/**
 * The compiler for Jslt2
 * 
 * @author Tony
 *
 */
public class Compiler {

    private Jslt2 runtime;

    public Compiler(Jslt2 runtime) {    
        this.runtime = runtime;
    }
    
    /**
     * Compiles the {@link ProgramExpr}
     * 
     * @param program
     * @return the {@link Bytecode}
     */
    public Bytecode compile(ProgramExpr program) throws Jslt2Exception {        
        return new BytecodeEmitterNodeVisitor().compile(program);
    }
    
    
    private class BytecodeEmitterNodeVisitor implements ExprVisitor {        
        private BytecodeEmitter asm;
        private Stack<String> moduleStack;
        private Stack<String> libraryStack;
        private boolean inAsyncBlock;
        private Locals asyncLocals;
        
        public BytecodeEmitterNodeVisitor() {
            this.asm = new BytecodeEmitter(new EmitterScopes());
            this.asm.setDebug(runtime.isDebugMode());
            
            this.moduleStack = new Stack<>();
            this.libraryStack = new Stack<>();
            
            this.inAsyncBlock = false;
        }
        
        public Bytecode compile(ProgramExpr program) {
            visit(program);
            
            return this.asm.compile();
        }
        
        private Jslt2Exception error(Expr expr, String msg) {                
            return new Jslt2Exception(ErrorCode.errorMessage(expr.token, msg, expr.sourceLine));
        }

        /**
         * Used for matcher expressions, we must determine which
         * input path to resolve to (i.e., if we're in a chain of
         * ObjectExpr, we'll want to match the keys of the input)
         */
        private void pushInputContext(Expr expr) {
            StringBuilder sb = new StringBuilder();
            
            Expr child = expr;
            Expr parent = expr.parentNode;
            if(parent instanceof ArrayExpr) {
                throw error(parent, "Object matching not allowed in an array");
            }
            
            int count = 0;
            
            while(parent != null && count < 2) {
                if(parent instanceof ObjectExpr) {
                    ObjectExpr objExpr = (ObjectExpr) parent;
                    if(objExpr.forObjectExpr == null) {
                        List<Tuple<Expr, Expr>> fields = objExpr.fields;
                        for(Tuple<Expr, Expr> field : fields) {
                            if(field.getSecond() == child) {
                                sb.append(field.getFirst()).append(":");
                            }
                        }
                    }
                    
                    count = 0;
                }
                
                child = parent;
                parent = parent.parentNode;
                count++;
            }
            
            asm.addAndloadconst(sb.toString());
        }
        
        @Override
        public void visit(NullExpr expr) {
            asm.line(expr.lineNumber);
            
            asm.loadnull();
        }
    
        @Override
        public void visit(BooleanExpr expr) {
            asm.line(expr.lineNumber);
            
            if(expr.bool) {
                asm.loadtrue();
            }
            else {
                asm.loadfalse();
            }
        }
    
        @Override
        public void visit(NumberExpr expr) {
            asm.line(expr.lineNumber);
            asm.addAndloadconst(expr.number);        
        }
    
        @Override
        public void visit(StringExpr expr) {
            asm.line(expr.lineNumber);
            asm.addAndloadconst(expr.string);
        }
    
        @Override
        public void visit(ObjectExpr expr) {
            asm.line(expr.lineNumber);
            
            expr.lets.forEach(field -> field.visit(this));
            
            ForObjectExpr forExpr = expr.forObjectExpr;
            if(forExpr != null) {
                forExpr.visit(this);
            }
            else {
                asm.newobj();
                for(Tuple<Expr, Expr> field : expr.fields) {
                    Expr fieldName = field.getFirst();
                    Expr fieldValue = field.getSecond();
                    
                    if(fieldName instanceof IdentifierExpr) {
                        fieldValue.visit(this);
                        asm.addfieldk(((IdentifierExpr)fieldName).identifier);
                    }
                    else if(fieldName instanceof StringExpr) {
                        fieldValue.visit(this);
                        asm.addfieldk(((StringExpr)fieldName).string);
                    }
                    else if(fieldName instanceof MatchExpr) {
                        pushInputContext(expr);
                        fieldName.visit(this);
                        
                        // this is the body of the matcher function
                        fieldValue.visit(this);                        
                        asm.end();
                    }
                    else {                     
                        fieldName.visit(this);
                        fieldValue.visit(this);
                        asm.addfield();
                    }
                }
                asm.sealobj();
            }
            
        }
    
        @Override
        public void visit(ArrayExpr expr) {
            asm.line(expr.lineNumber);
            
            ForArrayExpr arrayExpr = expr.forExpr;
            if(arrayExpr != null) {
                arrayExpr.visit(this);
            }
            else {
                asm.newarray();
                List<Expr> elements = expr.elements;
                
                for(Expr e : elements) {
                    e.visit(this);
                    asm.addelement();
                    
                }
                asm.sealarray();
            }
        }
    
        @Override
        public void visit(IfExpr expr) {
            asm.line(expr.lineNumber);
            
            Expr cond = expr.condition;
            cond.visit(this);
            
            asm.markLexicalScope();
            expr.lets.forEach(field -> field.visit(this));
            
            String elseLabel = asm.ifeq();
            Expr then = expr.thenExpr;
            then.visit(this);
            String endif = asm.jmp();
            
            asm.unmarkLexicalScope();
            
            asm.label(elseLabel);
            Expr elseExpr = expr.elseExpr;
            if(elseExpr != null) {
                elseExpr.visit(this);            
            }
            else {
                asm.loadnull();
            }
            asm.label(endif);
            
        }
        
        @Override
        public void visit(ElseExpr expr) {
            asm.line(expr.lineNumber);
            
            asm.markLexicalScope();
            expr.lets.forEach(field -> field.visit(this));
            expr.expr.visit(this);
            asm.unmarkLexicalScope();
        }
    
        
        @Override
        public void visit(ForObjectExpr expr) {
            asm.line(expr.lineNumber);
            
            Expr cond = expr.condition;
            cond.visit(this);
            asm.forobjdef();                
                expr.lets.forEach(let -> let.visit(this));
                
                Expr ifExpr = expr.ifExpr;
                if(ifExpr != null) {
                    ifExpr.visit(this);
                    String skipLabel = asm.ifeq();
                    
                    Expr key = expr.keyExpr;
                    key.visit(this);
                    
                    Expr value = expr.valueExpr;
                    value.visit(this);
                    String endif = asm.jmp();
                    
                    asm.label(skipLabel);
                    asm.loadjnull();
                    asm.loadjnull();
                    
                    asm.label(endif);
                }
                else {
                    Expr key = expr.keyExpr;
                    key.visit(this);
                    
                    Expr value = expr.valueExpr;
                    value.visit(this);
                }
            asm.end();
        }
    
        @Override
        public void visit(ForArrayExpr expr) {
            asm.line(expr.lineNumber);     
            
            Expr cond = expr.condition;
            cond.visit(this);
            asm.forarraydef();
                expr.lets.forEach(let -> let.visit(this));
                
                Expr ifExpr = expr.ifExpr;
                if(ifExpr != null) {
                    ifExpr.visit(this);
                    String skipLabel = asm.ifeq();
                    
                    Expr value = expr.valueExpr;
                    value.visit(this);
                    
                    String endif = asm.jmp();
                    
                    asm.label(skipLabel);                    
                    asm.loadjnull();
                    
                    asm.label(endif);
                }
                else {
                    Expr value = expr.valueExpr;
                    value.visit(this);
                }
            asm.end();
        }
    
        @Override
        public void visit(LetDecl expr) {
            asm.line(expr.lineNumber);
            
            String localVarName = "$" + expr.identifier; 
            int index = asm.addLocal(localVarName);
            expr.value.visit(this);
            asm.storelocal(index);
        }
    
        @Override
        public void visit(DefDecl expr) {
            asm.line(expr.lineNumber);
            
            String functionName = expr.identifier;
            if(!this.moduleStack.isEmpty()) {
                asm.addFunction(this.moduleStack.peek() + ":" + functionName, asm.getBytecodeIndex());
            }
            
            asm.addFunction(functionName, asm.getBytecodeIndex());
            
            List<String> parameters = expr.parameters;
            asm.funcdef(parameters.size());        
                for(String param : parameters) {
                    asm.addLocal("$"+param);
                }
                expr.lets.forEach(let -> let.visit(this));
                
                expr.expr.visit(this);
            asm.end();
        }
        
        @Override
        public void visit(MacroCallExpr expr) {
            asm.line(expr.lineNumber);
    
            List<Expr> arguments = expr.arguments;
            int numberOfArgs = arguments.size();
            
            for(Expr arg : arguments) {
                asm.funcdef(0);
                arg.visit(this);
                asm.end();
            }
            
            int bytecodeIndex = asm.getBytecodeIndex();
            String functionName = expr.object.identifier;
            
            asm.addAndloadconst(bytecodeIndex);            
            asm.macroinvoke(numberOfArgs, functionName);            
        }
    
        @Override
        public void visit(FuncCallExpr expr) {
            asm.line(expr.lineNumber);
    
            List<Expr> arguments = expr.arguments;
            int numberOfArgs = arguments.size();
            
            for(Expr arg : arguments) {
                arg.visit(this);
            }
            
            int bytecodeIndex = -1;
            String functionName = null;
            
            Expr identifier = expr.object;
            if(identifier instanceof IdentifierExpr) {
                functionName = ((IdentifierExpr)identifier).identifier;
                bytecodeIndex = asm.getFunction(functionName);
                if(!runtime.hasFunction(functionName)) {
                    if(inAsyncBlock && bytecodeIndex < 0) {
                        throw error(identifier, "'" + functionName + "' is undefined or must be defined before the async block");
                    }
                    
                    asm.addPendingFunction(functionName);
                    bytecodeIndex = 999;
                }
            }
    
            if(bytecodeIndex < 0) {
                asm.userinvoke(numberOfArgs, functionName);
            }        
            else {
                asm.invoke(numberOfArgs, bytecodeIndex);
            }
        }
    
        @Override
        public void visit(IdentifierExpr expr) {
            asm.line(expr.lineNumber);
            asm.getfieldk(expr.identifier); 
        }
    
        @Override
        public void visit(VariableExpr expr) {
            asm.line(expr.lineNumber);
            if(this.inAsyncBlock) {                
                if(asyncLocals.get(expr.variable) > -1) {
                    throw error(expr, "'" + expr.variable + "' can't be referenced in the same async block");
                }
            }
            
            if(!asm.load(expr.variable)) {
                throw error(expr, "'" + expr.variable + "' not defined");
            }            
        }
    
        @Override
        public void visit(ArraySliceExpr expr) {
            asm.line(expr.lineNumber);
            expr.array.visit(this);
            expr.startExpr.visit(this);
            expr.endExpr.visit(this);
            asm.arrayslice();
        }
    
    
        @Override
        public void visit(ArrayIndexExpr expr) {
            expr.array.visit(this);
            expr.index.visit(this);
            asm.getarrayelement();        
        }
        
        @Override
        public void visit(GetExpr expr) {
            asm.line(expr.lineNumber);
            
            expr.object.visit(this);
            asm.getfieldk(expr.identifier);        
        }
    
        @Override
        public void visit(GroupExpr expr) {
            asm.line(expr.lineNumber);
            expr.expr.visit(this);        
        }
        
    
        @Override
        public void visit(ImportDecl expr) {
            asm.line(expr.lineNumber);
    
            String fileName = expr.library;
            fileName = fileName.substring(1, fileName.length() - 1);
            
            if(this.libraryStack.contains(fileName)) {
                throw error(expr, "'" + fileName + "' is already imported");
            }
            
            try {
                Scanner scanner = new Scanner(new Source(runtime.getResolver().resolve(fileName)));
                Parser parser = new Parser(runtime, scanner);
                
                this.libraryStack.push(fileName);
                this.moduleStack.push(expr.alias);
                parser.parseModule().visit(this);
                this.moduleStack.pop();
                this.libraryStack.pop();
            }
            catch(Exception e) {
                throw new Jslt2Exception(e);
            }
        }
    
        @Override
        public void visit(ProgramExpr expr) {
            asm.startGlobal();
                asm.line(expr.lineNumber);
                expr.declarations.forEach(decl -> decl.visit(this));                
                expr.expr.visit(this);
            asm.end();
        }
        
        @Override
        public void visit(ModuleExpr expr) {      
            asm.line(expr.lineNumber);
            expr.declarations.forEach(decl -> decl.visit(this));   
            
            Expr funcExpr = expr.expr;
            if(funcExpr != null) {
                DefDecl defExpr = new DefDecl(this.moduleStack.peek(), new ArrayList<>(), new ArrayList<>(), funcExpr);
                defExpr.visit(this);
            }
        }
    
        @Override
        public void visit(AsyncBlockDecl expr) {
            asm.line(expr.lineNumber);
            
            inAsyncBlock = true;            
            asyncLocals = asm.getLocals();
                        
            List<LetDecl> lets = expr.lets;
            for(LetDecl let : lets) {
                // store off the local index, so that ASYNC opcode
                // can use it
                String localVarName = "$" + let.identifier; 
                int index = asm.addLocal(localVarName);
                asm.addAndloadconst(index);
                
                // now run the value in an async thread
                asm.async();
                let.value.visit(this);
                asm.end();
            }
            
            asm.await();
            
            inAsyncBlock = false;
        }
        
        @Override
        public void visit(UnaryExpr expr) {
            asm.line(expr.lineNumber);
    
            expr.expr.visit(this);
            switch(expr.operator) {
                case NOT:
                    asm.not();
                    break;
                case MINUS:
                    asm.neg();
                    break;
                default:
                    throw error(expr, "Invalid unary operator: " + expr.operator);
            }
        }
    
        @Override
        public void visit(BinaryExpr expr) {
            asm.line(expr.lineNumber);
    
            TokenType operator = expr.operator;
            switch(operator) {
                case AND: {
                    expr.left.visit(this);
                    String escape = asm.ifeq();
                    
                    expr.right.visit(this);
                    asm.istrue();
               
                    String endif = asm.jmp();
                    asm.label(escape);
                    asm.loadfalse();
                    asm.label(endif);
                    break;
                }
                case OR: {
                    expr.left.visit(this);
                    String secondConditional = asm.ifeq();
                    String skip = asm.jmp();
                    
                    asm.label(secondConditional);
                    expr.right.visit(this);
                    asm.istrue();
                    String end = asm.jmp();
                    
                    asm.label(skip);
                    asm.loadtrue();
                    asm.label(end);
                    
                    break;
                }
                default: {
                    expr.left.visit(this);
                    expr.right.visit(this);
                    
                    visitBinaryExpression(expr, operator);        
                }
            }
        }
        
        /**
         * Visits a Binary Expression
         * 
         * @param op
         */
        private void visitBinaryExpression(Expr expr, TokenType op) {
            switch(op) {
                case PLUS:  asm.add(); break;
                case MINUS: asm.sub(); break;
                case STAR:  asm.mul(); break;
                case SLASH: asm.div(); break;
                case MOD:   asm.mod(); break;
                
                // comparisons
                case NOT_EQUALS:      asm.neq();  break;
                case GREATER_THAN:    asm.gt();   break;
                case GREATER_EQUALS:  asm.gte();  break;
                case LESS_THAN:       asm.lt();   break;
                case LESS_EQUALS:     asm.lte();  break;
                case EQUALS_EQUALS:
                case EQUALS:          asm.eq();   break;
                            
                default: 
                    throw error(expr, "Unknown BinaryOperator: " + op);            
            }
            
        }
    
        @Override
        public void visit(DotExpr expr) {
            asm.line(expr.lineNumber);
            
            Expr field = expr.field;
            if(field != null) {   
                if(field instanceof StringExpr) {
                    String fieldName = ((StringExpr)field).string;
                    if(fieldName.startsWith("\"") && fieldName.endsWith("\"") && fieldName.length() > 2) {
                        fieldName = fieldName.substring(1, fieldName.length() - 1);
                    }
                    asm.getinputfieldk(fieldName);
                }
                else if (field instanceof IdentifierExpr) {
                    IdentifierExpr iExpr = (IdentifierExpr)field;
                    asm.getinputfieldk(iExpr.identifier);
                }
                else {
                    asm.loadinput();
                    field.visit(this);
                }
            }        
            else {
                asm.loadinput();
            }
        }
    
        @Override
        public void visit(MatchExpr expr) {
            asm.line(expr.lineNumber);
            List<Expr> fields = expr.fields;
            int numFieldsToOmit = 0;
            
            if(fields != null) {
                numFieldsToOmit = fields.size();
                for(Expr field : fields) {
                    if(field instanceof IdentifierExpr) {
                        asm.addAndloadconst(((IdentifierExpr)field).identifier);
                    }
                    else if(field instanceof StringExpr) {
                        asm.addAndloadconst(((StringExpr)field).string);
                    }
                    else {
                        throw error(expr, "Invalid match field expression: " + field);
                    }
                }
            }
            
            asm.matcher(numFieldsToOmit);
        }
    }
}
