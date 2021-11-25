package sbml.conversion.nodes.operators;

import it.imt.erode.booleannetwork.updatefunctions.ArithmeticConnector;
import it.imt.erode.booleannetwork.updatefunctions.BinaryExprIUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.BooleanUpdateFunctionExpr;
import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.MVComparison;
import it.imt.erode.booleannetwork.updatefunctions.NotBooleanUpdateFunction;
import it.imt.erode.crn.symbolic.constraints.BasicConstraintComparator;
import it.imt.erode.crn.symbolic.constraints.BooleanConnector;

public class ErodeOperator implements IOperator<IUpdateFunction> {

	boolean mv;
	public ErodeOperator(boolean mv) {
		this.mv=mv;
	}
	
    @Override
    public IUpdateFunction not(IUpdateFunction x) {
        return new NotBooleanUpdateFunction(x);
    }

    @Override
    public IUpdateFunction and(IUpdateFunction x, IUpdateFunction y) {
        return new BooleanUpdateFunctionExpr(x, y, BooleanConnector.AND);
    }

    @Override
    public IUpdateFunction or(IUpdateFunction x, IUpdateFunction y) {
        return new BooleanUpdateFunctionExpr(x, y, BooleanConnector.OR);
    }

    @Override
    public IUpdateFunction xor(IUpdateFunction x, IUpdateFunction y) {
        return new BooleanUpdateFunctionExpr(x,y,BooleanConnector.XOR);
    }

    @Override
    public IUpdateFunction implies(IUpdateFunction x, IUpdateFunction y) {
        return new BooleanUpdateFunctionExpr(x,y,BooleanConnector.IMPLIES);
    }

    @Override
    public IUpdateFunction equals(IUpdateFunction x, IUpdateFunction y) {
        return new BooleanUpdateFunctionExpr(x,y,BooleanConnector.EQ);
    }

    @Override
    public IUpdateFunction notEquals(IUpdateFunction x, IUpdateFunction y) {
        return new BooleanUpdateFunctionExpr(x,y,BooleanConnector.NEQ);
    }
    
    
    
    @Override
    public IUpdateFunction plus(IUpdateFunction x, IUpdateFunction y) {
		return new BinaryExprIUpdateFunction(x, y, ArithmeticConnector.SUM);
    }
    @Override
    public IUpdateFunction product(IUpdateFunction x, IUpdateFunction y) {
		return new BinaryExprIUpdateFunction(x, y, ArithmeticConnector.MUL);
    }
    @Override
	public IUpdateFunction comparison(IUpdateFunction l, IUpdateFunction r, BasicConstraintComparator cmp) {
		return new MVComparison(l, r, cmp);
	}
    
    @Override
	public IUpdateFunction bn_comparison(IUpdateFunction l, IUpdateFunction r, BasicConstraintComparator cmp) {
    	if(cmp.equals(BasicConstraintComparator.EQ)) {
    		//a = b stays a = b
    		//return new BooleanUpdateFunctionExpr(l, r, BooleanConnector.EQ);
    		return equals(l, r);
    	}
//    	else if(cmp.equals(BasicConstraintComparator.NEQ)) {
//    		//a != b stays a != b
//    		return new BooleanUpdateFunctionExpr(l, r, BooleanConnector.NEQ);
//    	}
    	else if(cmp.equals(BasicConstraintComparator.GT)) {
    		//a > b becomes a & !b
    		return new BooleanUpdateFunctionExpr(l, new NotBooleanUpdateFunction(r), BooleanConnector.AND);
    	}
    	else if(cmp.equals(BasicConstraintComparator.GEQ)) {
    		//a >= b becomes (a=b) | (a& !b)
    		IUpdateFunction eq= bn_comparison(l, r, BasicConstraintComparator.EQ);
    		IUpdateFunction gt= bn_comparison(l, r, BasicConstraintComparator.GT);
    		return new BooleanUpdateFunctionExpr(eq, gt , BooleanConnector.OR);
    	}
    	else if(cmp.equals(BasicConstraintComparator.LT)) {
    		//a < b becomes !a & b
    		return new BooleanUpdateFunctionExpr(new NotBooleanUpdateFunction(l), r, BooleanConnector.AND);
    	}
    	else if(cmp.equals(BasicConstraintComparator.LEQ)) {
    		//a <= b becomes (a=b) | (a& !b)
    		IUpdateFunction eq= bn_comparison(l, r, BasicConstraintComparator.EQ);
    		IUpdateFunction lt= bn_comparison(l, r, BasicConstraintComparator.LT);
    		return new BooleanUpdateFunctionExpr(eq, lt, BooleanConnector.OR);
    	}
    	else {
    		throw new UnsupportedOperationException(cmp.toString());
    	}
	}
}
