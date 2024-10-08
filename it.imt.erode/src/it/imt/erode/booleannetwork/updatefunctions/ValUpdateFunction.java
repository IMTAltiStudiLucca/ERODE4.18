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

public class ValUpdateFunction implements IUpdateFunction/*_ArithExprRefToNode_Value*/ {

	private double val;
	
	public ValUpdateFunction(double val) {
		this.val=val;
	}
	
	public double getVal() {
		return val;
	}

	@Override
	public String toString() {
		return String.valueOf(val);
	}
	
	
	@Override
	public Expr toZ3(Context ctx, /*IBooleanNetwork booleanNetwork,*/ HashMap<String, ISpecies> nodeNameToNode,
			HashMap<ISpecies, Expr> nodeToTruthValue,boolean realSort) throws Z3Exception {
		if(realSort) {
			return ctx.mkReal(val+"");
		}
		else {
			return ctx.mkInt((int)val);
		}
		//
		
		//return ctx.mkNumeral(val,ctx.getIntSort());
	}
	
	@Override
	public IUpdateFunction cloneReplacingWithRepresentative(IPartition partition,
			LinkedHashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,
			HashMap<String, ISpecies> speciesNameToOriginalSpecies) {
		return new ValUpdateFunction(val);
	}
	
	@Override
	public IUpdateFunction cloneReplacingNorRepresentativeWithNeutral(IPartition partition,
			LinkedHashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,
			HashMap<String, ISpecies> speciesNameToOriginalSpecies, FBEAggregationFunctions aggregationFunction, IBooleanNetwork bn) {
		return new ValUpdateFunction(val);
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
		throw new UnsupportedOperationException("Only supported for Boolean BNs");
	}
}
