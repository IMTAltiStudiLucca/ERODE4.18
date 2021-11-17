package it.imt.erode.crn.symbolic.constraints;

import java.io.BufferedWriter;
import java.util.HashMap;

import org.eclipse.ui.console.MessageConsoleStream;
import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.text.parser.ParseException;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Z3Exception;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.partitionrefinement.algorithms.SMTExactFluidBisimilarity;

public class BasicConstraint implements IConstraint {
	
	private String symbolicParameter;
	//private IConstraint rhs;
	private BasicConstraintComparator comp;
	//private HashMap<String, ArithExpr> symbParNameToSymbParZ3;
	private ASTNode expression;
	private String expressionString;
	
	public BasicConstraint(String symbolicParameter, String expr, BasicConstraintComparator comp, MessageConsoleStream out,BufferedWriter bwOut) {
		super();
		this.symbolicParameter = symbolicParameter;
		this.expressionString=expr;
		try {
			this.expression = ASTNode.parseFormula(expr);
		} catch (ParseException e) {
			CRNReducerCommandLine.printStackTrace(out,bwOut, e);
		}
		this.comp = comp;
	}

	public static String getMathSymbol(BasicConstraintComparator c) {
		switch (c) {
		case EQ:
			return "=";
		case GT:
			return ">";
		case GEQ:
			return ">=";
		case LT:
			return "<";
		case LEQ:
			return "<=";
		case NOTEQ:
			return "=/=";
		default:
			throw new UnsupportedOperationException(c.toString());
		}
	}

	@Override
	public String toString() {
		return "(" + symbolicParameter + " "+ getMathSymbol(comp) +" " + expressionString + ")";
	}
	
	@Override
	public BoolExpr toZ3(Context ctx, HashMap<String, ArithExpr> symbParNameToSymbParZ3, ICRN crn, HashMap<String, ISpecies> speciesNameToSpecies, HashMap<ISpecies, ArithExpr> speciesToPopulation) throws Z3Exception {
		//take the z3 version of the symbolic parameter 
		ArithExpr lhsZ3 = symbParNameToSymbParZ3.get(symbolicParameter);
		//ArithExpr rhsz3 = ctx.mkReal(5);
		ArithExpr rhsz3 = SMTExactFluidBisimilarity.computeArbitraryz3RateExpression(ctx,speciesToPopulation,symbParNameToSymbParZ3,expression,null,speciesNameToSpecies,crn.getMath(),null);
		
		
		switch (comp) {
		case EQ:
			return ctx.mkEq(lhsZ3, rhsz3);
		case GT:
			return ctx.mkGt(lhsZ3, rhsz3);
		case GEQ:
			return ctx.mkGe(lhsZ3, rhsz3);
		case LT:
			return ctx.mkLt(lhsZ3, rhsz3);
		case LEQ:
			return ctx.mkLe(lhsZ3, rhsz3);
		case NOTEQ:
			return ctx.mkNot(ctx.mkEq(lhsZ3, rhsz3));
		default:
			throw new UnsupportedOperationException(comp.toString());
		}
	}


}
