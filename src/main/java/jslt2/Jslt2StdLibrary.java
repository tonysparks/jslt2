/*
 * see license.txt
 */
package jslt2;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;

import jslt2.util.Jslt2Util;

/**
 * @author Tony
 *
 */
public class Jslt2StdLibrary {
    static Set<String> zonenames = new HashSet<>();
    static {
      zonenames.addAll(Arrays.asList(TimeZone.getAvailableIDs()));
    }
    
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
        runtime.addFunction("size", (input, arguments) -> {
            if (arguments[0].isArray() || arguments[0].isObject()) {
                return IntNode.valueOf(arguments[0].size());
            }

            else if (arguments[0].isTextual()) {
                return IntNode.valueOf(arguments[0].asText().length());
            }
            else if (arguments[0].isNull()) {
                return arguments[0];
            }
            
            throw new Jslt2Exception("Function size() cannot work on " + arguments[0]);
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
            return TextNode.valueOf(buf.toString());
        });
        runtime.addFunction("lowercase", (input, arguments) -> {
            // if input string is missing then we're doing nothing
            if (arguments[0].isNull()) {
                return arguments[0]; // null
            }
            
            String string = Jslt2Util.toString(arguments[0], false);
            return TextNode.valueOf(string.toLowerCase());
        });
        runtime.addFunction("uppercase", (input, arguments) -> {
            // if input string is missing then we're doing nothing
            if (arguments[0].isNull()) {
                return arguments[0]; // null
            }
            
            String string = Jslt2Util.toString(arguments[0], false);
            return TextNode.valueOf(string.toUpperCase());
        });
        runtime.addFunction("starts-with", (input, arguments) -> {
            String string = Jslt2Util.toString(arguments[0], false);
            String suffix = Jslt2Util.toString(arguments[1], false);
            return Jslt2Util.toJson(string.startsWith(suffix));
        });
        runtime.addFunction("ends-with", (input, arguments) -> {
            String string = Jslt2Util.toString(arguments[0], false);
            String suffix = Jslt2Util.toString(arguments[1], false);
            return Jslt2Util.toJson(string.endsWith(suffix));
        });
        runtime.addFunction("from-json", (input, arguments) -> {
            String json = Jslt2Util.toString(arguments[0], true);
            if (json == null) {
                return NullNode.instance;
            }

            try {
                JsonNode parsed = runtime.getObjectMapper().readTree(json);
                // if input is "", for example
                if (parsed == null) { 
                    return NullNode.instance;
                }
                
                return parsed;
            } 
            catch (Exception e) {
                if (arguments.length == 2) {
                    return arguments[1]; // return fallback on parse fail
                }
                else {
                    throw new Jslt2Exception("from-json can't parse " + json + ": " + e);
                }
            }
        });
        runtime.addFunction("to-json", (input, arguments) -> {
            try {
                String json = runtime.getObjectMapper().writeValueAsString(arguments[0]);
                return new TextNode(json);
            } 
            catch (Exception e) {
                throw new Jslt2Exception("to-json can't serialize " + arguments[0] + ": " + e);
            }
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
        runtime.addFunction("get-key", (input, arguments) -> {
            String key = Jslt2Util.toString(arguments[1], true);
            if (key == null) {
                return NullNode.instance;
            }
            
            JsonNode obj = arguments[0];
            if (obj.isObject()) {
                JsonNode value = obj.get(key);
                if (value == null) {
                    if (arguments.length == 2) {
                        return NullNode.instance;
                    }
                    else {
                        return arguments[2]; // fallback argument
                    }
                }
                
                return value;                
            } 
            else if (obj.isNull()) {
                return NullNode.instance;
            }
            
            throw new Jslt2Exception("get-key: can't look up keys in " + obj);
        });
        
        // Array
        runtime.addFunction("array", (input, arguments) -> {            
            JsonNode value = arguments[0];
            if (value.isNull() || value.isArray()) {
                return value;
            } 
            else if (value.isObject()) {
                return Jslt2Util.convertObjectToArray(runtime, value);
            }

            throw new Jslt2Exception("array() cannot convert " + value);
        });
        runtime.addFunction("is-array", (input, arguments) -> {            
            return Jslt2Util.toJson(arguments[0].isArray());
        });
        runtime.addFunction("flatten", (input, arguments) -> {            
            JsonNode value = arguments[0];
            if (value.isNull()) {
                return value;
            } 
            else if (!value.isArray()) {
                throw new Jslt2Exception("flatten() cannot operate on " + value);
            }

            ArrayNode array = runtime.newArrayNode(value.size());
            flatten(array, value);
            return array;
        });
        
        // Time
        runtime.addFunction("now", (input, arguments) -> {            
            long ms = System.currentTimeMillis();
            return Jslt2Util.toJson(ms / 1000.0);
        });
        runtime.addFunction("parse-time", (input, arguments) -> {            
            String text = Jslt2Util.toString(arguments[0], true);
            if (text == null) {
                return NullNode.instance;
            }
            
            String formatstr = Jslt2Util.toString(arguments[1], false);
            JsonNode fallback = null;
            if (arguments.length > 2) {
                fallback = arguments[2];
            }
            
            // the performance of this could be better, but it's not so easy
            // to fix that when SimpleDateFormat isn't thread-safe, so we
            // can't safely share it between threads

            try {
                SimpleDateFormat format = new SimpleDateFormat(formatstr);
                format.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"));
                Date time = format.parse(text);
                return Jslt2Util.toJson((double) (time.getTime() / 1000.0));
            } 
            catch (IllegalArgumentException e) {
                // thrown if format is bad
                throw new Jslt2Exception("parse-time: Couldn't parse format '" + formatstr + "': " + e.getMessage());
            } 
            catch (ParseException e) {
                if (fallback == null) {
                    throw new Jslt2Exception("parse-time: " + e.getMessage());
                }
                
                return fallback;
            }
        });
        runtime.addFunction("format-time", (input, arguments) -> {            
            JsonNode number = Jslt2Util.number(arguments[0], false, null);
            if (number == null || number.isNull()) {
                return NullNode.instance;
            }

            double timestamp = number.asDouble();

            String formatstr = Jslt2Util.toString(arguments[1], false);

            TimeZone zone = new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC");
            if (arguments.length == 3) {
                String zonename = Jslt2Util.toString(arguments[2], false);
                if (!zonenames.contains(zonename)) {
                    throw new Jslt2Exception("format-time: Unknown timezone " + zonename);
                }
                
                zone = TimeZone.getTimeZone(zonename);
            }

            // the performance of this could be better, but it's not so easy
            // to fix that when SimpleDateFormat isn't thread-safe, so we
            // can't safely share it between threads

            try {
                SimpleDateFormat format = new SimpleDateFormat(formatstr);
                format.setTimeZone(zone);
                String formatted = format.format(Math.round(timestamp * 1000));
                
                return new TextNode(formatted);
            } 
            catch (IllegalArgumentException e) {
                // thrown if format is bad
                throw new Jslt2Exception("format-time: Couldn't parse format '" + formatstr + "': " + e.getMessage());
            }
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
        
        
        // Macro
        
        runtime.addMacro("fallback", (vm, input, args) -> {
            
            for(int i = 0; i < args.size(); i++) {
                JsonNode value = vm.execute(args.get(i), input);
                if(Jslt2Util.isValue(value)) {
                    return value;
                }
            }
            
            return NullNode.instance;
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
    
    private static void flatten(ArrayNode array, JsonNode current) {
        for (int ix = 0; ix < current.size(); ix++) {
            JsonNode node = current.get(ix);
            if (node.isArray()) {
                flatten(array, node);
            }
            else {
                array.add(node);
            }
        }
    }
}
