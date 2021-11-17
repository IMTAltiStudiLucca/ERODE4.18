package sbml.conversion.nodes.value;

import org.sbml.jsbml.ASTNode;

import sbml.conversion.nodes.elements.ErodeElement;

public class ValueReader extends ValueASTConverter {

    private ErodeElement element;

    public ValueReader(ASTNode node,boolean mv) {
        super(node,mv);
        this.element = new ErodeElement();
        this.convert();
    }

    @Override
    protected void convert() {
        ASTNode.Type type = currentNode.getType();
        switch (type) {
            case NAME:
                this.updateFunction = element.reference(currentNode);
                break;
            case INTEGER:
                this.updateFunction = element.constant(currentNode,mv);
                break;
            case CONSTANT_FALSE:
            case CONSTANT_TRUE:
                this.updateFunction = element.booleanConstant(currentNode);
                break;
            default:
                throw new IllegalArgumentException("Unknown type name");
        }
    }
}
