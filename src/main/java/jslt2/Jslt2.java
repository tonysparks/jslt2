/*
 * see license.txt
 */
package jslt2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jslt2.parser.Parser;
import jslt2.parser.Scanner;
import jslt2.parser.Source;
import jslt2.vm.Bytecode;
import jslt2.vm.VM;
import jslt2.vm.compiler.Compiler;

/**
 * @author Tony
 *
 */
public class Jslt2 {

    private boolean isDebugMode;
    private int minStackSize;
    private int maxStackSize;
    
    private ObjectMapper objectMapper;
    
    private Compiler compiler;
    private VM vm;
    
    /**
     * 
     */
    public Jslt2(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        
        this.isDebugMode = true;
        
        this.vm = new VM(this);
        this.compiler = new Compiler(this);
    }
    
    public Jslt2() {
        this(new ObjectMapper());
    }
    
    public ObjectNode newObjectNode() {
        return new ObjectNode(this.objectMapper.getNodeFactory());
    }
    
    public ArrayNode newArrayNode(int capacity) {
        return new ArrayNode(this.objectMapper.getNodeFactory(), capacity);
    }
    
    public JsonNode eval(File file, JsonNode input) {
        try {
            return eval(new BufferedReader(new FileReader(file)), input);
        }
        catch(IOException e) {
            throw new Jslt2Exception(e);
        }
    }
    
    public JsonNode eval(String script, JsonNode input) {
        return eval(new StringReader(script), input);
    }
    
    /**
     * Evaluates the script
     * 
     * @param reader
     * @param input
     * @return the resulting {@link JsonNode}
     */
    public JsonNode eval(Reader reader, JsonNode input) {
        Source source = new Source(reader);
        Scanner scanner = new Scanner(source);
        Parser parser = new Parser(scanner);
        
        Bytecode code = this.compiler.compile(parser.parseProgram());
        
        if(this.isDebugMode) {
            System.out.println(code.dump());
        }
        
        return this.vm.execute(code, input);
    }
    
    /**
     * @return the objectMapper
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
    
    /**
     * @return the isDebugMode
     */
    public boolean isDebugMode() {
        return isDebugMode;
    }
    
    /**
     * @return the minStackSize
     */
    public int getMinStackSize() {
        return minStackSize;
    }
    
    /**
     * @return the maxStackSize
     */
    public int getMaxStackSize() {
        return maxStackSize;
    }

}
