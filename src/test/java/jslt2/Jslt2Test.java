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

import static org.junit.Assert.*;

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
}
