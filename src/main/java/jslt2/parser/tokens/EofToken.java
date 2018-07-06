package jslt2.parser.tokens;

import jslt2.parser.Source;

/**
 * End of File token
 * 
 * @author Tony
 *
 */
public class EofToken extends Token {
    
    public EofToken(Source source) {
        super(source);
        this.type = TokenType.END_OF_FILE;
    }
}
