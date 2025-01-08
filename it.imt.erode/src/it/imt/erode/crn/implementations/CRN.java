package it.imt.erode.crn.implementations;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math3.util.CombinatoricsUtils;
import org.eclipse.ui.console.MessageConsoleStream;
//import org.matheclipse.parser.client.math.MathUtils;
import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.ASTNode.Type;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.ICommand;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.crn.symbolic.constraints.IConstraint;
import it.imt.erode.expression.evaluator.MathEval;
import it.imt.erode.expression.parser.IMonomial;
import it.imt.erode.expression.parser.NumberMonomial;
import it.imt.erode.expression.parser.ProductMonomial;
import it.imt.erode.importing.AbstractImporter;
//import it.imt.erode.importing.AbstractImporter;
import it.imt.erode.importing.CRNImporter;
import it.imt.erode.importing.GUICRNImporter;
import it.imt.erode.importing.ODEorNET;
import it.imt.erode.importing.UnsupportedReactionNetworkEncodingException;
import it.imt.erode.partition.implementations.ICRNReactionComparatorToCollapse;
import it.imt.erode.partitionrefinement.algorithms.CRNBisimulationsNAry;

public class CRN implements ICRN {

	private List<ISpecies> allSpecies;
	private List<ICRNReaction> reactions;
	private boolean elementary;
	//private TreeSet<IComposite> products;
	private String name;
	private String[] viewNames;
	private String[] viewExpressions;
	private String[] viewExpressionsSupportedByMathEval;
	private boolean[] viewExpressionsUsesCovariances;
	private MathEval math;
	//The zero/null species of this CRN
	private ISpecies zeroSpecies;
	private boolean influenceNetwork=false;
	private int maxArity=0,minArity=Integer.MAX_VALUE;
	private int maxArityProducts=0;
	
	
	//private TreeSet<IComposite> reagents;
	private boolean massAction=true;
	private MessageConsoleStream out;
	private BufferedWriter bwOut;
	private ODEorNET modelDefKind=ODEorNET.RN;
	private ArrayList<HashSet<ISpecies>> userDefinedInitialPartition=new ArrayList<>(0);
	private int algebraicSpeciesAdded=0;
	
	//This list is used to maintain the list of parameters with no assigned value
	private List<String> symbolicParameters;
	//This list is used to store the constraints on the symbolicParameterd
	private List<IConstraint> constraints;
	//This list is used to maintain the original parameters
    private List<String> parameters;
    //This method sets information about the views/groups specified by the user. These information can then be used to prepartition the species according to the views/groups
	private List<HashMap<ISpecies,Integer>> setsOfSpeciesOfViews;
	
	private List<ICommand> commands;
	
	@Override
	public List<ICommand> getCommands() {
		return commands;
	}
	
	@Override
	public void addCommand(Command command) {
		if(commands==null){
			commands = new ArrayList<>();
		}
		commands.add(command);
	}
	
	@Override
	public MathEval getMath() {
		return math;
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		ICRN newcrn = new CRN(getName(), getSymbolicParameters(), getConstraints(), getParameters(),getMath(), getOut(), getBWOut());
		
		return newcrn;
	}
	
	public void setName(String name) {
		this.name=name;
	}
	
	public CRN(String name) {
		this(name,new MathEval(),null,null);
	}
	
	public CRN(String name, int expectedParams,int expectedSpecies, int expectedReactions) {
		this(name,new MathEval(),null,null);
		this.parameters=new ArrayList<String>(expectedParams);
		this.allSpecies=new ArrayList<ISpecies>(expectedSpecies);
		this.reactions=new ArrayList<ICRNReaction>(expectedReactions);
	}
	
	public CRN(String name, MathEval mathUsedWhileImporting, MessageConsoleStream out,BufferedWriter bwOut) {
		super();
		this.out=out;
		this.bwOut=bwOut;
		this.name=name;
		this.allSpecies = new ArrayList<ISpecies>();
		this.reactions = new ArrayList<ICRNReaction>();
		//this.products =  new TreeSet<IComposite>();
		//this.reagents = new TreeSet<IComposite>();
		this.parameters = new ArrayList<String>();
		this.symbolicParameters = new ArrayList<String>(0);
		this.constraints=new ArrayList<>(0);
		elementary=true;
		this.math=mathUsedWhileImporting;
		zeroSpecies=null;
		this.viewNames=new String[0];
		this.viewExpressions=new String[0];
		this.viewExpressionsSupportedByMathEval=new String[0];
		this.viewExpressionsUsesCovariances=new boolean[0];
	}

	public CRN(String name, List<String> symbolicParameters, List<IConstraint> constraints, List<String> parameters, MathEval mathUsedWhileImporting,MessageConsoleStream out,BufferedWriter bwOut) {
		this(name,mathUsedWhileImporting,out,bwOut);
		this.parameters=parameters;
		this.symbolicParameters=symbolicParameters;
		this.constraints=constraints;
	}

//	@Override
//	public TreeSet<IComposite> getProducts() {
//		return products;
//	}

	//@Override
//	private void addProduct(IComposite product) {
//		products.add(product);
//	}
	
//	@Override
//	public IComposite addProductIfNew(IComposite product) {
//		IComposite contained = products.ceiling(product);
//		if(contained==null || !contained.equals(product)){
//			if(product.isUnary()){
//				addProduct((IComposite)product.getFirstReagent());
//				return (IComposite)product.getFirstReagent();
//			}
//			else{
//				addProduct(product);
//				return product;
//			}
//		}
//		else{
//			return contained;
//		}
//	}
	
	//@Override
//	private void addReagent(IComposite newComposite) {
//		reagents.add(newComposite);
//	}
	
//	@Override
//	public IComposite addReagentsIfNew(IComposite reagent) {
//		IComposite contained = reagents.ceiling(reagent);
//		if(contained==null || !contained.equals(reagent)){
//			if(reagent.isUnary()){
//				addReagent((IComposite)reagent.getFirstReagent());
//				return (IComposite)reagent.getFirstReagent();
//			}
//			else{
//				addReagent(reagent);
//				return reagent;
//			}
//		}
//		else{
//			return contained;
//		}
//	}

	@Override
	public List<ISpecies> getSpecies() {
		return allSpecies;
	}
	
	@Override
	public int getSpeciesSize() {
		return allSpecies.size();
	}

	@Override
	public List<ICRNReaction> getReactions() {
		return reactions;
	}
	
	@Override
	public boolean isElementary() {
		return elementary;
	}
	
	@Override
	public void addSpecies(ISpecies species) {
		if(species.getName().equalsIgnoreCase("e")){
			CRNReducerCommandLine.printWarning(out,bwOut,"The use of \"e\" or \"E\" as name of a species is deprecated, as expressions containing it might be erroneously considered as numbers in scientific notation.");
		}
		allSpecies.add(species);
		if(species.isAlgebraic()) {
			algebraicSpeciesAdded++;
		}
	}
	
	@Override
	public void setSpecies(ISpecies species,int where) {
		if(where==allSpecies.size()) {
			addSpecies(species);
		}
		else {
			if(species.getName().equalsIgnoreCase("e")){
				CRNReducerCommandLine.printWarning(out,bwOut,"The use of \"e\" or \"E\" as name of a species is deprecated, as expressions containing it might be erroneously considered as numbers in scientific notation.");
			}
			
			if(allSpecies.get(where).isAlgebraic()) {
				algebraicSpeciesAdded--;
			}
			allSpecies.set(where, species);
			if(species.isAlgebraic()) {
				algebraicSpeciesAdded++;
			}
		}
	}

	@Override
	public boolean addReaction(ICRNReaction reaction) {
		if(maxArity<reaction.getArity()){
			maxArity=reaction.getArity();
		}
		if(minArity>reaction.getArity()){
			minArity=reaction.getArity();
		}
		int productsArity=reaction.getProducts().computeArity();
		if(maxArityProducts<productsArity){
			maxArityProducts=productsArity;
		}
		reactions.add(reaction);
		if(!reaction.isElementary()){
			elementary=false;
		}
		if(reaction.hasArbitraryKinetics()){
			elementary=false;
			massAction=false;
		}
		boolean hasZeroSpecies=false;
		if(containsTheZeroSpecies()){
			ISpecies zeroSpecies = getCreatingIfNecessaryTheZeroSpecies();
			if(reaction.getReagents().contains(zeroSpecies)||reaction.getProducts().contains(zeroSpecies)){
				hasZeroSpecies=true;
			}
		}
		return hasZeroSpecies;
	}

	@Override
	public boolean isMassAction(){
		return massAction;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	public static void addReactionsWithReagentsDifferentFromProducts(ICRN reducedCRN,
			List<ICRNReaction> reducedReactions) {
		for(ICRNReaction reducedReaction : reducedReactions){
			if(!reducedReaction.getReagents().equals(reducedReaction.getProducts()))
			{
				reducedCRN.addReaction(reducedReaction);
				//zeroSpeciesAppearsInReactions = zeroSpeciesAppearsInReactions || appears;
				/*
				AbstractImporter.addToIncomingReactionsOfProducts(reducedReaction.getArity(),reducedReaction.getProducts(), reducedReaction,CRNReducerCommandLine.addReactionToComposites);
				AbstractImporter.addToOutgoingReactionsOfReagents(reducedReaction.getArity(), reducedReaction.getReagents(), reducedReaction,CRNReducerCommandLine.addReactionToComposites);
				if(CRNReducerCommandLine.hasToAddToReactionsWithNonZeroStoichiometry){
					AbstractImporter.addToReactionsWithNonZeroStoichiometry(reducedReaction.getArity(), reducedReaction.computeProductsMinusReagentsHashMap(),reducedReaction);
				}
				*/
			}
		}
	}
	
	/**
	 * This method should be invoked on mass action reaction networks only
	 * @param reducedCRN
	 * @param reducedReactions
	 * @param collapseReactionsWithSameReagentsAndRates
	 * @return true if the zero species is present, but it has been made unnecessary (and thus removed) by the combination of reactions with same reagents and rates
	 */
	@SuppressWarnings("unused")
	public static boolean collapseAndCombineAndAddReactions(ICRN reducedCRN, List<ICRNReaction> reducedReactions, MessageConsoleStream out,BufferedWriter bwOut){
		//System.out.println("Before collapsing we have "+reducedReactions.size()+" reactions.");
		if(reducedReactions.isEmpty()){
			return false;
		}
		Collections.sort(reducedReactions, new ICRNReactionComparatorToCollapse());

		//I first collapse reactions with same reagents and products
		List<ICRNReaction> collapsedReducedReactions = new ArrayList<>(reducedReactions.size());
		ICRNReaction current=null;
		//boolean hasArbitraryReactions =false;
		for (ICRNReaction reducedReaction : reducedReactions) {
			if(!reducedReaction.getReagents().equals(reducedReaction.getProducts()))
			{
				if(current!=null && current.getReagents().equals(reducedReaction.getReagents()) && current.getProducts().equals(reducedReaction.getProducts())){
					//Begin set name
					String currentId = current.getID();
					String reducedReactionId = reducedReaction.getID();
					String cumulativeId = null;
					if(currentId == null || currentId.equals("")){
						cumulativeId = reducedReactionId;
					}
					else{
						if(reducedReactionId == null || reducedReactionId.equals("")){
							cumulativeId = currentId;
						}
						else{
							cumulativeId = currentId+" "+reducedReactionId;
						}
					}
					current.setID(cumulativeId);
					//End set name
					
					if(current instanceof CRNReactionArbitraryAbstract){
						//hasArbitraryReactions=true;
						CRNReactionArbitraryAbstract currentAbstract = (CRNReactionArbitraryAbstract) current; 
						ASTNode sum = new ASTNode(Type.PLUS);
						sum.addChild(currentAbstract.getRateLaw());
						if(reducedReaction instanceof CRNReactionArbitraryAbstract){
							sum.addChild(((CRNReactionArbitraryAbstract) reducedReaction).getRateLaw());
						}
						else{
							sum.addChild(new ASTNode(reducedReaction.getRate().doubleValue()));
						}
						currentAbstract.setRateLaw(sum);
					}
					else{
						if(reducedReaction instanceof CRNReactionArbitraryAbstract){
							//hasArbitraryReactions=true;
							//current (the last reaction added to collapsedReducedReactions) is mass action, while the new one is arbitrary.
							//I have to collapse the two, in an arbitrary reaction. Hence I first have to remove the 'mass-action' one from collapsedReducedReactions.
							CRNReactionArbitraryAbstract reducedReactionAbstract = (CRNReactionArbitraryAbstract) reducedReaction;
							collapsedReducedReactions.remove(collapsedReducedReactions.size()-1);
							collapsedReducedReactions.add(reducedReactionAbstract);
							ASTNode sum = new ASTNode(Type.PLUS);
							sum.addChild(reducedReactionAbstract.getRateLaw());
							sum.addChild(new ASTNode(current.getRate().doubleValue()));						
							reducedReactionAbstract.setRateLaw(sum);
						}
						else{
							if(CRNReducerCommandLine.KEEPTRACEOFRATEEXPRESSIONS || current.getRate()==null || reducedReaction.getRate()==null){
								if(current.getRate()==null || reducedReaction.getRate()==null){
									current.setRate(null, current.getRateExpression()+"+"+reducedReaction.getRateExpression());
								}
								else{
									current.setRate(current.getRate().add(reducedReaction.getRate()), current.getRateExpression()+"+"+reducedReaction.getRateExpression());
								}
							}
							else{
								BigDecimal rate = current.getRate().add(reducedReaction.getRate()); 
								current.setRate(rate, String.valueOf(rate.doubleValue()));
							}
							
						}
					}

 				}
				else{
					current = reducedReaction;
					collapsedReducedReactions.add(current);
				}
			}
		}

		//Now I combine reactions with same reagents and rate
		/*if(collapseReactionsWithSameReagentsAndRates){
			if(hasArbitraryReactions){
				CRNReducerCommandLine.printWarning(out,bwOut, "It is currently forbidden to compose reactions with same products and rates of models with arbitrary rates");
			}
			else{
				ISpecies zeroSpecies=null;
				if(reducedCRN.containsTheZeroSpecies()){
					zeroSpecies = reducedCRN.getCreatingIfNecessaryTheZeroSpecies();
				}

				//Here I have to sort according to reagents and rate
				List<ICRNReaction> collapsedSameProductsAndRates = new ArrayList<>(collapsedReducedReactions.size());
				current=null;
				HashMap<ISpecies, Integer> currentCombinedProducts = null;
				boolean combined=false;
				for(ICRNReaction collapsedReaction : collapsedReducedReactions){
					if(current!=null && current.getReagents().equals(collapsedReaction.getReagents()) && (current.getRate().compareTo(collapsedReaction.getRate())==0)){
						combineProducts(currentCombinedProducts, collapsedReaction.getProducts());
						combined=true;
					}
					else{
						if(current!=null){
							//I have to create a new reaction with the coombinedProducts....
							computeCombinedReaction(out, current, collapsedSameProductsAndRates, currentCombinedProducts,combined,zeroSpecies);
						}
						current = collapsedReaction;
						currentCombinedProducts = new HashMap<>();
						combined=false;
					}
				}

				//The last combined reaction is not added in the loop
				if(current!=null){
					computeCombinedReaction(out, current, collapsedSameProductsAndRates, currentCombinedProducts, combined,zeroSpecies);
				}


				collapsedReducedReactions = new ArrayList<ICRNReaction>(collapsedSameProductsAndRates.size());
				for(ICRNReaction collapsedReaction : collapsedSameProductsAndRates){
					IComposite reducedReagentsOfReaction = collapsedReaction.getReagents();
					IComposite reducedProductsOfReaction = collapsedReaction.getProducts();
					if(! reducedReagentsOfReaction.equals(reducedProductsOfReaction)){
						if(CRNReducerCommandLine.univoqueReagents){
							reducedReagentsOfReaction = reducedCRN.addReagentsIfNew(reducedReagentsOfReaction);
						}
						if(CRNReducerCommandLine.univoqueProducts){
							reducedProductsOfReaction = reducedCRN.addProductIfNew(reducedProductsOfReaction);
						}
						if(collapsedReaction.hasArbitraryKinetics()){
							throw new UnsupportedOperationException("This is not a massaction CRN. Use method: it.imt.erode.partitionrefinement.algorithms.SMTOrdinaryFluidBisimilarityBinary.addToListOfReducedReactionsNonMassAction");
						}
						else{
							ICRNReaction reducedReaction = new CRNReaction(collapsedReaction.getRate(), reducedReagentsOfReaction, reducedProductsOfReaction, ((CRNReaction)collapsedReaction).getRateExpression());
							collapsedReducedReactions.add(reducedReaction);
						}
					}
				}
			}
		}*/

		//boolean zeroSpeciesAppearsInReactions=false;
		addReactions(reducedCRN, collapsedReducedReactions);		

		/*if(reducedCRN.containsTheZeroSpecies() && !zeroSpeciesAppearsInReactions){
			reducedCRN.removeUnusedZeroSpecies();
			return true;
		}
		else{
			return false;
		}*/
		//System.out.println("After collapsing we have "+reducedCRN.getReactions().size()+" reactions.");
		return false;
	}

	public static void addReactions(ICRN reducedCRN, List<ICRNReaction> collapsedReducedReactions) {
		for(ICRNReaction collapsedReaction : collapsedReducedReactions){
			reducedCRN.addReaction(collapsedReaction);
			//zeroSpeciesAppearsInReactions = zeroSpeciesAppearsInReactions || appears;
			/*
			AbstractImporter.addToIncomingReactionsOfProducts(collapsedReaction.getArity(),collapsedReaction.getProducts(), collapsedReaction,CRNReducerCommandLine.addReactionToComposites);
			AbstractImporter.addToOutgoingReactionsOfReagents(collapsedReaction.getArity(), collapsedReaction.getReagents(), collapsedReaction,CRNReducerCommandLine.addReactionToComposites);
			if(CRNReducerCommandLine.hasToAddToReactionsWithNonZeroStoichiometry){
				AbstractImporter.addToReactionsWithNonZeroStoichiometry(collapsedReaction.getArity(), collapsedReaction.computeProductsMinusReagentsHashMap(),collapsedReaction);
			}
			*/
		}
	}
	
//	protected static void addToListOfReducedReactions(IPartition partition,
//			ICRN reducedCRN, ISpecies[] speciesIdToSpecies,
//			HashMap<String, ISpecies> speciesNameToSpecies, HashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,
//			List<ICRNReaction> reducedReactions, ICRNReaction reaction,
//			IComposite reducedReagentsOfReaction,
//			IComposite reducedProductsOfReaction) throws IOException {
//		
//		if(! reducedReagentsOfReaction.equals(reducedProductsOfReaction)){
//			if(CRNReducerCommandLine.univoqueReagents){
//				reducedReagentsOfReaction = reducedCRN.addReagentsIfNew(reducedReagentsOfReaction);
//			}
//			if(CRNReducerCommandLine.univoqueProducts){
//				reducedProductsOfReaction = reducedCRN.addProductIfNew(reducedProductsOfReaction);
//			}
//			if(reaction.hasArbitraryKinetics()){
//				throw new UnsupportedOperationException("This is not a massaction CRN. Use method: it.imt.erode.partitionrefinement.algorithms.SMTOrdinaryFluidBisimilarityBinary.addToListOfReducedReactionsNonMassAction");
//			}
//			else{
//				ICRNReaction reducedReaction = new CRNReaction(reaction.getRate(), reducedReagentsOfReaction, reducedProductsOfReaction, reaction.getRateExpression(),reaction.getID());
//				//reducedCRN.addReaction(reducedReaction);
//				//AbstractImporter.addToIncomingReactionsOfProducts(reducedReaction.getArity(),reducedProductsOfReaction, reducedReaction);
//				reducedReactions.add(reducedReaction);
//			}
//		}
//	}
	
	

	/*private static void computeCombinedReaction(MessageConsoleStream out, ICRNReaction current,List<ICRNReaction> collapsedSameProductsAndRates, HashMap<ISpecies, Integer> currentCombinedProducts,boolean combined, ISpecies zeroSpecies) {
		if(combined){
			combineProducts(currentCombinedProducts,current.getProducts());
			
			if(zeroSpecies!=null){
				Integer multOfZeroSpecies = currentCombinedProducts.get(zeroSpecies);
				if(multOfZeroSpecies!=null){
					if(currentCombinedProducts.size()>1){
						//I can discard the sink/zero species
						currentCombinedProducts.remove(zeroSpecies);
					}
					else{
						//I can set multiplicity of the zero species to one
						currentCombinedProducts.put(zeroSpecies, 1);
					}
				}
			}
			try {
				current = current.cloneReplacingProducts(new Composite(currentCombinedProducts));
			} catch (IOException e) {
				CRNReducerCommandLine.printStackTrace(out, e);
			}
		}
		collapsedSameProductsAndRates.add(current);
	}*/
	
	/*private static void combineProducts(HashMap<ISpecies, Integer> currentCombinedProducts, IComposite products) {
		for(int s = 0; s< products.getNumberOfDifferentSpecies();s++){
			int mult = products.getMultiplicities(s);
			ISpecies species = products.getAllSpecies(s);
			Integer currentMult = currentCombinedProducts.get(species);
			if(currentMult!=null){
				mult+=currentMult;
			}
			currentCombinedProducts.put(species, mult);
		}
	}*/

//	@Override
//	public TreeSet<IComposite> getReagents() {
//		return reagents;
//	}
	
	@Override
	public Set<IComposite> computeSetOfReagents() {
		Set<IComposite> reagents = new HashSet<>();
		for(ICRNReaction reaction : getReactions()) {
			reagents.add(reaction.getReagents());
		}
		return reagents;
	}
	@Override
	public Set<IComposite> computeSetOfProducts() {
		Set<IComposite> products = new HashSet<>();
		for(ICRNReaction reaction : getReactions()) {
			products.add(reaction.getProducts());
		}
		return products;
	}
	
	
	@Override
	public void printCRN(){
		CRNReducerCommandLine.println(out,bwOut, toString());
		//System.out.println(toString());
	}
	
	@Override
	public String toString() {
		StringBuilder sb=null;
		
		String params=" ";
		if(getParameters()!=null && getParameters().size()>0) {
			params=getParameters().size()+" parameters, ";
		}
		
		if(getName()==null){
			//sb = new StringBuilder(getReactions().size()+" reactions, "+getSpecies().size()+" species, "+getProducts().size()+" products.\n");
			sb = new StringBuilder(params+getReactions().size()+" reactions, "+getSpecies().size()+" species.\n");
		}
		else{
			//sb = new StringBuilder(getName()+": "+getReactions().size()+" reactions, "+getSpecies().size()+" species, "+getProducts().size()+" products.\n");
			sb = new StringBuilder(getName()+": "+params+getReactions().size()+" reactions, "+getSpecies().size()+" species.\n");
		}
		if(getParameters()!=null && getParameters().size()>0) {
			sb.append("Parameters:\n");
//			for(String p : getParameters()) {
//				sb.append(p);
//				sb.append(" ");
//			}
			sb.append(getParameters().toString());
			sb.append("\n");
			//sb.append(getParameters().toString());
		}
		sb.append("Species:\n");
		sb.append(getSpecies().toString());
		sb.append("\n Reactions:\n");
		sb.append(getReactions());
		return sb.toString();
	}

	@Override
	public String toStringShort() {
		StringBuilder sb = new StringBuilder();
		//String in="";
		if(isInfluenceNetwork()){
			//in="Influence network. ";
			sb.append("Influence network. ");
		}
		if(getName()!=null) {
			sb.append(getName());
			sb.append(".\n\t");
		}
		sb.append(getSpecies().size()+" "+((getMdelDefKind().equals(ODEorNET.ODE)?"variables":"species")));
		if(!this.modelDefKind.equals(ODEorNET.ODE)) {
			sb.append(", "+getReactions().size()+" reactions");
		}
		
		/*
		if(getName()==null){
			//return in+getReactions().size()+" reactions, "+getSpecies().size()+" species, "+getReagents().size()+" reagents, "+getProducts().size()+" products.";
			in= in+getReactions().size()+" reactions, "+getSpecies().size()+" species";
		}
		else{
			//return getName()+": "+in+getReactions().size()+" reactions, "+getSpecies().size()+" species, "+getReagents().size()+" reagents, "+getProducts().size()+" products.";
			in = getName()+".\n\t"+in+getReactions().size()+" reactions, "+getSpecies().size()+" species";
		}
		*/
		/*if(getReagents().size()>0){
			in+=", " + getReagents().size() + " reagents ";
		}
		if(getProducts().size()>0){
			in+=", " + getProducts().size() + " products ";
		}*/
		sb.append(". ");
		
		return sb.toString();
	}
	
	@Override
	public void addSymbolicParameter(String parameterName) {
		if(parameterName.equalsIgnoreCase("e")){
			CRNReducerCommandLine.printWarning(out,bwOut,"The use of \"e\" or \"E\" as name of a parameter is deprecated, as expressions containing it might be erroneously considered as numbers in scientific notation.");
		}
		symbolicParameters.add(parameterName);
	}
	
	@Override
	public void addConstraint(IConstraint constraint) {
		constraints.add(constraint);
	}
	
	@Override
	public void addParameter(String parameterName, String parameterExpression) {
		addParameter(parameterName, parameterExpression,false);
	}
	
	@Override
	public void addParameter(String parameterName, String parameterExpression, boolean updateMathEval) {
		if(parameterName.equalsIgnoreCase("e")){
			CRNReducerCommandLine.printWarning(out,bwOut,"The use of \"e\" or \"E\" as name of a parameter is deprecated, as expressions containing it might be erroneously considered as numbers in scientific notation.");
		}
		parameters.add(parameterName+" "+parameterExpression);
		if(updateMathEval) {
			AbstractImporter.updateMathEval(parameterName, parameterExpression, true, math);
		}
	}
	
	@Override
	public double setParameter(String parameterName, String parameterExpression) {
		//First update the value. 
		double val;
		try  
		{  
			val = Double.valueOf(parameterExpression);  
		}  
		catch(NumberFormatException nfe)  
		{  
			val = math.evaluate(parameterExpression);  
		}
		getMath().setVariable(parameterName, val);
		
		//Then update the expression
		String parameterNameAndSpace = parameterName+" ";
		boolean replaced=false;
		int i=0;
		for(;i<parameters.size();i++){
			if(parameters.get(i).startsWith(parameterNameAndSpace)){
				parameters.remove(i);
				parameters.add(i, parameterNameAndSpace+parameterExpression);
				replaced=true;		
				break;
			}
		}
		
		//If we added a new parameter we are done, otherwise we have to update the values of the parameters, the rates of the reaction, and the IC
		if(!replaced){
			parameters.add(parameterNameAndSpace+parameterExpression);
		}
		else{
			//I have to update the values of 1) the parameters, 2) the rates of the reactions, 3) the initial conditions of the species.
			//parameters (I start from the parameters following the modified one, i.e. i).
			i++;
			for(;i<parameters.size();i++){
				String paramAndExpr=parameters.get(i);
				int space = paramAndExpr.indexOf(' ');
				String param = paramAndExpr.substring(0,space);
				String expr = paramAndExpr.substring(space+1,paramAndExpr.length());
				double parameterValue;
				try  
				{  
					parameterValue = Double.valueOf(expr);  
				}  
				catch(NumberFormatException nfe)  
				{  
					//I update math only if the expression is not a raw number, but is an arithmetical expression.
					parameterValue = math.evaluate(expr);
					getMath().setVariable(param, parameterValue);
				}  			
				
			}
			//IC
			for(ISpecies species : allSpecies){
				String icExpr= species.getInitialConcentrationExpr();
				species.setInitialConcentration(BigDecimal.valueOf(getMath().evaluate(icExpr)),icExpr);
			}
			//rates of reactions
			for (ICRNReaction reaction : reactions) {
				if(!reaction.hasArbitraryKinetics()){
					String rateExpr = reaction.getRateExpression();
					reaction.setRate(BigDecimal.valueOf(getMath().evaluate(rateExpr)),rateExpr);
				}
			}
		}
		return val;
	}
	
	@Override
	public List<String> getParameters(){
		return parameters;
	}
	
	@Override
	public List<String> getSymbolicParameters(){
		return symbolicParameters;
	}
	
	@Override
	public List<IConstraint> getConstraints() {
		return constraints;
	}

	@Override
	public String[] createArrayOfAllSpeciesNames() {
		String[] speciesName = new String[allSpecies.size()];
		int i=0;
		for (ISpecies iSpecies : allSpecies) {
			speciesName[i]=iSpecies.getName();
			i++;
		}
		return speciesName;
	}
	
	@Override
	public void setViews(String[] viewNames, String[] viewExprs, String[] viewExprsSupportedByMathEval, boolean[] viewExpressionsUsesCovariances) {
		this.viewNames=viewNames;
		this.viewExpressions=viewExprs;
		this.viewExpressionsSupportedByMathEval=viewExprsSupportedByMathEval;
		this.viewExpressionsUsesCovariances=viewExpressionsUsesCovariances;
	}

	@Override
	public String[] getViewNames() {
		return viewNames;
	}

	
	 @Override
	 public String[] getViewExpressions() {
		return viewExpressions;
	}

	@Override
	public String[] getViewExpressionsSupportedByMathEval() {
		return viewExpressionsSupportedByMathEval;
	}
	
	 @Override
	 public boolean[] getViewExpressionsUsesCovariances() {
		return viewExpressionsUsesCovariances;
	}

	@Override
	public void setNewMath(MathEval math) {
		this.math=math;
		
	}

	@Override
	public boolean setIC(String speciesName, double ic, String icExpr) {
		for (ISpecies species : allSpecies) {
			if(species.getName().equalsIgnoreCase(speciesName)){
				species.setInitialConcentration(BigDecimal.valueOf(ic),icExpr);
				return true;
			}
		}
		return false;
		
	}

	@Override
	public boolean containsTheZeroSpecies() {
		return zeroSpecies!=null;
	}

	@Override
	public ISpecies getCreatingIfNecessaryTheZeroSpecies() {
		return getCreatingIfNecessaryTheZeroSpecies(Species.ZEROSPECIESNAME);
	}
	
	@Override
	public ISpecies getCreatingIfNecessaryTheZeroSpecies(String name) {
		if(!containsTheZeroSpecies()){
			setZeroSpecies(new Species(name/*Species.ZEROSPECIESNAME*/, null, allSpecies.size(), BigDecimal.ZERO, "0",false));
		}
		return zeroSpecies;
	}
	
	@Override
	public boolean isZeroSpecies(ISpecies species) {
		if(zeroSpecies==null){
			return false;
		}
		else{
			return zeroSpecies.equals(species);
		}
	}
	
	
	public static ISpecies createZeroSpecies(int id){
		return new Species(Species.ZEROSPECIESNAME, null, id, BigDecimal.ZERO, "0",false);
	}
	
	/**
	 * This method should be invoked only while computing a BB/BDE reduced mass action model. The reduction might have created the zero species. But the "combination" of reactions with same reagents and rates might have made it unnecessary. 
	 */
	@Override
	public void removeUnusedZeroSpecies() {
		if(containsTheZeroSpecies()){
			//The zeroSpecies is always the last one
			/*for (ISpecies species : allSpecies) {
				if(species.getID()>zeroSpecies.getID()){
					species.decreaseID();
				}
			}*/
			if(zeroSpecies.getID()!=allSpecies.size()-1){
				throw new UnsupportedOperationException("It is expected that the sink/null species is stored as last one.");
			}
			allSpecies.remove(zeroSpecies);
			zeroSpecies=null;
		}
	}
	
	//@Override
	private void setZeroSpecies(ISpecies zeroSpecies) {
		this.zeroSpecies = zeroSpecies;
		addSpecies(zeroSpecies);
		zeroSpecies.addCommentLine("This is the null species. It has been added because some reactions became with empty products after the transformations.");
	}

	@Override
	public boolean isInfluenceNetwork() {
		return influenceNetwork;
	}

	@Override
	public void setInfluenceNetwork(boolean isInfluenceNetwork) {
		this.influenceNetwork=isInfluenceNetwork;
	}

	@Override
	public void setViewsAsMultiset(List<HashMap<ISpecies,Integer>> setsOfSpeciesOfViews) {
		this.setsOfSpeciesOfViews=setsOfSpeciesOfViews;
	}

	@Override
	public List<HashMap<ISpecies,Integer>> getViewsAsMultiset() {
		return setsOfSpeciesOfViews;
	}

	@Override
	public int getMaxArity() {
		return maxArity;
	}
	
	@Override
	public int getMinArity() {
		return minArity;
	}

	@Override
	public void setExpectedNumberOfReactions(int numberOfReactions) {
		if(reactions!=null && reactions.size()>0) {
			throw new UnsupportedOperationException("CRN.setExpectedNumberOfSpecies: There are already"+reactions.size()+" reactions.");
		}
		this.reactions = new ArrayList<ICRNReaction>(numberOfReactions);
	}
	
	@Override
	public void setExpectedNumberOfSpecies(int numberOfSpecies) {
		if(allSpecies!=null && allSpecies.size()>0) {
			throw new UnsupportedOperationException("CRN.setExpectedNumberOfSpecies: There are already"+allSpecies.size()+" species.");
		}
		this.allSpecies = new ArrayList<ISpecies>(numberOfSpecies);
	}

	@Override
	public void setMdelDefKind(ODEorNET modelDefKind) {
		this.modelDefKind=modelDefKind;
	}

	@Override
	public ODEorNET getMdelDefKind() {
		return modelDefKind;
	}

	@Override
	public void setUserDefinedPartition(ArrayList<HashSet<ISpecies>> userDefinedInitialPartition) {
		this.userDefinedInitialPartition=userDefinedInitialPartition;
	}
	@Override
	public ArrayList<HashSet<ISpecies>> getUserDefinedPartition() {
		return userDefinedInitialPartition;
	}

	@Override
	public boolean isSymbolic() {
		return symbolicParameters.size()>0;
	}

	@Override
	public boolean hasAtoMostBinaryProducts() {
		return maxArityProducts<=2;
	}

	@Override
	public MessageConsoleStream getOut() {
		return out;
	}
	@Override
	public BufferedWriter getBWOut() {
		return bwOut;
	}
	
	@Override
	public int algebraicSpecies() {
		return algebraicSpeciesAdded;
	}
	
	public static ICRN halveRatesOfHomeoReactions(ICRN crn, MessageConsoleStream out, BufferedWriter bwOut) throws IOException {
		CRNReducerCommandLine.print(out,bwOut," (halving rates of homeoreactions ");
		ICRN crnNew = new CRN(crn.getName(),crn.getMath(),out,bwOut);
	
		copyParameters(crn, crnNew);
		ISpecies[] idToNewSpecies = copySpecies(crn, crnNew);
		copyReactions(crn, crnNew, idToNewSpecies);
		
		BigDecimal two = BigDecimal.valueOf(2);
		int howMany = 0;
		for (ICRNReaction reaction : crnNew.getReactions()) {
			if(reaction.isElementary() && reaction.isBinary() && reaction.getReagents().getFirstReagent().equals(reaction.getReagents().getSecondReagent())) {
				reaction.setRate(reaction.getRate().divide(two), "("+reaction.getRateExpression()+")/2");
				howMany++;
			}
		}
		CRNReducerCommandLine.print(out,bwOut, howMany+") ");
		
		return crnNew;
	}
	
	public static ICRN copyCRN(ICRN crn, MessageConsoleStream out, BufferedWriter bwOut) throws IOException {
		return copyCRN(crn, out, bwOut, true,true,true);
	}
	public static ICRN copyCRN(ICRN crn, MessageConsoleStream out, BufferedWriter bwOut, 
			boolean copyParameters, boolean copyReactions) throws IOException {
		return copyCRN(crn, out, bwOut, copyParameters,copyReactions,true);
	}
	public static ICRN copyCRN(ICRN crn, MessageConsoleStream out, BufferedWriter bwOut, 
			boolean copyParameters, boolean copyReactions,boolean copySpecies) throws IOException {
		ICRN crnNew = new CRN(crn.getName(),crn.getMath(),out,bwOut);
		if(copyParameters) {
			copyParameters(crn, crnNew);
		}
		
		ISpecies[] idToNewSpecies =null;
		if(copySpecies) {
			idToNewSpecies = copySpecies(crn, crnNew);
		}
		if(copyReactions) {
			copyReactions(crn, crnNew, idToNewSpecies);
		}
		return crnNew;
	}
	
	public static ICRN exportMACRN(ICRN crn, MessageConsoleStream out, BufferedWriter bwOut) {
		CRNReducerCommandLine.print(out,bwOut,"Making mass-action explicit... ");
		ICRN crnNew = new CRN(crn.getName(),crn.getMath(),out,bwOut);
		crnNew.setMdelDefKind(ODEorNET.RN);
		
		copyParameters(crn, crnNew);
		HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpecies().size());
		for (ISpecies species : crn.getSpecies()) {
			speciesNameToSpecies.put(species.getName(), species);
			ISpecies newSpecies = species.cloneWithoutReactions();
			crnNew.addSpecies(newSpecies);
		}
		
		for (ICRNReaction reaction : crn.getReactions()) {
			//Mass action reactions do not have to be modified
			if(!reaction.hasArbitraryKinetics()){
				crnNew.addReaction(reaction);
			}
			else{
				ArrayList<IMonomial> monomials;
				try{
					monomials= GUICRNImporter.parseGUIPolynomialArbitraryRateExpression(((CRNReactionArbitraryGUI)reaction).getRateLaw(),speciesNameToSpecies,crnNew.getMath());
				}
				catch(UnsupportedReactionNetworkEncodingException e){
					CRNReducerCommandLine.println(out,bwOut, "\nThe model cannot be encoded as a mass action reaction network. Problems in computing the monomials of\n\t"+reaction);
					return null;
				}
				
				String id = reaction.getID();
				
				int forw=0;
				int rev=0;
				for(IMonomial mon : monomials) {
					BigDecimal coeff = mon.getOrComputeCoefficient();
					if(coeff.compareTo(BigDecimal.ZERO) >0) {
						//forward reaction
						HashMap<ISpecies, Integer> reagentSpecies=new HashMap<ISpecies, Integer>(3);
						mon.computeSpecies(reagentSpecies);
						IComposite comp = new Composite(reagentSpecies);
						if(reaction.getReagents().equals(comp)) {
							CRNReaction maReaction = new CRNReaction(coeff,reaction.getReagents(),reaction.getProducts(),mon.getOrComputeCoefficientExpression(),computeID(id,"forw",forw++));
							crnNew.addReaction(maReaction);
							if(forw==1) {
								maReaction.addCommentLines(reaction.getComments());
							}
							
						}
						else {
							CRNReducerCommandLine.println(out,bwOut, "\nThe model cannot be encoded as a mass action reaction network.\nThe reagents do not match the species in the positive monomials\n\t"+reaction);
							return null;
						}
					}
					else if(coeff.compareTo(BigDecimal.ZERO) <0){
						//reverse reaction
						HashMap<ISpecies, Integer> productSpecies=new HashMap<ISpecies, Integer>(3);
						mon.computeSpecies(productSpecies);
						IComposite comp = new Composite(productSpecies);
						if(reaction.getProducts().equals(comp)) {
							coeff = coeff.abs();
							IMonomial minusMon = new ProductMonomial(new NumberMonomial(BigDecimal.ZERO.subtract(BigDecimal.ONE), "-1"),mon);
							String coeffExpr = minusMon.getOrComputeCoefficientExpression();
							CRNReaction maReaction = new CRNReaction(coeff,reaction.getProducts(),reaction.getReagents(),coeffExpr,computeID(id,"rev",rev++));
							crnNew.addReaction(maReaction);
						}
						else {
							CRNReducerCommandLine.println(out,bwOut, "\nThe model cannot be encoded as a mass action reaction network.\nThe products (the reagents of the reverse reaction) do not match the species in the negative monomials\n\t"+reaction);
							return null; 
						}
					}
				}
				
				
				/*
				//If the reaction is X -> X+X, arbitrary drift
				if(reaction.isODELike()){
					try{
						boolean anyReactionUsesI = computeRNEncoding((CRNReactionArbitraryGUI)reaction,reaction.getReagents().getFirstReagent(),speciesNameToSpecies,crn.getMath(),I,rnReactions);
						anyReactionFromRNEncodingUsesI = anyReactionFromRNEncodingUsesI || anyReactionUsesI; 
					}
					catch(UnsupportedReactionNetworkEncodingException e){
						computeRNEncoding=false;
						CRNReducerCommandLine.print(out,bwOut, "\n The model cannot be encoded as a mass action reaction network.\n I encode it as an arbitrary one...");
						break;
					}
				}
				else{
					try{
						boolean anyReactionUsesI = GUICRNImporter.computeRNEncodingOfArbitraryReaction((CRNReactionArbitraryGUI)reaction,speciesNameToSpecies,crn.getMath(),I,rnReactions,null);
						anyReactionFromRNEncodingUsesI = anyReactionFromRNEncodingUsesI || anyReactionUsesI;
					}
					catch(UnsupportedReactionNetworkEncodingException e){
						computeRNEncoding=false;
						CRNReducerCommandLine.print(out,bwOut, "\n The model cannot be encoded as a mass action reaction network.\n I encode it as an arbitrary one...");
						break;
					}
				}
				*/
			}
		}
		
		CRNReducerCommandLine.println(out,bwOut,"completed");
		
		return crnNew;
		
	}
	
	private static String computeID(String id, String forwRev, int i) {
		String idr = (id==null)?null:id+"_"+forwRev;
		if(i>1) {
			idr+=i;
		}
		return idr;
	}

	public static ICRN deterministicCorrection(ICRN crn, MessageConsoleStream out, BufferedWriter bwOut) throws IOException {
		CRNReducerCommandLine.print(out,bwOut," (applying deterministic correction to ");
		ICRN crnNew = new CRN(crn.getName(),crn.getMath(),out,bwOut);
	
		copyParameters(crn, crnNew);
		ISpecies[] idToNewSpecies = copySpecies(crn, crnNew);
		copyReactions(crn, crnNew, idToNewSpecies);
		
		BigDecimal two = BigDecimal.valueOf(2);
		int howMany = 0;
		for (ICRNReaction reaction : crnNew.getReactions()) {	
			//For elementary CRNs, I just have to halve the rate of reactions 
			if(reaction.isElementary() && reaction.isBinary() && reaction.getReagents().getFirstReagent().equals(reaction.getReagents().getSecondReagent())) {
				reaction.setRate(reaction.getRate().divide(two), "("+reaction.getRateExpression()+")/2");
				howMany++;
			}
			else if(reaction.getReagents().isTernaryOrMore() && reaction.getReagents().getNumberOfDifferentSpecies()<reaction.getArity()) {
				long num = 1;
				for(int i=0;i<reaction.getReagents().getNumberOfDifferentSpecies();i++) { 
					int m = reaction.getReagents().getMultiplicities(i);
					if(m>1) {
						num = num*CombinatoricsUtils.factorial(m);
						//num = num*MathUtils.factorial(m);
					}
				}
				
				if(num>1) {
					BigDecimal bd = BigDecimal.valueOf(num);
					String exp = "("+reaction.getRateExpression()+")/"+num;
					reaction.setRate(reaction.getRate().divide(bd,CRNBisimulationsNAry.getSCALE(),CRNBisimulationsNAry.RM), exp);	
				}
				howMany++;
			}
		}
		crnNew.setViewsAsMultiset(crn.getViewsAsMultiset());
		CRNReducerCommandLine.print(out,bwOut, howMany+" reactions) ");
		
		return crnNew;
	}
		
	public static ISpecies[] copySpecies(ICRN crn, ICRN crnNew) {
		ISpecies[] idToNewSpecies=new ISpecies[crn.getSpecies().size()];
		for (ISpecies species : crn.getSpecies()) {
			ISpecies newSpecies = species.cloneWithoutReactions();
			crnNew.addSpecies(newSpecies);
			idToNewSpecies[species.getID()]=newSpecies;
		}
		return idToNewSpecies;
	}
	
	public static void copyParameters(ICRN crn, ICRN crnNew) {
		for (String param : crn.getParameters()) {
			int space = param.indexOf(' ');
			String paramName = param.substring(0, space);
			String paramExpr = param.substring(space,param.length());
			AbstractImporter.addParameter(paramName, paramExpr, true, crnNew);
			//crnNew.addParameter(paramName, paramExpr);
		}
	}
	/*
	public static ISpecies[] copyParametersAndSpecies(ICRN crn, ICRN crnNew) {
		for (String param : crn.getParameters()) {
			int space = param.indexOf(' ');
			String paramName = param.substring(0, space);
			String paramExpr = param.substring(space,param.length());
			crnNew.addParameter(paramName, paramExpr);
		}
		ISpecies[] idToNewSpecies=copySpecies(crn, crnNew);
		return idToNewSpecies;
	}
	*/
	
	private static ISpecies getCorrespSpecies(ISpecies[] idToNewSpecies,ISpecies old) {
		if(idToNewSpecies==null) {
			return old;
		}
		else {
			return idToNewSpecies[old.getID()];
		}
		
	}
	public static void copyReactions(ICRN crn, ICRN crnNew, ISpecies[] idToNewSpecies) throws IOException {
		copyReactions(crn, crnNew, idToNewSpecies,null);
	}
	public static void copyReactions(ICRN crn, ICRN crnNew, ISpecies[] idToNewSpecies,
			HashMap<IComposite, BigDecimal> outGoingRates) throws IOException {
		for (ICRNReaction reaction : crn.getReactions()) {
			IComposite oldReagents =reaction.getReagents();
			HashMap<ISpecies, Integer> reagentsHM = new HashMap<>(oldReagents.getNumberOfDifferentSpecies());
			for(int i=0;i<oldReagents.getNumberOfDifferentSpecies();i++) {
				ISpecies newSpecies = getCorrespSpecies(idToNewSpecies,oldReagents.getAllSpecies(i));
				reagentsHM.put(newSpecies, oldReagents.getMultiplicities(i));
			}
			//IComposite newReagents = crnNew.addReagentsIfNew(new Composite(reagentsHM));
			IComposite newReagents;
			newReagents = compositeOrSpecies(reagentsHM);
			

			IComposite oldProducts =reaction.getProducts();
			HashMap<ISpecies, Integer> productsHM = new HashMap<>(oldProducts.getNumberOfDifferentSpecies());
			for(int i=0;i<oldProducts.getNumberOfDifferentSpecies();i++) {
				ISpecies newSpecies = getCorrespSpecies(idToNewSpecies,oldProducts.getAllSpecies(i));
				productsHM.put(newSpecies, oldProducts.getMultiplicities(i));
			}
			//IComposite newProducts = crnNew.addProductIfNew(new Composite(productsHM));
			IComposite newProducts = compositeOrSpecies(productsHM);
			
			ICRNReaction newReaction;
			if(reaction.hasArbitraryKinetics()) {
				//arbitrary
				newReaction = new CRNReactionArbitraryGUI(newReagents, newProducts, reaction.getRateExpression(),reaction.getID());
			}
			else {
				//mass action
//				if(reaction instanceof CRNMassActionReactionCompact) {
//					newReaction = new CRNMassActionReactionCompact(reaction.getRate(), newReagents, newProducts);
//				}
//				else {
//					newReaction = new CRNReaction(reaction.getRate(), newReagents, newProducts, reaction.getRateExpression(),reaction.getID());
//				}
				newReaction = new CRNReaction(reaction.getRate(), newReagents, newProducts, reaction.getRateExpression(),reaction.getID());
			}
			CRNImporter.addReaction(crnNew, newReagents, newProducts, newReaction);

			if(outGoingRates!=null) {
				BigDecimal bd = outGoingRates.get(newReagents);
				if(bd==null){
					outGoingRates.put(newReagents,reaction.getRate());
				}
				else{
					outGoingRates.put(newReagents,bd.add(reaction.getRate()));
				}
			}
		}
	}

	public static IComposite compositeOrSpecies(HashMap<ISpecies, Integer> multisetOfspecies) {
		IComposite comp;
		Entry<ISpecies, Integer> pair;
		if(multisetOfspecies.size()==1 && (pair = multisetOfspecies.entrySet().iterator().next()).getValue()==1) {
			comp=(IComposite)pair.getKey();
		}
		else {
			comp = new Composite(multisetOfspecies);
		}
		return comp;
	}

	@Override
	public String reactionKineticsReferToMissingSpeciesOrParameter() {
		MathEval mathToCheckMissingSpecies = new MathEval(getMath());
		for(ISpecies species : getSpecies()) {
			mathToCheckMissingSpecies.setVariable(species.getName(), species.getInitialConcentration().doubleValue()+1);
		}
		String rateExpr=null;
		boolean allZero=true;
		for(ICRNReaction reaction : getReactions()) {
			try{
				rateExpr = reaction.getRateExpression();
				if(rateExpr.contains("piecewise(") ||rateExpr.contains("delay(") || rateExpr.contains("tanh(") || rateExpr.contains("ceil(") || rateExpr.contains("sin(") || rateExpr.contains("cos(") || rateExpr.contains("exp(")|| rateExpr.contains("log(")) {
					return "UNDEFFUNCTION:"+rateExpr;
				}
				double val = mathToCheckMissingSpecies.evaluate(rateExpr);
				if(val!=0) {
					allZero=false;
				}
			}catch(NumberFormatException | ArithmeticException e) {
				return rateExpr;
			}
		}
		
		if(allZero) {
			return "JUST_ZERO_KINETICS";
		}

		return "NONE";
	}
	



}
