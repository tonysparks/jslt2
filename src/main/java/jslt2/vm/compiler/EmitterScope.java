/*
 * see license.txt
 */
package jslt2.vm.compiler;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import jslt2.Jslt2Exception;

/**
 * Used to keep track of the current scope while compiling/emitting bytecode.
 * 
 * @author Tony
 *
 */
public class EmitterScope {

    /**
     * Scope type
     * @author Tony
     *
     */
    public static enum ScopeType {
        LOCAL_SCOPE,
        OBJECT_SCOPE,
        GLOBAL_SCOPE
        ;
    }
    
    /**
     * Constants pool
     */
    private Constants constants;
    
    /**
     * Local variables
     */
    private Locals locals;
    
    /**
     * Closure 'outer' variables
     */
    private Outers outers;
    
    /**
     * Function indexes
     */
    private Map<String, Integer> functions;
    
    
    /**
     * Max stack space needed for this scope
     */
    private int maxstacksize;
    
    /**
     * The type of scope this is
     */
    private ScopeType scopeType;
    
    
    /**
     * Parent Scope
     */
    private EmitterScope parent;
    
    
    /**
     * The bytecode instructions
     */
    private Instructions instructions;  
    private Labels labels;
    
    
    /**
     * Lexical scopes of local variables
     */
    private Stack<Integer> lexicalScopes;
    
   
    /**
     * Debug information symbols
     */
    private DebugSymbols debugSymbols;
    
    private boolean usesLocals;
    private boolean debug;    
    
    private int currentLineNumber;
    private int numArgs;
    
    
    /**
     * @param parent
     * @param scopeType 
     */
    public EmitterScope(EmitterScope parent, ScopeType scopeType) {
        this.parent = parent;
        this.scopeType = scopeType;
        this.maxstacksize = 2; /* always leave room for binary operations */
        
        this.usesLocals = false;
        
        this.currentLineNumber = -1;
        this.lexicalScopes = new Stack<Integer>();
        this.debugSymbols = new DebugSymbols();
        
        this.usesLocals = scopeType == ScopeType.LOCAL_SCOPE;
        
        this.instructions = new Instructions();     
        this.labels = new Labels();
    }
    
    /**
     * @return the debugSymbols
     */
    public DebugSymbols getDebugSymbols() {
        return debugSymbols;
    }
    
    /**
     * @return the numArgs
     */
    public int getNumArgs() {
        return numArgs;
    }
    
    /**
     * @param numArgs the numArgs to set
     */
    public void setNumArgs(int numArgs) {
        this.numArgs = numArgs;
    }
       
    /**
     * Retrieves the raw instruction set that has been built up.
     * @return the fixed array size (i.e., all element in the array are
     * populated with an instruction) of the instructions.
     */
    public int[] getRawInstructions() {
        return this.instructions.truncate();
    }
    
    /**
     * @return the currentLineNumber
     */
    public int getCurrentLineNumber() {
        return currentLineNumber;
    }
    
    /**
     * @param currentLineNumber the currentLineNumber to set
     */
    public void setCurrentLineNumber(int currentLineNumber) {
        this.currentLineNumber = currentLineNumber;
    }

    
    /**
     * Determines if this {@link EmitterScope} has a parent
     * @return true if there is a parent {@link EmitterScope}
     */
    public boolean hasParent() {
        return this.parent != null;
    }
    
    /**
     * @return the parent
     */
    public EmitterScope getParent() {
        return parent;
    }
    
    /**
     * @return the scopeType
     */
    public ScopeType getScopeType() {
        return scopeType;
    }
    
    /**
     * @return the debug
     */
    public boolean isDebug() {
        return debug;
    }
    
    /**
     * @param debug the debug to set
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    
    /**
     * @return true if the current scope stores variables on the stack
     * or in the current environment
     */
    public boolean usesLocals() {
        return usesLocals || !lexicalScopes.isEmpty();
    }

    
    /**
     * Adds the symbol to the {@link Locals}.
     * 
     * @param reference
     * @return the index it is stored in the locals table
     */
    public int addLocal(String reference) {
        if(isDebug()) {
            debugSymbols.store(reference, getInstructionCount());
        }
        
        Locals locals = getLocals();
        return locals.store(reference);
    }
    
    public Map<String, Integer> getFunctions() {
        if(this.functions == null) {
            this.functions = new HashMap<>();
        }
        
        return this.functions;
    }
    
    public void addFunction(String reference, int bytecodeIndex) {
        getFunctions().put(reference, bytecodeIndex);
    }
    
    public int getFunction(String reference) {
        return getFunctions().get(reference);
    }
    
    /**
     * Adds an instruction
     * 
     * @param instruction
     */
    public void addInstr(int instruction) {
        instructions.add(instruction);
    }
    
    /**
     * Reconcile the labels, will correctly mark
     * the <code>jump</code> labels with the correct instruction 
     * positions. 
     */
    public void reconcileLabels() {
        getLabels().reconcileLabels(getInstructions());
    }
    
    /**
     * @return the instructions
     */
    public Instructions getInstructions() {
        return instructions;
    }
    
    /**
     * @return the number of instructions
     */
    public int getInstructionCount() {
        return getInstructions().getCount();
    }
    
    /**
     * Mark the beginning of an inner scope
     */
    public void markLexicalScope() {
        int index = getLocals().getIndex();
        lexicalScopes.push(index);
        
        if(isDebug()) {
            debugSymbols.startScope(getInstructionCount());
        }
    }
    
    /**
     * Leave the scope
     */
    public void unmarkLexicalScope() {
        if(lexicalScopes.isEmpty()) {
            throw new Jslt2Exception("Illegal lexical scope");
        }
        
        /*
         * This allows us for reusing the stack space
         * for other local variables that will be in
         * of scope by the time they get here
         */
        int index = lexicalScopes.pop();
        int currentIndex = getLocals().getIndex();
        if(currentIndex != index) {         
            getLocals().setIndex(index);                
        }
        
        if(isDebug()) {
            debugSymbols.endScope(getInstructionCount());
        }
    }
    
    /**
     * @return the maxstacksize
     */
    public int getMaxstacksize() {
        return maxstacksize;
    }

    /**
     * Increments the allocated stack size by delta.
     * @param delta
     */
    public void incrementMaxstacksize(int delta) {
        this.maxstacksize += delta;
    }
    
    /**
     * @return the constants
     */
    public Constants getConstants() {
        if ( constants == null ) {
            constants = new Constants();
        }
        return constants;
    }

    /**
     * @return true if there are constants in this scope
     */
    public boolean hasConstants() {
        return constants != null && constants.getNumberOfConstants() > 0;
    }

    /**
     * @return the globals
     */
    public Outers getOuters() {
        if ( outers == null ) {
            outers = new Outers();
        }
        return outers;
    }

    /**
     * @return true if there are outers in this scope
     */
    public boolean hasOuters() {
        return outers != null && outers.getNumberOfOuters() > 0;
    }
    
    /**
     * @return the labels
     */
    public Labels getLabels() {
        return labels;
    }

    /**
     * @return the locals
     */
    public Locals getLocals() {
        if ( locals == null ) {
            locals = new Locals();
        }
        return locals;
    }

    /**
     * @return true if there are locals for this scope
     */
    public boolean hasLocals() {
        return locals != null && locals.getNumberOfLocals() > 0;
    }


    /**
     * Finds a reference, generating an {@link OuterDesc} if found
     * 
     * @param reference
     * @return the {@link OuterDesc} that describes the {@link Outer}, which
     * includes its local index and the up value (the number of scopes above
     * this current scope).
     */
    public OuterDesc find(String reference) {
        OuterDesc upvalue = null;
        
        int up = 0;
        EmitterScope scope = this;
        while(scope != null) {
            if(scope.hasLocals()) {
                Locals locals = scope.getLocals();
                int index = locals.get(reference);
                if ( index > -1) {
                    upvalue = new OuterDesc(index, up);             
                    break;
                }
            }
            
            scope = scope.getParent();
            up++;
        }
        
        return upvalue;
    }  
}
