package it.imt.erode.simulation.stochastic.fern;

import java.util.Collection;
import java.util.HashMap;

import fern.network.AbstractNetworkImpl;
import fern.network.AnnotationManager;
import fern.network.ArrayKineticConstantPropensityCalculator;
import fern.network.DefaultAmountManager;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;

public class FernNetworkFromLoadedCRN extends AbstractNetworkImpl {

	private int numSpecies;
	private int numReactions;
	private long[] initialAmounts;
	private double[] rateOfReactions;
	
	private ICRN loadedCRN;
	private double defaultIC;
	
	public FernNetworkFromLoadedCRN(ICRN loadedCRN,double defaultIC) {
		super(loadedCRN.getName());
		
		this.loadedCRN=loadedCRN;
		this.defaultIC=defaultIC;
		init();
		
	}
	
	private void init() {
		numSpecies = loadedCRN.getSpecies().size();
		
		//I can't use this. Because I have to delete all reactions that have 0 rate. Otherwise I get the exception: "if (constants[i]<=0) throw new IllegalArgumentException("There is a non positive constant!");" 
		//numReactions=loadedCRN.getReactions().size();
		
		//I discard reactions with 0 or negative rate. 
		//However, given that I don't know how to do this check with arbitrary reactions, I keep all arbitrary reactions.
		numReactions=0;
		for(ICRNReaction reaction : loadedCRN.getReactions()){
			if(reaction.hasArbitraryKinetics() || reaction.getRate().doubleValue()>0){
				numReactions++;
			}
		}
		
		createAnnotationManager();
		//species
		createSpeciesMapping();
		createAmountManager();
		//reactions
		createAdjacencyLists();
		createPropensityCalulator();
	}

	@Override
	public int getNumSpecies() {
		return numSpecies;
	}

	@Override
	public int getNumReactions() {
		return numReactions;
	}
	
	@Override
	public long getInitialAmount(int species) {
		/*if(species<0 || species>= numSpecies){
			return -1;
		}*/
		return initialAmounts[species];
	}

	@Override
	public void setInitialAmount(int species, long value) {
		/*if(species>=0 && species< numSpecies){
			initialAmounts[species]=value;
		}*/
		initialAmounts[species]=value;
	}
	
	public String[] getIndexToSpeciesId(){
		return indexToSpeciesId;
	}
	
	@Override
	protected void createAmountManager() {
		amountManager = new DefaultAmountManager(this);
	}

	@Override
	protected void createAnnotationManager() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected void createSpeciesMapping() {
	
		initialAmounts = new long[numSpecies];
		indexToSpeciesId = new String[numSpecies];
		speciesIdToIndex = new HashMap<String, Integer>(numSpecies);
		int s=0;
		for(ISpecies species : loadedCRN.getSpecies()){
			double ic = species.getInitialConcentration().doubleValue();
			if(species.getInitialConcentrationExpr().equals("0")||species.getInitialConcentrationExpr().equals("0.0")){
				ic=defaultIC;
			}
			indexToSpeciesId[s]=species.getNameAlphanumeric();
			initialAmounts[s]=(long)(ic+0.5);
			speciesIdToIndex.put(species.getNameAlphanumeric(), s);
			s++;
		}
		
	}
	
	/**
	 * Does nothing, the {@link PropensityCalculator} is created in <code>createAdjacencyLists</code>
	 * because the reactions constants are already parsed there.
	 */
	@Override
	protected void createPropensityCalulator() {
		// done in createAdjacencyLists
	}

	@Override
	protected void createAdjacencyLists() {
		adjListRea = new int[numReactions][];
		adjListPro = new int[numReactions][];
		rateOfReactions = new double[numReactions];
		int r=0;
		for(ICRNReaction reaction : loadedCRN.getReactions()){
			if(reaction.hasArbitraryKinetics()){
				//CRNReducerCommandLine.printWarning("UNSUPPORTED: CTMC simulations of arbitrary kinetics");
				//throw new UnsupportedOperationException("UNSUPPORTED: CTMC simulations of arbitrary kinetics");
				//System.exit(-1);
				adjListRea[r] = createSpeciesReferences(reaction.getReagents());
				adjListPro[r] = createSpeciesReferences(reaction.getProducts());
				rateOfReactions[r]=-1;
				r++;
			}
			else {
				//I skip all mass-actin reactions with rate 0 to avoid the exception: "if (constants[i]<=0) throw new IllegalArgumentException("There is a non positive constant!");"
				if(reaction.getRate().doubleValue()>0){
					adjListRea[r] = createSpeciesReferences(reaction.getReagents());
					adjListPro[r] = createSpeciesReferences(reaction.getProducts());
					/*//I halve the rate of binary homeo-reactions
					if(reaction.getReagents().isBinary() && reaction.getReagents().getFirstReagent().equals(reaction.getReagents().getFirstReagent())){
						rateOfReactions[r]=reaction.getRate().doubleValue()/2.0;
					}
					else{
						rateOfReactions[r]=reaction.getRate().doubleValue();
					}*/
					rateOfReactions[r]=reaction.getRate().doubleValue();
					r++;
				}
			}
		}

		if(loadedCRN.isMassAction()){
			propensitiyCalculator = new ArrayKineticConstantPropensityCalculator(adjListRea,rateOfReactions);
		}
		else{
			//propensitiyCalculator = new SBMLPropensityCalculator(new SBMLNetwork(file))
			propensitiyCalculator = new ArbitraryKineticsCRNPropensityCalculator(this,loadedCRN,adjListRea);
			//propensitiyCalculator = new ArbitraryKineticsCRNPropensityCalculatorLessEfficientWithArbitraryRates(this,loadedCRN,adjListRea);
			//throw new UnsupportedOperationException("Non-mass-action stochastic simulation has yet to be implemented");
			/*propensitiyCalculator = new PropensityCalculator() {
						
						@Override
						public double calculatePropensity(int reaction, AmountManager amount,
								Simulator sim) {
							
							// TODO Auto-generated method stub
							return 0;
						}
					};*/
		}
	}
	
	private int[] createSpeciesReferences(IComposite composite) {
		int[] re = new int[composite.getTotalMultiplicity()];
		int index = 0;
		for(int s=0;s<composite.getNumberOfDifferentSpecies();s++){
			ISpecies species = composite.getAllSpecies(s);
			int multiplicity = composite.getMultiplicities(s); 
			for(int m=0;m<multiplicity;m++){
				re[index]= getSpeciesByName(species.getNameAlphanumeric());
				index++;
			}
		}
		return re;
	}
	
	@Override
	public AnnotationManager getAnnotationManager() {
		//final AnnotationManager ori = getParentNetwork().getAnnotationManager();
		return new AnnotationManager() {

			public boolean containsNetworkAnnotation(String typ) {
				// TODO
				return false;
			}

			public boolean containsReactionAnnotation(int reaction, String typ) {
				// TODO
				return false;
			}

			public boolean containsSpeciesAnnotation(int species, String typ) {
				// TODO
				return false;
			}

			public String getNetworkAnnotation(String typ) {
				// TODO
				return null;
			}

			public Collection<String> getNetworkAnnotationTypes() {
				// TODO
				return null;
			}

			public String getReactionAnnotation(int reaction, String typ) {
				// TODO
				return null;
			}

			public Collection<String> getReactionAnnotationTypes(int reaction) {
				// TODO
				return null;
			}

			public String getSpeciesAnnotation(int species, String typ) {
				// TODO
				return null;
			}

			public Collection<String> getSpeciesAnnotationTypes(int species) {
				// TODO
				return null;
			}

			public void setNetworkAnnotation(String typ, String annotation) {
				// TODO				
			}

			public void setReactionAnnotation(int reaction, String typ,String annotation) {
				// TODO
			}

			public void setSpeciesAnnotation(int species, String typ,String annotation) {
				// TODO
			}

			
		};
	}

}
