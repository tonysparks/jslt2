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
        Jslt2 runtime = new Jslt2();
        JsonNode input = runtime.getObjectMapper().readTree(new FileReader(new File(args[1])));
        
        Template template = runtime.compile(new FileReader(new File(args[0])));
        JsonNode result = template.eval(input);
        
        System.out.println(result);
        
//        Source source = new Source(new FileReader(new File(args[0])));
//        Scanner scanner = new Scanner(source);
//        Parser parser = new Parser(scanner);
//        ProgramExpr program = parser.parseProgram();
//        program.visit(new PrettyPrinter());
    }

}
