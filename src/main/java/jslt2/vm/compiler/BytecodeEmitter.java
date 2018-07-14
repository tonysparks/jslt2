/*
    Leola Programming Language
    Author: Tony Sparks
    See license.txt
*/
package jslt2.vm.compiler;

import static jslt2.vm.Opcodes.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;

import jslt2.util.ArrayUtil;
import jslt2.vm.Bytecode;
import jslt2.vm.Opcodes;
import jslt2.vm.compiler.EmitterScope.ScopeType;



/**
 * Easily emit opcode codes via the assembler methods.  There are also some helper (non-opcode) methods here to ease the
 * development of emitting instructions.
 * 
 * <p>
 * When using the {@link BytecodeEmitter}, in general you will only need one instance to compile code as all the scope
 * handling is abstracted away by the use of {@link BytecodeEmitter#start(ScopeType)} and {@link BytecodeEmitter#end()}
 * methods.
 * 
 * <p>
 * Example usage:
 * <pre>
 *    BytecodeEmitter em = new BytecodeEmitter();
 *    
 *    // start compiling using the global scope
 *    em.start(ScopeType.GLOBAL_SCOPE);
 *    {
 *        em.funcdef(2, false);
 *        {
 *          // all instructions will be for the newly defined function scope
 *          em.loadlocal(0);
 *          em.loadlocal(1);
 *          // ...
 *          
 *        }
 *        em.end(); // pops the function definition scope, returning control to the parent scope (global scope)
 *    } 
 *    em.end(); // pops the global scope
 * </pre>
 * 
 * @author Tony
 *
 */
public class BytecodeEmitter {    
    
    /**
     * Keeps track of the embedded scopes
     */
    private Stack<BytecodeEmitter> innerEmitterStack;
    
    /**
     * Keeps track of all of the scopes that we have 
     * created in this Emitter; so that we can compile bytecode for 
     * them
     */
    private List<BytecodeEmitter> innerEmmitters;
    
    /**
     * The local scope of a {@link BytecodeEmitter}
     */
    private EmitterScope localScope;
    
    /**
     * The global scope stack
     */
    private EmitterScopes scopes;
    
    
    /**
     * If we are in debugging mode
     */
    private boolean isDebugMode;
    
    
    /**
     */
    public BytecodeEmitter() {
        this(new EmitterScopes());
    }
    
    /**
     * @param scopes keeps track of created {@link EmitterScope}'s
     */
    public BytecodeEmitter(EmitterScopes scopes) {
        this.scopes = scopes;

        this.innerEmitterStack = new Stack<BytecodeEmitter>();
        this.innerEmmitters = new ArrayList<BytecodeEmitter>();
    }

    
    /**
     * @return true if the current scope stores variables on the stack
     * or in the current environment
     */
    public boolean usesLocals() {
        return peek().localScope.usesLocals();
    }

    /**
     * Mark the beginning of an inner scope
     */
    public void markLexicalScope() {
        peek().localScope.markLexicalScope();
    }
    
    /**
     * Leave the scope
     */
    public void unmarkLexicalScope() {
        peek().localScope.unmarkLexicalScope();
    }
    
    /**
     * @return the debug
     */
    public boolean isDebug() {
        return this.isDebugMode;
    }
    
    /**
     * @param debug the debug to set
     */
    public void setDebug(boolean debug) {
        this.isDebugMode = debug;
        if(this.localScope != null) {
            this.localScope.setDebug(debug);
        }
    }
    
    private void incrementMaxstackSize(int delta) {
        peek().localScope.incrementMaxstacksize(delta);
    }
    
    private void incrementMaxstackSize() {
        peek().localScope.incrementMaxstacksize(1);
    }
    private void decrementMaxstackSize(int delta) {
        peek().localScope.incrementMaxstacksize(-delta);
    }
    
    private void decrementMaxstackSize() {
        peek().localScope.incrementMaxstacksize(-1);
    }
    
    
    /**
     * @return the max stack size this bytecode chunk will require
     */
    public int getMaxstacksize() {
        return peek().localScope.getMaxstacksize();
    }
        
    /**
     * Set the number of arguments this {@link BytecodeEmitter} can accept.
     * 
     * @param numberOfArgs
     */
    public void setNumberOfArgs(int numberOfArgs) {
        peek().localScope.setNumArgs(numberOfArgs);
    }

    /**
     * The current active {@link BytecodeEmitter}.  {@link BytecodeEmitter} may contain
     * sub-scopes (aka {@link BytecodeEmitter} within {@link BytecodeEmitter}'s).  This 
     * will return the current active one.  The {@link BytecodeEmitter#start(ScopeType)} will always
     * place <code>this</code> instance on the stack.
     * 
     * <p>
     * Unless stated otherwise, all methods within the {@link BytecodeEmitter} will always take
     * the <b>head</b> of the stack ( via {@link #peek()}).  That is, all operations invoked are operated on
     * the current scoped {@link BytecodeEmitter}.
     * 
     * @return the current active {@link BytecodeEmitter}
     */
    public BytecodeEmitter peek() {
        return this.innerEmitterStack.peek();
    }   
        
    /**
     * Starts an assembler scope.  Any {@link Bytecode} to be generated must be contained within
     * a scope.  The {@link BytecodeEmitter#start(ScopeType)} and {@link BytecodeEmitter#end()} coordinate the
     * life cycle of the {@link Bytecode} scope.
     * 
     * <p>
     * Scopes are created in {@link LeoNamespace}, {@link LeoClass}, {@link LeoFunction}, and {@link LeoGenerator}.  These
     * scopes are not to be confused with the lexical-scopes ({@link BytecodeEmitter#markLexicalScope()} and {@link BytecodeEmitter#unmarkLexicalScope()});
     * which a lexical-scope only accounts for the life cycle of a local variable as it pertains the the current scope.
     * 
     * <p>
     * These assembler scopes can be embedded within other assembler scopes because Leola allows to define scoped objects within
     * other scoped objects.  As an example:
     * 
     * <pre>
     *   class ClassScope() {
     *     var functionScope = def() {
     *       var innerFunctionScope = def() {
     *       }
     *     }
     *    }
     * </pre>
     * 
     * <p>
     * In the example code above, we have three scopes defined (<code>ClassScope</code>, <code>functionScope</code> and <code>innerFunctionScope</code>), 
     * all contained within one parent scope of the global {@link LeoNamespace} scope.  Each subsequent scope is stored in within its parent scope.
     * 
     * 
     * 
     * @param scopeType the type of scope
     * @param numberOfArguments
     * @param hasVarargs
     */
    public void start(ScopeType scopeType, int numberOfArguments) {        
        this.localScope = scopeType == ScopeType.GLOBAL_SCOPE ? 
                              this.scopes.getGlobalScope()
                            : this.scopes.pushScope(scopeType); 
                              
        this.localScope.setNumArgs(numberOfArguments);
        this.localScope.setDebug(isDebugMode);
        
        this.innerEmitterStack.push(this);
    }
    

    
    /**
     * Ends a scope, which will pop the current embedded {@link BytecodeEmitter} (if any).
     * 
     * @see BytecodeEmitter#start(ScopeType, int, boolean)
     */
    public BytecodeEmitter end() {    
        
        reconcileLabels();
        
        reconcileOuters(peek());
        
        this.scopes.popScope();
        
        BytecodeEmitter asm = this;
        if (!this.innerEmitterStack.isEmpty()) {
            asm = this.innerEmitterStack.pop();
        }
        
        return asm;
    }
    
    /**
     * Reconciles the labels
     */
    private void reconcileLabels() {
        peek().localScope.reconcileLabels();        
    }
        
    /**
     * Reconciles the outer variables
     * 
     * @param asm - this scoped Asm
     */
    private void reconcileOuters(BytecodeEmitter asm) {
        Outers outers = asm.getOuters();

        for (int i = 0; i < outers.getNumberOfOuters(); i++) {
            OuterDesc outer = outers.get(i);            
            int outerUpIndex = outer.getUp();
            
            /*
             * If the outer is not in this scope (UP index is != 0),
             * then we must find the parent Scope to reconcile
             * it
             */
            if (outerUpIndex > 0) {
                
                EmitterScope scope = asm.localScope;
                if (scope != null) {

                    /* find the asm from the parent scope */
                    EmitterScope parentScope = scope.getParent();
                    BytecodeEmitter outerAsm = findAsmByScope(parentScope);
                    if (outerAsm != null) {
                        int nup = outerUpIndex - 1;

                        /* if the outer is several parent scopes deep, we'll need to store
                         * an outer in this parent scope, so that we can chain this outer
                         * to finally become an xLOAD_LOCAL once it reaches the appropriate
                         * scope
                         */
                        if (nup > 0) {
                            int store = outerAsm.localScope.getOuters().store(new OuterDesc(outer.getIndex(), nup));
                            outerAsm.linstrx(xLOAD_OUTER, store);
                        }
                        else {
                            outerAsm.linstrx(xLOAD_LOCAL, outer.getIndex());                            
                        }
                    }
                }
            }
        }
    }
    
    /**
     * @return the globals
     */
    public Outers getOuters() {
        return peek().localScope.getOuters();
    }
    
    /**
     * @return the constants
     */
    public Constants getConstants() {
        return peek().localScope.getConstants();
    }
    
    /**
     * @return the locals
     */
    public Locals getLocals() {
        return peek().localScope.getLocals();
    }

    
    /**
     * Reserve space for the number of locals
     * @param numberOfLocals the number of locals to reserve space for
     */
    public void allocateLocals(int numberOfLocals) {
        getLocals().allocate(numberOfLocals);
    }
    

    /**
     * Finds the associated {@link BytecodeEmitter} by Scope
     * @param scope the scope to find the representative {@link BytecodeEmitter}
     * @return the {@link BytecodeEmitter} if found, null if not found
     */
    private BytecodeEmitter findAsmByScope(EmitterScope scope) {
        for(int i = 0; i < this.innerEmitterStack.size(); i++ ) {
            BytecodeEmitter asm = this.innerEmitterStack.get(i);
            
            /* check by reference */
            if ( asm.localScope == scope ) {
                return asm;
            }
        }
        
        return null;
    }
    
    /**
     * Stores the {@link JsonNode} in the constant table.
     * 
     * @param obj
     * @return the index at which the constant is stored.
     */
    public int addConst(JsonNode obj) {
        Constants constants = getConstants();
        return constants.store(obj);
    }

    /**
     * Stores the {@link JsonNode} in the constants table and
     * emits a load instruction for it.
     * 
     * @param obj
     */
    public void addAndloadconst(JsonNode obj) {
        int index = addConst(obj);
        loadconst(index);
    }
    
    /**
     * Stores the constant 'str' into the constants table and
     * emits a load instruction for it.
     * 
     * @param str
     */
    public void addAndloadconst(String str) {
        addAndloadconst(TextNode.valueOf(str));
    }
    
    
    /**
     * Stores the integer in the constants table and
     * emits a load instruction for it.
     * 
     * @param i
     */
    public void addAndloadconst(int i) {
        addAndloadconst(IntNode.valueOf(i));
    }
    
    
    /**
     * Stores the double in the constants table and
     * emits a load instruction for it.
     * 
     * @param i
     */
    public void addAndloadconst(double i) {
        addAndloadconst(DoubleNode.valueOf(i));
    }
    
    
    /**
     * Stores the long in the constants table and
     * emits a <code>loadconst</code> instruction for it.
     * 
     * @param i
     */
    public void addAndloadconst(long i) {
        addAndloadconst(LongNode.valueOf(i));
    }
        
    /**
     * Invokes a load instruction of the variable, either as a <code>loadlocal</code> or a <code>loadouter</code>.
     * 
     * @see Outer
     * @see BytecodeEmitter#loadlocal(int)
     * @see BytecodeEmitter#loadouter(int)
     * @param ref the reference of the variable name to load
     * @return true if loaded to either as a local or an outer; false if it was not found in either the 
     * local storage or any parent scopes (i.e., Outer).
     */
    public boolean load(String ref) {
        boolean success = true;            
        int i = getLocals().get(ref);
        if (i > -1) {
            loadlocal(i);
        }        
        else {                    
            
            OuterDesc upvalue = this.scopes.peek().find(ref);
            if (upvalue == null) {                
                success = false;
            }
            else {
                Outers outers = getOuters();
                int store = outers.store(upvalue);
                loadouter(store);
            }    
        }        
                
        return success;
    }
    
    public void addFunction(String reference, int bytecodeIndex) {
        peek().localScope.addFunction(reference, bytecodeIndex);
    }
    
    public int getFunction(String reference) {
        return peek().localScope.getFunction(reference);
    }
    
    public void addPendingFunction(String reference) {
        peek().localScope.addPendingFunction(reference);
    }
        
    /**
     * Adds the symbol to the {@link Locals}.
     * 
     * @param reference
     * @return the index it is stored in the locals table
     */
    public int addLocal(String reference) {
        return peek().localScope.addLocal(reference);
    }

    /**
     * Adds the reference to the {@link Locals} pool and calls 
     * invokes a {@link BytecodeEmitter#storelocal(int)}.
     * 
     * @see BytecodeEmitter#addLocal(String)
     * @see BytecodeEmitter#storelocal(int)
     * @param reference
     */
    public void addAndstorelocal(String reference) {
        int index = addLocal(reference);
        storelocal(index);
    }
    
    /**
     * @return the number of instructions
     */
    public int getInstructionCount() {
        return getInstructions().getCount();
    }
    
    /**
     * @return the labels
     */
    public Labels getLabels() {
        return peek().localScope.getLabels();
    }
    
    /**
     * @return the instructions
     */
    public Instructions getInstructions() {
        return peek().localScope.getInstructions();
    }

    /**
     * @return the localScope
     */
    public EmitterScope getLocalScope() {
        return peek().localScope;
    }
    
    /**
     * @return the current instruction
     */
    private int peekInstr() {
        Instructions instrs = peek().localScope.getInstructions();
        return instrs.peekLast();
    }
    
    
    
    /**
     * Adds an instruction to this (local) scoped Asm
     * 
     * @param opcode
     * @param argx
     */
    private void linstrx(int opcode, int argx) {
        localScope.addInstr(SET_ARGx(opcode, argx));
    }
    
    /**
     * Outputs an instruction with no arguments
     * 
     * @param instruction
     */
    private void instr(int instruction) {
        peek().localScope.addInstr(instruction);
    }
    
    /**
     * Outputs an instruction with 1 (x) argument
     * 
     * @param opcode
     * @param argx (x size argument -- see {@link Opcodes}).
     */
    private void instrx(int opcode, int argx) {
        instr(SET_ARGx(opcode, argx));
    }
    
    /**
     * Outputs an instruction with 1 (x-signed) argument
     * 
     * @param opcode
     * @param argsx (x signed size argument -- see {@link Opcodes}).
     */
    private void instrsx(int opcode, int argsx) {
        instr(SET_ARGsx(opcode, argsx));
    }
    /**
     * Outputs an instruction with 2 arguments
     * 
     * @param opcode
     * @param arg1
     * @param arg2
     */
    private void instr2(int opcode, int arg1, int arg2) {
        instr(SET_ARG2(SET_ARG1(opcode, arg1), arg2));
    }
    
    /**
     * Marks a Label, so it can be eventually calculated for a jmp delta
     * 
     * @param opcode
     * @param label
     */
    private void markLabel(int opcode, String label) {        
        instr(opcode); // will eventually be replaced
        getLabels().markLabel(this, label, opcode);                
    }

    /**
     * Constructs a new embedded {@link BytecodeEmitter} with the 
     * local scope.
     * 
     * @param numberOfParameters
     */
    private void newLocalScopeEmitter(int numberOfParameters) {
        BytecodeEmitter asm = new BytecodeEmitter(this.scopes);
        asm.start(ScopeType.LOCAL_SCOPE, numberOfParameters);
        asm.setDebug(this.isDebug());

        peek().innerEmmitters.add(asm);
        this.innerEmitterStack.push(asm);
    }
    
    /**
     * Creates a label with the supplied name
     * 
     * @param name
     */
    public void label(String name) {        
        getLabels().setLabel(this, name);
    }
    
    
    /**
     * Creates a label with a sequenced name.
     * 
     * @return the label name
     */
    public String label() {
        String labelName = nextLabelName();
        label(labelName);
        
        return labelName;
    }
    
    
    /**
     * Generates the next sequenced labeled name
     * 
     * @return the labeled name
     */
    public String nextLabelName() {
        return peek().getLabels().nextLabelName();
    }
    
    /**
     * The current bytecode index of this {@link BytecodeEmitter}.  That is,
     * the index to be used to retrieve this {@link Bytecode} from {@link Bytecode#inner}
     *  
     * @return the index that references this {@link BytecodeEmitter}
     */
    public int getBytecodeIndex() {
        return peek().innerEmmitters.size();
    }
    
    /*================================================================================
     * The Assembler
     *================================================================================*/
     
    
    
    /**
     * Emits the LINE opcode.  Only enabled if the debug flags are set.  This marks
     * the line numbers in the Leola script with the associated byte code.
     * 
     * <p>
     * As an optimization, this will only emit the <code>line</code> instruction if the supplied
     * line number is greater than the last stored line number.  This helps prevent unnecessary
     * <code>line</code> instructions which would further impact performance.
     *  
     * @param line the line number in the Leola script code
     */
    public void line(int line) {
        
        if (this.isDebug()) {
            if (line != peek().localScope.getCurrentLineNumber() && line != 0 
                && (getInstructionCount() >= 0 )) {

                peek().localScope.setCurrentLineNumber(line);
                
                if (getInstructionCount()==0 || OPCODE(peekInstr()) != LINE) {
                    instrx(LINE, line);
                }
                else {
                    // replace the last instruction (which is a LINE)
                    // with the updated line number
                    getInstructions().setLast(SET_ARGx(LINE, line));
                }
            }
        }
    }
    
    public void loadconst(int index) {
        instrx(LOAD_CONST, index);    
        
        incrementMaxstackSize();
    }
    public void loadlocal(int index) {        
        instrx(LOAD_LOCAL, index);
        incrementMaxstackSize();
    }

    public void loadouter(int index) {
        instrx(LOAD_OUTER, index);
        incrementMaxstackSize();
    }   
    
    public void storelocal(int index) {        
        instrx(STORE_LOCAL, index);
        decrementMaxstackSize();
    }
            
    public void loadnull() {
        instr(LOAD_NULL);
        incrementMaxstackSize();
    }
    public void loadtrue() {
        instr(LOAD_TRUE);
        incrementMaxstackSize();
    }
    public void loadfalse() {
        instr(LOAD_FALSE);
        incrementMaxstackSize();
    }
    
    public void jmp(String label) {
        markLabel(JMP, label);
    }
    public String jmp() {
        String label = nextLabelName();
        jmp(label);
        return label;
    }
    
    public void jmp(int offset) {
        if(offset != 0) {
            instrsx(JMP, offset);
        }
    }
        
    public void newobj() {
        instr(NEW_OBJ);        
    }
    public void sealobj() {
        instr(SEAL_OBJ);
        incrementMaxstackSize();
    }
    
    public void newarray() {
        instr(NEW_ARRAY);        
    }    
    
    public void sealarray() {
        instr(SEAL_ARRAY);
        incrementMaxstackSize();
    }
    
    public void addfieldk(String fieldName) {
        int index = addConst(TextNode.valueOf(fieldName));
        addfieldk(index);
    }
    
    public void addfieldk(int constIndex) {
        instrx(ADD_FIELDK, constIndex);
        decrementMaxstackSize();
    }
    
    public void addfield() {
        instr(ADD_FIELD);
        decrementMaxstackSize(2);
    }
    
    public void addelement() {
        instr(ADD_ELEMENT);
        decrementMaxstackSize();
    }
    
    public void arrayslice() {
        instr(ARRAY_SLICE);
        decrementMaxstackSize(2);
    }
    
    public void loadinput() {
        instr(LOAD_INPUT);
        incrementMaxstackSize();
    }
    
    public void forarraydef() {
        instrx(FOR_ARRAY_DEF, getBytecodeIndex());        
        newLocalScopeEmitter(0);
    }
    
    public void forobjdef() {
        instrx(FOR_OBJ_DEF, getBytecodeIndex());        
        newLocalScopeEmitter(0);
    }
        
    public void matcher(int numOfOmittedFields) {
        instrx(MATCHER, numOfOmittedFields);
        decrementMaxstackSize(numOfOmittedFields);
    }
    
    public void getfieldk(int constIndex) {
        instrx(GET_FIELDK, constIndex);        
    }
    public void getfieldk(String stringconst) {
        int index = getConstants().store(stringconst);
        instrx(GET_FIELDK, index);
    }
    
    public void getfield() {
        instr(GET_FIELD);
        incrementMaxstackSize();
    }
            
    public void funcdef(int numberOfParameters) {
        instrx(FUNC_DEF, getBytecodeIndex());        
        incrementMaxstackSize(numberOfParameters);
        
        newLocalScopeEmitter(numberOfParameters);        
    }
               
    public void ifeq(String label) {
        markLabel(IFEQ, label);
        decrementMaxstackSize();
    }
    public void ifeq(int offset) {
        instrsx(IFEQ, offset);
        decrementMaxstackSize();
    }
    
    public String ifeq() {
        String labelName = nextLabelName();
        ifeq(labelName);
        
        return labelName;
    }
    
    public void invoke(int numberOfArgs, int bytecodeIndex) {
        instr2(INVOKE, numberOfArgs, bytecodeIndex);          
        decrementMaxstackSize(numberOfArgs-1);
    }
    
    public void userinvoke(int numberOfArgs, String functionName) {
        int index = addConst(TextNode.valueOf(functionName));
        instr2(USER_INVOKE, numberOfArgs, index);
        decrementMaxstackSize(numberOfArgs-1);
    }
    
    public void userinvoke(int numberOfArgs, int functionNameConstIndex) {        
        instr2(USER_INVOKE, numberOfArgs, functionNameConstIndex);
        decrementMaxstackSize(numberOfArgs-1);
    }
       
        
    /* arithmetic operators */
    public void add() {
        instr(ADD);
        decrementMaxstackSize();
    }
    public void sub() {
        instr(SUB);
        decrementMaxstackSize();
    }
    public void mul() {
        instr(MUL);
        decrementMaxstackSize();
    }
    public void div() {
        instr(DIV);
        decrementMaxstackSize();
    }
    public void mod() {
        instr(MOD);
        decrementMaxstackSize();
    }
    public void neg() {
        instr(NEG);
    }
        
    public void or() {
        instr(OR);
        decrementMaxstackSize();
    }
    public void and() {
        instr(AND);
        decrementMaxstackSize();
    }
    public void not() {
        instr(NOT);
    }
        
    public void eq() {
        instr(EQ);
        decrementMaxstackSize();
    }
    public void neq() {
        instr(NEQ);
        decrementMaxstackSize();
    }
    public void gt() {
        instr(GT);
        decrementMaxstackSize();
    }
    public void gte() {
        instr(GTE);
        decrementMaxstackSize();
    }
    public void lt() {
        instr(LT);
        decrementMaxstackSize();
    }
    public void lte() {
        instr(LTE);
        decrementMaxstackSize();
    }
    
    /**
     * Compiles the assembler into {@link Bytecode}.
     * 
     * @return the {@link Bytecode} object
     */
    public Bytecode compile() {
        localScope.reconcilePendingFunctions();
        
        int[] code = localScope.getRawInstructions();
        Bytecode bytecode = new Bytecode(code);
                    
        bytecode.numArgs = localScope.getNumArgs();
        
        if(this.localScope.hasOuters()) {
            Outers outers = this.localScope.getOuters();
            bytecode.numOuters = outers.getNumberOfOuters();
            bytecode.outers = new JsonNode[bytecode.numOuters];
        }
        
        
        bytecode.paramNames = new JsonNode[bytecode.numArgs];
        if(this.localScope.hasLocals()) {
            Locals locals = this.localScope.getLocals();
            bytecode.numLocals = locals.getNumberOfLocals();
            
            if(bytecode.numArgs > 0) {
                for(int i = 0; i < bytecode.numArgs; i++) {
                    String ref = locals.getReference(i);
                    if(ref != null) {
                        bytecode.paramNames[i] = TextNode.valueOf(ref);
                    }
                }
            }
        }
        
        /* we only care about this for classes */
        if (isDebug()) {
            bytecode.setDebug();
            bytecode.debugSymbols = localScope.getDebugSymbols();
        }
                                        
        if (this.localScope.hasConstants()) {
            Constants constants = this.localScope.getConstants();
            bytecode.constants = constants.compile();
            bytecode.numConstants = constants.getNumberOfConstants();
        }
        else {
            bytecode.numConstants = 0;
            bytecode.constants = ArrayUtil.EMPTY_NODES;
        }
        
        int stacksize = this.localScope.getMaxstacksize();
        stacksize += bytecode.numArgs;
        stacksize += bytecode.numLocals; 
        stacksize += bytecode.numConstants;                
        stacksize += bytecode.numOuters;        
                
        bytecode.maxstacksize = stacksize;        
        
        
        bytecode.numInners = this.innerEmmitters.size();
        bytecode.inner = new Bytecode[bytecode.numInners];
        for(int i = 0; i < bytecode.inner.length; i++) {
            bytecode.inner[i] = this.innerEmmitters.get(i).compile();
            if(this.localScope == this.scopes.getGlobalScope()) {                
                bytecode.inner[i].setGlobalBytecode(bytecode);
            }
        }
                
        return bytecode;        
    }
}

