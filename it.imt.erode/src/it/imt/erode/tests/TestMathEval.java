package it.imt.erode.tests;

import java.math.BigDecimal;

import it.imt.erode.expression.evaluator.Expression;
import it.imt.erode.expression.evaluator.MathEval;

public class TestMathEval {

	public static void main(String[] args) {
		MathEval math = new MathEval();
		/*
		//System.out.println("resJava="+resJava+", resMathEval="+resMathEval);
		
		math.setVariable("lambda", 0.010999999999999996);
		
		math.setVariable("sX", 1.85);
		math.setVariable("sS", 8.149999999999999);
		double resMathEvalExpr=math.evaluate("sX/(lambda*sS)");
		System.out.println("resMathEvalExpr="+resMathEvalExpr);
		
		math.setVariable("sX", 2.180000000000001);
		math.setVariable("sS", 7.82);
		resMathEvalExpr=math.evaluate("sX/(lambda*sS)");
		System.out.println("resMathEvalExpr="+resMathEvalExpr);
		
		
		double resJava=2.1700000000000004/(0.010999999999999996*7.83);
		double resMathEval=math.evaluate("2.1700000000000004/(0.010999999999999996*7.83)");
		
		math.setVariable("sX", 2.1700000000000004);
		math.setVariable("sS", 7.83);
		
		resMathEvalExpr=math.evaluate("sX/(lambda*sS)");
		System.out.println("resJava="+resJava+", resMathEval="+resMathEval+", resMathEvalExpr="+resMathEvalExpr);
		*/
//		double res = math.evaluate("2^3");
//		System.out.println(res);
//		/*res = math.evaluate("+(2 3)");
//		System.out.println(res);*/
//		
//		res = math.evaluate("abs(-2)");
//		System.out.println(res);
//		
//		math.setVariable("K1", 2);
//		math.setVariable("k1", 1);
//		
//		//Iterable<Entry<String, Double>> vars = math.getVariables();
//		
//		System.out.println("k1 (1) = "+math.evaluate("k1"));
//		System.out.println("K1 (2) = "+math.evaluate("K1"));
		for(int i=1;i<200;i++){
			printSumOneOverN(i,math);
		}
		
		
		
	}
	
	static void printSumOneOverN(int n,MathEval math){
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<n;i++){
			sb.append("1/"+n);
			if(i<n-1){
				sb.append("+");
			}
		}
		String expr = sb.toString();
		BigDecimal bd = BigDecimal.valueOf(math.evaluate(expr));
		BigDecimal oneOvern = BigDecimal.valueOf(math.evaluate("1/"+n));
		System.out.println("n = "+n+"\n\t1/n = "+oneOvern.toPlainString()+"\n\tsum = "+bd.toPlainString());
		Expression expression = new Expression(expr);
		BigDecimal result = expression.eval();
		System.out.println("\t bd = "+result.toPlainString());
		System.out.println("\n");
		
	}

}
