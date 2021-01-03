package it.imt.erode.onthefly;

import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;

public class Pair implements Comparable<Object>{

	private IComposite first,second;
	private boolean monomialPair;

	public Pair(IComposite first, IComposite second) {
		super();
		this.first = first;
		this.second = second;
		monomialPair = !(first.isUnary() && second.isUnary());
	}
	public Pair(ISpecies first, ISpecies second) {
		this((IComposite)first,(IComposite)second);
	}

	public boolean isMonomialPair() {
		return monomialPair;
	}
	public boolean isSpeciesPair() {
		return !monomialPair;
	}
	
	public IComposite getFirst() {
		return first;
	}

	public IComposite getSecond() {
		return second;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Pair other = (Pair) obj;
		if (first == null) {
			if (other.first != null)
				return false;
		} else if (!first.equals(other.first))
			return false;
		if (second == null) {
			if (other.second != null)
				return false;
		} else if (!second.equals(other.second))
			return false;
		return true;
	}

	/**
	 * A lexicographical ordering on first and second
	 * @param o
	 * @return
	 */
	@Override
	public int compareTo(Object o) {
		if(o instanceof Pair) {
			int cmp = first.compareTo(((Pair) o).first);
			if(cmp!=0) {
				return cmp;
			}
			else {
				return second.compareTo(((Pair) o).second);
			}
		}

		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return "("+first.toString()+","+second.toString()+")";
	}
	
}
