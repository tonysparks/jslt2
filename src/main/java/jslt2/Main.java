/*
 * see license.txt
 */
package jslt2;

import java.io.File;
import java.io.FileReader;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Tony
 *
 */
public class Main {
    
    
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        if(args.length == 0) {
            System.out.println("<usage> jslt2 [options] ");
        }
        
        Jslt2 runtime = new Jslt2();
        JsonNode input = runtime.getObjectMapper().readTree(new FileReader(new File(args[1])));
        
        Template template = runtime.compile(new FileReader(new File(args[0])));
        JsonNode result = template.eval(input);
        
        System.out.println(result);        
    }

}
