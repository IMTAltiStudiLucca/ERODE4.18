package sbml.conversion.nodes.unary;

import it.imt.erode.booleannetwork.updatefunctions.BooleanUpdateFunctionExpr;
import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.NotBooleanUpdateFunction;
import it.imt.erode.crn.symbolic.constraints.BooleanConnector;
import sbml.conversion.nodes.NodeManager;
import sbml.conversion.nodes.operators.SBMLOperator;

public class UnaryWriter extends UnaryASTConverter {

    private SBMLOperator operator;

    public UnaryWriter(NotBooleanUpdateFunction updateFunction) {
        super(updateFunction);
        this.operator = new SBMLOperator();
        this.child = NodeManager.create(updateFunction.getInnerUpdateFunction());
        this.convert();
    }

    @Override
    protected void convert() {
        IUpdateFunction updateFunction = child.getUpdateFunction();
        Class<?> classType = updateFunction.getClass();
        if(classType.equals(BooleanUpdateFunctionExpr.class)) {
            BooleanUpdateFunctionExpr expression = (BooleanUpdateFunctionExpr) updateFunction;
            BooleanConnector connector = expression.getOperator();
            if(connector.equals(BooleanConnector.EQ)) {
                BooleanUpdateFunctionExpr neq = new BooleanUpdateFunctionExpr(
                        expression.getFirst(), expression.getSecond(), BooleanConnector.NEQ);
                this.child = NodeManager.create(neq);
                this.currentNode = child.getExpressionAST();
                return;
            }
        }
        this.currentNode = operator.not(child.getExpressionAST());
    }
}
