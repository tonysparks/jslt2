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
import java.util.HashMap;
import java.util.Map;

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
 * The main API to JSLT2.
 * 
 * @author Tony
 *
 */
public class Jslt2 {

    /**
     * @return a new {@link Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * A Builder to help create a {@link Jslt2} runtime
     * 
     * @author Tony
     *
     */
    public static class Builder {
        private boolean isDebugMode = false;
        private int minStackSize = 1024;
        private int maxStackSize = Integer.MAX_VALUE;
        
        private ObjectMapper objectMapper;
        
        public Builder enableDebugMode(boolean enableDebugMode) {
            this.isDebugMode = enableDebugMode;
            return this;
        }
        
        public Builder minStackSize(int minStackSize) {
            this.minStackSize = minStackSize;
            return this;
        }
        
        public Builder maxStackSize(int maxStackSize) {
            this.maxStackSize = maxStackSize;
            return this;
        }
        
        public Builder objectMapper(ObjectMapper mapper) {
            this.objectMapper = mapper;
            return this;
        }
        
        public Jslt2 build() {
            return new Jslt2(this.objectMapper != null 
                                ? this.objectMapper : new ObjectMapper(), 
                             this.isDebugMode, 
                             this.minStackSize, 
                             this.maxStackSize);
        }
    }
    
    private boolean isDebugMode;
    private int minStackSize;
    private int maxStackSize;
    
    private ObjectMapper objectMapper;
    
    private Compiler compiler;
    private VM vm;
    
    private Map<String, Jslt2Function> userFunctions;
    
    /**
     * @param objectMapper
     */
    public Jslt2(ObjectMapper objectMapper, boolean debugMode, int minStackSize, int maxStackSize) {
        this.objectMapper = objectMapper;
        
        this.isDebugMode = debugMode;
        this.minStackSize = minStackSize;
        this.maxStackSize = maxStackSize;
        
        this.vm = new VM(this);
        this.compiler = new Compiler(this);
        
        this.userFunctions = new HashMap<>();
    }
    
    public Jslt2() {
        this(new ObjectMapper(), false, 1024, Integer.MAX_VALUE);
    }
    
    /**
     * Register a user defined {@link Jslt2Function}
     * 
     * @param name
     * @param function
     */
    public void addFunction(String name, Jslt2Function function) {
        this.userFunctions.put(name, function);
    }
    
    /**
     * Get a {@link Jslt2Function} by name
     * 
     * @param name
     * @return the {@link Jslt2Function} if registered otherwise null
     */
    public Jslt2Function getFunction(String name) {
        return this.userFunctions.get(name);
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
        return eval(code, input);
    }
        
    /**
     * Evaluates the supplied {@link Bytecode} with the supplied {@link JsonNode}
     * input.
     * 
     * @param bytecode
     * @param input
     * @return the {@link JsonNode} result
     */
    public JsonNode eval(Bytecode bytecode, JsonNode input) {
        if(this.isDebugMode) {
            System.out.println(bytecode.dump());
        }
        
        return this.vm.execute(bytecode, input);
    }
    

    /**
     * Creates a {@link Template} for the expression so that it can be
     * executed multiple times with different inputs
     * 
     * @param file the file containing the expression template
     * @return the {@link Template}
     */
    public Template compile(File file) {
        try {
            return compile(new BufferedReader(new FileReader(file)));
        }
        catch(IOException e) {
            throw new Jslt2Exception(e);
        }
    }
    
    /**
     * Creates a {@link Template} for the expression so that it can be
     * executed multiple times with different inputs
     * 
     * @param expression
     * @return the {@link Template}
     */
    public Template compile(String expression) {
        return compile(new StringReader(expression));
    }
    
    /**
     * Creates a {@link Template} for the expression so that it can be
     * executed multiple times with different inputs
     * 
     * @param reader
     * @return the {@link Template}
     */
    public Template compile(Reader reader) {
        Source source = new Source(reader);
        Scanner scanner = new Scanner(source);
        Parser parser = new Parser(scanner);
        
        Bytecode code = this.compiler.compile(parser.parseProgram());
        return new Template(this, code);
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
