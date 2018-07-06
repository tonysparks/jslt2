/*
    Leola Programming Language
    Author: Tony Sparks
    See license.txt
*/
package jslt2.vm.compiler;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * An {@link Outer} references a value off of the stack.  These are created via Closures.
 * 
 * @author Tony
 *
 */
public class Outer {

    /**
     * A Value that can be obtained from the operation
     * stack.
     *  
     * @author Tony
     *
     */
    public static interface StackValue {
        
        /**
         * Retrieve an element from the stack at
         * the supplied index.
         * 
         * @param index the index into the stack
         * @return the value
         */
        JsonNode getStackValue(int index);
        
        
        /**
         * Sets the stack at the supplied index with the
         * supplied value.
         * 
         * @param index the index into the stack
         * @param value the value to set
         */
        void setStackValue(int index, JsonNode value);
    }

    /**
     * Closed over stack value.  This is used when the value is
     * no longer on the stack, and the Closure is carrying this value
     * within its scope (aka the value has been 'closed' over).
     * 
     * @author Tony
     *
     */
    private static class ClosedStackValue implements StackValue {
        private JsonNode value;
        
        /**
         * @param value
         */
        public ClosedStackValue(JsonNode value) {
            this.value = value;
        }

        @Override
        public JsonNode getStackValue(int index) {        
            return value;
        }

        @Override
        public void setStackValue(int index, JsonNode value) {
            this.value = value;
        }
        
    }
    
    
    private StackValue stack;
    private int index;
    
    /**
     * @param stack
     * @param index
     */
    public Outer(StackValue stack, int index) {
        this.stack = stack;
        this.index = index;
    }
    

    
    /**
     * @return the index
     */
    public int getIndex() {
        return index;
    }
    
    /**
     * Sets the value
     * @param value
     */
    public void setValue(JsonNode value) {
        stack.setStackValue(index, value);
    }
    
    /**
     * @return the value
     */
    public JsonNode getValue() {
        return stack.getStackValue(index);
    }
    
    /**
     * The value is no longer on the stack,
     * so therefore we take the current value and store
     * it.  This value is now "closed" upon for a closure.
     */
    public void close() {        
        this.stack = new ClosedStackValue(this.stack.getStackValue(this.index));
        this.index = 0;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getValue().toString();
    }
}

