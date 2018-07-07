/*
 * see license.txt
 */
package jslt2;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Parser;

import static org.junit.Assert.*;

import java.util.concurrent.SynchronousQueue;

/**
 * @author Tony
 *
 */
public class Jslt2Test {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() {
        Jslt2 runtime = new Jslt2();
        ObjectNode input = new ObjectNode(runtime.getObjectMapper().getNodeFactory());
        input.set("name", TextNode.valueOf("tony"));
        
        JsonNode result = runtime.eval("{ \"x\": .name }", input);
        System.out.println(result);
        assertEquals("{\"x\":\"tony\"}", result.toString());
    }

    @Test
    public void testIf() {
        Jslt2 runtime = new Jslt2();
        ObjectNode input = new ObjectNode(runtime.getObjectMapper().getNodeFactory());
        input.set("name", TextNode.valueOf("tony"));
        
        JsonNode result = runtime.eval("{ \"x\": if (.name = \"tony\") true else false }", input);
        System.out.println(result);        
        assertEquals("{\"x\":true}", result.toString());
        
        result = runtime.eval("{ \"x\": if (.name = \"x\") true else false }", input);
        System.out.println(result);        
        assertEquals("{\"x\":false}", result.toString());
    }
    
    @Test
    public void testDef() {
        Jslt2 runtime = new Jslt2();
        ObjectNode input = new ObjectNode(runtime.getObjectMapper().getNodeFactory());
        input.set("name", TextNode.valueOf("tony"));
        
        JsonNode result = runtime.eval("def name(x) \"yy\" { \"x\": name(.) }", input);
        System.out.println(result);        
        assertEquals("{\"x\":\"yy\"}", result.toString());
        

        result = runtime.eval("def name(x) $x { \"x\": name(.) }", input);
        System.out.println(result);        
        assertEquals("{\"x\":{\"name\":\"tony\"}}", result.toString());
        
    }
    

    @Test
    public void testLet() {
        Jslt2 runtime = new Jslt2();
        ObjectNode input = new ObjectNode(runtime.getObjectMapper().getNodeFactory());
        input.set("name", TextNode.valueOf("tony"));
        
        JsonNode result = runtime.eval("let name = \"yy\" { \"x\": $name }", input);
        System.out.println(result);        
        assertEquals("{\"x\":\"yy\"}", result.toString());
    }
    
    @Test
    public void testObjectFor() {
        Jslt2 runtime = new Jslt2();
        ObjectNode input = new ObjectNode(runtime.getObjectMapper().getNodeFactory());
        input.set("name", TextNode.valueOf("tony"));
        
        final String query = "let t = {\"one\":1, \"two\": 2, \"three\": 3 } {for ($t) .key : .}";
        
        JsonNode result = runtime.eval(query, input);
        System.out.println("Mine: " + result);        
        assertEquals("{\"one\":{\"key\":\"three\",\"value\":3.0},\"two\":{\"key\":\"three\",\"value\":3.0},\"three\":{\"key\":\"three\",\"value\":3.0}}", result.toString());
        
        Expression jslt = Parser.compileString(query);
        JsonNode jsltResult = jslt.apply(input);
        System.out.println("Jslt: " + jsltResult);
        assertEquals(jsltResult, result);
    }
    
    @Test
    public void testArrayFor() {
        Jslt2 runtime = new Jslt2();
        ObjectNode input = new ObjectNode(runtime.getObjectMapper().getNodeFactory());
        input.set("name", TextNode.valueOf("tony"));
        
        JsonNode result = runtime.eval("let t = [1,2,3] [for ($t) . + 2]", input);
        System.out.println(result);        
        assertEquals("[3.0,4.0,5.0]", result.toString());
    }
    

    @Test
    public void testArrayForWithLets() {
        Jslt2 runtime = new Jslt2();
        ObjectNode input = new ObjectNode(runtime.getObjectMapper().getNodeFactory());
        input.set("name", TextNode.valueOf("tony"));
        
        JsonNode result = runtime.eval("let t = [1,2,3] [for ($t) let i = . let x = 10 $i + 2]", input);
        System.out.println(result);        
        assertEquals("[3.0,4.0,5.0]", result.toString());
    }
    
    @Test
    public void testJslt() {
        ObjectNode input = new ObjectNode(new ObjectMapper().getNodeFactory());
        input.set("name", TextNode.valueOf("tony"));
        
        Expression jslt = Parser.compileString("let t = [1,2,3] [for ($t) . + 2]");
        JsonNode result = jslt.apply(input);
        System.out.println(result);
    }
    
    @Test
    public void testJslt2() {
        ObjectNode input = new ObjectNode(new ObjectMapper().getNodeFactory());
        input.set("name", TextNode.valueOf("tony"));
        
        Expression jslt = Parser.compileString("let t = {\"one\":1, \"two\": 2, \"three\": 3 } {for ($t) .key : .}");
        JsonNode result = jslt.apply(input);
        System.out.println(result);
    }
}
