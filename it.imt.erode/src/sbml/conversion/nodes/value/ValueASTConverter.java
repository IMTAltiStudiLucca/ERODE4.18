package sbml.conversion.nodes.value;

import org.sbml.jsbml.ASTNode;

import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import sbml.conversion.nodes.NodeConverter;

public abstract class ValueASTConverter extends NodeConverter {

    public static ValueASTConverter create(ASTNode node,boolean mv) {
        return new ValueReader(node,mv);
    }

    public static ValueASTConverter create(IUpdateFunction updateFunction) {
        return new ValueWriter(updateFunction);
    }

    public ValueASTConverter(ASTNode node,boolean mv) {
        super(node,mv);
    }

    public ValueASTConverter(IUpdateFunction updateFunction) {
        super(updateFunction);
    }
}
