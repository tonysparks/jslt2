/*
 * see license.txt
 */
package jslt2.vm;

import static jslt2.vm.Opcodes.*;

import java.util.Stack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import jslt2.Jslt2;
import jslt2.Jslt2Exception;
import jslt2.vm.compiler.Outer;
import jslt2.vm.compiler.Outer.StackValue;

/**
 * @author Tony
 *
 */
public class VM {

    /**
     * Default stack size
     */
    public static final int DEFAULT_STACKSIZE = 1024;
    
    private Jslt2 runtime;
    
    /* Value stack 
     */
    private Stack<JsonNode> valueStack;
    

    /*thread stack
     */
    private JsonNode[] stack;
    

    /* list of open outers, if this function goes out of scope (i.e., the stack) then the outers
     * are closed (i.e., the value contained on the stack is transferred used instead of the indexed value
     */
    private Outer[] openouters;
    private int top;

    /**
     * The maximum stack size
     */
    private final int maxStackSize;
    
    
    /**
     * The stack value accounts for closures requesting a value off
     * of the stack and when the are finally 'closed' over.
     * 
     * We can't just use the VM.stack variable when closing over
     * the Outer because the VM.stack variable may be replaced
     * when the stack grows.
     */
    private StackValue vmStackValue = new StackValue() {        
        @Override
        public JsonNode getStackValue(int index) {
            return stack[index];
        }
        
        @Override
        public void setStackValue(int index, JsonNode value) {         
            stack[index] = value;
        }
    };
    
    /**
     * 
     */
    public VM(Jslt2 runtime) {
        this.runtime = runtime;
        
        int stackSize = runtime.getMinStackSize();
        stackSize = (stackSize <= 0) ? DEFAULT_STACKSIZE : stackSize;

        this.maxStackSize = Math.max(runtime.getMaxStackSize(), stackSize);
        
        this.valueStack = new Stack<>();
        
        this.stack = new JsonNode[stackSize];
        this.openouters = new Outer[stackSize];
        this.top = 0;        
    }

    public JsonNode execute(Bytecode code, JsonNode input) throws Jslt2Exception {
        final int base = top;
        prepareStack(code);        
        
        return executeStackFrame(code, base, input);        
    }
    
    public JsonNode execute(Bytecode code, JsonNode input, JsonNode arg1) throws Jslt2Exception {
        final int base = top;
        prepareStack(code);        
        
        stack[base + 0] = arg1;
        
        return executeStackFrame(code, base, input);        
    }
    
    public JsonNode execute(Bytecode code, JsonNode input, JsonNode arg1, JsonNode arg2) throws Jslt2Exception {
        final int base = top;
        prepareStack(code);        
        
        stack[base + 0] = arg1;
        stack[base + 1] = arg2;
        
        return executeStackFrame(code, base, input);        
    }
    
    public JsonNode execute(Bytecode code, JsonNode input, JsonNode arg1, JsonNode arg2, JsonNode arg3) throws Jslt2Exception {
        final int base = top;
        prepareStack(code);        
        
        stack[base + 0] = arg1;
        stack[base + 1] = arg2;
        stack[base + 2] = arg3;
        
        return executeStackFrame(code, base, input);        
    }
    
    public JsonNode execute(Bytecode code, JsonNode input, JsonNode arg1, JsonNode arg2, JsonNode arg3, JsonNode arg4) throws Jslt2Exception {
        final int base = top;
        prepareStack(code);        
        
        stack[base + 0] = arg1;
        stack[base + 1] = arg2;
        stack[base + 2] = arg3;
        stack[base + 3] = arg4;
        
        return executeStackFrame(code, base, input);        
    }
    
    public JsonNode execute(Bytecode code, JsonNode input, JsonNode arg1, JsonNode arg2, JsonNode arg3, JsonNode arg4, JsonNode arg5) throws Jslt2Exception {
        final int base = top;
        prepareStack(code);        
        
        stack[base + 0] = arg1;
        stack[base + 1] = arg2;
        stack[base + 2] = arg3;
        stack[base + 3] = arg4;
        stack[base + 4] = arg5;
        
        return executeStackFrame(code, base, input);        
    }
    
    public JsonNode execute(Bytecode code, JsonNode input, JsonNode[] args) throws Jslt2Exception {
        final int base = top;
        prepareStack(code);        
        
        if (args != null) {
            System.arraycopy(args, 0, stack, base, args.length);
        }
        
        return executeStackFrame(code, base, input);        
    }
    
    private JsonNode executeStackFrame(Bytecode code, int base, JsonNode input) throws Jslt2Exception {
        JsonNode result = NullNode.instance;
        
        final int[] instr = code.instr;
        final int len = code.len;
        int pc = code.pc;


        final JsonNode[] constants = code.constants;
        final Bytecode[] inner = code.inner;
        
        final Outer[] calleeouters = code.outers;
        
        final int topStack = base + code.numLocals;
        top = topStack;
        
        boolean closeOuters = false;

        int lineNumber = -1;
        
        Stack<ForEntry> forStack = null;
        
        try {
            while( pc < len ) {
                int i = instr[pc++];
                int opcode =  i & 255; //OPCODE(i);

                switch(opcode) {
                    case LINE:
                        break;
                    case NEW_OBJ: {
                        this.valueStack.push(this.runtime.newObjectNode());
                        break;     
                    }
                    case SEAL_OBJ: {
                        stack[top++] = this.valueStack.pop();
                        break;
                    }
                    
                    case NEW_ARRAY: {
                        this.valueStack.push(this.runtime.newArrayNode(16));
                        break;       
                    }
                    case SEAL_ARRAY: {
                        stack[top++] = this.valueStack.pop();
                        break;
                    }
                    
                    case ADD_FIELDC: {                       
                        JsonNode node = this.valueStack.peek();
                        if(!node.isObject()) {
                            error(node + " is not an object.");
                        }
                        ObjectNode obj = (ObjectNode)node;
                        
                        int iname = ARGx(i);
                        JsonNode fieldName = constants[iname];
                        obj.set(fieldName.asText(), stack[--top]);
                        
                        break;
                    }
                    case ADD_FIELD: {
                        JsonNode obj = this.valueStack.peek();
                        
                        JsonNode value = stack[--top];
                        JsonNode index = stack[--top];
                        
                        if(obj.isArray()) {
                            ArrayNode array = (ArrayNode)obj;
                            array.set(index.asInt(), value);
                        }
                        else if(obj.isObject()) {
                            ObjectNode object = (ObjectNode)obj;
                            object.set(index.asText(), value);
                        }
                        else {
                            error(obj + " is not an indexable object");
                        }
                        
                        stack[top++] = obj; 
                        break;
                    }
                    case ADD_ELEMENT: {
                        JsonNode node = this.valueStack.peek();
                        if(!node.isArray()) {
                            error(node + " is not an array.");
                        }
                                            
                        ArrayNode array = (ArrayNode)node;
                        array.add(stack[--top]);
                        
                        break;
                    }
                    
                    /* Store operations */
                    case LOAD_CONST: {
                        int iname = ARGx(i);
                        stack[top++] = constants[iname];
                        break;
                    }
                    case LOAD_LOCAL: {
                        int iname = ARGx(i);
                        stack[top++] = stack[base + iname];
                        break;
                    }
                    case LOAD_OUTER: {
                        int iname = ARGx(i);
                        stack[top++] = calleeouters[iname].getValue();
                        break;
                    }
                    case LOAD_NULL: {
                        stack[top++] = NullNode.instance;
                        break;
                    }
                    case LOAD_TRUE: {
                        stack[top++] = BooleanNode.TRUE;
                        break;
                    }
                    case LOAD_FALSE: {
                        stack[top++] = BooleanNode.FALSE;
                        break;
                    }
                    case LOAD_INPUT: {
                        JsonNode obj = null;
                        if(forStack != null && !forStack.isEmpty()) {                            
                            ForEntry it = forStack.peek();
                            obj = it.current();                            
                        }
                        else {
                            obj = input;
                        }
                        
                        stack[top++] = obj;
                        break;
                    }
                    case STORE_LOCAL: {
                        int iname = ARGx(i);
                        stack[base + iname] = stack[--top];
                        break;
                    }
                    case STORE_OUTER: {
                        int iname = ARGx(i);
                        calleeouters[iname].setValue(stack[--top]);
                        break;
                    }

                    /* stack operators */
                    case POP:    {
                        stack[--top] = null;                            
                        break;
                    }
                    case OPPOP:    {
                        if (top>topStack) {
                            stack[--top] = null;
                        }
                        break;
                    }
                    case DUP: {
                        JsonNode obj = stack[top-1];
                        stack[top++] = obj;
                        break;
                    }
                    case JMP:    {
                        int pos = ARGsx(i);
                        pc += pos;
                        break;
                    }
                    case IFEQ:    {
                        JsonNode cond = stack[--top];
                        if (!cond.asBoolean()) {
                            int pos = ARGsx(i);
                            pc += pos;
                        }
                        break;
                    }
                    case FOR_START: {
                        if(forStack == null) {
                            forStack = new Stack<>();
                        }
                        
                        ForEntry entry = new ForEntry(this.runtime, stack[top-1]);
                        forStack.push(entry);
                        
                        break;
                    }
                    case FOR_END: {
                        forStack.pop();
                        break;
                    }
                    case FOR_INC: {
                        if(!forStack.peek().advance()) {
                            pc += ARGsx(i);
                        }
                        break;
                    }
                    case INVOKE:    {
                        int nargs = ARG1(i);
                        int bytecodeIndex = ARG2(i);
                                                                                           
                        Bytecode funCode = inner[bytecodeIndex];                        
                        JsonNode c = null;

                        switch(nargs) {
                            case 0: {
                                c = execute(funCode, input);
                                break;
                            }
                            case 1: {
                                JsonNode arg1 = stack[--top];
                                c = execute(funCode, input, arg1);
                                break;
                            }
                            case 2: {
                                JsonNode arg2 = stack[--top];
                                JsonNode arg1 = stack[--top];
                                c = execute(funCode, input, arg1, arg2);
                                break;
                            }
                            case 3: {
                                JsonNode arg3 = stack[--top];
                                JsonNode arg2 = stack[--top];
                                JsonNode arg1 = stack[--top];
                                c = execute(funCode, input, arg1, arg2, arg3);
                                break;
                            }
                            case 4: {
                                JsonNode arg4 = stack[--top];
                                JsonNode arg3 = stack[--top];
                                JsonNode arg2 = stack[--top];
                                JsonNode arg1 = stack[--top];
                                c = execute(funCode, input, arg1, arg2, arg3, arg4);
                                break;
                            }
                            case 5: {
                                JsonNode arg5 = stack[--top];
                                JsonNode arg4 = stack[--top];
                                JsonNode arg3 = stack[--top];
                                JsonNode arg2 = stack[--top];
                                JsonNode arg1 = stack[--top];
                                c = execute(funCode, input, arg1, arg2, arg3, arg4, arg5);
                                break;
                            }
                            default: {
                                JsonNode[] args = readArrayFromStack(nargs, stack);
                                c = execute(funCode, input, args);
                            }
                        }

                        stack[top++] = c;    
                        break;
                    }
                    case FUNC_DEF: {
                        int innerIndex = ARGx(i);
                        Bytecode bytecode = inner[innerIndex];
                        
                        Outer[] outers = bytecode.outers;                            
                        if (assignOuters(outers, calleeouters, openouters, bytecode.numOuters, base, pc, code)) {
                            closeOuters = true;
                        }
                        pc += bytecode.numOuters;

                        break;
                    }
                    case IDX: {
                        JsonNode index = stack[--top];
                        JsonNode obj = stack[--top];

                        JsonNode value = null;
                        if(obj.isArray()) {
                            value = obj.get(index.asInt());
                        }
                        else if(obj.isObject()) {
                            value = obj.get(index.asText());
                        }
                        else {
                            error(obj + " is not an indexable object");
                        }
                        
                        stack[top++] = value;                            
                        break;
                    }
                    case SIDX: {
                        JsonNode index = stack[--top];
                        JsonNode obj = stack[--top];
                        JsonNode value = stack[--top];
              
                        if(obj.isArray()) {
                            ArrayNode array = (ArrayNode)obj;
                            array.set(index.asInt(), value);
                        }
                        else if(obj.isObject()) {
                            ObjectNode object = (ObjectNode)obj;
                            object.set(index.asText(), value);
                        }
                        else {
                            error(obj + " is not an indexable object");
                        }
                        
                        stack[top++] = obj; 
                        break;
                    }
                    
                    /* object access */
                    case GET: {
                        JsonNode index = stack[--top];
                        JsonNode obj = stack[--top];

                        JsonNode value = null;
                        if(obj.isArray()) {
                            value = obj.get(index.asInt());
                        }
                        else if(obj.isObject()) {
                            value = obj.get(index.asText());
                        }
                        else {
                            error(obj + " is not an indexable object");
                        }
                        
                        stack[top++] = value;
                        break;
                    }
                    case GETK: {
                        int iname = ARGx(i);                        
                        JsonNode index = constants[iname];
                        JsonNode obj = stack[--top];

                        JsonNode value = null;
                        if(obj.isArray()) {
                            value = obj.get(index.asInt());
                        }
                        else if(obj.isObject()) {
                            value = obj.get(index.asText());
                        }
                        else {
                            error(obj + " is not an indexable object");
                        }
                        
                        stack[top++] = value;
                        break;
                    }
                    case SETK: {
                        int iname = ARGx(i);
                        JsonNode index = constants[iname];
                        JsonNode obj = stack[--top];
                        JsonNode value = stack[--top];
              
                        if(obj.isArray()) {
                            ArrayNode array = (ArrayNode)obj;
                            array.set(index.asInt(), value);
                        }
                        else if(obj.isObject()) {
                            ObjectNode object = (ObjectNode)obj;
                            object.set(index.asText(), value);
                        }
                        else {
                            error(obj + " is not an indexable object");
                        }
                        
                        stack[top++] = obj; 
                        break;
                    }
                    case ARRAY_SLICE: {
                        JsonNode end = stack[--top];
                        JsonNode start = stack[--top];
                        JsonNode array = stack[--top];
                        
                        if(!array.isArray()) {
                            error(array + " is not an array");
                        }
                        
                        ArrayNode a = (ArrayNode)array;
                        
                        int startIndex = start.asInt();
                        int endIndex = end.asInt();
                        if(endIndex < 0) {
                            endIndex = a.size(); 
                        }
                        
                        if(endIndex < startIndex) {
                            error("The end range (" + endIndex + ") is smaller than the start range (" + startIndex + ")");
                        }
                        
                        ArrayNode slice = new ArrayNode(this.runtime.getObjectMapper().getNodeFactory(), endIndex - startIndex);
                        for(int j = startIndex; j < endIndex; j++) {
                            slice.add(a.get(j));
                        }
                        stack[top++] = slice;                        
                        break;
                    }
                    /* arithmetic operators */
                    case ADD:    {
                        JsonNode r = stack[--top];
                        JsonNode l = stack[--top];
                        JsonNode c = null;
                        if(l.isTextual()) {
                            c = TextNode.valueOf(l.asText() + r.asText()); 
                        }
                        else {
                            c = DoubleNode.valueOf(l.asDouble() + r.asDouble());
                        }
                        stack[top++] = c;
                        break;
                    }
                    case SUB:    {
                        JsonNode r = stack[--top];
                        JsonNode l = stack[--top];
                        JsonNode c = DoubleNode.valueOf(l.asDouble() - r.asDouble());
                        stack[top++] = c;
                        break;
                    }
                    case MUL:    {
                        JsonNode r = stack[--top];
                        JsonNode l = stack[--top];
                        JsonNode c = DoubleNode.valueOf(l.asDouble() * r.asDouble());
                        stack[top++] = c;
                        break;
                    }
                    case DIV:    {
                        JsonNode r = stack[--top];
                        JsonNode l = stack[--top];
                        JsonNode c = DoubleNode.valueOf(l.asDouble() / r.asDouble());
                        stack[top++] = c;
                        break;
                    }
                    case MOD:    {
                        JsonNode r = stack[--top];
                        JsonNode l = stack[--top];
                        JsonNode c = DoubleNode.valueOf(l.asDouble() % r.asDouble());
                        stack[top++] = c;
                        break;
                    }
                    case NEG:    {
                        JsonNode l = stack[--top];
                        JsonNode c = DoubleNode.valueOf(-l.asDouble());
                        stack[top++] = c;
                        break;
                    }
                    
                    case OR:    {
                        JsonNode r = stack[--top];
                        JsonNode l = stack[--top];
                        JsonNode c = BooleanNode.valueOf(l.asBoolean() || r.asBoolean());
                        stack[top++] = c;
                        break;
                    }
                    case AND:    {
                        JsonNode r = stack[--top];
                        JsonNode l = stack[--top];
                        JsonNode c = BooleanNode.valueOf(l.asBoolean() && r.asBoolean());
                        stack[top++] = c;
                        break;
                    }
                    case NOT:    {
                        JsonNode l = stack[--top];
                        JsonNode c = BooleanNode.valueOf(!l.asBoolean());
                        stack[top++] = c;
                        break;
                    }

                    case EQ:    {
                        JsonNode r = stack[--top];
                        JsonNode l = stack[--top];
                        JsonNode c = BooleanNode.valueOf(l.equals(r));
                        stack[top++] = c;
                        break;
                    }
                    case NEQ:    {
                        JsonNode r = stack[--top];
                        JsonNode l = stack[--top];
                        JsonNode c = BooleanNode.valueOf(!l.equals(r));
                        stack[top++] = c;
                        break;
                    }
                    case GT:    {
                        JsonNode r = stack[--top];
                        JsonNode l = stack[--top];
                        JsonNode c = BooleanNode.valueOf(l.asDouble() > r.asDouble());
                        stack[top++] = c;
                        break;
                    }
                    case GTE:    {
                        JsonNode r = stack[--top];
                        JsonNode l = stack[--top];
                        JsonNode c = BooleanNode.valueOf(l.asDouble() >= r.asDouble());
                        stack[top++] = c;
                        break;
                    }
                    case LT:    {
                        JsonNode r = stack[--top];
                        JsonNode l = stack[--top];
                        JsonNode c = BooleanNode.valueOf(l.asDouble() < r.asDouble());
                        stack[top++] = c;
                        break;
                    }
                    case LTE:    {
                        JsonNode r = stack[--top];
                        JsonNode l = stack[--top];
                        JsonNode c = BooleanNode.valueOf(l.asDouble() <= r.asDouble());
                        stack[top++] = c;
                        break;
                    }
                    default: {
                        error("Unknown opcode '" + opcode + "' found for the Bytecode '" + Integer.toHexString(i) + "'");
                    }
                }
            }
        }
        catch(Exception e) {
            error("RuntimeError: " + e);
        }
        
        result = stack[--top];        
        return result;
    }
    
    /**
     * Prepares the stack by assigning NULL to all of the bytecode's
     * arguments.
     * 
     * @param code
     */
    private void prepareStack(Bytecode code) {
        final int base = top;
        
        growStackIfRequired(stack, base, code.maxstacksize);
        
        for(int i = 0; i < code.numArgs; i++) {
            stack[base + i] = NullNode.instance;
        }
    }

    /**
     * Checks to see if we should grow the stack
     * 
     * @param stack
     * @param base
     * @param neededSize
     * @return the new stack (if no growth was required, the supplied stack is returned).
     */
    private void growStackIfRequired(JsonNode[] stack, int base, int neededSize) {
        final int requiredStackSize = base + neededSize;
        if ( requiredStackSize > this.maxStackSize) {
            error("Stack overflow, required stack size over maxStackSize '" + this.maxStackSize + "'");
        }
        
        if( requiredStackSize > stack.length) {
            final int newStackSize = Math.min( stack.length + ((requiredStackSize-stack.length) << 1), this.maxStackSize);
            JsonNode[] newStack = new JsonNode[newStackSize];
            System.arraycopy(stack, 0, newStack, 0, base);
            this.stack = newStack;
            
            Outer[] newOuters = new Outer[newStack.length];
            System.arraycopy(openouters, 0, newOuters, 0, base);
            this.openouters = newOuters;
        }        
    }
    
    /**
     * Reads an array of values from the stack.
     * 
     * @param args
     * @param nargs
     * @param stack
     * @return the array of {@link JsonNode}'s, or null if nargs <= 0
     */
    private JsonNode[] readArrayFromStack(JsonNode[] args, int nargs, JsonNode[] stack) {                           
        for(int j = nargs - 1; j >= 0; j--) {
            args[j] = stack[--top];
        }                
        return args;
    }
    
    
    /**
     * Reads an array of values from the stack.
     * 
     * @param nargs
     * @param stack
     * @return the array of {@link JsonNode}'s, or null if nargs <= 0
     */
    private JsonNode[] readArrayFromStack(int nargs, JsonNode[] stack) {
        JsonNode[] args = null;
        if ( nargs > 0 ) {
            args = new JsonNode[nargs];
            return readArrayFromStack(args, nargs, stack);
        }
        
        return args;
    }
    
    /**
     * Handles an error in the execution.
     *
     * @param errorMsg
     */
    private void error(String errorMsg) {
        if(errorMsg==null) {
            errorMsg = "";
        }
        
        throw new Jslt2Exception("ExecutionError: " + errorMsg);
    }

    /**
     * Close over the outer variables for closures.
     * 
     * @param outers
     * @param calleeouters
     * @param openouters
     * @param numOuters
     * @param base
     * @param pc
     * @param code
     * @return true if there where Outers created that should be closed over once we leave the function
     * scope
     */
    private boolean assignOuters(Outer[] outers, Outer[] calleeouters, Outer[] openouters, 
                        int numOuters, 
                        int base, 
                        int pc, 
                        Bytecode code) {
        
        boolean closeOuters = false;
        for(int j = 0; j < numOuters; j++) {
            int i = code.instr[pc++];

            int opCode = i & 255;
            int index = ARGx(i);

            switch(opCode) {
                case xLOAD_OUTER: {
                    outers[j] = calleeouters[index];
                    break;
                }
                case xLOAD_LOCAL: {
                    int bindex = base + index;
                    outers[j] = openouters[bindex] != null ?
                                openouters[bindex] :
                                (openouters[bindex] = new Outer(vmStackValue, bindex));
                    closeOuters = true;
                    break;
                }
                default: {
                    error("Outer opcode '" + opCode +"' is invalid");
                }
            }
        }

        return closeOuters;
    }
    
    
}
