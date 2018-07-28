/*
 * see license.txt 
 */
package jslt2;

import java.io.Reader;

/**
 * @author Tony
 *
 */
public interface ResourceResolver {

    /**
     * Resolve the jslt file path
     * 
     * @param jsltFile
     * @return the {@link Reader}
     */
    public Reader resolve(String jsltFile);
}
