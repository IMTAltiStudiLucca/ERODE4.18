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

public interface IUpdateFunction {

	BoolExpr toZ3(Context ctx, /*IBooleanNetwork booleanNetwork,*/
			HashMap<String, ISpecies> speciesNameToSpecies, HashMap<ISpecies, BoolExpr> speciesToSpeciesVariable)
			throws Z3Exception;

	IUpdateFunction cloneReplacingWithRepresentative(
			IPartition partition,
			LinkedHashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,
			HashMap<String, ISpecies> speciesNameToOriginalSpecies);
	
	IUpdateFunction cloneReplacingNorRepresentativeWithNeutral(
			IPartition partition,
			LinkedHashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,
			HashMap<String, ISpecies> speciesNameToOriginalSpecies, FBEAggregationFunctions aggregationFunction);
	
}
