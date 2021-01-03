package it.imt.erode.crn.implementations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.crn.label.EmptySetLabel;
import it.imt.erode.crn.label.MutableNAryLabel;
import it.imt.erode.crn.label.NAryBlockLabel;
import it.imt.erode.expression.evaluator.MathEval;
import it.imt.erode.expression.parser.IMonomial;
import it.imt.erode.expression.parser.SpeciesMonomial;

public class Species implements ISpecies, IComposite {

	private String name;
	private String originalName;
	private String nameSupportedByMathEval;
	private Collection<String> comments;
	//private Collection<ISpecies> representedEquivalenceClass;
	private int id;

	private BigDecimal initialConcentration;
	private String initialConcentrationExpr;

	//private Collection<ICRNReaction> incomingReactions;

	//private Collection<ICRNReaction> outgoingReactions;

	//private Collection<ICRNReaction> reactionsWithNonZeroStoichiometry;

	public static final String ZEROSPECIESNAME= "SINK";//"ZERONULL";
	public static final String I_SPECIESNAME = "I";

	@Override
	public ISpecies cloneWithoutReactions(){
		Species s = new Species(name, originalName, id, initialConcentration, initialConcentrationExpr, nameSupportedByMathEval,algebraic);
		s.comments=comments;
		//s.representedEquivalenceClass=representedEquivalenceClass;
		
		//s.incomingReactions=null;
		//s.outgoingReactions=null;
		//s.reactionsWithNonZeroStoichiometry=null;
		return s;
	}
	
	public Species(String name, int id, BigDecimal initialConcentration, String initialConcentrationExpr) {
		this(name,id,initialConcentration,initialConcentrationExpr,false,false);
	}
	
	public Species(String name, int id, BigDecimal initialConcentration, String initialConcentrationExpr, boolean computeNameSupportedByMathEval,boolean algebraic) {
		this(name,null,id,initialConcentration,initialConcentrationExpr,computeNameSupportedByMathEval,algebraic);
	}
	
	public Species(String name, String originalName, int id, BigDecimal initialConcentration, String initialConcentrationExpr,boolean algebraic) {
		this(name, originalName,id, initialConcentration, initialConcentrationExpr,true,algebraic);
	}
	
	public Species(String name, int id, BigDecimal initialConcentration, String initialConcentrationExpr,boolean algebraic) {
		this(name, null,id, initialConcentration, initialConcentrationExpr,true,algebraic);
	}
	
	public Species(String name, String originalName, int id, BigDecimal initialConcentration, String initialConcentrationExpr, String nameSupportedByMathEval,boolean algebraic) {
		this(name, originalName,id, initialConcentration, initialConcentrationExpr,false,algebraic);
		this.nameSupportedByMathEval=nameSupportedByMathEval;
	}

	public Species(String name, String originalName,int id, BigDecimal initialConcentration, String initialConcentrationExpr, boolean computeNameSupportedByMathEval,boolean algebraic) {
		this.name=name;
		if(computeNameSupportedByMathEval){
			this.nameSupportedByMathEval = MathEval.getCorrespondingStringSupportedByMathEval(name);
		}
		else{
			this.nameSupportedByMathEval = name;
		}
		this.originalName=originalName;
		this.id=id;
		this.initialConcentration = initialConcentration;
		this.initialConcentrationExpr = initialConcentrationExpr;
		this.algebraic=algebraic;
	}
	
	@Override
	public void decreaseID() {
		id=id-1;
	}	
	
/*
	public Species(ISpecies species) {
		this(species.getName(),species.getOriginalName(),species.getID(),species.getInitialConcentration(),species.getInitialConcentrationExpr(),species.getNameAlphanumeric(),species.isAlgebraic());
		comments=species.getComments();
		representedEquivalenceClass=null;
		incomingReactions=null;
		outgoingReactions=null;
		reactionsWithNonZeroStoichiometry=null;
	}
	*/

	@Override
	public void setName(String name) {
		this.name=name;
		this.nameSupportedByMathEval = MathEval.getCorrespondingStringSupportedByMathEval(name);
	}


	@Override
	public void addCommentLine(String commentLine) {
		if(comments==null){
			comments=new ArrayList<String>();
		}
		comments.add(commentLine);
	}
	
	@Override
	public void addCommentLines(Collection<String> commentLines) {
		if(comments==null){
			comments=new ArrayList<String>(commentLines.size());
		}
		comments.addAll(commentLines);
	}

	@Override
	public Collection<String> getComments() {
		return comments;
	}
	


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null)
			return false;

		if(o instanceof Species){
			Species other = (Species) o;
			return id == other.id;
		}
		else if(o instanceof SpeciesCompact) {
			return id == ((SpeciesCompact)o).getID();
		}
		else if(o instanceof EmptyComposite){
			return false;
		}
		else if(o instanceof Composite){
			Composite other = (Composite) o;
			if(!other.isUnary()){
				return false;
			}
			else{
				return this.equals(other.getFirstReagent());
			}
		}
		else if(o instanceof MutableNAryLabel){
			return o.equals(this);
			/*
			MutableNAryLabel other = (MutableNAryLabel) o;
			return other.equals(this);
			*/
		}
		else if(o instanceof EmptySetLabel){
			return false;
		}
		else if(o instanceof NAryBlockLabel){
			NAryBlockLabel no = (NAryBlockLabel)o;
			if(no.getReferredArity()==2){
				return id == no.getLabelIDOfFirstSpecies();
			}
			else{
				return false;
			}
		}

		throw new UnsupportedOperationException("o.getClass(): "+o.getClass());
	}

	@Override
	public int compareTo(Object other) {
		if(other instanceof ISpecies){
			return Integer.compare(getID(), ((ISpecies)other).getID());
		}
		else if(other instanceof EmptyComposite){
			return 1;
		}
		else if(other instanceof Composite){
			Composite cp = (Composite)other;
			//int cmp = Integer.compare(getLabelID(), cp.getAllSpecies()[0].getLabelID());
			int cmp = this.compareTo(cp.getFirstReagent());
			if(cmp==0){
				if(cp.isUnary()){
					return 0;
				}
				else{
					return -1;
				}
			}
			return cmp;
		}
		else if(other instanceof EmptySetLabel){
			return 1;//A species is greater than the emptysetlabel
		}
		
		String oth = "null";
		if(other!=null){
			oth=other.toString();
		}
		//String message = "Cannot compare "+this+" with "+oth;
		//throw new UnsupportedFormatException(message);
		throw new UnsupportedOperationException("Cannot compare "+this+" with "+oth);
		
	}




	/*public Species(String name, int id, IBlock block, BigDecimal initialConcentration) {
		this(name,id,initialConcentration);
		this.block = block;
	}*/

	@Override
	public BigDecimal getInitialConcentration() {
		return initialConcentration;
	}
	
	@Override
	public String getInitialConcentrationExpr() {
		return initialConcentrationExpr;
	}
	
	@Override
	public void setInitialConcentration(BigDecimal initialConcentration, String initialConcentrationExpr){
		this.initialConcentration=initialConcentration;
		this.initialConcentrationExpr=initialConcentrationExpr;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public String getOriginalName() {
		return originalName;
	}
	
	@Override
	public String getNameAlphanumeric() {
		return nameSupportedByMathEval;
	}

	@Override
	public int getLabelID() {
		return getID();
	}
	
	@Override
	public int getID() {
		return id;
	}
	@Override
	public void setId(int id) {
		this.id=id;
	}

//	@Override
//	public Collection<ICRNReaction> getIncomingReactions() {
//		return incomingReactions;
//	}
	
//	@Override
//	public void addIncomingReactions(ICRNReaction incomingReaction) {
//		if(incomingReactions==null){
//			incomingReactions = new ArrayList<ICRNReaction>();
//		}
//		incomingReactions.add(incomingReaction);
//	}
	
//	@Override
//	public void addOutgoingReactions(ICRNReaction outgoingReaction) {
//		if(outgoingReactions==null){
//			outgoingReactions = new ArrayList<ICRNReaction>();
//		}
//		outgoingReactions.add(outgoingReaction);
//	}
	
//	@Override
//	public Collection<ICRNReaction> getOutgoingReactions() {
//		return outgoingReactions;
//	}
	
//	@Override
//	public Collection<ICRNReaction> getReactionsWithNonZeroStoichiometry() {
//		return reactionsWithNonZeroStoichiometry;
//	}
	
//	@Override
//	public void addReactionsWithNonZeroStoichiometry(ICRNReaction reaction) {
//		if(reactionsWithNonZeroStoichiometry==null){
//			reactionsWithNonZeroStoichiometry=new ArrayList<ICRNReaction>();
//		}
//		reactionsWithNonZeroStoichiometry.add(reaction);
//	}
	
	@Override
	public String toStringWithId() {
		return id+"-"+name;
	}
	
	@Override
	public String toString(){
		return name;
	}
	
	@Override
	public int getReferredArity() {
		return 2;
	}

	/*@Override
	public ISpecies[] getAllSpecies() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int[] getMultiplicities() {
		throw new UnsupportedOperationException();
	}*/
	
	@Override
	public ISpecies getAllSpecies(int pos) {
		if(pos==0){
			return this;
		}
		else{
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public int getMultiplicities(int pos) {
		if(pos==0){
			return 1;
		}
		else{
			throw new UnsupportedOperationException();
		}
	}
	
	@Override
	public void setMultiplicities(int pos, int val) {
		if(pos==0 && val ==1){
			//do nothing
		}
		else{
			throw new UnsupportedOperationException();
		}
	}
	
	@Override
	public int getNumberOfDifferentSpecies(){
		return 1;
	}

	@Override
	public boolean isUnary() {
		return true;
	}

	@Override
	public boolean isBinary() {
		return false;
	}

	@Override
	public boolean isTernaryOrMore() {
		return false;
	}

	@Override
	public ISpecies getSecondReagent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ISpecies getFirstReagent() {
		return this;
	}

	@Override
	public int getMultiplicityOfSpecies(ISpecies species) {
		if(this.compareTo(species)==0){
			return 1;
		}
		else{
			return 0;
		}
	}

	@Override
	public int getMultiplicityOfSpeciesWithId(int speciesId) {
		if(speciesId==getID()){
			return 1;
		}
		else{
			return 0;
		}
	}

	@Override
	public int computeArity() {
		return 1;
	}

	@Override
	public int getTotalMultiplicity() {
		return 1;
	}

	@Override
	public String toMultiSet() {
		return getName();
	}
	@Override
	public String toMultiSetCompact() {
		return "S"+getID();
	}
	
	@Override
	public String toMultiSetWithStoichiometries(boolean useSpaces) {
		return getName();
	}
	@Override
	public String toMultiSetWithStoichiometriesOrigNames(boolean useSpaces) {
		if(getOriginalName()!=null && getOriginalName().length()>0) {
			return getOriginalName();
		}
		else{
			return getName();
		}
	}

	@Override
	public String toMultiSetWithAlphaNumericNames() {
		return getNameAlphanumeric();
	}

	//private HashMap<String, String> spaceSeparatedMultiset;//=new HashMap<String, String>(2);
	private boolean algebraic;
	
	@Override
	public String getSpaceSeparatedMultisetAlphaNumeric() {
		return getNameAlphanumeric();
		/*
		if(spaceSeparatedMultiset==null){
			spaceSeparatedMultiset=new HashMap<String, String>(2);
		}
		
		String spaceSeparatedMS=spaceSeparatedMultiset.get("");
		if(spaceSeparatedMS==null || spaceSeparatedMS.equals("")){
			spaceSeparatedMS = getNameAlphanumeric();
			spaceSeparatedMultiset.put("", spaceSeparatedMS);
		}
		return spaceSeparatedMS;
		*/
	}

	@Override
	public String getSpaceSeparatedMultisetUsingIdsAsNames(String prefix, String suffix) {
//		if(spaceSeparatedMultiset==null){
//			spaceSeparatedMultiset=new HashMap<String, String>(2);
//		}
//		String spaceSeparatedMS=spaceSeparatedMultiset.get(suffix);
//		if(spaceSeparatedMS==null || spaceSeparatedMS.equals("")){
//			spaceSeparatedMS = " "+prefix+getID()+suffix;
//			spaceSeparatedMultiset.put(suffix, spaceSeparatedMS);
//		}
//		return spaceSeparatedMS;
		return " "+prefix+getID()+suffix;
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
	public String getMassActionExpression(boolean withAlphaNumericNames,String prefix,ISpecies specialSpecies, String specialPrefix,boolean ignoreI) {
		if(ignoreI && getName().equals(Species.I_SPECIESNAME)){
			return "1";
		}
		else{
			return (this.equals(specialSpecies) ? specialPrefix : prefix) + 
					(withAlphaNumericNames ? getNameAlphanumeric() : getName());
		}

	}
	
	@Override
	public String getMassActionExpressionReplacingSpeciesNames(String variableAsFunction, int idIncrement, boolean writeParenthesis, String suffix) {
		return getMassActionExpressionReplacingSpeciesNames(variableAsFunction,idIncrement,writeParenthesis,suffix,false);
	}
	
	@Override
	public String getMassActionExpressionReplacingSpeciesNames(String variableAsFunction, int idIncrement, boolean writeParenthesis, String suffix,boolean squaredParenthesis) {
		if(writeParenthesis){
			if(squaredParenthesis){
				return variableAsFunction+"["+(getID()+idIncrement)+"]";
			}
			else{
				return variableAsFunction+"("+(getID()+idIncrement)+")";
			}
			
		}
		else{
			return variableAsFunction+(getID()+idIncrement)+suffix;
		}
		
	}
	
	@Override
	public String getMassActionExpressionReplacingSpeciesNames(String variablePrefix, int idIncrement, String variableSuffix) {
			return variablePrefix+(getID()+idIncrement)+variableSuffix;
	}
	

	@Override
	public ISpecies getNthReagent(int pos) {
		if(pos==1){
			return this;
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean contains(ISpecies species) {
		return this.equals(species);
	}

	@Override
	public int getPosOfSpecies(ISpecies species) {
		if(this.equals(species)){
			return 0;
		}
		else{
			return -1;
		}
	}

	@Override
	public boolean contains(IComposite other) {
		if(other.getNumberOfDifferentSpecies()>1){
			return false;
		}
		else {
			if(other.getMultiplicities(0)>1){
				return false;
			}
			else{
				return other.getAllSpecies(0).equals(this);
			}
		}
	}
	
	@Override
	public IComposite apply(ICRNReaction reaction) {
		IComposite net = Composite.createFirstMinusSecond(reaction.getReagents(), reaction.getProducts());
		HashMap<ISpecies, Integer> resultHM = new HashMap<>(net.getNumberOfDifferentSpecies());
		resultHM.put(this, 1);
		for(int s=0;s<net.getNumberOfDifferentSpecies();s++){
			ISpecies species = net.getAllSpecies(s);
			int resMult = resultHM.get(species) + net.getMultiplicities(s);
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
	public HashMap<ISpecies, Integer> toHashMap() {
		HashMap<ISpecies, Integer> compositeHM = new HashMap<>(1);
		compositeHM.put(this, 1);
		return compositeHM;
	}

//	@Override
//	public void setRepresentedEquivalenceClass(Collection<ISpecies> speciesEq) {
//		this.representedEquivalenceClass=speciesEq;
//	}
//	
//	@Override
//	public Collection<ISpecies> getRepresentedEquivalenceClass(){
//		return representedEquivalenceClass;
//	}

	@Override
	public void setIsAlgebraic(boolean isAlgebraic) {
		this.algebraic=isAlgebraic;
	}
	
	@Override
	public boolean isAlgebraic() {
		return algebraic;
	}
	
	@Override
	public boolean isHomeo() {
		return false;
	}
	
	@Override
	public IComposite copyDecreasingMultiplicityOf(ISpecies xj) {
		if(this.equals(xj)) {
			return EmptyComposite.EMPTYCOMPOSITE;
		}
		else {
			return this;
		}
	}

	@Override
	public IMonomial toMonomials() {
		return new SpeciesMonomial(this);
	}
	
	

}
