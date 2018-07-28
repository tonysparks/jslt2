/*
 * see license.txt
 */
package jslt2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Function;
import com.schibsted.spt.data.jslt.JsltException;
import com.schibsted.spt.data.jslt.Parser;
import com.schibsted.spt.data.jslt.ResourceResolver;

import static org.junit.Assert.*;

/**
 * @author Tony
 *
 */
public class Jslt2Test {

    private Jslt2 runtime = Jslt2.builder()
            .resourceResolver(ResourceResolvers.newFilePathResolver(new File("./examples")))
            .build();
    
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
        File baseDir = new File("./examples");
        
        Expression jslt = new Parser(new StringReader(query))
                .withResourceResolver(new ResourceResolver() {
                    
                    @Override
                    public Reader resolve(String jsltFile) {
                        File file = new File(baseDir, jsltFile);
                        if(!file.exists()) {
                            throw new JsltException("Could not find: '" + jsltFile + "' in '" + file + "'.");
                        }
                        
                        try {
                            return new BufferedReader(new FileReader(file));
                        }
                        catch(Exception e) {
                            throw new JsltException("Unable to load '" + jsltFile + "'", e);
                        }
                    }
                })
                .compile();
        
        JsonNode jsltResult = jslt.apply(input);
        JsonNode result = runtime.eval(query, input);
        
        System.out.println("Mine: " + result);
        System.out.println("Jslt: " + jsltResult);
        
        assertEquals(jsltResult, result);
    }
    
    @Test
    public void testImport() throws Exception {
        ObjectNode input = runtime.newObjectNode();
        input.set("name", TextNode.valueOf("tony"));
        
        String query = new String(Files.readAllBytes(new File("./examples/import-test.json").toPath()));
        
        testAgainstSpec(input, query);        
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
    public void testAnd() {
        ObjectNode input = runtime.newObjectNode();
        input.set("name", TextNode.valueOf("tony"));
        
        testAgainstSpec(input, "{ \"x\": if (.name == \"tony\" and .name != \"t\") true else false }");
        testAgainstSpec(input, "{ \"x\": if ((.name == \"x\" or .name == \"y\") and false) true else false }");        
    }
    
    @Test
    public void testOr() {
        ObjectNode input = runtime.newObjectNode();
        input.set("name", TextNode.valueOf("tony"));
        
        testAgainstSpec(input, "{ \"x\": if (.name == \"tony\" or .name != \"t\") true else false }");
        testAgainstSpec(input, "{ \"x\": if ((.name == \"x\" and .name == \"y\") or true) true else false }");        
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
    public void testDot() {
        ObjectNode input = runtime.newObjectNode();
        input.set("name", TextNode.valueOf("tony"));
        
        ArrayNode array = runtime.newArrayNode(12);
        for(int i = 0; i < 10; i++) {
            array.add(i);
        }
        
        input.set("array", array);
        
        testAgainstSpec(input, "{ \".name\" : .\"name\" } ");
        testAgainstSpec(input, "{ \".name\" : .name } ");
        testAgainstSpec(input, "{ \"array\" : .array[0] } ");    
    }
    
    @Test
    public void testDotArray() {        
        ArrayNode input = runtime.newArrayNode(12);
        for(int i = 0; i < 10; i++) {
            input.add(i);
        }
                        
        testAgainstSpec(input, "{ \"array\" : .[0] } ");
        testAgainstSpec(input, "{ \"array\" : . } ");
        testAgainstSpec(input, "{ \"array\" : [for (.) . + 1] }");
    }
    
    @Test
    public void testJslt() {
        ObjectNode input = new ObjectNode(new ObjectMapper().getNodeFactory());
        input.set("name", TextNode.valueOf("tony"));
        
        Expression jslt = Parser.compileString("let x = [2] [$x[0]]");
        JsonNode result = jslt.apply(input);
        System.out.println(result);
    }
    
    @Test
    public void testQueens() throws Exception {
        ObjectNode input = runtime.newObjectNode();
        input.set("name", TextNode.valueOf("tony"));
        input.set("team", TextNode.valueOf("packers"));
        
        testAgainstSpec(input, new String(Files.readAllBytes(new File("./examples/queens.json").toPath())));
    }
    
    @Test
    public void testJsltByFile() throws Exception {
        ObjectNode input = new ObjectNode(new ObjectMapper().getNodeFactory());
        input.set("name", TextNode.valueOf("tony"));
        
        Function print = new Function() {
            
            @Override
            public String getName() {
                return "print";
            }
            
            @Override
            public int getMinArguments() {
                return 1;
            }
            
            @Override
            public int getMaxArguments() {
                return 10;
            }
            
            @Override
            public JsonNode call(JsonNode input, JsonNode[] args) {
                if(args == null || args.length < 1) return NullNode.instance;
                JsonNode node = args[0];
                
                StringBuilder sb = new StringBuilder();
                for(int i = 0; i < args.length; i++) {
                    if(i > 0) sb.append(" ");
                    sb.append(args[i]);
                }
                
                System.out.println(sb);
                
                return node;
            }
        };
        
        List<Function> functions = new ArrayList<>();
        functions.add(print);
        
        String script = new String(Files.readAllBytes(new File("./examples/tests.json").toPath()));
        Expression jslt = Parser.compileString(script, functions);
        JsonNode result = jslt.apply(input);
        System.out.println(result);
    }
}
