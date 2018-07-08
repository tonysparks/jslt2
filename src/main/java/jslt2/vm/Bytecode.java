/*
 * see license.txt
 */
package jslt2.vm;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.TextNode;

import jslt2.Jslt2Exception;
import jslt2.vm.compiler.DebugSymbols;
import jslt2.vm.compiler.Outer;


/**
 * @author Tony
 *
 */
public class Bytecode {

    private static final String lineFormat2 = "%-12s %-16s \n";
    private static final String lineFormat3 = "%-12s %-16s \t\t; %s \n";
    private static final String lineFormat4 = "%-12s %-16s \t\t; %-6s (%s) \n";
    private static final String lineFormat4Ex = "%-12s %-8s %-6s \t\t; %-6s \n";
    private static final String Indent = "  ";
    
    public static final int MAGIC_NUMBER = 0x1E01A;
    public static final int VERSION = 1;
    
    public static final int FL_DEBUG       = (1<<0);
    public static final int FL_BLOCKS      = (1<<1);
    public static final int FL_VARARGS     = (1<<2);
    public static final int FL_PARAMS_IDX  = (1<<3);
    
    
    public int flags;
    
    public final int[] instr;
    public       int pc;
    public final int len;
    
    public JsonNode[] constants;
    public int numConstants;
        
    public DebugSymbols debugSymbols;        
    
    public int numLocals;        
    public int numOuters;
    
    public JsonNode[] paramNames;
    
    public int numArgs;
    public int numInners;
            
    public int maxstacksize;
    
    public Outer[] outers;
    
    public Bytecode[] inner;    
        
    /**
     * @param instructions
     */
    public Bytecode(int[] instructions) {
        this(instructions, 0, instructions.length);
    }

    /**
     * @param instr
     * @param pc
     * @param len
     */
    public Bytecode(int[] instr, int pc, int len) {        
        this.instr = instr;
        this.pc = pc;
        this.len = len;        
    }
            
    /**
     * denotes that this byte code contains debug information
     */
    public void setDebug() {
        this.flags |= FL_DEBUG;
    }
        
    
    /**
     * @return true if this contains debug information
     */
    public boolean hasDebug() {
        return (this.flags & FL_DEBUG) != 0;
    }
    
    
    /**
     * Sets the filename in which generated this {@link Bytecode}.  Only
     * stores this information in DEBUG mode.
     * 
     * @param filename
     */
    public void setSourceFile(File filename) {
        /*
         * TEMP HACK -- move the compiler!!!
         */
        
        if(this.debugSymbols!=null) {
            this.debugSymbols.setSourceFile(filename);
            
            for(int i = 0; i < this.numInners; i++) {
                this.inner[i].setSourceFile(filename);
            }
        }                
    }
    
    /**
     * @return the source file that created this {@link Bytecode}
     */
    public File getSourceFile() {
        return (this.debugSymbols!=null) ? this.debugSymbols.getSourceFile() : null;
    }
    
    /**
     * @return the name of the source file that created this {@link Bytecode}
     */
    public String getSourceFileName() {
        File sourceFile = getSourceFile();
        if(sourceFile != null) {
            return sourceFile.getName();
        }
        return "";
    }
    
    /**
     * Clones this {@link Bytecode}
     */
    public Bytecode clone() {
        Bytecode clone = new Bytecode(instr);
        clone.flags = this.flags;
        clone.constants = this.constants;        
        clone.debugSymbols = this.debugSymbols;
        clone.inner = new Bytecode[this.numInners];
        for(int i = 0; i<this.numInners;i++) {
            clone.inner[i] = this.inner[i].clone();
        }
        
        clone.maxstacksize = this.maxstacksize;
        clone.numArgs = this.numArgs;            
        clone.numConstants = this.numConstants;
        clone.numInners = this.numInners;
        clone.numLocals = this.numLocals;
        clone.numOuters = this.numOuters;
        clone.paramNames = this.paramNames;
                
        return clone;
    }
    
    /**
     * @see Bytecode#dump()
     */
    @Override
    public String toString() {
        return dump();
    }
    
    /**
     * Dump the contents of the {@link Bytecode} into a {@link String}
     * 
     * @return the {@link Bytecode} represented as a {@link String}
     */
    public String dump() {
        StringBuilder sb = new StringBuilder();
        return dump(sb, 0, this.pc, this.len);
    }
    
    private static String quote(Object obj) {
        return "\"" + obj + "\"";
    }
    
    /**
     * Dump the contents of the {@link Bytecode} into a {@link String}
     * 
     * @param sb
     * @param numTabs
     * @param pc
     * @param len
     * @return the {@link Bytecode} represented as a {@link String}
     */
    private String dump(StringBuilder sb, int numTabs, int pc, int len) {
                
        for(int t = 0; t < numTabs; t++) sb.append(Indent);
        //sb.append(".locals ").append(this.numLocals).append("\n");
        sb.append(String.format(lineFormat2, ".locals", Integer.toString(this.numLocals)));
        
        for(int i = 0; i < this.numConstants; i++) {
            for(int t = 0; t < numTabs; t++) sb.append(Indent);
            if(this.constants[i].isTextual())                 
                sb.append(String.format(lineFormat3, ".const", quote(this.constants[i]), "[ index: " + i + "]") );
            else 
                sb.append(String.format(lineFormat3, ".const", this.constants[i].toString(), "[ index: " + i + "]") );
        }
        
        dumpRaw(this, sb, numTabs, this.instr, pc, len);        
        return sb.toString();
    }
    
    /**
     * 
     * @param bytecode
     * @param sb
     * @param numTabs
     * @param instr
     * @param pc
     * @param len
     */
    private static void dumpRaw(Bytecode bytecode, StringBuilder sb, int numTabs, int[] instr, int pc, int len) {
        List<Integer> visited = new ArrayList<Integer>(bytecode.numInners);
        for(int i = pc; i < len; i++) {
            int code = instr[i];
            
            int iopcode = Opcodes.OPCODE(code);
            String opcode = Opcodes.op2str(iopcode);
            
            for(int t = 0; t < numTabs; t++) sb.append(Indent);
            switch(iopcode) {
                case Opcodes.TAIL_CALL:
                case Opcodes.NEW_OBJ: 
                case Opcodes.INVOKE: {
                    String arg1 = Integer.toString(Opcodes.ARG1(code));
                    String arg2 = Integer.toString(Opcodes.ARG2(code));
                    if( "0".equals(arg2)) {
                        sb.append(String.format(lineFormat3, opcode, arg1, i));
                    }
                    else {
                        sb.append(String.format(lineFormat4Ex, opcode, arg1, arg2, i));
                    }
                    break;
                }
                case Opcodes.FUNC_DEF: {                                
                    int inner = Opcodes.ARGx(code);
                    Bytecode bc = bytecode.inner[inner];
                    sb.append(String.format(lineFormat3, opcode, bc.numArgs, i));
                    
                    visited.add(inner);
                    
                    bc.dump(sb, numTabs + 1, bc.pc, bc.len);
                    //for(int t = 0; t < numTabs; t++) sb.append("\t");
                    
                    while(i < len) {
                        int pCode = Opcodes.OPCODE(instr[i+1]);
                        if(Opcodes.xLOAD_LOCAL == pCode || 
                           Opcodes.xLOAD_OUTER == pCode ) {
                            
                            for(int t = 0; t < numTabs + 1; t++) sb.append(Indent);
                            String argx = Integer.toString(Opcodes.ARGx(instr[i+1]));                                                        
                            sb.append(String.format(lineFormat3, Opcodes.op2str(pCode), argx, i));
                            
                            i++;
                        }
                        else break;
                    }
                    
                    for(int t = 0; t < numTabs; t++) sb.append(Indent);
                    sb.append(".end\n");                                   
                                   
                    break;
                }
                case Opcodes.IFEQ:
                case Opcodes.JMP: {
                    String argsx = Integer.toString(Opcodes.ARGsx(code));                                                     
                    sb.append(String.format(lineFormat3, opcode, argsx, i));
                    break;
                }            
                
                case Opcodes.POP:
                case Opcodes.DUP:
                                
                case Opcodes.LOAD_NULL:
                case Opcodes.LOAD_TRUE:
                case Opcodes.LOAD_FALSE:
                    
                case Opcodes.ADD:
                case Opcodes.SUB:
                case Opcodes.MUL:
                case Opcodes.DIV:
                case Opcodes.MOD:
                case Opcodes.NEG:
                
                case Opcodes.OR:
                case Opcodes.AND:
                case Opcodes.NOT:
                    
                case Opcodes.EQ:
                case Opcodes.NEQ:
                case Opcodes.GT:
                case Opcodes.GTE:
                case Opcodes.LT:
                case Opcodes.LTE: {                                                                 
                    sb.append(String.format(lineFormat3, opcode, "", i));
                    break;
                }
                case Opcodes.STORE_LOCAL:
                case Opcodes.LOAD_LOCAL: {
                    if(bytecode.hasDebug()) {
                        String argx = Integer.toString(Opcodes.ARGx(code));                                                         
                        sb.append(String.format(lineFormat4, opcode, argx, i, bytecode.debugSymbols.getSymbol(Opcodes.ARGx(code), i)));
                    }
                    else {
                        String argx = Integer.toString(Opcodes.ARGx(code));                                                         
                        sb.append(String.format(lineFormat3, opcode, argx, i));
                    }
                    break;
                }
                case Opcodes.GET_FIELDK:
                case Opcodes.LOAD_CONST: {
                    String argx = Integer.toString(Opcodes.ARGx(code));                                                         
                    sb.append(String.format(lineFormat4, opcode, argx, i, bytecode.constants[Opcodes.ARGx(code)]));
                    break;
                }
                
                default: {
                    String argx = Integer.toString(Opcodes.ARGx(code));                                                         
                    sb.append(String.format(lineFormat3, opcode, argx, i));
                }
            }
        }    
                
        if ( bytecode.inner != null && bytecode.inner.length > 0 ) {
            sb.append("\n");
                
            for(int i = 0; i < bytecode.numInners; i++) {
                if(visited.contains(i)) {
                    continue;
                }
                
                Bytecode bc = bytecode.inner[i];
            
                for(int t = 0; t < numTabs; t++) sb.append(Indent);        
                sb.append("; scope ").append(i).append("\n");
                
                bc.dump(sb, numTabs + 1, bc.pc, bc.len);
                
                for(int t = 0; t < numTabs; t++) sb.append(Indent);        
                sb.append(".end ; scope ").append(i).append("\n");
            }
        }
    }
    
    /**
     * Writes the {@link Bytecode} out to a stream.
     * 
     * @param out
     * @throws IOException
     */
    public void write(DataOutput out) throws IOException {
        out.writeInt(MAGIC_NUMBER);    /* magic number */
        out.writeInt(VERSION); /* the version */
        
        switch(VERSION) {
            case 1: {
                writeVersion1(out); /* notice the 2 */
                break;    
            }
            
            default: {
                throw new IOException("Unsupported version: " + VERSION);
            }
        }
    }

    /**
     * Writes out Version 2
     * @param out
     * @throws IOException
     */
    private void writeVersion1(DataOutput out) throws IOException {                
        out.writeInt(this.len); /* length */
        for(int i = this.pc; i < this.len; i++) {
            out.writeInt(this.instr[i]);
        }
        
        out.writeInt(this.maxstacksize);
                
        if ( constants != null ) {
            out.writeInt(this.numConstants);
            for(int i = 0; i < this.numConstants; i++) {
                if(constants[i].isNumber()) {
                    out.writeByte(0); // 0 = Number
                    out.writeDouble(constants[i].asDouble());
                }
                else if(constants[i].isTextual()) {
                    out.writeByte(1); // 1 = String
                    out.writeUTF(constants[i].asText());          
                }
                else {
                    throw new Jslt2Exception("Illegal constant type: " + constants[i]);
                }
            }
        }
        else {
            out.writeInt(0);
        }
        
        out.writeInt(this.flags);
        out.writeInt(this.numArgs);            
        out.writeInt(this.numOuters);
        out.writeInt(this.numLocals);
        
        for(int i = 0; i < this.numArgs; i++) {
            if(this.paramNames[i] == null) {
                out.writeUTF("");
            }
            else {
                out.writeUTF(this.paramNames[i].asText());
            }
        }
        
        /* write out the var names if this is a class */        
        if( (this.flags & FL_DEBUG) != 0) {
            this.debugSymbols.write(out);
        }
        
        if ( this.inner != null ) {
            out.writeInt(this.inner.length);
            for(int i = 0; i < this.inner.length; i++ ) {
                this.inner[i].write(out);
            }
        }
        else {
            out.writeInt(0);
        }
    }
    
    
    /**
     * Reads from the {@link DataInput} stream, constructing the appropriate {@link Bytecode}
     * 
     * @param in
     * @return the {@link Bytecode}
     * @throws IOException
     */
    public static Bytecode read(JsonNode env, DataInput in) throws IOException {
        int magic = in.readInt();
        if ( magic != MAGIC_NUMBER ) {
            throw new IllegalArgumentException
                ("The magic number doesn't match 0x" + Integer.toHexString(MAGIC_NUMBER) +" : 0x" + Integer.toHexString(magic));
        }
        
        int version = in.readInt();
        Bytecode code = null;
        switch(version) {
            case 1: {
                code = readVersion1(env, in);
                break;
            }
            default: {
                throw new IOException("Illegal version: " + version);
            }
        }
        
        return code;
    }
    
    /**
     * Reads Version 1
     * @param symbols
     * @param in
     * @return
     * @throws IOException
     */
    private static Bytecode readVersion1(JsonNode env, DataInput in) throws IOException {
        int len = in.readInt();
        int[] instr = new int[len];
        for(int i = 0; i < len; i++) {
            instr[i] = in.readInt();
        }
        
        Bytecode result = new Bytecode(instr);
        result.maxstacksize = in.readInt();        
        
        result.numConstants = in.readInt();        
        result.constants = new JsonNode[result.numConstants];
        for(int i = 0; i < result.numConstants; i++) {
            byte type = in.readByte();
            switch(type) {
                case 0: result.constants[i] = DoubleNode.valueOf(in.readDouble());
                    break;
                case 1: result.constants[i] = TextNode.valueOf(in.readUTF());
                    break;
                default: {
                    throw new Jslt2Exception("Illegal constant type: " + type);
                }
            }
        }
        
        result.flags = in.readInt();
        result.numArgs = in.readInt();                
        result.numOuters = in.readInt();                
        result.numLocals = in.readInt();    
        
        result.paramNames = new JsonNode[result.numArgs];
        for(int i = 0; i < result.numArgs; i++) {
            String text = in.readUTF();
            result.paramNames[i] = TextNode.valueOf(text);
        }
                
        if( (result.flags & FL_DEBUG) != 0 ) {
            result.debugSymbols = DebugSymbols.read(in);
        }        
                
        result.numInners = in.readInt();
        result.inner = new Bytecode[result.numInners];
        for(int i = 0; i < result.numInners; i++ ) {            
            Bytecode code = Bytecode.read(env, in);            
            result.inner[i] = code;            
        }                
        
        return result;
    }

}
