package jslt2.parser;

import static jslt2.parser.Source.EOF;
import static jslt2.parser.Source.EOL;

import java.util.ArrayList;
import java.util.List;

import jslt2.parser.tokens.EofToken;
import jslt2.parser.tokens.ErrorToken;
import jslt2.parser.tokens.NumberToken;
import jslt2.parser.tokens.SpecialSymbolToken;
import jslt2.parser.tokens.StringToken;
import jslt2.parser.tokens.Token;
import jslt2.parser.tokens.TokenType;
import jslt2.parser.tokens.WordToken;


/**
 * A {@link Scanner} for the JSLT.  This will break up
 * the source code into {@link Token} that will be used by the {@link Parser}
 * to make sense of.
 * 
 * @author Tony
 *
 */
public class Scanner {
    
    public static final String START_COMMENT = "/*";
    public static final String END_COMMENT = "*/";
    public static final String SINGLE_COMMENT = "//";

    private List<Token> tokens;
    private Source source;
    
    /**
     * Constructor
     * 
     * @param source
     *            the source to be used with this scanner.
     */
    public Scanner(Source source) {
        this.source = source;
        this.tokens = new ArrayList<>();
        
        try {
            while(!source.atEof()) {
                this.tokens.add(this.extractToken());
            }    
        }
        finally {
            source.close();
        }
    }
    
    /**
     * Get a line of raw source code
     * 
     * @param lineNumber
     * @return the line of raw source code
     */
    public String getSourceLine(int lineNumber) {
        return this.source.getLine(lineNumber);
    }

    /**
     * The scanned {@link Token}s
     * 
     * @return The scanned {@link Token}s
     */
    public List<Token> getTokens() {
        return tokens;
    }
    

    /**
     * Do the actual work of extracting and returning the next token from the
     * source. Implemented by scanner subclasses.
     * 
     * @return the next token.
     */   
    private Token extractToken() {
        skipWhiteSpace();
        Token token;
        char currentChar = this.source.currentChar();

        // Construct the next token.  The current character determines the
        // token type.
        if (currentChar == EOF) {
            token = new EofToken(source);
        }
        else if ( WordToken.isValidStartIdentifierCharacter(currentChar) ) {
            token = new WordToken(source);
        }
        else if (Character.isDigit(currentChar)) {
            token = new NumberToken(source);
        }
        else if (currentChar == StringToken.STRING_CHAR) {
            token = new StringToken(source);
        }
        else if (TokenType.SPECIAL_SYMBOLS
                 .containsKey(Character.toString(currentChar))) {
            token = new SpecialSymbolToken(source);
        }
        else {
            token = new ErrorToken(source, ErrorCode.INVALID_CHARACTER,
                                         Character.toString(currentChar));
            this.source.nextChar();  // consume character
        }

        return token;
    }

    /**
     * Skip whitespace characters by consuming them.  A comment is whitespace.
     */
    private void skipWhiteSpace() {
        char currentChar = this.source.currentChar();

        while (Character.isWhitespace(currentChar)
                || checkSequence(START_COMMENT)
                || checkSequence(SINGLE_COMMENT) ) {

            // Start of a comment?
            if ( checkSequence(START_COMMENT) ) {
                do {
                    currentChar = this.source.nextChar();  // consume comment characters
                }
                while ((!checkSequence(END_COMMENT)) && (currentChar != EOF));

                // Found closing '*/'?
                if ( checkSequence(END_COMMENT) ) {
                    for(int i = 0; i < END_COMMENT.length(); i++) {
                        currentChar = this.source.nextChar();  // consume the comment
                    }
                }
            }
            else if ( checkSequence(SINGLE_COMMENT) ) {
                do {
                    currentChar = this.source.nextChar();  // consume comment characters
                }
                while (currentChar != EOL);
            }
            // Not a comment.
            else {
                currentChar = this.source.nextChar();  // consume whitespace character
            }
        }
    }

    /**
     * Check the sequence matches the input (2 chars).
     *
     * @param seq
     * @return
     */
    private boolean checkSequence(String seq) {
        boolean result = true;
        char currentChar = this.source.currentChar();

        for(int i = 0; i < 2; i++) {
            if (currentChar == EOF ) {
                result = false;
                break;
            }

            if (currentChar != seq.charAt(i)) {
                result = false;
                break;
            }

            currentChar = this.source.peekChar();
        }
        /*if ( result ) {
            nextChar(); // eat the char
        }*/

        return result;
    }
}
