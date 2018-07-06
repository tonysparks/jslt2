/*
 * see license.txt
 */
package jslt2.vm.compiler;

import jslt2.Jslt2;
import jslt2.Jslt2Exception;
import jslt2.ast.ProgramExpr;
import jslt2.vm.Bytecode;

/**
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
        
        BytecodeGeneratorVisitor visitor = new BytecodeGeneratorVisitor(runtime, new EmitterScopes());
        visitor.visit(program);
        
        BytecodeEmitter em = visitor.getAsm();
        return em.compile();
    }

}
