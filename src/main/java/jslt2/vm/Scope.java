/*
    Leola Programming Language
    Author: Tony Sparks
    See license.txt
*/
package jslt2.vm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;


/**
 * A {@link Scope} represents a lexical scope of variables
 *
 * @author Tony
 *
 */
public class Scope {            
    /**
     * The parent scope
     */
    private Scope parent;

    
    /**
     * The values stored in this scope
     */
    private ObjectNode values;
    
    /**
     * @param parent
     */
    public Scope(Scope parent) {
        this.parent = parent;
    }
    
    /**
     * This will clear out (i.e., remove) all data elements from this {@link Scope}.
     * 
     * <p>
     * This includes:
     * <ul>
     *     <li>The {@link ClassDefinitions}</li>
     *     <li>The {@link NamespaceDefinitions}</li>
     *     <li>Invokes {@link Scope#clear()} on the <code>parent</code> scope of this Scope</li>
     *     <li>Any bound variables within this Scope</li>
     * </ul>
     * As the name implies, this does remove all allocated objects which will become garbage and 
     * there before be collected by the JVM GC.  Use this method with caution.
     */
    public void clear() {        
        if(hasParent()) {
            this.parent.clear();
        }
                
        if(hasObjects()) {            
            this.values.removeAll();            
        }
    }
    
    /**
     * @return true if this {@link Scope} has a parent {@link Scope} defined.
     */
    public boolean hasParent() {
        return this.parent != null;
    }


    /**
     * Recursively attempts to retrieve the value associated with the reference.  If it
     * isn't found in this scope, it will ask its parent scope.
     * 
     * @param reference
     * @return the value if found, otherwise null
     */
    public JsonNode getObject(JsonNode reference) {
        JsonNode value = (this.values != null) ? this.values.get(reference.asText()) : null;
        if ( value == null && parent != null) {
            value = parent.getObject(reference);
        }

        return value;
    }

    /**
     * Recursively attempts to retrieve the value associated with the reference.  If it
     * isn't found in this scope, it will ask its parent scope.
     * 
     * @see Scope#getObject(LeoString)
     * @param reference
     * @return the LeoObject that is linked to the reference, if not found null is returned
     */
    public JsonNode getObject(String reference){
        return getObject(TextNode.valueOf(reference));
    }

    /**
     * Stores an object in this scope and only this scope. This does
     * not traverse the parent scopes to see if a value is already
     * held.
     * 
     * @param reference
     * @param value
     * @return the previously held value, if any
     */
    public JsonNode putObject(String reference, JsonNode value) {
        if(this.values==null) {
            this.values = new ObjectNode(JsonNodeFactory.instance);
        }

        return this.values.set(reference, value);        
    }
    
    public JsonNode putObject(JsonNode reference, JsonNode value) {
        return putObject(reference.asText(), value);
    }
    
    /**
     * Stores an object in this scope, it first checks to see if any parent
     * values contain the supplied reference, if it does it will override the existing
     * value.  This is to account for class data members.
     * 
     * @param reference
     * @param value
     * @return the previously held value, if any
     */
    public JsonNode storeObject(String reference, JsonNode newValue) {
        Scope current = this;        
        while (current != null) {
            
            // if the value is the the current scope, break out 
            if (current.values != null && current.values.get(reference) != null ) {
                break;
            }
            
            if(current.parent != null) {                
                current = current.parent;
                continue;
            }
            
            current = this;
            break;            
        }
        
        return current.putObject(reference, newValue);
    }

    public JsonNode storeObject(JsonNode reference, JsonNode value) {
        return storeObject(reference.asText(), value);
    }

    /**
     * Removes an object from this {@link Scope}
     *
     * @param reference
     * @return the {@link LeoObject} previously held by the reference (or null if no value was held
     * by this reference).
     */
    public JsonNode removeObject(String reference) {
        return (this.values!=null) ? this.values.remove(reference) : null;
    }

    
    /**
     * Removes an object from this {@link Scope}
     *
     * @param reference
     * @return the {@link LeoObject} previously held by the reference (or null if no value was held
     * by this reference).
     */
    public JsonNode removeObject(JsonNode reference) {
        return removeObject(reference.asText());
    }

    /**
     * @return true if and only if there are stored objects in this {@link Scope}
     */
    public boolean hasObjects() {
        return (this.values != null) && this.values.size() > 0;
    }
    
    /**
     * @return the number of {@link LeoObject}s in this {@link Scope}
     */
    public int getNumberOfObjects() {
        return (this.values != null) ? this.values.size() : 0;
    }

    /**
     * @return true if this scope is the global scope
     */
    public boolean isGlobalScope() {
        return parent == null;
    }
    
    /**
     * @return the parent
     */
    public Scope getParent() {
        return parent;
    }

}

