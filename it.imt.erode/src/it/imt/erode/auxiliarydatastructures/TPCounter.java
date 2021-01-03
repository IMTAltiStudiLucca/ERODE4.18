package it.imt.erode.auxiliarydatastructures;

public class TPCounter {

	private int tpSolved;
	private int tpUnbalancedSkept;
	
	public TPCounter() {
		tpSolved=0;
		tpUnbalancedSkept=0;
	}
	
//	public void setValue(int value) {
//		this.value = value;
//	}
	
	public int getTPUnbalancedSkept() {
		return tpUnbalancedSkept;
	}
	
	public void increaseTPUnbalancedSkept(){
		tpUnbalancedSkept++;
	}
	
	public int getTPSolved() {
		return tpSolved;
	}
	
	public void increaseTPSolved(){
		tpSolved++;
	}
	
	@Override
	public String toString() {
		return String.valueOf(tpSolved);
	}
}
