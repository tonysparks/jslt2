/*
 * see license.txt 
 */
package jslt2;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.JsltException;


import static org.junit.Assert.*;

import jslt2.parser.ParseException;

/**
 * @author Tony
 *
 */
public class TestBase {

    static ObjectMapper mapper = new ObjectMapper();

    Map<String, JsonNode> makeVars(String var, String val) {
        try {
            Map<String, JsonNode> map = new HashMap<>();
            map.put(var, mapper.readTree(val));
            return map;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void check(String input, String query, String result) {
        check(input, query, result, Collections.emptyMap(), Collections.emptySet());
    }

    void check(String input, String query, String result, Map<String, JsonNode> variables) {
        check(input, query, result, variables, Collections.emptySet());
    }

    void check(String input, String query, String result, Map<String, JsonNode> variables, Collection<Jslt2Function> functions) {
        try {
            JsonNode context = mapper.readTree(input);

            try {
                Expression expr2 = com.schibsted.spt.data.jslt.Parser.compileString(query);
                JsonNode actual2 = expr2.apply(variables, context);
                System.out.println("AST: " + actual2);
            }
            catch(Exception e) {
                System.out.println("AST Error: " + e);
            }
            Jslt2 runtime = Jslt2.builder()
                                    .objectMapper(mapper)
                                    .build()
                                    .addFunctions(functions);
            
            JsonNode actual = runtime.eval(query, context);
            System.out.println("VM: " + actual);
            if (actual == null) {
                //throw new JsltException("Returned Java null");
                JsonNode expected = mapper.readTree(result);
                
                assertEquals("actual class 'null', expected class " + expected.getClass(), expected, actual);
            }
            else {
                // reparse to handle IntNode(2) != LongNode(2)
                actual = mapper.readTree(mapper.writeValueAsString(actual));
    
                JsonNode expected = mapper.readTree(result);
    
                assertEquals("actual class " + actual.getClass() + ", expected class " + expected.getClass(), expected, actual);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    JsonNode execute(String input, String query) {
        try {
            JsonNode context = mapper.readTree(input);
            Jslt2 runtime = new Jslt2();
            return runtime.eval(query, context);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // result must be contained in the error message
    void error(String query, String result) {
        error("{}", query, result);
    }

    // result must be contained in the error message
    void error(String input, String query, String result) {
        try {
            JsonNode context = mapper.readTree(input);

            Jslt2 runtime = new Jslt2();
            JsonNode r = runtime.eval(query, context);
            System.out.println("Should not have output: " + r);
            fail("JSTL did not detect error");
        } catch (ParseException|JsltException|Jslt2Exception e) {
            assertTrue("incorrect error message: '" + e.getMessage() + "'", e.getMessage().indexOf(result) != -1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
