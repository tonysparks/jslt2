/*
 * see license.txt 
 */
package jslt2.vm;

import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jslt2.Jslt2;
import jslt2.Jslt2Exception;

/**
 * @author Tony
 *
 */
public class ForEntry {
    private final JsonNode object;
    private JsonNode current;    
    private ArrayNode array;
    
    private int index;
    
    private ObjectNode scopeNode;
    private boolean isObject;
    private boolean isNull;
    
    /**
     * @param object
     * @param index
     */
    public ForEntry(Jslt2 runtime, JsonNode object) {
        this.object = object;
        if(object.isNull()) {
            this.current = NullNode.instance;
        }
        else if(object.isObject()) {
            this.isObject = true;
            this.scopeNode = runtime.newObjectNode();
            this.array = runtime.newArrayNode(object.size());
            
            Iterator<String> it = ((ObjectNode)object).fieldNames();            
            while(it.hasNext()) {
                this.array.add(it.next());
            }
        }
        else if(object.isArray()) {            
            this.array = runtime.newArrayNode(object.size());
            
            Iterator<JsonNode> it = object.elements();           
            while(it.hasNext()) {
                this.array.add(it.next());
            }
        }
        else {
            throw new Jslt2Exception("ForIterationError: " + object + " is not an iterable element");
        }
        
    }
    
    public JsonNode current() {
        return this.current;
    }
    
    public boolean hasNext() {
        return !this.isNull && this.index < this.array.size();
    }
    
    public boolean advance() {
        if(hasNext()) {
            JsonNode next = this.array.get(this.index++);
            if(this.isObject) {
                String key = next.asText(); 
                this.scopeNode.set("key", next);
                this.scopeNode.set("value", this.object.get(key));
                this.current = this.scopeNode;
            }            
            else {
                this.current = (JsonNode) next;
            }
            
            return true;
        }
        
        return false;
    }

}
