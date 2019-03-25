/*
 * see license.txt 
 */
package jslt2.util;

import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import jslt2.Jslt2;
import jslt2.Jslt2Exception;

/**
 * @author Tony
 *
 */
public class Jslt2Util {

    public static JsonNode toJson(Jslt2 runtime, String[] array) {
        ArrayNode node = runtime.newArrayNode(array.length);
        for (int ix = 0; ix < array.length; ix++)
            node.add(array[ix]);
        return node;
    }
    
    
    public static JsonNode toJson(boolean value) {
        if (value)
            return BooleanNode.TRUE;
        else
            return BooleanNode.FALSE;
    }

    public static JsonNode toJson(double value) {
        return new DoubleNode(value);
    }
    
    /**
     * nullok => return Java null for Json null
     * 
     * @param value
     * @param nullok
     * @return
     */
    public static String toString(JsonNode value, boolean nullok) {
        // JSLT logic for toString..
        
        // check what type this is
        if (value.isTextual())
            return value.asText();
        else if (value.isNull() && nullok)
            return null;


        return value.toString();
    }
    
    public static ArrayNode toArray(JsonNode value, boolean nullok) {
        // check what type this is
        if (value.isArray()) {
            return (ArrayNode) value;
        }
        else if (value.isNull() && nullok) {
            return null;
        }

        throw new Jslt2Exception("Cannot convert " + value + " to array");
    }
    
    public static ArrayNode convertObjectToArray(Jslt2 runtime, JsonNode object) {
        ArrayNode array = runtime.newArrayNode(object.size());
        Iterator<Map.Entry<String, JsonNode>> it = object.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> item = it.next();
            ObjectNode element = runtime.newObjectNode();
            element.set("key", new TextNode(item.getKey()));
            element.set("value", item.getValue());
            array.add(element);
        }
        return array;
    }
    
    /**
     * Does JSLT logic for true
     * 
     * @param value
     * @return true if this {@link JsonNode} is truthy
     */
    public static boolean isTrue(JsonNode value) {
        return value != BooleanNode.FALSE && 
              !(value.isObject() && value.size() == 0) && 
              !(value.isTextual() && value.asText().length() == 0) && 
              !(value.isArray() && value.size() == 0) && 
              !(value.isNumber() && value.doubleValue() == 0.0) && 
              !value.isNull();
    }
    
    public static boolean isValue(JsonNode value) {
        return !value.isNull() && 
               !(value.isObject() && value.size() == 0) && 
               !(value.isArray()  && value.size() == 0);
    }
    
    /**
     * Does JSLT logic for equals
     * 
     * @param l
     * @param r
     * @return true if the {@link JsonNode}s are equal
     */
    public static boolean equals(JsonNode l, JsonNode r) {
        if(l.isNumber() && r.isNumber()) {
            if(l.isIntegralNumber() && r.isIntegralNumber()) {
                return l.longValue() == r.longValue();
            }
            else {
                return l.doubleValue() == r.doubleValue();
            }
        }
        
        return l.equals(r);
    }
    
    /**
     * Does a compareTo of the two nodes
     * 
     * @param l
     * @param r
     * @return positive number if l is greater, negative number if l is smaller or 0 if they are equal
     */
    public static int compare(JsonNode l, JsonNode r) {
        if(l.isNumber() && r.isNumber()) {
            return (int)(l.asDouble() - r.asDouble());
        }
        else if(l.isTextual() && r.isTextual()) {
            return l.asText().compareTo(r.asText());
        }
        else if(l.isNull() || r.isNull()) {
            if(l.isNull() && r.isNull()) {
                return 0;
            }
            else if(l.isNull()) {
                return -1;
            }
            else {
                return 1;
            }
        }
        
        throw new Jslt2Exception("Can't compare " + l + " and " + r);
    } 
    
    public static JsonNode number(JsonNode value, boolean strict, JsonNode fallback) {
        // check what type this is
        if (value.isNumber() || value.isNull()) {
            return value;
        }
        else if (!value.isTextual()) {
            if (strict) {
                throw new Jslt2Exception("Can't convert " + value + " to number");
            }
            
            if(fallback != null) {
                return fallback;
            }
            
            return NullNode.instance;
            
        }

        // let's look at this number
        String number = value.asText();
        try {
            if (number.indexOf('.') != -1) {
                return new DoubleNode(Double.parseDouble(number));
            }
            else {
                return IntNode.valueOf(Integer.parseInt(number));
            }
        } 
        catch (NumberFormatException e) {        
            if(fallback != null) {
                return fallback;
            }
            
            throw new Jslt2Exception("number(" + number + ") failed: not a number");
        }
    }
    
    
    /**
     * Removes null nodes
     * 
     * @param node
     */
    public static JsonNode removeNullNodes(JsonNode node) {
        if(node.isNull()) {
            return node;
        }
        
        Iterator<JsonNode> it = node.iterator();
        while (it.hasNext()) {
            JsonNode child = it.next();
            
            if(removeNullNodes(child).isNull()) {
                it.remove();
                continue;
            }
            
            if (child.isNull() || ((child.isArray()||child.isObject()) && child.size() == 0)) {
                it.remove();
                continue;
            }
        }
        
        if ((node.isArray()||node.isObject()) && node.size() == 0) {
            return NullNode.instance;
        }
        
        return node;
    }
}
