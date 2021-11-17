package sbml.conversion.nodes.unary;

import org.sbml.jsbml.ASTNode;

import it.imt.erode.booleannetwork.updatefunctions.NotBooleanUpdateFunction;
import sbml.conversion.nodes.INodeConverter;
import sbml.conversion.nodes.NodeConverter;

public abstract class UnaryASTConverter extends NodeConverter {

    public static UnaryASTConverter create(ASTNode node, boolean mv) {
        return new UnaryReader(node,mv);
    }

    public static UnaryASTConverter create(NotBooleanUpdateFunction updateFunction) {
        return new UnaryWriter(updateFunction);
    }

    protected INodeConverter child;

    public UnaryASTConverter(ASTNode node,boolean mv) {
        super(node,mv);
    }

    public UnaryASTConverter(NotBooleanUpdateFunction updateFunction) {
        super(updateFunction);
    }
}
