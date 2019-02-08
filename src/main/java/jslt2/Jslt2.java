/*
 * see license.txt
 */
package jslt2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

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
            System.out.println("<usage> jslt2 [options] -template [template file path] -input [input file path] -nulls");
            return;
        }
        
        String templatePath = null;
        String inputPath = null;
        
        boolean removeNulls = false;
        boolean displayBytecode = false;
        boolean debugMode = false;
        
        for(int i = 0; i < args.length; i++) {
            final String arg = args[i];
            switch(arg.toLowerCase()) {
                case "-template": {
                    if(i+1 >= args.length) {
                        System.out.println("template option needs a value");
                        return;
                    }
                    
                    templatePath = args[i+1];
                    i++; 
                    break;
                }
                case "-input": {
                    if(i+1 >= args.length) {
                        System.out.println("input option needs a value");
                        return;
                    }
                    
                    inputPath = args[i+1];
                    i++;
                    break;
                }
                case "-nulls": {
                    removeNulls = true;
                    break;
                }
                case "-bytecode": {
                    displayBytecode = true;
                    break;
                }
                case "-debug": {
                    debugMode = true;
                    break;
                }
            }
        }
        
        if(templatePath == null) {
            System.out.println("Requires -template option");
            return;
        }
        
        Reader inputReader = null;
        if(inputPath != null) {
            inputReader = new FileReader(inputPath);
        }
        else {
            inputReader = new InputStreamReader(System.in);
        }
        
        Jslt2 runtime = Jslt2.builder()
                .enableDebugMode(debugMode)
                .includeNulls(!removeNulls)
                .printBytecode(displayBytecode)
                .build();
        
        JsonNode input = runtime.getObjectMapper().readTree(inputReader);        
        JsonNode result = runtime.eval(new FileReader(new File(templatePath)), input);
        
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
        
        private int minStackSize = 128;
        private int maxStackSize = Integer.MAX_VALUE;
        
        private ObjectMapper objectMapper;
        private ResourceResolver resolver = ResourceResolvers.newClassPathResolver();       
        private ExecutorService executorService;
        
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
        
        public Builder executorService(ExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }
        
        public Jslt2 build() {
            return new Jslt2(this.objectMapper != null 
                                ? this.objectMapper : new ObjectMapper(), 
                             this.resolver,
                             this.executorService != null 
                                 ? this.executorService : Executors.newCachedThreadPool(new DaemonThreadFactory()),
                             this.isDebugMode, 
                             this.includeNulls,
                             this.printBytecode,
                             this.minStackSize, 
                             this.maxStackSize);
        }
    }
    
    static class DaemonThreadFactory implements ThreadFactory {
        AtomicInteger id = new AtomicInteger(0);
        
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "jslt-thread-" + id.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
    
    private boolean isDebugMode;
    private boolean includeNulls;
    private boolean printBytecode;
    
    private int minStackSize;
    private int maxStackSize;
    
    private ObjectMapper objectMapper;
    private ExecutorService executorService;
    private ResourceResolver resolver;
    
    private Compiler compiler;        
    private Map<String, Jslt2Function> userFunctions;
    private Map<String, Jslt2MacroFunction> macroFunctions;
    
    /**
     * @param objectMapper
     */
    public Jslt2(ObjectMapper objectMapper, 
                 ResourceResolver resolver,
                 ExecutorService executorService,
                 boolean debugMode,
                 boolean includeNulls,
                 boolean printBytecode,
                 int minStackSize, 
                 int maxStackSize) {
        
        this.objectMapper = objectMapper;
        this.resolver = resolver;
        this.executorService = executorService;
        
        this.isDebugMode = debugMode;
        this.includeNulls = includeNulls;
        this.printBytecode = printBytecode;
        
        this.minStackSize = minStackSize;
        this.maxStackSize = maxStackSize;
        
        this.compiler = new Compiler(this);        
        this.userFunctions = new HashMap<>();
        this.macroFunctions = new HashMap<>();
        
        new Jslt2StdLibrary(this);
    }
    
    public Jslt2() {
        this(new ObjectMapper(), 
             ResourceResolvers.newClassPathResolver(),
             Executors.newCachedThreadPool(new DaemonThreadFactory()),
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
    
    public void addMacro(Jslt2MacroFunction macro) {
        this.macroFunctions.put(macro.name(), macro);
    }
    
    public Jslt2 addMacro(String name, Jslt2MacroFunction macro) {
        this.macroFunctions.put(name, macro);
        return this;
    }
    
    public Jslt2MacroFunction getMacro(String name) {
        return this.macroFunctions.get(name);
    }
    
    public boolean hasMacro(String name) {
        return this.macroFunctions.containsKey(name);
    }
    
    public ObjectNode newObjectNode() {
        return new ObjectNode(this.objectMapper.getNodeFactory());
    }
    
    public ArrayNode newArrayNode(int capacity) {
        return new ArrayNode(this.objectMapper.getNodeFactory(), capacity);
    }
    
    /**
     * @return the executorService
     */
    public ExecutorService getExecutorService() {
        return executorService;
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
        Parser parser = new Parser(this, scanner);
        
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
        Parser parser = new Parser(this, scanner);
        
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
