/*
 * see license.txt
 */
package jslt2.ast;

import java.util.List;

import jslt2.util.Tuple;

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

    protected <T extends Node> T becomeParentOf(T node) {
        if(node != null) {
            node.setParentNode(this);
        }
        return node;
    }
    
    protected <T extends Node> List<T> becomeParentOf(List<T> nodes) {
        if(nodes != null) {
            for(int i = 0; i < nodes.size(); i++) {
                becomeParentOf(nodes.get(i));
            }
        }
        return nodes;
    }
    
    protected <T extends Node, Y extends Node> List<Tuple<T, Y>> becomeParentOfByTuples(List<Tuple<T, Y>> nodes) {
        if(nodes != null) {
            for(int i = 0; i < nodes.size(); i++) {
                Tuple<T, Y> tuple = nodes.get(i);
                becomeParentOf(tuple.getFirst());
                becomeParentOf(tuple.getSecond());
            }
        }
        return nodes;
    }
}
