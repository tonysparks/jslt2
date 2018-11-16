/*
 * (c)2014 Expeditors International of Washington, Inc.
 * Business confidential and proprietary.  This information may not be reproduced 
 * in any form without advance written consent of an authorized officer of the 
 * copyright holder.
 *
 */
package jslt2;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.LongNode;

import static org.junit.Assert.*;

import jslt2.ast.Expr.IfExpr;
import jslt2.ast.Expr.LetExpr;
import jslt2.parser.Parser;
import jslt2.parser.Scanner;
import jslt2.parser.Source;
import jslt2.vm.Bytecode;
import jslt2.ast.Expr;
import jslt2.ast.Expr.*;

/**
 * @author chq-tonys
 *
 */
public class OptimizeTest {

    private Jslt2 runtime = Jslt2.builder().enableDebugMode(true).includeNulls(false).build();
    
    private ProgramExpr ast(String template) throws Exception {
        Source source = new Source(new StringReader(template));
        Scanner scanner = new Scanner(source);
        Parser parser = new Parser(runtime, scanner);
        
        return parser.parseProgram();
    }
    
    private <T extends Expr> T astAs(String template) throws Exception {
        ProgramExpr p = ast(template);
        return p.expr.as();
    }
    
    @Test
    public void testIf() throws Exception {
        
        //        
        Expr ifExpr = astAs("if (true) 1 else 2");                
        assertTrue(ifExpr instanceof NumberExpr);
        assertEquals(1, ((NumberExpr)ifExpr).number.asLong());
        
        ifExpr = astAs("if (false) 1 else 2");
        assertTrue(ifExpr instanceof ElseExpr);
        ElseExpr elseExpr = ifExpr.as();
        assertEquals(2, ((NumberExpr)elseExpr.expr).number.asLong());
    }

    

    @Test
    public void testBinary() throws Exception {
        
        //        
        Expr e = astAs("1 + 2");                
        assertTrue(e instanceof NumberExpr);
        assertEquals(3, ((NumberExpr)e).number.asLong());
        
        e = astAs("1 - 2");                
        assertTrue(e instanceof NumberExpr);
        assertEquals(-1, ((NumberExpr)e).number.asLong());
        
        e = astAs("2 * 2");                
        assertTrue(e instanceof NumberExpr);
        assertEquals(4, ((NumberExpr)e).number.asLong());
        
        
        e = astAs("4 / 2");                
        assertTrue(e instanceof NumberExpr);
        assertEquals(2, ((NumberExpr)e).number.asLong());
        
        
        e = astAs("4 % 2");                
        assertTrue(e instanceof NumberExpr);
        assertEquals(0, ((NumberExpr)e).number.asLong());
        
        e = astAs("\"s\" + 2");                
        assertTrue(e instanceof StringExpr);
        assertEquals("s2", ((StringExpr)e).string);
        
        e = astAs("2 + \"s\"");                
        assertTrue(e instanceof StringExpr);
        assertEquals("2s", ((StringExpr)e).string);
        
        e = astAs("\"s\" + \"2\"");                
        assertTrue(e instanceof StringExpr);
        assertEquals("s2", ((StringExpr)e).string);
        
        e = astAs("def x() true \"s\" + x()");                
        assertTrue(e instanceof BinaryExpr);
        
        
        e = astAs("4 / 2 + 3");                
        assertTrue(e instanceof NumberExpr);
        assertEquals(5, ((NumberExpr)e).number.asLong());
        
        
        e = astAs("3 * 4 / 2 + 3");                
        assertTrue(e instanceof NumberExpr);
        assertEquals(9, ((NumberExpr)e).number.asLong());
        
        
        e = astAs("5 * 4 / (2 + 3)");                
        assertTrue(e instanceof NumberExpr);
        assertEquals(4, ((NumberExpr)e).number.asLong());
        
        
        e = astAs("(5 * 4 / (2 + 3)) * 2");                
        assertTrue(e instanceof NumberExpr);
        assertEquals(8, ((NumberExpr)e).number.asLong());
        
        e = astAs("(5 * 4 / (2 + 3)) * 2 + \"s\"");                
        assertTrue(e instanceof StringExpr);
        assertEquals("8s", ((StringExpr)e).string);
        
        
        // ~~~~~~~~~~~~~~~~~~~~~
        // Test AND / OR
        // ~~~~~~~~~~~~~~~~~~~~~        
        
        e = astAs("(5 * 4 / (2 + 3)) * 2 and \"s\"");                
        assertTrue(e instanceof BooleanExpr);
        assertEquals(true, ((BooleanExpr)e).bool);
        
        
        e = astAs("(5 * 4 / (2 + 3)) * 2 and \"s\" and false");                
        assertTrue(e instanceof BooleanExpr);
        assertEquals(false, ((BooleanExpr)e).bool);
        
        
        e = astAs("true and false");                
        assertTrue(e instanceof BooleanExpr);
        assertEquals(false, ((BooleanExpr)e).bool);
        
        e = astAs("true and true");                
        assertTrue(e instanceof BooleanExpr);
        assertEquals(true, ((BooleanExpr)e).bool);
        
        e = astAs("true or false");                
        assertTrue(e instanceof BooleanExpr);
        assertEquals(true, ((BooleanExpr)e).bool);
        
        e = astAs("true or true");                
        assertTrue(e instanceof BooleanExpr);
        assertEquals(true, ((BooleanExpr)e).bool);
        
        e = astAs("false or false");                
        assertTrue(e instanceof BooleanExpr);
        assertEquals(false, ((BooleanExpr)e).bool);
        
        e = astAs("false or true");                
        assertTrue(e instanceof BooleanExpr);
        assertEquals(true, ((BooleanExpr)e).bool);
        
        e = astAs("false and false");                
        assertTrue(e instanceof BooleanExpr);
        assertEquals(false, ((BooleanExpr)e).bool);
        
        e = astAs("false and true");                
        assertTrue(e instanceof BooleanExpr);
        assertEquals(false, ((BooleanExpr)e).bool);
    }
    
    @Test
    public void testForArray() throws Exception {
        
        //        
        Expr e = astAs("[ for ([1,2,3]) . if (true) ]");                
        assertTrue(e instanceof ArrayExpr);
        ForArrayExpr forExpr = ((ArrayExpr)e).forExpr;
        assertNotNull(forExpr);
        assertNull(forExpr.ifExpr);
        
        
        e = astAs("[ for ([1,2,3]) . if (false) ]");                
        assertTrue(e instanceof ArrayExpr);
        forExpr = ((ArrayExpr)e).forExpr;
        assertNull(forExpr);
        assertEquals(0, ((ArrayExpr)e).elements.size());
    }
    
    @Test
    public void testForObject() throws Exception {
        
        //        
        Expr e = astAs("{ for ([1,2,3]) . : . if (true) }");                
        assertTrue(e instanceof ObjectExpr);
        ForObjectExpr forExpr = ((ObjectExpr)e).forObjectExpr;
        assertNotNull(forExpr);
        assertNull(forExpr.ifExpr);
        
        
        e = astAs("{ for ([1,2,3]) . : . if (false) }");                
        assertTrue(e instanceof ObjectExpr);
        forExpr = ((ObjectExpr)e).forObjectExpr;
        assertNull(forExpr);
        assertEquals(0, ((ObjectExpr)e).fields.size());
    }
    
    @Test
    public void testUnary() throws Exception {
        
        //        
        Expr e = astAs("!true");                
        assertTrue(e instanceof BooleanExpr);
        assertEquals(false, ((BooleanExpr)e).bool);
        
        e = astAs("!false");                
        assertTrue(e instanceof BooleanExpr);
        assertEquals(true, ((BooleanExpr)e).bool);  
        
        e = astAs("!(true or false)");                
        assertTrue(e instanceof BooleanExpr);
        assertEquals(false, ((BooleanExpr)e).bool);
        
        e = astAs("!2");                
        assertTrue(e instanceof BooleanExpr);
        assertEquals(false, ((BooleanExpr)e).bool);
        
        e = astAs("-2");                
        assertTrue(e instanceof NumberExpr);
        assertEquals(-2, ((NumberExpr)e).number.longValue());
        
        
        e = astAs("--2");                
        assertTrue(e instanceof NumberExpr);
        assertEquals(2, ((NumberExpr)e).number.longValue());
        
        e = astAs("-(2 + 3)");                
        assertTrue(e instanceof NumberExpr);
        assertEquals(-5, ((NumberExpr)e).number.longValue());
        
        
        e = astAs("(2 + 3) * -3");                
        assertTrue(e instanceof NumberExpr);
        assertEquals(-15, ((NumberExpr)e).number.longValue());
        
    }
}
