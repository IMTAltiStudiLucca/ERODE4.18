package it.imt.erode.booleannetwork.updatefunctions;


import java.util.HashMap;
import java.util.LinkedHashMap;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Z3Exception;

import it.imt.erode.booleannetwork.interfaces.IBooleanNetwork;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;

public class NotBooleanUpdateFunction implements IUpdateFunction {

	private IUpdateFunction innerUpdateFunction;
	
	public NotBooleanUpdateFunction(IUpdateFunction first) {
		super();
		this.innerUpdateFunction = first;
	}
	
	@Override
	public String toString() {
		return "(!"+innerUpdateFunction.toString()+")";
	}

	@Override
	public BoolExpr toZ3(Context ctx, IBooleanNetwork booleanNetwork, HashMap<String, ISpecies> nodeNameToNode,
			HashMap<ISpecies, BoolExpr> nodeToTruthValue) throws Z3Exception {
		return ctx.mkNot(innerUpdateFunction.toZ3(ctx,booleanNetwork,nodeNameToNode,nodeToTruthValue));
	}
	
	@Override
	public IUpdateFunction cloneReplacingWithRepresentative(IPartition partition,
			LinkedHashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,HashMap<String, ISpecies> speciesNameToOriginalSpecies) {
		IUpdateFunction innerCloned = innerUpdateFunction.cloneReplacingWithRepresentative(partition, correspondenceBlock_ReducedSpecies,speciesNameToOriginalSpecies);
		return new NotBooleanUpdateFunction(innerCloned);
	}
			
}
