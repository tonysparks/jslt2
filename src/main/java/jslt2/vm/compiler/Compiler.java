/*
 * see license.txt
 */
package jslt2.vm.compiler;

import java.util.List;

import jslt2.Jslt2;
import jslt2.Jslt2Exception;
import jslt2.ast.*;
import jslt2.parser.tokens.TokenType;
import jslt2.util.Tuple;
import jslt2.vm.Bytecode;
import jslt2.vm.compiler.EmitterScope.ScopeType;

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
    
    
    private class BytecodeEmitterNodeVisitor implements NodeVisitor {
        private BytecodeEmitter asm;
    
        public BytecodeEmitterNodeVisitor() {
            this.asm = new BytecodeEmitter(new EmitterScopes());
            this.asm.setDebug(runtime.isDebugMode());
        }
        
        public Bytecode compile(ProgramExpr program) {
            visit(program);
            
            return this.asm.compile();
        }

        @Override
        public void visit(NullExpr expr) {
            asm.line(expr.getLineNumber());
            
            asm.loadnull();
        }
    
        @Override
        public void visit(BooleanExpr expr) {
            asm.line(expr.getLineNumber());
            
            if(expr.getBoolean()) {
                asm.loadtrue();
            }
            else {
                asm.loadfalse();
            }
        }
    
        @Override
        public void visit(NumberExpr expr) {
            asm.line(expr.getLineNumber());
            asm.addAndloadconst(expr.getNumber());        
        }
    
        @Override
        public void visit(StringExpr expr) {
            asm.line(expr.getLineNumber());
            asm.addAndloadconst(expr.getString());
        }
    
        @Override
        public void visit(ObjectExpr expr) {
            asm.line(expr.getLineNumber());
            
            expr.getLets().forEach(field -> field.visit(this));
            
            ForObjectExpr forExpr = expr.getForObjectExpr();
            if(forExpr != null) {
                forExpr.visit(this);
            }
            else {
                asm.newobj();
                for(Tuple<Expr, Expr> field : expr.getFields()) {
                    field.getSecond().visit(this);
                    
                    Expr fieldName = field.getFirst();
                    if(fieldName instanceof IdentifierExpr) {
                        asm.addfieldk(((IdentifierExpr)fieldName).getIdentifier());
                    }
                    else if(fieldName instanceof StringExpr) {
                        asm.addfieldk(((StringExpr)fieldName).getString());
                    }
                    else if(fieldName instanceof MatchExpr) {
                        fieldName.visit(this);
                    }
                    else {
                        throw new Jslt2Exception("Invalid field expression: " + fieldName);
                    }
                }
                asm.sealobj();
            }
            
        }
    
        @Override
        public void visit(ArrayExpr expr) {
            asm.line(expr.getLineNumber());
            
            ForArrayExpr arrayExpr = expr.getForExpr();
            if(arrayExpr != null) {
                arrayExpr.visit(this);
            }
            else {
                asm.newarray();
                List<Expr> elements = expr.getElements();
                for(Expr e : elements) {
                    e.visit(this);
                    asm.addelement();
                }
                asm.sealarray();
            }
        }
    
        @Override
        public void visit(IfExpr expr) {
            asm.line(expr.getLineNumber());
            
            Expr cond = expr.getCondition();
            cond.visit(this);
            
            asm.markLexicalScope();
            expr.getLets().forEach(field -> field.visit(this));
            
            String elseLabel = asm.ifeq();
            Expr then = expr.getThenExpr();
            then.visit(this);
            String endif = asm.jmp();
            
            asm.unmarkLexicalScope();
            
            asm.label(elseLabel);
            Expr elseExpr = expr.getElseExpr();
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
            asm.line(expr.getLineNumber());
            
            asm.markLexicalScope();
            expr.getLets().forEach(field -> field.visit(this));
            expr.getExpr().visit(this);
            asm.unmarkLexicalScope();
        }
    
        
        @Override
        public void visit(ForObjectExpr expr) {
            asm.line(expr.getLineNumber());
            
            Expr cond = expr.getCondition();
            cond.visit(this);
            asm.forobjdef();                
                expr.getLets().forEach(let -> let.visit(this));
                
                Expr key = expr.getKeyExpr();
                key.visit(this);
                
                Expr value = expr.getValueExpr();
                value.visit(this);
            asm.end();
        }
    
        @Override
        public void visit(ForArrayExpr expr) {
            asm.line(expr.getLineNumber());     
            
            Expr cond = expr.getCondition();
            cond.visit(this);
            asm.forarraydef();
                expr.getLets().forEach(let -> let.visit(this));
                
                Expr value = expr.getValueExpr();
                value.visit(this);
            asm.end();
        }
    
        @Override
        public void visit(LetExpr expr) {
            asm.line(expr.getLineNumber());
            
            String localVarName = "$" + expr.getIdentifier(); 
            int index = asm.addLocal(localVarName);
            expr.getValue().visit(this);
            asm.storelocal(index);
        }
    
        @Override
        public void visit(DefExpr expr) {
            asm.line(expr.getLineNumber());
            
            String functionName = expr.getIdentifier();
            asm.addFunction(functionName, asm.getBytecodeIndex());
            
            List<String> parameters = expr.getParameters();
            asm.funcdef(parameters.size());        
                for(String param : parameters) {
                    asm.addLocal("$"+param);
                }
                expr.getLets().forEach(let -> let.visit(this));
                
                expr.getExpr().visit(this);
            asm.end();
        }
    
        @Override
        public void visit(FuncCallExpr expr) {
            asm.line(expr.getLineNumber());
    
            List<Expr> arguments = expr.getArguments();
            int numberOfArgs = arguments.size();
            
            for(Expr arg : arguments) {
                arg.visit(this);
            }
            
            int bytecodeIndex = -1;
            String functionName = null;
            
            Expr identifier = expr.getObject();
            if(identifier instanceof IdentifierExpr) {
                functionName = ((IdentifierExpr)identifier).getIdentifier();
                bytecodeIndex = asm.getFunction(functionName);
                if(!runtime.hasFunction(functionName)) {
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
            asm.line(expr.getLineNumber());
            asm.getfieldk(expr.getIdentifier()); 
        }
    
        @Override
        public void visit(VariableExpr expr) {
            asm.line(expr.getLineNumber());
            if(!asm.load(expr.getVariable())) {
                throw new Jslt2Exception(expr.getVariable() + " not defined at line: " + expr.getLineNumber());
            }
        }
    
        @Override
        public void visit(ArraySliceExpr expr) {
            asm.line(expr.getLineNumber());
            expr.getArray().visit(this);
            expr.getStartExpr().visit(this);
            expr.getEndExpr().visit(this);
            asm.arrayslice();
        }
    
    
        @Override
        public void visit(ArrayIndexExpr expr) {
            expr.getArray().visit(this);
            expr.getIndex().visit(this);
            asm.getfield();        
        }
        
        @Override
        public void visit(GetExpr expr) {
            asm.line(expr.getLineNumber());
            
            expr.getObject().visit(this);
            asm.getfieldk(expr.getIdentifier());        
        }
    
        @Override
        public void visit(GroupExpr expr) {
            asm.line(expr.getLineNumber());
            expr.getExpr().visit(this);        
        }
        
        /* (non-Javadoc)
         * @see jslt2.ast.NodeVisitor#visit(jslt2.ast.ImportGetExpr)
         */
        @Override
        public void visit(ImportGetExpr expr) {
            asm.line(expr.getLineNumber());
            // TODO
        }
    
        /* (non-Javadoc)
         * @see jslt2.ast.NodeVisitor#visit(jslt2.ast.ImportExpr)
         */
        @Override
        public void visit(ImportExpr expr) {
            asm.line(expr.getLineNumber());
            // TODO
        }
    
        @Override
        public void visit(ProgramExpr expr) {
            asm.start(ScopeType.GLOBAL_SCOPE, 0);
                asm.line(expr.getLineNumber());
                expr.getImports().forEach(imp -> imp.visit(this));
                expr.getLets().forEach(let -> let.visit(this));
                expr.getDefs().forEach(def -> def.visit(this));
                
                expr.getExpr().visit(this);
            asm.end();
        }
    
        @Override
        public void visit(UnaryExpr expr) {
            asm.line(expr.getLineNumber());
    
            expr.getExpr().visit(this);
            switch(expr.getOperator()) {
                case NOT:
                    asm.not();
                    break;
                case MINUS:
                    asm.neg();
                    break;
                default:
                    throw new Jslt2Exception("Invalid unary operator: " + expr.getOperator());
            }
        }
    
        @Override
        public void visit(BinaryExpr expr) {
            asm.line(expr.getLineNumber());
    
            TokenType operator = expr.getOperator();
            switch(operator) {
                case AND: {
                    expr.getLeft().visit(this);
                    String escape = asm.ifeq();
                    
                    expr.getRight().visit(this);
                    String endif = asm.jmp();
                    asm.label(escape);
                    asm.loadfalse();
                    asm.label(endif);
                    break;
                }
                case OR: {
                    expr.getLeft().visit(this);
                    String secondConditional = asm.ifeq();
                    String skip = asm.jmp();
                    
                    asm.label(secondConditional);
                    expr.getRight().visit(this);                            
                    String end = asm.jmp();
                    
                    asm.label(skip);
                    asm.loadtrue();
                    asm.label(end);
                    
                    break;
                }
                default: {
                    expr.getLeft().visit(this);
                    expr.getRight().visit(this);
                    
                    visitBinaryExpression(operator);        
                }
            }
        }
        
        /**
         * Visits a Binary Expression
         * 
         * @param op
         * @throws EvalException
         */
        private void visitBinaryExpression(TokenType op) throws Jslt2Exception {
            switch(op) {
                case PLUS:  asm.add(); break;
                case MINUS: asm.sub(); break;
                case STAR:  asm.mul(); break;
                case SLASH: asm.div(); break;
                case MOD:   asm.mod(); break;
                
                // comparisons
                case AND:             asm.and();  break;
                case OR:              asm.or();   break;
                case NOT_EQUALS:      asm.neq();  break;
                case GREATER_THAN:    asm.gt();   break;
                case GREATER_EQUALS:  asm.gte();  break;
                case LESS_THAN:       asm.lt();   break;
                case LESS_EQUALS:     asm.lte();  break;
                case EQUALS_EQUALS:
                case EQUALS:          asm.eq();   break;
                            
                default: 
                    throw new Jslt2Exception("Unknown BinaryOperator: " + op);            
            }
            
        }
    
        @Override
        public void visit(DotExpr expr) {
            asm.line(expr.getLineNumber());
            
            asm.loadinput();        
            Expr field = expr.getField();
            if(field != null) {            
                field.visit(this);
            }        
        }
    
        @Override
        public void visit(MatchExpr expr) {
            asm.line(expr.getLineNumber());
            List<Expr> fields = expr.getFields();
            int numFieldsToOmit = 0;
            
            if(fields != null) {
                numFieldsToOmit = fields.size();
                for(Expr field : fields) {
                    if(field instanceof IdentifierExpr) {
                        asm.addAndloadconst(((IdentifierExpr)field).getIdentifier());
                    }
                    else if(field instanceof StringExpr) {
                        asm.addAndloadconst(((StringExpr)field).getString());
                    }
                    else {
                        throw new Jslt2Exception("Invalid match field expression: " + field);
                    }
                }
            }
            
            asm.matcher(numFieldsToOmit);
        }
    }
}
