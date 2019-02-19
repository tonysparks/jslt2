package jslt2.parser;

import jslt2.parser.tokens.Token;
import jslt2.parser.tokens.TokenType;

/**
 * JSTL2 error codes
 * 
 * @author Tony
 *
 */
public enum ErrorCode {
    INVALID_ASSIGNMENT("Invalid assignment statement"),
    INVALID_CHARACTER("Invalid character"),
    INVALID_NUMBER("Invalid number"),
    INVALID_IMPORT_ACCESS("Invalid import access"),

    MISSING_COMMA("Missing ,"),
    MISSING_RIGHT_BRACE("Missing Right Brace"),
    MISSING_EQUALS("Missing ="),
    MISSING_IDENTIFIER("Missing identifier"),
    MISSING_RIGHT_BRACKET("Missing ]"),
    MISSING_RIGHT_PAREN("Missing )"),
    MISSING_LEFT_PAREN("Missing ("),
    MISSING_LEFT_BRACE("Missing {"),
    MISSING_COLON("Missing :"),
    
    
    RANGE_INTEGER("Integer literal out of range"),
    RANGE_LONG("Long literal out of range"),
    RANGE_REAL("Real literal out of range"),

    UNEXPECTED_EOF("Unexpected end of file"),
    UNEXPECTED_TOKEN("Unexpected token"),
    UNIMPLEMENTED("Unimplemented feature"),

    UNKNOWN_ERROR("An unknown error occured"),

    // Fatal errors.
    //IO_ERROR(-101, "Object I/O error"),
    TOO_MANY_ERRORS(-102, "Too many syntax errors");

    private int status;      // exit status
    private String message;  // error message

    /**
     * Constructor.
     * @param message the error message.
     */
    ErrorCode(String message) {
        this.status = 0;
        this.message = message;
    }

    /**
     * Constructor.
     * @param status the exit status.
     * @param message the error message.
     */
    ErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    /**
     * Getter.
     * @return the exit status.
     */
    public int getStatus() {
        return status;
    }

    /**
     * @return the message.
     */
    @Override
    public String toString() {
        return message;
    }
    
    /**
     * Formatted error message 
     * 
     * @param token
     * @param errorMessage
     * @param sourceLine
     * @return the formatted error message
     */
    public static String errorMessage(Token token, String errorMessage, String sourceLine) {
        int lineNumber = token.getLineNumber();
        int position = token.getPosition();
        String tokenText = token.getType() != TokenType.END_OF_FILE ? token.getText() : null;
         
        
        int spaceCount = position + 1;        
        StringBuilder flagBuffer = new StringBuilder();
        flagBuffer.append("\n").append(sourceLine).append("\n");

        // Spaces up to the error position.
        for (int i = 1; i < spaceCount; ++i) {
            flagBuffer.append(' ');
        }

        // A pointer to the error followed by the error message.
        flagBuffer.append("^\n*** ").append(errorMessage);

        flagBuffer.append(" [at line: ").append(lineNumber);
        
        // Text, if any, of the bad token.
        if (tokenText != null) {
            flagBuffer.append(" '").append(tokenText).append("'");
        }
        
        flagBuffer.append("]\n");

        return flagBuffer.toString();
    }
}
