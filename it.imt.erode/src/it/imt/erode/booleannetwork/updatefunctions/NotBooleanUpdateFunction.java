package it.imt.erode.booleannetwork.updatefunctions;


import java.util.HashMap;
import java.util.LinkedHashMap;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Z3Exception;

import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.algorithms.booleannetworks.FBEAggregationFunctions;

public class NotBooleanUpdateFunction implements IUpdateFunction {

	private IUpdateFunction innerUpdateFunction;
	
	public NotBooleanUpdateFunction(IUpdateFunction first) {
		super();
		this.innerUpdateFunction = first;
	}
	
	public IUpdateFunction getInnerUpdateFunction() {
		return innerUpdateFunction;
	}
	
	@Override
	public String toString() {
		return "(!"+innerUpdateFunction.toString()+")";
	}

	@Override
	public BoolExpr toZ3(Context ctx, /*IBooleanNetwork booleanNetwork,*/ HashMap<String, ISpecies> nodeNameToNode,
			HashMap<ISpecies, Expr> nodeToTruthValue) throws Z3Exception {
		return ctx.mkNot((BoolExpr)innerUpdateFunction.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue));
	}
	
	@Override
	public IUpdateFunction cloneReplacingWithRepresentative(IPartition partition,
			LinkedHashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,HashMap<String, ISpecies> speciesNameToOriginalSpecies) {
		IUpdateFunction innerCloned = innerUpdateFunction.cloneReplacingWithRepresentative(partition, correspondenceBlock_ReducedSpecies,speciesNameToOriginalSpecies);
		return new NotBooleanUpdateFunction(innerCloned);
	}

	@Override
	public IUpdateFunction cloneReplacingNorRepresentativeWithNeutral(IPartition partition,
			LinkedHashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,
			HashMap<String, ISpecies> speciesNameToOriginalSpecies, FBEAggregationFunctions aggregationFunction) {
		IUpdateFunction innerCloned = innerUpdateFunction.cloneReplacingNorRepresentativeWithNeutral(partition, correspondenceBlock_ReducedSpecies,speciesNameToOriginalSpecies,aggregationFunction);
		return new NotBooleanUpdateFunction(innerCloned);
	}
	
	@Override
	public boolean seemsInputSpecies(String sp) {
		return false;
	}
			
}
