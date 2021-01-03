package it.imt.erode.crn.label;

import java.util.HashMap;

import it.imt.erode.crn.implementations.Composite;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;

public class NAryBlockLabel implements ILabel {

	private final IComposite blockLabel;
	private final int referredArity;
	private final int hc;
	
	public NAryBlockLabel(HashMap<ISpecies, Integer> blockLabelHM,int referredArity) {
		this.blockLabel=new Composite(blockLabelHM);
		this.referredArity=referredArity;
		this.hc=computeHashCode();
	}

	private int computeHashCode() {
		if(getReferredArity()==1){
			return EmptySetLabel.EMPTYSETLABEL.hashCode();
		}
		else if(getReferredArity()==2){
			return blockLabel.getAllSpecies(0).hashCode();
		}
		else{
			return blockLabel.hashCode();
		}
	}
	
	@Override
	public int hashCode() {
		return hc;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if(((ILabel)obj).getReferredArity()!=getReferredArity()){
			return false;
		}
		if(obj instanceof ISpecies){
			if(getReferredArity()==2){
				return blockLabel.getAllSpecies(0).equals(obj);
			}
			else{
				return false;
			}
		}
		
		//Otherwise it is another NAryBlockLabel
		NAryBlockLabel other = (NAryBlockLabel) obj;
		if(hashCode()!=other.hashCode()){
			return false;
		}
		return blockLabel.equals(other.getComposite());
	}

	private IComposite getComposite() {
		// TODO Auto-generated method stub
		return blockLabel;
	}

	@Override
	public int getReferredArity() {
		return referredArity;
	}

	@Override
	public int getLabelID() {
		if(getReferredArity()==1){
			return EmptySetLabel.EMPTYSETLABEL.getLabelID();
		}
		else if(getReferredArity()==2){
			return blockLabel.getAllSpecies(0).getLabelID();
		}
		else {
			throw new UnsupportedOperationException("We can assign a label only to empty or unary labels (i.e., which refer to unarty or binary reactions)");
		}
	}

	public int getLabelIDOfFirstSpecies() {
		return blockLabel.getAllSpecies(0).getLabelID();
	}

	public int deepCompare(NAryBlockLabel bl2) {
		return getComposite().compareTo(bl2.getComposite());
	}
	
	@Override
	public String toString() {
		return getComposite().toString();
	}

}
