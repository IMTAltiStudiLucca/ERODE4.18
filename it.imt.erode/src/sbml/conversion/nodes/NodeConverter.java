package sbml.conversion.nodes;

import org.sbml.jsbml.ASTNode;

import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;

public abstract class NodeConverter implements INodeConverter {

    protected ASTNode currentNode;
    protected IUpdateFunction updateFunction;
    protected boolean mv;

    public NodeConverter(ASTNode node,boolean mv) {
        this.currentNode = node;
        this.mv=mv;
    }

    public NodeConverter(IUpdateFunction updateFunction) {
        this.updateFunction = updateFunction;
    }

    protected abstract void convert();

    @Override
    public IUpdateFunction getUpdateFunction() {
        return updateFunction;
    }

    @Override
    public ASTNode getExpressionAST() {
        return currentNode;
    }
}
