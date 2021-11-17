package it.imt.erode.booleannetwork.updatefunctions;

import java.util.HashMap;
import java.util.LinkedHashMap;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Z3Exception;

import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.algorithms.booleannetworks.FBEAggregationFunctions;

public class BinaryExprIUpdateFunction implements IUpdateFunction/*_ArithExprRefToNode_Value*/ {

	private IUpdateFunction/*_ArithExprRefToNode_Value*/ first;
	private IUpdateFunction/*_ArithExprRefToNode_Value*/ second;
	private ArithmeticConnector op;
	
	public BinaryExprIUpdateFunction(IUpdateFunction/*_ArithExprRefToNode_Value*/ first,IUpdateFunction/*_ArithExprRefToNode_Value*/ second,ArithmeticConnector op) {
		this.first=first;
		this.second=second;
		this.op=op;
	}
	
	public IUpdateFunction getFirst() {
		return first;
	}
	public IUpdateFunction getSecond() {
		return second;
	}
	public ArithmeticConnector getOperator() {
		return op;
	}
	
	@Override
	public String toString() {
		return "("+first.toString()+getSymbol(op)+second.toString()+")";
	}

	private static String getSymbol(ArithmeticConnector op) {
		switch (op) {
		case SUM:
			return " + ";
		case MUL:
			return " * ";
		default:
			throw new UnsupportedOperationException(op.toString());
		}
	}

	@Override
	public ArithExpr toZ3(Context ctx, /*IBooleanNetwork booleanNetwork,*/ HashMap<String, ISpecies> nodeNameToNode,
			HashMap<ISpecies, Expr> nodeToTruthValue) throws Z3Exception {
		switch (op) {
		case SUM:
			return ctx.mkAdd(new ArithExpr[] {(ArithExpr)first.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue),(ArithExpr)second.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue)});
		case MUL:
			return ctx.mkMul(new ArithExpr[] {(ArithExpr)first.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue),(ArithExpr)second.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue)});
		default:
			throw new UnsupportedOperationException(op.toString());
		}
	}
	
	@Override
	public IUpdateFunction cloneReplacingWithRepresentative(IPartition partition,
			LinkedHashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,HashMap<String, ISpecies> speciesNameToOriginalSpecies) {
		IUpdateFunction firstCloned = first.cloneReplacingWithRepresentative(partition, correspondenceBlock_ReducedSpecies,speciesNameToOriginalSpecies);
		IUpdateFunction secondCloned = second.cloneReplacingWithRepresentative(partition, correspondenceBlock_ReducedSpecies,speciesNameToOriginalSpecies);
		return new BinaryExprIUpdateFunction(firstCloned,secondCloned, op);
	}

	@Override
	public IUpdateFunction cloneReplacingNorRepresentativeWithNeutral(IPartition partition,
			LinkedHashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,
			HashMap<String, ISpecies> speciesNameToOriginalSpecies, FBEAggregationFunctions aggregationFunction) {
		IUpdateFunction firstCloned = first.cloneReplacingNorRepresentativeWithNeutral(partition, correspondenceBlock_ReducedSpecies,speciesNameToOriginalSpecies,aggregationFunction);
		IUpdateFunction secondCloned = second.cloneReplacingNorRepresentativeWithNeutral(partition, correspondenceBlock_ReducedSpecies,speciesNameToOriginalSpecies,aggregationFunction);
		return new BinaryExprIUpdateFunction(firstCloned,secondCloned, op);
	}
	
	@Override
	public boolean seemsInputSpecies(String sp) {
		return false;
	}

}
