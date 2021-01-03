package it.imt.erode.decode;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class StateSet extends HashMap<StateId, StateId> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public StateSet() {
		super();
	}

	public StateSet(int initialCapacity) {
		super(initialCapacity);
		// TODO Auto-generated constructor stub
	}

	public StateSet(Map<? extends StateId, ? extends StateId> m) {
		super(m);
		// TODO Auto-generated constructor stub
	}

	public StateSet(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
		// TODO Auto-generated constructor stub
	}
	
	public void add(StateId s){
		this.put(s, s);
	}
	
	public void addAll(Collection<? extends StateId> c){
		for(StateId s: c)
			this.add(s);
	}

}
