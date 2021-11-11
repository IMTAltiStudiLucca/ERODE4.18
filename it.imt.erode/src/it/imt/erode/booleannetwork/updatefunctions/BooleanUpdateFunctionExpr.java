package it.imt.erode.booleannetwork.updatefunctions;

import java.util.HashMap;
import java.util.LinkedHashMap;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Z3Exception;

import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.crn.symbolic.constraints.BooleanConnector;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.algorithms.booleannetworks.FBEAggregationFunctions;

public class BooleanUpdateFunctionExpr implements IUpdateFunction {

	private IUpdateFunction first;
	private IUpdateFunction second;
	private BooleanConnector op;
	
	public BooleanUpdateFunctionExpr(IUpdateFunction first, IUpdateFunction second,
			BooleanConnector op) {
		super();
		this.first = first;
		this.second = second;
		this.op = op;  
	}
	
	public IUpdateFunction getFirst() {
		return first;
	}
	public IUpdateFunction getSecond() {
		return second;
	}
	public BooleanConnector getOperator() {
		return op;
	}
	
	@Override
	public String toString() {
		return "("+first.toString()+getSymbol(op)+second.toString()+")";
	}

	private static String getSymbol(BooleanConnector op) {
		switch (op) {
		case AND:
			return "&";//" /\\ ";
		case IMPLIES:
			return " -> ";
		case OR:
			return " | ";//" \\/ ";
		case XOR:
			return " XOR ";
		case EQ:
			return " = ";
		case NEQ:
			return " != ";
		default:
			throw new UnsupportedOperationException(op.toString());
		}
	}

	@Override
	public BoolExpr toZ3(Context ctx, /*IBooleanNetwork booleanNetwork,*/ HashMap<String, ISpecies> nodeNameToNode,
			HashMap<ISpecies, BoolExpr> nodeToTruthValue) throws Z3Exception {
		switch (op) {
		case AND:
			return ctx.mkAnd(new BoolExpr[] {first.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue),second.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue)});
		case IMPLIES:
			return ctx.mkImplies(first.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue),second.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue));
		case OR:
			return ctx.mkOr(new BoolExpr[] {first.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue),second.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue)});
		case XOR:
			return ctx.mkXor(first.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue),second.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue));
		case EQ:
			return ctx.mkEq(first.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue),second.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue));
		case NEQ:
			return ctx.mkNot(ctx.mkEq(first.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue),second.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue)));						
		default:
			throw new UnsupportedOperationException(op.toString());
		}
	}
	
	@Override
	public IUpdateFunction cloneReplacingWithRepresentative(IPartition partition,
			LinkedHashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,HashMap<String, ISpecies> speciesNameToOriginalSpecies) {
		IUpdateFunction firstCloned = first.cloneReplacingWithRepresentative(partition, correspondenceBlock_ReducedSpecies,speciesNameToOriginalSpecies);
		IUpdateFunction secondCloned = second.cloneReplacingWithRepresentative(partition, correspondenceBlock_ReducedSpecies,speciesNameToOriginalSpecies);
		return new BooleanUpdateFunctionExpr(firstCloned, secondCloned, op);
	}

	@Override
	public IUpdateFunction cloneReplacingNorRepresentativeWithNeutral(IPartition partition,
			LinkedHashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,
			HashMap<String, ISpecies> speciesNameToOriginalSpecies, FBEAggregationFunctions aggregationFunction) {
		IUpdateFunction firstCloned = first.cloneReplacingNorRepresentativeWithNeutral(partition, correspondenceBlock_ReducedSpecies,speciesNameToOriginalSpecies,aggregationFunction);
		IUpdateFunction secondCloned = second.cloneReplacingNorRepresentativeWithNeutral(partition, correspondenceBlock_ReducedSpecies,speciesNameToOriginalSpecies,aggregationFunction);
		return new BooleanUpdateFunctionExpr(firstCloned, secondCloned, op);
	}
}
