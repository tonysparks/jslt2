/*
 * see license.txt
 */
package jslt2.vm;

import java.util.HashMap;
import java.util.Map;

import jslt2.Jslt2Exception;

/**
 * @author Tony
 *
 */
public class Opcodes {

    // instruction for ARG1 ARG2
    // 0x222111FF
    
    // instruction for ARGx
    // 0xXXXXXXFF
    
    // instruction for ARGsx (signed)
    // 0x7XXXXXFF
    
    // instruction
    // 0x002211FF
    // FF = opcode
    // 11 = arg1
    // 22 = arg2
    // X = part of ARG(s)x
    // 7 = 0111 last bit for signed value
    
    public static final int OP_SIZE = 8;
    public static final int ARG1_SIZE = 12;     // number of bits
    public static final int ARG2_SIZE = 12;     // number of bits
    public static final int ARGx_SIZE = ARG1_SIZE + ARG2_SIZE;
    public static final int ARGsx_SIZE = ARG1_SIZE + ARG2_SIZE-1;
    public static final int OP_POS = 0;
    public static final int ARG1_POS = OP_POS + OP_SIZE;
    public static final int ARG2_POS = ARG1_POS + ARG1_SIZE;
    public static final int ARGx_POS = ARG1_POS;
    
    public static final int MAX_OP   = ( (1<<OP_SIZE) - 1);
    public static final int MAX_ARG1 = ( (1<<ARG1_SIZE) - 1);
    public static final int MAX_ARG2 = ( (1<<ARG2_SIZE) - 1);
    public static final int MAX_ARGx = ( (1<<ARGx_SIZE) - 1);
    public static final int MAX_ARGsx = (MAX_ARGx>>1);
    
    public static final int OP_MASK   = ((1<<OP_SIZE)-1)<<OP_POS; 
    public static final int ARG1_MASK = ((1<<ARG1_SIZE)-1)<<ARG1_POS;
    public static final int ARG2_MASK = ((1<<ARG2_SIZE)-1)<<ARG2_POS;
    public static final int ARGx_MASK = ((1<<ARGx_SIZE)-1)<<ARGx_POS;
    
    public static final int NOT_OP_MASK   = ~OP_MASK;
    public static final int NOT_ARG1_MASK = ~ARG1_MASK;
    public static final int NOT_ARG2_MASK = ~ARG2_MASK;
    public static final int NOT_ARGx_MASK = ~ARGx_MASK;
    
    
    /**
     * Strips the opinstr out of the supplied integer
     * @param instr
     * @return
     */
    public static final int OPCODE(int instr) {
//      return (instr >> OP_POS) & MAX_OP;
        return instr & MAX_OP;
    }
    
    /**
     * Returns the first argument
     * @param instr
     * @return arg1 one value
     */
    public static final int ARG1(int instr) {
        return (instr >>>  ARG1_POS) & MAX_ARG1;
    }

    /**
     * Returns the second argument
     * @param instr
     * @return arg2 value
     */
    public static final int ARG2(int instr) {
        return (instr >>> ARG2_POS) & MAX_ARG2;     
    }
    
    /**
     * Returns the x argument
     * @param instr 
     * @return the x argument
     */
    public static final int ARGx(int instr) {
        return ((instr >>> ARGx_POS) & MAX_ARGx);
    }
    
    /**
     * Returns the signed x argument
     * @param instr
     * @return the signed x argument
     */
    public static final int ARGsx(int instr) {
        return ((instr >> ARGx_POS) & MAX_ARGx) - MAX_ARGsx;
    }
    
    /**
     * Sets the signed x value on the instruction
     * @param instr
     * @param x
     * @return the instruction with the set value
     */
    public static final int SET_ARGsx(int instr, int sx) {
        return (instr & (NOT_ARGx_MASK)) | (( (sx + MAX_ARGsx) << ARGx_POS) & ARGx_MASK);
    }
    
    /**
     * Sets the x value on the instruction
     * @param instr
     * @param x
     * @return the instruction with the set value
     */
    public static final int SET_ARGx(int instr, int argx) {
        return (instr & (NOT_ARGx_MASK)) | (( argx << ARGx_POS) & ARGx_MASK);
    }
    
    /**
     * Sets the arg1 value on the instruction
     * @param instr
     * @param x
     * @return the instruction with the set value
     */
    public static final int SET_ARG1(int instr, int arg1) {
        return (instr & NOT_ARG1_MASK) | (( arg1 << ARG1_POS) & ARG1_MASK);
    }
    
    /**
     * Sets the arg2 value on the instruction
     * @param instr
     * @param x
     * @return the instruction with the set value
     */
    public static final int SET_ARG2(int instr, int arg2) {
        return (instr & NOT_ARG2_MASK) | (( arg2 << ARG2_POS) & ARG2_MASK);
    }
    
        
    /**
     * String to integer opcode
     * @param opcode
     * @return the opcode represented by the string, or -1 if not found.
     */
    public static final int str2op(String opcode) {
        Integer op = opcodes.get(opcode.toUpperCase());
        return (op != null) ? op : -1;
    }
    
    /**
     * Converts the byte opcode to a readable string
     * 
     * @param opcode
     * @return a string
     */
    public static final String op2str(int opcode) {
        String op = "";
        switch(opcode) {
            case LOAD_CONST: {
                op = "LOAD_CONST";
                break;
            }
            case LOAD_LOCAL: {
                op = "LOAD_LOCAL";
                break;
            }
            case LOAD_OUTER: {
                op = "LOAD_OUTER";                
                break;
            }
            case LOAD_NULL: {
                op = "LOAD_NULL";
                break;
            }
            case LOAD_TRUE: {
                op = "LOAD_TRUE";
                break;
            }
            case LOAD_FALSE: {
                op = "LOAD_FALSE";
                break;
            }
            case LOAD_INPUT: {
                op = "LOAD_INPUT";
                break;
            }
            
            case STORE_LOCAL: {
                op = "STORE_LOCAL";
                break;
            }         
                        
            case xLOAD_OUTER: {
                op = "xLOAD_OUTER";
                break;
            }
            case xLOAD_LOCAL: {
                op = "xLOAD_LOCAL";
                break;
            }
            
            case JMP:    {
                op = "JMP";                
                break;
            }
            case IFEQ:    {                   
                op = "IFEQ";
                break;
            }            

            case NEW_ARRAY: {
                op = "NEW_ARRAY";
                break;
            }
            case SEAL_ARRAY: {
                op = "SEAL_ARRAY";
                break;
            }
            case NEW_OBJ: {
                op = "NEW_OBJ";
                break;
            }     
            case SEAL_OBJ: {
                op = "SEAL_OBJ";
                break;
            }
            
            /* object access */
            case GET_FIELDK: {
                op = "GET_FIELDK";
                break;
            }
            case GET_FIELD: {
                op = "GET_FIELD";
                break;
            }
            
            
            case ADD_FIELD: {
                op = "ADD_FIELD";
                break;
            }
            case ADD_FIELDK: {
                op = "ADD_FIELDK";
                break;
            }
            case ADD_ELEMENT: {
                op = "ADD_ELEMENT";
                break;
            }

            case ARRAY_SLICE:    {
                op = "ARRAY_SLICE";
                break;
            }
            case FOR_ARRAY_DEF: {
                op = "FOR_DEF";
                break;
            }
            case FOR_OBJ_DEF: {
                op = "FOR_OBJ_DEF";
                break;
            }
            case FUNC_DEF: {
                op = "FUNC_DEF";
                break;
            }            
            case MATCHER: {
                op = "MATCHER";
                break;
            }
            
            case INVOKE:    {        
                op = "INVOKE";
                break;
            }
            case USER_INVOKE: {
                op = "USER_INVOKE";
                break;
            }

            /* arithmetic operators */
            case ADD:    {
                op = "ADD";
                break;
            }
            case SUB:    {
                op = "SUB";
                break;
            }
            case MUL:    {
                op = "MUL";
                break;
            }
            case DIV:    {
                op = "DIV";
                break;
            }
            case MOD:    {
                op = "MOD";
                break;
            }
            case NEG: {
                op = "NEG";
                break;
            }
                        
            case OR:    {
                op = "OR";
                break;
            }
            case AND:    {
                op = "AND";
                break;
            }
            case NOT:    {
                op = "NOT";
                break;
            }
            case EQ:    {
                op = "EQ";
                break;
            }
            case NEQ:    {
                op = "NEQ";
                break;
            }
            case GT:    {
                op = "GT";
                break;
            }
            case GTE:    {
                op = "GTE";
                break;
            }
            case LT:    {
                op = "LT";
                break;
            }
            case LTE:    {
                op = "LTE";
                break;
            }

            case LINE: {
                op = "LINE";
                break;
            }           
            default: {
                throw new Jslt2Exception("Unknown Opcode: " + opcode);
            }
        }
    
        return op;
    }
    
    /**
     * The opcode is in the range of 0-255 
     */
    public static final int
                
        /* loading of values */
        LOAD_CONST = 1,               /* ARGx */
        LOAD_LOCAL = 2,               /* ARGx */
        LOAD_OUTER = 3,               /* ARGx */
        
        LOAD_NULL  = 4,               /*      */
        LOAD_TRUE  = 5,               /*      */
        LOAD_FALSE = 6,               /*      */        
        LOAD_INPUT = 7,               /*      */
        
        /* storage of values */
        STORE_LOCAL = 8,              /* ARGx */
        
        /* pseudo bytecodes */
        xLOAD_OUTER  = 9,             /* ARGx */
        xLOAD_LOCAL  = 10,            /* ARGx */
                
        /* jump instructions */
        JMP  = 11,                    /* ARGsx */
        IFEQ = 12,                    /* ARGsx */
        
        
        /* value creation */
        NEW_ARRAY  = 13,              /*       */
        SEAL_ARRAY = 14,
        NEW_OBJ    = 15,              /*       */
        SEAL_OBJ   = 16,
        
        ADD_FIELDK   = 17,            /* ARGx  */
        ADD_FIELD    = 18,            /*       */
        ADD_ELEMENT  = 19,            /*       */

        /* member access */   
        GET_FIELDK = 20,              /* ARGx */
        GET_FIELD  = 21,              /*      */
        
        /* type declarations */
        FUNC_DEF       = 22,          /* ARGx  */        
        FOR_ARRAY_DEF  = 23,          /* ARGx  */
        FOR_OBJ_DEF    = 24,          /* ARGx  */
        
        MATCHER      = 25,            /* ARGx  */
        ARRAY_SLICE  = 26,            /*      */
        
        /* method invocation */
        INVOKE      = 27,             /* ARG1, ARG2 */          
        USER_INVOKE = 28,             /* ARG1, ARG2 */
        

        /* arithmetic operators */
        ADD = 29,                     /*      */
        SUB = 30,                     /*      */
        MUL = 31,                     /*      */
        DIV = 32,                     /*      */
        MOD = 33,                     /*      */
        NEG = 34,                     /*      */
            
        OR  = 35,                     /*      */
        AND = 36,                     /*      */
        NOT = 37,                     /*      */
        
        EQ  = 38,                     /*      */
        NEQ = 39,                     /*      */
        GT  = 40,                     /*      */
        GTE = 41,                     /*      */
        LT  = 42,                     /*      */
        LTE = 43,                     /*      */
        
        /* debug */
        LINE = 44                     /* ARGx */
        ;
    
    
    private static final Map<String, Integer> opcodes = new HashMap<String, Integer>();
    static {                        
        opcodes.put("LOAD_CONST", LOAD_CONST);
        opcodes.put("LOAD_LOCAL", LOAD_LOCAL);
        opcodes.put("LOAD_OUTER", LOAD_OUTER);        
        opcodes.put("LOAD_NULL", LOAD_NULL);
        opcodes.put("LOAD_TRUE", LOAD_TRUE);
        opcodes.put("LOAD_FALSE", LOAD_FALSE);
        
        opcodes.put("LOAD_INPUT", LOAD_INPUT);
        
        opcodes.put("STORE_LOCAL", STORE_LOCAL);
                        
        opcodes.put("xLOAD_OUTER", xLOAD_OUTER);
        opcodes.put("xLOAD_LOCAL", xLOAD_LOCAL);

        opcodes.put("JMP", JMP);
        opcodes.put("IFEQ", IFEQ);
        
        opcodes.put("NEW_ARRAY", NEW_ARRAY);
        opcodes.put("SEAL_ARRAY", SEAL_ARRAY);
        
        opcodes.put("NEW_OBJ", NEW_OBJ);
        opcodes.put("SEAL_OBJ", SEAL_OBJ);

        opcodes.put("GET_FIELDK", GET_FIELDK);
        opcodes.put("GET_FIELD", GET_FIELD);
        
        opcodes.put("ADD_FIELD", ADD_FIELD);
        opcodes.put("ADD_FIELDK", ADD_FIELDK);
        opcodes.put("ADD_ELEMENT", ADD_ELEMENT);
        
        opcodes.put("ARRAY_SLICE", ARRAY_SLICE);
        opcodes.put("FOR_ARRAY_DEF", FOR_ARRAY_DEF);
        opcodes.put("FOR_OBJ_DEF", FOR_OBJ_DEF);
        
        opcodes.put("FUNC_DEF", FUNC_DEF);
        opcodes.put("MATCHER", MATCHER);
                        
        opcodes.put("INVOKE", INVOKE);
        opcodes.put("USER_INVOKE", USER_INVOKE);
        
        /* arithmetic operators */
        opcodes.put("ADD", ADD);
        opcodes.put("SUB", SUB);
        opcodes.put("MUL", MUL);
        opcodes.put("DIV", DIV);
        opcodes.put("MOD", MOD);
        opcodes.put("NEG", NEG);
        
        opcodes.put("OR", OR);
        opcodes.put("AND", AND);
        opcodes.put("NOT", NOT);
        
        opcodes.put("EQ", EQ);
        opcodes.put("NEQ", NEQ);
        opcodes.put("GT", GT);
        opcodes.put("GTE", GTE);
        opcodes.put("LT", LT);
        opcodes.put("LTE", LTE);
        
        opcodes.put("LINE", LINE);    
    }

}
