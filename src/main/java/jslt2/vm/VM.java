/*
 * see license.txt
 */
package jslt2.vm;

import static jslt2.vm.Opcodes.*;
import static jslt2.vm.Bytecode.GLOBAL_FLAG;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import jslt2.Jslt2;
import jslt2.Jslt2Exception;
import jslt2.Jslt2Function;
import jslt2.Jslt2MacroFunction;
import jslt2.util.Jslt2Util;
import jslt2.util.Stack;


/**
 * @author Tony
 *
 */
public class VM {

    /**
     * Default stack size
     */
    public static final int DEFAULT_STACKSIZE = 128;
    
    private Jslt2 runtime;
    
    /* Value stacks 
     */
    private Stack<ObjectNode> objectStack;
    private Stack<ArrayNode>  arrayStack;
    

    /*thread stack
     */
    private JsonNode[] stack;

    private int top;

    /**
     * The maximum stack size
     */
    private final int maxStackSize;
    private Async async;
       
    public VM(Jslt2 runtime) {
        this(runtime, runtime.getMinStackSize() <= 0 
                ? DEFAULT_STACKSIZE : runtime.getMinStackSize());
    }
    
    /**
     * @param runtime
     * @param stackSize - starting stack size
     */
    public VM(Jslt2 runtime, int stackSize) {
        this.runtime = runtime;

        this.maxStackSize = Math.max(runtime.getMaxStackSize(), stackSize);
        
        this.objectStack = new Stack<>();
        this.arrayStack  = new Stack<>();
        
        this.stack = new JsonNode[stackSize];
        this.top = 0;   
        
        this.async = new Async(runtime);
    }

    /**
     * Executes the supplied {@link Bytecode} with the {@link JsonNode} input
     * 
     * @param code the bytecode to execute
     * @param input the json input
     * @return the json output
     * @throws Jslt2Exception
     */
    public JsonNode execute(Bytecode code, JsonNode input) throws Jslt2Exception {
        final int base = top;
        prepareStack(code);        
        
        return executeStackFrame(code, base, input);        
    }
        
    private JsonNode executeStackFrame(Bytecode code, int base, JsonNode input) throws Jslt2Exception {
        executeBytecode(code, base, input);
        
        JsonNode result = stack[--top];
        
        exitCall(code, base);        
        return result;
    }
    
    private void executeBytecode(Bytecode code, int base, JsonNode input) throws Jslt2Exception {
        final int[] instr = code.instr;
        final int len = code.len;
        int pc = code.pc;

        final JsonNode[] constants = code.constants;
        final Bytecode[] inner = code.inner;
        
        final JsonNode[] calleeouters = code.outers;
        
        final int topStack = base + code.numLocals;
        top = topStack;
        
        int lineNumber = -1;
        
        try {
            while(pc < len) {
                int i = instr[pc++];
                int opcode =  i & 255; //OPCODE(i);

                switch(opcode) {
                    case LINE: {
                        lineNumber = ARGx(i);
                        break;
                    }
                    case NEW_OBJ: {
                        this.objectStack.push(this.runtime.newObjectNode());
                        break;     
                    }
                    case SEAL_OBJ: {
                        stack[top++] = this.objectStack.pop();
                        break;
                    }
                    
                    case NEW_ARRAY: {
                        this.arrayStack.push(this.runtime.newArrayNode(16));
                        break;       
                    }
                    case SEAL_ARRAY: {
                        stack[top++] = this.arrayStack.pop();
                        break;
                    }
                    
                    case ADD_FIELDK: {                       
                        ObjectNode obj = this.objectStack.peek();
                        
                        int iname = ARGx(i);
                        JsonNode fieldName = constants[iname];
                        
                        obj.set(fieldName.asText(), stack[--top]);                        
                        break;
                    }
                    case ADD_FIELD: {
                        ObjectNode obj = this.objectStack.peek();
                        
                        JsonNode value = stack[--top];
                        JsonNode index = stack[--top];
                        
                        obj.set(index.asText(), value);
                        break;
                    }
                    case ADD_ELEMENT: {
                        ArrayNode array = this.arrayStack.peek();                        
                        array.add(stack[--top]);                        
                        break;
                    }
                    
                    case GET_FIELDK: {
                        int iname = ARGx(i);                        
                        JsonNode index = constants[iname];
                        JsonNode obj = stack[--top];

                        JsonNode value = obj.get(index.asText());
                        if(value == null) {
                            value = NullNode.instance;
                        }
                                                
                        stack[top++] = value;
                        break;
                    }     
                    case GET_FIELD: {                                     
                        JsonNode index = stack[--top];
                        JsonNode obj = stack[--top];

                        JsonNode value = obj.get(index.asText());
                        if(value == null) {
                            value = NullNode.instance;
                        }
                        
                        stack[top++] = value;
                        break;
                    } 
                    case GET_ARRAY_ELEMENT: {                                     
                        JsonNode index = stack[--top];
                        JsonNode obj = stack[--top];

                        JsonNode value = obj.get(index.asInt());
                        if(value == null) {
                            value = NullNode.instance;
                        }
                        
                        stack[top++] = value;
                        break;
                    }
                    case GET_INPUT_FIELDK: {
                        int iname = ARGx(i);                        
                        JsonNode index = constants[iname];
                        JsonNode value = input.get(index.asText());
                        if(value == null) {
                            value = NullNode.instance;
                        }
                                                
                        stack[top++] = value;
                        break;
                    }     
                    case ARRAY_SLICE: {
                        JsonNode end = stack[--top];
                        JsonNode start = stack[--top];
                        JsonNode array = stack[--top];
                        
                        int startIndex = start.asInt();
                        int endIndex = end.asInt();
                        
                        if(array.isArray()) {
                            ArrayNode a = (ArrayNode)array;
                            
                            if(endIndex < 0) {
                                endIndex = a.size(); 
                            }
                            
                            if(endIndex < startIndex) {
                                error("The end range (" + endIndex + ") is smaller than the start range (" + startIndex + ")");
                            }
                            
                            ArrayNode slice = this.runtime.newArrayNode(endIndex - startIndex);
                            for(int j = startIndex; j < endIndex; j++) {
                                slice.add(a.get(j));
                            }
                            
                            stack[top++] = slice;                        
                        }
                        else if(array.isTextual()) {
                            TextNode a = (TextNode)array;
                            String text = a.asText();
                            
                            if(endIndex < 0) {
                                endIndex = text.length(); 
                            }
                            
                            if(endIndex < startIndex) {
                                error("The end range (" + endIndex + ") is smaller than the start range (" + startIndex + ")");
                            }
                                                                                    
                            stack[top++] = new TextNode(text.substring(startIndex, endIndex));
                        }
                        else {
                            stack[top++] = NullNode.instance;
                        }
                        break;
                    }
                    case ASYNC: {                        
                        int bytecodeIndex = ARGx(i);
                        Bytecode asyncCode = inner[bytecodeIndex].clone();
                        
                        JsonNode[] outers = asyncCode.outers;                            
                        pc += assignOuters(outers, calleeouters, asyncCode.numOuters, base, pc, code);
                        
                        JsonNode key = stack[--top];
                        async.submit(this.objectStack.peek(), key.asText(), input, asyncCode);                        
                        break;
                    }
                    case MATCHER: {
                        ObjectNode outputObj = this.objectStack.peek();
                        
                        int n = ARG1(i);
                        JsonNode[] omittedFields = readArrayFromStack(n, stack);
                        
                        JsonNode contextPath = stack[--top];
                        JsonNode context = resolveContext(contextPath, input);
                        
                        JsonNode inputNode = context;                        
                        if(!inputNode.isObject()) {
                            continue;
                        }
                        
                        int bytecodeIndex = ARG2(i);
                        Bytecode valueCode = inner[bytecodeIndex].clone();
                        
                        JsonNode[] outers = valueCode.outers;                            
                        pc += assignOuters(outers, calleeouters, valueCode.numOuters, base, pc, code);
                        
                        prepareStack(valueCode);
                        
                        ObjectNode inputObj = (ObjectNode)inputNode;
                        Iterator<Map.Entry<String, JsonNode>> it = inputObj.fields();
                        
                        while(it.hasNext()) {
                            Map.Entry<String, JsonNode> next = it.next();
                            String key = next.getKey();
                            if(!outputObj.has(key)) {
                                if(isOmittedField(omittedFields, key)) {
                                    continue;
                                }
                                
                                executeBytecode(valueCode, top, next.getValue());
                                JsonNode value = stack[--top];
                                
                                outputObj.set(key, value);
                            }
                        }
                        
                        exitCall(valueCode, top);
                        
                        break;
                    }
                    
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
                        stack[top++] = calleeouters[iname];
                        break;
                    }
                    case LOAD_JNULL: {
                        stack[top++] = null;
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
                        stack[top++] = input;
                        break;
                    }
                    case STORE_LOCAL: {
                        int iname = ARGx(i);
                        stack[base + iname] = stack[--top];
                        break;
                    }

                    case JMP:    {
                        int pos = ARGsx(i);
                        pc += pos;
                        break;
                    }
                    case IFEQ:    {
                        JsonNode cond = stack[--top];
                        if (!Jslt2Util.isTrue(cond)) {
                            int pos = ARGsx(i);
                            pc += pos;
                        }
                        break;
                    }
                    
                    case FOR_ARRAY_DEF: {                        
                        int bytecodeIndex = ARGx(i);
                        Bytecode forCode = inner[bytecodeIndex].clone();
                        
                        JsonNode[] outers = forCode.outers;                            
                        pc += assignOuters(outers, calleeouters, forCode.numOuters, base, pc, code);
                        
                        prepareStack(forCode);
                        
                        JsonNode object = stack[--top];
                        ArrayNode array = this.runtime.newArrayNode(object.size());
                        
                        if(object.isNull()) {
                            JsonNode current = NullNode.instance;
                            
                            executeBytecode(forCode, top, current); 
                            JsonNode n = stack[--top];
                            if(n != null) {
                                array.add(n);
                            }
                        }
                        else if(object.isObject()) {
                            Iterator<String> it = ((ObjectNode)object).fieldNames();
                            while(it.hasNext()) {
                                String key = it.next();
                                
                                ObjectNode current = runtime.newObjectNode();
                                current.set("key", new TextNode(key));
                                current.set("value", object.get(key));
                                
                                executeBytecode(forCode, top, current); 
                                JsonNode n = stack[--top];
                                
                                if(n != null) {
                                    array.add(n);
                                }
                            }
                        }
                        else if(object.isArray()) {
                            ArrayNode a = (ArrayNode)object;
                            int size = a.size();
                            for(int ix = 0; ix < size; ix++) {
                                JsonNode current = a.get(ix);
                                
                                executeBytecode(forCode, top, current); 
                                JsonNode n = stack[--top];
                                
                                if(n != null) {
                                    array.add(n);  
                                }
                            }
                        }
                        else {
                            throw new Jslt2Exception("ForIterationError: " + object + " is not an iterable element");
                        }
                        
                        stack[top++] = array;
                        
                        exitCall(forCode, top); // ensure stack is cleared for forCode calls
                        
                        break;
                    }
                    case FOR_OBJ_DEF: {                        
                        int bytecodeIndex = ARGx(i);
                        Bytecode forCode = inner[bytecodeIndex].clone();
                        
                        JsonNode[] outers = forCode.outers;                            
                        pc += assignOuters(outers, calleeouters, forCode.numOuters, base, pc, code);
                                                
                        ObjectNode obj = this.runtime.newObjectNode();
                        
                        prepareStack(forCode);
                        
                        JsonNode object = stack[--top];
                        if(object.isNull()) {
                            JsonNode current = NullNode.instance;
                            
                            executeBytecode(forCode, top, current); 
                            JsonNode v = stack[--top];
                            JsonNode k = stack[--top];
                            
                            if(k != null) {
                                obj.set(k.asText(), v);
                            }
                        }
                        else if(object.isObject()) {
                            Iterator<String> it = ((ObjectNode)object).fieldNames();            
                            while(it.hasNext()) {
                                String key = it.next();
                                
                                ObjectNode current = runtime.newObjectNode();
                                current.set("key", new TextNode(key));
                                current.set("value", object.get(key));
                                
                                executeBytecode(forCode, top, current); 
                                JsonNode v = stack[--top];
                                JsonNode k = stack[--top];

                                if(k != null) {
                                    obj.set(k.asText(), v);
                                }
                            }                                                   
                        }
                        else if(object.isArray()) {            
                            ArrayNode array = (ArrayNode)object;
                            int size = array.size();
                            for(int ix = 0; ix < size; ix++) {
                                JsonNode current = array.get(ix);
                                executeBytecode(forCode, top, current); 
                                JsonNode v = stack[--top];
                                JsonNode k = stack[--top];
                              
                                if(k != null) {
                                    obj.set(k.asText(), v);  
                                }
                            }
                        }
                        else {
                            throw new Jslt2Exception("ForIterationError: " + object + " is not an iterable element");
                        }
                        
                        stack[top++] = obj;
                        
                        exitCall(forCode, top); // ensure stack is cleared for forCode calls
                        
                        break;
                    }
                    case FUNC_DEF: {
                        int innerIndex = ARGx(i);
                        Bytecode funcCode = inner[innerIndex];
                        
                        JsonNode[] outers = funcCode.outers;                            
                        pc += assignOuters(outers, calleeouters, funcCode.numOuters, base, pc, code);
                        
                        break;
                    }
                    
                    case INVOKE:    {
                        int nargs = ARG1(i);
                        int bytecodeIndex = ARG2(i);
                                                
                        Bytecode funcCode = (bytecodeIndex & GLOBAL_FLAG) > 0 ?
                                code.global.inner[bytecodeIndex & ~GLOBAL_FLAG] :
                                inner[bytecodeIndex];             
                                
                        prepareStack(funcCode);

                        JsonNode result = executeStackFrame(funcCode, top - nargs, input);
                        
                        stack[top++] = result;    
                        break;
                    }
                    case USER_INVOKE: {
                        int nargs = ARG1(i);
                        int constIndex = ARG2(i);
                        
                        JsonNode name = constants[constIndex];
                        
                        JsonNode[] args = readArrayFromStack(nargs, stack);
                        Jslt2Function function = this.runtime.getFunction(name.asText());
                        if(function == null) {
                            error("No function defined with the name '" + name.asText() + "'");
                        }
                        
                        JsonNode c = function.execute(input, args);
                        
                        stack[top++] = c;
                        break;
                    }
                    case MACRO_INVOKE: {
                        int nargs = ARG1(i);
                        int constIndex = ARG2(i);
                        
                        int endIndex = stack[--top].asInt();
                        JsonNode name = constants[constIndex];
                        
                        int startIndex = endIndex - nargs;
                        
                        
                        List<Bytecode> args = new ArrayList<>(nargs);
                        for(int ix = startIndex; ix < endIndex; ix++) {
                            args.add(inner[ix]);
                        }
                        
                        //JsonNode[] args = readArrayFromStack(nargs, stack);
                        Jslt2MacroFunction macro = this.runtime.getMacro(name.asText());
                        if(macro == null) {
                            error("No macro defined with the name '" + name.asText() + "'");
                        }
                        
                        JsonNode c = macro.execute(this, input, args);
                        
                        stack[top++] = c;
                        break;
                    }
                    
                    /* arithmetic operators */
                    case ADD:    {
                        JsonNode r = stack[--top];
                        JsonNode l = stack[--top];
                        JsonNode c = null;
                        if(l.isTextual() || r.isTextual()) {
                            c = new TextNode(Jslt2Util.toString(l, false) + 
                                             Jslt2Util.toString(r, false)); 
                        }
                        else if(l.isArray() && r.isArray()) {
                            ArrayNode a = (ArrayNode)l;
                            ArrayNode b = (ArrayNode)r;
                            ArrayNode union = this.runtime.newArrayNode(a.size() + b.size());
                            union.addAll(a);
                            union.addAll(b);
                            
                            c = union;
                        }
                        else if(l.isObject() && r.isObject()) {
                            ObjectNode a = (ObjectNode)l;
                            ObjectNode b = (ObjectNode)r;
                            ObjectNode union = this.runtime.newObjectNode();
                            union.setAll(a);
                            union.setAll(b);
                            
                            c = union;
                        }
                        else {
                            if(l.isIntegralNumber() && r.isIntegralNumber()) {
                                c = new LongNode(l.asLong() + r.asLong());
                            }
                            else {
                                c = new DoubleNode(l.asDouble() + r.asDouble());
                            }
                        }
                        stack[top++] = c;
                        break;
                    }
                    case SUB:    {
                        JsonNode r = stack[--top];
                        JsonNode l = stack[--top];
                        
                        JsonNode c = null;
                        if(l.isIntegralNumber() && r.isIntegralNumber()) {
                            c = new LongNode(l.asLong() - r.asLong());
                        }
                        else {
                            c = new DoubleNode(l.asDouble() - r.asDouble());
                        }
                        stack[top++] = c;
                        break;
                    }
                    case MUL:    {
                        JsonNode r = stack[--top];
                        JsonNode l = stack[--top];
                        JsonNode c = null;
                        if(l.isIntegralNumber() && r.isIntegralNumber()) {
                            c = new LongNode(l.asLong() * r.asLong());
                        }
                        else {
                            c = new DoubleNode(l.asDouble() * r.asDouble());
                        }
                        stack[top++] = c;
                        break;
                    }
                    case DIV:    {
                        JsonNode r = stack[--top];
                        JsonNode l = stack[--top];
                        JsonNode c = null;
                        if(l.isIntegralNumber() && r.isIntegralNumber()) {
                            long ld = l.longValue();
                            long rd = r.longValue();
                            if((ld % rd) == 0) {                               
                                c = new LongNode(l.asLong() / r.asLong());
                            }
                            else {
                                c = new DoubleNode((double)ld / (double)rd);
                            }
                        }
                        else {
                            c = new DoubleNode(l.asDouble() / r.asDouble());
                        }
                        stack[top++] = c;
                        break;
                    }
                    case MOD:    {
                        JsonNode r = stack[--top];
                        JsonNode l = stack[--top];
                        JsonNode c = null;
                        if(l.isIntegralNumber() && r.isIntegralNumber()) {
                            c = new LongNode(l.asLong() % r.asLong());
                        }
                        else {
                            c = new DoubleNode(l.asDouble() % r.asDouble());
                        }
                        stack[top++] = c;
                        break;
                    }
                    case NEG:    {
                        JsonNode l = stack[--top];
                        JsonNode c = null;
                        if(l.isIntegralNumber()) {
                            c = new LongNode(-l.asLong());
                        }
                        else {
                            c = new DoubleNode(-l.asDouble());
                        }
                        stack[top++] = c;
                        break;
                    }
                    
                    case NOT:    {
                        JsonNode l = stack[--top];
                        JsonNode c = BooleanNode.valueOf(!Jslt2Util.isTrue(l));
                        stack[top++] = c;
                        break;
                    }
                    case IS_TRUE:    {
                        JsonNode l = stack[--top];
                        JsonNode c = BooleanNode.valueOf(Jslt2Util.isTrue(l));
                        stack[top++] = c;
                        break;
                    }
                    case EQ:    {
                        JsonNode r = stack[--top];
                        JsonNode l = stack[--top];
                        JsonNode c = BooleanNode.valueOf(Jslt2Util.equals(l, r));
                        stack[top++] = c;
                        break;
                    }
                    case NEQ:    {
                        JsonNode r = stack[--top];
                        JsonNode l = stack[--top];
                        JsonNode c = BooleanNode.valueOf(!Jslt2Util.equals(l, r));
                        stack[top++] = c;
                        break;
                    }
                    case GT:    {
                        JsonNode r = stack[--top];
                        JsonNode l = stack[--top];
                        int n = Jslt2Util.compare(l, r);
                        JsonNode c = BooleanNode.valueOf(n > 0);
                        stack[top++] = c;
                        break;
                    }
                    case GTE:    {
                        JsonNode r = stack[--top];
                        JsonNode l = stack[--top];
                        int n = Jslt2Util.compare(l, r);
                        JsonNode c = BooleanNode.valueOf(n >= 0);
                        stack[top++] = c;
                        break;
                    }
                    case LT:    {
                        JsonNode r = stack[--top];
                        JsonNode l = stack[--top];
                        int n = Jslt2Util.compare(l, r);
                        JsonNode c = BooleanNode.valueOf(n < 0);
                        stack[top++] = c;
                        break;
                    }
                    case LTE:    {
                        JsonNode r = stack[--top];
                        JsonNode l = stack[--top];
                        int n = Jslt2Util.compare(l, r);
                        JsonNode c = BooleanNode.valueOf(n <= 0);
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
            buildStackTrace(code, lineNumber, e);
        }        
    }
    
    private void buildStackTrace(Bytecode code, int lineNumber, Exception e) {
        error(String.format("RuntimeError: '%s' at line %d stack trace: %s", code.getSourceFileName(), lineNumber, e));
    }
    
    private void exitCall(Bytecode code, int base) {
        final int stackSize = Math.min(stack.length, base+code.maxstacksize);

        for(int j=base;j<stackSize;j++) {
            stack[j] = null;
        }                

        top = base;   
        
        if(code.hasAsync()) {
            this.async.await();
        }
    }
    
    /**
     * Checks to see if we should grow the stack
     * 
     * @param code
     */
    private void prepareStack(Bytecode code) {        
        final int requiredStackSize = top + code.maxstacksize;
        if(requiredStackSize > stack.length) {
            if (requiredStackSize > this.maxStackSize) {
                error("Stack overflow, required stack size over maxStackSize '" + this.maxStackSize + "'");
            }
            
            final int newStackSize = Math.min( stack.length + ((requiredStackSize-stack.length) << 1), this.maxStackSize);
            JsonNode[] newStack = new JsonNode[newStackSize];
            System.arraycopy(stack, 0, newStack, 0, top);
            this.stack = newStack;            
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
        if (nargs > 0) {
            args = new JsonNode[nargs];
            return readArrayFromStack(args, nargs, stack);
        }
        
        return args;
    }
    
    private boolean isOmittedField(JsonNode[] omittedFields, String key) {
        if(omittedFields != null) {
            for(int j = 0; j < omittedFields.length; j++) {
                String field = omittedFields[j].asText(); 
                
                // check identifier
                if(field.equals(key)) {
                    return true;
                }
                
                // check string
                if(field.startsWith("\"") && field.endsWith("\"") &&
                  (field.length() > 2 && field.substring(1, field.length()-1).equals(key))) {
                    return true;
                }
            }
        }
        
        return false;
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

    private int assignOuters(JsonNode[] outers, JsonNode[] calleeouters, 
                        int numOuters, 
                        int base, 
                        int pc, 
                        Bytecode code) {
        
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
                    outers[j] = stack[base + index];
                    break;
                }                
                default: {
                    error("Outer opcode '" + opCode +"' is invalid");
                }
            }
        }
        
        return numOuters;
    }
    
    private JsonNode resolveContext(JsonNode contextPath, JsonNode input) {
        if(!input.isObject()) {
            return input;
        }
        
        String[] split = contextPath.asText().split(":");
        for(int i = split.length - 1; i >= 0; i--) {
            String fieldName = split[i];
            if(fieldName.isEmpty()) {
                continue;
            }
            
            input = input.get(fieldName);
            
            if(input == null) {
                return NullNode.instance;
            }            
        }
        
        return input;
    }
}
