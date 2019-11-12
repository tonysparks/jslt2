/*
 * see license.txt
 */
package jslt2;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;

import jslt2.util.Jslt2Util;

/**
 * @see https://github.com/schibsted/jslt/blob/master/src/main/java/com/schibsted/spt/data/jslt/impl/BuiltinFunctions.java
 * 
 * Code intentially taken from original JSLT source to be as compliant as possible with original functionality.
 *
 */
public class Jslt2StdLibrary {
    static Set<String> zonenames = new HashSet<>();
    static {
      zonenames.addAll(Arrays.asList(TimeZone.getAvailableIDs()));
    }
    
    static class Jslt2FunctionValidation implements Jslt2Function {
        
        int minArgs;
        Jslt2Function func;
        
        public Jslt2FunctionValidation(int minArgs, Jslt2Function func) {
            this.minArgs = minArgs;
            this.func = func;
        }
        
        @Override
        public JsonNode execute(JsonNode input, JsonNode... args) throws Jslt2Exception {
            if(this.minArgs > -1) {
                if(args == null || args.length < this.minArgs) {
                    throw new Jslt2Exception(this.func.name() + " requires at least " + this.minArgs + " arguments");
                }
            }
                
            return this.func.execute(input, args);
        }
    }
    
    private Map<String, Pattern> regexCache;
    
    public Jslt2StdLibrary(final Jslt2 runtime) {
        this(runtime, new ConcurrentHashMap<>());
    }
    
    public Jslt2StdLibrary(final Jslt2 runtime, Map<String, Pattern> regexCache) {
        this.regexCache = regexCache;
        
        // General
        
        runtime.addFunction("contains", 2, (input, arguments) -> {
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
        runtime.addFunction("size", 1, (input, arguments) -> {
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
        
        runtime.addFunction("error", 1, (input, arguments) -> {
            String msg = Jslt2Util.toString(arguments[0], false);
            throw new Jslt2Exception("error: " + msg);
        });
        
        runtime.addFunction("min", 2, (input, arguments) -> {
            // this works because null is the smallest of all values
            if (Jslt2Util.compare(arguments[0], arguments[1]) < 0) {
                return arguments[0];
            }
            else {
              return arguments[1];
            }
        });
        
        runtime.addFunction("max", 2, (input, arguments) -> {
            if (arguments[0].isNull() || arguments[1].isNull()) {
                return NullNode.instance;
            }
            else if (Jslt2Util.compare(arguments[0], arguments[1]) > 0) {
                return arguments[0];
            }
            else {
                return arguments[1];
            }
        });
        
        
        
        // Numeric
        
        runtime.addFunction("is-number", 1, (input, arguments) -> {
            return Jslt2Util.toJson(arguments[0].isNumber());
        });
        runtime.addFunction("number", 1, (input, arguments) -> {
            if (arguments.length == 1) {
                return Jslt2Util.number(arguments[0], true, null);
            }
            
            return Jslt2Util.number(arguments[0], true, arguments[1]);
            
        });
        runtime.addFunction("round", 1, (input, arguments) -> {
            JsonNode number = arguments[0];
            if (number.isNull()) {
                return NullNode.instance;
            }
            else if (!number.isNumber()) {
                throw new Jslt2Exception("round() cannot round a non-number: " + number);
            }

            return LongNode.valueOf(Math.round(number.doubleValue()));
        });
        runtime.addFunction("floor", 1, (input, arguments) -> {
            JsonNode number = arguments[0];
            if (number.isNull())
              return NullNode.instance;
            else if (!number.isNumber())
              throw new Jslt2Exception("floor() cannot round a non-number: " + number);

            return LongNode.valueOf((long) Math.floor(number.doubleValue()));
        });
        runtime.addFunction("ceiling", 1, (input, arguments) -> {
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
        
        runtime.addFunction("is-string", 1, (input, arguments) -> {            
            return Jslt2Util.toJson(arguments[0].isTextual());
        });
        runtime.addFunction("string", 1, (input, arguments) -> {            
            if (arguments[0].isTextual()) {
                return arguments[0];
            }
            
            return TextNode.valueOf(arguments[0].toString());
        });
        runtime.addFunction("test", 2, (input, arguments) -> {
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
        runtime.addFunction("capture", 2, (input, arguments) -> {
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
        runtime.addFunction("split", 2, (input, arguments) -> {
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
        runtime.addFunction("join", 2, (input, arguments) -> {
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
        runtime.addFunction("lowercase", 1, (input, arguments) -> {
            // if input string is missing then we're doing nothing
            if (arguments[0].isNull()) {
                return arguments[0]; // null
            }
            
            String string = Jslt2Util.toString(arguments[0], false);
            return TextNode.valueOf(string.toLowerCase());
        });
        runtime.addFunction("uppercase", 1, (input, arguments) -> {
            // if input string is missing then we're doing nothing
            if (arguments[0].isNull()) {
                return arguments[0]; // null
            }
            
            String string = Jslt2Util.toString(arguments[0], false);
            return TextNode.valueOf(string.toUpperCase());
        });
        runtime.addFunction("starts-with", 2, (input, arguments) -> {
            String string = Jslt2Util.toString(arguments[0], false);
            String suffix = Jslt2Util.toString(arguments[1], false);
            return Jslt2Util.toJson(string.startsWith(suffix));
        });
        runtime.addFunction("ends-with", 2, (input, arguments) -> {
            String string = Jslt2Util.toString(arguments[0], false);
            String suffix = Jslt2Util.toString(arguments[1], false);
            return Jslt2Util.toJson(string.endsWith(suffix));
        });
        runtime.addFunction("from-json", 1, (input, arguments) -> {
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
        runtime.addFunction("to-json", 1, (input, arguments) -> {
            try {
                String json = runtime.getObjectMapper().writeValueAsString(arguments[0]);
                return new TextNode(json);
            } 
            catch (Exception e) {
                throw new Jslt2Exception("to-json can't serialize " + arguments[0] + ": " + e);
            }
        });
        
        runtime.addFunction("substr", 2, (input, arguments) -> {
            String str = arguments[0].asText();
            int startIndex = arguments[1].asInt();
            
            if(arguments.length > 2) {
                int endIndex = arguments[2].asInt();
                return new TextNode(str.substring(startIndex, endIndex));
            }
            
            return new TextNode(str.substring(startIndex));
        });

        runtime.addFunction("replace", 3, (input, arguments) -> {
            String string = Jslt2Util.toString(arguments[0], true);
            if (string == null)
              return NullNode.instance;

            String regexp = Jslt2Util.toString(arguments[1], false);
            String sep = Jslt2Util.toString(arguments[2], false);

            Pattern p = getRegexp(regexp);
            Matcher m = p.matcher(string);
            char[] buf = new char[string.length() * Math.max(sep.length(), 1)];
            int pos = 0; // next untouched character in input
            int bufix = 0; // next unwritten character in buf

            while (m.find(pos)) {
              // we found another match, and now matcher state has been updated
              if (m.start() == m.end())
                throw new Jslt2Exception("Regexp " + regexp + " in replace() matched empty string in '" + arguments[0] + "'");

              // if there was text between pos and start of match, copy to output
              if (pos < m.start())
                bufix = copy(string, buf, bufix, pos, m.start());

              // copy sep to output (corresponds with the match)
              bufix = copy(sep, buf, bufix, 0, sep.length());

              // step over match
              pos = m.end();
            }

            if (pos == 0 && arguments[0].isTextual())
              // there were matches, so the string hasn't changed
              return arguments[0];
            else if (pos < string.length())
              // there was text remaining after the end of the last match. must copy
              bufix = copy(string, buf, bufix, pos, string.length());

            return new TextNode(new String(buf, 0, bufix));
        });
        
        runtime.addFunction("trim", 1, (input, arguments) -> {
            String string = Jslt2Util.toString(arguments[0], true);
            if (string == null)
              return NullNode.instance;

            int prefix = 0; // first non-space character

            // first: find leading spaces
            for (;
                 prefix < string.length() && isSpace(string, prefix);
                 prefix++)
              ;

            // if the whole thing is spaces we're done
            if (prefix == string.length())
              return new TextNode("");

            int suffix = string.length() - 1; // last non-space character

            // second: find trailing spaces
            for (;
                 suffix >= 0 && isSpace(string, suffix);
                 suffix--)
              ;

            // INV suffix >= prefix (they're equal if non-space part is 1 character)

            // if there are no leading or trailing spaces: keep input
            if (prefix == 0 && suffix == string.length() - 1 &&
                arguments[0].isTextual())
              return arguments[0];

            // copy out middle section and we're done
            return new TextNode(string.substring(prefix, suffix + 1));
        });
        
        // Boolean
        
        runtime.addFunction("not", (input, args) -> {
            if(args == null || args.length < 1) return BooleanNode.TRUE;
            JsonNode node = args[0];
            
            return BooleanNode.valueOf(!Jslt2Util.isTrue(node));
        });
        runtime.addFunction("boolean", 1, (input, arguments) -> {            
            return Jslt2Util.toJson(Jslt2Util.isTrue(arguments[0]));
        });
        runtime.addFunction("is-boolean", 1, (input, arguments) -> {            
            return Jslt2Util.toJson(arguments[0].isBoolean());
        });
        
        // Object
        
        runtime.addFunction("is-object", 1, (input, arguments) -> {            
            return Jslt2Util.toJson(arguments[0].isObject());
        });
        runtime.addFunction("get-key", 2, (input, arguments) -> {
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
        runtime.addFunction("array", 1, (input, arguments) -> {            
            JsonNode value = arguments[0];
            if (value.isNull() || value.isArray()) {
                return value;
            } 
            else if (value.isObject()) {
                return Jslt2Util.convertObjectToArray(runtime, value);
            }

            throw new Jslt2Exception("array() cannot convert " + value);
        });
        runtime.addFunction("is-array", 1, (input, arguments) -> {            
            return Jslt2Util.toJson(arguments[0].isArray());
        });
        runtime.addFunction("flatten", 1, (input, arguments) -> {            
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
        runtime.addFunction("parse-time", 2, (input, arguments) -> {            
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
        runtime.addFunction("format-time", 2, (input, arguments) -> {            
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

    private Pattern getRegexp(String regexp) {
        Pattern p = regexCache.get(regexp);
        if (p == null) {
            p = Pattern.compile(regexp);
            regexCache.put(regexp, p);
        }
        return p;
    }

    private static int copy(String input, char[] buf, int bufix, int from, int to) {
        for (int ix = from; ix < to; ix++) {
            buf[bufix++] = input.charAt(ix);
        }
        
        return bufix;
    }
    
    private static boolean isSpace(String string, int ix) {
        char ch = string.charAt(ix);
        return ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n';
    }
}
