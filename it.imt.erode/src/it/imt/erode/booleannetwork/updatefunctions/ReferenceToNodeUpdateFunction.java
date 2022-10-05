package it.imt.erode.booleannetwork.updatefunctions;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Z3Exception;

import it.imt.erode.booleannetwork.interfaces.IBooleanNetwork;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.algorithms.booleannetworks.FBEAggregationFunctions;
import it.imt.erode.partitionrefinement.algorithms.booleannetworks.SMTForwardBooleanEquivalence;

public class ReferenceToNodeUpdateFunction implements IUpdateFunction/*_ArithExprRefToNode_Value*/ {

	private String name;
	
	public ReferenceToNodeUpdateFunction(String name) {
		this.name=name;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	
	@Override
	public Expr toZ3(Context ctx, /*IBooleanNetwork booleanNetwork,*/ HashMap<String, ISpecies> nodeNameToNode,
			HashMap<ISpecies, Expr> nodeToTruthValue,boolean realSort) throws Z3Exception {
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
			HashMap<String, ISpecies> speciesNameToOriginalSpecies, FBEAggregationFunctions aggregationFunction
			, IBooleanNetwork bn) {
		
		ISpecies original = speciesNameToOriginalSpecies.get(name);
		IBlock block = partition.getBlockOf(original);
		if(original.equals(block.getRepresentative())) {
			ISpecies reducedSpecies = correspondenceBlock_ReducedSpecies.get(block);
			return new ReferenceToNodeUpdateFunction(reducedSpecies.getName());
		}
		else {
			Collection<ISpecies> blockOfSpecies = partition.getBlockOf(original).getSpecies();
			return SMTForwardBooleanEquivalence.neutralElementUpdFunc(aggregationFunction,blockOfSpecies,bn);
		}
	}
	
	@Override
	public boolean seemsInputSpecies(String sp) {
		return sp.equals(name);
	}
	@Override
	public void dropNonOutputSpecies(String sp, HashSet<String> guessedOutputs) {
		//A species that appears in the update function of other species, is not an output 
		if(!sp.equals(name)){
			guessedOutputs.remove(name);
		}
	}

	
		
}
