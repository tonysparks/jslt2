/*
 * see license.txt
 */
package jslt2;

import java.io.InputStream;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;


/**
 * @author Tony
 *
 */
public class TestHarness extends TestBase {

    static class TestSuite {
        public String description;
        public TestCase[] tests;
    }
    
    static class TestCase {
        public String input;
        public String output;
        public String query;
        public Map<String, JsonNode> variables;
    }

    @Ignore
    private void executeTests(String filename) throws Exception {
        InputStream iStream = TestHarness.class.getResourceAsStream(filename);
        TestSuite suite = mapper.readValue(iStream, TestSuite.class);
        
        System.out.println("Running: " + suite.description);
        
        for(TestCase test : suite.tests) {
            if(test.variables != null) {
                check(test.input, test.query, test.output, test.variables);
            }
            else {
                check(test.input, test.query, test.output);
            }
        }
    }
    
    @Test
    public void queryTests() throws Exception {
        executeTests("/query-tests.json");
    }
    
    @Test
    public void simpleTest() throws Exception {
        executeTests("/simple-test.json");
    }
}
