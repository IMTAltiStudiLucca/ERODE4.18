package it.imt.erode.importing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.DialogType;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.crn.implementations.CRN;
import it.imt.erode.crn.implementations.CRNReaction;
import it.imt.erode.crn.implementations.Command;
import it.imt.erode.crn.implementations.CommandParameter;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.partition.implementations.Block;
import it.imt.erode.partition.implementations.Partition;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;


/**
 * 
 * @author Andrea Vandin
 * This class is used to import reaction networks wrote as a list of reactions, possibly preceded by a list of parameters
 */
public class LBSImporter extends AbstractImporter {

	public static final String LBSNetworksFolder = "."+File.separator+"LBSNetworks"+File.separator;
	public LBSImporter(String fileName,MessageConsoleStream out,BufferedWriter bwOut,IMessageDialogShower msgDialogShower) {
		super(fileName,out,bwOut,msgDialogShower);
	}

	public InfoCRNImporting importLBSNetwork(boolean printInfo, boolean printCRN,boolean print) throws FileNotFoundException, IOException{

		if(print){
			CRNReducerCommandLine.println(out,bwOut,"Importing: "+getFileName());
		}

		initInfoImporting();
		initMath();
		initCRN();


		/*//dummy species used to let the id of a species coincide with its position in the list
		crn.addSpecies(Species.ZEROSPECIES);
		//the initial partition: one block for 0 and one block for all the other species
		IBlock zeroBlock = new Block();
		zeroBlock.addSpecies(Species.ZEROSPECIES);
		IBlock uniqueBlock = new Block();
		setInitialPartition(new Partition(zeroBlock, uniqueBlock));//initialPartition = new Partition(uniqueBlock); 
		 */
		BufferedReader br = getBufferedReader();
		String line;
		//I assume that first the parameters are generated, then the species, and finally the reactions. In this way I reduce the string comparisons
		//boolean parametersLoaded = false;
		//boolean reactionsLoaded = false;
		//boolean initialConcentrationsLoaded = false;
		//boolean viewsLoaded = false;
		//boolean crnOpen=false;

		//I use this data structure to efficiently check if a species has been already generated.
		HashMap<String, ISpecies> speciesStoredInHashMap = new HashMap<String, ISpecies>();
		//I use this data structure to efficiently check if a product has been already generated.
		//HashMap<IComposite,IComposite> productsStoredInHashMap = new HashMap<IComposite, IComposite>();
		//I use this data structure to efficiently check if a reagent has been already generated.
		//HashMap<IComposite,IComposite> reagentsStoredInHashMap = new HashMap<IComposite, IComposite>();

		List<String> icsToApply = new ArrayList<String>();
		String sampleTEnd = "1";
		String sampleSteps = "1";
		boolean stochastic = false;
		boolean deterministic = false;
		boolean lna = false;
		List<String> viewExpressions = new ArrayList<>();

		while ((line = br.readLine()) != null) {
			line=line.trim();
			line=removeCommentAtTheEnd(line,"//");

			//Skip comments and empty lines
			if(line.equals("")||line.startsWith("//")/*||line.startsWith("directive ")*/){
				continue;
			}
			
			if(line.startsWith("directive sample")){
				//directive sample tEnd steps
				//directive sample 5.0 100
				sampleTEnd = line.substring(line.indexOf("sample")+7);
				sampleSteps = sampleTEnd.substring(sampleTEnd.indexOf(' ')+1);
				sampleTEnd = sampleTEnd.substring(0,sampleTEnd.indexOf(' '));
			}
			else if(line.startsWith("directive simulation deterministic")){
				//directive simulation deterministic|stochastic|cme|lna
				deterministic=true;
				stochastic=false;
				lna=false;
			} 
			else if(line.startsWith("directive simulation stochastic")){
				deterministic=false;
				stochastic=true;
				lna=false;
			} 
			else if(line.startsWith("directive simulation cme")){
				CRNReducerCommandLine.printWarning(out, bwOut,true, msgDialogShower, "directive simulation cme is not currently supported",DialogType.Warning);
			} 
			else if(line.startsWith("directive simulation lna")){
				deterministic=false;
				stochastic=false;
				lna=true;
			} 
			else if(line.startsWith("directive plot")){
				//directive plot OO; PP; OP+PO //THESE ARE THE VIEWS!
				String plots = line.substring(line.indexOf("plot")+5);
				StringTokenizer viewsST = new StringTokenizer(plots);
				while(viewsST.hasMoreTokens()){
					String token = viewsST.nextToken(";").trim();
					viewExpressions.add(token);
				}
			} 
			
			else if(line.startsWith("rate ")){
				//I load one or more rates
				//In case more rates have been defined in the same line
				String[] rates = line.split(";");
				for (String rate : rates) {
					//remove "rate prefix"
					rate=rate.trim();
					rate=rate.substring(5).replace(" ", "");
					int eqPos=rate.indexOf('=');
					String rateName=rate.substring(0,eqPos);
					String rateExpr = rate.substring(eqPos+1);

					addParameter(rateName, rateExpr);
//					getCRN().addParameter(rateName,rateExpr);
//
//					double rateValue;
//					try  
//					{  
//						rateValue = Double.valueOf(rateExpr);  
//					}  
//					catch(NumberFormatException nfe)  
//					{  
//						rateValue = getMath().evaluate(rateExpr);  
//					}  			
//					getMath().setVariable(rateName, rateValue);
				}
			}
			else{
				//I load one or more initial conditions or reactions
				//init gA 1 | gA ->{transcribe} gA + mA |
				//In case more things have been defined in the same line
				String[] defs = line.split("\\|");
				for (String def : defs) {
					def=def.trim();
					if(def.startsWith("init ")){
						//ic //init gA 1
						String ic=def.substring(5);
						icsToApply.add(ic);
					}
					else{
						//reaction // gA ->{transcribe} gA + mA
						int arrowPos = def.indexOf("->");
						int openCurlyBracketPos = def.indexOf("{", arrowPos);
						int closedCurlyBracketPos = def.indexOf("}", arrowPos);
						String reagentsString = def.substring(0, arrowPos).trim();
						String rateString = def.substring(openCurlyBracketPos+1, closedCurlyBracketPos);
						String productsString = def.substring(closedCurlyBracketPos+1, def.length()).trim();

						String[] reagentsArray = reagentsString.split("\\+");
						String[] productsArray = productsString.split("\\+");

						//build the reagents and products
						HashMap<ISpecies, Integer> reagentsHM = generateNewSpeciesAndBuildArrayOfNames(speciesStoredInHashMap, reagentsArray,getCRN());
						//IComposite compositeReagents = getCRN().addReagentsIfNew(new Composite(reagentsHM));
						IComposite compositeReagents = CRN.compositeOrSpecies(reagentsHM);
						HashMap<ISpecies, Integer> productsHM = generateNewSpeciesAndBuildArrayOfNames(speciesStoredInHashMap, productsArray,getCRN());
						//IComposite compositeProducts = getCRN().addProductIfNew(new Composite(productsHM));
						IComposite compositeProducts = CRN.compositeOrSpecies(productsHM);
						//int arity = compositeReagents.getTotalMultiplicity();
						
						//compute rate of the reaction 
						BigDecimal reactionRate = BigDecimal.valueOf(evaluate(rateString));

						//build the reacion, and add it to the CRN
						ICRNReaction reaction = new CRNReaction(reactionRate, compositeReagents, compositeProducts, rateString,null);
						getCRN().addReaction(reaction);
						//addToIncomingReactionsOfProducts(arity,compositeProducts, reaction,CRNReducerCommandLine.addReactionToComposites);
						//addToReactionsWithNonZeroStoichiometry(arity, reaction.computeProductsMinusReagentsHashMap(),reaction);
					}
				}
			}

		}
		br.close();

		//I now apply all loaded ICs
		for (String ic : icsToApply) {
			int spacePos=ic.indexOf(' ');
			String speciesName=ic.substring(0,spacePos);
			String icExpr = ic.substring(spacePos+1);
			BigDecimal initialConcentration = BigDecimal.valueOf(evaluate(icExpr));
			speciesStoredInHashMap.get(speciesName).setInitialConcentration(initialConcentration,icExpr);
		}
	
		loadViews(viewExpressions, speciesStoredInHashMap, getCRN());
		
		if(stochastic){
			//simulateCTMC(tEnd=1.0,steps=1,csvFile="kaic.csv",method=nextReaction)
			List<CommandParameter> parameters = new ArrayList<CommandParameter>(3);
			parameters.add(new CommandParameter("tEnd", sampleTEnd));
			parameters.add(new CommandParameter("steps", sampleSteps));
			//parameters.add(new CommandParameter("csvFile", getCRN().getName()+".csv"));
			parameters.add(new CommandParameter("method", "nextReaction"));			
			Command simulateCTMC = new Command("simulateCTMC", parameters);
			getCRN().addCommand(simulateCTMC);
			addCommand(simulateCTMC.toCRNFormat());
		}
		else{
			if(deterministic){
				List<CommandParameter> parameters = new ArrayList<CommandParameter>(2);
				parameters.add(new CommandParameter("tEnd", sampleTEnd));
				parameters.add(new CommandParameter("steps", sampleSteps));
				//parameters.add(new CommandParameter("csvFile", "\""+getCRN().getName()+".csv\""));			
				Command simulateODE = new Command("simulateODE", parameters);
				getCRN().addCommand(simulateODE);
				addCommand(simulateODE.toCRNFormat());
			}
			else if(lna){
				
			}
		}

		//I now create the trivial partition
		IBlock uniqueBlock = new Block();
		setInitialPartition(new Partition(uniqueBlock,getCRN().getSpecies().size()));
		for (ISpecies species : getCRN().getSpecies()) {
			uniqueBlock.addSpecies(species);
		}

		getInfoImporting().setReadParameters(getCRN().getParameters().size());
		getInfoImporting().setReadCRNReactions(getCRN().getReactions().size());
		//getInfoImporting().setReadProducts(getCRN().getProducts().size());
		getInfoImporting().setLoadedCRN(true);
		getInfoImporting().setReadSpecies(getCRN().getSpecies().size());

		if(print){
			if(printInfo){
				CRNReducerCommandLine.println(out,bwOut,getInfoImporting().toString());
			}
			if(printCRN){
				getCRN().printCRN();
			}
		}
		return getInfoImporting();
	}
	
	/**
	 * It is assumed that the reactions have been already read (and thus the species have been created)
	 * @param viewExpressions the read views expressions
	 * @param speciesNameToSpecies 
	 * @param speciesNameToSpecies an hash map from species name to species
	 * @throws IOException 
	 */
	protected static void loadViews(List<String> readViewExpressions, HashMap<String, ISpecies> speciesNameToSpecies, ICRN crn) throws IOException {

		List<String> viewNames = new ArrayList<String>(readViewExpressions.size());
		List<String> viewExpressionsSupportedByMathEval = new ArrayList<String>(readViewExpressions.size());
		List<String> viewExpressions = new ArrayList<String>(readViewExpressions.size());
		List<Boolean> viewExpressionUsesCovariance = new ArrayList<Boolean>(readViewExpressions.size());
		boolean viewsSupportedForPrepartitioning=true;
		List<HashMap<ISpecies,Integer>> setsOfSpeciesOfViews = new ArrayList<HashMap<ISpecies,Integer>>();
		int v=0;
		for (String viewExpr : readViewExpressions) {
			v++;
			String viewName = "v"+v;
			viewsSupportedForPrepartitioning=CRNImporter.loadAView(viewNames,viewExpressions,viewExpressionsSupportedByMathEval,viewExpressionUsesCovariance,viewName,viewExpr,viewsSupportedForPrepartitioning,speciesNameToSpecies,setsOfSpeciesOfViews);
		}
		
		CRNImporter.finalizeToLoadViews(viewNames, viewExpressions,
				viewExpressionsSupportedByMathEval,
				viewExpressionUsesCovariance, viewsSupportedForPrepartitioning,
				setsOfSpeciesOfViews,crn);
	}
	
	private static String toStringLBSFormat(ICRNReaction reaction) {
		String reagentsMultiSet = reaction.getReagents().toMultiSet();
		String productsMultiSet = reaction.getProducts().toMultiSet();
		if(AbstractImporter.isSpecialNullSpecies(productsMultiSet)){
		//if(productsMultiSet.equals(Species.ZEROSPECIESNAME)){
			productsMultiSet="";
		}
		
		return reagentsMultiSet + " ->{" + reaction.getRateExpression() +"} " + productsMultiSet;
	}



	public static void printCRNToLBSFile(ICRN crn,IPartition partition, String name, boolean assignPopulationOfRepresentative, boolean groupAccordingToCurrentPartition, Collection<String> preambleCommentLines, boolean verbose, String icComment,MessageConsoleStream out,BufferedWriter bwOut,IMessageDialogShower msgDialogShower){
		String fileName = name;
		fileName=overwriteExtensionIfEnabled(fileName,".lbs");

		if(verbose){
			CRNReducerCommandLine.print(out,bwOut,"Writing model in file "+fileName);
		}
		
		if(!crn.isMassAction()){
			CRNReducerCommandLine.printWarning(out,bwOut,true,msgDialogShower,"The model is not supported because it is not a mass action CRN (i.e., it has reactions with arbitrary rates). I terminate.",DialogType.Error);
			return;
		}

		createParentDirectories(fileName);
		BufferedWriter br;
		try {
			br = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printCRNToLBSFile, exception raised while creating the filewriter for file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}

		try {

			if(preambleCommentLines!=null && preambleCommentLines.size()>0){
				for (String comment : preambleCommentLines) {
					br.write("//"+comment+"\n");
				}
			}
			br.write("\n\n");

			if(crn.getParameters()!=null && crn.getParameters().size()>0){
				for(String parameterDefinition : crn.getParameters()){
					int posOfSpace=parameterDefinition.indexOf(' ');
					String paramName = parameterDefinition.substring(0, posOfSpace);
					String paramExpr=parameterDefinition.substring(posOfSpace+1,parameterDefinition.length());
					br.write("rate "+paramName+" = " +paramExpr+";\n");
				}
			}

			br.write("\n");

			for (ISpecies species : crn.getSpecies()) {
				//double ic = species.getInitialConcentration().doubleValue();
				double initialConcentration;
				if(assignPopulationOfRepresentative){
					initialConcentration = partition.getBlockOf(species).getRepresentative(CRNReducerCommandLine.COMPUTEREPRESENTATIVEBYMINOUTSIDEPARTITIONREFINEMENT).getInitialConcentration().doubleValue();
					//initialConcentration=BigDecimal.valueOf(10000);
				}
				else{
					initialConcentration = species.getInitialConcentration().doubleValue();
					//initialConcentration=BigDecimal.valueOf(10000);
				}
				if(initialConcentration!=0){
					br.write("init "+species.getName() + " "+ initialConcentration+" |\n");
				}
			}

			//br.write("\n");

			Iterator<ICRNReaction> iter = crn.getReactions().iterator();
			while(iter.hasNext()){
				ICRNReaction crnReaction = iter.next();
				if(iter.hasNext()){
					br.write(toStringLBSFormat(crnReaction)+" |\n");
				}
				else{
					br.write(toStringLBSFormat(crnReaction)+"\n");
				}
			}
			/*for (ICRNReaction crnReaction : crn.getReactions()) {
				br.write(crnReaction.toStringLBSFormat()+" |\n");
			}*/			

			boolean printedBeginEndComments = false;
			//br.write("//Comments associated to the species\n");
			for (ISpecies species : crn.getSpecies()) {
				boolean speciesWritten=false;
				/*boolean isZeroSpecies = false;
				if(crn.containsTheZeroSpecies()){
					isZeroSpecies=species.equals(crn.getCreatingIfNecessaryTheZeroSpecies());
				}*/
				if((!crn.isZeroSpecies(species)) && (species.getComments()!=null)){
				//if((!species.getName().equals(Species.ZEROSPECIESNAME)) && (species.getComments()!=null)){
					br.write("//"+species.getName()+":  \n");
					for(String comment : species.getComments()){
						if(!comment.equals("")){
							if(!printedBeginEndComments){
								printedBeginEndComments=true;
								//writeCommentLine(br, " Comments associated to the species");
								//br.write(" //Comments associated to the species\n");
								//br.write("begin comments\n");
								br.write("//Comments associated to the species\n");
							}
							if(!speciesWritten){
								br.write(" //"+species.getName()+":  \n");
								speciesWritten=true;
							}
							
						}
						br.write("  //"+comment+"\n");
					}
				}
			}

		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printCRNToCRNFile, exception raised while writing in the file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		finally{
			if(verbose){
				CRNReducerCommandLine.println(out,bwOut,"Writing model in file "+fileName+" completed");
			}
			try {
				br.close();
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in printCRNToCRNFile, exception raised while closing the bufferedwriter of the file: "+fileName);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}

	}

}
