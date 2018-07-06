/*
    Leola Programming Language
    Author: Tony Sparks
    See license.txt
*/
package jslt2.vm.compiler;

import java.util.List;

import jslt2.Jslt2;
import jslt2.Jslt2Exception;
import jslt2.ast.*;
import jslt2.parser.tokens.TokenType;
import jslt2.util.Tuple;
import jslt2.vm.compiler.EmitterScope.ScopeType;

/**
 * Generates {@link Bytecode} based off of an Abstract Syntax Tree.
 * 
 * 
 * @author Tony
 *
 */
public class BytecodeGeneratorVisitor implements NodeVisitor {

    /**
     * The assembler
     */
    private BytecodeEmitter asm;
        
    /**
     * @param runtime
     * @param symbols
     */
    public BytecodeGeneratorVisitor(Jslt2 runtime, EmitterScopes symbols) {
        this.asm = new BytecodeEmitter(symbols);
        this.asm.setDebug(runtime.isDebugMode());
    }
        
    /**
     * @return the asm
     */
    public BytecodeEmitter getAsm() {
        return asm;
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
        
        asm.newobj();
        
        expr.getLets().forEach(field -> field.visit(this));
        
        ForObjectExpr forExpr = expr.getForObjectExpr();
        if(forExpr != null) {
            forExpr.visit(this);
        }
        else {
            for(Tuple<Expr, Expr> field : expr.getFields()) {
                field.getSecond().visit(this);
                
                Expr fieldName = field.getFirst();
                if(fieldName instanceof IdentifierExpr) {
                    asm.addfield(((IdentifierExpr)fieldName).getIdentifier());
                }
                else if(fieldName instanceof StringExpr) {
                    asm.addfield(((StringExpr)fieldName).getString());
                }
                else {
                    throw new Jslt2Exception("Invalid field expression: " + fieldName);
                }
            }
        }
        
        asm.sealobj();
    }

    @Override
    public void visit(ArrayExpr expr) {
        asm.line(expr.getLineNumber());
        //expr.
    }

    @Override
    public void visit(IfExpr expr) {
        asm.line(expr.getLineNumber());
        
        Expr cond = expr.getCondition();
        cond.visit(this);
        
        String elseLabel = asm.ifeq();
        Expr then = expr.getThenExpr();
        then.visit(this);
        String endif = asm.jmp();
        
        asm.label(elseLabel);
        Expr elseExpr = expr.getElseExpr();
        if(elseExpr != null) {
            elseExpr.visit(this);            
        }
        asm.label(endif);
        
    }

    /* (non-Javadoc)
     * @see jslt2.ast.NodeVisitor#visit(jslt2.ast.ForObjectExpr)
     */
    @Override
    public void visit(ForObjectExpr expr) {
        asm.line(expr.getLineNumber());
        
    }

    /* (non-Javadoc)
     * @see jslt2.ast.NodeVisitor#visit(jslt2.ast.ForArrayExpr)
     */
    @Override
    public void visit(ForArrayExpr expr) {
        asm.line(expr.getLineNumber());
        
    }

    @Override
    public void visit(LetExpr expr) {
        asm.line(expr.getLineNumber());
        expr.getValue().visit(this);
        asm.addAndstorelocal("$"+expr.getIdentifier());
    }

    @Override
    public void visit(DefExpr expr) {
        asm.line(expr.getLineNumber());
        
        asm.addFunction(expr.getIdentifier(), asm.getBytecodeIndex());
        
        List<String> parameters = expr.getParameters();
        asm.funcdef(parameters.size());        
            expr.getLets().forEach(let -> let.visit(this));
            for(String param : parameters) {
                asm.addLocal("$"+param);
            }
            
            expr.getExpr().visit(this);
        asm.end();
    }

    @Override
    public void visit(FuncCallExpr expr) {
        asm.line(expr.getLineNumber());
        
        int bytecodeIndex = -1;
        Expr identifier = expr.getObject();
        if(identifier instanceof IdentifierExpr) {
            String name = ((IdentifierExpr)identifier).getIdentifier();
            bytecodeIndex = asm.getFunction(name);
        }
        //expr.getObject().visit(this);
        
        List<Expr> arguments = expr.getArguments();
        for(Expr arg : arguments) {
            arg.visit(this);
        }
        asm.invoke(arguments.size(), bytecodeIndex);
    }

    @Override
    public void visit(IdentifierExpr expr) {
        asm.line(expr.getLineNumber());
        asm.getk(expr.getIdentifier()); 
    }

    @Override
    public void visit(VariableExpr expr) {
        asm.line(expr.getLineNumber());
        if(!asm.load(expr.getVariable())) {
            throw new Jslt2Exception(expr.getVariable() + " not defined");
        }
    }

    @Override
    public void visit(ArraySliceExpr expr) {
        asm.line(expr.getLineNumber());
        expr.getArray().visit(this);
        expr.getStartExpr().visit(this);
        expr.getEndExpr().visit(this);
    }

    @Override
    public void visit(GetExpr expr) {
        asm.line(expr.getLineNumber());
        
        expr.getObject().visit(this);
        asm.getk(expr.getIdentifier());        
    }

    /* (non-Javadoc)
     * @see jslt2.ast.NodeVisitor#visit(jslt2.ast.ImportGetExpr)
     */
    @Override
    public void visit(ImportGetExpr expr) {
        asm.line(expr.getLineNumber());
        
    }

    /* (non-Javadoc)
     * @see jslt2.ast.NodeVisitor#visit(jslt2.ast.ImportExpr)
     */
    @Override
    public void visit(ImportExpr expr) {
        asm.line(expr.getLineNumber());
        
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
            case LOGICAL_AND: {
                expr.getLeft().visit(this);
                String escape = asm.ifeq();
                
                expr.getRight().visit(this);
                String endif = asm.jmp();
                asm.label(escape);
                asm.loadfalse();
                asm.label(endif);
                break;
            }
            case LOGICAL_OR: {
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
            case LOGICAL_AND:     asm.and();  break;
            case LOGICAL_OR:      asm.or();   break;
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

    /* (non-Javadoc)
     * @see jslt2.ast.NodeVisitor#visit(jslt2.ast.MatchExpr)
     */
    @Override
    public void visit(MatchExpr expr) {
        asm.line(expr.getLineNumber());
        
    }
        
    
}

