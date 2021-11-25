package it.imt.erode.crn.interfaces;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.crn.implementations.Command;
import it.imt.erode.crn.symbolic.constraints.IConstraint;
import it.imt.erode.expression.evaluator.MathEval;
import it.imt.erode.importing.ODEorNET;

public interface ICRN extends IModel{
	
	public MessageConsoleStream getOut();
	public BufferedWriter getBWOut();
	
	public int getSpeciesSize();
	
	public Set<IComposite> computeSetOfReagents();
	public Set<IComposite> computeSetOfProducts();
	
	public void addParameter(String parameterName, String parameterExpression);
	public void addParameter(String parameterName, String parameterExpression, boolean updateMathEval);

	public List<ICRNReaction> getReactions();
	
	public boolean isElementary();

	public void addSpecies(ISpecies species);

	public boolean addReaction(ICRNReaction reaction);
	
	public int getMaxArity();

	public void printCRN();

	//public void addProduct(IComposite product);

	//public TreeSet<IComposite> getProducts();

	
	
	public boolean isZeroSpecies(ISpecies species);

//	/**
//	 * If product is already in the products of the CRN, the already present composite is returned. Otherwise products is added to the products, and then returned.
//	 * @param product
//	 * @return
//	 */
	//public IComposite addProductIfNew(IComposite product);

	public String toStringShort();
	
	//public void addReagent(IComposite newComposite);
	//public TreeSet<IComposite> getReagents();
	
//	/**
//	 * If reagent is already in the reagents of the CRN, the already present composite is returned. Otherwise reagent is added to the reagents, and then returned.
//	 * @param reagent
//	 * @return
//	 */
//	public IComposite addReagentsIfNew(IComposite reagent);
	
	public String[] createArrayOfAllSpeciesNames();

	/**
	 * Set the views to be shown in output
	 * @param viewNames the names of the views
	 * @param viewExprs the arithmetic expressions of speciesNames specifying how to compute the value of each view
	 * @param viewExprsSupportedByMathEval as viewExprs, but slightly manipulated to allow MathEval to evaluate it 
	 */
	public void setViews(String[] viewNames, String[] viewExprs, String[] viewExprsSupportedByMathEval,boolean[] viewExpressionsUsesCovariances);
	
	/**
	 * 
	 * @return the names of the required views
	 */
	public String[] getViewNames();

	/**
	 * 
	 * @return the expressions of the required views
	 */
	public String[] getViewExpressions();

	/**
	 * 
	 * @return the expressions of the required views in a format supported by MathEval
	 */
	public String[] getViewExpressionsSupportedByMathEval();

	public MathEval getMath();

	public void setNewMath(MathEval math);

	public boolean setIC(String specieName, double ic, String icExpr);

	public double setParameter(String parameterName, String parameterExpression);

	public boolean containsTheZeroSpecies();

	public ISpecies getCreatingIfNecessaryTheZeroSpecies();
	
	public ISpecies getCreatingIfNecessaryTheZeroSpecies(String name);

	boolean[] getViewExpressionsUsesCovariances();

	public boolean isInfluenceNetwork();

	public void setInfluenceNetwork(boolean isInfluenceNetwork);


	/**
	 * This method sets information about the views/groups specified by the user. These information can then be used to prepartition the species according to the views/groups
	 */
	public void setViewsAsMultiset(List<HashMap<ISpecies,Integer>> setsOfSpeciesOfViews);

	/**
	 * This method gets information about the views/groups specified by the user. These information can then be used to prepartition the species according to the views/groups
	 */
	public List<HashMap<ISpecies,Integer>> getViewsAsMultiset();

	/**
	 * Initialize the collection that will store the reactions. Useful when the number of reactions to load is known in advance
	 * @param numberOfReactions
	 */
	public void setExpectedNumberOfReactions(int numberOfReactions);

	boolean isMassAction();

	/**
	 * Set the information of whether the CRN has been defined via an ODE system (\"ODE\"), or via a CRN (\"CRN\"). 
	 * @param modelDefKind
	 */
	public void setMdelDefKind(ODEorNET modelDefKind);
	/**
	 * Get the information of whether the CRN has been defined via an ODE system (\"ODE\"), or via a CRN (\"CRN\"). 
	 * @param modelDefKind
	 */
	public ODEorNET getMdelDefKind();

	public void removeUnusedZeroSpecies();

	/**
	 * Store the user defined initial partition. It can be used to prepartition species before reductions.
	 * @param userDefinedInitialPartition
	 */
	public void setUserDefinedPartition(ArrayList<HashSet<ISpecies>> userDefinedInitialPartition);

	/**
	 * Get the user defined initial partition. It can be used to prepartition species before reductions.
	 * @param userDefinedInitialPartition
	 */
	public ArrayList<HashSet<ISpecies>> getUserDefinedPartition();

	/**
	 * Add a parameter with no actual value assigned
	 * @param parameterName
	 */
	public void addSymbolicParameter(String parameterName);

	/**
	 * 
	 * @return the list of parameters with no assigned value
	 */
	List<String> getSymbolicParameters();

	public boolean isSymbolic();

	public boolean hasAtoMostBinaryProducts();

	List<ICommand> getCommands();

	void addCommand(Command command);

	public List<IConstraint> getConstraints();

	void addConstraint(IConstraint constraint);

	//public void setZeroSpecies(ISpecies reducedSpecies);
	
	//public void setDefaultIC(BigDecimal defIC);
	
	public int algebraicSpecies();
	void setExpectedNumberOfSpecies(int numberOfSpecies);
	void setSpecies(ISpecies species,int where);
	public String reactionKineticsReferToMissingSpeciesOrParameter();
	public void setName(String substring);
	
}
