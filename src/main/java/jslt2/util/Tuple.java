/*
    See license.txt
*/
package jslt2.util;

/**
 * Simple way to group two values together.
 * 
 * @author Tony
 *
 */
public class Tuple<X, Y> {

    /**
     * Get the first item
     */
    private X first;
    
    /**
     * Get the second item
     */
    private Y second;

    /**
     * 
     * @param first
     * @param second
     */
    public Tuple(X first, Y second) {
        this.first=first;
        this.second=second;
    }
    
    /**
     */
    public Tuple() {        
    }
    
    /**
     * @param first the first to set
     */
    public void setFirst(X first) {
        this.first = first;
    }

    /**
     * @return the first
     */
    public X getFirst() {
        return first;
    }

    /**
     * @param second the second to set
     */
    public void setSecond(Y second) {
        this.second = second;
    }

    /**
     * @return the second
     */
    public Y getSecond() {
        return second;
    }
}

