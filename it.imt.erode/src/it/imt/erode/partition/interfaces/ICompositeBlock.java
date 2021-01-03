package it.imt.erode.partition.interfaces;

import java.util.Collection;

import it.imt.erode.crn.interfaces.IComposite;

public interface ICompositeBlock {

	public Collection<IComposite> getComposites();

	public void addComposite(IComposite composite);

	public boolean isEmpty();

	public int size();

	public void setToBeRefined(boolean toBeRefined);
	
	public boolean getToBeRefined();

	void setCreatedAtStep(int step);

	int getCreatedAtStep();

	ICompositeBlock getPrev();

	ICompositeBlock getNext();

	void setPrev(ICompositeBlock block);

	void setNext(ICompositeBlock block);
	
}
