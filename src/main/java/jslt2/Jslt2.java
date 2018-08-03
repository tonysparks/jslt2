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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jslt2.parser.Parser;
import jslt2.parser.Scanner;
import jslt2.parser.Source;
import jslt2.util.Jslt2Util;
import jslt2.vm.Bytecode;
import jslt2.vm.VM;
import jslt2.vm.compiler.Compiler;

/**
 * The main API to JSLT2.
 * 
 * <p>
 * In general, you only need one instance of <code>Jslt2</code>, but they are lightweight enough to create multiple.  
 * The method {@link Jslt2#addFunction(String, Jslt2Function)} are not thread-safe, you should add your functions on startup 
 * of your application.  The rest of the functions (such as {@link Jslt2#eval(Bytecode, JsonNode)}, {@link Jslt2#compile(File)}) are 
 * thread-safe.  
 * 
 * <p>
 * The {@link Template} class it <b>not</b> thread-safe, please see {@link Template} for more information. 
 * 
 * @see Template
 * 
 * @author Tony
 *
 */
public class Jslt2 {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        // TODO: Implement proper command line arguments
        if(args.length == 0) {
            System.out.println("<usage> jslt2 [options] ");
        }
        
        Jslt2 runtime = Jslt2.builder().enableDebugMode(true).build();
        JsonNode input = runtime.getObjectMapper().readTree(new FileReader(new File(args[1])));
        
        Template template = runtime.compile(new FileReader(new File(args[0])));
        JsonNode result = template.eval(input);
        
        System.out.println(result);        
    }
    
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
        private boolean includeNulls = false;
        
        private boolean printBytecode = false;
        
        private int minStackSize = 1024;
        private int maxStackSize = Integer.MAX_VALUE;
        
        private ObjectMapper objectMapper;
        private ResourceResolver resolver = ResourceResolvers.newClassPathResolver();        
        
        public Builder printBytecode(boolean printBytecode) {
            this.printBytecode = printBytecode;
            return this;
        }
        
        public Builder enableDebugMode(boolean enableDebugMode) {
            this.isDebugMode = enableDebugMode;
            return this;
        }
        
        public Builder includeNulls(boolean includeNulls) {
            this.includeNulls = includeNulls;
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
        
        public Builder resourceResolver(ResourceResolver resolver) {
            this.resolver = resolver;
            return this;
        }
        
        public Jslt2 build() {
            return new Jslt2(this.objectMapper != null 
                                ? this.objectMapper : new ObjectMapper(), 
                             this.resolver,
                             this.isDebugMode, 
                             this.includeNulls,
                             this.printBytecode,
                             this.minStackSize, 
                             this.maxStackSize);
        }
    }
    
    private boolean isDebugMode;
    private boolean includeNulls;
    private boolean printBytecode;
    
    private int minStackSize;
    private int maxStackSize;
    
    private ObjectMapper objectMapper;
    
    private ResourceResolver resolver;
    
    private Compiler compiler;        
    private Map<String, Jslt2Function> userFunctions;
    
    /**
     * @param objectMapper
     */
    public Jslt2(ObjectMapper objectMapper, 
                 ResourceResolver resolver,
                 boolean debugMode,
                 boolean includeNulls,
                 boolean printBytecode,
                 int minStackSize, 
                 int maxStackSize) {
        
        this.objectMapper = objectMapper;
        this.resolver = resolver;
        
        this.isDebugMode = debugMode;
        this.includeNulls = includeNulls;
        this.printBytecode = printBytecode;
        
        this.minStackSize = minStackSize;
        this.maxStackSize = maxStackSize;
        
        this.compiler = new Compiler(this);        
        this.userFunctions = new HashMap<>();
        
        new Jslt2StdLibrary(this);
    }
    
    public Jslt2() {
        this(new ObjectMapper(), 
             ResourceResolvers.newClassPathResolver(), 
             false, 
             false,
             false,
             1024, 
             Integer.MAX_VALUE);
    }
    
    /**
     * Add the functions to this runtime
     * 
     * @param functions
     */
    public Jslt2 addFunctions(Collection<Jslt2Function> functions) {
        for(Jslt2Function f : functions) {
            addFunction(f.name(), f);
        }
        
        return this;
    }
    
    /**
     * Register a user defined {@link Jslt2Function}
     * 
     * @param name
     * @param function
     */
    public Jslt2 addFunction(String name, Jslt2Function function) {        
        this.userFunctions.put(name, function);
        
        return this;
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
    
    /**
     * Determines if the {@link Jslt2Function} exists
     * 
     * @param name
     * @return true if the function is defined
     */
    public boolean hasFunction(String name) {
        return this.userFunctions.containsKey(name);
    }
    
    public ObjectNode newObjectNode() {
        return new ObjectNode(this.objectMapper.getNodeFactory());
    }
    
    public ArrayNode newArrayNode(int capacity) {
        return new ArrayNode(this.objectMapper.getNodeFactory(), capacity);
    }
    
    /**
     * @return the resolver
     */
    public ResourceResolver getResolver() {
        return resolver;
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
        if(this.printBytecode) {
            System.out.println(bytecode.dump());
        }
                
        JsonNode result = new VM(this).execute(bytecode, input);
        if(!this.includeNulls) {
            result = Jslt2Util.removeNullNodes(result);
        }
        
        return result;
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
     * @return the includeNulls
     */
    public boolean includeNulls() {
        return includeNulls;
    }
    
    public boolean printBytecode() {
        return printBytecode;        
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
