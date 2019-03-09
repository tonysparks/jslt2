/*
 * see license.txt
 */
package jslt2;

import java.io.File;
import java.nio.file.Files;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import static org.junit.Assert.*;

/**
 * @author Tony
 *
 */
public class AsyncTest {

    private Jslt2 runtime = Jslt2.builder()
            .resourceResolver(ResourceResolvers.newFilePathResolver(new File("./examples")))
            .build();
    
    {
        runtime.addFunction("sleep", (in, args) -> {
            try {
                Thread.sleep(args[0].asLong());
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return args[1];
        });
    }
    
    
    @Test
    public void testAsyncConstant() throws Exception {        
        ObjectNode input = runtime.newObjectNode();
        input.set("name", TextNode.valueOf("tony"));
        input.set("team", TextNode.valueOf("packers"));
        
        String script = new String(Files.readAllBytes(new File("./examples/async.json").toPath()));         
        JsonNode result = runtime.eval(script, input);
        System.out.println(result);
       // testAgainstSpec(input, query);
        assertEquals("{\"name\":\"favre\",\"a\":true,\"b\":{\"b1\":\"z\",\"c1\":\"hi\",\"name\":\"tony\",\"team\":\"packers\"},\"c\":20,\"d\":[10,8],\"e\":{\"test\":{\"i\":0}},\"f\":2,\"g\":\"g2\"}", result.toString());
    }
    
    @Test(expected=Jslt2Exception.class)
    public void testAsyncBadFunctionReference() throws Exception {        
        ObjectNode input = runtime.newObjectNode();
        
        String script = new String(Files.readAllBytes(new File("./examples/async-bad-def.json").toPath()));         
        runtime.eval(script, input);
    }
    
    @Test(expected=Jslt2Exception.class)
    public void testAsyncBadVarReference() throws Exception {        
        ObjectNode input = runtime.newObjectNode();
        
        String script = new String(Files.readAllBytes(new File("./examples/async-bad-var.json").toPath()));         
        runtime.eval(script, input);
    }
    
    @Test(expected=Jslt2Exception.class)
    public void testAsyncReferenceVarInAsync() throws Exception {        
        ObjectNode input = runtime.newObjectNode();
        
        String script = new String(Files.readAllBytes(new File("./examples/async-bad-var-in-async.json").toPath()));         
        runtime.eval(script, input);        
    }
    
    @Test(expected=Jslt2Exception.class)
    public void testAsyncReferenceVarInAsync2() throws Exception {        
        ObjectNode input = runtime.newObjectNode();
        
        String script = new String(Files.readAllBytes(new File("./examples/async-bad-var-in-async2.json").toPath()));         
        JsonNode result = runtime.eval(script, input);
        System.out.println(result);
    }
    
    @Test
    public void testAsyncReferenceVarInFunction() throws Exception {        
        ObjectNode input = runtime.newObjectNode();
        
        String script = new String(Files.readAllBytes(new File("./examples/async-reference-var-in-function.json").toPath()));         
        JsonNode result = runtime.eval(script, input);
        System.out.println(result);
        assertEquals("{\"k\":\"hi\"}", result.toString());
    }
    
    @Test
    public void testAsyncReferenceVar() throws Exception {        
        ObjectNode input = runtime.newObjectNode();
        
        String script = new String(Files.readAllBytes(new File("./examples/async-reference-var.json").toPath()));         
        JsonNode result = runtime.eval(script, input);
        System.out.println(result);
        assertEquals("{\"k\":\"hi\"}", result.toString());
    }
    
    @Test
    public void testAsyncEmbed() throws Exception {        
        ObjectNode input = runtime.newObjectNode();        
        String script = new String(Files.readAllBytes(new File("./examples/async-embed.json").toPath()));         
        long startTime = System.currentTimeMillis();
        JsonNode result = runtime.eval(script, input);
        long endTime = System.currentTimeMillis();
        
        System.out.println(result);       
        System.out.println("Total time: " + (endTime - startTime) + " msec.");
        assertEquals("{\"c\":\"2\"}", result.toString());
     
    }
    
    @Test
    public void testAsyncSpeed() throws Exception {        
        ObjectNode input = runtime.newObjectNode();
        input.set("name", TextNode.valueOf("tony"));
        input.set("team", TextNode.valueOf("packers"));
        
        String script = new String(Files.readAllBytes(new File("./examples/sleep.json").toPath()));      
        long startTime = System.currentTimeMillis();
        JsonNode result = runtime.eval(script, input);
        long endTime = System.currentTimeMillis();
        
        System.out.println(result);       
        System.out.println("(No Async) Total time: " + (endTime - startTime) + " msec.");
        
        
        String asyncScript = new String(Files.readAllBytes(new File("./examples/sleep-async.json").toPath()));      
        startTime = System.currentTimeMillis();
        result = runtime.eval(asyncScript, input);
        endTime = System.currentTimeMillis();
        
        System.out.println(result);       
        System.out.println("(With Async) Total time: " + (endTime - startTime) + " msec.");
    }
        
    @Test
    public void testAsyncMultipleBlocks() throws Exception {        
        ObjectNode input = runtime.newObjectNode();
        
        String script = new String(Files.readAllBytes(new File("./examples/async-multiple-blocks.json").toPath()));         
        JsonNode result = runtime.eval(script, input);
        System.out.println(result);
        assertEquals("{\"z\":\"test\"}", result.toString());
    }
    
    
    @Test
    public void testAsyncMultipleBlocks2() throws Exception {        
        ObjectNode input = runtime.newObjectNode();
        
        String script = new String(Files.readAllBytes(new File("./examples/async-multiple-blocks2.json").toPath()));         
        JsonNode result = runtime.eval(script, input);
        System.out.println(result);
        assertEquals("{\"z\":\"testbar\"}", result.toString());
    }
}
