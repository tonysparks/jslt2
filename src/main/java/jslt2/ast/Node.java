/*
 * see license.txt
 */
package jslt2.ast;

/**
 * Abstract Syntax Tree Node
 * 
 * @author Tony
 *
 */
public abstract class Node {

    private Node parentNode;
    private int lineNumber;
    private String sourceLine;
    
    /**
     * 
     */
    public Node() {
        // TODO Auto-generated constructor stub
    }
    
    public abstract void visit(NodeVisitor v);
    
    /**
     * @return the sourceLine
     */
    public String getSourceLine() {
        return sourceLine;
    }
    
    /**
     * @param sourceLine the sourceLine to set
     */
    public void setSourceLine(String sourceLine) {
        this.sourceLine = sourceLine;
    }
    
    /**
     * @return the lineNumber
     */
    public int getLineNumber() {
        return lineNumber;
    }
    
    /**
     * @param lineNumber the lineNumber to set
     */
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }
    
    /**
     * @param parentNode the parentNode to set
     */
    public void setParentNode(Node parentNode) {
        this.parentNode = parentNode;
    }
    
    /**
     * @return the parentNode
     */
    public Node getParentNode() {
        return parentNode;
    }

}
