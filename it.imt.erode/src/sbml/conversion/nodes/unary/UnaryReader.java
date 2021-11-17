package sbml.conversion.nodes.unary;

import org.sbml.jsbml.ASTNode;

import sbml.conversion.nodes.NodeManager;
import sbml.conversion.nodes.operators.ErodeOperator;

public class UnaryReader extends UnaryASTConverter {

    private ErodeOperator operator;

    public UnaryReader(ASTNode node,boolean mv) {
        super(node,mv);
        this.operator = new ErodeOperator(mv);
        this.child = NodeManager.create(node.getChild(0),mv);
        this.convert();
    }

    @Override
    protected void convert() {
        ASTNode.Type type = currentNode.getType();
        switch (type.name()) {
            case "LOGICAL_NOT":
                this.updateFunction = operator.not(child.getUpdateFunction());
                break;
            default:
                throw new IllegalArgumentException("Invalid type name");
        }
    }
}
