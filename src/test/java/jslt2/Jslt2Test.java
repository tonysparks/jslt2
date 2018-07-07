/*
 * see license.txt
 */
package jslt2;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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

    private Jslt2 runtime = new Jslt2();
    
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

    
    @Ignore
    private void testAgainstSpec(JsonNode input, String query) {
        JsonNode result = runtime.eval(query, input);
        System.out.println("Mine: " + result);        

        
        Expression jslt = Parser.compileString(query);
        JsonNode jsltResult = jslt.apply(input);
        System.out.println("Jslt: " + jsltResult);
        
        assertEquals(jsltResult, result);
    }
    
    @Test
    public void testSimple() {
        ObjectNode input = runtime.newObjectNode();
        input.set("name", TextNode.valueOf("tony"));
        
        testAgainstSpec(input, "{ \"x\": .name }");        
    }

    @Test
    public void testIf() {
        ObjectNode input = runtime.newObjectNode();
        input.set("name", TextNode.valueOf("tony"));
        
        testAgainstSpec(input, "{ \"x\": if (.name == \"tony\") true else false }");
        testAgainstSpec(input, "{ \"x\": if (.name == \"x\") true else false }");        
    }
    
    @Test
    public void testDef() {
        ObjectNode input = runtime.newObjectNode();
        input.set("name", TextNode.valueOf("tony"));
        
        testAgainstSpec(input, "def name(x) \"yy\" { \"x\": name(.) }");
        testAgainstSpec(input, "def name(x) $x { \"x\": name(.) }");
    }
    

    @Test
    public void testLet() {
        ObjectNode input = runtime.newObjectNode();
        input.set("name", TextNode.valueOf("tony"));
        
        testAgainstSpec(input, "let name = \"yy\" { \"x\": $name }");
    }
    
    @Test
    public void testObjectFor() {
        ObjectNode input = runtime.newObjectNode();
        input.set("name", TextNode.valueOf("tony"));
        
    //    testAgainstSpec(input, "let t = {\"one\":\"1\", \"two\": \"3\", \"three\": \"3\" } {for ($t) .key : .}");
    }

    
    @Test
    public void testMatcher() {
        ObjectNode input = runtime.newObjectNode();
        input.set("name", TextNode.valueOf("tony"));
        input.set("team", TextNode.valueOf("packers"));
        
        ObjectNode person = runtime.newObjectNode();
        person.set("first", TextNode.valueOf("brett"));
        person.set("last", TextNode.valueOf("favre"));
        input.set("qb", person);
        
        testAgainstSpec(input, "{ \"team\": \"Green Bay\", * - name: .}");
    }
    
    @Test
    public void testArrayFor() {
        ObjectNode input = runtime.newObjectNode();
        input.set("name", TextNode.valueOf("tony"));
        
        testAgainstSpec(input, "let t = [1.0,2.0,3.0] [for ($t) . + 2]");        
    }
    

    @Test
    public void testArrayForWithLets() {
        ObjectNode input = runtime.newObjectNode();
        input.set("name", TextNode.valueOf("tony"));
        
        testAgainstSpec(input, "let t = [1.0,2.0,3.0] [for ($t) let i = . let x = 10 $i + 2]");        
    }
    
    @Test
    public void testJslt() {
        ObjectNode input = new ObjectNode(new ObjectMapper().getNodeFactory());
        input.set("name", TextNode.valueOf("tony"));
        
        Expression jslt = Parser.compileString("let t = [1,2,3] [for ($t) . + 2]");
        JsonNode result = jslt.apply(input);
        System.out.println(result);
    }
}
