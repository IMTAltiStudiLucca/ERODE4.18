package it.imt.erode.booleannetwork.updatefunctions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Z3Exception;

import it.imt.erode.booleannetwork.interfaces.IBooleanNetwork;
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
		if(op.equals(ArithmeticConnector.SUM)||op.equals(ArithmeticConnector.MUL)) {
			return "("+first.toString()+getSymbol(op)+second.toString()+")";
		}
		else {
			return getSymbol(op)+"("+first.toString()+","+second.toString()+")";
		}
		
	}

	private static String getSymbol(ArithmeticConnector op) {
		switch (op) {
		case SUM:
			return " + ";
		case MUL:
			return " * ";
		case MIN:
			return "min";
		case MAX:
			return "max";	
		default:
			throw new UnsupportedOperationException(op.toString());
		}
	}

	@Override
	public ArithExpr toZ3(Context ctx, /*IBooleanNetwork booleanNetwork,*/ HashMap<String, ISpecies> nodeNameToNode,
			HashMap<ISpecies, Expr> nodeToTruthValue) throws Z3Exception {
		ArithExpr f=(ArithExpr)first.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue);
		ArithExpr s=(ArithExpr)second.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue);
		switch (op) {
		case SUM:
			return ctx.mkAdd(new ArithExpr[] {f,s});
		case MUL:
			return ctx.mkMul(new ArithExpr[] {f,s});
		case MIN:
			return (ArithExpr)ctx.mkITE(ctx.mkLt(f, s), f, s);
		case MAX:
			return (ArithExpr)ctx.mkITE(ctx.mkGt(f, s), f, s);	
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
			HashMap<String, ISpecies> speciesNameToOriginalSpecies, FBEAggregationFunctions aggregationFunction
			, IBooleanNetwork bn) {
		IUpdateFunction firstCloned = first.cloneReplacingNorRepresentativeWithNeutral(partition, correspondenceBlock_ReducedSpecies,speciesNameToOriginalSpecies,aggregationFunction,bn);
		IUpdateFunction secondCloned = second.cloneReplacingNorRepresentativeWithNeutral(partition, correspondenceBlock_ReducedSpecies,speciesNameToOriginalSpecies,aggregationFunction,bn);
		return new BinaryExprIUpdateFunction(firstCloned,secondCloned, op);
	}
	
	@Override
	public boolean seemsInputSpecies(String sp) {
		return false;
	}
	@Override
	public void dropNonOutputSpecies(String sp, HashSet<String> guessedOutputs) {
		first.dropNonOutputSpecies(sp, guessedOutputs);
		second.dropNonOutputSpecies(sp, guessedOutputs);
	}

}
