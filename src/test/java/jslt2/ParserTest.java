/*
 * see license.txt
 */
package jslt2;

import java.io.StringReader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import jslt2.ast.Node;
import jslt2.ast.ProgramExpr;
import jslt2.parser.Parser;
import jslt2.parser.Scanner;
import jslt2.parser.Source;

/**
 * @author Tony
 *
 */
public class ParserTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }
    
    private ProgramExpr parse(String script) {
        Source source = new Source(new StringReader(script));
        Scanner scanner = new Scanner(source);
        return new Parser(scanner).parseProgram();
    }
    
    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testImport() {
        ProgramExpr prog = parse("import x");
        assertEquals(1, prog.getImports().size());
        assertEquals("x", prog.getImports().get(0).getLibrary());
        
        
        prog = parse("import z as y");
        assertEquals(1, prog.getImports().size());
        assertEquals("z", prog.getImports().get(0).getLibrary());
        assertEquals("y", prog.getImports().get(0).getAlias());
    }

}
