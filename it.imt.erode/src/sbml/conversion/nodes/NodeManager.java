package sbml.conversion.nodes;

import java.util.List;

import org.sbml.jsbml.ASTNode;

import it.imt.erode.booleannetwork.updatefunctions.BooleanUpdateFunctionExpr;
import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.MVComparison;
import it.imt.erode.booleannetwork.updatefunctions.NotBooleanUpdateFunction;
import sbml.configurations.Strings;
import sbml.conversion.nodes.binary.BinaryASTConverter;
import sbml.conversion.nodes.unary.UnaryASTConverter;
import sbml.conversion.nodes.value.ValueASTConverter;

public class NodeManager {
    public static INodeConverter create(ASTNode node, boolean mv) {
        switch (node.getChildCount()) {
            case 2:
                return BinaryASTConverter.create(node,mv);
            case 1:
                return UnaryASTConverter.create(node,mv);
            case 0:
                return ValueASTConverter.create(node,mv);
            default:
            	//There can be nodes like this:(CEBPb == 0) && (CEBPa == 1) && (Pu1 == 2)
            	//I transform it in (CEBPb == 0) && ((CEBPa == 1) && (Pu1 == 2))
            	ASTNode new_root = new ASTNode(node.getType());
            	List<ASTNode> nodes = node.getListOfNodes();
            	ASTNode left = nodes.get(0);
            	new_root.addChild(left);
            	ASTNode right = new ASTNode(node.getType());
            	for(int n=1;n<node.getListOfNodes().size();n++) {
            		right.addChild(nodes.get(n));
            	}
            	new_root.addChild(right);
            	return create(new_root,mv);
                //throw new IllegalArgumentException("A node cannot have more than 2 children");
        }
    }

    public static INodeConverter create(IUpdateFunction updateFunction) {
        Class<?> classType = updateFunction.getClass();
        String className = classType.getSimpleName();
        switch (className) {
            case Strings.BINARY_EXPRESSION:
                return BinaryASTConverter.create((BooleanUpdateFunctionExpr)updateFunction);
            case Strings.MV_COMPARISON:
                return BinaryASTConverter.create((MVComparison)updateFunction);    
            case Strings.NEGATION:
                return UnaryASTConverter.create((NotBooleanUpdateFunction)updateFunction);
            case Strings.REFERENCE:
            case Strings.TRUE:
            case Strings.FALSE:
            case Strings.VALUE:	
                return ValueASTConverter.create(updateFunction);
            default:
                throw new IllegalArgumentException("Unknown update function type");
        }
    }
}
