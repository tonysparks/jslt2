/*
 * see license.txt
 */
package jslt2;

import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
        
        
        
        // String
        
        runtime.addFunction("is-string", (input, arguments) -> {            
            return Jslt2Util.toJson(arguments[0].isTextual());
        });
        runtime.addFunction("string", (input, arguments) -> {            
            if (arguments[0].isTextual()) {
                return arguments[0];
            }
            
            return TextNode.valueOf(arguments[0].toString());
        });
        runtime.addFunction("test", (input, arguments) -> {
            // if data is missing then it doesn't match, end of story
            if (arguments[0].isNull()) {
                return BooleanNode.FALSE;
            }

            String string = Jslt2Util.toString(arguments[0], false);
            String regexp = Jslt2Util.toString(arguments[1], true);
            if (regexp == null)
                throw new Jslt2Exception("test() can't test null regexp");

            Pattern p = Pattern.compile(regexp);
             
            java.util.regex.Matcher m = p.matcher(string);
            return Jslt2Util.toJson(m.find(0));
        });
        runtime.addFunction("capture", (input, arguments) -> {
            // if data is missing then it doesn't match, end of story
            if (arguments[0].isNull()) {
                return arguments[0]; // null
            }
            
            String string = Jslt2Util.toString(arguments[0], false);
            String regexps = Jslt2Util.toString(arguments[1], true);
            if (regexps == null) {
                throw new Jslt2Exception("capture() can't match against null regexp");
            }
            
            JstlPattern regex = new JstlPattern(regexps);
             
            ObjectNode node = runtime.newObjectNode();
            
            Matcher m = regex.matcher(string);
            if (m.find()) {
                for (String group : regex.getGroups()) {
                    try {
                        node.put(group, m.group(group));
                    } catch (IllegalStateException e) {
                        // this group had no match: do nothing
                    }
                }
            }

            return node;
        });
        runtime.addFunction("split", (input, arguments) -> {
            // if input string is missing then we're doing nothing
            if (arguments[0].isNull()) {
                return arguments[0]; // null
            }
            
            String string = Jslt2Util.toString(arguments[0], false);
            String split = Jslt2Util.toString(arguments[1], true);
            if (split == null) {
                throw new Jslt2Exception("split() can't split on null");
            }
            
            return Jslt2Util.toJson(runtime, string.split(split));
        });
        runtime.addFunction("join", (input, arguments) -> {
            ArrayNode array = Jslt2Util.toArray(arguments[0], true);
            if (array == null) {
                return NullNode.instance;
            }
            
            String sep = Jslt2Util.toString(arguments[1], false);

            StringBuilder buf = new StringBuilder();
            for (int ix = 0; ix < array.size(); ix++) {
                if (ix > 0) {
                    buf.append(sep);
                }
                
                buf.append(Jslt2Util.toString(array.get(ix), false));
            }
            return new TextNode(buf.toString());
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

    // from https://stackoverflow.com/a/15588989/5974641
    private static class JstlPattern {
        private Pattern pattern;
        private Set<String> groups;

        public JstlPattern(String regexp) {
            this.pattern = Pattern.compile(regexp);
            this.groups = getNamedGroups(regexp);
        }

        public Matcher matcher(String input) {
            return pattern.matcher(input);
        }

        public Set<String> getGroups() {
            return groups;
        }

        private static Pattern extractor = Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>");

        private static Set<String> getNamedGroups(String regex) {
            Set<String> groups = new TreeSet<String>();

            Matcher m = extractor.matcher(regex);
            while (m.find())
                groups.add(m.group(1));

            return groups;
        }
    }
    
}
