package sbml.conversion.nodes.elements;

import org.sbml.jsbml.ASTNode;

import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.ReferenceToNodeUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.TrueUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.ValUpdateFunction;
import sbml.conversion.nodes.operators.ASTNodeBuilder;

public class SBMLElement implements IElement<IUpdateFunction,ASTNode> {

    private ASTNodeBuilder builder;

    public SBMLElement() {
        this.builder = new ASTNodeBuilder();
    }
    @Override
    public ASTNode reference(IUpdateFunction node) {
        Class<?> classType = node.getClass();
        if(!classType.equals(ReferenceToNodeUpdateFunction.class))
            throw new IllegalArgumentException("Given update function is not a reference");
        ReferenceToNodeUpdateFunction reference = (ReferenceToNodeUpdateFunction) node;
        return builder.reference(reference.toString());
    }

    /**
     * This method requires ERODE to support multi-valued networks,
     * to be meaningful. It could then be modified to convert any integer
     * representation.
     *
     * Currently, it is unused, but required by the interface
     * */
    @Override
    public ASTNode constant(IUpdateFunction node,boolean mv) {
        Class<?> classType = node.getClass();
        if(classType.equals(ValUpdateFunction.class)) {
        	double v=((ValUpdateFunction)node).getVal();
        	int v_int = (int)v;
        	if(v!=v_int) {
        		throw new UnsupportedOperationException("Currently supported only for integers");
        	}
        	return builder.integer(v_int);
        }
        else if(classType.equals(TrueUpdateFunction.class))
            return builder.integer(1);
        else
            return builder.integer(0);
    }

    @Override
    public ASTNode booleanConstant(IUpdateFunction node) {
        Class<?> classType = node.getClass();
        if(classType.equals(TrueUpdateFunction.class))
            return builder.integer(1);
        else
            return builder.integer(0);
    }
}
