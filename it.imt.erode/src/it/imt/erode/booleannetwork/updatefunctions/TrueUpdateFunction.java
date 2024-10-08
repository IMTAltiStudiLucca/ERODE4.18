package it.imt.erode.booleannetwork.updatefunctions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Z3Exception;

import it.imt.erode.booleannetwork.interfaces.IBooleanNetwork;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.expression.parser.IMonomial;
import it.imt.erode.expression.parser.NumberMonomial;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.algorithms.booleannetworks.FBEAggregationFunctions;

public class TrueUpdateFunction implements IUpdateFunction {

	@Override
	public String toString() {
		return "true";
	}
	
	
	@Override
	public BoolExpr toZ3(Context ctx, /*IBooleanNetwork booleanNetwork,*/ HashMap<String, ISpecies> nodeNameToNode,
			HashMap<ISpecies, Expr> nodeToTruthValue,boolean realSort) throws Z3Exception {
		return ctx.mkTrue();
	}
	
	@Override
	public IUpdateFunction cloneReplacingWithRepresentative(IPartition partition,
			LinkedHashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,
			HashMap<String, ISpecies> speciesNameToOriginalSpecies) {
		return new TrueUpdateFunction();
	}
	
	@Override
	public IUpdateFunction cloneReplacingNorRepresentativeWithNeutral(IPartition partition,
			LinkedHashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,
			HashMap<String, ISpecies> speciesNameToOriginalSpecies, FBEAggregationFunctions aggregationFunction, IBooleanNetwork bn) {
		return new TrueUpdateFunction();
	}
	
	@Override
	public boolean seemsInputSpecies(String sp) {
		return true;
	}
	@Override
	public void dropNonOutputSpecies(String sp, HashSet<String> guessedOutputs) {
		//do nothing
	}
	
	@Override
	public List<IMonomial> toPolynomial(HashMap<String, ISpecies> speciesNameToSpecies) throws Z3Exception {
		IMonomial one = new NumberMonomial(BigDecimal.ONE, "1");
		List<IMonomial> monomials = new ArrayList<>(1);
		monomials.add(one);
		return monomials;
	}
		
}
