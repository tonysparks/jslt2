/*
 * see license.txt
 */
package jslt2;

import java.io.File;
import java.io.FileReader;

import jslt2.ast.ProgramExpr;
import jslt2.parser.Parser;
import jslt2.parser.Scanner;
import jslt2.parser.Source;

/**
 * @author Tony
 *
 */
public class Main {

    /**
     * 
     */
    public Main() {
        // TODO Auto-generated constructor stub
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {        
        Source source = new Source(new FileReader(new File(args[0])));
        Scanner scanner = new Scanner(source);
        Parser parser = new Parser(scanner);
        ProgramExpr program = parser.parseProgram();
        program.visit(new PrettyPrinter());
    }

}
