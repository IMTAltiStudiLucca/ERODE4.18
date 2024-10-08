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
import it.imt.erode.crn.symbolic.constraints.BooleanConnector;
import it.imt.erode.expression.parser.IMonomial;
import it.imt.erode.expression.parser.Monomial;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.algorithms.booleannetworks.FBEAggregationFunctions;

public class BooleanUpdateFunctionExpr implements IUpdateFunction {
	//BooleanBinaryExprUpdateFunction
	
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
		//return first.toString()+getSymbol(op)+second.toString();
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
			HashMap<ISpecies, Expr> nodeToTruthValue,boolean realSort) throws Z3Exception {
		switch (op) {
		case AND:
			return ctx.mkAnd(new BoolExpr[] {(BoolExpr)first.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue,realSort),(BoolExpr)second.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue,realSort)});
		case IMPLIES:
			return ctx.mkImplies((BoolExpr)first.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue,realSort),(BoolExpr)second.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue,realSort));
		case OR:
			return ctx.mkOr(new BoolExpr[] {(BoolExpr)first.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue,realSort),(BoolExpr)second.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue,realSort)});
		case XOR:
			return ctx.mkXor((BoolExpr)first.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue,realSort),(BoolExpr)second.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue,realSort));
		case EQ:
			return ctx.mkEq(first.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue,realSort),second.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue,realSort));
		case NEQ:
			return ctx.mkNot(ctx.mkEq(first.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue,realSort),second.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue,realSort)));						
		default:
			throw new UnsupportedOperationException(op.toString());
		}
	}
	
	
	@Override
	public List<IMonomial> toPolynomial(HashMap<String, ISpecies> speciesNameToSpecies) throws Z3Exception {
		List<IMonomial> leftMonomials=this.first.toPolynomial(speciesNameToSpecies);
		List<IMonomial> rightMonomials=this.second.toPolynomial(speciesNameToSpecies);
		List<IMonomial> retMonomials=null;
		switch (op) {
		case AND:
			//p_left * p_right
			retMonomials = polynomialOfAND(leftMonomials, rightMonomials);
			return retMonomials;
		case OR:
			//p_left + p_right - p_left * p_right
			retMonomials = polynomialOfOR(leftMonomials, rightMonomials);
			
			return retMonomials;
//		case IMPLIES:
//			return ctx.mkImplies((BoolExpr)first.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue,realSort),(BoolExpr)second.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue,realSort));	
//		case XOR:
//			return ctx.mkXor((BoolExpr)first.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue,realSort),(BoolExpr)second.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue,realSort));
//		case EQ:
//			return ctx.mkEq(first.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue,realSort),second.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue,realSort));
//		case NEQ:
//			return ctx.mkNot(ctx.mkEq(first.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue,realSort),second.toZ3(ctx,/*booleanNetwork,*/nodeNameToNode,nodeToTruthValue,realSort)));						
		default:
			throw new UnsupportedOperationException("Only supported for Boolean BNs. We found this operator: "+op.toString());
		}
		
	}

	public static List<IMonomial> polynomialOfAND(List<IMonomial> leftMonomials, List<IMonomial> rightMonomials) {
		List<IMonomial> retMonomials;
		retMonomials = Monomial.multiplyMonomials(leftMonomials, rightMonomials);
		return retMonomials;
	}

	public static List<IMonomial> polynomialOfOR(List<IMonomial> leftMonomials, List<IMonomial> rightMonomials) {
		List<IMonomial> retMonomials;
		retMonomials = new ArrayList<IMonomial>(leftMonomials.size()+rightMonomials.size()+leftMonomials.size()*rightMonomials.size());
		
		retMonomials.addAll(leftMonomials);
		retMonomials.addAll(rightMonomials);
		
		List<IMonomial> prodMonomials=Monomial.multiplyMonomials(leftMonomials, rightMonomials);
		
		List<IMonomial> minusOneList=new ArrayList<>(1);
		minusOneList.add(Monomial.minusOneMon);
		List<IMonomial> minusProdMonomials=Monomial.multiplyMonomials(minusOneList, prodMonomials);
		
		retMonomials.addAll(leftMonomials);
		retMonomials.addAll(rightMonomials);
		retMonomials.addAll(minusProdMonomials);
		return retMonomials;
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
			HashMap<String, ISpecies> speciesNameToOriginalSpecies, FBEAggregationFunctions aggregationFunction
			, IBooleanNetwork bn) {
		IUpdateFunction firstCloned = first.cloneReplacingNorRepresentativeWithNeutral(partition, correspondenceBlock_ReducedSpecies,speciesNameToOriginalSpecies,aggregationFunction,bn);
		IUpdateFunction secondCloned = second.cloneReplacingNorRepresentativeWithNeutral(partition, correspondenceBlock_ReducedSpecies,speciesNameToOriginalSpecies,aggregationFunction,bn);
		return new BooleanUpdateFunctionExpr(firstCloned, secondCloned, op);
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
