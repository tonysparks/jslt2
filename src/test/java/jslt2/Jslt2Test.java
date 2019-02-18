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
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Function;
import com.schibsted.spt.data.jslt.JsltException;
import com.schibsted.spt.data.jslt.Parser;
import com.schibsted.spt.data.jslt.ResourceResolver;

import static org.junit.Assert.*;

import jslt2.parser.ParseException;

/**
 * @author Tony
 *
 */
public class Jslt2Test {

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
                .withFunctions(Arrays.asList(new Function() {                    
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
                    
                    @Override
                    public int getMinArguments() {                    
                        return 0;
                    }
                    
                    @Override
                    public int getMaxArguments() {                    
                        return 1024;
                    }
                    
                    @Override
                    public String getName() {                    
                        return "print";
                    }
                }))
                .compile();
        

        
        JsonNode jsltResult = jslt.apply(input);
        JsonNode result = runtime.eval(query, input);
        
        System.out.println("Mine: " + result);
        System.out.println("Jslt: " + jsltResult);
        
        assertEquals(jsltResult, result);
    }
    
    @Test
    public void testExamples() throws Exception {
        ObjectNode input = runtime.newObjectNode();
        input.set("name", TextNode.valueOf("Brett Favre"));
        input.set("team", TextNode.valueOf("Green Bay Packers"));
        
        String query = new String(Files.readAllBytes(new File("./examples/example1.json").toPath()));
        
        testAgainstSpec(input, query);        
    }
    
    @Test
    public void testMacro() throws Exception {
        ObjectNode input = runtime.newObjectNode();
        input.set("name", TextNode.valueOf("tony"));
        
        String query = new String(Files.readAllBytes(new File("./examples/macros.json").toPath()));
        
        testAgainstSpec(input, query);        
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
        

        testAgainstSpec(input, "{ \"x\": 1 and 1 }");
        testAgainstSpec(input, "{ \"x\": (1 and .) }");
        
        testAgainstSpec(input, "{ \"x\": . and 1 }");
        testAgainstSpec(input, "{ \"x\": (. and 1) }");
        
        testAgainstSpec(input, "{ \"x\": 1 and 0 }");
        testAgainstSpec(input, "{ \"x\": (. and false) }");
    }
    
    @Test
    public void testOr() {
        ObjectNode input = runtime.newObjectNode();
        input.set("name", TextNode.valueOf("tony"));
        
        testAgainstSpec(input, "{ \"x\": if (.name == \"tony\" or .name != \"t\") true else false }");
        testAgainstSpec(input, "{ \"x\": if ((.name == \"x\" and .name == \"y\") or true) true else false }");        
    
        testAgainstSpec(input, "{ \"x\": 1 or 1 }");
        testAgainstSpec(input, "{ \"x\": (1 or .) }");
    
        testAgainstSpec(input, "{ \"x\": . or 1 }");
        testAgainstSpec(input, "{ \"x\": (. or 1) }");
        
        testAgainstSpec(input, "{ \"x\": 1 or 0 }");
        testAgainstSpec(input, "{ \"x\": (. or false) }");
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
    public void testObjectForIf() {
        Jslt2 runtime = Jslt2.builder().includeNulls(true).build();
        ObjectNode input = runtime.newObjectNode();
        input.set("name", TextNode.valueOf("tony"));
        
        //testAgainstSpec(input, "let t = {\"one\":\"1\", \"two\": \"2\", \"three\": \"3\" } {for ($t) .key : . if(.value = \"\3\") }");
        String query = "let t = {\"one\":\"1\", \"two\": \"2\", \"three\": \"3\" } {for ($t) .key : . if(.value = \"3\") }";
        String query2 = "let t = {\"one\":1, \"two\": 2, \"three\": 3 } {for ($t) .key : . if(.value = 3 or .value = 2) }";
        
        JsonNode result = runtime.eval(query, input);
        System.out.println(result);
        assertEquals("{\"three\":{\"key\":\"three\",\"value\":\"3\"}}", result.toString());
        JsonNode result2 = runtime.eval(query2, input);
        System.out.println(result2);
        assertEquals("{\"two\":{\"key\":\"two\",\"value\":2},\"three\":{\"key\":\"three\",\"value\":3}}", result2.toString());
    }
    
    @Test
    public void testArrayForIf() {
        Jslt2 runtime = Jslt2.builder().includeNulls(true).build();
        ObjectNode input = runtime.newObjectNode();
        input.set("name", TextNode.valueOf("tony"));
        
        //testAgainstSpec(input, "let t = {\"one\":\"1\", \"two\": \"2\", \"three\": \"3\" } {for ($t) .key : . if(.value = \"\3\") }");
        String query = "let t = [\"1\",\"2\",\"3\",\"4\"] [for ($t) . if(. = \"3\") ]";
        String query2 = "let t = [1,2,3,4,5] [for ($t) . if(. = 3 or . = 2) ]";
        
        JsonNode result = runtime.eval(query, input);
        System.out.println(result);
        assertEquals("[\"3\"]", result.toString());
        JsonNode result2 = runtime.eval(query2, input);
        System.out.println(result2);
        assertEquals("[2,3]", result2.toString());
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
        
        testAgainstSpec(input, "{ \"team\": \"Green Bay\", * - name: . + \"x\" }");
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
    public void testObjectConcat() throws Exception {        
        ObjectNode input = runtime.newObjectNode();
        input.set("c", new IntNode(3));
                        
        testAgainstSpec(input, new String(Files.readAllBytes(new File("./examples/concat.json").toPath())));
    }
    
    @Test(expected=ParseException.class)
    public void testIfThenBug() throws Exception {        
        ObjectNode input = runtime.newObjectNode();
        input.set("c", new IntNode(3));
        
        String script = new String(Files.readAllBytes(new File("./examples/if-then-bug.json").toPath()));
        //Expression jslt = new Parser(new StringReader(script)).compile();
        //JsonNode jsltResult = jslt.apply(input);
        
        Template template = runtime.compile(script);
        template.eval(input);
        
        //testAgainstSpec(input, new String(Files.readAllBytes(new File("./examples/if-then-bug.json").toPath())));
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
    public void testMatcher2() throws Exception {
        String query = "{ \"a\": \"b\" }\r\n" + 
                "+\r\n" + 
                "{\r\n" + 
                "  \"type\" : \"Anonymized-View\",\r\n" + 
                "  * : .\r\n" + 
                "} \r\n"  
                ;
        ObjectNode input = runtime.newObjectNode();
        input.set("name", TextNode.valueOf("tony"));
        input.set("team", TextNode.valueOf("packers"));
        
        JsonNode result = runtime.eval(query, input);
        System.out.println(result);
       // testAgainstSpec(input, query);
        assertEquals("{\"a\":\"b\",\"type\":\"Anonymized-View\",\"name\":\"tony\",\"team\":\"packers\"}", result.toString());
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
    public void testArrays() throws Exception {        
        ArrayNode input = runtime.newArrayNode(12);
        for(int i = 0; i < 10; i++) {
            input.add(i);
        }
        
        String script = new String(Files.readAllBytes(new File("./examples/for-arrays.json").toPath())); 
        JsonNode result = runtime.eval(script, input);
        System.out.println(result);
       // testAgainstSpec(input, query);        
    }
    
    @Test
    public void testClosures() throws Exception {        
        ObjectNode input = runtime.newObjectNode();
        input.set("name", TextNode.valueOf("tony"));
        input.set("team", TextNode.valueOf("packers"));
        
        String script = new String(Files.readAllBytes(new File("./examples/closures.json").toPath())); 
        JsonNode result = runtime.eval(script, input);
        System.out.println(result);

        assertEquals("{\"test\":\"ab\"}", result.toString());
    }
    
    @Test
    public void testMultStrings() throws Exception {
        ObjectNode input = new ObjectNode(new ObjectMapper().getNodeFactory());
        input.set("name", TextNode.valueOf("tony"));
                
        String script = new String(Files.readAllBytes(new File("./examples/multstrings.json").toPath()));        
        JsonNode result = runtime.eval(script, input);
        System.out.println(result);
        
        assertEquals("\n    hello\n    there\n    \"in quotes\"\n", result.asText());
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
