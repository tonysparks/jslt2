/*
 * see license.txt 
 */
package jslt2.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;

import jslt2.Jslt2Exception;

/**
 * @author Tony
 *
 */
public class Jslt2Util {

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
}
