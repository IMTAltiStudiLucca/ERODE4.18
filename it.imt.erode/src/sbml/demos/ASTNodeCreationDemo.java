package sbml.demos;

import org.sbml.jsbml.ASTNode;

public class ASTNodeCreationDemo {

    @SuppressWarnings("unused")
	public static void main(String[] args) {
        ASTNode variable = new ASTNode("variableName");

        ASTNode integerConstant = new ASTNode(1);

        ASTNode booleanConstant = new ASTNode(ASTNode.Type.CONSTANT_FALSE);

        ASTNode operator = new ASTNode(ASTNode.Type.LOGICAL_AND);
    }
}
