/*
 * see license.txt 
 */
package jslt2.vm;

import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

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
            this.array = runtime.newArrayNode(object.size());
            
            Iterator<String> it = ((ObjectNode)object).fieldNames();            
            while(it.hasNext()) {
                String key = it.next();
                
                ObjectNode node = runtime.newObjectNode();
                node.set("key", TextNode.valueOf(key));
                node.set("value", this.object.get(key));
                
                this.array.add(node);
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
            this.current = this.array.get(this.index++);            
            return true;
        }
        
        return false;
    }

}
