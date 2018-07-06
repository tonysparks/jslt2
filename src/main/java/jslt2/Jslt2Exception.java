/*
 * see license.txt
 */
package jslt2;

/**
 * @author Tony
 *
 */
public class Jslt2Exception extends RuntimeException {

    /**
     * SUID
     */
    private static final long serialVersionUID = 4445110898818625900L;

    public Jslt2Exception() {
    }

    /**
     * @param message
     */
    public Jslt2Exception(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public Jslt2Exception(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public Jslt2Exception(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public Jslt2Exception(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
