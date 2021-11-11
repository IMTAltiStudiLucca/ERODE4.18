package it.imt.erode.booleannetwork.updatefunctions;

import java.util.HashMap;
import java.util.Map.Entry;

import com.microsoft.z3.BoolExpr;

import it.imt.erode.booleannetwork.interfaces.IBooleanNetwork;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.crn.symbolic.constraints.BooleanConnector;

public class UpdateFunctionCompiler {

	private HashMap<BoolExpr,ISpecies > populationToSpecies;
	
	public UpdateFunctionCompiler(IBooleanNetwork bn,HashMap<ISpecies,BoolExpr> speciesToPopulation) {
		populationToSpecies = new HashMap<BoolExpr, ISpecies>(speciesToPopulation.size());
		for(Entry<ISpecies, BoolExpr> entry : speciesToPopulation.entrySet()) {
			populationToSpecies.put(entry.getValue(), entry.getKey());
		}
	}
	
	public IUpdateFunction toUpdateFunction(BoolExpr z3Expr) {
		
		if(z3Expr.isTrue()) {
			return new TrueUpdateFunction();
		}
		else if(z3Expr.isFalse()) {
			return new FalseUpdateFunction();
		} 
		else if(z3Expr.isNot()) {
			IUpdateFunction inner = toUpdateFunction((BoolExpr)z3Expr.getArgs()[0]);
			return new NotBooleanUpdateFunction(inner);
		}
		else if(z3Expr.isConst()) {
			ISpecies sp = populationToSpecies.get(z3Expr);
			return new ReferenceToNodeUpdateFunction(sp.getName());
		}
		else {
			BooleanConnector op=null;
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
			else if(z3Expr.isEq()) {
				op=BooleanConnector.EQ;
			}
			else {
				throw new UnsupportedOperationException(z3Expr.toString());
			}
			
			
			IUpdateFunction[] args = new IUpdateFunction[z3Expr.getNumArgs()];
			for(int i=0;i<args.length;i++) {
				args[i]=toUpdateFunction((BoolExpr)z3Expr.getArgs()[i]);
			}
			
			
			if(args.length==1) {
				return args[0];
			}
			else if(args.length==2) {
				return new BooleanUpdateFunctionExpr(args[0], args[1], op);
			} 
			else {
				IUpdateFunction c = args[0];
				for(int i=1;i<args.length;i++) {
					c=new BooleanUpdateFunctionExpr(c, args[i], op);
				}
				return c;
			}
		}		
	}
	

}
