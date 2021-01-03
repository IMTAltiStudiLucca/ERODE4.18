package it.imt.erode.partition.implementations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.importing.AbstractImporter;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SpeciesSplayBST;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SpeciesSplayBSTWithTolerance;
import it.imt.erode.partitionrefinement.splittersbstandcounters.VectorOfBigDecimalsForEachLabel;








public class Block implements IBlock {

	//I might need to do equals for comparisons with label....but this would create problems in performances.
	
	//public static final IBlock ANYBLOCK = new Block();
	
	//private List<ISpecies> allgetpecies;
	private LinkedHashSet<ISpecies> allSpecies;
	private IBlock next;
	private IBlock prev;
	private SpeciesSplayBST<BigDecimal, IBlock> bst;
	private SpeciesSplayBSTWithTolerance<BigDecimal, IBlock> bstWT;
	//private SpeciesSplayBST<HashMap<ILabel, BigDecimal>, IBlock> bstFB;
	private SpeciesSplayBST<VectorOfBigDecimalsForEachLabel, IBlock> bstForVectors;
	private ISpecies representative;
	private IPartition partition;
	private boolean useAsSplitter=true;
	private boolean hasBeenAlreadyConsideredAsASplitter=false;
	
	//private static int IDASLABEL=0;
	
	
	public Block(){
		super();
		//idAsLabel=IDASLABEL;IDASLABEL++;
		//allSpecies = new ArrayList<ISpecies>();
		allSpecies = new LinkedHashSet<ISpecies>();
	}
	
	/*public Block(ISpecies species) {
		this();
		addSpecies(species);
	}*/
	
	@Override
	public IBlock copyAndAddToPartition(IPartition partition) {
		Block shallowCopy = new Block();
		partition.add(shallowCopy);
		for (ISpecies species : allSpecies) {
			shallowCopy.addSpecies(species);
		}
		
		//shallowCopy.next=next;
		//shallowCopy.prev=prev;
		//shallowCopy.bst=null;
		shallowCopy.representative=representative;
		return shallowCopy;
	}

	@Override
	public void setPartition(IPartition partition) {
		this.partition=partition;
	}
	
	@Override
	public void updatePartition(IPartition partition) {
		setPartition(partition);
		for(ISpecies species : allSpecies){
			partition.updateSpeciesToBlockMapping(species, this);
		}
	}
	
	@Override
	public Collection<ISpecies> getSpecies() {
		return allSpecies;
	}

	@Override
	public void addSpecies(ISpecies species) {
		allSpecies.add(species);
		/*if(partition==null){
			CRNReducerCommandLine.println("problem");
		}*/
		partition.updateSpeciesToBlockMapping(species,this); //species.setBlock(this);
		if(representative==null){
			representative=species;
		}
		else if(representative!=null && AbstractImporter.isSpecialNullSpecies(representative.getName())){
		//else if(representative!=null && representative.getName().equals(Species.ZEROSPECIESNAME)){
			representative=species;
		}
		/*
		//Here I could just do this: 
		if(species.compareTo(representative)<0){
			representative=species;
		}
		But I don't remember if this could create me problems.
		*/
	}

	@Override
	public boolean isEmpty() {
		return allSpecies.isEmpty();
	}
	
	@Override
	public ISpecies getRepresentative() {
		return getRepresentative(false);
		//return representative;
	}
	
	@Override
	public ISpecies getRepresentative(boolean getMinSpecies) {
		if(getMinSpecies){
			ISpecies minSpecies=null;
			for (ISpecies species : allSpecies) {
				if(minSpecies==null){
					minSpecies=species;
				}
				else{
					if(species.compareTo(minSpecies)<0){
						minSpecies=species;
					}
				}
			}
			return minSpecies;
		}
		else{
			return representative;
		}
		
	}

	@Override
	public void setMinAsRepresentative() {
		ISpecies minSpecies=null;
		for (ISpecies species : allSpecies) {
			if(minSpecies==null){
				minSpecies=species;
			}
			else{
				if(species.compareTo(minSpecies)<0){
					minSpecies=species;
				}
			}
		}
		representative=minSpecies;
	}

	@Override
	public void removeSpecies(ISpecies species) {
		allSpecies.remove(species);
		partition.updateSpeciesToBlockMapping(species,null);
		if(representative.equals(species)){
			if(isEmpty()){
				representative=null;
			}
			else{
				//representative=allSpecies.get(0);
				representative=allSpecies.iterator().next();
			}
		}
	}

	@Override
	public IBlock getPrev() {
		return prev;
	}

	@Override
	public IBlock getNext() {
		return next;
	}

	@Override
	public void setPrev(IBlock block) {
		this.prev = block;
	}

	@Override
	public void setNext(IBlock block) {
		this.next = block;
	}
	
	@Override
	public SpeciesSplayBST<BigDecimal, IBlock> getBST() {
		if(bst == null){
			bst = new SpeciesSplayBST<>();
		}
		return bst;
	}
	
	@Override
	public SpeciesSplayBST<VectorOfBigDecimalsForEachLabel, IBlock> getBSTForVectors() {
		if(bstForVectors == null){
			bstForVectors = new SpeciesSplayBST<>();
		}
		return bstForVectors;
	}
	
	@Override
	public SpeciesSplayBSTWithTolerance<BigDecimal, IBlock> getBST(BigDecimal tolerance) {
		if(bstWT == null){
			bstWT = new SpeciesSplayBSTWithTolerance<BigDecimal, IBlock>(tolerance);
		}
		return bstWT;
	}

	/*@Override
	public void setSpecies(Collection<ISpecies> newSpecies) {
		allSpecies = new ArrayList<>();
		for (ISpecies species : newSpecies) {
			addSpecies(species);
		}
	}*/

	@Override
	public void throwAwayBST() {
		bst = null;
	}
	
	@Override
	public void throwAwayBSTWithTolerance() {
		bstWT=null;
	}
	
	@Override
	public void throwAwayBSTForVectors() {
		bstForVectors=null;
	}

	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder("Size: ");
		sb.append(getSpecies().size()+"\n");
		for (ISpecies species : getSpecies()) {
			sb.append(species.toStringWithId());
			sb.append(" ");
			sb.append("\n");
		}
		return sb.toString();
	}

	@Override
	public BigDecimal getBlockConcentration() {
		BigDecimal blockConcentration = BigDecimal.ZERO;
		for (ISpecies species : allSpecies) {
			blockConcentration = blockConcentration.add(species.getInitialConcentration());
		}
		return blockConcentration;
	}
	
	@Override
	public String computeBlockConcentrationExpr() {
		StringBuilder blockConcentration=new StringBuilder();
		boolean first=true;
		for (ISpecies species : allSpecies) {
			if(species.getInitialConcentration().compareTo(BigDecimal.ZERO)!=0){
				if(first){
					first=false;
				}
				else{
					blockConcentration.append('+');
				}
				blockConcentration.append(species.getInitialConcentrationExpr());
			}
		}
		return blockConcentration.toString();
	}
	
	@Override
	public String toStringSeparatedSpeciesNames(String sep){
		StringBuilder sb=new StringBuilder();
		//StringBuilder sb=new StringBuilder(getRepresentative().getName() + " "+ getRepresentative().getName().replace("~", "").replace(",", ""));
		
		for (ISpecies species : allSpecies) {
			sb.append(species.getName());
			sb.append(sep);
		}
		String ret = sb.toString();
		if(ret.endsWith(sep)){
			ret = ret.substring(0, ret.length()-sep.length());
		}
		
		return ret;
	}

	@Override
	public Collection<String> computeBlockComment() {
		//reducedSpecies.addComment("This species is the reduction of these original species: "+currentBlock.toStringSeparatedSpeciesNames(", "));
		Collection<String> commentLines = new ArrayList<String>(this.getSpecies().size()+1);
		if(this.getSpecies().size()==1){
			ISpecies species = this.getSpecies().iterator().next();
			commentLines.add("Singleton block ");
			if(species.getOriginalName()!=null){
				commentLines.add(" \t"+species.getName() + " ( "+species.getOriginalName()+" )");
			}
			else{
				commentLines.add(" \t"+species.getName());
			}
		}
		else{
			commentLines.add("Representative of block (with "+allSpecies.size()+" species)");
			for(ISpecies species : allSpecies){
				if(species.getOriginalName()!=null){
					commentLines.add(" "+"\t"+species.getName()+ " ( "+species.getOriginalName()+" )");
				}
				else{
					commentLines.add(" "+"\t"+species.getName());
				}
			}
		}
		return commentLines;
		/*
		StringBuilder comment = new StringBuilder("Representative of block (with ");
		comment.append(allSpecies.size());
		comment.append(" species):\n");
		for(ISpecies species : allSpecies){
			comment.append(" "+commSymbol+"\t");
			comment.append(species.getName());
			comment.append("\n");
		}
		return comment.toString();
		*/
	}

	
	@Override
	public void setCanBeUsedAsSplitter(boolean val) {
		useAsSplitter=val;
	}

	@Override
	public boolean canBeUsedAsSplitter() {
		return useAsSplitter;
	}
	
	@Override
	public void setHasBeenAlreadyUsedAsSplitter(boolean val) {
		hasBeenAlreadyConsideredAsASplitter=val;
	}
	
	@Override
	public boolean hasBeenAlreadyUsedAsSplitter() {
		return hasBeenAlreadyConsideredAsASplitter;
	}

	/*@Override
	public int getReferredArity() {
		return 2;
	}

	@Override
	public int getLabelID() {
		return idAsLabel;
	}*/

	/*@Override
	public void setLabelId(int id) {
		idAsLabel=id;		
	}*/

}
