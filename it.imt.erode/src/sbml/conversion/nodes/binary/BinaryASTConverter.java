package sbml.conversion.nodes.binary;

import org.sbml.jsbml.ASTNode;

import it.imt.erode.booleannetwork.updatefunctions.BooleanUpdateFunctionExpr;
import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.MVComparison;
import sbml.conversion.nodes.INodeConverter;
import sbml.conversion.nodes.NodeConverter;

public abstract class BinaryASTConverter extends NodeConverter {

    public static BinaryASTConverter create(ASTNode node,boolean mv) {
        return new BinaryReader(node,mv);
    }

    public static BinaryASTConverter create(BooleanUpdateFunctionExpr updateFunction) {
        return new BinaryWriter(updateFunction);
    }
    public static BinaryASTConverter create(MVComparison updateFunction) {
        return new BinaryWriter(updateFunction);
    }

    protected INodeConverter leftChild;
    protected INodeConverter rightChild;

    public BinaryASTConverter(ASTNode node,boolean mv) {
        super(node,mv);
    }

    public BinaryASTConverter(IUpdateFunction updateFunction) {
        super(updateFunction);
    }

}
