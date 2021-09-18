package it.imt.erode.crn.implementations;

//import java.util.ArrayList;
import java.util.Arrays;
//import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.expression.parser.IMonomial;
import it.imt.erode.expression.parser.ProductMonomial;
import it.imt.erode.expression.parser.SpeciesMonomial;

public class Composite implements IComposite
{

	private ISpecies[] allSpecies;
	private int[] multiplicities;

//	private Collection<ICRNReaction> incomingReactions;
//	private Collection<ICRNReaction> outgoingReactions;

	private Composite(){
		super();
	}
	
	private static HashMap<ISpecies, Integer> list2hashmap(List<ISpecies> compositeList){
		HashMap<ISpecies, Integer> compositeHM = new HashMap<ISpecies, Integer>();
		for(ISpecies sp : compositeList) {
			Integer prev = compositeHM.get(sp);
			compositeHM.put(sp, 1 + ((prev==null)?0:prev));
		}
		return compositeHM;
	}

	public Composite(List<ISpecies> compositeList) {
		this(list2hashmap(compositeList));
	}
	
	public Composite(HashMap<ISpecies, Integer> compositeHM){
		this();
		multiplicities=new int[compositeHM.keySet().size()];
		allSpecies=new ISpecies[compositeHM.keySet().size()];
		compositeHM.keySet().toArray(allSpecies);
		Arrays.sort(allSpecies);
		for(int s=0;s<allSpecies.length;s++){
			multiplicities[s]=compositeHM.get(allSpecies[s]);
			if(multiplicities[s]==0) {
				throw new UnsupportedOperationException("All species in a composite must have non-zero multiplicity");
			}
		}
	}
	
	public Composite(IComposite mil) {
		this();
		multiplicities=new int[mil.getNumberOfDifferentSpecies()];
		allSpecies = new ISpecies[mil.getNumberOfDifferentSpecies()];
		for(int i=0;i<mil.getNumberOfDifferentSpecies();i++) {
			multiplicities[i]=mil.getMultiplicities(i);
			allSpecies[i]=mil.getAllSpecies(i);
		}
	}

	public Composite(List<ISpecies> allExistingSpecies, int[] speciesIds) {
		this();

		int[] sortedSpeciesIds = Arrays.copyOf(speciesIds, speciesIds.length);
		Arrays.sort(sortedSpeciesIds);

		int currentId=-1;
		int distinctSpecies =0;
		//count the number of distinct species
		for(int i=0;i<sortedSpeciesIds.length;i++){
			if(sortedSpeciesIds[i]!=currentId){
				distinctSpecies++;
			}
			currentId=sortedSpeciesIds[i];
		}
		allSpecies = new ISpecies[distinctSpecies];
		multiplicities = new int[distinctSpecies];
		currentId=-1;
		int pos=-1;
		for(int i=0;i<sortedSpeciesIds.length;i++){
			if(sortedSpeciesIds[i]!=currentId){
				pos++;
				allSpecies[pos]= allExistingSpecies.get(sortedSpeciesIds[i]);
			}
			multiplicities[pos]++;
			currentId=sortedSpeciesIds[i];
		}
	}
	
	/**
	 * @param allSpecies
	 * @param speciesIds the ids of the species with repetitions to represent multiplicities. The array is copied, and sorted with respect to an order on ids.
	 */
	public Composite(ISpecies[] allExistingSpecies, int[] speciesIds) {
		this();

		int[] sortedSpeciesIds = Arrays.copyOf(speciesIds, speciesIds.length);
		Arrays.sort(sortedSpeciesIds);

		int currentId=-1;
		int distinctSpecies =0;
		//count the number of distinct species
		for(int i=0;i<sortedSpeciesIds.length;i++){
			if(sortedSpeciesIds[i]!=currentId){
				distinctSpecies++;
			}
			currentId=sortedSpeciesIds[i];
		}
		allSpecies = new ISpecies[distinctSpecies];
		multiplicities = new int[distinctSpecies];
		currentId=-1;
		int pos=-1;
		for(int i=0;i<sortedSpeciesIds.length;i++){
			if(sortedSpeciesIds[i]!=currentId){
				pos++;
				allSpecies[pos]= allExistingSpecies[sortedSpeciesIds[i]];
			}
			multiplicities[pos]++;
			currentId=sortedSpeciesIds[i];
		}
	}

	/**
	 * @param allSpecies
	 * @param speciesIds the ids of the species with repetitions to represent multiplicities. The array is copied, and sorted with respect to an order on ids.
	 */
	public Composite(List<ISpecies> allExistingSpecies, int idSpeciesA, int idSpeciesB) {
		this();

		int minID = Math.min(idSpeciesA, idSpeciesB);
		int maxID = Math.max(idSpeciesA, idSpeciesB);

		int distinctSpecies=2;
		if(minID==maxID){
			distinctSpecies=1;
			allSpecies = new ISpecies[distinctSpecies];
			multiplicities = new int[distinctSpecies];
			allSpecies[0] = allExistingSpecies.get(minID);
			multiplicities[0]=2;
		}
		else{
			distinctSpecies=2;
			allSpecies = new ISpecies[distinctSpecies];
			multiplicities = new int[distinctSpecies];
			allSpecies[0] = allExistingSpecies.get(minID);
			multiplicities[0]=1;
			allSpecies[1] = allExistingSpecies.get(maxID);
			multiplicities[1]=1;
		}
	}

		
	/**
	 * @param allSpecies
	 * @param speciesIds the ids of the species with repetitions to represent multiplicities. The array is copied, and sorted with respect to an order on ids.
	 */
	public Composite(ISpecies[] allExistingSpecies, int idSpeciesA, int idSpeciesB) {
		this();

		int minID = Math.min(idSpeciesA, idSpeciesB);
		int maxID = Math.max(idSpeciesA, idSpeciesB);

		int distinctSpecies=2;
		if(minID==maxID){
			distinctSpecies=1;
			allSpecies = new ISpecies[distinctSpecies];
			multiplicities = new int[distinctSpecies];
			allSpecies[0] = allExistingSpecies[minID];
			multiplicities[0]=2;
		}
		else{
			distinctSpecies=2;
			allSpecies = new ISpecies[distinctSpecies];
			multiplicities = new int[distinctSpecies];
			allSpecies[0] = allExistingSpecies[minID];
			multiplicities[0]=1;
			allSpecies[1] = allExistingSpecies[maxID];
			multiplicities[1]=1;
		}

	}
	
	
	
	/**
	 * @param allSpecies
	 * @param speciesIds the ids of the species with repetitions to represent multiplicities. The array is copied, and sorted with respect to an order on ids.
	 */
	public Composite(ISpecies speciesA, ISpecies speciesB) {
		this();

		int idSpeciesA=speciesA.getID();
		int idSpeciesB=speciesB.getID();
				
		
		int minID = Math.min(idSpeciesA, idSpeciesB);
		int maxID = Math.max(idSpeciesA, idSpeciesB);
		
		ISpecies minSpecies;
		ISpecies maxSpecies;
		if(minID==idSpeciesA){
			minSpecies = speciesA;
			maxSpecies = speciesB;
		}
		else{
			minSpecies = speciesB;
			maxSpecies = speciesA;
		}

		if(minID==maxID){
			allSpecies = new ISpecies[1];
			multiplicities = new int[1];
			allSpecies[0] = minSpecies;
			multiplicities[0]=2;
		}
		else{
			allSpecies = new ISpecies[2];
			multiplicities = new int[2];
			allSpecies[0] = minSpecies;
			multiplicities[0]=1;
			allSpecies[1] = maxSpecies;
			multiplicities[1]=1;
		}

	}



	

	@Override
	public HashMap<ISpecies, Integer> toHashMap(){
		HashMap<ISpecies, Integer> compositeHM=new HashMap<>(getNumberOfDifferentSpecies());
		for(int s=0;s<getNumberOfDifferentSpecies();s++){
			compositeHM.put(getAllSpecies(s), getMultiplicities(s));
		}
		return compositeHM;
	}
	

	/**
	 * @param allSpecies
	 * @param speciesIds the ids of the species with repetitions to represent multiplicities. The array is copied, and sorted with respect to an order on ids.
	 */
	//public Composite(ISpecies speciesA, ISpecies speciesB) {

	@Override
	public int hashCode() {
//		if(isEmpty()) {
//			return new EmptyComposite().hashCode();
//		}
		if(isUnary()){
			return allSpecies[0].hashCode();
		}
		else{
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(allSpecies);
			result = prime * result + Arrays.hashCode(multiplicities);
			return result;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null)
			return false;
//		if(o instanceof EmptyComposite) {
//			return isEmpty();
//		}
		else if(o instanceof ISpecies){
			if(!isUnary()){
				return false;
			}
			else{
				return allSpecies[0].equals((ISpecies)o);
			}
		}
		else if(o instanceof Composite){
			//if (getClass() != o.getClass()) return false;
			Composite other = (Composite) o;
			if (!Arrays.equals(multiplicities, other.multiplicities))
				return false;
			if (!Arrays.equals(allSpecies, other.allSpecies))
				return false;
			return true;
		}
		/*else if(o instanceof EmptySetLabel){
			if(allSpecies.length==0){
				return true;
			}
			else{
				return false;
			}
		}*/
		throw new UnsupportedOperationException();
	}

	/**
	 * A sort or lexicographical ordering on allSpecies + multiplicities
	 * @param o
	 * @return
	 */
	@Override
	public int compareTo(Object o) {
		if(o instanceof ISpecies){
			//int cmp = Integer.compare(getAllSpecies()[0].getLabelID(),((ISpecies)o).getLabelID());
			int cmp = getAllSpecies(0).compareTo(o);
			if(cmp==0){
				if(isUnary()){
					return 0;
				}
				else{
					return 1;
				}
			}
			return cmp;
		}
		else if(o instanceof EmptyComposite) {
			return 1;
		}
		else if(o instanceof Composite){
			IComposite cp = (IComposite)o;
			for(int i=0;i<Math.min(allSpecies.length, cp.getNumberOfDifferentSpecies());i++){
				int cmp = allSpecies[i].compareTo(cp.getAllSpecies(i));
				if(cmp!=0){
					return cmp;
				}
				else{
					//Same current species. I compare the multiplicities.
					cmp = Integer.compare(multiplicities[i],cp.getMultiplicities(i));
					if(cmp!=0){
						return cmp;
					}
				}
			}
			/*
			 * If I arrive here, it means that the arrays have same species and multiplicities for the first n species, with n = Math.min(allSpecies.length, o.allSpecies.length.
			 * Now, if the composites have same number of species are equal, while the composite with less species is smaller.
			 */
			return Integer.compare(allSpecies.length, cp.getNumberOfDifferentSpecies());
		}
		/*else if(o instanceof EmptySetLabel){
			if(allSpecies.length==0){
				return 0; //EmptyComposite
			}
			else{
				return 1; //A species is greater than the emptysetlabel
			}
		}*/

		throw new UnsupportedOperationException();
	}

	public ISpecies[] getAllSpecies() {
		return allSpecies;
	}

	public int[] getMultiplicities() {
		return multiplicities;
	}
	
	@Override
	public ISpecies getAllSpecies(int pos) {
		return allSpecies[pos];
	}

	@Override
	public int getMultiplicities(int pos) {
		return multiplicities[pos];
	}
	
	@Override
	public void setMultiplicities(int pos, int val) {
		multiplicities[pos]=val;
		if(multiplicities[pos]==0) {
			throw new UnsupportedOperationException("All species in a composite must have non-zero multiplicity");
		}
	}
	
	@Override
	public int getNumberOfDifferentSpecies(){
		return allSpecies.length;
	}

	@Override
	public ISpecies getFirstReagent(){
		//return allSpecies[0];
		return getNthReagent(1);
	}

	@Override
	public ISpecies getNthReagent(int pos){
		int currentPos=0;
		for(int s=0;s<allSpecies.length;s++){
			currentPos=currentPos+multiplicities[s];
			if(pos<=currentPos){
				return allSpecies[s];
			}
		}

		//CRNReducerCommandLine.printWarning("I could not find the "+pos+"-th species in the composite "+this);
		return null;
	}

	@Override
	public ISpecies getSecondReagent(){
		return getNthReagent(2);
		/*if(multiplicities[0]>1){
			return allSpecies[0];
		}
		else{
			return allSpecies[1];
		}*/
	}

	@Override
	public int getMultiplicityOfSpecies(ISpecies species){
		int i =Arrays.binarySearch(allSpecies, species);
		if(i<0){
			return 0;
		}
		return multiplicities[i];
	}
	
	@Override
	public int getPosOfSpecies(ISpecies species){
		int i =Arrays.binarySearch(allSpecies, species);
		if(i<0){
			return -1;
		}
		return i;
	}

	@Override
	public boolean contains(ISpecies species){
		int i =Arrays.binarySearch(allSpecies, species);
		if(i<0){
			return false;
		}
		else{
			return true;
		}
	}

	@Override
	public int getMultiplicityOfSpeciesWithId(int speciesId){
		for(int i = 0; i< allSpecies.length;i++){
			if(allSpecies[i].getID()==speciesId){
				return multiplicities[i];
			}
		}
		return 0;
	}

	@Override
	public boolean isUnary() {
		return multiplicities.length==1 && multiplicities[0]==1 ;
	}

	@Override
	public boolean isBinary() {
		return (multiplicities.length==1 && multiplicities[0]==2) ||
				(multiplicities.length==2 && multiplicities[0]==1 && multiplicities[1]==1);
	}

	@Override
	public boolean isTernaryOrMore() {
		return (multiplicities.length>2) ||
				(multiplicities.length==1 && multiplicities[0]>2) ||
				(multiplicities.length==2 && multiplicities[0] + multiplicities[1] > 2);
	}

	@Override
	public int computeArity() {
		int arity =0;
		for(int i=0;i<multiplicities.length;i++){
			arity+=multiplicities[i];
		}
		return arity;
	}

	@Override
	public String toString(){
		if(allSpecies.length==0){
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<allSpecies.length;i++){
			if(multiplicities[i]==1){
				sb.append(allSpecies[i]);
				sb.append("+");
			}
			else {
				sb.append(multiplicities[i]);
				sb.append('*');
				sb.append(allSpecies[i]);
				sb.append("+");
			}
		}
		sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}


	private String toMultiSet(boolean compact) {
		if(allSpecies.length==0){
			return "";
		}

		StringBuilder sb=new StringBuilder();
		for(int i=0;i<allSpecies.length;i++){
			ISpecies species = allSpecies[i];
			int multiplicity = multiplicities[i];
			if(multiplicity>0){
				for(int m = 0;m<multiplicity;m++){
					if(compact){
						sb.append("S"+species.getID());
					}
					else{
						sb.append(species.getName());
					}
					sb.append(" + ");
				}
			}
		}
		if(sb.length()==0){
			return "0";
		}
		else{
			sb.delete(sb.length()-3, sb.length());//remove last " + "
		}

		return sb.toString();
	}
	

	@Override
	public String toMultiSet() {
		return toMultiSet(false);
	}
	@Override
	public String toMultiSetCompact() {
		return toMultiSet(true);
	}
	
	@Override
	public String toMultiSetWithStoichiometries(boolean useSpaces) {
		if(allSpecies.length==0){
			return "";
		}

		StringBuilder sb=new StringBuilder();
		for(int i=0;i<allSpecies.length;i++){
			ISpecies species = allSpecies[i];
			int multiplicity = multiplicities[i];
			if(multiplicity>0){
				if(multiplicity>1){
					sb.append(multiplicities[i]);
					sb.append("*");
				}
				sb.append(species.getName());
				
				if(useSpaces){
					sb.append(" + ");
				}
				else{
					sb.append("+");
				}
				
			}
		}
		if(sb.length()==0){
			return "0";
		}
		else{
			if(useSpaces){
				sb.delete(sb.length()-3, sb.length());//remove last " + "
			}
			else{
				sb.delete(sb.length()-1, sb.length());//remove last "+"
			}
			
		}
		return sb.toString();
	}
	
	@Override
	public String toMultiSetWithStoichiometriesOrigNames(boolean useSpaces) {
		if(allSpecies.length==0){
			return "";
		}

		StringBuilder sb=new StringBuilder();
		for(int i=0;i<allSpecies.length;i++){
			ISpecies species = allSpecies[i];
			int multiplicity = multiplicities[i];
			if(multiplicity>0){
				if(multiplicity>1){
					sb.append(multiplicities[i]);
					sb.append("*");
				}
				if(species.getOriginalName()!=null && species.getOriginalName().length()>0) {
					sb.append(species.getOriginalName());
				}
				else {
					sb.append(species.getName());
				}
				
				
				if(useSpaces){
					sb.append(" + ");
				}
				else{
					sb.append("+");
				}
				
			}
		}
		if(sb.length()==0){
			return "0";
		}
		else{
			if(useSpaces){
				sb.delete(sb.length()-3, sb.length());//remove last " + "
			}
			else{
				sb.delete(sb.length()-1, sb.length());//remove last "+"
			}
			
		}
		return sb.toString();
	}

	@Override
	public String toMultiSetWithAlphaNumericNames() {

		if(allSpecies.length==0){
			return "";
		}

		StringBuilder sb=new StringBuilder();
		for(int i=0;i<allSpecies.length;i++){
			ISpecies species = allSpecies[i];
			int multiplicity = multiplicities[i];
			if(multiplicity>0){
				for(int m = 0;m<multiplicity;m++){
					sb.append(species.getNameAlphanumeric());
					sb.append(" + ");
				}
			}
		}
		if(sb.length()==0){
			return "0";
		}
		else{
			sb.delete(sb.length()-3, sb.length());//remove last " + "
		}

		return sb.toString();

	}

	@Override
	public String getMassActionExpression(boolean withAlphaNumericNames,boolean ignoreI) {
		return getMassActionExpression(withAlphaNumericNames,"",ignoreI);
	}
	
	@Override
	public String getMassActionExpression(boolean withAlphaNumericNames,String prefix,boolean ignoreI) {
		return getMassActionExpression(withAlphaNumericNames, prefix,null,prefix,ignoreI);
	}
	
	@Override
	public String getMassActionExpression(boolean withAlphaNumericNames,String prefix,ISpecies specialSpecies,String specialPrefix,boolean ignoreI) {
		if(allSpecies.length==0){
			return "1";
		}

		StringBuilder sb=new StringBuilder();
		for(int i=0;i<allSpecies.length;i++){
			ISpecies species = allSpecies[i];
			int multiplicity = multiplicities[i];
			String pref = (species.equals(specialSpecies))?specialPrefix:prefix;
			if(multiplicity>0){
				if(ignoreI && species.getName().equals(Species.I_SPECIESNAME)){
					sb.append("1");
				}
				else{
					//sb.append(pref);
					String name =pref+(withAlphaNumericNames?species.getNameAlphanumeric():species.getName()); 
					sb.append(name);
					for(int m=1;m<multiplicity;m++){
						sb.append("*"+name);
					}
					/*if(multiplicity>1){
						sb.append("^");
						sb.append(multiplicity);
					}*/
				}
				sb.append("*");
			}
		}
		if(sb.length()==0){
			return "1";
		}
		else{
			sb.delete(sb.length()-1, sb.length());//remove last "*"
			return sb.toString();
		}
	}
	
	@Override
	public String getMassActionExpressionReplacingSpeciesNames(String variablePrefix, int idIncrement, String variableSuffix) {
		//return variablePrefix+(getID()+idIncrement)+variableSuffix;
		if(allSpecies.length==0){
			return "1";
		}

		StringBuilder sb=new StringBuilder();
		for(int i=0;i<allSpecies.length;i++){
			ISpecies species = allSpecies[i];
			int multiplicity = multiplicities[i];
			if(multiplicity>0){
				String name =variablePrefix+(species.getID()+idIncrement)+variableSuffix;		
				sb.append(name);
				for(int m=1;m<multiplicity;m++){
					sb.append("*"+name);
				}
				sb.append("*");
			}
		}
		if(sb.length()==0){
			return "1";
		}
		else{
			sb.delete(sb.length()-1, sb.length());//remove last "*"
			return sb.toString();
		}
	}
	
	@Override
	public String getMassActionExpressionReplacingSpeciesNames(String variableAsFunction, int idIncrement, boolean writeParenthesis, String suffix) {
		return getMassActionExpressionReplacingSpeciesNames(variableAsFunction,idIncrement,writeParenthesis,suffix,false);
	}
	
	@Override
	public String getMassActionExpressionReplacingSpeciesNames(String variableAsFunction, int idIncrement, boolean writeParenthesis, String suffix, boolean squaredParenthesis) {
		if(allSpecies.length==0){
			return "1";
		}

		StringBuilder sb=new StringBuilder();
		for(int i=0;i<allSpecies.length;i++){
			ISpecies species = allSpecies[i];
			int multiplicity = multiplicities[i];
			if(multiplicity>0){
				String name="";
				if(writeParenthesis){
					if(squaredParenthesis){
						name =variableAsFunction+"["+(species.getID()+idIncrement)+"]";
					}
					else{
						name =variableAsFunction+"("+(species.getID()+idIncrement)+")";
					}
				}
				else{
					name =variableAsFunction+(species.getID()+idIncrement)+suffix;
				}
				
				sb.append(name);
				for(int m=1;m<multiplicity;m++){
					sb.append("*"+name);
				}
				/*if(multiplicity>1){
					sb.append("^");
					sb.append(multiplicity);
				}*/
				sb.append("*");
			}
		}
		if(sb.length()==0){
			return "1";
		}
		else{
			sb.delete(sb.length()-1, sb.length());//remove last "*"
			return sb.toString();
		}
	}

//	@Override
//	public Collection<ICRNReaction> getIncomingReactions() {
//		return incomingReactions;
//	}
//
//	@Override
//	public void addIncomingReactions(ICRNReaction incomingReaction) {
//		if(incomingReactions==null){
//			incomingReactions=new ArrayList<ICRNReaction>();
//		}
//		incomingReactions.add(incomingReaction);
//	}
//	
//	@Override
//	public Collection<ICRNReaction> getOutgoingReactions() {
//		return outgoingReactions;
//	}
//	@Override
//	public void addOutgoingReactions(ICRNReaction outgoingReaction) {
//		if(outgoingReactions==null){
//			outgoingReactions=new ArrayList<ICRNReaction>();
//		}
//		outgoingReactions.add(outgoingReaction);
//	}

	//private HashMap<String, String> spaceSeparatedMultiset=new HashMap<String, String>(2);

	//END SPECIFIC FOR EFL

	@Override
	public int getTotalMultiplicity() {
		int mult =0;
		for(int i=0;i<multiplicities.length;i++){
			mult+=multiplicities[i];
		}
		return mult;
	}
//	public boolean isEmpty() {
//		for(int i=0;i<multiplicities.length;i++) {
//			if(multiplicities[i]!=0) {
//				return false;
//			}
//		}
//		return true;
//	}




	@Override
	public String getSpaceSeparatedMultisetAlphaNumeric() {
		/*
		String spaceSeparatedMS=spaceSeparatedMultiset.get("");
		if(spaceSeparatedMS==null || spaceSeparatedMS.equals("")){
			if(allSpecies.length==0){
				spaceSeparatedMS="1.0";
			}
			else{
				StringBuilder sb=new StringBuilder();
				for(int i=0;i<allSpecies.length;i++){
					ISpecies species = allSpecies[i];
					int multiplicity = multiplicities[i];
					if(multiplicity>0){
						for(int m = 0;m<multiplicity;m++){
							sb.append(" ");
							sb.append(species.getNameAlphanumeric());
						}
					}
				}
				spaceSeparatedMS = sb.toString();
			}
			spaceSeparatedMultiset.put("", spaceSeparatedMS);
		}
		return spaceSeparatedMS;
		*/

		String spaceSeparatedMS;
		if(allSpecies.length==0){
			spaceSeparatedMS="1.0";
		}
		else{
			StringBuilder sb=new StringBuilder();
			for(int i=0;i<allSpecies.length;i++){
				ISpecies species = allSpecies[i];
				int multiplicity = multiplicities[i];
				if(multiplicity>0){
					for(int m = 0;m<multiplicity;m++){
						sb.append(" ");
						sb.append(species.getNameAlphanumeric());
					}
				}
			}
			spaceSeparatedMS = sb.toString();
		}
		return spaceSeparatedMS;
	}

	@Override
	public String getSpaceSeparatedMultisetUsingIdsAsNames(String prefix, String suffix) {
		/*
		String spaceSeparatedMS=spaceSeparatedMultiset.get(suffix);
		if(spaceSeparatedMS==null || spaceSeparatedMS.equals("")){
			if(allSpecies.length==0){
				spaceSeparatedMS="1.0";
			}
			else{
				StringBuilder sb=new StringBuilder();
				for(int i=0;i<allSpecies.length;i++){
					ISpecies species = allSpecies[i];
					int multiplicity = multiplicities[i];
					if(multiplicity>0){
						for(int m = 0;m<multiplicity;m++){
							sb.append(" "+prefix+species.getID()+suffix);
						}
					}
				}
				spaceSeparatedMS = sb.toString();
			}
			spaceSeparatedMultiset.put(suffix, spaceSeparatedMS);
		}
		return spaceSeparatedMS;
		*/
		String spaceSeparatedMS;
		if(allSpecies.length==0){
			spaceSeparatedMS="1.0";
		}
		else{
			StringBuilder sb=new StringBuilder();
			for(int i=0;i<allSpecies.length;i++){
				ISpecies species = allSpecies[i];
				int multiplicity = multiplicities[i];
				if(multiplicity>0){
					for(int m = 0;m<multiplicity;m++){
						sb.append(" "+prefix+species.getID()+suffix);
					}
				}
			}
			spaceSeparatedMS = sb.toString();
		}
		return spaceSeparatedMS;
	}

	/**
	 * This method returns a new composite containing the net stoichiometry first - second
	 * @return
	 */
	public static IComposite createFirstMinusSecond(IComposite first,IComposite second) {

		HashMap<ISpecies,Integer> hm = new HashMap<ISpecies,Integer>();
		//int[] multiplicities = first.getMultiplicities();
		//ISpecies[] allSpecies = first.getAllSpecies();
		for(int i=0;i<first.getNumberOfDifferentSpecies();i++){
			if(first.getMultiplicities(i)!=0){
				hm.put(first.getAllSpecies(i), first.getMultiplicities(i));
			}
		}

		//multiplicities = second.getMultiplicities();
		//allSpecies = second.getAllSpecies();
		for(int i=0;i<second.getNumberOfDifferentSpecies();i++){
			Integer prev = hm.get(second.getAllSpecies(i));
			if(prev==null){
				hm.put(second.getAllSpecies(i), 0-second.getMultiplicities(i));
			}
			else{
				if(prev-second.getMultiplicities(i) !=0 ){
					hm.put(second.getAllSpecies(i), prev-second.getMultiplicities(i));
				}
				else{
					hm.remove(second.getAllSpecies(i));
				}
			}
		}

		return new Composite(hm);
	}

	@Override
	public boolean contains(IComposite other) {
		if(other.getNumberOfDifferentSpecies()>getNumberOfDifferentSpecies()){
			return false;
		}
		for(int s=0;s<other.getNumberOfDifferentSpecies();s++){
			ISpecies species = other.getAllSpecies(s);
			int otherMult = other.getMultiplicities(s);
			int myMult=this.getMultiplicityOfSpecies(species);
			if(myMult<otherMult){
				return false;
			}
		}
		return true;
	}

	/**
	 * Applies the reaction: removes the reagents and adds the products. It is assumed that the reaction can be fired by this composite (i.e., it contains the reagents)
	 */
	@Override
	public IComposite apply(ICRNReaction reaction) {
		IComposite net = createFirstMinusSecond(reaction.getProducts(), reaction.getReagents());
		HashMap<ISpecies, Integer> resultHM = new HashMap<>(getNumberOfDifferentSpecies());
		for(int s=0;s<getNumberOfDifferentSpecies();s++){
			ISpecies species = getAllSpecies(s);
			int mult = getMultiplicities(s);
			resultHM.put(species, mult);
		}
		for(int s=0;s<net.getNumberOfDifferentSpecies();s++){
			ISpecies species = net.getAllSpecies(s);
			Integer prev = resultHM.get(species);
			int resMult = (prev==null)?net.getMultiplicities(s) : prev+net.getMultiplicities(s);
			if(resMult!=0){
				resultHM.put(species, resMult);
			}
			else{
				resultHM.remove(species);
			}
		}
		return new Composite(resultHM);
	}

	@Override
	public boolean isHomeo() {
		return  allSpecies.length==1 && 
				multiplicities[0]==2 	;
	}

	@Override
	public IComposite copyDecreasingMultiplicityOf(ISpecies xj) {
		if(isUnary()) {
			if(allSpecies[0].equals(xj)) {
				return EmptyComposite.EMPTYCOMPOSITE;
			}
			else {
				return this;
			}
		}
		else if(isBinary()) {
			if(getFirstReagent().equals(xj)) {
				return (IComposite)getSecondReagent();
			}
			else if(getSecondReagent().equals(xj)) {
				return (IComposite)getFirstReagent();
			} 
			else {
				return this;
			}
		}
		int pos=getPosOfSpecies(xj);
		if(pos>=0) {
			HashMap<ISpecies, Integer> retHM = toHashMap();
			int newMul = retHM.get(xj)-1;
			if(newMul==0) {
				retHM.remove(xj);
			}
			else {
				retHM.put(xj, newMul);
			}
			return new Composite(retHM);
		}
		else {
			return this;
		}
	}
	
	@Override
	public IMonomial toMonomials() {
		IMonomial ma = null;
		for(int s = 0;s<this.getNumberOfDifferentSpecies();s++) {
			//ISpecies current = this.getAllSpecies(s);
			IMonomial current_mon=new SpeciesMonomial(this.getAllSpecies(s));
			for(int m=0;m<this.getMultiplicities(s);m++) {
				if(ma==null) {
					ma=current_mon;
				}
				else {
					ma=new ProductMonomial(ma, current_mon);
				}
			}
		}
		//return new SpeciesMonomial(this);
		return ma;
	}
	
}
