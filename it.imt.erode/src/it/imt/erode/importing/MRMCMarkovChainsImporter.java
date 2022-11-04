package it.imt.erode.importing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.crn.implementations.CRNMassActionReactionCompact;
import it.imt.erode.crn.implementations.CRNReaction;
import it.imt.erode.crn.implementations.SpeciesCompact;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.importing.csv.CompactCSVMatrixImporter;
import it.imt.erode.partition.implementations.Block;
import it.imt.erode.partition.implementations.Partition;
import it.imt.erode.partition.interfaces.IBlock;


/**
 * 
 * @author Andrea Vandin
 * This class is used to import Markov chains given in the .tra format of MRMC. It is also possible to use its .lab format, defining the labelling function of the Markov chain, to prepartition the states
 */
public class MRMCMarkovChainsImporter extends AbstractImporter{

 	public static final String MRMCMarkovChainsFolder = "."+File.separator+"MRMCMarkovChains"+File.separator;
 	private String labellingFileName;
 	//private static final boolean addComposites=true;
	//public static final boolean IGNORELABELS = false;
	//private ISpecies[] speciesIdToSpecies;
	//boolean lowMemory=true;
	boolean lowMemory=true;
	boolean addSelfLoops=false;
	boolean asMatrix=false;

	public MRMCMarkovChainsImporter(String fileName, String[] labellingFileName,MessageConsoleStream out,BufferedWriter bwOut,IMessageDialogShower msgDialogShower) {
		super(fileName,out,bwOut,msgDialogShower);
		this.labellingFileName=labellingFileName[0];
		if(labellingFileName!=null&&labellingFileName.length>=0&&labellingFileName[0].endsWith("same")){
			this.labellingFileName=fileName.replace(".tra", ".lab");
		}
		if(labellingFileName.length>1) {
			if(labellingFileName[1].equalsIgnoreCase("true")) {
				asMatrix=true;
				if(addSelfLoops) {
					throw new UnsupportedOperationException("addSelfLoops and asMatrix cannot be used together");
				}
			}
		}
	}
	
	public InfoCRNImporting importMRMCMarkovChain(boolean printInfo, boolean printCRN,boolean print) throws FileNotFoundException, IOException{

		if(print){
			CRNReducerCommandLine.println(out,bwOut,"\nImporting: "+getFileName());
		}
		

		initInfoImporting();
		initCRNAndMath();
		ICRN crn = getCRN();
		getInfoImporting().setLoadedCRN(true);
				
		BufferedReader br = getBufferedReader();
		String line;
		//I assume that first the number of states is given. Then the number of transitions (which I ignore). Then the transitions. 
		/*
		 * STATES 276
		 * TRANSITIONS 1120
         * 2 1 0.00025
		 */
		boolean speciesGenerated = false;
		boolean reactionsLoaded=false;
		//HashMap<Integer, ISpecies> speciesIdToSpecies=new HashMap<Integer, ISpecies>();
		
		long beginImp = System.currentTimeMillis();
		
		//ISpecies[] speciesIdToSpecies=null;
		while ((line = br.readLine()) != null) {
			line=line.trim();
			
			//Any other line is ignored (including comments)
			if(line.equals("")||line.startsWith("#")){
				//if this is a comment line, skip to the next iteration of the loop (i.e. to the next line)
				continue;
			}
			else if( (!speciesGenerated) && line.startsWith("STATES")){
				long begin = System.currentTimeMillis();
				loadSpecies(line);
//				if(stop){
//					getInfoImporting().setLoadingCRNFailed();
//					return null;
//				}
				speciesGenerated = true;
				/*if(!IGNORELABELS){
					speciesIdToSpecies = new ISpecies[getCRN().getSpecies().size()];
					getCRN().getSpecies().toArray(speciesIdToSpecies);
				}*/
				long end = System.currentTimeMillis();
				CRNReducerCommandLine.println(out,bwOut,getCRN().getSpecies().size()+" species loaded. Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)");
				//CRNReducerCommandLine.println(out,"Species loaded");
			}
			else if(speciesGenerated && (!reactionsLoaded) && line.startsWith("TRANSITIONS")){
				String numberOfReactions = line.substring(line.indexOf(' ')+1, line.length());
				long begin = System.currentTimeMillis();
				loadReactions(br,/*speciesIdToSpecies,*/ Integer.valueOf(numberOfReactions));
				long end = System.currentTimeMillis();
				CRNReducerCommandLine.println(out,bwOut,getCRN().getReactions().size()+" reactions loaded. Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)");
				IBlock uniqueBlock = new Block();
				setInitialPartition(new Partition(uniqueBlock,getCRN().getSpecies().size()));
				for (ISpecies species : getCRN().getSpecies()) {
					uniqueBlock.addSpecies(species);
				}
				reactionsLoaded=true;
			}
		}
		br.close();
		
		//if(!IGNORELABELS){
			importLabellingFunction(print/*,speciesIdToSpecies*/);
		//}
		
		long endImp = System.currentTimeMillis();	
		getInfoImporting().setRequiredMS(endImp-beginImp);	
			
		//CRNReducerCommandLine.println(out,"BioNetGenImporter.importBioNetGenNetwork: completed importing of model "+fileName);
		if(print){
			if(printInfo){
				CRNReducerCommandLine.println(out,bwOut,getInfoImporting().toString());
			}
			if(printCRN){
				crn.printCRN();
			}
		}
		
		/*if(getCRN().getViewNames().length==0){
			Set<ISpecies> speciesOfDefaultView = new LinkedHashSet<ISpecies>(getCRN().getSpecies());
			ArrayList<Set<ISpecies>> defaultView = new ArrayList<Set<ISpecies>>(1);
			defaultView.add(speciesOfDefaultView);
			getCRN().setViewsForPrepartioning(defaultView);
		}*/
		
		/*if(crn!=null){
			checkACTMC();
		}*/
		
		
		/*
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		/////////////////////////////////////////////////////BEGIN ATOMIC CTMC////////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		
		boolean allAtomicCTMCsDerivable=true;
		//Build the set of monomers
		LinkedHashSet<String> monomers = new LinkedHashSet<String>();
		for (ISpecies species : crn.getSpecies()) {
			String[] monomersArray = species.getName().replace(" ","").split("\\.");
			for(int m=0;m<monomersArray.length;m++){
				if(monomersArray[m].isEmpty()){
					continue;
				}
				else{
					int posPar = monomersArray[m].indexOf('(');
					if(posPar==-1){
						if(monomersArray[m].equals(Species.ZEROSPECIESNAME)){
							monomers.add(Species.ZEROSPECIESNAME);
						}
						else{
							allAtomicCTMCsDerivable=false;
							CRNReducerCommandLine.println(out,crn.getName()+": It is not possible to derive the atomic CTMC because its species are not monomers-based");
							break;
						}
					}
					else{
						monomers.add(monomersArray[m].substring(0, posPar));
					}
				}
			}
			if(!allAtomicCTMCsDerivable){
				break;
			}
		}
	
		if(allAtomicCTMCsDerivable){
			CRNReducerCommandLine.println(out,"We have "+monomers.size()+" monomers. These are monomers for which it is not possible to generate the atomic CTMC.");
			LinkedHashSet<String> unsupportedMonomers = new LinkedHashSet<String>();
			MutableInteger counterUnsupportedMonomers=new MutableInteger(0);			
			for (String mon : monomers) {
				if(!crn.isAtomicCTMCDerivable(mon,counterUnsupportedMonomers)){
					allAtomicCTMCsDerivable=false;
					unsupportedMonomers.add(mon);
					//CRNReducerCommandLine.println(out,crn.getName()+": It is not possible to derive the atomic CTMC for the monomer "+mon);
					//break;
				}
			}
		}
		if(allAtomicCTMCsDerivable){
			CRNReducerCommandLine.println(out,"It is possible to derive the atomic CTMC for all monomers.");
		}
		//else {
		//	CRNReducerCommandLine.println(out,crn.getName()+": It not is possible to derive the atomic CTMC for some monomer.");
		//}
		
		CRNReducerCommandLine.println(out,"");
		CRNReducerCommandLine.println(out,"");
		CRNReducerCommandLine.println(out,"");
		
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		/////////////////////////////////////////////////////END ATOMIC CTMC////////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		*/
		return getInfoImporting();
	}
	
	public void importLabellingFunction(boolean print/*, ISpecies[] speciesIdToSpecies*/) throws IOException{

		if(print){
			CRNReducerCommandLine.println(out,bwOut,"\nImporting: "+labellingFileName);
		}

		List<String> viewNames = new ArrayList<String>();
		List<String> viewExpressions = new ArrayList<String>();
		List<String> viewExpressionsSupportedByMathEval = new ArrayList<String>();
		List<HashMap<ISpecies,Integer>> setsOfSpeciesOfViews;
		
		BufferedReader br=null;
		try{
			br = getBufferedReader(labellingFileName);
		}catch(FileNotFoundException e){
			CRNReducerCommandLine.printWarning(out,bwOut,"File with labelling not provided.");
		}

		String line;
		boolean skip=false;
		HashMap<String, Set<ISpecies>> labellingFunction = new HashMap<String, Set<ISpecies>>();
		while ((line = br.readLine()) != null) {
			line=line.trim();

			if(line.equalsIgnoreCase("#DECLARATION")) {
				skip=true;
			}
			if(skip&&line.equalsIgnoreCase("#end")){
				skip=false;
				continue;
			}

			if(!skip){
				StringTokenizer st = new StringTokenizer(line);
				String speciesIdString = st.nextToken();
				int id = Integer.valueOf(speciesIdString);
				id--;
				ISpecies species = getCRN().getSpecies().get(id);//speciesIdToSpecies[id];
				while(st.hasMoreTokens()){
					String label = st.nextToken();
					Set<ISpecies> speciesWithThisLabel = labellingFunction.get(label);
					if(speciesWithThisLabel==null){
						speciesWithThisLabel=new HashSet<ISpecies>();
						labellingFunction.put(label, speciesWithThisLabel);
					}
					speciesWithThisLabel.add(species);
				}
			}
		}
		
		setsOfSpeciesOfViews=new ArrayList<HashMap<ISpecies,Integer>>(labellingFunction.size());
		for (Entry<String, Set<ISpecies>> entry : labellingFunction.entrySet()) {
			StringBuffer viewExpr=new StringBuffer();
			StringBuffer viewExprSupportedByMathEval=new StringBuffer();
			
			Set<ISpecies> speciesSet = entry.getValue();
			HashMap<ISpecies, Integer> view = new HashMap<ISpecies, Integer>(speciesSet.size());
			for (ISpecies species : speciesSet) {
				view.put(species, 1);
				viewExpr.append(species.getName()+"+");
				viewExprSupportedByMathEval.append(species.getNameAlphanumeric()+"+");
			}
			setsOfSpeciesOfViews.add(view);
			
			//Views
			String label ="lab"+entry.getKey(); 
			viewNames.add(label);
			viewExpr.append("0");
			viewExprSupportedByMathEval.append("0");
			viewExpressions.add(viewExpr.toString());
			viewExpressionsSupportedByMathEval.add(viewExprSupportedByMathEval.toString());
		}
		
		String[] viewNamesArray = new String[viewNames.size()];
		String[] viewExpressionsArray = new String[viewNames.size()];
		String[] viewExpressionsSupportedByMathEvalArray = new String[viewNames.size()];
		boolean[] useCovariances = new boolean[viewNames.size()];//Array init to false
		getCRN().setViews(viewNames.toArray(viewNamesArray), viewExpressions.toArray(viewExpressionsArray),viewExpressionsSupportedByMathEval.toArray(viewExpressionsSupportedByMathEvalArray),useCovariances);
		
		getCRN().setViewsAsMultiset(setsOfSpeciesOfViews);
		getCRN().setUserDefinedPartition(BioNetGenImporter.viewsAsMultisetsToUserPartition(setsOfSpeciesOfViews,getCRN()));

		br.close();
	} 

	/**
	 * 
	 * @param line \"STATES numberOfStates\"
	 * @param speciesNameToSpecies 
	 * @throws IOException
	 * We generate numberOfStates species, with name s0,s1,...snumberOfSpeciesMinus1. It is assumed that the id of a species corresponds to the order with which it has been created: the i_th created species has id "i". 
	 * Note that we start counting from 0 rather than from 1.
	 */
	private void loadSpecies(String line) throws IOException {
		line = removeCommentAtTheEnd(line, '#');
		StringTokenizer st = new StringTokenizer(line);
		st.nextToken();
		String n =st.nextToken();
		int numberOfSpecies = Integer.valueOf(n);
		
		loadSpecies(numberOfSpecies);
	}
	
	private void loadSpecies(int numberOfSpecies){

		/*if(numberOfSpecies>50000){
			return true;
		}*/
		
		getCRN().setExpectedNumberOfSpecies(numberOfSpecies);
		
		//I don't need this array. ArrayList.get takes constant time
//		ISpecies[] speciesIdToSpecies;
//		if(!IGNORELABELS){
//			speciesIdToSpecies = new ISpecies[numberOfSpecies];
//		}
		
		for(int i=0;i<numberOfSpecies;i++){
			int id = i;
			ISpecies species;
			species = new SpeciesCompact(id, BigDecimal.ZERO);
//			if(lowMemory) {
//				species = new SpeciesCompact(id, BigDecimal.ZERO);
//			}
//			else {
//				String speciesName = "s"+id;
//				species = new Species(speciesName, id, BigDecimal.ZERO,"0.0",false);
//			}
			
			getCRN().addSpecies(species);
			/*if(!IGNORELABELS){
				speciesIdToSpecies.put(id, species);
			}*/
//			if(!IGNORELABELS){
//				speciesIdToSpecies[i]=species;
//			}
		}
		getInfoImporting().setReadSpecies(getCRN().getSpecies().size());
		
		//return speciesIdToSpecies;
	}
	
	/**
	 * It is assumed that the id of a species corresponds to the order with which it has been inserted: the i_th inserted species has id "i". We have to decrease the id by one because we start to count from 0 
	 * @param br
	 * @param speciesIdToSpecies 
	 * @param integer 
	 * @throws IOException
	 */
	private void loadReactions(BufferedReader br, /*ISpecies[] speciesIdToSpecies,*/ int numberOfReactions) throws IOException {
		
		if(addSelfLoops) {
			getCRN().setExpectedNumberOfReactions(numberOfReactions+getCRN().getSpecies().size());
		}
		else if(asMatrix) {
			getCRN().setExpectedNumberOfReactions(numberOfReactions);
			//getCRN().setExpectedNumberOfReactions(numberOfReactions*2);
		}
		else {
			getCRN().setExpectedNumberOfReactions(numberOfReactions);
		}
		
		//double minRate=Double.MIN_VALUE;
		
		BigDecimal[] outgoingRates=null;
		if(addSelfLoops) {
			outgoingRates= new BigDecimal[getCRN().getSpecies().size()];
		}
		
		long begin=System.currentTimeMillis();
		int r=0;
		String line = br.readLine();//2 1 0.00025
//		boolean[] compositeIsReagent=null;
//		boolean[] compositeIsProduct=null;
//		
//		if(CRNReducerCommandLine.univoqueReagents){
//			compositeIsReagent=new boolean[speciesIdToSpecies.length];
//		}
//		if(CRNReducerCommandLine.univoqueProducts){
//			compositeIsProduct=new boolean[speciesIdToSpecies.length];
//		}
		
		double min = Double.MAX_VALUE;
		
		while (line != null) {
			//Skip comments or empty lines
			if(!(line.equals("") || line.startsWith("#"))){
				line=removeCommentAtTheEnd(line,'#');
				//Replace all constant function invocations in parameters 
				//line=line.replace("()", "");

				StringTokenizer st = new StringTokenizer(line);
				
				
				//Reagent
				String reagentIDString = st.nextToken();
				int reagentIDInt = Integer.valueOf(reagentIDString) -1;
				//IComposite compositeReagents = getCRN().addReagentsIfNew(new Composite(getCRN().getSpecies(),new int[]{reagentIDInt}));
				//IComposite compositeReagents = getCRN().addReagentsIfNew((IComposite)speciesIdToSpecies.get(reagentIDInt));
				IComposite compositeReagents = (IComposite)getCRN().getSpecies().get(reagentIDInt);
				//Product
				String productIDString = st.nextToken();
				int productIDInt = Integer.valueOf(productIDString) -1;
				//IComposite compositeProducts = getCRN().addProductIfNew(new Composite(getCRN().getSpecies(),new int[]{productIDInt}));
				//IComposite compositeProducts = getCRN().addProductIfNew((IComposite)speciesIdToSpecies.get(productIDInt));
				IComposite compositeProducts = (IComposite)getCRN().getSpecies().get(productIDInt);
				
				/*if(addComposites){
					if(!compositeIsReagent[reagentIDInt]){
						compositeIsReagent[reagentIDInt]=true;
						getCRN().addReagent(compositeReagents);
					}
					if(!compositeIsProduct[productIDInt]){
						compositeIsProduct[productIDInt]=true;
						getCRN().addProduct(compositeProducts);
					}
				}*/
				
				//compute rate of the reaction 
				String rateExpression = st.nextToken();
				double d =evaluate(rateExpression);
				if(d<min) {
					min=d;
				}
				BigDecimal reactionRate = BigDecimal.valueOf(d);
				
				/*
				ICRNReaction reaction = new CRNReaction(reactionRate, compositeReagents, compositeProducts, rateExpression,false,false);
				getCRN().addReaction(reaction);
				addToIncomingReactionsOfProducts(1,compositeProducts, reaction);
				addToOutgoingReactionsOfReagents(1, compositeReagents, reaction);*/
				//addToReactionsWithNonZeroStoichiometry(1, reaction.computeProductsMinusReagents(),reaction);
				/*if(r>0&&r%1000000==0){
					CRNReducerCommandLine.println(out,"reaction "+r+" loaded");
				}
				r++;*/
				
				if(asMatrix) {
					//To obtain a matrix Ax, an edge Si -> Sj , k of a DTMC corresponds to:
					//	d(Sj) = k*Si	A_{j,i}= k	row=j, col=i
					ISpecies Si = compositeReagents.getFirstReagent();
					ISpecies Sj = compositeProducts.getFirstReagent();
					int row =Sj.getID();
					int col= Si.getID();
					CompactCSVMatrixImporter.loadReactionLinearSystemAX(row, col,
							reactionRate,rateExpression,null,getCRN(),false);
					
					/*
					//FIST BUGGY VERSION
					//To obtain a matrix Ax, an edge Si -> Sj , k of a DTMC corresponds to:
					//1)	d(Si) = -0.125*Si	A_{i,i}=-0.125	A_{row,col}
					//2)  d(Sj) = 0.125*Si	A_{j,i}= 0.125	A_{row,col}
					
					//1)	A_{row,col}=A_{i,i}=-k
					int row =compositeReagents.getFirstReagent().getID();
					int col= compositeReagents.getFirstReagent().getID();
					CompactCSVMatrixImporter.loadReactionLinearSystemAX(row, col,
							BigDecimal.ZERO.subtract(reactionRate),"-("+rateExpression+")",null,getCRN(),false);
					//2)	A_{row,col}=A_{j,i}= k
					row =compositeProducts.getFirstReagent().getID();
					col= compositeReagents.getFirstReagent().getID();
					CompactCSVMatrixImporter.loadReactionLinearSystemAX(row, col,
							reactionRate,rateExpression,null,getCRN(),false);
					*/
					
					//addReaction(outgoingRates, i, row, col, val, valExpr,"");
//					if(form.equals(MatrixForm.PQ)){
//						loadReactionMarkovChainPQ(row,col,val,valExpr/*,speciesIdToSpecies*/,outgoingRates);
//					}
//					else{
//						loadReactionLinearSystemAX(row, col, val,valExpr/*, speciesIdToSpecies*/,outgoingRates);
//					}
				}
				else {
					addReaction(compositeReagents, compositeProducts, reactionRate, rateExpression/*,compositeIsReagent,compositeIsProduct*/);
					if(addSelfLoops) {
						if(outgoingRates[reagentIDInt]==null){
							outgoingRates[reagentIDInt]=reactionRate;
						}
						else{
							outgoingRates[reagentIDInt]=outgoingRates[reagentIDInt].add(reactionRate);
						}
					}
				}
				
				r++;
				if(r%500000==0){
					CRNReducerCommandLine.println(out,bwOut,r + " reactions loaded after "+(System.currentTimeMillis()-begin)/1000.0+ " seconds");
				}
				
			}
			
			line = br.readLine();
		}
		
		CRNReducerCommandLine.println(out,bwOut," (min rate ="+min+")");
		
		if(addSelfLoops) {
			int i=0;
			for(ISpecies species : getCRN().getSpecies()) {
			//for(int i=0;i<speciesIdToSpecies.length;i++){
				if(outgoingRates[i]!=null){
					BigDecimal rate = BigDecimal.ZERO.subtract(outgoingRates[i]);
					addReaction((IComposite)species, (IComposite)species, rate, rate.toPlainString()/*,compositeIsReagent,compositeIsProduct*/);

					r++;
					if(r%500000==0){
						CRNReducerCommandLine.println(out,bwOut,r + " reactions loaded after "+(System.currentTimeMillis()-begin)/1000.0+ " seconds");
					}
				}
				i++;
			}
		}
		
		getInfoImporting().setReadCRNReactions(getCRN().getReactions().size());
		//getInfoImporting().setReadReagents(getCRN().getReagents().size());
		//getInfoImporting().setReadProducts(getCRN().getProducts().size());
		//CRNReducerCommandLine.println(out,getCRN());
	}
	
	private void addReaction(IComposite compositeReagents, IComposite compositeProducts, BigDecimal reactionRate, String rateExpression/*, boolean[] compositeIsReagent, boolean[] compositeIsProduct*/){
		/*
		int reagentId = ((ISpecies)compositeReagents).getID();
		int productId = ((ISpecies)compositeProducts).getID();
		if(CRNReducerCommandLine.univoqueReagents && !compositeIsReagent[reagentId]){
			compositeIsReagent[reagentId]=true;
			compositeReagents = getCRN().addReagentsIfNew(compositeReagents);
		}
		
		if(CRNReducerCommandLine.univoqueProducts && !compositeIsProduct[productId]){
			compositeIsProduct[productId]=true;
			compositeProducts = getCRN().addProductIfNew(compositeProducts);
		}
		*/
		
		ICRNReaction reaction;
		if(lowMemory) {
			reaction = new CRNMassActionReactionCompact(reactionRate, compositeReagents, compositeProducts);
		}
		else {
			reaction = new CRNReaction(reactionRate, compositeReagents, compositeProducts, rateExpression,null);
		}
		
		getCRN().addReaction(reaction);
		
	
		/*
		addToIncomingReactionsOfProducts(1,reaction.getProducts(), reaction,CRNReducerCommandLine.addReactionToComposites);
		addToOutgoingReactionsOfReagents(1, reaction.getReagents(), reaction,CRNReducerCommandLine.addReactionToComposites);
		if(CRNReducerCommandLine.hasToAddToReactionsWithNonZeroStoichiometry){
			addToReactionsWithNonZeroStoichiometry(1, reaction.computeProductsMinusReagentsHashMap(),reaction);
		}
		*/
		
	}

}
