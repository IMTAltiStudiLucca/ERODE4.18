package sbml.conversion.nodes.binary;

import it.imt.erode.booleannetwork.updatefunctions.BooleanUpdateFunctionExpr;
import it.imt.erode.booleannetwork.updatefunctions.MVComparison;
import it.imt.erode.crn.symbolic.constraints.BasicConstraintComparator;
import it.imt.erode.crn.symbolic.constraints.BooleanConnector;
import sbml.conversion.nodes.NodeManager;
import sbml.conversion.nodes.operators.SBMLOperator;

public class BinaryWriter extends BinaryASTConverter {

    private SBMLOperator operator;

    public BinaryWriter(BooleanUpdateFunctionExpr updateFunction) {
        super(updateFunction);
        this.leftChild = NodeManager.create(updateFunction.getFirst());
        this.rightChild = NodeManager.create(updateFunction.getSecond());
        this.operator = new SBMLOperator();
        this.convert();
    }
    
    public BinaryWriter(MVComparison updateFunction) {
        super(updateFunction);
        this.leftChild = NodeManager.create(updateFunction.getLeft());
        this.rightChild = NodeManager.create(updateFunction.getRight());
        this.operator = new SBMLOperator();
        this.convert();
    }

    @Override
    protected void convert() {
    	if(updateFunction instanceof BooleanUpdateFunctionExpr) {
    		BooleanUpdateFunctionExpr expression = (BooleanUpdateFunctionExpr) updateFunction;
    		BooleanConnector connector = expression.getOperator();
    		switch (connector) {
    		case AND:
    			this.currentNode = operator.and(leftChild.getExpressionAST(),rightChild.getExpressionAST());
    			break;
    		case OR:
    			this.currentNode = operator.or(leftChild.getExpressionAST(),rightChild.getExpressionAST());
    			break;
    		case IMPLIES:
    			this.currentNode = operator.implies(leftChild.getExpressionAST(),rightChild.getExpressionAST());
    			break;
    		case XOR:
    			this.currentNode = operator.xor(leftChild.getExpressionAST(),rightChild.getExpressionAST());
    			break;
    		case EQ:
    			this.currentNode = operator.equals(leftChild.getExpressionAST(),rightChild.getExpressionAST());
    			break;
    		case NEQ:
    			this.currentNode = operator.notEquals(leftChild.getExpressionAST(),rightChild.getExpressionAST());
    			break;
    		}
    	}
    	else {
    		MVComparison expression = (MVComparison) updateFunction;
    		BasicConstraintComparator comparator = expression.getComp();
    		this.currentNode=operator.comparison(leftChild.getExpressionAST(),rightChild.getExpressionAST(),comparator);
    	}
    }
}
