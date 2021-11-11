package it.imt.erode.booleannetwork.updatefunctions;

import java.util.HashMap;
import java.util.LinkedHashMap;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Z3Exception;

import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.algorithms.booleannetworks.FBEAggregationFunctions;
import it.imt.erode.partitionrefinement.algorithms.booleannetworks.SMTForwardBooleanEquivalence;

public class ReferenceToNodeUpdateFunction implements IUpdateFunction {

	private String name;
	
	public ReferenceToNodeUpdateFunction(String name) {
		this.name=name;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	
	@Override
	public BoolExpr toZ3(Context ctx, /*IBooleanNetwork booleanNetwork,*/ HashMap<String, ISpecies> nodeNameToNode,
			HashMap<ISpecies, BoolExpr> nodeToTruthValue) throws Z3Exception {
		//return ctx.mkTrue();
		ISpecies referredNode = nodeNameToNode.get(name);
		return nodeToTruthValue.get(referredNode);
	}

	@Override
	public IUpdateFunction cloneReplacingWithRepresentative(IPartition partition,
			LinkedHashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,
			HashMap<String, ISpecies> speciesNameToOriginalSpecies) {
		ISpecies original = speciesNameToOriginalSpecies.get(name);
		IBlock block = partition.getBlockOf(original);
		ISpecies reducedSpecies = correspondenceBlock_ReducedSpecies.get(block);
		return new ReferenceToNodeUpdateFunction(reducedSpecies.getName());
	}
	
	@Override
	public IUpdateFunction cloneReplacingNorRepresentativeWithNeutral(IPartition partition,
			LinkedHashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,
			HashMap<String, ISpecies> speciesNameToOriginalSpecies, FBEAggregationFunctions aggregationFunction) {
		
		ISpecies original = speciesNameToOriginalSpecies.get(name);
		IBlock block = partition.getBlockOf(original);
		if(original.equals(block.getRepresentative())) {
			ISpecies reducedSpecies = correspondenceBlock_ReducedSpecies.get(block);
			return new ReferenceToNodeUpdateFunction(reducedSpecies.getName());
		}
		else {
			return SMTForwardBooleanEquivalence.neutralElementUpdFunc(aggregationFunction);
		}
	}
		
}
