package it.imt.erode.partition.implementations;

import java.util.ArrayList;
import java.util.Collection;

import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.partition.interfaces.ICompositeBlock;

public class CompositeBlock implements ICompositeBlock {

	private Collection<IComposite> composites;
	private boolean toBeRefined=false;
	private int createdAtStep=0;
	
	private ICompositeBlock next;
	private ICompositeBlock prev;
	
	@Override
	public ICompositeBlock getPrev() {
		return prev;
	}

	@Override
	public ICompositeBlock getNext() {
		return next;
	}

	@Override
	public void setPrev(ICompositeBlock block) {
		this.prev = block;
	}

	@Override
	public void setNext(ICompositeBlock block) {
		this.next = block;
	}

	
	public CompositeBlock(){
		composites=new ArrayList<IComposite>();
	}
	
	public CompositeBlock(IComposite composite) {
		this();
		addComposite(composite);
	}

	public CompositeBlock(Collection<IComposite> composites) {
		this.composites=composites;
	}

	@Override
	public Collection<IComposite> getComposites() {
		return composites;
	}

	@Override
	public void addComposite(IComposite composite) {
		composites.add(composite);
	}

	@Override
	public boolean isEmpty() {
		return composites.isEmpty();
	}

	@Override
	public int size() {
		return composites.size();
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder("\nSize: ");
		sb.append(size()+"\n");
		for (IComposite composite : getComposites()) {
			sb.append(composite.toString());
			sb.append(" ");
			sb.append("\n");
		}
		return sb.toString();
	}

	@Override
	public void setToBeRefined(boolean toBeRefined) {
		this.toBeRefined=toBeRefined;
	}

	@Override
	public boolean getToBeRefined() {
		return toBeRefined;
	}
	
	@Override
	public void setCreatedAtStep(int step) {
		this.createdAtStep=step;
	}

	@Override
	public int getCreatedAtStep() {
		return createdAtStep;
	}
	
}
