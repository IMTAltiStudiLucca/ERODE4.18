package sbml.conversion.nodes.operators;

import org.sbml.jsbml.ASTNode;

public class ASTNodeBuilder {

    public ASTNode unary(ASTNode x, ASTNode.Type type) {
        ASTNode node = new ASTNode(type);
        node.addChild(x);
        return node;
    }

    public ASTNode binary(ASTNode x, ASTNode y, ASTNode.Type type) {
        ASTNode node = new ASTNode(type);
        node.addChild(x);
        node.addChild(y);
        return node;
    }

    public ASTNode reference(String name) {
        return new ASTNode(name);
    }

    public ASTNode integer(int value) {
            return new ASTNode(value);
    }
}
