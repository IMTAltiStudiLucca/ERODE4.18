package it.imt.erode.decode;
import java.util.Arrays;

public class StateId {
	
	private int[] id;
	
	/**
	 * number of identical permutations for reactions, 
	 * id number for decomp states
	 */
	private int number;

	/**
	 * creates empty state
	 * @param size
	 */
	public StateId(int size) {
		this.id = new int[size];
		this.number = 1;
	}
	
	/**
	 * creates state with one element
	 * @param size
	 * @param elem position of 1
	 */
	public StateId(int size, int elem) {
		this(size);
		this.id[elem] = 1;
	}
	
	/**
	 * creates state with one species at specified number
	 * @param size
	 * @param elem
	 * @param number
	 */
	public StateId(int size, int elem, int number) {
		this(size);
		this.id[elem] = number;
	}
	
	/**
	 * creates state with given id
	 * @param id
	 */
	public StateId(int[] id) {
		this.id = id;
		this.number = 1;
	}

	public int[] get() {
		return id;
	}
	
	public int get(int i){
		return id[i];
	}

	void setStates(int[] id) throws Exception {
		if(id.length == this.id.length) this.id = id;
		else throw new Exception("Wrong identifier length.");
	}
	
	/**
	 * creates new StateId as combination/addition of this and another one.
	 * Values of permutation field are multiplied.
	 * Does not change the value of this Object, returns the new StateId.
	 * 
	 * @param state2
	 * @return
	 * @throws Exception
	 */
	public StateId add(StateId state2) throws IndexOutOfBoundsException{
		int[] id2 = state2.get();
		StateId neu = this.add(id2);
		neu.setNumber(this.number*state2.getNumber());
		return neu;
	}
	
	/**
	 * creates new StateId as combination/addition of this and an id array.
	 * Does not change the value of this Object, returns the new StateId.
	 * 
	 * @param state2
	 * @return
	 * @throws Exception
	 */
	public StateId add(int[] id2) throws IndexOutOfBoundsException{
		if(id2.length != id.length) throw new IndexOutOfBoundsException("Length of state identifiers doesn't match!");
		int[] newId = new int[id.length];
		for(int i = 0; i<id.length; i++){
			newId[i] = this.id[i] + id2[i];
		}
		StateId neu = new StateId(newId);
		neu.setNumber(this.number);
		return neu;
	}
	
	/**
	 * creates new StateId equaling this one with a modification of one specified value.
	 * Does not change the value of this Object, returns the new StateId.
	 * 
	 * @param pos
	 * @param val
	 * @return
	 * @throws Exception
	 */
	public StateId add(int pos, int val) throws IndexOutOfBoundsException{
		if(pos >= id.length) throw new IndexOutOfBoundsException("Length of state identifiers doesn't match!");
		StateId neu = this.copy();
		neu.change(pos, val);
		neu.setNumber(this.getNumber());
		return neu;
	}
	
	/**
	 * copy to be safe for modification/call by reference
	 * 
	 * @return
	 */
	public StateId copy(){
		int[] newId = new int[id.length];
		for(int i = 0; i<id.length; i++){
			newId[i] = this.id[i];
		}
		StateId neu = new StateId(newId);
		neu.setNumber(this.getNumber());
		return neu;
	}
	
	
	/**
	 * returns state that results from changing one unit ed to one unit prod.
	 * Does not modify input.
	 * @param ed
	 * @param prod
	 */
	public StateId transform(int ed, int prod){
		int[] newId = new int[id.length];
		for(int i = 0; i<id.length; i++){
			if(i==ed) newId[i] = this.id[i]-1;
			else if(i==prod) newId[i] = this.id[i]+1;
			else newId[i] = this.id[i];
		}
		StateId neu = new StateId(newId);
		return neu;
	}
	
	/**
	 * changes the id of this object at the given place by the given number
	 * @param species
	 * @param number
	 */
	public void change(int species, int number){
		this.id[species] = this.id[species]+number;
	}
	
	/**
	 * sets the id of this object at the given place to the given number
	 * @param species
	 * @param number
	 */
	public void set(int species, int number){
		this.id[species] = number;
	}
	
	/**
	 * Checks if this state can change by performing a reaction.
	 * 
	 * 
	 * @param effect. Has to be same size as this object, i.e. same total number of species
	 * @return 
	 */
	public boolean canReact(int[] effect) {
		
		for(int i = 0; i<effect.length; i++){
			if(this.id[i]+effect[i]<0)
				return false;
		}
		return true;
			
	}
	
	/**
	 * Checks if this state can change by performing a reaction.
	 * 
	 * 
	 * @param effect. Has to be same size as this object, i.e. same total number of species
	 * @return 
	 */
	public boolean canReact(StateId effect){
		return canReact(effect.get());
	}
	
	public boolean equals(StateId id2){
		return this.equals(id2.get());
	}
	
	public boolean equals(int[] id2){
		if(id.length != id2.length)
			return false;
		for(int i = 0; i<this.id.length; i++){
			if(id[i]!=id2[i]) return false;
		}
		return true;
	}
	
	@Override
	public boolean equals(Object o) {
	    if (o == this) {
	        return true;
	    }
	    if (o == null) {
	        return false;
	    }
	    if (o.getClass() == this.getClass()) {
	        return this.equals((StateId) o);
	    }
	    return false;
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(id);
	}

	
	public boolean isEmpty(){
		for(int i: id) if(i!=0) return false;
		return true;
	}
	
	public int size(){
		return this.id.length;
	}
	
	public String toString(){
		String s = "["+id[0];
		for(int i=1; i<id.length; i++)
			s = s+" "+id[i];
		s = s+"]";
		return s;
	}

	public int getNumber() {
		return number;
	}

	void setNumber(int permutations) {
		this.number = permutations;
	}
	
	void increasePermutations() {
		this.number++;
	}
	
	/**
	 * adds permutation number of argument to this object's current number
	 * @param neu
	 */
	public void increasePermutations(StateId neu) {
		this.number += neu.getNumber();
		
	}
	
	void combinePermutations(int p) {
		this.number = this.number*p;
	}

	

}
