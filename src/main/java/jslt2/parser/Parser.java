package jslt2.parser;


import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;

import static jslt2.parser.tokens.TokenType.*;

import jslt2.ast.*;
import jslt2.parser.tokens.Token;
import jslt2.parser.tokens.TokenType;
import jslt2.util.Tuple;


/**
 * A {@link Parser} for the JSLT programming language.
 * 
 * @author Tony
 *
 */
public class Parser {   
    private final Scanner scanner;
    private final List<Token> tokens;
    private int current;
        
    private Token startToken;
    
    /**
     * @param scanner
     *            the scanner to be used with this parser.
     */
    public Parser(Scanner scanner) {
        this.scanner = scanner;
        this.tokens = scanner.getTokens();
        
        this.current = 0;    
    }

    /**
     * Parses the program
     * 
     * @return the {@link ProgramExpr}
     */
    public ProgramExpr parseProgram() {
        
        List<ImportExpr> imports = new ArrayList<>();
        List<LetExpr> lets = new ArrayList<>();
        List<DefExpr> defs = new ArrayList<>();
        
        while(!isAtEnd()) {
            if(match(IMPORT))    imports.add(importDeclaration());
            else if(match(DEF))  defs.add(defDeclaration());
            else if(match(LET))  lets.add(letDeclaration());            
            else                 break;
        }
        
        Expr expr = null;
        if(!isAtEnd()) {
            expr = expression();
        }
        
        return node(new ProgramExpr(imports, lets, defs, expr));
    }
        
    private ImportExpr importDeclaration() {
        source();
        
        String aliasName = null;
        
        Token library = consume(IDENTIFIER, ErrorCode.MISSING_IDENTIFIER);
        if(match(AS)) {
            Token alias = consume(IDENTIFIER, ErrorCode.MISSING_IDENTIFIER);
            aliasName = alias.getText();
        }
        
        return node(new ImportExpr(library.getText(), aliasName));
    }
    
    private List<LetExpr> letDeclarations() {
        List<LetExpr> lets = new ArrayList<>();
        while(match(LET)) {
            lets.add(letDeclaration());
        }
        
        return lets;
    }
    
    private LetExpr letDeclaration() {
        source();
        
        Token identifier = consume(IDENTIFIER, ErrorCode.MISSING_IDENTIFIER);
        consume(EQUALS, ErrorCode.MISSING_EQUALS);
        Expr expr = expression();
        
        return node(new LetExpr(identifier.getText(), expr));
    }
    
    private DefExpr defDeclaration() {
        source();
        
        Token identifier = consume(IDENTIFIER, ErrorCode.MISSING_IDENTIFIER);        
        List<String> parameters = parameters();
        List<LetExpr> lets = letDeclarations();
        Expr body = expression();
        return node(new DefExpr(identifier.getText(), parameters, lets, body));
    }
    
    
    /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *                      Expression parsing
     *~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
    
    
    private Expr expression() {
        source();
        return or();
    }
    
    
    private IfExpr ifExpr() {
        List<LetExpr> lets = letDeclarations();
        
        consume(LEFT_PAREN, ErrorCode.MISSING_LEFT_PAREN);
        Expr condition = expression();
        consume(RIGHT_PAREN, ErrorCode.MISSING_RIGHT_PAREN);
        
        Expr thenBranch = expression();
        ElseExpr elseBranch = null;
        if (match(ELSE)) {
            elseBranch = elseExpr();
        }
        
        return node(new IfExpr(lets, condition, thenBranch, elseBranch));
    }
    
    private ElseExpr elseExpr() {
        List<LetExpr> lets = letDeclarations();
        Expr expr = expression();
        return new ElseExpr(lets, expr);
    }
    
    private DotExpr dotExpr() {
        Expr field = null;
        if(match(IDENTIFIER))  field = node(new IdentifierExpr(previous().getText()));
        else if(match(STRING)) field = node(new StringExpr(previous().getText()));
        
        return node(new DotExpr(field));
    }
    
    private MatchExpr matchExpr() {
        List<Expr> fields = new ArrayList<>();
        if(match(MINUS)) {
            do {
                if(match(IDENTIFIER))  fields.add(node(new IdentifierExpr(previous().getText())));
                else if(match(STRING)) fields.add(node(new StringExpr(previous().getText())));                
            }
            while(match(COMMA));
        }
        
        return node(new MatchExpr(fields));
    }
    
    private Expr or() {
        Expr expr = and();
        
        while(match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = node(new BinaryExpr(expr, operator.getType(), right));
        }
        
        return expr;
    }
    
    private Expr and() {
        Expr expr = equality();
        
        while(match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = node(new BinaryExpr(expr, operator.getType(), right));
        }
        
        return expr;
    }
    
    private Expr equality() {
        Expr expr = comparison();
        
        while(match(NOT_EQUALS, EQUALS_EQUALS, EQUALS)) {
            Token operator = previous();
            Expr right = comparison();
            expr = node(new BinaryExpr(expr, operator.getType(), right));
        }
        
        return expr;
    }
    
    private Expr comparison() {
        Expr expr = term();
        
        while(match(GREATER_THAN, GREATER_EQUALS, LESS_THAN, LESS_EQUALS)) {
            Token operator = previous();
            Expr right = term();
            expr = node(new BinaryExpr(expr, operator.getType(), right));
        }
        
        return expr;
    }
    
    private Expr term() {
        Expr expr = factor();
        
        while(match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = node(new BinaryExpr(expr, operator.getType(), right));
        }
        
        return expr;
    }
    
    private Expr factor() {
        Expr expr = unary();
        
        while(match(SLASH, STAR, MOD)) {
            Token operator = previous();
            Expr right = unary();
            expr = node(new BinaryExpr(expr, operator.getType(), right));
        }
        
        return expr;
    }
    
    private Expr unary() {
        if(match(NOT, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return node(new UnaryExpr(operator.getType(), right)); 
        }
        return functionCall();
    }
    
    private Expr functionCall() {
        Expr expr = primary();
        while(true) {
            if(match(LEFT_PAREN)) {
                expr = finishFunctionCall(expr);
            }
            else if(check(LEFT_BRACKET)) {
                // following spec - but this is still broken 
                // if we allow variables to be referenced in other let/def
                // expressions
                if(!(expr instanceof FuncCallExpr ||
                   expr instanceof VariableExpr || 
                   expr instanceof DotExpr)) {
                    return expr;
                }
                
                advance();
                
                Expr startIndexExpr = null;
                Expr endIndexExpr = null;
                
                if(!check(COLON)) {
                    startIndexExpr = expression();
                }
                
                if(match(COLON)) {
                    if(!check(TokenType.RIGHT_BRACKET)) {
                        endIndexExpr = expression();
                    }
                    else {
                        endIndexExpr = new NumberExpr(IntNode.valueOf(-1));
                    }
                    
                    if(startIndexExpr == null) {
                        startIndexExpr = new NumberExpr(IntNode.valueOf(0));
                    }
                    
                    consume(RIGHT_BRACKET, ErrorCode.MISSING_RIGHT_BRACKET);                
                    expr = node(new ArraySliceExpr(expr, startIndexExpr, endIndexExpr));
                }
                else {                    
                    consume(RIGHT_BRACKET, ErrorCode.MISSING_RIGHT_BRACKET);                
                    expr = node(new ArrayIndexExpr(expr, startIndexExpr));
                }
            }
            else if(match(DOT)) {
                Token name = consume(IDENTIFIER, ErrorCode.MISSING_IDENTIFIER);
                expr = node(new GetExpr(expr, name.getText()));
            }/* TODO - Modules
            else if(match(COLON)) {
                IdentifierExpr varExpr = null;
                if((expr instanceof IdentifierExpr)) {
                    varExpr = (IdentifierExpr)expr;
                }
                else {
                    throw error(previous(), ErrorCode.INVALID_IMPORT_ACCESS);
                }
                
                Token name = consume(IDENTIFIER, ErrorCode.MISSING_IDENTIFIER);
                expr = node(new ImportGetExpr(varExpr, name.getText()));
            }*/
            else {
                break;
            }
        }
        
        return expr;
    }
    
    private Expr primary() {
        source();
        
        if(match(TRUE))  return node(new BooleanExpr(true));
        if(match(FALSE)) return node(new BooleanExpr(false));
        if(match(NULL))  return node(new NullExpr());
        
        if(match(NUMBER))  return node(new NumberExpr((JsonNode)previous().getValue()));        
        if(match(STRING))  return node(new StringExpr(previous().getValue().toString()));
        
        if(match(IDENTIFIER)) return node(new IdentifierExpr(previous().getText()));
        if(match(VARIABLE))   return node(new VariableExpr(previous().getText()));
        
        if(match(LEFT_PAREN)) return groupExpr();
        
        if(match(LEFT_BRACKET)) return array();
        if(match(LEFT_BRACE))   return object();
        
        if(match(IF))   return ifExpr();
        if(match(DOT))  return dotExpr();
        if(match(STAR)) return matchExpr();
                        
        throw error(peek(), ErrorCode.UNEXPECTED_TOKEN);
    }     
    
    private Expr finishFunctionCall(Expr callee) {
        List<Expr> arguments = arguments();        
        return node(new FuncCallExpr(callee, arguments));
    }
    
    private Expr groupExpr() {
        Expr expr = expression();
        consume(RIGHT_PAREN, ErrorCode.MISSING_RIGHT_PAREN);
        return new GroupExpr(expr);
    }
    
    private ForArrayExpr forArrayExpr() {
        consume(LEFT_PAREN, ErrorCode.MISSING_LEFT_PAREN);
        Expr condition = expression();
        consume(RIGHT_PAREN, ErrorCode.MISSING_RIGHT_PAREN);
        List<LetExpr> lets = letDeclarations();
        Expr valueExpr = expression();
        
        return node(new ForArrayExpr(condition, lets, valueExpr));
    }
    
    private ForObjectExpr forObjectExpr() {
        consume(LEFT_PAREN, ErrorCode.MISSING_LEFT_PAREN);
        Expr condition = expression();
        consume(RIGHT_PAREN, ErrorCode.MISSING_RIGHT_PAREN);
        List<LetExpr> lets = letDeclarations();

        Expr key = expression();
        consume(COLON, ErrorCode.MISSING_COLON);
        Expr value = expression();
        
        return node(new ForObjectExpr(condition, lets, key, value));
    }
    
    private ArrayExpr array() {
        List<Expr> elements = new ArrayList<>();
        ForArrayExpr forArrayExpr = null;
        
        if(match(FOR)) {
            forArrayExpr = forArrayExpr();
        }
        else {
            do {
                if(check(RIGHT_BRACKET)) {
                    break;
                }
             
                Expr element = expression();
                elements.add(element);
             
            }
            while(match(COMMA));
        }
        
        consume(RIGHT_BRACKET, ErrorCode.MISSING_RIGHT_BRACKET);
        
        return node(new ArrayExpr(forArrayExpr, elements));
    }
    
    private ObjectExpr object() {
        List<LetExpr> lets = letDeclarations();
        
        ForObjectExpr forObjectExpr = null;
        List<Tuple<Expr, Expr>> elements = new ArrayList<>();
        
        if(match(FOR)) {
            forObjectExpr = forObjectExpr();
        }
        else {
            do {
                if(check(RIGHT_BRACE)) {
                    break;
                }

                Expr key = expression();
                consume(COLON, ErrorCode.MISSING_COLON);
                Expr value = expression();
                
                elements.add(new Tuple<>(key, value));

            }
            while(match(COMMA));
        }
        
        consume(RIGHT_BRACE, ErrorCode.MISSING_RIGHT_BRACE);
        
        return node(new ObjectExpr(lets, forObjectExpr, elements));
    }

    /**
     * Parses parameters:
     * 
     * def(x,y,z) {
     * }
     * 
     * The (x,y,z) part
     * 
     * @return the parsed {@link ParameterList}
     */
    private List<String> parameters() {
        consume(LEFT_PAREN, ErrorCode.MISSING_LEFT_PAREN);
        
        List<String> parameters = new ArrayList<>();        
        if(!check(RIGHT_PAREN)) {
            do {                
                Token param = consume(IDENTIFIER, ErrorCode.MISSING_IDENTIFIER);
                parameters.add(param.getText());
            }
            while(match(COMMA));
        }
        
        consume(RIGHT_PAREN, ErrorCode.MISSING_RIGHT_PAREN);
        return parameters;
    }
    
    /**
     * Parses arguments:
     * 
     * someFunction( 1.0, x );
     * 
     * Parses the ( 1.0, x ) into a {@link List} of {@link Expr}
     * 
     * @return the {@link List} of {@link Expr}
     */
    private List<Expr> arguments() {
        List<Expr> arguments = new ArrayList<>();
        if(!check(RIGHT_PAREN)) {        
            do {
                arguments.add(expression());
            } 
            while(match(COMMA));            
        }
        
        consume(RIGHT_PAREN, ErrorCode.MISSING_RIGHT_PAREN);
        
        return arguments;
    }
       

    
    /**
     * Mark the start of parsing a statement
     * so that we can properly mark the AST node
     * source line and number information
     */
    private void source() {
        this.startToken = peek();
    }
    
    /**
     * Updates the AST node parsing information
     * 
     * @param node
     * @return the supplied node
     */
    private <T extends Node> T node(T node) {
        if(this.startToken != null) {
            node.setSourceLine(this.startToken.getText());
            node.setLineNumber(this.startToken.getLineNumber());
        }
        return node;
    }
    
    /**
     * Determines if the supplied {@link TokenType} is
     * the current {@link Token}, if it is it will advance
     * over it.
     * 
     * @param type
     * @return true if we've advanced (i.e., the supplied token type was
     * the current one).
     */
    private boolean match(TokenType type) {        
        if(check(type)) {
            advance();
            return true;
        }        
        
        return false;
    }

    /**
     * Determines if any of the supplied {@link TokenType}'s are
     * the current {@link Token}, if it is it will advance.
     * 
     * @param type
     * @return true if we've advanced (i.e., the supplied token type was
     * the current one).
     */
    private boolean match(TokenType ...types) {
        for(TokenType type : types) {
            if(check(type)) {
                advance();
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Ensures the supplied {@link TokenType} is the current one and 
     * advances.  If the {@link TokenType} does not match, this will
     * throw a {@link ParseException}
     * 
     * @param type
     * @param errorCode
     * @return the skipped {@link Token}
     */
    private Token consume(TokenType type, ErrorCode errorCode) {
        if(check(type)) {
            return advance();
        }
        
        throw error(peek(), errorCode);
    }
    
    
    /**
     * Checks to see if the current {@link Token} is of the supplied
     * {@link TokenType}
     * 
     * @param type
     * @return true if it is
     */
    private boolean check(TokenType type) {
        if(isAtEnd()) {
            return false;
        }
        
        return peek().getType() == type;
    }
  
    /**
     * Advances to the next Token.  If we've reached
     * the END_OF_FILE token, this stop advancing.
     * 
     * @return the previous token.
     */
    private Token advance() {
        if(!isAtEnd()) {
            this.current++;
        }
        return previous();
    }
    
    
    /**
     * The previous token
     * @return The previous token
     */
    private Token previous() {
        return this.tokens.get(current - 1);
    }
        
    /**
     * The current token
     * @return The current token
     */
    private Token peek() {
        return this.tokens.get(current);
    }
        
    /**
     * If we've reached the end of the file
     * 
     * @return true if we're at the end
     */
    private boolean isAtEnd() {
        return peek().getType() == END_OF_FILE;
    }
    
    
    /**
     * Constructs an error message into a {@link ParseException}
     * 
     * @param token
     * @param errorCode
     * @return the {@link ParseException} to be thrown
     */
    private ParseException error(Token token, ErrorCode errorCode) {
        int lineNumber = token.getLineNumber();
        int position = token.getPosition();
        String tokenText = token.getType() != TokenType.END_OF_FILE ? token.getText() : null;
        String errorMessage = errorCode.toString(); 
        
        int spaceCount = position + 1;
        String currentLine = this.scanner.getSourceLine(lineNumber);
        StringBuilder flagBuffer = new StringBuilder(currentLine != null ? currentLine : "");
        flagBuffer.append("\n");

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
        
        flagBuffer.append("]");

        return new ParseException(errorCode, token, flagBuffer.toString());
    }
}
