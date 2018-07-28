/*
 * see license.txt 
 */
package jslt2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * @author Tony
 *
 */
public class ResourceResolvers {

    /**
     * Resolver which finds files on the classpath.
     * 
     * @return the {@link Reader} if the file is found
     * @throws Jslt2Exception if the resource is not found
     */
    public static ResourceResolver newClassPathResolver() {
        return new ResourceResolver() {
            
            @Override
            public Reader resolve(String jsltFile) {
                InputStream iStream = ResourceResolvers.class.getResourceAsStream(jsltFile);
                if(iStream == null) {
                    throw new Jslt2Exception("Could not find: '" + jsltFile + "' on the classpath.");
                }
                
                return new InputStreamReader(iStream);
            }
        };
    }

    /**
     * Resolver which finds files relative to the supplied base directory
     * 
     * @param baseDir the base directory to start looking for the file
     * @return the {@link Reader} if the file is found
     * @throws Jslt2Exception if the resource is not found
     */
    public static ResourceResolver newFilePathResolver(File baseDir) {
        return new ResourceResolver() {
            
            @Override
            public Reader resolve(String jsltFile) {
                File file = new File(baseDir, jsltFile);
                if(!file.exists()) {
                    throw new Jslt2Exception("Could not find: '" + jsltFile + "' in '" + file + "'.");
                }
                
                try {
                    return new BufferedReader(new FileReader(file));
                }
                catch(Exception e) {
                    throw new Jslt2Exception(e);
                }
            }
        };
    }
}
