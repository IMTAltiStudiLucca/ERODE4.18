package it.imt.erode.importing.sbml;

/**
 * 
 * @author Andrea Vandin
 *
 */
public class ReversibleReaction {

	private String forward;
	private String reverse;
	private boolean hasReverse;
	private String id;
	private String descr;
	
	public String getForward() {
		return forward;
	}
	public String getReverse() {
		return reverse;
	}
	public String getForwardWithID() {
		if(getID()!=null) {
			return getForward() +" ["+getID()+"]";
		}
		else {
			return getForward();
		}
	}
	public String getReverseWithID() {
		if(getID()!=null) {
			return getReverse() +" ["+getID()+"]";
		}
		else {
			return getReverse();
		}
	}
	public ReversibleReaction(String forward, String reverse) {
		super();
		this.forward = forward;
		this.reverse = reverse;
		if(reverse!=null) {
			hasReverse=true;
		}
		else {
			hasReverse=false;
		}
	}
	public ReversibleReaction(String forward) {
		super();
		this.forward = forward;
		hasReverse=false;
	}
	
	public boolean isRevervible() {
		return hasReverse;
	}
	public void setIDAndDescr(String id,String descr) {
		this.id=id;	
		this.descr=descr;
	}
	public String getID() {
		return id;
	}
	public String getDescr() {
		return descr;
	}
	
	
	
}
