package it.imt.erode.crn.label;

import it.imt.erode.crn.implementations.Species;

public class EmptySetLabel implements ILabel {
	
	private EmptySetLabel(){
		super();
	}
	
	public static final EmptySetLabel EMPTYSETLABEL=new EmptySetLabel();
	
	@Override
	public String toString(){
		return "emptyLabel";
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
		else if(o instanceof EmptySetLabel){
			return true;
		}
		//System.out.println("o.getClass(): "+o.getClass());
		throw new UnsupportedOperationException();
	}
	
	

}
