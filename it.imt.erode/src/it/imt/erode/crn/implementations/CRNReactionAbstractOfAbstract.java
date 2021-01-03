package it.imt.erode.crn.implementations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;

public abstract class CRNReactionAbstractOfAbstract implements ICRNReaction {

	private final IComposite reagents;
	private final IComposite products;
	private Collection<String> comments;
	
	@Override
	public boolean isODELike() {
		return  hasArbitraryKinetics() && 
				getReagents().getTotalMultiplicity()==1 && getProducts().getTotalMultiplicity()==2 && 
				getProducts().getFirstReagent().equals(getProducts().getSecondReagent()) &&
				getProducts().getFirstReagent().equals(getReagents().getFirstReagent());	
	}
	
	public CRNReactionAbstractOfAbstract(IComposite reagents,IComposite products) {
		super();
		this.reagents = reagents;
		this.products = products;
	}
	
	@Override
	public int compareReagentsAndProducts(ICRNReaction o) {
		int cmp = getReagents().compareTo(o.getReagents());
		if(cmp!=0){
			return cmp;
		}
		cmp = getProducts().compareTo(o.getProducts());
		return cmp;
	}
		
	@Override
	public boolean hasUnaryProduct() {
		return products.isUnary();
	}

	@Override
	public IComposite getReagents() {
		return reagents;
	}

	@Override
	public IComposite getProducts() {
		return products;
	}
	
	@Override
	public boolean isUnary() {
		return getReagents().isUnary();
	}
	
	@Override
	public boolean isBinary() {
		return getReagents().isBinary();
	}
		
	@Override
	public boolean isElementary(){
		return (isUnary() || isBinary()) && (!hasArbitraryKinetics()) /*&& (!hasHillKinetics())*/ ;
	}
	
	@Override
	public int getArity() {
		return reagents.computeArity();
	}
		
	@Override
	public IComposite computeProductsMinusReagents() {
		return new Composite(computeProductsMinusReagentsHashMap());
	}
	
	@Override
	public HashMap<ISpecies,Integer> computeProductsMinusReagentsHashMap() {

		IComposite products = getProducts();		
		HashMap<ISpecies,Integer> hm = new HashMap<ISpecies,Integer>();
		
		if(products.isUnary()){
			hm.put(products.getFirstReagent(), 1);
		}
		else{
			//int[] multiplicities = products.getMultiplicities();
			//ISpecies[] allSpecies = products.getAllSpecies();
			for(int i=0;i<products.getNumberOfDifferentSpecies();i++){
				if(products.getMultiplicities(i)!=0){
					hm.put(products.getAllSpecies(i), products.getMultiplicities(i));
				}
			}
		}
		
		IComposite reagents = getReagents();
		if(reagents.isUnary()){
			ISpecies species = reagents.getFirstReagent();
			Integer prev = hm.get(species);
			if(prev==null){
				hm.put(species, -1);
			}
			else{
				if(prev-1 !=0 ){
					hm.put(species, prev-1);
				}
				else{
					hm.remove(species);
				}
			}
		}
		else{
			//int[] multiplicities = reagents.getMultiplicities();
			//ISpecies[] allSpecies = reagents.getAllSpecies();
			for(int i=0;i<reagents.getNumberOfDifferentSpecies();i++){
				Integer prev = hm.get(reagents.getAllSpecies(i));
				if(prev==null){
					hm.put(reagents.getAllSpecies(i), 0-reagents.getMultiplicities(i));
				}
				else{
					if(prev-reagents.getMultiplicities(i) !=0 ){
						hm.put(reagents.getAllSpecies(i), prev-reagents.getMultiplicities(i));
					}
					else{
						hm.remove(reagents.getAllSpecies(i));
					}
				}
			}
		}
		
		return hm;
		
	}
	
	@Override
	public String toString(){	
		return getReagents() + "-- ("+getRateExpression()+") -->" + getProducts();
	}
	
	@Override
	public void addCommentLine(String commentLine) {
		if(commentLine==null || commentLine.length()==0) {
			return;
		}
		else {
			if(comments==null){
				comments=new ArrayList<String>();
			}
			comments.add(commentLine);
		}
	}
	
	@Override
	public void addCommentLines(Collection<String> commentLines) {
		if(commentLines==null || commentLines.size()==0) {
			return;
		}
		else {
			for(String commentLine : commentLines) {
				addCommentLine(commentLine);
			}
		}
	}

	@Override
	public Collection<String> getComments() {
		return comments;
	}

}
