/*
 * see license.txt
 */
package jslt2;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import static org.junit.Assert.*;

import jslt2.vm.Bytecode;
import jslt2.vm.VM;
import jslt2.vm.compiler.BytecodeEmitter;
import jslt2.vm.compiler.EmitterScope.ScopeType;

/**
 * @author Tony
 *
 */
public class VMTest {

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

    @Test
    public void test() {
        Jslt2 runtime = new Jslt2(new ObjectMapper());
        
        JsonNode input = NullNode.instance;
        
        BytecodeEmitter em = new BytecodeEmitter();
        em.start(ScopeType.GLOBAL_SCOPE, 0);
            em.newobj();
            em.addAndloadconst("tony");
            em.addfieldc("name");
            em.sealobj();
            
            em.funcdef(1);
                em.loadlocal(0);
            em.end();
            
            em.invoke(1, 0);
            
        em.end();
        
        Bytecode code = em.compile();
        
        VM vm = new VM(runtime);
        JsonNode result = vm.execute(code, input);
        System.out.println(result);
    }

    @Test
    public void testGetField() {
        Jslt2 runtime = new Jslt2();
        ObjectNode input = new ObjectNode(runtime.getObjectMapper().getNodeFactory());
        input.set("name", TextNode.valueOf("tony"));
        
        ObjectNode person = new ObjectNode(runtime.getObjectMapper().getNodeFactory());
        person.set("first", TextNode.valueOf("anthony"));
        person.set("last", TextNode.valueOf("sparks"));
        input.set("person", person);
        
        BytecodeEmitter em = new BytecodeEmitter();
        em.start(ScopeType.GLOBAL_SCOPE, 0);
            em.newobj();
            em.loadinput();
            em.getk("name");
            em.addfieldc("name");
            
            em.loadinput();
            em.getk("person");
            em.dup();
            em.getk("first");
            em.addfieldc("first");
            
            em.dup();
            em.getk("last");
            em.addfieldc("last");
            
            //em.dup();
            em.getk("age");
            em.addfieldc("age");
            em.sealobj();                        
        em.end();
        
        Bytecode code = em.compile();
        
        VM vm = new VM(runtime);
        JsonNode result = vm.execute(code, input);
        System.out.println(result);
    }
    
    
    @Test
    public void testIf() {
        Jslt2 runtime = new Jslt2();
        ObjectNode input = new ObjectNode(runtime.getObjectMapper().getNodeFactory());
        input.set("name", TextNode.valueOf("tony"));
                
        BytecodeEmitter em = new BytecodeEmitter();
        em.start(ScopeType.GLOBAL_SCOPE, 0);
            em.newobj();
            em.loadinput();
            em.getk("name");
            em.dup();
            em.addAndloadconst("favre");
            em.ifeq("endif");
            em.pop();
            em.label("endif");
            em.addfieldc("name");            
            em.sealobj();                        
        em.end();
        
        Bytecode code = em.compile();
        
        VM vm = new VM(runtime);
        JsonNode result = vm.execute(code, input);
        System.out.println(result);
    }
}
