/*
 * see license.txt 
 */
package jslt2.util;


/**
 * Non-synchronized stack
 * 
 * @author Tony
 *
 */
public class Stack<T> {

    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    
    private Object[] elements;
    private int count;
    
    public Stack() {
        this(16);
    }

    /**
     * @param initialCapacity
     */
    public Stack(int initialCapacity) {
        this.elements = new Object[initialCapacity];
    }

    private void ensureCapacity(int minCapacity) {
        if(minCapacity > elements.length) {
            int oldCapacity = elements.length;
            int newCapacity = oldCapacity + (oldCapacity >> 1);
            if (newCapacity - minCapacity < 0)
                newCapacity = minCapacity;
            if (newCapacity - MAX_ARRAY_SIZE > 0)
                newCapacity = hugeCapacity(minCapacity);
            Object[] newArray = new Object[newCapacity];
            System.arraycopy(elements, 0, newArray, 0, oldCapacity);
            elements = newArray;
        }
    }
    
    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError();
        return (minCapacity > MAX_ARRAY_SIZE) ?
            Integer.MAX_VALUE :
            MAX_ARRAY_SIZE;
    }
    

    
    public int size() {
        return count;
    }
    
    public boolean isEmpty() {
        return count == 0;
    }
    
    public boolean contains(T element) {
        for(int i = 0; i < count; i++) {
            if(elements[i].equals(element)) {
                return true;
            }
        }
        
        return false;
    }
    
    public void push(T element) {
        ensureCapacity(count + 1);
        elements[count++] = element;
    }
    
    public void add(T element) {
        ensureCapacity(count + 1);
        elements[count++] = element;
    }
    
    @SuppressWarnings("unchecked")
    public T pop() {
        if(count < 1) {
            return null;
        }
        
        T result = (T)elements[--count]; 
        return result;
    }
    
    @SuppressWarnings("unchecked")
    public T peek() {
        if(count < 1) {
            return null;
        }
        
        T result = (T)elements[count - 1]; 
        return result; 
    }

}
