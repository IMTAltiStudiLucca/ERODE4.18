package it.imt.erode.partitionrefinement.splittersbstandcounters;

import java.util.ArrayList;

import it.imt.erode.expression.parser.IMonomial;

public class CoefficientToMonomialsPosNeg {

	private final VectorOfCoefficientsForEachMonomial vPos, vNeg;
	
	public CoefficientToMonomialsPosNeg(VectorOfCoefficientsForEachMonomial vPos, VectorOfCoefficientsForEachMonomial vNeg) {
		this.vPos=vPos;
		this.vNeg=vNeg;
	}
	
	public CoefficientToMonomialsPosNeg(VectorOfCoefficientsForEachMonomial v) {
		CoefficientToMonomialsPosNeg vposAndNeg = v.splitPositiveAndNegativeCoefficients();
		this.vPos=vposAndNeg.vPos;
		this.vNeg=vposAndNeg.vNeg;
	}
	
	public CoefficientToMonomialsPosNeg(ArrayList<IMonomial> monomials) {
		this(new VectorOfCoefficientsForEachMonomial(monomials));
	}
	
	public CoefficientToMonomialsPosNeg() {
		this.vPos=new VectorOfCoefficientsForEachMonomial();
		this.vNeg=new VectorOfCoefficientsForEachMonomial();
	}

	public VectorOfCoefficientsForEachMonomial getPositiveMonomials() {
		return vPos;
	}
	public VectorOfCoefficientsForEachMonomial getNegativeMonomials() {
		return vNeg;
	}
	
	@Override
	public String toString() {
		return "\n + ("+vPos.toString() +")\n - ( "+vNeg.toString()+" )\n";
	}
}
