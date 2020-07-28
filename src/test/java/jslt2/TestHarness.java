/*
 * see license.txt
 */
package jslt2;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import org.hjson.JsonValue;
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
        public String error;
        public Map<String, JsonNode> variables;
    }
    
    @Ignore
    private TestSuite loadTestSuite(String filename) throws Exception {
        InputStream iStream = TestHarness.class.getResourceAsStream(filename);
        String json = JsonValue.readHjson(new InputStreamReader(iStream)).toString();
        TestSuite suite = mapper.readValue(json, TestSuite.class);
        
        return suite;
    }

    @Ignore
    private void executeTests(String filename) throws Exception {
        TestSuite suite = loadTestSuite(filename);
        
        System.out.println("Running: " + suite.description);
        
        for(TestCase test : suite.tests) {
            System.out.println("Test: " + test.query);
            if(test.variables != null) {
                check(test.input, test.query, test.output, test.variables);
            }
            else {
                check(test.input, test.query, test.output);
            }
        }
    }
    
    @Ignore
    private void executeFailTests(String filename) throws Exception {
        TestSuite suite = loadTestSuite(filename);
        
        System.out.println("Running: " + suite.description);
        
        for(TestCase test : suite.tests) {
            System.out.println("Test: " + test.query);            
            error(test.input, test.query, test.error);            
        }
    }
    
    @Test
    public void queryErrorTests() throws Exception {
        executeFailTests("/query-error-tests.json");
    }
    
    
    @Test
    public void queryTests() throws Exception {
        executeTests("/query-tests.json");
    }
    
    @Test
    public void simpleTest() throws Exception {
        executeTests("/simple-test.json");
    }
    
    
    @Test
    public void forIndexTests() throws Exception {
        executeTests("/for-index-tests.json");
    }
}
