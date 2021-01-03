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
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.CommandsReader;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.crn.implementations.CRN;
import it.imt.erode.crn.implementations.CRNReaction;
import it.imt.erode.crn.implementations.CRNReactionArbitraryGUI;
import it.imt.erode.crn.implementations.Composite;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.expression.evaluator.MathEval;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
//import sun.java2d.xr.MutableInteger;

/**
 * 
 * @author Andrea Vandin
 * This class is used to import reaction networks wrote as a list of reactions, possibly preceded by a list of parameters
 */
public class CRNImporter extends AbstractImporter {

	public static final String CRNNetworksFolder = "."+File.separator+"CRNNetworks"+File.separator;
	private static List<String> sampleCommands;
	private static List<String> sampleCommandsShort;
	private static final boolean PRINTCOMMANDSASCOMMENT=false;
	//protected static int MAXARITY = 0;
	private static String[] supportedCommands;
	
	public CRNImporter(String fileName,MessageConsoleStream out,BufferedWriter bwOut,IMessageDialogShower msgDialogShower) {
		super(fileName,out,bwOut,msgDialogShower);
	}
	
	
	
	public static String[] getSupportedCommands(){
		if(supportedCommands==null){
			//TODO CLEAN!
			supportedCommands=new String[57];
			supportedCommands[0]="reduceDSB";
			supportedCommands[1]="reduceSMB";
			supportedCommands[2]="reduceEFL";
			supportedCommands[3]="simulateCTMC";
			supportedCommands[4]="simulateODE";
			supportedCommands[5]="load";
			supportedCommands[6]="write";
			supportedCommands[7]="quit";
			supportedCommands[8]="quit()";
			supportedCommands[9]="importBNG";
			supportedCommands[10]="exportBNG";
			supportedCommands[11]="-h";
			supportedCommands[12]="--help";
			supportedCommands[13]="print";
			supportedCommands[14]="print()";
			supportedCommands[15]="setParam";
			supportedCommands[16]="setIC";
			supportedCommands[17]="importLBS";
			supportedCommands[18]="exportLBS";
			supportedCommands[19]="-m";
			supportedCommands[20]="--man";
			supportedCommands[21]="multivestaSMC";
			supportedCommands[22]="exportSBML";
			supportedCommands[23]="simulateODEACTMC";
			supportedCommands[24]="exportFlyFast";
			supportedCommands[25]="importBNGL";
			supportedCommands[26]="exportBNGL";
			supportedCommands[27]="exportFluidCompiler";
			supportedCommands[28]="exportZ3";
			supportedCommands[29]="reduceBDE";//supportedCommands[29]="reduceEFLsmt";
			supportedCommands[30]="reduceFDE";//supportedCommands[30]="reduceOOBsmt";
			supportedCommands[31]="reduceGEFLsmt";
			supportedCommands[32]="reduceWFB";
			supportedCommands[33]="newLine";
			supportedCommands[34]="importChemKin";
			supportedCommands[35]="reduceFB";
			supportedCommands[36]="reduceBB";
			supportedCommands[37]="importMRMC";
			supportedCommands[38]="importLinearSystemAsCSVMatrix";
			supportedCommands[39]="importLinearSystemAsCCSVMatrix";
			supportedCommands[40]="reduceFE";
			supportedCommands[41]="reduceBE";
			supportedCommands[42]="importBoolCubeSBML";
			supportedCommands[43]="importMatlabODEs";
			supportedCommands[44]="reduceEMSB";
			supportedCommands[45]="generateCME";
			supportedCommands[46]="importCRN";
			supportedCommands[47]="exportJacobianFunction";
			supportedCommands[48]="reduceEpsNBB";
			supportedCommands[49]="reduceEpsNFB";
			supportedCommands[50]="exportEpsilonBoundsScript";
			supportedCommands[51]="utopic";
			supportedCommands[52]="approximateBDE";
			supportedCommands[53]="approximateFDE";
			supportedCommands[54]="simulateDAE";
			supportedCommands[55]="exportModelica";
			supportedCommands[56]="exportCERENA";
		}
		return supportedCommands;
	}
	
	public static boolean startsWithASupportedCommand(String line){
		String linelc = line.toLowerCase();
		for(String command : getSupportedCommands()){
			if(linelc.startsWith(command.toLowerCase())){
				return true;
			}
		}
		return false;
	}

	/*public CRNImporter() {
		super();
	}*/
	
	/*public CRNImporter(MessageConsoleStream out) {
		this();
		out.print("A message from the plugin");
	}*/
	
	protected static void storeMassActionReaction(HashMap<String, ISpecies> speciesStoredInHashMap, String[] reagentsArray,String[] productsArray, String rateExpression, ICRN crn, MathEval math,String id,String comment) {
		HashMap<ISpecies, Integer> reagentsHM = generateNewSpeciesAndBuildArrayOfNames(speciesStoredInHashMap, reagentsArray,crn);
		//IComposite compositeReagents = crn.addReagentsIfNew(new Composite(reagentsHM));
		IComposite compositeReagents = CRN.compositeOrSpecies(reagentsHM);
		HashMap<ISpecies, Integer> productsHM = generateNewSpeciesAndBuildArrayOfNames(speciesStoredInHashMap, productsArray,crn);
		//IComposite compositeProducts = crn.addProductIfNew(new Composite(productsHM));
		IComposite compositeProducts = CRN.compositeOrSpecies(productsHM);
		/*int arity = compositeReagents.getTotalMultiplicity();
		if(MAXARITY<arity){
			MAXARITY=arity;
		}*/

		BigDecimal reactionRate=null;
		try{
			double r =    math.evaluate(rateExpression);
			reactionRate = BigDecimal.valueOf(r);
		}catch(java.lang.ArithmeticException e){
			//System.out.println("Symbolic rate? "+rateExpression);
		}
		ICRNReaction reaction = new CRNReaction(reactionRate, compositeReagents, compositeProducts, rateExpression,id);
		reaction.addCommentLine(comment);
		addReaction(crn, compositeReagents, compositeProducts, reaction);
	}

	public static void addReaction(ICRN crn, IComposite compositeReagents, IComposite compositeProducts,
			ICRNReaction reaction) {
		//int arity = reaction.getArity();
		crn.addReaction(reaction);
		/*
		addToIncomingReactionsOfProducts(arity,compositeProducts, reaction,CRNReducerCommandLine.addReactionToComposites);
		addToOutgoingReactionsOfReagents(arity, compositeReagents, reaction,CRNReducerCommandLine.addReactionToComposites);
		if(CRNReducerCommandLine.hasToAddToReactionsWithNonZeroStoichiometry){
			addToReactionsWithNonZeroStoichiometry(arity, reaction.computeProductsMinusReagentsHashMap(),reaction);
		}
		*/
	}
	
	protected static void storeArbitraryReaction(HashMap<String, ISpecies> speciesStoredInHashMap, String[] reagentsArray,String[] productsArray, String rateExpression, ICRN crn,String id, String comment) throws IOException {
		HashMap<ISpecies, Integer> reagentsHM = generateNewSpeciesAndBuildArrayOfNames(speciesStoredInHashMap, reagentsArray,crn);
		HashMap<ISpecies, Integer> productsHM = generateNewSpeciesAndBuildArrayOfNames(speciesStoredInHashMap, productsArray,crn);
		//IComposite reagents = crn.addReagentsIfNew(new Composite(reagentsHM));
		IComposite reagents = CRN.compositeOrSpecies(reagentsHM);
		//IComposite products = crn.addProductIfNew(new Composite(productsHM));
		IComposite products = CRN.compositeOrSpecies(productsHM);
		//create reaction reagents -> products, arbitrary rateLaw
		if(rateExpression.startsWith("arbitrary ")||rateExpression.startsWith("Arbitrary ")){
			rateExpression=rateExpression.substring(rateExpression.indexOf(' ')+1);
		}
		ICRNReaction reaction = new CRNReactionArbitraryGUI(reagents, products, rateExpression,id);
		reaction.addCommentLine(comment);
		
		addReaction(crn, reagents, products, reaction);
		
		/*crn.addReaction(reaction);
		addToIncomingReactionsOfProducts(reaction.getArity(),products, reaction,CRNReducerCommandLine.addReactionToComposites);
		addToOutgoingReactionsOfReagents(reaction.getArity(), reaction.getReagents(), reaction,CRNReducerCommandLine.addReactionToComposites);
		if(CRNReducerCommandLine.hasToAddToReactionsWithNonZeroStoichiometry){
			addToReactionsWithNonZeroStoichiometry(reaction.getArity(), reaction.computeProductsMinusReagentsHashMap(),reaction);
		}*/
	}
	
	protected static void storeHillReaction(HashMap<String, ISpecies> speciesStoredInHashMap,String[] reagentsArray, String[] productsArray,String rateExpression, ICRN crn,MessageConsoleStream out,String id, String comment) throws IOException {
		HashMap<ISpecies, Integer> reagentsHM = generateNewSpeciesAndBuildArrayOfNames(speciesStoredInHashMap, reagentsArray,crn);
		IComposite reagents = new Composite(reagentsHM);
		int arity = reagents.computeArity();
		//I assume the reaction to be binary
		// x + xT -> x + x + xT, Hill K R1 R2 n1 n2
		// x + xT ->         xT, Hill K R1 R2 n1 n2
		// (K * x^n1 * xT^n2) / ((R1 + x^n1)*(R2 + xT^n2)) 
		if(arity!=2){
			/*CRNReducerCommandLine.println(out,bwOut,"Only binary reactions are supported for Hill kinetics. ");
			System.exit(-1);*/
			throw new UnsupportedOperationException("Only binary reactions can have Hill kinetics. ");
		}

		StringTokenizer st = new StringTokenizer(rateExpression);
		st.nextToken();//discard Hill
		String K = st.nextToken();
		String R1 = st.nextToken();
		String R2 = st.nextToken();
		String n1 = st.nextToken();
		String n2 = st.nextToken();
		
		
		ISpecies firstReagent = reagents.getFirstReagent();
		ISpecies secondReagent = reagents.getSecondReagent();
		
		
		
		String firstReagToN1 = "(("+firstReagent.getName() +")^(" + n1 + "))";
		String secondReagToN2 = "(("+secondReagent.getName() +")^(" + n2 + "))";
		
		String num = "("+ K +")*"+ firstReagToN1 + "*" + secondReagToN2;
		String denum = "(("+ R1 +")+"+ firstReagToN1 + ")*("+ 
					   "("+ R2 +")+"+ secondReagToN2 +")";
		String rate = "("+num +")/("+ denum+")";
		
		storeArbitraryReaction(speciesStoredInHashMap, reagentsArray, productsArray, rate, crn,id,comment);
		
		//rateExpression = rateExpression + " " + K + " " + R1 + " " + R2 + " " + n1 + " " + n2;
		/*hasHillKinetics = true;
		CRNReducerCommandLine.printWarning(out,bwOut,"Warning: the model has a reaction with Hill kinetics: "+line);
		
		ICRNReaction reaction = new CRNReaction(BigDecimal.valueOf(-1), reagents, products, rateExpression,true);
		getCRN().addReaction(reaction);
		addToIncomingReactionsOfProducts(arity,products, reaction);
		addToOutgoingReactionsOfReagents(arity, reagents, reaction);
		if(CRNReducerCommandLine.hasToAddToReactionsWithNonZeroStoichiometry){
			addToReactionsWithNonZeroStoichiometry(arity, reaction.computeProductsMinusReagentsHashMap(),reaction);
		}*/
		
	}





	public InfoCRNImporting importCRNNetwork(boolean printInfo, boolean printCRN,boolean print) throws FileNotFoundException, IOException{

		if(print){
			CRNReducerCommandLine.println(out,bwOut,"\nImporting: "+getFileName());
		}

		initInfoImporting();


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
		boolean parametersLoaded = false;
		boolean reactionsLoaded = false;
		boolean initialConcentrationsLoaded = false;
		boolean viewsLoaded = false;
		boolean crnOpen=false;
		
		long beginImp = System.currentTimeMillis();
		initMath();

		HashMap<String, ISpecies> speciesNameToSpecies=null;

		while ((line = br.readLine()) != null) {
			line=line.trim();
			line=removeCommentAtTheEnd(line,'#');
			line=removeCommentAtTheEnd(line,"//");

			//Skip comments and empty lines
			if(line.equals("")||line.startsWith("#")||line.startsWith("//")){
				continue;
			}

			if(!crnOpen){
				if(line.startsWith("begin crn")||line.startsWith("begin CRN") ||line.startsWith("begin model")){
					crnOpen=true;

					initCRN();
					getInfoImporting().setLoadedCRN(true);

					String[] parameters = CRNReducerCommandLine.getParameters(line);
					if(parameters!=null){
						for(int p=0;p<parameters.length;p++){
							if(parameters[p].startsWith("isInfluenceNetwork=>")){
								if(parameters[p].length()<="isInfluenceNetwork=>".length()){
									CRNReducerCommandLine.println(out,bwOut,"Please, specify if or not only the partition has to be computed (without thus reducing the model). ");
								}
								else{
									boolean isInfluenceNetwork = Boolean.valueOf(parameters[p].substring("isInfluenceNetwork=>".length()));
									getCRN().setInfluenceNetwork(isInfluenceNetwork);
									getInfoImporting().setLoadedCRNIsInlfuenceNetwork(isInfluenceNetwork);
								}
							}
							else if(parameters[p].equals("")){
								continue;
							}
							else{
								CRNReducerCommandLine.println(out,bwOut,"Unknown model parameter \""+parameters[p]+"\". I skip this it."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
							}
						}
					}
				}
				else if(startsWithASupportedCommand(line) || line.startsWith("this")){
					//else if(line.startsWith("reduce") || line.startsWith("simulate") || line.startsWith("load")|| line.startsWith("write")|| line.startsWith("quit") || line.startsWith("import") || line.startsWith("export") || line.startsWith("this") ){
					//remove comments at the end of the command
					int commentPos=line.indexOf('#');
					if(commentPos!=-1){
						line=line.substring(0, commentPos);
					}
					addCommand(line);
					//getInfoImporting().increaseCommandsCounter();
				}
				else if(line.startsWith("begin parameters") || line.startsWith("begin reactions") || line.startsWith("begin initialConcentrations") || line.startsWith("begin views")){
					CRNReducerCommandLine.printWarning(out,bwOut,"Line \""+line +"\" found outside a begin CRN - end CRN block. It will be ignored.");
				}
				else{
					CRNReducerCommandLine.printWarning(out,bwOut,"Unknown line: "+line+"\nI skip this line.");
				}
			}
			else{//crnOpen=true
				//parameters are optional. If there are some, they must appear before the species
				if((!parametersLoaded) && line.startsWith("begin parameters")){
					initMath();//I throw away the old math
					loadParameters(br,false,true);
					parametersLoaded = true;
				}
				else if((!reactionsLoaded) && line.startsWith("begin reactions")){
					speciesNameToSpecies =loadReactions(br,speciesNameToSpecies);
					createInitialPartition();
					reactionsLoaded=true;
				}
				else if((!reactionsLoaded) && line.startsWith("begin ODE")){
					speciesNameToSpecies =loadODEs(br,speciesNameToSpecies);
					createInitialPartition();
					reactionsLoaded=true;
				}
				else if(line.startsWith("begin algebraic"))
				{
					speciesNameToSpecies = loadAlgebraic(br,speciesNameToSpecies);
					
				}
				else if(reactionsLoaded && (!initialConcentrationsLoaded) && line.startsWith("begin initialConcentrations")){
					//The initial concentrations must be specified after the reactions
					loadInitialConcentrations(br,speciesNameToSpecies);
					initialConcentrationsLoaded=true;
				}
				else if(line.startsWith("begin species")||line.startsWith("begin init")){
					speciesNameToSpecies = loadSpecies(br/*,line.startsWith("begin init")*/);
				}
				else if(line.startsWith("begin alginit"))
				{
					speciesNameToSpecies = loadAlgSpecies(br,line.startsWith("begin alginit"),speciesNameToSpecies);      // forse c'è da metterle in algebraic? 
				}
				else if(line.startsWith("begin partition")){
					loadPartition(br,speciesNameToSpecies);
				}
				else if(reactionsLoaded && (!viewsLoaded) && line.startsWith("begin views")){
					//The views must be specified after the reactions
					loadViews(br,speciesNameToSpecies,getCRN());
					viewsLoaded=true;
				}
				else if(line.startsWith("end CRN")||line.startsWith("end crn")||line.startsWith("end model")){
					crnOpen=false;
				}
				else{
					CRNReducerCommandLine.printWarning(out,bwOut,"Unknown line: "+line+"\nI skip this line.");
				}
			}
		}
		br.close();
		long endImp = System.currentTimeMillis();
		getInfoImporting().setRequiredMS(endImp-beginImp);
		getInfoImporting().setLoadedCRNFormat(getCRN().getMdelDefKind());
		if(printInfo&&print){
			CRNReducerCommandLine.println(out,bwOut,getInfoImporting().toString());
		}
		if(printCRN&&print&&getCRN()!=null){
			getCRN().printCRN();
		}

		/*if(getCRN()!=null){
		checkACTMC();
		}*/
		
		/*
		//Add an user partition with one block containing all species starting with S
		ArrayList<HashSet<ISpecies>> userDefinedInitialPartition = new ArrayList<HashSet<ISpecies>>(1);
		getCRN().setUserDefinedPartition(userDefinedInitialPartition);
		HashSet<ISpecies> block = new HashSet<ISpecies>();
		userDefinedInitialPartition.add(block);
		for(ISpecies s : getCRN().getSpecies()) {
			if(s.getName().startsWith("S")) {
				block.add(s);
			}
		}
		*/

		return getInfoImporting();
	}

	private void loadPartition(BufferedReader br, HashMap<String, ISpecies> speciesNameToSpecies) throws IOException {
		ArrayList<ArrayList<String>> initialPartition=new ArrayList<>();
		String line=br.readLine();
		line=line.trim();
		while(!line.startsWith("end partition")){
			if(line.startsWith("//")) {
				//Do nothing
			}
			else {
				while(line.indexOf('}')>0){
					int firstClosedPar = line.indexOf('}');
					String block;
					if(firstClosedPar<line.length()){
						block=line.substring(0,firstClosedPar+1).trim();
						line=line.substring(firstClosedPar+1).trim();
						//remove comma
						if(line.startsWith(",")){
							line=line.substring(1).trim();
						}
						if(line.endsWith(",")){
							line=line.substring(0,line.length()-1).trim();
						}
						block=block.substring(1,block.length()-1);
						String[] speciesOfBlock=block.split("\\,");
						ArrayList<String> bl = new ArrayList<>(speciesOfBlock.length);
						for(int i=0;i<speciesOfBlock.length;i++){
							bl.add(speciesOfBlock[i].trim());
						}
						if(!bl.isEmpty()){
							initialPartition.add(bl);
						}
					}
					else{
						block=line;
						line="";
					}
				}
			}

			line=br.readLine();
			line=line.trim();
		}

		GUICRNImporter.readPartition(initialPartition, speciesNameToSpecies, getCRN());
	}



	/**
	 * This method is used to load the optional list of species. It is useful in case one wants to give an order on the species
	 * I now impose that there must be an = if you want to specify the IC
	 * @param br
	 * @return
	 * @throws IOException
	 */
	private HashMap<String, ISpecies> loadSpecies(BufferedReader br/*, boolean erodeFormat*/)  throws IOException {

		HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>();
		
		//int id=0;
		
		String line = br.readLine();
		while ((line != null) && (!(line=line.trim()).startsWith("end species")) && (!(line=line.trim()).startsWith("end init"))) {
			if(!(line.equals("") || line.startsWith("#") || line.startsWith("//"))){
				line=removeCommentAtTheEnd(line,'#');
				line=removeCommentAtTheEnd(line,"//");
				
				//Read a list of species without IC: x0 x1 x2
				//	Species can stay in the same line if
				//		they have no IC x = 1
				//		they have no original name. x ("originalname")
				//		both
				if(!(line.contains("=")|| line.contains("\""))) {
					StringTokenizer st = new StringTokenizer(line);
					while(st.hasMoreTokens()) {
						String speciesName = st.nextToken();
						addSpecies(speciesName, null, "0", speciesNameToSpecies);
					}
				}
				else {
					//If you specify an IC or an original name for a species, then it has to stay on its own line.
					//Read the initial concentration  "x0 = arithmeticExpression"
					
					//It does not work for species defined in the same line!
					String speciesName = line.trim();
					String originalName=null;
					
					//BigDecimal ic = BigDecimal.ZERO;
					String expr="0";
					if(speciesName.contains("=")){
						String[] s=speciesName.split("\\=");
						speciesName=s[0].trim();
						
						s[1]=s[1].trim();
						//These checks do not work for: Q1 = 1.0 * n-15 which becomes Q1 = 1.0.
						//I added them for Q1 = 1.0 ("originalName")
						//	I have to discard the original name
						/*
						if(s[1].indexOf(' ')>0){
							s[1]=s[1].substring(0,s[1].indexOf(' '));
						}
						s[1]=s[1].trim();
						if(s[1].indexOf(' ')>0){
							s[1]=s[1].substring(0,s[1].indexOf(' '));
						}
						*/
						//I replace them with:
						s[1]=s[1].replaceAll("\\s+","");
						int originalNameStart = s[1].indexOf("(\"");
						if(originalNameStart!=-1) {
							int originalNameEnd = s[1].indexOf("\")");
							originalName=s[1].substring(originalNameStart+2,originalNameEnd);
							s[1]=s[1].substring(0,originalNameStart);
						}
						
						
						
						if((!s[1].equals("0"))||(!s[1].equals("0.0"))){
							expr=s[1];
							//ic=new BigDecimal(getMath().evaluate(expr));
						}
					}
					else {
						//There might be the orginal name: x0 ("ciao")
						speciesName=speciesName.replaceAll("\\s+","");
						int originalNameStart = speciesName.indexOf("(\"");
						if(originalNameStart!=-1) {
							int originalNameEnd = speciesName.indexOf("\")");
							originalName=speciesName.substring(originalNameStart+2,originalNameEnd);
							speciesName=speciesName.substring(0,originalNameStart);
						}
					}
					
					
					//If the name contains also the original name, I discard it like this.
//					if(speciesName.indexOf(' ')>0){
//						speciesName=speciesName.substring(0,speciesName.indexOf(' '));
//						int originalNameStart = speciesName.indexOf("(\"");
//						if(originalNameStart!=-1) {
//							//int originalNameEnd = s[1].indexOf("\")");
//							speciesName=speciesName.substring(0,originalNameStart);
//						}
//					}
					
					
					addSpecies(speciesName, originalName, expr, speciesNameToSpecies);
					
					

//					ISpecies species = new Species(speciesName, id, ic, expr);
//					id++;
//					speciesNameToSpecies.put(speciesName, species);
//					getCRN().addSpecies(species);
				}
				
				
				
				
			}
			line = br.readLine();
		}
		
		
		return speciesNameToSpecies;
	}
	
//	
//	private HashMap<String, ISpecies> loadSpeciesOldFormat(BufferedReader br)  throws IOException {
//
//		HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>();
//		
//		//int id=0;
//		
//		String line = br.readLine();
//		while ((line != null) && (!(line=line.trim()).startsWith("end species")) && (!(line=line.trim()).startsWith("end init"))) {
//			if(!(line.equals("") || line.startsWith("#") || line.startsWith("//"))){
//				line=removeCommentAtTheEnd(line,'#');
//				line=removeCommentAtTheEnd(line,"//");
//				
//				
//				//Read the initial concentration  "x0 arithmeticExpression"
//				
//				//It does not work for species defined in the same line!
//				String speciesName = line;
//				
//				//BigDecimal ic = BigDecimal.ZERO;
//				String expr="0";
//				if(speciesName.contains("=")){
//					String[] s=speciesName.split("\\=");
//					speciesName=s[0].trim();
//					
//					s[1]=s[1].trim();
//					//These checks do not work for: Q1 = 1.0 * n-15 which becomes Q1 = 1.0.
//					//I added them for Q1 = 1.0 ("originalName")
//					//	I have to discard the original name
//					/*
//					if(s[1].indexOf(' ')>0){
//						s[1]=s[1].substring(0,s[1].indexOf(' '));
//					}
//					s[1]=s[1].trim();
//					if(s[1].indexOf(' ')>0){
//						s[1]=s[1].substring(0,s[1].indexOf(' '));
//					}
//					*/
//					//I replace them with:
//					s[1]=s[1].replaceAll("\\s+","");
//					int originalNameStart = s[1].indexOf("(\"");
//					if(originalNameStart!=-1) {
//						//int originalNameEnd = s[1].indexOf("\")");
//						s[1]=s[1].substring(0,originalNameStart);
//					}
//					
//					
//					
//					if((!s[1].equals("0"))||(!s[1].equals("0.0"))){
//						expr=s[1];
//						//ic=new BigDecimal(getMath().evaluate(expr));
//					}
//				}
//				
//				//If the name contains also the original name, I discard it like this.
//				if(speciesName.indexOf(' ')>0){
//					speciesName=speciesName.substring(0,speciesName.indexOf(' '));
//				}
//				
//				
//				addSpecies(speciesName, null, expr, speciesNameToSpecies);
//				
//				
//
////				ISpecies species = new Species(speciesName, id, ic, expr);
////				id++;
////				speciesNameToSpecies.put(speciesName, species);
////				getCRN().addSpecies(species);
//				
//			}
//			line = br.readLine();
//		}
//		
//		
//		return speciesNameToSpecies;
//	}

	
	private HashMap<String, ISpecies> loadAlgSpecies(BufferedReader br, boolean erodeFormat, HashMap<String, ISpecies> speciesNameToSpecies)  throws IOException {
		
		if ( speciesNameToSpecies == null ) 
		{
			speciesNameToSpecies = new HashMap<>();
		}
		
		//int id=0;
		
		String line = br.readLine();
		while ((line != null) && (!(line=line.trim()).startsWith("end species")) && (!(line=line.trim()).startsWith("end alginit"))) {
			if(!(line.equals("") || line.startsWith("#") || line.startsWith("//"))){
				line=removeCommentAtTheEnd(line,'#');
				line=removeCommentAtTheEnd(line,"//");
				//Read the initial concentration  "x0 arithmeticExpression"
				String speciesName = line;
				
				//BigDecimal ic = BigDecimal.ZERO;
				String expr="0";
				if(speciesName.contains("=")){
					String[] s=speciesName.split("\\=");
					speciesName=s[0].trim();
					
					s[1]=s[1].trim();
					if(s[1].indexOf(' ')>0){
						s[1]=s[1].substring(0,s[1].indexOf(' '));
					}
					s[1]=s[1].trim();
					if(s[1].indexOf(' ')>0){
						s[1]=s[1].substring(0,s[1].indexOf(' '));
					}
					if((!s[1].equals("0"))||(!s[1].equals("0.0"))){
						expr=s[1];
						//ic=new BigDecimal(getMath().evaluate(expr));
					}
				}
				
				
				if(speciesName.indexOf(' ')>0){
					speciesName=speciesName.substring(0,speciesName.indexOf(' '));
				}
				
				addSpecies(speciesName, null, expr, speciesNameToSpecies,true);
				
				

//				ISpecies species = new Species(speciesName, id, ic, expr);
//				id++;
//				speciesNameToSpecies.put(speciesName, species);
//				getCRN().addSpecies(species);
				
			}
			line = br.readLine();
		}
		
		
		return speciesNameToSpecies;
	}


	/**
	 * It is assumed that the reactions have been already read (and thus the species have been created)
	 * @param br the buffered reader from which the initial concentrations have to be read
	 * @param speciesNameToSpecies an hash map from species name to species
	 * @throws IOException 
	 */
	private void loadInitialConcentrations(BufferedReader br, HashMap<String,ISpecies> speciesNameToSpecies) throws IOException {

		String line = br.readLine();
		while ((line != null) && (!(line=line.trim()).startsWith("end initialConcentrations"))) {
			//Skip comments or empty lines
			if(!(line.equals("") || line.startsWith("#"))){
				line=removeCommentAtTheEnd(line,'#');

				//Read the initial concentration  "x0 arithmeticExpression"
				int posFirstSpaceAfterSpeciesName = line.indexOf(' ');
				String speciesName = line.substring(0,posFirstSpaceAfterSpeciesName);
				String icExpr = line.substring(posFirstSpaceAfterSpeciesName+1, line.length()).trim();
				BigDecimal initialConcentration = BigDecimal.valueOf(evaluate(icExpr));
				ISpecies species = speciesNameToSpecies.get(speciesName);
				if(species!=null){
					species.setInitialConcentration(initialConcentration,icExpr);
				}
				else{
					CRNReducerCommandLine.printWarning(out,bwOut,"Initial concentration "+ initialConcentration+" ignored for the non existing species "+speciesName);
				}
			}
			line = br.readLine();
		}

	}

	/**
	 * It is assumed that the reactions have been already read (and thus the species have been created)
	 * @param br the buffered reader from which to read
	 * @param speciesNameToSpecies 
	 * @param speciesNameToSpecies an hash map from species name to species
	 * @throws IOException 
	 */
	protected static void loadViews(BufferedReader br, HashMap<String, ISpecies> speciesNameToSpecies, ICRN crn) throws IOException {

		String line = br.readLine();
		List<String> viewNames = new ArrayList<String>();
		List<String> viewExpressions = new ArrayList<String>();
		List<String> viewExpressionsSupportedByMathEval = new ArrayList<String>();
		List<Boolean> viewExpressionUsesCovariance = new ArrayList<Boolean>();
		boolean viewsSupportedForPrepartitioning=true;
		//The user has specified an initial partition.
		if(!crn.getUserDefinedPartition().isEmpty()){
			viewsSupportedForPrepartitioning=false;
		}
		List<HashMap<ISpecies,Integer>> setsOfSpeciesOfViews = new ArrayList<HashMap<ISpecies,Integer>>();
		while ((line != null) && (!(line=line.trim()).startsWith("end views"))) {
			//Skip comments or empty lines
			if(!(line.equals("") || line.startsWith("#") || line.startsWith("//"))){

				line = removeCommentAtTheEnd(line,'#');
				line = removeCommentAtTheEnd(line,"//");
				
				String viewName;
				String viewExpr;
				
				if(line.contains("=")){
					String[] v=line.split("\\=");
					viewName=v[0].trim();
					viewExpr=v[1].trim();
				}
				else{
					//Read the view  "viewName arithmeticExpression"
					int posFirstSpaceAfterViewName = line.indexOf(' ');
					viewName = line.substring(0,posFirstSpaceAfterViewName).trim();
					viewExpr = line.substring(posFirstSpaceAfterViewName+1, line.length()).replace(" ", "").trim();
				}

				viewsSupportedForPrepartitioning=loadAView(viewNames,viewExpressions,viewExpressionsSupportedByMathEval,viewExpressionUsesCovariance,viewName,viewExpr,viewsSupportedForPrepartitioning,speciesNameToSpecies,setsOfSpeciesOfViews);
			}
			line = br.readLine();
		}
		finalizeToLoadViews(viewNames, viewExpressions,
				viewExpressionsSupportedByMathEval,
				viewExpressionUsesCovariance, viewsSupportedForPrepartitioning,
				setsOfSpeciesOfViews,crn);
	}

	protected static boolean loadAView(List<String> viewNames, List<String> viewExpressions, List<String> viewExpressionsSupportedByMathEval, List<Boolean> viewExpressionUsesCovariance, String viewName, String viewExpr, boolean viewsSupportedForPrepartitioning, HashMap<String, ISpecies> speciesNameToSpecies, List<HashMap<ISpecies, Integer>> setsOfSpeciesOfViews) {
		viewNames.add(viewName);
		StringBuilder sb = new StringBuilder();
		StringBuilder sbViews = new StringBuilder();
		MutableInteger openParenthesis=new MutableInteger(0);
		boolean usesCovariance=visitExpr(sb, sbViews,viewExpr,openParenthesis,speciesNameToSpecies);
		String expr = sb.toString();
		viewExpressions.add(expr);
		viewExpressionsSupportedByMathEval.add(sbViews.toString());
		viewExpressionUsesCovariance.add(usesCovariance);
		
		
		//When prepartitioning according to views, views with covariances are ignored.
//		if((!usesCovariance) && viewsSupportedForPrepartitioning){
//			HashMap<ISpecies, Integer> setsOfSpeciesOfView = setOfSpeciesInCaseViewIsAMultiSet(expr,speciesNameToSpecies);
//			if(setsOfSpeciesOfView==null){
//				viewsSupportedForPrepartitioning=false;
//				setsOfSpeciesOfViews=null;
//			}
//			else{
//				setsOfSpeciesOfViews.add(setsOfSpeciesOfView);
//			}
//		}

		return viewsSupportedForPrepartitioning;
	}

	protected static void finalizeToLoadViews(List<String> viewNames,
			List<String> viewExpressions,
			List<String> viewExpressionsSupportedByMathEval,
			List<Boolean> viewExpressionUsesCovariance,
			boolean viewsSupportedForPrepartitioning,
			List<HashMap<ISpecies, Integer>> setsOfSpeciesOfViews, ICRN crn) {
		String[] viewNamesArray = new String[viewNames.size()];
		String[] viewExpressionsArray = new String[viewNames.size()];
		String[] viewExpressionsSupportedByMathEvalArray = new String[viewNames.size()];
		boolean[] viewExpressionsUsesCovarianceArray = toboolean(viewExpressionUsesCovariance);
		crn.setViews(viewNames.toArray(viewNamesArray), viewExpressions.toArray(viewExpressionsArray),viewExpressionsSupportedByMathEval.toArray(viewExpressionsSupportedByMathEvalArray),viewExpressionsUsesCovarianceArray);

		if(viewsSupportedForPrepartitioning){
			crn.setViewsAsMultiset(setsOfSpeciesOfViews);
			crn.setUserDefinedPartition(viewsAsMultisetsToUserPartition(setsOfSpeciesOfViews,crn));
		}
	}

	private static boolean visitExpr(StringBuilder sb, StringBuilder sbViews, String expr,MutableInteger openParenthesis, HashMap<String, ISpecies> speciesNameToSpecies) {
		boolean usesCovariances=false;
		if(expr.startsWith("(")){
			sb.append('(');
			sbViews.append('(');
			openParenthesis.setValue(openParenthesis.getValue()+1);
			usesCovariances=visitExpr(sb,sbViews, expr.substring(1),openParenthesis,speciesNameToSpecies);
		}
		else if(expr.contains("*")){
			String[] nodes = expr.split("\\*");
			for(int i=0;i<nodes.length;i++){
				String node=nodes[i].trim();
				boolean usesCov=visitExpr(sb,sbViews,node,openParenthesis,speciesNameToSpecies);
				usesCovariances=usesCovariances||usesCov;
				if(i!=nodes.length-1){
					sb.append('*');
					sbViews.append('*');
				}
			}
		}
		else if(expr.contains("/")){
			String[] nodes = expr.split("/");
			for(int i=0;i<nodes.length;i++){
				String node=nodes[i].trim();
				boolean usesCov=visitExpr(sb,sbViews,node,openParenthesis,speciesNameToSpecies);
				usesCovariances=usesCovariances||usesCov;
				if(i!=nodes.length-1){
					sb.append('/');
					sbViews.append('/');
				}
			}
		}
		else if(expr.contains("+")){
			String[] nodes = expr.split("\\+");
			for(int i=0;i<nodes.length;i++){
				String node=nodes[i].trim();
				boolean usesCov=visitExpr(sb,sbViews,node,openParenthesis,speciesNameToSpecies);
				usesCovariances=usesCovariances||usesCov;
				if(i!=nodes.length-1){
					sb.append('+');
					sbViews.append('+');
				}
			}
		}
		else if(expr.contains("-")){
			String[] nodes = expr.split("\\-");
			for(int i=0;i<nodes.length;i++){
				String node=nodes[i].trim();
				boolean usesCov=visitExpr(sb,sbViews,node,openParenthesis,speciesNameToSpecies);
				usesCovariances=usesCovariances||usesCov;
				if(i!=nodes.length-1){
					sb.append('-');
					sbViews.append('-');
				}
			}
		}
		else if(expr.startsWith("var(")){
			//TODO: PROBLEM IF SPECIES NAME CONTAINS PARENTHESIS
			String speciesName = expr.substring(4,expr.indexOf(')'));
			sb.append(expr);
			sbViews.append("V_"+MathEval.getCorrespondingStringSupportedByMathEval(speciesName));
			usesCovariances=true;
		}
		else if(expr.startsWith("covar(")){
			int commaPos=expr.indexOf(',');
			String species1Name = expr.substring(6,commaPos);
			//TODO: PROBLEM IF SPECIES NAME CONTAINS PARENTHESIS
			String species2Name = expr.substring(commaPos+1,expr.indexOf(')'));
			sb.append(expr);
			sbViews.append("C_"+MathEval.getCorrespondingStringSupportedByMathEval(species1Name)+"_"+MathEval.getCorrespondingStringSupportedByMathEval(species2Name));
			usesCovariances=true;
		}
		else if(expr.endsWith(")") && openParenthesis.getValue()>0){
			openParenthesis.setValue(openParenthesis.getValue()-1);
			boolean usesCov=visitExpr(sb, sbViews,expr.substring(0,expr.length()-1),openParenthesis/*--*/,speciesNameToSpecies);
			usesCovariances=usesCovariances||usesCov;
			sb.append(')');
			sbViews.append(')');
		}
		else{
			//Then it can be either a number, a species name or a parameter name.
			//Add other else if cases if required.
				if(speciesNameToSpecies.get(expr)!=null){
					//it is a species
					//Then I assume that this the name of a species.
					sb.append(expr);
					sbViews.append(MathEval.getCorrespondingStringSupportedByMathEval(expr));
				}
				else{
					try{
					double number = Double.valueOf(expr);
					sb.append(number);
					sbViews.append(number);
					}catch(NumberFormatException e){
						//Then I assume that this is a parameter.
						sb.append(expr);
						//sbViews.append(MathEval.getCorrespondingStringSupportedByMathEval(expr));
						sbViews.append(expr);
					}
				}
		}
		return usesCovariances;
	}


//	private static LinkedHashMap<ISpecies,Integer> setOfSpeciesInCaseViewIsAMultiSet(String group, HashMap<String, ISpecies> speciesNameToSpecies){
//
//		LinkedHashMap<ISpecies,Integer> speciesOfTheGroup = new LinkedHashMap<ISpecies,Integer>();
//		String[] addends=group.split("\\+");
//		for(int i=0;i<addends.length;i++){
//			String addend = addends[i].trim();
//			if(!addend.equals("")){
//				//Case 1: Views made of general arithmetical expressions are not supported. We accept only "speciesID" or "multiplicity*speciesID" (e.g., 2*4). We also accept the case in which a species appears more than once.
//				if(addend.contains("/")||addend.contains("-")||addend.contains("+")||addend.contains("(")){//The "+" could actually be treated. 
//					return null;
//				}
//				//Case 2: multiplicity*speciesID (e.g., 2*4)
//				else if(addend.contains("*")){ 
//					String[] factors=addend.split("\\*");
//					if(factors.length!=2){
//						return null;
//					}
//					else{
//						int mult = Integer.valueOf(factors[0]);
//						ISpecies species = speciesNameToSpecies.get(factors[1]); //getCRN().getSpecies().get(Integer.valueOf(factors[1])-decrement);
//						if(species==null){
//							return null;
//						}
//						Integer prevMultiplicity = speciesOfTheGroup.get(species);
//						if(prevMultiplicity==null){
//							speciesOfTheGroup.put(species,mult);
//						}
//						else{
//							speciesOfTheGroup.put(species,mult + prevMultiplicity);
//						}
//					}
//				}
//				//Case 3: just a simple species name
//				else{
//					ISpecies species = speciesNameToSpecies.get(addend); //getCRN().getSpecies().get(Integer.valueOf(addend)-decrement);
//					Integer prevMultiplicity = speciesOfTheGroup.get(species);
//					if(prevMultiplicity==null){
//						speciesOfTheGroup.put(species,1);
//					}
//					else{
//						speciesOfTheGroup.put(species,1 + prevMultiplicity);
//					}
//				}
//			}
//		}
//		return speciesOfTheGroup;
//	}

	private static boolean[] toboolean(List<Boolean> list){
		boolean[] ret=new boolean[list.size()];
		for(int i=0;i<list.size();i++){
			ret[i]=list.get(i);
		}
		return ret;
	}



	/**
	 * It is assumed that the id of a species corresponds to the order with which it appears in the reactions. Also, we assume that multiplicies of species in the reagents or products are always explicit: "X+X->Y+Y+Y,rate" rather than "2X->3Y,rate".
	 * If instead the species have been already loaded, then that order is kept. 
	 * @param br
	 * @param crn
	 * @param uniqueBlock 
	 * @param math
	 * @param infoImporting
	 * @throws IOException
	 */
	private HashMap<String, ISpecies> loadReactions(BufferedReader br,HashMap<String, ISpecies> speciesStoredInHashMap) throws IOException {
		getCRN().setMdelDefKind(ODEorNET.RN);
		if(speciesStoredInHashMap==null){
			//I use this data structure to efficiently check if a species has been already generated.
			speciesStoredInHashMap = new HashMap<String, ISpecies>();
			//I use this data structure to efficiently check if a product has been already generated.
			//HashMap<IComposite,IComposite> productsStoredInHashMap = new HashMap<IComposite, IComposite>();
			//I use this data structure to efficiently check if a reagent has been already generated.
			//HashMap<IComposite,IComposite> reagentsStoredInHashMap = new HashMap<IComposite, IComposite>();
		}
		
		String line = br.readLine();
		while ((line != null) && (!(line=line.trim()).startsWith("end reactions"))) {
			//Skip comments or empty lines
			if(!(line.equals("") || line.startsWith("#") || line.startsWith("//"))){

				line = parseAndAddReaction(speciesStoredInHashMap, line,getCRN(),out,getMath());
			}
			line = br.readLine();
		}
		getInfoImporting().setReadCRNReactions(getCRN().getReactions().size());
		//getInfoImporting().setReadProducts(getCRN().getProducts().size());
		//getInfoImporting().setReadReagents(getCRN().getReagents().size());
		getInfoImporting().setReadSpecies(getCRN().getSpecies().size());
		getInfoImporting().setReadParameters(getCRN().getParameters().size());
		return speciesStoredInHashMap;
	}


	public static String parseAndAddReaction(HashMap<String, ISpecies> speciesStoredInHashMap, String line,ICRN crn,MessageConsoleStream out,MathEval math) throws IOException {
		return parseAndAddReaction(speciesStoredInHashMap, line,crn,out,math,null);
	}

	public static String parseAndAddReaction(HashMap<String, ISpecies> speciesStoredInHashMap, String line,ICRN crn,MessageConsoleStream out,MathEval math, String comment) throws IOException {
		line = removeCommentAtTheEnd(line,'#');
		line = removeCommentAtTheEnd(line,"//");
		
		int idPos=line.indexOf('[');
		String id=null;
		if(idPos!=-1){
			id = line.substring(idPos+1,line.indexOf(']')).trim();
			line = line.substring(0, idPos).trim();
		}

		//Read the reaction x0 + x2 -> x2 + x1 , 1.0
		int arrowPos = line.indexOf("->");
		
		boolean arbitrary=false;
		int arbitraryPos=line.indexOf("arbitrary");
		if(arbitraryPos==-1){
			arbitraryPos=line.indexOf("Arbitrary");
		}
		if(arbitraryPos>0){
			arbitrary=true;
		}
		
		int lastCommaPos = line.lastIndexOf(",");
		if(arbitrary){
			String app = line.substring(0,arbitraryPos+1);
			lastCommaPos=app.lastIndexOf(",");
		}
		String reagentsString = line.substring(0, arrowPos).trim();
		String productsString = line.substring(arrowPos+2,lastCommaPos).trim();
		String rateString = line.substring(lastCommaPos+1).trim();
		int commentPos=rateString.indexOf('#');
		if(commentPos!=-1){
			commentPos=rateString.indexOf("//");
			if(commentPos!=-1){
				rateString=rateString.substring(0, commentPos);
			}
		}
		String[] reagentsArray = reagentsString.split("\\+");
		String[] productsArray = productsString.split("\\+");
		
		if(rateString.startsWith("Hill") ||rateString.startsWith("hill")){
			storeHillReaction(speciesStoredInHashMap, reagentsArray, productsArray, rateString,crn,out,id,comment);
		}
		else if(rateString.startsWith("arbitrary") ||rateString.startsWith("Arbitrary")){
			String rs = rateString.replace("arbitrary", "").trim();
			if(!rs.equals("0")) {
				storeArbitraryReaction(speciesStoredInHashMap, reagentsArray, productsArray, rateString,crn,id,comment);
			}
		}
		else{
			storeMassActionReaction(speciesStoredInHashMap, reagentsArray, productsArray, rateString,crn,math,id,comment);
		}
		return line;
	}
	
	private HashMap<String, ISpecies> loadODEs(BufferedReader br,HashMap<String, ISpecies> speciesStoredInHashMap) throws IOException {
		getCRN().setMdelDefKind(ODEorNET.ODE);
		if(speciesStoredInHashMap==null){
			//I use this data structure to efficiently check if a species has been already generated.
			speciesStoredInHashMap = new HashMap<String, ISpecies>();
			//I use this data structure to efficiently check if a product has been already generated.
			//HashMap<IComposite,IComposite> productsStoredInHashMap = new HashMap<IComposite, IComposite>();
			//I use this data structure to efficiently check if a reagent has been already generated.
			//HashMap<IComposite,IComposite> reagentsStoredInHashMap = new HashMap<IComposite, IComposite>();
		}
		
		String line = br.readLine();
		while ((line != null) && (!(line=line.trim()).startsWith("end ODE"))) {
			//Skip comments or empty lines
			if(!(line.equals("") || line.startsWith("#") || line.startsWith("//"))){

				line = removeCommentAtTheEnd(line,'#');
				line = removeCommentAtTheEnd(line,"//");
				// d(B1) = -o1*B1 + i1*S1 + i1t2*(T1_2)*S1 + i1t3*(T1_3)*S1
				
				int posOfEqual = line.indexOf('=');
				int posOfOpenPar=line.indexOf('(');
				int posOfClosedPar=line.indexOf(')');
				String species=line.substring(posOfOpenPar+1,posOfClosedPar).trim();
				String rateString = line.substring(posOfEqual+1).trim();
				String[] reagentsArray = new String[1];
				reagentsArray[0]=species;
				String[] productsArray = new String[2];
				productsArray[0]=species;
				productsArray[1]=species;
				storeArbitraryReaction(speciesStoredInHashMap, reagentsArray, productsArray, rateString,getCRN(),null,null);
			}
			line = br.readLine();
		}
		getInfoImporting().setReadCRNReactions(getCRN().getReactions().size());
		//getInfoImporting().setReadProducts(getCRN().getProducts().size());
		//getInfoImporting().setReadReagents(getCRN().getReagents().size());
		getInfoImporting().setReadSpecies(getCRN().getSpecies().size());
		getInfoImporting().setReadParameters(getCRN().getParameters().size());
		return speciesStoredInHashMap;
	}
	
	private HashMap<String, ISpecies> loadAlgebraic(BufferedReader br,HashMap<String, ISpecies> speciesStoredInHashMap) throws IOException {
		getCRN().setMdelDefKind(ODEorNET.ODE);
		if(speciesStoredInHashMap==null){
			//I use this data structure to efficiently check if a species has been already generated.
			speciesStoredInHashMap = new HashMap<String, ISpecies>();
			//I use this data structure to efficiently check if a product has been already generated.
			//HashMap<IComposite,IComposite> productsStoredInHashMap = new HashMap<IComposite, IComposite>();
			//I use this data structure to efficiently check if a reagent has been already generated.
			//HashMap<IComposite,IComposite> reagentsStoredInHashMap = new HashMap<IComposite, IComposite>();
		}
		
		String line = br.readLine();
		while ((line != null) && (!(line=line.trim()).startsWith("end algebraic"))) {
			//Skip comments or empty lines
			if(!(line.equals("") || line.startsWith("#") || line.startsWith("//"))){

				line = removeCommentAtTheEnd(line,'#');
				line = removeCommentAtTheEnd(line,"//");
				// d(B1) = -o1*B1 + i1*S1 + i1t2*(T1_2)*S1 + i1t3*(T1_3)*S1
				
				int posOfEqual = line.indexOf('=');
				String species=line.substring(0,posOfEqual).trim();
				String rateString = line.substring(posOfEqual+1).trim();
				String[] reagentsArray = new String[1];
				reagentsArray[0]=species;
				String[] productsArray = new String[2];
				productsArray[0]=species;
				productsArray[1]=species;
				storeArbitraryReaction(speciesStoredInHashMap, reagentsArray, productsArray, rateString,getCRN(),null,null);
			}
			line = br.readLine();
		}
		getInfoImporting().setReadCRNReactions(getCRN().getReactions().size());
		//getInfoImporting().setReadProducts(getCRN().getProducts().size());
		//getInfoImporting().setReadReagents(getCRN().getReagents().size());
		getInfoImporting().setReadSpecies(getCRN().getSpecies().size());
		getInfoImporting().setReadParameters(getCRN().getParameters().size());
		return speciesStoredInHashMap;
	}

	private static String toStringCRNFileFormat(ICRNReaction reaction, boolean compactNames) {
		String reagentsMultiSet = "";
		String productsMultiSet = "";
		
		if(compactNames){
			reagentsMultiSet = reaction.getReagents().toMultiSetCompact();
			productsMultiSet = reaction.getProducts().toMultiSetCompact();
		}
		else{
			reagentsMultiSet = reaction.getReagents().toMultiSet();
			productsMultiSet = reaction.getProducts().toMultiSet();
		}
		

		//return reagentsMultiSet + " -> " + productsMultiSet + ", " + getRateExpression();
		if(!reaction.hasArbitraryKinetics()){
			return reagentsMultiSet + " -> " + productsMultiSet + ", " + reaction.getRateExpression(); //reaction.getRate()
		}
		else{
			return reagentsMultiSet + " -> " + productsMultiSet + ", arbitrary " + reaction.getRateExpression();
		}
		
	}

	public static void printCRNToCRNFile(ICRN crn,IPartition partition, String name, boolean assignPopulationOfRepresentative, boolean groupAccordingToCurrentPartition, Collection<String> preambleCommentLines, boolean verbose, String icComment,MessageConsoleStream out,BufferedWriter bwOut,boolean writeSpecies){
		printCRNToCRNFile(crn,partition,name,assignPopulationOfRepresentative,groupAccordingToCurrentPartition,preambleCommentLines,verbose,icComment,out,bwOut,false,writeSpecies);
	}
	public static void printCRNToCRNFile(ICRN crn,IPartition partition, String name, boolean assignPopulationOfRepresentative, boolean groupAccordingToCurrentPartition, Collection<String> preambleCommentLines, boolean verbose, String icComment,MessageConsoleStream out,BufferedWriter bwOut, boolean compactNames,boolean writeSpecies){
		String fileName = name;
		fileName=overwriteExtensionIfEnabled(fileName,".crn");
		if(verbose){
			CRNReducerCommandLine.print(out,bwOut,"Writing model in file "+fileName);
		}

		createParentDirectories(fileName);
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printCRNToCRNFile, exception raised while creating the filewriter for file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}

		try {

			if(preambleCommentLines!=null && preambleCommentLines.size()>0){
				for (String comment : preambleCommentLines) {
					bw.write("#"+comment+"\n");
				}
				bw.write("\n\n");
			}

			bw.write("begin CRN\n");

			if(crn.getParameters()!=null && crn.getParameters().size()>0){
				bw.write(" #The optional list of parameters\n");
				bw.write(" begin parameters\n");
				for(String parameterDefinition : crn.getParameters()){
					String[] p=parameterDefinition.split("\\ ");
					//bw.write("  "+parameterDefinition+"\n");
					bw.write("  "+p[0]+" = "+p[1]+"\n");
				}
				bw.write(" end parameters\n\n");
			}
			
			if(writeSpecies){
				bw.write(" #The optional list of species\n");
				bw.write(" begin species\n");
				for (ISpecies species : crn.getSpecies()) {
					if(compactNames){
						bw.write("   s"+species.getID()+"\n");
					}
					else{
						bw.write("  "+species.getName()+"\n");
					}
					
				}
				bw.write(" end species\n\n");
			}

			bw.write(" #The mandatory list of reactions: reagents -> products , rate. Where rate can be any arithmetic expression, possibly using the parameters or numbers\n");
			bw.write(" begin reactions\n");
			for (ICRNReaction crnReaction : crn.getReactions()) {
				bw.write("  "+toStringCRNFileFormat(crnReaction,compactNames)+"\n");
			}
			bw.write(" end reactions\n\n");

			bw.write(" #The optional list of initial concentration/population of each species. Real initial concentrations are converted to the nearest natural if performing stochastic simulations. Non listed species have assigned 0 as initial concentration.\n");
			if(icComment!=null && !icComment.equals("")){
				bw.write(" #"+icComment+"\n");
			}
			bw.write(" begin initialConcentrations\n");
			for (ISpecies species : crn.getSpecies()) {
				//double ic = species.getInitialConcentration().doubleValue();
				double initialConcentration;
				//String initialConcentrationExpr;
				if(assignPopulationOfRepresentative){
					initialConcentration = partition.getBlockOf(species).getRepresentative(CRNReducerCommandLine.COMPUTEREPRESENTATIVEBYMINOUTSIDEPARTITIONREFINEMENT).getInitialConcentration().doubleValue();
					//initialConcentrationExpr=partition.getBlockOf(species).getRepresentative().getInitialConcentrationExpr();
					//initialConcentration=BigDecimal.valueOf(10000);
				}
				else{
					initialConcentration = species.getInitialConcentration().doubleValue();
					//initialConcentrationExpr=species.getInitialConcentrationExpr();
					//initialConcentration=BigDecimal.valueOf(10000);
				}
				//if(!(initialConcentrationExpr.equals("0")||initialConcentrationExpr.equals("0.0"))){
				if(initialConcentration!=0){
					bw.write("  "+species.getName() + " "+ initialConcentration+"\n");
				}
			}
			bw.write(" end initialConcentrations\n\n");


			if(groupAccordingToCurrentPartition){
				writeCommentLine(bw," The optional list of views â€œviewName viewExpressionâ€�, used to specify the required output: this can be used to specify a subset of the species, or combinations (via an arithmetic expression) of the concentrations of the species.","#");
				bw.write(" begin views\n");
				IBlock currentBlock = partition.getFirstBlock();
				while(currentBlock!=null){
					bw.write("  " + currentBlock.getRepresentative(CRNReducerCommandLine.COMPUTEREPRESENTATIVEBYMINOUTSIDEPARTITIONREFINEMENT).getName());
					bw.write(" " + currentBlock.toStringSeparatedSpeciesNames("+")+"\n");
					currentBlock=currentBlock.getNext();
				}
				bw.write(" end views\n\n");
			}
			else{
				if(crn.getViewNames()!=null && crn.getViewNames().length>0){
					bw.write(" #The optional list of views â€œviewName viewExpressionâ€�, used to specify the required output: this can be used to specify a subset of the species, or combinations (via an arithmetic expression) of the concentrations of the species.\n");
					bw.write(" begin views\n");
					for(int i=0;i<crn.getViewNames().length;i++){
						bw.write("  "+crn.getViewNames()[i]);
						bw.write(" "+crn.getViewExpressions()[i]+"\n");
					}
					bw.write(" end views\n\n");
				}
			}

			boolean printedBeginEndComments = false;
			for (ISpecies species : crn.getSpecies()) {
				if(species.getComments()!=null && species.getComments().size()>0){
					boolean speciesWritten=false;
					bw.write("#"+species.getName()+":  \n");
					for(String comment : species.getComments()){
						if(!comment.equals("")){
							if(!printedBeginEndComments){
								printedBeginEndComments=true;
								//writeCommentLine(br, " Comments associated to the species");
								//br.write(" #Comments associated to the species\n");
								//br.write("begin comments\n");
								bw.write("#Comments associated to the species\n");
							}
							if(!speciesWritten){
								bw.write(" #"+species.getName()+":  \n");
								speciesWritten=true;
							}
							
							bw.write("  #"+comment+"\n");
						}
					}
				}
			}
			/*if(printedBeginEndComments){
				br.write("end comments\n\n");
			}*/

			GUICRNImporter.writeOriginalNames(crn, bw,"#");
			
			bw.write("end CRN\n\n");

			if(PRINTCOMMANDSASCOMMENT){
				writeSampleCommands(bw);
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
				bw.close();
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in printCRNToCRNFile, exception raised while closing the bufferedwriter of the file: "+fileName);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}

	}

	public static void printPartition(ICRN crn,IPartition partition, String name,boolean verbose, String reduction,MessageConsoleStream out,BufferedWriter bwOut, IPartition partitionOfParams){
		printPartition(crn.getName(), partition, name, verbose, reduction, out, bwOut,partitionOfParams);
	}
	
	public static void printPartition(String crnName,IPartition partition, String name,boolean verbose, String reduction,MessageConsoleStream out,BufferedWriter bwOut, IPartition partitionOfParams){
		String fileName = name;
		fileName=overwriteExtensionIfEnabled(fileName,".txt");
		if(verbose){
			CRNReducerCommandLine.print(out,bwOut,"Writing the computed partition in file "+fileName);
		}

		createParentDirectories(fileName);
		BufferedWriter br;
		try {
			br = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printPartition, exception raised while creating the filewriter for file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}

		try {
			br.write("This is the " + reduction + " partition computed for the model "+crnName+"\n\n");
			if(partitionOfParams!=null) {
				br.write("\nPartition of species\n");
			}
			//br.write("The partition has " + partition.size() + " blocks out of "+crn.getSpecies().size()+" species.\n");
			br.write(partition.toString());
			
			if(partitionOfParams!=null) {
				br.write("\nPartition of params\n");
				br.write(partitionOfParams.toString());
			}
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printPartition, exception raised while writing in the file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		finally{
			if(verbose){
				CRNReducerCommandLine.println(out,bwOut,"Writing the partition in file "+fileName+" completed");
			}
			try {
				br.close();
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in printPartition, exception raised while closing the bufferedwriter of the file: "+fileName);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}

	}



	private static List<String> getSampleCommands(boolean detailed){
		if(detailed){
			if(sampleCommands==null){
				sampleCommands=new ArrayList<String>();
			}
			else {
				return sampleCommands;
			}
		}
		else {
			if(sampleCommandsShort==null){
				sampleCommandsShort=new ArrayList<String>();
			}
			else {
				return sampleCommandsShort;
			}
		} 

		String suffix="";
		if(!detailed){
			suffix="\n";
		}
		List<String> toBeFilled;
		if(detailed){
			toBeFilled=sampleCommands;
		}
		else{
			toBeFilled=sampleCommandsShort;
		}

		toBeFilled.add("CRNReducer is a command line tool.");
		toBeFilled.add("Invoke it with \"java -jar CRNReducerVERSION.jar -c commands.crn [-i]\", where commands.crn is a file containing a list of commands exemplified below.");
		toBeFilled.add("Also, if the -i option is provided, the tool starts in interactive mode, allowing the user to provide commands from the console.\n");
		toBeFilled.add("Some sample commands:");

		toBeFilled.add(" To have a short help.");
		toBeFilled.add(" -h or --help\n");

		toBeFilled.add(" To have a detailed description of CRNReducer's commands and featuresshort help CRNReducer.");
		toBeFilled.add(" -m or --man\n");

		toBeFilled.add(" Print minimal information about the current CRN.");
		toBeFilled.add(" print()\n");

		toBeFilled.add(" Quit CRNReducer if in interactive mode.");
		toBeFilled.add(" quit()\n");

		toBeFilled.add(" Load a(nother) .crn file, which can be a CRN or a sequence of commands.");
		toBeFilled.add(" load({fileIn=>./CRNNetworks/am.crn})"+suffix);
		if(detailed){
			toBeFilled.add("  [fileIn] Mandatory. The file to be loaded.\n");
		}

		toBeFilled.add(" Write a CRN in a .crn file."); 
		toBeFilled.add(" write({fileIn=>inputfileName.crn,fileOut=>outputfileName})"+suffix);
		if(detailed){
			toBeFilled.add("  [fileOut] Mandatory. The name of the file where to write the current CRN.");
			toBeFilled.add("  [fileIn] Optional. If provided, the file is first loaded, and the operation is performed on the resulting CRN. Otherwise, the operation is performed on the current CRN.\n");
		}

		toBeFilled.add(" Import a CRN from a .net file (supported by the BioNteGen tool).");
		toBeFilled.add(" importBNG({fileIn=>./BNGNetworks/mmc1.net})"+suffix);
		if(detailed){
			toBeFilled.add("  [fileIn] Mandatory. The file to be loaded.\n");
		}

		toBeFilled.add(" Export a CRN in a .net file  (supported by the BioNteGen tool)."); 
		toBeFilled.add(" exportBNG({fileIn=>inputfileName.crn,fileOut=>outputfileName})"+suffix);
		if(detailed){
			toBeFilled.add("  [fileOut] Mandatory. The name of the file where to write the current CRN.");
			toBeFilled.add("  [fileIn] Optional. If provided, the file is first loaded, and the operation is performed on the resulting CRN. Otherwise, the operation is performed on the current CRN.\n");
		}

		toBeFilled.add(" Import a CRN from an SBML file representing the BoolCube ODE system of a boolean circuit network (the SBML file is assume to be generated by ODIFY).");
		toBeFilled.add(" importBoolCubeSBML({fileIn=>./BoolCubeSBMLNetworks/feedForwardBooleCube.sbml})"+suffix);
		if(detailed){
			toBeFilled.add("  [fileIn] Mandatory. The file to be loaded.\n");
		}

		toBeFilled.add(" Import a CRN by encoding a system of polynomial differential equations written in Matlab.");
		toBeFilled.add(" importMatlabODEs({fileIn=>./Matlab/model.m,polynomialODEs=>true})"+suffix);
		if(detailed){
			toBeFilled.add("  [fileIn] Mandatory. The file to be loaded.\n");
			toBeFilled.add("  [polynomialODEs] Optional. Specifies if the ODEs are actually polynomial ODEs, that is omposed by sums of products of doubles and variables. These ODEs will then be transformed in mass-action reaction networks, for which efficient partition refinement algorithms are implemented.\n");
		}

		toBeFilled.add(" Import a CRN from a .inp file (ChemKin format).");
		toBeFilled.add(" importChemKin({fileIn=>./ChemKinNetworks/example1.inp,thermoDynamicFile=>./ChemKinNetworks/example1.dat})"+suffix);
		if(detailed){
			toBeFilled.add("  [fileIn] Mandatory. The file to be loaded.\n");
			toBeFilled.add("  [thermoDynamicFile] Optional. If provided, it contains the name of the file containing thermodynamic data. If not provided, fileIn is used (after replacing .inp or .CKI with .dat)\n");
		}

		toBeFilled.add(" Import a CRN from a .tra file (MRMC format).");
		toBeFilled.add(" importMRMC({fileIn=>./MRMCMarkovChains/example1.tra,labellingFile=>./MRMCMarkovChains/example1.lab})"+suffix);
		if(detailed){
			toBeFilled.add("  [fileIn] Mandatory. The file to be loaded.\n");
			toBeFilled.add("  [labellingFile] Optional. If provided, it contains the name of the file containing the labelling function of the Markov chain. If the value \"same\" is provided, then fileIn is used (after replacing .tra with .label). Such information will be encoded as views, which can in turn be used to prepartition the initial partition\n");
		}

		toBeFilled.add(" Import a CRN from a matrix in a .csv file (assuming it is a linear system).");
		toBeFilled.add(" importLinearSystemAsCSVMatrix({fileIn=>./Matrices/example1.csv})"+suffix);
		if(detailed){
			toBeFilled.add("  [fileIn] Mandatory. The file to be loaded.\n");
		}

		toBeFilled.add(" Import a CRN from a matrix in a compact .csv file (assuming it is a linear system). ");
		toBeFilled.add(" importLinearSystemAsCCSVMatrix({fileIn=>./CompactMatrices/example1.csv,form=>PQ,normalize=>true})"+suffix);
		if(detailed){
			toBeFilled.add("  [fileIn] Mandatory. The file to be loaded.\n");
			toBeFilled.add("  [form] Mandatory. Can be either A*X (linear systems), P*Q (Markov chain) or FEM (finite element methods).\n");
			toBeFilled.add("  [normalize] Optional. If provided, the outgoing rate from each species is normalized to one.\n");
		}


		toBeFilled.add(" Import a CRN from an LBS file.");
		toBeFilled.add(" importLBS({fileIn=>./LBSNetworks/network1.lbs})"+suffix);
		if(detailed){
			toBeFilled.add("  [fileIn] Mandatory. The file to be loaded.\n");
		}

		toBeFilled.add(" Export a CRN in a LBS file."); 
		toBeFilled.add(" exportLBS({fileIn=>inputfileName.crn,fileOut=>outputfileName})"+suffix);
		if(detailed){
			toBeFilled.add("  [fileOut] Mandatory. The name of the file where to write the current CRN.");
			toBeFilled.add("  [fileIn] Optional. If provided, the file is first loaded, and the operation is performed on the resulting CRN. Otherwise, the operation is performed on the current CRN.\n");
		}

		toBeFilled.add(" Import a CRN from a .bngl file, treating it as a raw crn.");
		toBeFilled.add(" importBNGL({fileIn=>./BNGLNetworks/network1.bngl})"+suffix);
		if(detailed){
			toBeFilled.add("  [fileIn] Mandatory. The file to be loaded.\n");
		}

		toBeFilled.add(" Export a CRN in a .bngl file, treating it as a raw crn."); 
		toBeFilled.add(" exportBNGL({fileIn=>inputfileName.crn,fileOut=>outputfileName})"+suffix);
		if(detailed){
			toBeFilled.add("  [fileOut] Mandatory. The name of the file where to write the current CRN.");
			toBeFilled.add("  [fileIn] Optional. If provided, the file is first loaded, and the operation is performed on the resulting CRN. Otherwise, the operation is performed on the current CRN.\n");
		}

		toBeFilled.add(" Export a CRN in a .maude file, to be given in input to the fluid compiler."); 
		toBeFilled.add(" exportFluidCompiler({fileIn=>inputfileName.crn,fileOut=>outputfileName})"+suffix);
		if(detailed){
			toBeFilled.add("  [fileOut] Mandatory. The name of the file where to write the current CRN.");
			toBeFilled.add("  [fileIn] Optional. If provided, the file is first loaded, and the operation is performed on the resulting CRN. Otherwise, the operation is performed on the current CRN.\n");
		}

		toBeFilled.add(" Export a CRN and its partition in a z3, given a question: is OFL or EFL, there exists a rate assignemnt such that it is OFL or EFL? (OFL,EFL,EROFL,EREFL)."); 
		toBeFilled.add(" exportZ3({fileIn=>inputfileName.crn,fileOut=>outputfileName,question=>OFL})"+suffix);
		if(detailed){
			toBeFilled.add("  [fileOut] Mandatory. The name of the file where to write the current CRN.");
			toBeFilled.add("  [question] Mandatory. The condition to be checked on the current partition.");
			toBeFilled.add("  [fileIn] Optional. If provided, the file is first loaded, and the operation is performed on the resulting CRN. Otherwise, the operation is performed on the current CRN.\n");
		}

		toBeFilled.add(" Export a CRN in a SBML file."); 
		toBeFilled.add(" exportSBML({fileIn=>inputfileName.crn,fileOut=>outputfileName})"+suffix);
		if(detailed){
			toBeFilled.add("  [fileOut] Mandatory. The name of the file where to write the current CRN.");
			toBeFilled.add("  [fileIn] Optional. If provided, the file is first loaded, and the operation is performed on the resulting CRN. Otherwise, the operation is performed on the current CRN.\n");
		}

		toBeFilled.add(" Export a CRN in a FlyFast file."); 
		toBeFilled.add(" exportFlyFast({fileIn=>inputfileName.crn,fileOut=>outputfileName})"+suffix);
		if(detailed){
			toBeFilled.add("  [fileOut] Mandatory. The name of the file where to write the current CRN.");
			toBeFilled.add("  [fileIn] Optional. If provided, the file is first loaded, and the operation is performed on the resulting CRN. Otherwise, the operation is performed on the current CRN.\n");
		}

		toBeFilled.add(" [Deprecated. Superceded by reduceFB] Reduce a CRN using Differential Species Bisimulation. If it is prefixed by \"this=\", then the loaded CRN is updated to the obtained one.");
		//toBeFilled.add(" reduce({fileIn=>inputfileName.net,technique=>dsb,reducedFile=>outputFileNameOfReducedCRN.net,groupedFile=>outputFileNameOfGroupedCRN.net,sameICFile=>outputFileNameOfCRNWithSameICPerBlock.net})");
		toBeFilled.add(" reduceDSB({fileIn=>inputfileName.crn,reducedFile=>outputFileNameOfReducedCRN.crn,prePartition=>NONE,groupedFile=>outputFileNameOfGroupedCRN.crn,sameICFile=>outputFileNameOfCRNWithSameICPerBlock.crn,computeOnlyPartition=>false,fileWhereToStorePartition=>outputFileToStorePartition.txt,print=>true})"+suffix);
		if(detailed){
			toBeFilled.add("  [prePartition] Optional (default value=NONE). If set to IC, the initial partition is first refined in blocks of species with same initial concentrantions. If set to VIEWS, the initial partition is coherent with the specified groups/views in the original file (if they correspond to a partition, i.e. views are just disjoint sums of distinct species)");
			toBeFilled.add("  [reducedFile] Optional. If this parameter is specified, the reduced CRN is written in the file with the provided name.");
			toBeFilled.add("  [groupedFile] Optional. If this parameter is specified, the original CRN is written in the file with the provided name. Also, a view per block of the obtained partition is created, grouping (i.e., summing) all species belonging to the block.");
			toBeFilled.add("  [typeOfGroupedFile] Optional. This is the format in which the grouped file will be written. It can currently be either CRN, z3 or FluidCompiler (for the Maude tool FluidCompiler). If this parameter is not specified, the CRN format is assumed. If z3 is specified, then the question is implicitly assumed to be OFL");
			toBeFilled.add("  [sameICFile] Optional. If this parameter is specified, the original CRN is written in the file with the provided name. However, the same population is assigned to all the species of each block of the obtained partition.");
			toBeFilled.add("  [fileIn] Optional. If provided, the file is first loaded, and the operation is performed on the resulting CRN. Otherwise, the operation is performed on the current CRN.");
			toBeFilled.add("  [computeOnlyPartition] Optional (default value=false). If true, the reduced CRN is not actually computed, but only the corasest DSB partition.\n");
			toBeFilled.add("  [fileWhereToStorePartition] Optional. If specified, information on the computed partition will be stored in the given text file.\n");
			toBeFilled.add("  [print] Optional (default value=true). If set to true, some information is print in the console.\n");
		}

		toBeFilled.add(" Reduce a CRN using Forward CRN Bisimulation. If it is prefixed by \"this=\", then the loaded CRN is updated to the obtained one.");
		//toBeFilled.add(" reduce({fileIn=>inputfileName.net,technique=>dsb,reducedFile=>outputFileNameOfReducedCRN.net,groupedFile=>outputFileNameOfGroupedCRN.net,sameICFile=>outputFileNameOfCRNWithSameICPerBlock.net})");
		toBeFilled.add(" reduceFB({fileIn=>inputfileName.crn,reducedFile=>outputFileNameOfReducedCRN.crn,prePartition=>NONE,groupedFile=>outputFileNameOfGroupedCRN.crn,sameICFile=>outputFileNameOfCRNWithSameICPerBlock.crn,computeOnlyPartition=>false,fileWhereToStorePartition=>outputFileToStorePartition.txt,print=>true})"+suffix);
		if(detailed){
			toBeFilled.add("  [prePartition] Optional (default value=NONE). If set to IC, the initial partition is first refined in blocks of species with same initial concentrantions. If set to VIEWS, the initial partition is coherent with the specified groups/views in the original file (if they correspond to a partition, i.e. views are just disjoint sums of distinct species)");
			toBeFilled.add("  [reducedFile] Optional. If this parameter is specified, the reduced CRN is written in the file with the provided name.");
			toBeFilled.add("  [groupedFile] Optional. If this parameter is specified, the original CRN is written in the file with the provided name. Also, a view per block of the obtained partition is created, grouping (i.e., summing) all species belonging to the block.");
			toBeFilled.add("  [typeOfGroupedFile] Optional. This is the format in which the grouped file will be written. It can currently be either CRN, z3 or FluidCompiler (for the Maude tool FluidCompiler). If this parameter is not specified, the CRN format is assumed. If z3 is specified, then the question is implicitly assumed to be OFL");
			toBeFilled.add("  [sameICFile] Optional. If this parameter is specified, the original CRN is written in the file with the provided name. However, the same population is assigned to all the species of each block of the obtained partition.");
			toBeFilled.add("  [fileIn] Optional. If provided, the file is first loaded, and the operation is performed on the resulting CRN. Otherwise, the operation is performed on the current CRN.");
			toBeFilled.add("  [computeOnlyPartition] Optional (default value=false). If true, the reduced CRN is not actually computed, but only the corasest DSB partition.\n");
			toBeFilled.add("  [fileWhereToStorePartition] Optional. If specified, information on the computed partition will be stored in the given text file.\n");
			toBeFilled.add("  [print] Optional (default value=true). If set to true, some information is print in the console.\n");
		}

		toBeFilled.add(" Reduce a CRN using N-ary Forward CRN Bisimulation. If it is prefixed by \"this=\", then the loaded CRN is updated to the obtained one.");
		//toBeFilled.add(" reduce({fileIn=>inputfileName.net,technique=>dsb,reducedFile=>outputFileNameOfReducedCRN.net,groupedFile=>outputFileNameOfGroupedCRN.net,sameICFile=>outputFileNameOfCRNWithSameICPerBlock.net})");
		toBeFilled.add(" reduceFE({fileIn=>inputfileName.crn,reducedFile=>outputFileNameOfReducedCRN.crn,prePartition=>NONE,groupedFile=>outputFileNameOfGroupedCRN.crn,sameICFile=>outputFileNameOfCRNWithSameICPerBlock.crn,computeOnlyPartition=>false,fileWhereToStorePartition=>outputFileToStorePartition.txt,print=>true})"+suffix);
		if(detailed){
			toBeFilled.add("  [prePartition] Optional (default value=NONE). If set to IC, the initial partition is first refined in blocks of species with same initial concentrantions. If set to VIEWS, the initial partition is coherent with the specified groups/views in the original file (if they correspond to a partition, i.e. views are just disjoint sums of distinct species)");
			toBeFilled.add("  [reducedFile] Optional. If this parameter is specified, the reduced CRN is written in the file with the provided name.");
			toBeFilled.add("  [groupedFile] Optional. If this parameter is specified, the original CRN is written in the file with the provided name. Also, a view per block of the obtained partition is created, grouping (i.e., summing) all species belonging to the block.");
			toBeFilled.add("  [typeOfGroupedFile] Optional. This is the format in which the grouped file will be written. It can currently be either CRN, z3 or FluidCompiler (for the Maude tool FluidCompiler). If this parameter is not specified, the CRN format is assumed. If z3 is specified, then the question is implicitly assumed to be OFL");
			toBeFilled.add("  [sameICFile] Optional. If this parameter is specified, the original CRN is written in the file with the provided name. However, the same population is assigned to all the species of each block of the obtained partition.");
			toBeFilled.add("  [fileIn] Optional. If provided, the file is first loaded, and the operation is performed on the resulting CRN. Otherwise, the operation is performed on the current CRN.");
			toBeFilled.add("  [computeOnlyPartition] Optional (default value=false). If true, the reduced CRN is not actually computed, but only the corasest DSB partition.\n");
			toBeFilled.add("  [fileWhereToStorePartition] Optional. If specified, information on the computed partition will be stored in the given text file.\n");
			toBeFilled.add("  [print] Optional (default value=true). If set to true, some information is print in the console.\n");
		}

		toBeFilled.add(" Reduce a CRN using Weak Forward Bisimulation. If it is prefixed by \"this=\", then the loaded CRN is updated to the obtained one.");
		//toBeFilled.add(" reduce({fileIn=>inputfileName.net,technique=>dsb,reducedFile=>outputFileNameOfReducedCRN.net,groupedFile=>outputFileNameOfGroupedCRN.net,sameICFile=>outputFileNameOfCRNWithSameICPerBlock.net})");
		//toBeFilled.add(" reduceWFB({fileIn=>inputfileName.crn,reducedFile=>outputFileNameOfReducedCRN.crn,groupedFile=>outputFileNameOfGroupedCRN.crn,sameICFile=>outputFileNameOfCRNWithSameICPerBlock.crn,computeOnlyPartition=>false,fileWhereToStorePartition=>outputFileToStorePartition.txt})"+suffix);
		toBeFilled.add(" reduceWFB({fileIn=>inputfileName.crn,reducedFile=>outputFileNameOfReducedCRN.crn,prePartition=>NONE,groupedFile=>outputFileNameOfGroupedCRN.crn,sameICFile=>outputFileNameOfCRNWithSameICPerBlock.crn,computeOnlyPartition=>false,fileWhereToStorePartition=>outputFileToStorePartition.txt,print=>true})"+suffix);
		if(detailed){
			toBeFilled.add("  [prePartition] Optional (default value=NONE). If set to IC, the initial partition is first refined in blocks of species with same initial concentrantions. If set to VIEWS, the initial partition is coherent with the specified groups/views in the original file (if they correspond to a partition, i.e. views are just disjoint sums of distinct species)");
			toBeFilled.add("  [reducedFile] Optional. If this parameter is specified, the reduced CRN is written in the file with the provided name.");
			toBeFilled.add("  [groupedFile] Optional. If this parameter is specified, the original CRN is written in the file with the provided name. Also, a view per block of the obtained partition is created, grouping (i.e., summing) all species belonging to the block.");
			toBeFilled.add("  [typeOfGroupedFile] Optional. This is the format in which the grouped file will be written. It can currently be either CRN, z3 or FluidCompiler (for the Maude tool FluidCompiler). If this parameter is not specified, the CRN format is assumed. If z3 is specified, then the question is implicitly assumed to be OFL");
			toBeFilled.add("  [sameICFile] Optional. If this parameter is specified, the original CRN is written in the file with the provided name. However, the same population is assigned to all the species of each block of the obtained partition.");
			toBeFilled.add("  [fileIn] Optional. If provided, the file is first loaded, and the operation is performed on the resulting CRN. Otherwise, the operation is performed on the current CRN.");
			toBeFilled.add("  [computeOnlyPartition] Optional (default value=false). If true, the reduced CRN is not actually computed, but only the corasest DSB partition.\n");
			toBeFilled.add("  [fileWhereToStorePartition] Optional. If specified, information on the computed partition will be stored in the given text file.\n");
			toBeFilled.add("  [print] Optional (default value=true). If set to true, some information is print in the console.\n");
		}

		toBeFilled.add(" Reduce a CRN using Syntactic Markovian Bisimulation. If it is prefixed by \"this=\", then the loaded CRN is updated to the obtained one.");
		toBeFilled.add(" reduceSMB({fileIn=>inputfileName.crn,reducedFile=>outputFileNameOfReducedCRN.crn,prePartition=>NONE,groupedFile=>outputFileNameOfGroupedCRN.crn,sameICFile=>outputFileNameOfCRNWithSameICPerBlock.crn,computeOnlyPartition=>false,fileWhereToStorePartition=>outputFileToStorePartition.txt,print=>true})"+suffix);
		if(detailed){
			toBeFilled.add("  [prePartition] Optional (default value=NONE). If set to IC, the initial partition is first refined in blocks of species with same initial concentrantions. If set to VIEWS, the initial partition is coherent with the specified groups/views in the original file (if they correspond to a partition, i.e. views are just disjoint sums of distinct species)");
			toBeFilled.add("  [reducedFile] Optional. If this parameter is specified, the reduced CRN is written in the file with the provided name.");
			toBeFilled.add("  [groupedFile] Optional. If this parameter is specified, the original CRN is written in the file with the provided name. Also, a view per block of the obtained partition is created, grouping (i.e., summing) all species belonging to the block.");
			toBeFilled.add("  [sameICFile] Optional. If this parameter is specified, the original CRN is written in the file with the provided name. However, the same population is assigned to all the species of each block of the obtained partition.");
			toBeFilled.add("  [fileIn] Optional. If provided, the file is first loaded, and the operation is performed on the resulting CRN. Otherwise, the operation is performed on the current CRN.");
			toBeFilled.add("  [computeOnlyPartition] Optional (default value=false). If true, the reduced CRN is not actually computed, but only the corasest SMB partition.\n");
			toBeFilled.add("  [fileWhereToStorePartition] Optional. If specified, information on the computed partition will be stored in the given text file.\n");
			toBeFilled.add("  [print] Optional (default value=true). If set to true, some information is print in the console.\n");
		}

		toBeFilled.add(" [Deprecated. Superceded by reduceBB] Reduce a CRN using Exact Fluid Lumpability. If it is prefixed by \"this=\", then the loaded CRN is updated to the obtained one.");
		toBeFilled.add(" reduceEFL({fileIn=>inputfileName.crn,prePartition=>NONE,reducedFile=>outputFileNameOfReducedCRN.crn,groupedFile=>outputFileNameOfGroupedCRN.crn,sameICFile=>outputFileNameOfCRNWithSameICPerBlock.crn,computeOnlyPartition=>false,fileWhereToStorePartition=>outputFileToStorePartition.txt,print=>true,print=>true})"+suffix);
		if(detailed){
			toBeFilled.add("  [prePartition] Optional (default value=IC). If set to IC, the initial partition is first refined in blocks of species with same initial concentrantions. If set to VIEWS, the initial partition is coherent with the specified groups/views in the original file (if they correspond to a partition, i.e. views are just disjoint sums of distinct species)");
			//toBeFilled.add("  [icPrePartitioning] Optional (default value=false). If set to true, the initial partition is first refined in blocks of species with same initial concentrantions.");
			toBeFilled.add("  [reducedFile] Optional. If this parameter is specified, the reduced CRN is written in the file with the provided name.");
			toBeFilled.add("  [typeOfGroupedFile] Optional. This is the format in which the grouped file will be written. It can currently be either CRN, z3 or FluidCompiler (for the Maude tool FluidCompiler). If this parameter is not specified, the CRN format is assumed. If z3 is specified, then the question is implicitly assumed to be EFL");
			toBeFilled.add("  [groupedFile] Optional. If this parameter is specified, the original CRN is written in the file with the provided name. Also, a view per block of the obtained partition is created, grouping (i.e., summing) all species belonging to the block.");
			toBeFilled.add("  [sameICFile] Optional. If this parameter is specified, the original CRN is written in the file with the provided name. However, the same population is assigned to all the species of each block of the obtained partition.");
			toBeFilled.add("  [fileIn] Optional. If provided, the file is first loaded, and the operation is performed on the resulting CRN. Otherwise, the operation is performed on the current CRN.");
			toBeFilled.add("  [computeOnlyPartition] Optional (default value=false). If true, the reduced CRN is not actually computed, but only the corasest EFL partition.\n");
			toBeFilled.add("  [fileWhereToStorePartition] Optional. If specified, information on the computed partition will be stored in the given text file.\n");
			toBeFilled.add("  [print] Optional (default value=true). If set to true, some information is print in the console.\n");
		}

		toBeFilled.add(" Reduce a CRN using Backward Bisimulation. If it is prefixed by \"this=\", then the loaded CRN is updated to the obtained one.");
		toBeFilled.add(" reduceBB({fileIn=>inputfileName.crn,prePartition=>NONE,reducedFile=>outputFileNameOfReducedCRN.crn,groupedFile=>outputFileNameOfGroupedCRN.crn,sameICFile=>outputFileNameOfCRNWithSameICPerBlock.crn,computeOnlyPartition=>false,fileWhereToStorePartition=>outputFileToStorePartition.txt,print=>true})"+suffix);
		if(detailed){
			toBeFilled.add("  [prePartition] Optional (default value=IC). If set to IC, the initial partition is first refined in blocks of species with same initial concentrantions. If set to VIEWS, the initial partition is coherent with the specified groups/views in the original file (if they correspond to a partition, i.e. views are just disjoint sums of distinct species)");
			//toBeFilled.add("  [icPrePartitioning] Optional (default value=false). If set to true, the initial partition is first refined in blocks of species with same initial concentrantions.");
			toBeFilled.add("  [reducedFile] Optional. If this parameter is specified, the reduced CRN is written in the file with the provided name.");
			toBeFilled.add("  [typeOfGroupedFile] Optional. This is the format in which the grouped file will be written. It can currently be either CRN, z3 or FluidCompiler (for the Maude tool FluidCompiler). If this parameter is not specified, the CRN format is assumed. If z3 is specified, then the question is implicitly assumed to be EFL");
			toBeFilled.add("  [groupedFile] Optional. If this parameter is specified, the original CRN is written in the file with the provided name. Also, a view per block of the obtained partition is created, grouping (i.e., summing) all species belonging to the block.");
			toBeFilled.add("  [sameICFile] Optional. If this parameter is specified, the original CRN is written in the file with the provided name. However, the same population is assigned to all the species of each block of the obtained partition.");
			toBeFilled.add("  [fileIn] Optional. If provided, the file is first loaded, and the operation is performed on the resulting CRN. Otherwise, the operation is performed on the current CRN.");
			toBeFilled.add("  [computeOnlyPartition] Optional (default value=false). If true, the reduced CRN is not actually computed, but only the corasest EFL partition.\n");
			toBeFilled.add("  [fileWhereToStorePartition] Optional. If specified, information on the computed partition will be stored in the given text file.\n");
			toBeFilled.add("  [print] Optional (default value=true). If set to true, some information is print in the console.\n");
		}

		toBeFilled.add(" Reduce a CRN using Backward Bisimulation. If it is prefixed by \"this=\", then the loaded CRN is updated to the obtained one.");
		toBeFilled.add(" reduceBE({fileIn=>inputfileName.crn,prePartition=>NONE,reducedFile=>outputFileNameOfReducedCRN.crn,groupedFile=>outputFileNameOfGroupedCRN.crn,sameICFile=>outputFileNameOfCRNWithSameICPerBlock.crn,computeOnlyPartition=>false,fileWhereToStorePartition=>outputFileToStorePartition.txt,print=>true})"+suffix);
		if(detailed){
			toBeFilled.add("  [prePartition] Optional (default value=IC). If set to IC, the initial partition is first refined in blocks of species with same initial concentrantions. If set to VIEWS, the initial partition is coherent with the specified groups/views in the original file (if they correspond to a partition, i.e. views are just disjoint sums of distinct species)");
			//toBeFilled.add("  [icPrePartitioning] Optional (default value=false). If set to true, the initial partition is first refined in blocks of species with same initial concentrantions.");
			toBeFilled.add("  [reducedFile] Optional. If this parameter is specified, the reduced CRN is written in the file with the provided name.");
			toBeFilled.add("  [typeOfGroupedFile] Optional. This is the format in which the grouped file will be written. It can currently be either CRN, z3 or FluidCompiler (for the Maude tool FluidCompiler). If this parameter is not specified, the CRN format is assumed. If z3 is specified, then the question is implicitly assumed to be EFL");
			toBeFilled.add("  [groupedFile] Optional. If this parameter is specified, the original CRN is written in the file with the provided name. Also, a view per block of the obtained partition is created, grouping (i.e., summing) all species belonging to the block.");
			toBeFilled.add("  [sameICFile] Optional. If this parameter is specified, the original CRN is written in the file with the provided name. However, the same population is assigned to all the species of each block of the obtained partition.");
			toBeFilled.add("  [fileIn] Optional. If provided, the file is first loaded, and the operation is performed on the resulting CRN. Otherwise, the operation is performed on the current CRN.");
			toBeFilled.add("  [computeOnlyPartition] Optional (default value=false). If true, the reduced CRN is not actually computed, but only the corasest EFL partition.\n");
			toBeFilled.add("  [fileWhereToStorePartition] Optional. If specified, information on the computed partition will be stored in the given text file.\n");
			toBeFilled.add("  [print] Optional (default value=true). If set to true, some information is print in the console.\n");
		}

		toBeFilled.add(" Reduce a CRN using Exact Fluid Lumpability. The SMT solver Microsoft z3 is exploited.If it is prefixed by \"this=\", then the loaded CRN is updated to the obtained one.");
		toBeFilled.add(" reduceBDE({fileIn=>inputfileName.crn,prePartition=>NONE,reducedFile=>outputFileNameOfReducedCRN.crn,groupedFile=>outputFileNameOfGroupedCRN.crn,sameICFile=>outputFileNameOfCRNWithSameICPerBlock.crn,computeOnlyPartition=>false,fileWhereToStorePartition=>outputFileToStorePartition.txt,print=>true})"+suffix);
		if(detailed){
			toBeFilled.add("  [prePartition] Optional (default value=IC). If set to IC, the initial partition is first refined in blocks of species with same initial concentrantions. If set to VIEWS, the initial partition is coherent with the specified groups/views in the original file (if they correspond to a partition, i.e. views are just disjoint sums of distinct species)");
			//toBeFilled.add("  [icPrePartitioning] Optional (default value=false). If set to true, the initial partition is first refined in blocks of species with same initial concentrantions.");
			toBeFilled.add("  [reducedFile] Optional. If this parameter is specified, the reduced CRN is written in the file with the provided name.");
			toBeFilled.add("  [typeOfGroupedFile] Optional. This is the format in which the grouped file will be written. It can currently be either CRN, z3 or FluidCompiler (for the Maude tool FluidCompiler). If this parameter is not specified, the CRN format is assumed. If z3 is specified, then the question is implicitly assumed to be BDE");
			toBeFilled.add("  [groupedFile] Optional. If this parameter is specified, the original CRN is written in the file with the provided name. Also, a view per block of the obtained partition is created, grouping (i.e., summing) all species belonging to the block.");
			toBeFilled.add("  [sameICFile] Optional. If this parameter is specified, the original CRN is written in the file with the provided name. However, the same population is assigned to all the species of each block of the obtained partition.");
			toBeFilled.add("  [fileIn] Optional. If provided, the file is first loaded, and the operation is performed on the resulting CRN. Otherwise, the operation is performed on the current CRN.");
			toBeFilled.add("  [computeOnlyPartition] Optional (default value=false). If true, the reduced CRN is not actually computed, but only the corasest BDE partition.\n");
			toBeFilled.add("  [fileWhereToStorePartition] Optional. If specified, information on the computed partition will be stored in the given text file.\n");
			toBeFilled.add("  [print] Optional (default value=true). If set to true, some information is print in the console.\n");
		}

		toBeFilled.add(" Reduce a CRN using Ordinary ODEs Bisimilarity. The SMT solver Microsoft z3 is exploited. If it is prefixed by \"this=\", then the loaded CRN is updated to the obtained one.");
		toBeFilled.add(" reduceFDE({fileIn=>inputfileName.crn,prePartition=>NONE,reducedFile=>outputFileNameOfReducedCRN.crn,groupedFile=>outputFileNameOfGroupedCRN.crn,sameICFile=>outputFileNameOfCRNWithSameICPerBlock.crn,computeOnlyPartition=>false,fileWhereToStorePartition=>outputFileToStorePartition.txt,print=>true})"+suffix);
		if(detailed){
			toBeFilled.add("  [prePartition] Optional (default value=NONE). If set to IC, the initial partition is first refined in blocks of species with same initial concentrantions. If set to VIEWS, the initial partition is coherent with the specified groups/views in the original file (if they correspond to a partition, i.e. views are just disjoint sums of distinct species)");
			//toBeFilled.add("  [icPrePartitioning] Optional (default value=false). If set to true, the initial partition is first refined in blocks of species with same initial concentrantions.");
			toBeFilled.add("  [reducedFile] Optional. If this parameter is specified, the reduced CRN is written in the file with the provided name.");
			toBeFilled.add("  [typeOfGroupedFile] Optional. This is the format in which the grouped file will be written. It can currently be either CRN, z3 or FluidCompiler (for the Maude tool FluidCompiler). If this parameter is not specified, the CRN format is assumed. If z3 is specified, then the question is implicitly assumed to be FDE");
			toBeFilled.add("  [groupedFile] Optional. If this parameter is specified, the original CRN is written in the file with the provided name. Also, a view per block of the obtained partition is created, grouping (i.e., summing) all species belonging to the block.");
			toBeFilled.add("  [sameICFile] Optional. If this parameter is specified, the original CRN is written in the file with the provided name. However, the same population is assigned to all the species of each block of the obtained partition.");
			toBeFilled.add("  [fileIn] Optional. If provided, the file is first loaded, and the operation is performed on the resulting CRN. Otherwise, the operation is performed on the current CRN.");
			toBeFilled.add("  [computeOnlyPartition] Optional (default value=false). If true, the reduced CRN is not actually computed, but only the corasest FDE partition.\n");
			toBeFilled.add("  [fileWhereToStorePartition] Optional. If specified, information on the computed partition will be stored in the given text file.\n");
			toBeFilled.add("  [print] Optional (default value=true). If set to true, some information is print in the console.\n");
		}

		toBeFilled.add(" Reduce a CRN using Generalized Exact Fluid Lumpability. The SMT solver Microsoft z3 is exploited. If it is prefixed by \"this=\", then the loaded CRN is updated to the obtained one.");
		toBeFilled.add(" reduceGEFLsmt({fileIn=>inputfileName.crn,prePartition=>NONE,reducedFile=>outputFileNameOfReducedCRN.crn,groupedFile=>outputFileNameOfGroupedCRN.crn,sameICFile=>outputFileNameOfCRNWithSameICPerBlock.crn,computeOnlyPartition=>false,fileWhereToStorePartition=>outputFileToStorePartition.txt,print=>true})"+suffix);
		if(detailed){
			toBeFilled.add("  [prePartition] Optional (default value=NONE). If set to IC, the initial partition is first refined in blocks of species with same initial concentrantions. If set to VIEWS, the initial partition is coherent with the specified groups/views in the original file (if they correspond to a partition, i.e. views are just disjoint sums of distinct species)");
			//toBeFilled.add("  [icPrePartitioning] Optional (default value=false). If set to true, the initial partition is first refined in blocks of species with same initial concentrantions.");
			toBeFilled.add("  [reducedFile] Optional. If this parameter is specified, the reduced CRN is written in the file with the provided name.");
			toBeFilled.add("  [typeOfGroupedFile] Optional. This is the format in which the grouped file will be written. It can currently be either CRN, z3 or FluidCompiler (for the Maude tool FluidCompiler). If this parameter is not specified, the CRN format is assumed. If z3 is specified, then the question is implicitly assumed to be EFL");
			toBeFilled.add("  [groupedFile] Optional. If this parameter is specified, the original CRN is written in the file with the provided name. Also, a view per block of the obtained partition is created, grouping (i.e., summing) all species belonging to the block.");
			toBeFilled.add("  [sameICFile] Optional. If this parameter is specified, the original CRN is written in the file with the provided name. However, the same population is assigned to all the species of each block of the obtained partition.");
			toBeFilled.add("  [fileIn] Optional. If provided, the file is first loaded, and the operation is performed on the resulting CRN. Otherwise, the operation is performed on the current CRN.");
			toBeFilled.add("  [computeOnlyPartition] Optional (default value=false). If true, the reduced CRN is not actually computed, but only the corasest EFL partition.\n");
			toBeFilled.add("  [fileWhereToStorePartition] Optional. If specified, information on the computed partition will be stored in the given text file.\n");
			toBeFilled.add("  [print] Optional (default value=true). If set to true, some information is print in the console.\n");
		}

		toBeFilled.add(" Simulate the ODEs of a CRN. ");
		toBeFilled.add(" simulateODE({fileIn=>inputfileName,tEnd=>200,steps=>10,minStep=>1.0e-8,maxStep=>100.0,absTol=>1.0e-10,relTol=>1.0e-10,visualizePlot=>false,showLabels=>false,csvFile=>fileNameWhereToSaveCSVValues});"+suffix);//,imageFile=>fileNameWhereToPlot
		if(detailed){
			toBeFilled.add("  [tEnd] Mandatory. End time of simulation.");
			toBeFilled.add("  [steps] Optional (default value=1). Number of observed points.");
			toBeFilled.add("  [visualizePlot] Optional (default value=false). If true, an interactive plot is visualized.");
			toBeFilled.add("  [minStep] Optional (default value=1.0e-8). Minimal step (sign is irrelevant), the last step can be smaller than this.");
			toBeFilled.add("  [maxStep] Optional (default value=100.0). Maximal step (sign is irrelevant), the last step can be smaller than this.");
			toBeFilled.add("  [absTol] Optional (default value=1.0e-8). Allowed absolute error.");
			toBeFilled.add("  [relTol] Optional (default value=1.0e-8). Allowed relative error.");
			toBeFilled.add("  [visualizePlot] Optional (default value=true). If true, an interactive plot is visualized.");
			toBeFilled.add("  [showLabels] Optional (default value=true). If true, the labels of the interactive plot will be visualized.");
			//toBeFilled.add("  [imageFile] Optional. The name of the file where to save the plot. The image is saved only if the name is provided, and the interactive plot is visualized.");
			toBeFilled.add("  [csvFile] Optional. The name of the file where to save the plot. The plot is drawn only if the name is provided.");
			toBeFilled.add("  [fileIn] Optional. If provided, the file is first loaded, and the operation is performed on the resulting CRN. Otherwise, the operation is performed on the current CRN.");
			//toBeFilled.add("  [covariances] Optional (default value=false). If set to true, not just the means of the concentrations are computed, but also their variances.\n");
		}

		toBeFilled.add(" Simulate the CTMC of a CRN. ");
		toBeFilled.add(" simulateCTMC({fileIn=>inputfileName,tEnd=>200,steps=>10,method=>ssa,repeats=>100,visualizePlot=>false,showLabels=>false,csvFile=>fileNameWhereToSaveCSVValues})."+suffix);//imageFile=>fileNameWhereToPlot,
		if(detailed){
			toBeFilled.add("  [tEnd] Mandatory. End time of simulation.");
			toBeFilled.add("  [steps] Optional (default value=2). Number of observed points.");
			toBeFilled.add("  [method] Optional (default value=nextReaction) Seven different simulation methods are supported: ");
			toBeFilled.add("   ssa               (direct method by Gillepsie).");
			toBeFilled.add("   ssa+              (uses dependency graphs to improve the runtime of the original Gillepsie algorithm).");
			toBeFilled.add("   nextReaction      (next reaction method by Gibson and Bruck).");
			toBeFilled.add("   tauLeapingAbs     (Tau-leaping algorithm. The error is bounded by the sum of all propensity functions).");
			toBeFilled.add("   tauLeapingRelProp (Tau-leaping algorithm. The error is bounded by the relative change in the individual propensity functions).");
			toBeFilled.add("   tauLeapingRelPop  (Tau-leaping algorithm. The error is bounded by the relative changes in the molecular populations).");
			toBeFilled.add("   maximalTimeStep   (Maximal time step method by Puchalka. Automatic paritioning into slow and fast reactions, which are fired according to an exact and tau leaping method, respectively).");
			toBeFilled.add("  [repeats] Optional (default value=1). The number of simulations to be performed. The output will be the average of the values obtained in the simulations.");
			toBeFilled.add("  [visualizePlot] Optional (default value=true). If true, an interactive plot is visualized.");
			toBeFilled.add("  [showLabels] Optional (default value=true). If true, the labels of the interactive plot will be visualized.");
			//toBeFilled.add("  [imageFile] Optional. The name of the file where to save the plot. The plot is drawn only if the name is provided.");
			toBeFilled.add("  [csvFile] Optional. The name of the file where to save the plot. The plot is drawn only if the name is provided.");
			toBeFilled.add("  [fileIn] Optional. If provided, the file is first loaded, and the operation is performed on the resulting CRN. Otherwise, the operation is performed on the current CRN.\n");
		}

		toBeFilled.add(" Estimate the means of the species populations resorting to the MultiVeStA statistical model checker. ");
		toBeFilled.add(" multivestaSMC({fileIn=>inputfileName,alpha=>0.05,delta=>0.2,maxTime=>200,query=>query.quatex,steps=>10,method=>ssa,parallelism=>2,visualizePlot=>false,showLabels=>false,csvFile=>fileNameWhereToSaveCSVValues})."+suffix);//imageFile=>fileNameWhereToPlot,
		//multivestaSMC({fileIn=>./CRNNetworks/amSameRates.crn,maxTime=>5,steps=>50,parallelism=>2,alpha=>0.05,delta=>0.2,visualizePlot=>true}).
		if(detailed){
			toBeFilled.add("  [fileIn] Mandatory. The .crn file containing the CRN to analyze."); //TODO Why mandatory?
			toBeFilled.add("  [alpha] and [delta] Mandatory. The confidence interval which the estimations must respect: for each estimated mean \"x\", the actual mean belong to x+-delta/2 with probability (1-alpha)*100. ");
			toBeFilled.add("  [maxTime] Mandatory. Maximum simulated time."); //TODO Why mandatory?
			toBeFilled.add("  [query] Optinal (default value=allSpecies). The MultiQuaTEx expression specifying the properties to be estimated. The three keywords \"allSpecies,allViews,allSpeciesAndViews\" trigger the automatic generation of a query studying the mean population of each species, view or both, respectively, at the varying of time.");
			toBeFilled.add("  [steps] Optional (min value=2). Number of observed points. Used to generate the observation points of the three automatically generated queries. It is ognored if an actual query is provided.");
			toBeFilled.add("  [method] Optional (default value=nextReaction) Seven different simulation methods are supported: ");
			toBeFilled.add("   ssa               (direct method by Gillepsie).");
			toBeFilled.add("   ssa+              (uses dependency graphs to improve the runtime of the original Gillepsie algorithm).");
			toBeFilled.add("   nextReaction      (next reaction method by Gibson and Bruck).");
			toBeFilled.add("   tauLeapingAbs     (Tau-leaping algorithm. The error is bounded by the sum of all propensity functions).");
			toBeFilled.add("   tauLeapingRelProp (Tau-leaping algorithm. The error is bounded by the relative change in the individual propensity functions).");
			toBeFilled.add("   tauLeapingRelPop  (Tau-leaping algorithm. The error is bounded by the relative changes in the molecular populations).");
			toBeFilled.add("   maximalTimeStep   (Maximal time step method by Puchalka. Automatic paritioning into slow and fast reactions, which are fired according to an exact and tau leaping method, respectively).");
			toBeFilled.add("  [parallelism] Optional (default value=1). To distribute the simulations in different processes, allowing to exploit Multi-Core architectures.");
			toBeFilled.add("  [visualizePlot] Optional (default value=false). If true, an interactive plot containing the estimated means and variances is visualized.");
			toBeFilled.add("  [showLabels] Optional (default value=false). If true, the labels of the interactive plot will be visualized.");
			//toBeFilled.add("  [imageFile] Optional. The name of the file where to save the plot. The plot is drawn only if the name is provided.");
			toBeFilled.add("  [csvFile] Optional. The name of the file where to save the plot. The plot is drawn only if the name is provided.\n");
		}



		toBeFilled.add(" Set the value of a parameter. Note that parameters are specific to a CRN, and thus this command must be invoked after the loading of a CRN.");
		toBeFilled.add(" setParam({paramName=>p1,expr=>5})"+suffix);
		if(detailed){
			toBeFilled.add("  [paramName] Mandatory. The name of the parameter.");
			toBeFilled.add("  [expr] Mandatory. The new expression to assign to the parameter.\n");
		}

		toBeFilled.add(" Set the initial concentration of a species.");
		toBeFilled.add(" setIC({speciesName=>s1,expr=>5})"+suffix);
		if(detailed){
			toBeFilled.add("  [speciesName] Mandatory. The name of the species.");
			toBeFilled.add("  [expr] Mandatory. The new expression to assign as IC of the species.\n");
		}

		/*toBeFilled.add(" Change the rate of a reaction.");
			toBeFilled.add(" setRate({reaction=>5,expr=>5})");
			toBeFilled.add("  [reaction] Mandatory. The position of the reaction (i.e., 1 if it is the first defined).");
			toBeFilled.add("  [expr] Mandatory. The new expression to assign as rate of the reaction.\n");*/

		return toBeFilled;
	}

	public static void writeSampleCommands(BufferedWriter br) throws IOException {
		for (String command : getSampleCommands(true)) {
			writeCommentLine(br,command,"#");
		}
	}

	public static void printSampleCommands(MessageConsoleStream out,BufferedWriter bwOut) {
		for (String command : getSampleCommands(true)) {
			CRNReducerCommandLine.println(out,bwOut,command);
		}
	}

	public static void printSampleCommandsShort(MessageConsoleStream out,BufferedWriter bwOut) {
		for (String command : getSampleCommands(false)) {
			CRNReducerCommandLine.println(out,bwOut,command);
		}
	}


}
