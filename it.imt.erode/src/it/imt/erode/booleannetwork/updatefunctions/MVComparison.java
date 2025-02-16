package it.imt.erode.booleannetwork.updatefunctions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Z3Exception;

import it.imt.erode.booleannetwork.interfaces.IBooleanNetwork;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.crn.symbolic.constraints.BasicConstraint;
import it.imt.erode.crn.symbolic.constraints.BasicConstraintComparator;
import it.imt.erode.expression.parser.IMonomial;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.algorithms.booleannetworks.FBEAggregationFunctions;

public class MVComparison implements IUpdateFunction {

	private IUpdateFunction/*_ArithExprRefToNode_Value*/ left,right;
	private BasicConstraintComparator comp;
	
	public MVComparison(IUpdateFunction/*_ArithExprRefToNode_Value*/ left,IUpdateFunction/*_ArithExprRefToNode_Value*/ right,
			BasicConstraintComparator comp) {
		this.left=left;
		this.right=right;
		this.comp=comp;
	}
	
	public IUpdateFunction getLeft() {
		return left;
	}
	public IUpdateFunction getRight() {
		return right;
	}
	public BasicConstraintComparator getComp() {
		return comp;
	}
	
	@Override
	public String toString() {
		return "{"+left.toString()+" "+BasicConstraint.getMathSymbol(comp)+" "+right.toString()+"}";
	}
	
	@Override
	public BoolExpr toZ3(Context ctx, HashMap<String, ISpecies> speciesNameToSpecies,
			HashMap<ISpecies, Expr> speciesToSpeciesVariable, boolean realSort) throws Z3Exception {
		switch (comp) {
		case EQ:
			return ctx.mkEq(left.toZ3(ctx, speciesNameToSpecies, speciesToSpeciesVariable,realSort),right.toZ3(ctx, speciesNameToSpecies, speciesToSpeciesVariable,realSort));
		case GT:
			//return ">";
			return ctx.mkGt((ArithExpr)left.toZ3(ctx, speciesNameToSpecies, speciesToSpeciesVariable,realSort),(ArithExpr)right.toZ3(ctx, speciesNameToSpecies, speciesToSpeciesVariable,realSort));
		case GEQ:
			//return ">=";
			return ctx.mkGe((ArithExpr)left.toZ3(ctx, speciesNameToSpecies, speciesToSpeciesVariable,realSort),(ArithExpr)right.toZ3(ctx, speciesNameToSpecies, speciesToSpeciesVariable,realSort));
		case LT:
			//return "<";
			return ctx.mkLt((ArithExpr)left.toZ3(ctx, speciesNameToSpecies, speciesToSpeciesVariable,realSort),(ArithExpr)right.toZ3(ctx, speciesNameToSpecies, speciesToSpeciesVariable,realSort));
		case LEQ:
			//return "<=";
			return ctx.mkLe((ArithExpr)left.toZ3(ctx, speciesNameToSpecies, speciesToSpeciesVariable,realSort),(ArithExpr)right.toZ3(ctx, speciesNameToSpecies, speciesToSpeciesVariable,realSort));
		case NOTEQ:
			//return "=/=";
			return ctx.mkNot(ctx.mkEq(left.toZ3(ctx, speciesNameToSpecies, speciesToSpeciesVariable,realSort),right.toZ3(ctx, speciesNameToSpecies, speciesToSpeciesVariable,realSort)));
		default:
			throw new UnsupportedOperationException(comp.toString());
		}
	}

	@Override
	public IUpdateFunction cloneReplacingWithRepresentative(IPartition partition,
			LinkedHashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,
			HashMap<String, ISpecies> speciesNameToOriginalSpecies) {
		IUpdateFunction leftCloned= left.cloneReplacingWithRepresentative(partition, correspondenceBlock_ReducedSpecies, speciesNameToOriginalSpecies);
		IUpdateFunction rightCloned= right.cloneReplacingWithRepresentative(partition, correspondenceBlock_ReducedSpecies, speciesNameToOriginalSpecies);
		return new MVComparison(leftCloned, rightCloned, comp);
	}

	@Override
	public IUpdateFunction cloneReplacingNorRepresentativeWithNeutral(IPartition partition,
			LinkedHashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,
			HashMap<String, ISpecies> speciesNameToOriginalSpecies, FBEAggregationFunctions aggregationFunction, IBooleanNetwork bn) {
		IUpdateFunction leftCloned = left.cloneReplacingNorRepresentativeWithNeutral(partition, correspondenceBlock_ReducedSpecies, speciesNameToOriginalSpecies, aggregationFunction,bn);
		IUpdateFunction rightCloned= right.cloneReplacingNorRepresentativeWithNeutral(partition, correspondenceBlock_ReducedSpecies, speciesNameToOriginalSpecies, aggregationFunction,bn);
		return new MVComparison(leftCloned, rightCloned, comp);
	}
	
	@Override
	public boolean seemsInputSpecies(String sp) {
		return false;
	}
	@Override
	public void dropNonOutputSpecies(String sp, HashSet<String> guessedOutputs) {
		left.dropNonOutputSpecies(sp, guessedOutputs);
		right.dropNonOutputSpecies(sp, guessedOutputs);
	}
	@Override
	public List<IMonomial> toPolynomial(HashMap<String, ISpecies> speciesNameToSpecies) throws Z3Exception {
		throw new UnsupportedOperationException("Only supported for Boolean BNs");
	}

}
