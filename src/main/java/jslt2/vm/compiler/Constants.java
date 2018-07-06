/*
    Leola Programming Language
    Author: Tony Sparks
    See license.txt
*/
package jslt2.vm.compiler;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

import jslt2.util.ArrayUtil;
import jslt2.vm.Bytecode;


/**
 * The {@link Constants} pool.  For each {@link Bytecode} there 
 * exists a constants pool.  The pool stores literals such as strings and numbers.
 * 
 * @author Tony
 *
 */
public class Constants {

    /**
     * Storage
     */
    private List<JsonNode> storage;
    
    /**
     */
    public Constants() {        
    }
    
    /**
     * @return The size of the constant pool
     */
    public int getNumberOfConstants() {
        return (this.storage != null ) ? this.storage.size() : 0;
    }
    
    /**
     * @return the storage
     */
    private List<JsonNode> lazystorage() {
        if ( this.storage == null ) {
            this.storage = new ArrayList<>();
        }
        return storage;
    }
    
    /**
     * Stores the string literal (converts it to a {@link JsonNode}).
     * @param value
     * @return the constant index of where it's stored
     */
    public int store(String value) {                
        JsonNode str = TextNode.valueOf(value);
        return store(str);
    }
    
    /**
     * Stores the number literal (converts it to a {@link JsonNode}).
     * @param value
     * @return the constant index of where it's stored
     */
    public int store(int n) {
        JsonNode number = IntNode.valueOf(n);
        return store(number);
    }
    
    /**
     * Stores a {@link JsonNode} literal
     * 
     * @param obj
     * @return the constant index of where it's stored
     */
    public int store(JsonNode obj) {
        if ( this.lazystorage().contains(obj) ) {
            return this.storage.indexOf(obj);
        }
        
        this.storage.add(obj);
        return this.storage.size() - 1;
    }
    
    /**
     * Retrieves the index in which the supplied {@link JsonNode} is stored.
     * @param obj
     * @return the index (or -1 if not found) in where this object is stored in the pool
     */
    public int get(JsonNode obj) {
        return this.storage == null ? -1 : this.storage.indexOf(obj);
    }
    
    /**
     * Retrieves the {@link JsonNode} and a particular index.
     * 
     * @param index
     * @return the {@link JsonNode} stored and the supplied index
     */
    public JsonNode get(int index) {
        return this.storage == null ? null : this.storage.get(index);
    }
    
    /**
     * @return compiles into an array of constants
     */
    public JsonNode[] compile() {
        JsonNode[] constants = ArrayUtil.EMPTY_NODES;
        if (this.storage != null) {
            constants = this.storage.toArray(new JsonNode[this.storage.size()]);
        }
        return constants;
    }
}

