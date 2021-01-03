package it.imt.erode.tests;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;


public class TestBigDecimal {

	public static void main(String[] args) {
		/*double aaa = 1.1234567890123456;
		Double truncatedDouble = BigDecimal.valueOf(aaa)
			    .setScale(3, RoundingMode.HALF_UP)
			    .doubleValue();*/
		
//		double d = 30.1*6.0;
//		BigDecimal dbd = BigDecimal.valueOf(30.1).multiply(BigDecimal.valueOf(6.0));
//		double d2 = dbd.doubleValue();
//		
//		MathEval math = new MathEval();
//		d=math.evaluate("30.1*6.0");
//		d2=math.evaluate("30.1/6.0");
		
		
		BigDecimal a =new BigDecimal("2.00");
		BigDecimal b =new BigDecimal("2.0000");
		HashMap<BigDecimal, String> ab = new HashMap<BigDecimal, String>();
		ab.put(a, "a");
		ab.put(b, "b");
		
		HashMap<Double, String> abd = new HashMap<Double, String>();
		abd.put(a.doubleValue(), "ad");
		abd.put(b.doubleValue(), "bd");
		
		// TODO Auto-generated method stub
		//BigDecimal bd = new BigDecimal(19.5294233579994771060);
		BigDecimal bd = new BigDecimal(-16.118095650958320419);
		System.out.println(bd);
		//bd = bd.round(new MathContext(5, RoundingMode.FLOOR));
		//19.5294233579994771060 19.529
		//CRNReducerCommandLine.println(bd);
		//CRNReducerCommandLine.println(bd.precision());
		bd=bd.setScale(5, RoundingMode.HALF_DOWN);
		//bd.setScale(5);
		System.out.println(bd);
		
		//prev: -16.118095650958320419, now: -16.118095
		
		BigDecimal bd1 = new BigDecimal(-16.11803333);
		BigDecimal bd2 = new BigDecimal(-16.11813333);
		int scale = 4;
		bd1=bd1.setScale(scale, RoundingMode.HALF_DOWN);
		bd2=bd2.setScale(scale, RoundingMode.HALF_DOWN);
		BigDecimal epsilon = new BigDecimal("1E-"+scale);
		
		BigDecimal diff = bd1.subtract(bd2);
		if(diff.abs().compareTo(epsilon)<=0){
			System.out.println("equal");
		}
		else{
			System.out.println("different");
		}

	}

}
