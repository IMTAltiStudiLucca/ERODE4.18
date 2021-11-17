package sbml.conversion.nodes;

import org.sbml.jsbml.ASTNode;

import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;

public interface INodeConverter {

    IUpdateFunction getUpdateFunction();

    ASTNode getExpressionAST();
}
