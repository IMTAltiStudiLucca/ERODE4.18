package it.imt.erode.booleannetwork.updatefunctions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Z3Exception;

import it.imt.erode.booleannetwork.interfaces.IBooleanNetwork;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.expression.parser.IMonomial;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.algorithms.booleannetworks.FBEAggregationFunctions;

public class Otherwise implements IUpdateFunction {

	@Override
	public Expr toZ3(Context ctx, HashMap<String, ISpecies> speciesNameToSpecies,
			HashMap<ISpecies, Expr> speciesToSpeciesVariable,boolean realSort) throws Z3Exception {
		throw new UnsupportedOperationException("No z3 for 'otherwise'");
	}

	@Override
	public IUpdateFunction cloneReplacingWithRepresentative(IPartition partition,
			LinkedHashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,
			HashMap<String, ISpecies> speciesNameToOriginalSpecies) {
		return new Otherwise();
	}

	@Override
	public IUpdateFunction cloneReplacingNorRepresentativeWithNeutral(IPartition partition,
			LinkedHashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,
			HashMap<String, ISpecies> speciesNameToOriginalSpecies, FBEAggregationFunctions aggregationFunction, IBooleanNetwork bn) {
		return new Otherwise();
	}
	
	@Override
	public String toString() {
		return "Otherwise";
	}

	@Override
	public boolean seemsInputSpecies(String sp) {
		return false;
	}
	@Override
	public void dropNonOutputSpecies(String sp, HashSet<String> guessedOutputs) {
		//do nothing
	}
	
	@Override
	public List<IMonomial> toPolynomial(HashMap<String, ISpecies> speciesNameToSpecies) throws Z3Exception {
		throw new UnsupportedOperationException("Only supported for Boolean BNs");
	}

}
