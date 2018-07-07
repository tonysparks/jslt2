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
import com.fasterxml.jackson.databind.node.IntNode;
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
    public void testFunctionCalls() throws Exception {
        String query = new String(Files.readAllBytes(new File("./examples/functions.json").toPath()));
        
        Jslt2 runtime = new Jslt2();
        
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

            Tuple<Long, JsonNode> jslt2Result = runJslt2(array, template);
            jslt2Results.add(jslt2Result);
            
            Tuple<Long, JsonNode> jsltResult = runJslt(array, expr);
            jsltResults.add(jsltResult);
                        
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
        
        System.out.printf("Total JSLT   time: %10d nsec.  Avg. %10d nsec. \n", jsltSum, (jsltSum  / numberOfIterations));
        System.out.printf("Total JSLT2  time: %10d nsec.  Avg. %10d nsec. \n", jslt2Sum, (jslt2Sum  / numberOfIterations));
    }
}
