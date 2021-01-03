package it.imt.erode.crn.symbolic.constraints;


import java.util.HashMap;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Z3Exception;

import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ISpecies;

public class FalseConstraint implements IConstraint {
	
	@Override
	public String toString() {
		return "false";
	}

	@Override
	public BoolExpr toZ3(Context ctx, HashMap<String, ArithExpr> symbParNameToSymbParZ3, ICRN crn,
			HashMap<String, ISpecies> speciesNameToSpecies, HashMap<ISpecies, ArithExpr> speciesToPopulation)
			throws Z3Exception {
		return ctx.mkFalse();
	}

	
}
