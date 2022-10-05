package it.sssa.erode.or_tools;

import java.util.HashMap;

import it.imt.erode.onthefly.Pair;

public class StringAndPairs {

	HashMap<Pair,Double> pairs;
	String msg;
	public StringAndPairs(HashMap<Pair, Double> pairs, String msg) {
		super();
		this.pairs = pairs;
		this.msg = msg;
	}
	public HashMap<Pair, Double> getPairs() {
		return pairs;
	}
	public String getMsg() {
		return msg;
	}
	
	
}
