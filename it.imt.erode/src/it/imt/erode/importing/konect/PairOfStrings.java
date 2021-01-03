package it.imt.erode.importing.konect;


public class PairOfStrings implements Comparable<PairOfStrings>{
	private String first;
	private String second;
	
	public PairOfStrings(String first, String second) {
		this.first=first;
		this.second=second;
	}

	
	@Override
	public int compareTo(PairOfStrings o) {
		int cmpff = first.compareTo(o.first);
		if(cmpff==0) {
			return second.compareTo(o.second);
		}
		else if(first.equals(o.second)) {
			return second.compareTo(o.first);
		}
		else {
			return cmpff;
		}
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof PairOfStrings) {
			PairOfStrings other = (PairOfStrings)obj;
			if(first.equals(other.first)) {
				return second.equals(other.second);
			}
			else if(first.equals(other.second)) {
				return second.equals(other.first);
			}
			return false;
			//return this.compareTo((PairOfStrings)obj)==0;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return first.hashCode()+second.hashCode();
	}
}
