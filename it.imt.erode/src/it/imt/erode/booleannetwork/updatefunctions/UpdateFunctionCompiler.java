package it.imt.erode.booleannetwork.updatefunctions;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;

import it.imt.erode.booleannetwork.interfaces.IBooleanNetwork;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.crn.symbolic.constraints.BasicConstraintComparator;
import it.imt.erode.crn.symbolic.constraints.BooleanConnector;

public class UpdateFunctionCompiler {

	private HashMap</*Bool*/Expr,ISpecies > populationToSpecies;
	
	public UpdateFunctionCompiler(IBooleanNetwork bn,HashMap<ISpecies,/*Bool*/Expr> speciesToPopulation) {
		populationToSpecies = new HashMap<>(speciesToPopulation.size());
		for(Entry<ISpecies, /*Bool*/Expr> entry : speciesToPopulation.entrySet()) {
			populationToSpecies.put(entry.getValue(), entry.getKey());
		}
	}
	
	/**
	 * To be invoked first on expressions satisfying z3Expr.isITE()
	 * I assume we have: 
	 *	either 'if guard then val else eitherValOrNestedIf'
	 *	or	   'if guard then val else valOtherwise'
	 * @param z3Expr
	 * @return
	 */
	public IUpdateFunction unrolITE(Expr z3Expr,LinkedHashMap<Integer, IUpdateFunction> collectedCases) {
		Expr guard = z3Expr.getArgs()[0];
		Expr thenBranch = z3Expr.getArgs()[1];
		Expr elseBranch = z3Expr.getArgs()[2];
		
		int caseVal=Integer.valueOf(thenBranch.toString());
		IUpdateFunction caseCondition=toUpdateFunction(guard);
		collectedCases.put(caseVal, caseCondition);
		
		if(elseBranch.isNumeral()) {
			// 'if guard then val else valOtherwise'
			int caseOtherwiseVal=Integer.valueOf(elseBranch.toString());
			collectedCases.put(caseOtherwiseVal, new Otherwise());
			return new MVUpdateFunctionByCases(collectedCases, -1);
		}
		else {
			// 'if guard then val else eitherValOrNestedIf'
			return unrolITE(elseBranch,collectedCases);
		}
	}
	
	
	public IUpdateFunction toUpdateFunction(Expr z3Expr) {
		BasicConstraintComparator cmp=null;
		if(z3Expr.isTrue()) {
			return new TrueUpdateFunction();
		}
		else if(z3Expr.isFalse()) {
			return new FalseUpdateFunction();
		} 
		else if(z3Expr.isNot()) {
			IUpdateFunction inner = toUpdateFunction(z3Expr.getArgs()[0]);
			return new NotBooleanUpdateFunction(inner);
		}
		else if((cmp=isComparison(z3Expr))!=null) {
			//MV
			IUpdateFunction left = toUpdateFunction(z3Expr.getArgs()[0]);
			IUpdateFunction right = toUpdateFunction(z3Expr.getArgs()[1]);
			return new MVComparison(left, right, cmp);
		}
		else if(z3Expr.isNumeral()) {
			//MV
			return new ValUpdateFunction(Integer.valueOf(z3Expr.toString()));
		}
		else if(z3Expr.isITE()) {
			//MV update function by cases
//			for(Expr arg:z3Expr.getArgs()) {
//				System.out.println(arg);
//			}
			return unrolITE(z3Expr, new LinkedHashMap<>());
		}
		else if(z3Expr.isConst()) {
			ISpecies sp = populationToSpecies.get(z3Expr);
			return new ReferenceToNodeUpdateFunction(sp.getName());
		}
		else {
			BooleanConnector op=null;
			ArithmeticConnector opArith=null;
			if(z3Expr.isAnd()) {
				op=BooleanConnector.AND;
			}
			else if(z3Expr.isImplies()) {
				op=BooleanConnector.IMPLIES;
			}
			else if(z3Expr.isOr()) {
				op=BooleanConnector.OR;
			}
			else if(z3Expr.isXor()) {
				op=BooleanConnector.XOR;
			}
			else if(z3Expr.isEq()) {
				op=BooleanConnector.EQ;
			}
			else if(z3Expr.isAdd()) {
				opArith=ArithmeticConnector.SUM;
			}
			else if(z3Expr.isMul()) {
				opArith=ArithmeticConnector.MUL;
			}
			//there is no 'neq'
//			else if(z3Expr.isEq()) {
//				op=BooleanConnector.EQ;
//			}
			else {
				throw new UnsupportedOperationException(z3Expr.toString());
			}
			
			
			IUpdateFunction[] args = new IUpdateFunction[z3Expr.getNumArgs()];
			
			for(int i=0;i<args.length;i++) {
				if(op!=null)
					args[i]=toUpdateFunction((BoolExpr)z3Expr.getArgs()[i]);
				else if(opArith!=null)
					args[i]=toUpdateFunction((ArithExpr)z3Expr.getArgs()[i]);
			}
			
			if(args.length==1) {
				return args[0];
			}
			else if(args.length==2) {
				if(op!=null)
					return new BooleanUpdateFunctionExpr(args[0], args[1], op);
				else
					return new BinaryExprIUpdateFunction(args[0], args[1], opArith);
			} 
			else {
				IUpdateFunction c = args[0];
				for(int i=1;i<args.length;i++) {
					if(op!=null)
						c=new BooleanUpdateFunctionExpr(c, args[i], op);
					else 
						c=new BinaryExprIUpdateFunction(c, args[i], opArith);
				}
				return c;
			}
		}		
	}

	private BasicConstraintComparator isComparison(Expr z3Expr) {
		if( z3Expr.isLT()) {
			return BasicConstraintComparator.LT;
		}
		else if( z3Expr.isLE()) {
			return BasicConstraintComparator.LEQ;
		} 
		else if( z3Expr.isGT()) {
			return BasicConstraintComparator.GT;
		}
		else if( z3Expr.isGE()) {
			return BasicConstraintComparator.GEQ;
		} 
		else if(z3Expr.isEq()) {
			return BasicConstraintComparator.EQ;
		}
		else {
			return null;
		}
	}
	

}
