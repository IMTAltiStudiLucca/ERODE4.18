package it.imt.erode.booleannetwork.updatefunctions;


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
import it.imt.erode.expression.parser.Monomial;
import it.imt.erode.expression.parser.ProductMonomial;
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
		if(innerUpdateFunction instanceof ReferenceToNodeUpdateFunction ||innerUpdateFunction instanceof ValUpdateFunction) {
			return "!"+innerUpdateFunction.toString();
		}
		else {
			return "(!"+innerUpdateFunction.toString()+")";
		}
		
	}

	@Override
	public BoolExpr toZ3(Context ctx, /*IBooleanNetwork booleanNetwork,*/ HashMap<String, ISpecies> nodeNameToNode,
			HashMap<ISpecies, Expr> nodeToTruthValue,boolean realSort) throws Z3Exception {
		return ctx.mkNot((BoolExpr)innerUpdateFunction.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue,realSort));
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
			HashMap<String, ISpecies> speciesNameToOriginalSpecies, FBEAggregationFunctions aggregationFunction, IBooleanNetwork bn) {
		IUpdateFunction innerCloned = innerUpdateFunction.cloneReplacingNorRepresentativeWithNeutral(partition, correspondenceBlock_ReducedSpecies,speciesNameToOriginalSpecies,aggregationFunction,bn);
		return new NotBooleanUpdateFunction(innerCloned);
	}
	
	@Override
	public boolean seemsInputSpecies(String sp) {
		return false;
	}
	
	@Override
	public void dropNonOutputSpecies(String sp, HashSet<String> guessedOutputs) {
		innerUpdateFunction.dropNonOutputSpecies(sp, guessedOutputs);
	}
	
	@Override
	public List<IMonomial> toPolynomial(HashMap<String, ISpecies> speciesNameToSpecies) throws Z3Exception {
		//1 - p_{inner}
		List<IMonomial> innerMons = innerUpdateFunction.toPolynomial(speciesNameToSpecies);
		List<IMonomial> monomials = new ArrayList<>(innerMons.size()+1);
		monomials.add(Monomial.oneMon);
		for(IMonomial innerMon : innerMons){
			IMonomial minusInnerMon= new ProductMonomial(Monomial.minusOneMon, innerMon);
			monomials.add(minusInnerMon);
		}
		return monomials;
	}
			
}
