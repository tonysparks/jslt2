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
        Expression jslt = Parser.compileString(query);

        JsonNode jsltResult = jslt.apply(input);
        JsonNode result = runtime.eval(query, input);
        
        System.out.println("Mine: " + result);
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
        
        testAgainstSpec(input, "let t = {\"one\":\"1\", \"two\": \"2\", \"three\": \"3\" } {for ($t) .key : .}");
    }
    
    @Test
    public void testObject() {
        ObjectNode input = runtime.newObjectNode();
        input.set("name", TextNode.valueOf("tony"));
        input.set("team", TextNode.valueOf("packers"));
        
        testAgainstSpec(input, "{ let t = {\"one\":\"1\", \"two\": \"2\", \"three\": \"3\" } \"x\" : .name, \"y\": .team }");
    }
    
    
    @Test
    public void testEmbeddedObject() {
        ObjectNode input = runtime.newObjectNode();
        input.set("name", TextNode.valueOf("tony"));
        input.set("team", TextNode.valueOf("packers"));
        
        testAgainstSpec(input, "{ let t = {\"one\":\"1\", \"two\": \"2\", \"three\": \"3\" } \"x\" : .name, \"y\": .team, \"z\": { for ($t) .key : .value }}");
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
    public void testArraySlice() {
        ObjectNode input = runtime.newObjectNode();
        input.set("name", TextNode.valueOf("tony"));
        
        testAgainstSpec(input, "let t = [1.0,2.0,3.0] [for ($t[1:]) . + 20]");
        
        //testAgainstSpec(input, "let a = 10 let b = $a [0]"); ?? jslt doesn't allow reference other vars in global scope
        //testAgainstSpec(input, "let x = 10 def f() ($x) [f(), f(), 20, $x]");    
        
        
        testAgainstSpec(input, "let x = [0] [1]");
        //testAgainstSpec(input, "let x = [0,1] [$x[0:-1]]");
        testAgainstSpec(input, "def x() [2] \n x()[0]");
        testAgainstSpec(input, "let x = [2] [$x[0]]");
        //testAgainstSpec(input, "let x = [0] [$x[0:1]]");
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
    public void testFuncDefWithArrayDef() {
        ObjectNode input = runtime.newObjectNode();
        input.set("name", TextNode.valueOf("tony"));
        
        testAgainstSpec(input, "let x = 10 def f() 10 [f(), f(), 20, $x]");        
    }
    
    @Test
    public void testJslt() {
        ObjectNode input = new ObjectNode(new ObjectMapper().getNodeFactory());
        input.set("name", TextNode.valueOf("tony"));
        
        Expression jslt = Parser.compileString("let x = [2] [$x[0]]");
        JsonNode result = jslt.apply(input);
        System.out.println(result);
    }
}
