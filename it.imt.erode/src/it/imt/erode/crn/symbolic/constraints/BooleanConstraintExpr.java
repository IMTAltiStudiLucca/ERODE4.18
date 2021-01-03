package it.imt.erode.crn.symbolic.constraints;

import java.util.HashMap;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Z3Exception;

import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ISpecies;

public class BooleanConstraintExpr implements IConstraint {

	private IConstraint first;
	private IConstraint second;
	private BooleanConnector op;
	
	public BooleanConstraintExpr(IConstraint first, IConstraint second,
			BooleanConnector op) {
		super();
		this.first = first;
		this.second = second;
		this.op = op;
		//no need to add this constraint to involved actions-  
	}
	
	@Override
	public String toString() {
		return "("+first.toString()+getSymbol(op)+second.toString()+")";
	}

	private static String getSymbol(BooleanConnector op) {
		switch (op) {
		case AND:
			return " /\\ ";
		case IMPLIES:
			return " -> ";
		case OR:
			return " \\/ ";
		default:
			throw new UnsupportedOperationException(op.toString());
		}
	}

	@Override
	public BoolExpr toZ3(Context ctx, HashMap<String, ArithExpr> symbParNameToSymbParZ3, ICRN crn,
			HashMap<String, ISpecies> speciesNameToSpecies, HashMap<ISpecies, ArithExpr> speciesToPopulation)
			throws Z3Exception {
		switch (op) {
		case AND:
			return ctx.mkAnd(new BoolExpr[] {first.toZ3(ctx,symbParNameToSymbParZ3,crn,speciesNameToSpecies,speciesToPopulation),second.toZ3(ctx,symbParNameToSymbParZ3,crn,speciesNameToSpecies,speciesToPopulation)});
		case IMPLIES:
			return ctx.mkImplies(first.toZ3(ctx,symbParNameToSymbParZ3,crn,speciesNameToSpecies,speciesToPopulation),second.toZ3(ctx,symbParNameToSymbParZ3,crn,speciesNameToSpecies,speciesToPopulation));
		case OR:
			return ctx.mkOr(new BoolExpr[] {first.toZ3(ctx,symbParNameToSymbParZ3,crn,speciesNameToSpecies,speciesToPopulation),second.toZ3(ctx,symbParNameToSymbParZ3,crn,speciesNameToSpecies,speciesToPopulation)});
		default:
			throw new UnsupportedOperationException(op.toString());
		}
	}
}
