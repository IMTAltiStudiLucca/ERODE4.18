package sbml.conversion.nodes.elements;

import org.sbml.jsbml.ASTNode;

import it.imt.erode.booleannetwork.updatefunctions.FalseUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.ReferenceToNodeUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.TrueUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.ValUpdateFunction;

public class ErodeElement implements IElement<ASTNode, IUpdateFunction> {


    @Override
    public IUpdateFunction reference(ASTNode node) {
        return new ReferenceToNodeUpdateFunction(node.getName());
    }

    @Override
    public IUpdateFunction constant(ASTNode node,boolean mv) {
    	if(mv) {
    		return new ValUpdateFunction(node.getInteger());
    	}
    	else {
    		switch (node.getInteger()) {
    		case 1:
    			return new TrueUpdateFunction();
    		case 0:
    			return new FalseUpdateFunction();
    		default:
    			throw new IllegalArgumentException("Given node value is not a boolean value");
    		}
    	}
    }

    @Override
    public IUpdateFunction booleanConstant(ASTNode node) {
        ASTNode.Type type = node.getType();
        switch (type) {
            case CONSTANT_FALSE:
                return new FalseUpdateFunction();
            default:
                return new TrueUpdateFunction();
        }
    }
}
