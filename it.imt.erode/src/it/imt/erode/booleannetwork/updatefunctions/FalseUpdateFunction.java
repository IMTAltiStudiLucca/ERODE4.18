package it.imt.erode.booleannetwork.updatefunctions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Z3Exception;

import it.imt.erode.booleannetwork.interfaces.IBooleanNetwork;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.algorithms.booleannetworks.FBEAggregationFunctions;

public class FalseUpdateFunction implements IUpdateFunction {

	@Override
	public String toString() {
		return "false";
	}
	
	
	@Override
	public BoolExpr toZ3(Context ctx, /*IBooleanNetwork booleanNetwork,*/ HashMap<String, ISpecies> nodeNameToNode,
			HashMap<ISpecies, Expr> nodeToTruthValue,boolean realSort) throws Z3Exception {
		return ctx.mkFalse();
	}


	@Override
	public IUpdateFunction cloneReplacingWithRepresentative(IPartition partition,
			LinkedHashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,
			HashMap<String, ISpecies> speciesNameToOriginalSpecies) {
		return new FalseUpdateFunction();
	}
	


	@Override
	public IUpdateFunction cloneReplacingNorRepresentativeWithNeutral(IPartition partition,
			LinkedHashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,
			HashMap<String, ISpecies> speciesNameToOriginalSpecies, FBEAggregationFunctions aggregationFunction, IBooleanNetwork bn) {
		return new FalseUpdateFunction();
	}
	
	@Override
	public boolean seemsInputSpecies(String sp) {
		return true;
	}
	@Override
	public void dropNonOutputSpecies(String sp, HashSet<String> guessedOutputs) {
		//do nothing
	}
		
}
