/*
 * see license.txt
 */
package jslt2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;

import jslt2.util.Jslt2Util;

/**
 * @author Tony
 *
 */
public class Jslt2StdLibrary {

    /**
     * 
     */
    public Jslt2StdLibrary(Jslt2 runtime) {
        runtime.addFunction("size", (input, args) -> {
            if(args == null || args.length < 1) return IntNode.valueOf(0);
            JsonNode node = args[0];
            
            int result = 0;
            if(node.isNull()) {
                return NullNode.instance;
            }
            else if(node.isTextual()) {
                result = node.asText().length();
            }
            else {
                result = node.size();
            }
            
            return IntNode.valueOf(result);
        });
        
        runtime.addFunction("not", (input, args) -> {
            if(args == null || args.length < 1) return BooleanNode.TRUE;
            JsonNode node = args[0];
            
            return BooleanNode.valueOf(!Jslt2Util.isTrue(node));
        });
        
        
        runtime.addFunction("print", (input, args) -> {
            if(args == null || args.length < 1) return NullNode.instance;
            JsonNode node = args[0];
            
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < args.length; i++) {
                if(i > 0) sb.append(" ");
                sb.append(args[i]);
            }
            
            System.out.println(sb);
            
            return node;
        });
    }

}
