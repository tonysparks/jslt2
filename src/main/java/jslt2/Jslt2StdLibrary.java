/*
 * see license.txt
 */
package jslt2;

import java.util.Random;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;

import jslt2.util.Jslt2Util;

/**
 * @author Tony
 *
 */
public class Jslt2StdLibrary {
    public Jslt2StdLibrary(final Jslt2 runtime) {
        
        // General
        
        runtime.addFunction("contains", (input, arguments) -> {
            if (arguments[1].isNull())
                return BooleanNode.FALSE; // nothing is contained in null

            else if (arguments[1].isArray()) {
                for (int ix = 0; ix < arguments[1].size(); ix++)
                    if (arguments[1].get(ix).equals(arguments[0]))
                        return BooleanNode.TRUE;

            } else if (arguments[1].isObject()) {
                String key = Jslt2Util.toString(arguments[0], true);
                if (key == null)
                    return BooleanNode.FALSE;

                return Jslt2Util.toJson(arguments[1].has(key));

            } else if (arguments[1].isTextual()) {
                String sub = Jslt2Util.toString(arguments[0], true);
                if (sub == null)
                    return BooleanNode.FALSE;

                String str = arguments[1].asText();
                return Jslt2Util.toJson(str.indexOf(sub) != -1);

            } else
                throw new Jslt2Exception("Contains cannot operate on " + arguments[1]);

            return BooleanNode.FALSE;
        });
        
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
        
        runtime.addFunction("error", (input, arguments) -> {
            String msg = Jslt2Util.toString(arguments[0], false);
            throw new Jslt2Exception("error: " + msg);
        });
        
        
        
        
        // Numeric
        
        runtime.addFunction("is-number", (input, arguments) -> {
            return Jslt2Util.toJson(arguments[0].isNumber());
        });
        runtime.addFunction("number", (input, arguments) -> {
            if (arguments.length == 1) {
                return Jslt2Util.number(arguments[0], true, null);
            }
            
            return Jslt2Util.number(arguments[0], true, arguments[1]);
            
        });
        runtime.addFunction("round", (input, arguments) -> {
            JsonNode number = arguments[0];
            if (number.isNull()) {
                return NullNode.instance;
            }
            else if (!number.isNumber()) {
                throw new Jslt2Exception("round() cannot round a non-number: " + number);
            }

            return LongNode.valueOf(Math.round(number.doubleValue()));
        });
        runtime.addFunction("floor", (input, arguments) -> {
            JsonNode number = arguments[0];
            if (number.isNull())
              return NullNode.instance;
            else if (!number.isNumber())
              throw new Jslt2Exception("floor() cannot round a non-number: " + number);

            return LongNode.valueOf((long) Math.floor(number.doubleValue()));
        });
        runtime.addFunction("ceiling", (input, arguments) -> {
            JsonNode number = arguments[0];
            if (number.isNull())
              return NullNode.instance;
            else if (!number.isNumber())
              throw new Jslt2Exception("ceiling() cannot round a non-number: " + number);

            return LongNode.valueOf((long) Math.ceil(number.doubleValue()));
        });
        final Random random = new Random();
        runtime.addFunction("random", (input, arguments) -> {            
            return DoubleNode.valueOf(random.nextDouble());
        });
        
        

        // Boolean
        
        runtime.addFunction("not", (input, args) -> {
            if(args == null || args.length < 1) return BooleanNode.TRUE;
            JsonNode node = args[0];
            
            return BooleanNode.valueOf(!Jslt2Util.isTrue(node));
        });
        runtime.addFunction("boolean", (input, arguments) -> {            
            return Jslt2Util.toJson(Jslt2Util.isTrue(arguments[0]));
        });
        runtime.addFunction("is-boolean", (input, arguments) -> {            
            return Jslt2Util.toJson(arguments[0].isBoolean());
        });
        
        // Object
        
        runtime.addFunction("is-object", (input, arguments) -> {            
            return Jslt2Util.toJson(arguments[0].isObject());
        });
        
        // Array
        
        runtime.addFunction("is-array", (input, arguments) -> {            
            return Jslt2Util.toJson(arguments[0].isArray());
        });
        
        // Aux
        
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
