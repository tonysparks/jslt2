/*
 * see license.txt 
 */
package jslt2;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Parser;

import jslt2.util.Tuple;

/**
 * @author Tony
 *
 */
public class PerformanceTest {

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Ignore
    private String query() throws Exception {
        String query = new String(Files.readAllBytes(new File("./examples/performance-test.json").toPath()));
        return query;
    }
    
    
    @Ignore
    private Tuple<Long, JsonNode> runJslt(JsonNode input, Expression expr) {
        long startTime = System.nanoTime();
        JsonNode result = expr.apply(input);
        long endTime = System.nanoTime() - startTime;
        return new Tuple<Long, JsonNode>(endTime, result);
    }
    
    @Ignore
    private Tuple<Long, JsonNode> runJslt2(JsonNode input, Template template) {
        long startTime = System.nanoTime();
        JsonNode result = template.eval(input);
        long endTime = System.nanoTime() - startTime;
        return new Tuple<Long, JsonNode>(endTime, result);
    }
    
    @Test
    public void testFunctionCallsWithValidation() throws Exception {
        String query = query();
        
        Jslt2 runtime = Jslt2.builder()
                .enableDebugMode(false)
                .build();
        
        Template template = runtime.compile(query);
        Expression expr = Parser.compileString(query);
     
        int capacity = 1000;//1024 * 1024;
        ArrayNode array = runtime.newArrayNode(capacity);
        for(int i = 0; i < capacity; i++) {
            array.add((double)i);
        }
        
        List<Tuple<Long, JsonNode>> jsltResults = new ArrayList<>();
        List<Tuple<Long, JsonNode>> jslt2Results = new ArrayList<>();
        
        Random rand = new Random();
        
        final int numberOfIterations = 1000;
        
        int iterations = numberOfIterations;
        while(iterations --> 0) {
            
            Tuple<Long, JsonNode> jsltResult = runJslt(array, expr);
            jsltResults.add(jsltResult);                        
            
            Tuple<Long, JsonNode> jslt2Result = runJslt2(array, template);
            jslt2Results.add(jslt2Result);
                        
            for(int i = 0; i < capacity; i++) {    
                array.set(rand.nextInt(capacity), DoubleNode.valueOf(rand.nextInt(Integer.MAX_VALUE)));
            }
        }
        
        long jsltSum = 0L;
        long jslt2Sum = 0L;
        
        for(int i = 0; i < jslt2Results.size(); i++) {
            Tuple<Long, JsonNode> jsltResult = jsltResults.get(i);
            Tuple<Long, JsonNode> jslt2Result = jslt2Results.get(i);
            
            assertEquals(jsltResult.getSecond(), jslt2Result.getSecond());
            
            jsltSum  += jsltResult.getFirst();
            jslt2Sum += jslt2Result.getFirst();
        }
        
        System.out.printf("Total JSLT-AST total time: %10d nsec.  Avg. %10d nsec. \n", jsltSum, (jsltSum  / numberOfIterations));
        System.out.printf("Total JSLT-VM  total time: %10d nsec.  Avg. %10d nsec. \n", jslt2Sum, (jslt2Sum  / numberOfIterations));
    }
    
    
    @Test
    public void testFunctionCallsNoValidation() throws Exception {
        String query = query();
        
        Jslt2 runtime = Jslt2.builder()
                .enableDebugMode(false)
                .build();
        
        Template template = runtime.compile(query);
        Expression expr = Parser.compileString(query);
     
        int capacity = 1000;//1024 * 1024;
        ArrayNode array = runtime.newArrayNode(capacity);
        for(int i = 0; i < capacity; i++) {
            array.add((double)i);
        }
        
        List<Long> jsltResults = new ArrayList<>();
        List<Long> jslt2Results = new ArrayList<>();
        
        Random rand = new Random();
        
        final int numberOfIterations = 1000;
        
        int iterations = numberOfIterations;
        while(iterations --> 0) {
            
            Tuple<Long, JsonNode> jsltResult = runJslt(array, expr);
            jsltResults.add(jsltResult.getFirst());                        
            
            Tuple<Long, JsonNode> jslt2Result = runJslt2(array, template);
            jslt2Results.add(jslt2Result.getFirst());
                        
            for(int i = 0; i < capacity; i++) {    
                array.set(rand.nextInt(capacity), DoubleNode.valueOf(rand.nextInt(Integer.MAX_VALUE)));
            }
        }
        
        long jsltSum = 0L;
        long jslt2Sum = 0L;
        
        for(int i = 0; i < jslt2Results.size(); i++) {
            long jsltResult = jsltResults.get(i);
            long jslt2Result = jslt2Results.get(i);
            
            jsltSum  += jsltResult;
            jslt2Sum += jslt2Result;
        }
        
        System.out.printf("Total JSLT-AST total time: %10d nsec.  Avg. %10d nsec. \n", jsltSum, (jsltSum  / numberOfIterations));
        System.out.printf("Total JSLT-VM  total time: %10d nsec.  Avg. %10d nsec. \n", jslt2Sum, (jslt2Sum  / numberOfIterations));
    }
    
    @Test
    public void testVmFunctionCalls() throws Exception {
        String query = query();
        
        Jslt2 runtime = Jslt2.builder()
                .enableDebugMode(false)
                .build();
        
        Template template = runtime.compile(query);
     
        int capacity = 1000;//1024 * 1024;
        ArrayNode array = runtime.newArrayNode(capacity);
        for(int i = 0; i < capacity; i++) {
            array.add((double)i);
        }
        
        List<Long> jslt2Results = new ArrayList<>();
        
        Random rand = new Random();
        
        final int numberOfIterations = 1000;//10_000_000;
        
        int iterations = numberOfIterations;
        while(iterations --> 0) {

            Tuple<Long, JsonNode> jslt2Result = runJslt2(array, template);
            jslt2Results.add(jslt2Result.getFirst());
                        
            for(int i = 0; i < capacity; i++) {    
                array.set(rand.nextInt(capacity), DoubleNode.valueOf(rand.nextInt(Integer.MAX_VALUE)));
            }
        }
        
        long jslt2Sum = 0L;
        
        for(int i = 0; i < jslt2Results.size(); i++) {
            long time = jslt2Results.get(i);
            jslt2Sum += time;
        }
        
        System.out.printf("Total JSLT-VM  total time: %10d nsec.  Avg. %10d nsec. \n", jslt2Sum, (jslt2Sum  / numberOfIterations));
    }
    
    @Test
    public void testFunctionCallsAstWalkerNoValidation() throws Exception {
        String query = query();
        
        Jslt2 runtime = Jslt2.builder()
                .enableDebugMode(false)
                .build();
                
        Expression expr = Parser.compileString(query);
     
        int capacity = 1000;//1024 * 1024;
        ArrayNode array = runtime.newArrayNode(capacity);
        for(int i = 0; i < capacity; i++) {
            array.add((double)i);
        }
        
        List<Long> jsltResults = new ArrayList<>();
        
        Random rand = new Random();
        
        final int numberOfIterations = 1000;
        
        int iterations = numberOfIterations;
        while(iterations --> 0) {
            
            Tuple<Long, JsonNode> jsltResult = runJslt(array, expr);
            jsltResults.add(jsltResult.getFirst());                        
                        
            for(int i = 0; i < capacity; i++) {    
                array.set(rand.nextInt(capacity), DoubleNode.valueOf(rand.nextInt(Integer.MAX_VALUE)));
            }
        }
        
        long jsltSum = 0L;
        
        for(int i = 0; i < jsltResults.size(); i++) {
            long jsltResult = jsltResults.get(i);
            
            jsltSum  += jsltResult;
        }
        
        System.out.printf("Total JSLT-AST   total time: %10d nsec.  Avg. %10d nsec. \n", jsltSum, (jsltSum  / numberOfIterations));
    }
    
}

