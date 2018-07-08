/*
 * see license.txt 
 */
package jslt2.util;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Non-synchronized stack
 * 
 * @author Tony
 *
 */
public class Stack<T> extends ArrayList<T> {

    /**
     * SUID
     */
    private static final long serialVersionUID = 237872682067888089L;

    public Stack() {
        super();
    }

    /**
     * @param initialCapacity
     */
    public Stack(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * @param c
     */
    public Stack(Collection<T> c) {
        super(c);
    }
    
    public void push(T element) {
        this.add(element);
    }
    
    public T pop() {
        T result = this.remove(this.size() - 1);
        return result;
    }
    
    public T peek() {
        return get(this.size() - 1);
    }

}
