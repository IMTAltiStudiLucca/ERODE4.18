package it.imt.erode.crn.label;

import it.imt.erode.crn.implementations.Species;

public class SimpleLabel implements ILabel {
	
	private String name;
	
	private SimpleLabel(String name){
		super();
		this.name=name;
	}
	
	public static final SimpleLabel SOURCELABEL=new SimpleLabel("Source");
	public static final SimpleLabel TARGETLABEL=new SimpleLabel("Target");
	
	@Override
	public String toString(){
		return name;
	}

	@Override
	public int getReferredArity() {
		return 1;
	}

	@Override
	public int getLabelID() {
		return -1;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null)
			return false;
		if(o instanceof Species){
			return false;
		}
		/*else if(o instanceof Composite){
			Composite other = (Composite) o;
			if(other.getAllSpecies().length==0){
				return true;
			}
			else{
				return false;
			}
		}*/
		else if(o instanceof SimpleLabel){
			return name.compareTo(((SimpleLabel) o).name)==0;
		}
		//System.out.println("o.getClass(): "+o.getClass());
		throw new UnsupportedOperationException();
	}
	
	

}
