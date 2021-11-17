package it.imt.erode.importing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.ui.console.MessageConsoleStream;
import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.ASTNode.Type;
import org.sbml.jsbml.text.parser.ParseException;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.crn.implementations.CRN;
import it.imt.erode.crn.implementations.CRNReactionArbitraryAbstract;
import it.imt.erode.crn.implementations.CRNReactionArbitraryGUI;
import it.imt.erode.crn.implementations.Composite;
import it.imt.erode.crn.implementations.Species;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.ICommand;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.crn.symbolic.constraints.IConstraint;
import it.imt.erode.expression.evaluator.MathEval;
import it.imt.erode.expression.parser.ICRNReactionAndBoolean;
import it.imt.erode.expression.parser.IMonomial;
import it.imt.erode.expression.parser.MinusMonomial;
import it.imt.erode.expression.parser.NumberMonomial;
import it.imt.erode.expression.parser.ParameterMonomial;
import it.imt.erode.expression.parser.ProductMonomial;
import it.imt.erode.expression.parser.SpeciesMonomial;
import it.imt.erode.partition.implementations.Block;
import it.imt.erode.partition.implementations.Partition;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.algorithms.CRNBisimulationsNAry;
import it.imt.erode.partitionrefinement.algorithms.EpsilonDifferentialEquivalences;
//import it.imt.erode.smc.multivesta.FERNState;

/**
 * 
 * @author Andrea Vandin
 * This class is used to import reaction networks or ODEs written using the XTEXT/based GUI
 */
public class GUICRNImporter  extends AbstractImporter {

	public static final String GUICRNNetworksFolder = "."+File.separator+"GUICRNNetworks"+File.separator;
	
	
	public GUICRNImporter(String fileName,MessageConsoleStream out,BufferedWriter bwOut,IMessageDialogShower msgDialogShower){
		super(fileName,out,bwOut,msgDialogShower);
	}
	public GUICRNImporter(MessageConsoleStream out,BufferedWriter bwOut,IMessageDialogShower msgDialogShower) {
		super(out,bwOut,msgDialogShower);
	}
	
	
	
	public InfoCRNImporting importCRNNetwork(boolean printInfo, boolean printCRN,boolean print, String modelName, ArrayList<String> symbolicParameters, List<IConstraint> constraints, ArrayList<ArrayList<String>> parameters,ArrayList<LinkedHashMap<String,String>> reactions,ArrayList<LinkedHashMap<String,String>> algConstraints,ArrayList<ArrayList<String>> views,ArrayList<ArrayList<String>> initialConcentrations,ArrayList<ArrayList<String>> initialAlgConcentrations, ArrayList<ArrayList<String>> userPartition/*,ArrayList<String> commands*/, ODEorNET modelDefKind, MessageConsoleStream consoleOut) throws IOException{
		if(print){
			//CRNReducerCommandLine.println(out,"\nImporting the model "+ modelName +" from the editor");
			CRNReducerCommandLine.println(out,bwOut,"\nReading "+ modelName +"...");
		}

		initInfoImporting(modelName +" from editor");
		initMath();

		initCRN(modelName);
		getInfoImporting().setLoadedCRN(true);
		getCRN().setMdelDefKind(modelDefKind);
		getInfoImporting().setLoadedCRNFormat(modelDefKind);

		for(String symbolicParam : symbolicParameters){
			addSymbolicParameter(symbolicParam);
		}
		for(IConstraint constraint : constraints){
			addConstraint(constraint);
		}
		
		for (ArrayList<String> param : parameters) {
			try{
				addParameter(param.get(0), param.get(1));
			}catch(ArithmeticException e1){
				ArithmeticException e2 = new ArithmeticException("Problems loading parameters, please check dependency among parameters, and define parameters using preceding ones only.\n\t "+e1.getMessage());
				e2.setStackTrace(e1.getStackTrace());
				throw e2;
			}
		}
		
		HashMap<String, ISpecies> speciesStoredInHashMap = new HashMap<String, ISpecies>();
		for (ArrayList<String> initialConcentration : initialConcentrations) {
			if(initialConcentration.size()==3){
				addSpecies(initialConcentration.get(0), initialConcentration.get(2),initialConcentration.get(1), speciesStoredInHashMap);
			}
			else{
				addSpecies(initialConcentration.get(0), null,initialConcentration.get(1), speciesStoredInHashMap);
			}
			
		}
		
		//HashMap<String, ISpecies> algSpeciesStoredInHashMap = new HashMap<String, ISpecies>();
		for (ArrayList<String> initialAlgConcentration : initialAlgConcentrations) {
			if(initialAlgConcentration.size()==3){
				addSpecies(initialAlgConcentration.get(0), initialAlgConcentration.get(2),initialAlgConcentration.get(1), speciesStoredInHashMap,true);
			}
			else{
				addSpecies(initialAlgConcentration.get(0), null,initialAlgConcentration.get(1), speciesStoredInHashMap,true);
			}
			
		}
		
		//CRNImporter.MAXARITY=0;
		for (LinkedHashMap<String,String> react : reactions) {
			String[] reagentsArray = react.get("reagents").split("\\+");
			String[] productsArray = react.get("products").split("\\+");
			String rateExpression = react.get("rate");
			String kind = react.get("kind");
			String id = react.get("id");
			
			if(kind.equals("massaction")){
				CRNImporter.storeMassActionReaction(speciesStoredInHashMap, reagentsArray, productsArray, rateExpression,getCRN(),getMath(),id,null);
			}
			else if(kind.equals("arbitrary")){
				CRNImporter.storeArbitraryReaction(speciesStoredInHashMap, reagentsArray, productsArray, rateExpression,getCRN(),id,null);
			}
			else if(kind.equals("hill")){
				CRNImporter.storeHillReaction(speciesStoredInHashMap, reagentsArray, productsArray, rateExpression,getCRN(),out,id,null);
			}
			else if(kind.equals("ode")){
				storeODEReaction(speciesStoredInHashMap, reagentsArray, rateExpression,id);
			}
			else{
				throw new UnsupportedOperationException("Unsupported kind of reaction: "+kind);
			}
		}
		
		for (LinkedHashMap<String,String> algConstraint : algConstraints) {
			String[] reagentsArray = algConstraint.get("reagents").split("\\+");
			//String[] productsArray = algConstraint.get("products").split("\\+");
			String rateExpression = algConstraint.get("rate");
			String kind = algConstraint.get("kind");
			String id = algConstraint.get("id");
			
			/*
			if(kind.equals("massaction")){
				CRNImporter.storeMassActionReaction(speciesStoredInHashMap, reagentsArray, productsArray, rateExpression,getCRN(),getMath(),id);
			}
			else if(kind.equals("arbitrary")){
				CRNImporter.storeArbitraryReaction(speciesStoredInHashMap, reagentsArray, productsArray, rateExpression,getCRN(),id);
			}
			else if(kind.equals("hill")){
				CRNImporter.storeHillReaction(speciesStoredInHashMap, reagentsArray, productsArray, rateExpression,getCRN(),out,id);
			}
			else if(kind.equals("ode")){
			*/
			if(kind.equals("ode")){
				storeODEReaction(speciesStoredInHashMap, reagentsArray, rateExpression,id);
			}
			else{
				throw new UnsupportedOperationException("Unsupported kind of reaction: "+kind);
			}
		}
		
		//System.out.println("Max arity: "+CRNImporter.MAXARITY);

		/*for (ArrayList<String> initialConcentration : initialConcentrations) {
			ISpecies species=speciesStoredInHashMap.get(initialConcentration.get(0));
			if(species!=null){
				species.setInitialConcentration(BigDecimal.valueOf(evaluate(initialConcentration.get(1))), initialConcentration.get(1));
			}
		}*/

		if(views.size()>0){
			loadViews(views, speciesStoredInHashMap);
		}
		
		
		IBlock uniqueBlock = new Block();
		IBlock uniqueAlgebraicBlock = new Block();
		boolean algBlockAdded=false;
		IPartition partition = new Partition(uniqueBlock,getCRN().getSpecies().size());
		setInitialPartition(partition);
		for (ISpecies species : getCRN().getSpecies()) {
			if(species.isAlgebraic()) {
				if(!algBlockAdded) {
					partition.add(uniqueAlgebraicBlock);
					algBlockAdded=true;
				}
				uniqueAlgebraicBlock.addSpecies(species);
			}
			else {
				uniqueBlock.addSpecies(species);
				//species.setInitialConcentration(BigDecimal.TEN, "10");
			}
		}
		
		readPartition(userPartition, speciesStoredInHashMap,getCRN());
		
		
		/*
		if(initialPartition.size()>1 ||(initialPartition.size()==1 && initialPartition.get(0).equals(""))){
			IBlock uniqueBlock = new Block();
			setInitialPartition(new Partition(uniqueBlock,getCRN().getSpecies().size()));
			for (ISpecies species : getCRN().getSpecies()) {
				uniqueBlock.addSpecies(species);
				//species.setInitialConcentration(BigDecimal.TEN, "10");
			}
		}
		else{
			IPartition partition = new Partition(getCRN().getSpecies().size());
			setInitialPartition(partition);
			for (ArrayList<String> currentBlockString : initialPartition) {
				IBlock currentBlock = new Block();
				partition.add(currentBlock);
				for (String currentSpeciesName : currentBlockString) {
					ISpecies currentSpecies = speciesStoredInHashMap.get(currentSpeciesName);
					if(currentSpecies!=null){
						currentBlock.addSpecies(currentSpecies);
					}
				}
			}
		}*/

		getInfoImporting().setReadParameters(getCRN().getParameters().size());
		getInfoImporting().setReadCRNReactions(getCRN().getReactions().size());
		//getInfoImporting().setReadProducts(getCRN().getProducts().size());
		//getInfoImporting().setReadReagents(getCRN().getReagents().size());
		getInfoImporting().setReadSpecies(getCRN().getSpecies().size());
		getInfoImporting().setLoadedCRNFormat(getCRN().getMdelDefKind());


		/*for(String comm : commands){
			addCommand(comm);
			getInfoImporting().increaseCommandsCounter();
		}*/

		if(printInfo&&print){
			CRNReducerCommandLine.println(out,bwOut,getInfoImporting().toStringShort());
		}
		if(printCRN&&print){
			getCRN().printCRN();
		}

		return getInfoImporting();
	}
	public static void readPartition(ArrayList<ArrayList<String>> initialPartition,
			HashMap<String, ISpecies> speciesStoredInHashMap,ICRN crn) {
		//if(!(initialPartition.size()==0 ||(initialPartition.size()==1 && initialPartition.get(0).equals("")))){
		if(!(initialPartition.size()==0 ||(initialPartition.size()==1 && initialPartition.get(0).size()==0))){
			ArrayList<HashSet<ISpecies>> userDefinedInitialPartition = new ArrayList<HashSet<ISpecies>>(initialPartition.size());
			crn.setUserDefinedPartition(userDefinedInitialPartition);
			for (ArrayList<String> currentBlockString : initialPartition) {
				HashSet<ISpecies> currentBlock = new HashSet<>(currentBlockString.size());
				userDefinedInitialPartition.add(currentBlock);
				for (String currentSpeciesName : currentBlockString) {
					ISpecies currentSpecies = speciesStoredInHashMap.get(currentSpeciesName);
					if(currentSpecies!=null){
						currentBlock.add(currentSpecies);
					}
				}
			}
			
			/*for (ArrayList<String> currentBlockString : initialPartition) {
				IBlock currentBlock = new Block();
				partition.add(currentBlock);
				for (String currentSpeciesName : currentBlockString) {
					ISpecies currentSpecies = speciesStoredInHashMap.get(currentSpeciesName);
					if(currentSpecies!=null){
						partition.getBlockOf(currentSpecies).removeSpecies(currentSpecies);
						currentBlock.addSpecies(currentSpecies);
					}
				}
			}
			IBlock currentBlock = partition.getFirstBlock();
			while(currentBlock!=null){
				IBlock nextBlock = currentBlock.getNext();
				if(currentBlock.getSpecies().size()==0){
					partition.remove(currentBlock);
				}
				currentBlock=nextBlock;
			}*/
		}
	}
	
	
	private void loadViews(ArrayList<ArrayList<String>> views,
			HashMap<String, ISpecies> speciesNameToSpecies) {
		List<String> viewNames = new ArrayList<String>(views.size());
		List<String> viewExpressions = new ArrayList<String>(views.size());
		List<String> viewExpressionsSupportedByMathEval = new ArrayList<String>(views.size());
		List<Boolean> viewExpressionUsesCovariance = new ArrayList<Boolean>(views.size());
		boolean viewsSupportedForPrepartitioning=true;
		List<HashMap<ISpecies,Integer>> setsOfSpeciesOfViews = new ArrayList<HashMap<ISpecies,Integer>>();

		for (ArrayList<String> view : views) {
			String viewName = view.get(0);
			String viewExpr = view.get(1);
			viewsSupportedForPrepartitioning=CRNImporter.loadAView(viewNames,viewExpressions,viewExpressionsSupportedByMathEval,viewExpressionUsesCovariance,viewName,viewExpr,viewsSupportedForPrepartitioning,speciesNameToSpecies,setsOfSpeciesOfViews);
		}
		CRNImporter.finalizeToLoadViews(viewNames, viewExpressions,viewExpressionsSupportedByMathEval,viewExpressionUsesCovariance, viewsSupportedForPrepartitioning,setsOfSpeciesOfViews,getCRN());		
	}
	
	/**
	 * Add 
	 * 	1) a reaction 'speciesOfODE -> speciesOfODE + speciesOfODE , drift'  
	 * 				d(speciesOfODE) = drift
	 * or
	 *  2) an algebraic constraint reaction 'algSpeciesOfConstraint -> algSpeciesOfConstraint + algSpeciesOfConstraint , drift'
	 *  				algSpeciesOfConstraint = drift
	 * @param speciesStoredInHashMap
	 * @param reagentsArray
	 * @param body
	 * @param id
	 * @throws IOException
	 */
	private void storeODEReaction(HashMap<String, ISpecies> speciesStoredInHashMap, String[] reagentsArray,String body,String id) throws IOException {
		if(reagentsArray.length!=1){
			throw new UnsupportedOperationException("I expect one reagent only, which is the species of the considered ODE");
		}
		HashMap<ISpecies, Integer> reagentsHM = CRNImporter.generateNewSpeciesAndBuildArrayOfNames(speciesStoredInHashMap, reagentsArray,getCRN());
		ISpecies speciesOfODE = reagentsHM.keySet().iterator().next();
		//create reaction speciesOfODE -> speciesOfODE + speciesOfODE, arbitrary rateLaw
		IComposite products = new Composite(speciesOfODE,speciesOfODE);
		ICRNReaction reaction = new CRNReactionArbitraryGUI((IComposite)speciesOfODE, products, body,id);
		getCRN().addReaction(reaction);
		/*
		addToIncomingReactionsOfProducts(reaction.getArity(),(IComposite)speciesOfODE, reaction,CRNReducerCommandLine.addReactionToComposites);
		addToOutgoingReactionsOfReagents(reaction.getArity(), reaction.getReagents(), reaction,CRNReducerCommandLine.addReactionToComposites);
		if(CRNReducerCommandLine.hasToAddToReactionsWithNonZeroStoichiometry){
			//addToReactionsWithNonZeroStoichiometry(reaction.getArity(), reaction.computeProductsMinusReagentsHashMap(),reaction);
			addToReactionsWithNonZeroStoichiometry(reaction.getArity(), reagentsHM,reaction);
		}
		*/
	}
	
	public static String getOnlyAlphaNumeric(String s) {
	    Pattern pattern = Pattern.compile("[^0-9 a-z A-Z _]");
	    Matcher matcher = pattern.matcher(s.replace('-', '_'));
	    String number = matcher.replaceAll("");
	    return number;
	 }
	
	public static void printToERODEFIle(ICRN crn,IPartition partition, String name, boolean assignPopulationOfRepresentative, 
			boolean groupAccordingToCurrentPartition, Collection<String> preambleCommentLines, boolean verbose, 
			String icComment,MessageConsoleStream out,BufferedWriter bwOut, ODEorNET type, boolean rnEncoding,boolean originalNames){
		String fileName = name;
		
		fileName=overwriteExtensionIfEnabled(fileName,".ode");
		if(verbose){
			CRNReducerCommandLine.print(out,bwOut,"Writing model in file "+fileName);
		}

		createParentDirectories(fileName);
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printToERODEFIle, exception raised while creating the filewriter for file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}

		try {

			if(preambleCommentLines!=null && preambleCommentLines.size()>0){
				for (String comment : preambleCommentLines) {
					bw.write("//"+comment+"\n");
				}
				
			}
			//bw.write("\n\n");
			
			bw.write("begin model ");
			if(crn.getName()!=null&&crn.getName()!=""){
				String nam = getModelName(name);
				bw.write(nam+"\n");
			}
			else{
				bw.write("unnamed\n");
			}
			
			boolean writeAsImport = false;
			/*if(crn!=null && (crn.getSpecies().size()>300 || crn.getReactions().size()>1000)){
				writeAsImport=true;
			}*/
			
			if(writeAsImport){
				//We use the old .crn format
				writeOldCRNAndXtextImport(bw,crn,type,false,out,bwOut,rnEncoding,fileName,partition);
			}
			else{
				writeXtextCRN(bw,crn,type,false,out,bwOut,rnEncoding,originalNames);
				if(groupAccordingToCurrentPartition){
					bw.write(" begin views\n");
					IBlock currentBlock = partition.getFirstBlock();
					while(currentBlock!=null){
						bw.write("  " + currentBlock.getRepresentative(CRNReducerCommandLine.COMPUTEREPRESENTATIVEBYMINOUTSIDEPARTITIONREFINEMENT).getName());
						bw.write(" = " + currentBlock.toStringSeparatedSpeciesNames(" + ")+"\n");
						currentBlock=currentBlock.getNext();
					}
					bw.write(" end views\n");
				}
				else{
					if(crn.getViewNames()!=null && crn.getViewNames().length>0){
						bw.write(" begin views\n");
						for(int i=0;i<crn.getViewNames().length;i++){
							bw.write("  "+crn.getViewNames()[i]);
							bw.write(" = "+crn.getViewExpressions()[i]+"\n");
						}
						bw.write(" end views\n");
					}
				}
			}
			
			boolean printedBeginEndComments = false;
			//bw.write("\n //Comments associated to the species\n");
			for (ISpecies species : crn.getSpecies()) {
				boolean speciesWritten=false;
				if(species.getComments()!=null && species.getComments().size()>0){
					for(String comment : species.getComments()){
						if(!comment.equals("")){
							if(!printedBeginEndComments){
								printedBeginEndComments=true;
								//writeCommentLine(br, " Comments associated to the species");
								//bw.write("\n //Comments associated to the species\n");
								//br.write("begin comments\n");
								bw.write("\n//Comments associated to the species\n");
							}
							//bw.write("\n //"+species.getName()+":  \n");
							if(!speciesWritten){
								bw.write("//"+species.getName()+":  \n");
								speciesWritten=true;
							}
							bw.write("  //" +comment+"\n");
						}
					}
				}
			}
			
			printedBeginEndComments=false;
			int r=0;
			for (ICRNReaction reaction : crn.getReactions()) {
				r++;
				boolean reactionWritten=false;
				if(reaction.getComments()!=null && reaction.getComments().size()>0){
					for(String comment : reaction.getComments()){
						if(!(comment==null || comment.equals(""))){
							if(!printedBeginEndComments){
								printedBeginEndComments=true;
								//writeCommentLine(br, " Comments associated to the species");
								//bw.write("\n //Comments associated to the species\n");
								//br.write("begin comments\n");
								bw.write("\n//Comments associated to the reactions\n");
							}
							//bw.write("\n //"+species.getName()+":  \n");
							if(!reactionWritten){
								if(reaction.getID()!=null && reaction.getID().length()>0) {
									bw.write("//"+reaction.getID()+":  \n");
								}
								else {
									bw.write("//reaction "+r+":  \n");
								}
								reactionWritten=true;
							}
							bw.write("  //" +comment+"\n");
						}
					}
				}
			}
			
			//writeOriginalNames(crn, bw,"//");
			
			/*if(printedBeginEndComments){
				br.write("end comments\n\n");
			}*/
			
			//String crnString = buildXtextCRNString(crn,type,false);
			//bw.write(crnString);
			
			if(crn.getCommands()!=null && crn.getCommands().size()>0){
				for (ICommand command : crn.getCommands()) {
					bw.write(command.toODEFormat()+"\n");
//					StringBuffer sb = new StringBuffer(command.getName());
//					sb.append("(");
//					int i=0;
//					for (CommandParameter parameter : command.getParameters()) {
//						sb.append(parameter.getName());
//						sb.append("=");
//						sb.append(parameter.getValue());
//						if(i<command.getParameters().size()-1){
//							sb.append(",");
//						}
//						i++;
//					}
//					sb.append(")\n");
//					bw.write(sb.toString());
				}
			}

			//CRNReducerCommandLine.print(out, "end model\n\n");
			bw.write("\n"+"end model\n\n");
			//CRNReducerCommandLine.print(out, "end model\n\n");

			

		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printToCRNFile, exception raised while writing in the file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		finally{
			if(verbose){
				CRNReducerCommandLine.println(out,bwOut,"Writing model in file "+fileName+" completed");
			}
			try {
				bw.flush();
				bw.close();
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in printToCRNFile, exception raised while closing the bufferedwriter of the file: "+fileName);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}


		
	}
	
	
	protected static void writeOriginalNames(ICRN crn, BufferedWriter bw,String commentSymbol) throws IOException {
		boolean written = false;
		for (ISpecies species : crn.getSpecies()) {
			if(species.getOriginalName()!=null){
				if(!written){
					written=true;
					bw.write("\n"+commentSymbol+"Correspondence with original names\n");
				}
				bw.write("\n"+commentSymbol+species.getName()+" : "+species.getOriginalName()+"\n");
			}
		}
	}
	
	public static String getModelName(String fileName) {
		String name = overwriteExtensionIfEnabled(fileName,"",true);
		int sep = name.lastIndexOf('/');
		if(sep!=-1){
			name=name.substring(sep+1);
		}
		sep = name.lastIndexOf('\\');
		if(sep!=-1){
			name=name.substring(sep+1);
		}
		//nam = MathEval.getCorrespondingStringSupportedByMathEval(nam);nam=nam.substring(1);
		name = GUICRNImporter.getOnlyAlphaNumeric(name);
		
		if(name.startsWith("0")||name.startsWith("1")||name.startsWith("2")||name.startsWith("3")||name.startsWith("4")||name.startsWith("5")||name.startsWith("6")||name.startsWith("7")||name.startsWith("8")||name.startsWith("9")){
			name="m"+name;
		}
		return name;
	}
	
	private static void writeOldCRNAndXtextImport(BufferedWriter bw, ICRN crn, ODEorNET type, boolean printView,MessageConsoleStream out,BufferedWriter bwOut, boolean rnEncoding, String fileName, IPartition partition)  throws IOException {
		String crnFile = fileName.replace(".ode", "");
		crnFile=crnFile+".crn";
		int fileSep = crnFile.lastIndexOf(File.separator);
		String crnFileLocal = crnFile;
		if(fileSep!=1){
			crnFileLocal=crnFileLocal.substring(fileSep+1);
		}
		bw.write(" importCRN(fileIn=\""+crnFileLocal+"\")\n");
		Collection<String> preambleComments = new ArrayList<>(1);
		preambleComments.add("To be used jointly with "+fileName);
		boolean writeSpecies=false;
		CRNImporter.printCRNToCRNFile(crn, partition, crnFile, false, false, preambleComments, false, null, out,bwOut,writeSpecies);
		//BioNetGenImporter.printCRNToNetFile(crn, partition,crnFile,false,false,false,out);
	}
	
	/*public static void writeXtextCRN(BufferedWriter bw,ICRN crn,ODEorNET type,MessageConsoleStream out,BufferedWriter bwOut,boolean rnEncoding) throws IOException{
		writeXtextCRN(bw,crn,type,true,out,rnEncoding);
	}*/
	
/*
	private static void writeXtextCRNAddingIEvenIfNotNecessary(BufferedWriter bw,ICRN crn, ODEorNET type, boolean printView,MessageConsoleStream out,BufferedWriter bwOut,boolean rnEncoding) throws IOException {
		//StringBuilder sb = new StringBuilder();
		
		boolean usedI=false;
		
		if(crn!=null){
			if(crn.getSymbolicParameters()!=null && crn.getSymbolicParameters().size()>0){
				bw.write(" begin symbolic parameters\n");//sb.append(" begin symbolic parameters\n");
				for (String param : crn.getSymbolicParameters()) {
					bw.write("  ");
					bw.write(param);
					bw.write("\n");
				}
				bw.write(" end symbolic parameters\n");
			}
			if(crn.getParameters()!=null && crn.getParameters().size()>0){
				bw.write(" begin parameters\n");
				for (String param : crn.getParameters()) {
					int space = param.indexOf(' ');
					String parName=param.substring(0,space);
					String parExpr = param.substring(space+1);
					bw.write("  ");
					bw.write(parName.trim());
					bw.write(" = ");
					bw.write(parExpr.trim());
					bw.write("\n");
				}
				bw.write(" end parameters\n");
			}
			
			ISpecies I =null;
			HashMap<String,ISpecies> speciesNameToSpecies = new HashMap<String,ISpecies>(crn.getSpecies().size());
			bw.write(" begin init\n");
			for (ISpecies species : crn.getSpecies()) {
				speciesNameToSpecies.put(species.getName(), species);
				if(species.getName().equals("I")){
					I=species;
				}
				
				if(!species.isAlgebraic()) {
					bw.write("  ");
					bw.write(species.getName());
					
					if(species.getInitialConcentration().compareTo(BigDecimal.ZERO)!=0){
						bw.write(" = ");
						bw.write(species.getInitialConcentrationExpr());
					}
					
					if(species.getOriginalName()!=null){
						bw.write(" ( \"" + species.getOriginalName() + "\" ) ");
					}
					
					bw.write("\n");
				}
			}
			boolean IAdded=false;
			if(!((!rnEncoding) || crn.isMassAction())){
				//boolean computeRNEncoding=true;
				//I try to encode the CRN in a mass action RN. I store all reaction in rnReactions.
				if(I==null){
					IAdded=true;
					I = new Species("I", null, crn.getSpecies().size(), BigDecimal.ONE, "1");
					bw.write("  ");
					bw.write(I.getName());
					if(I.getInitialConcentration().compareTo(BigDecimal.ZERO)!=0){
						bw.write(" = ");
						bw.write(I.getInitialConcentrationExpr());
					}
					bw.write("\n");
				}
			}
			bw.write(" end init\n");
			
			if(crn.algebraicSpecies()>0) {
				bw.write(" begin alginit\n");
				for (ISpecies species : crn.getSpecies()) {
					if(species.isAlgebraic()) {
						bw.write("  ");
						bw.write(species.getName());
						
						if(species.getInitialConcentration().compareTo(BigDecimal.ZERO)!=0){
							bw.write(" = ");
							bw.write(species.getInitialConcentrationExpr());
						}
						
						if(species.getOriginalName()!=null){
							bw.write(" ( \"" + species.getOriginalName() + "\" ) ");
						}
						bw.write("\n");
					}
				}
				
				bw.write(" end alginit\n");
			}
			
			if(crn.getUserDefinedPartition().size()>0 || IAdded){
				bw.write(" begin partition\n");
				if(IAdded){
					bw.write("  {");
					bw.write(I.getName());
					if(crn.getUserDefinedPartition().size()>0){
						bw.write("},");
					}
					else{
						bw.write("}");
					}
					bw.write("\n");
				}
				int b=0;
				for (HashSet<ISpecies> block : crn.getUserDefinedPartition()) {
					bw.write("  {");
					int i=0;
					for (ISpecies species : block) {
						bw.write(species.getName());
						i++;
						if(i<block.size()){
							bw.write(',');
						}
					}
					//sb.delete(sb.length()-1, sb.length());
					b++;
					if(b<crn.getUserDefinedPartition().size()){
						bw.write("},");
					}
					else{
						bw.write("}");
					}
					bw.write("\n");
				}
				bw.write(" end partition\n");
			}
			
//			if(crn.getUserDefinedPartition().size()>0){
//				bw.write(" begin partition\n");
//				int b=0;
//				for (HashSet<ISpecies> block : crn.getUserDefinedPartition()) {
//					bw.write("  {");
//					int i=0;
//					for (ISpecies species : block) {
//						bw.write(species.getName());
//						i++;
//						if(i<block.size()){
//							bw.write(',');
//						}
//					}
//					//sb.delete(sb.length()-1, sb.length());
//					b++;
//					if(b<crn.getUserDefinedPartition().size()){
//						bw.write("},");
//					}
//					else{
//						bw.write("}");
//					}
//					bw.write("\n");
//				}
//				bw.write(" end partition\n");
//			}
			
			

			if(type.equals(ODEorNET.ODE)){
			//if(crn.getMdelDefKind()!=null && crn.getMdelDefKind().equalsIgnoreCase("ODE")){
				boolean ignoreOnes = false;
				boolean ignoreI=false;
				HashMap<ISpecies, StringBuilder> speciesToDrift = computeDrifts(crn,ignoreOnes,ignoreI);
				
				bw.write(" begin ODE\n");
				for(ISpecies species : crn.getSpecies()){
					if(!species.isAlgebraic()) {
						String speciesDrift="0";
						StringBuilder speciesDriftSB = speciesToDrift.get(species);
						if(speciesDriftSB!=null){
							speciesDrift=speciesDriftSB.toString();
						}
						bw.write("  d(");
						bw.write(species.getName());
						bw.write(") = ");
						bw.write(speciesDrift);
						bw.write("\n");
					}
				}
//				for (ICRNReaction reaction : crn.getReactions()) {
//					//d(x1) = drift; corresponds to x1 -> x1+x1 arbitrary drift
//					String species = reaction.getReagents().getFirstReagent().getName();
//					sb.append("  d(");
//					sb.append(species);
//					sb.append(") = ");
//					sb.append(reaction.getRateExpression());
//					sb.append(";\n");
//				}
				//ic
//				sb.append("  begin initialConcentrations\n");
//				for (ISpecies species : crn.getSpecies()) {
//					if(species.getInitialConcentration().compareTo(BigDecimal.ZERO)!=0){
//						sb.append("   ");
//						sb.append(species.getName());
//						sb.append(" = ");
//						sb.append(species.getInitialConcentrationExpr());
//						sb.append("\n");
//					}
//				}
//				sb.append("  end initialConcentrations\n");
				bw.write(" end ODE\n");
				
				
				if(crn.algebraicSpecies()>0) {
					bw.write(" begin algebraic\n");
					for(ISpecies species : crn.getSpecies()){
						if(species.isAlgebraic()) {
							String speciesAlgebraicConstraint="0";
							StringBuilder speciesAlgebraicConstraintSB = speciesToDrift.get(species);
							if(speciesAlgebraicConstraintSB!=null){
								speciesAlgebraicConstraint=speciesAlgebraicConstraintSB.toString();
							}
							bw.write("  ");
							bw.write(species.getName());
							bw.write(" = ");
							bw.write(speciesAlgebraicConstraint);
							bw.write("\n");
						}
					}
					bw.write(" end algebraic\n");
				}
			}
			else{
				//sb.append(" begin net\n");
//				sb.append("  begin species\n");
//				for (ISpecies species : crn.getSpecies()) {
//					sb.append("   ");
//					sb.append(species.getName());
//					sb.append(" = ");
//					sb.append(species.getInitialConcentrationExpr());
//					sb.append("\n");
//				}
//				sb.append("  end species\n");
				bw.write(" begin reactions\n");
				
				//BEGINBEGINBEGIN
				if((!rnEncoding) || crn.isMassAction()){
					for (ICRNReaction reaction : crn.getReactions()) {
						writeReaction(bw, reaction);
					}
				}
				else{
					boolean computeRNEncoding=true;
					//I try to encode the CRN in a mass action RN. I store all reaction in rnReactions.
					//ISpecies I = new Species("I", speciesNameToSpecies.size(), BigDecimal.ONE, "1");
					ArrayList<ICRNReaction> rnReactions = new ArrayList<>(crn.getReactions().size());
					//Mass action reactions do not have to be modified
					for (ICRNReaction reaction : crn.getReactions()) {
						if(!reaction.hasArbitraryKinetics()){
							rnReactions.add(reaction);
						}
						else{
							//If the reaction is X -> X+X, arbitrary drift
							if(reaction.isODELike()){
								try{
									boolean anyReactionUsesI = computeRNEncoding((CRNReactionArbitraryGUI)reaction,reaction.getReagents().getFirstReagent(),speciesNameToSpecies,crn.getMath(),I,rnReactions);
									usedI = usedI || anyReactionUsesI; 
								}
								catch(UnsupportedReactionNetworkEncodingException e){
									computeRNEncoding=false;
									CRNReducerCommandLine.print(out,bwOut, "\n The model cannot be encoded as a mass action reaction network.\n I encode it as an arbitrary one...");
									break;
									//writeReaction(bw, reaction);
								}
							}
							else{
								//writeReaction(bw, reaction);
								//computeRNEncoding=false;
								//CRNReducerCommandLine.print(out,bwOut, "\n The model cannot be encoded as a mass action reaction network.\n I encode it as an arbitrary one...");
								//break;
								try{
									boolean anyReactionUsesI = GUICRNImporter.computeRNEncodingOfArbitraryReaction((CRNReactionArbitraryGUI)reaction,speciesNameToSpecies,crn.getMath(),I,rnReactions,null);
									usedI = usedI || anyReactionUsesI;
								}
								catch(UnsupportedReactionNetworkEncodingException e){
									computeRNEncoding=false;
									CRNReducerCommandLine.print(out,bwOut, "\n The model cannot be encoded as a mass action reaction network.\n I encode it as an arbitrary one...");
									break;
									//writeReaction(bw, reaction);
								}
							}
						}
					}
					//If the RN encoding failed, I write the model as an arbitrary RN.
					if(computeRNEncoding){
						ICRN crnRN = new CRN(crn.getName()+"RN",crn.getMath(), out, bwOut);
						CRN.collapseAndCombineAndAddReactions(crnRN, rnReactions, out,bwOut);
						for (ICRNReaction reaction : crnRN.getReactions()) {
							writeReaction(bw, reaction);
						}
					}
					else{
						for (ICRNReaction reaction : crn.getReactions()) {
							writeReaction(bw, reaction);
						}
					}
				}
				///ENDENDEND
				bw.write(" end reactions\n");
				//bw.write(" end net\n");
			}
			if(printView){
				bw.write(" begin views\n");
				for(int i=0; i<crn.getViewNames().length;i++){
					bw.write("  ");
					bw.write(crn.getViewNames()[i]);
					bw.write(" = ");
					bw.write(crn.getViewExpressions()[i]);
					bw.write("\n");
				}
				bw.write(" end views");
			}
		}
		else{
			bw.write("//UNDEFINED MODEL");
		}
	}
	*/
	
	
	
	
	
	
	
	
	
	
	private static void writeXtextCRN(BufferedWriter bw,ICRN crn, ODEorNET type, boolean printView,MessageConsoleStream out,
			BufferedWriter bwOut,boolean rnEncoding,boolean originalNames) throws IOException {		
		if(crn!=null){
			//Begin compute RN encoding
			boolean computeRNEncoding=rnEncoding && !crn.isMassAction() && !type.equals(ODEorNET.ODE);
			ArrayList<ICRNReaction> rnReactions = null;
			if(computeRNEncoding) {
				rnReactions = new ArrayList<>(crn.getReactions().size());
			}
			boolean anyReactionFromRNEncodingUsesI=false;
			boolean IalreadyPresent=false;
			
			
			HashMap<String,ISpecies> speciesNameToSpecies = new HashMap<String,ISpecies>(crn.getSpecies().size());
			ISpecies I =null;
			for (ISpecies species : crn.getSpecies()) {
				speciesNameToSpecies.put(species.getName(), species);
				if(species.getName().equals(Species.I_SPECIESNAME)){
					I=species;
					IalreadyPresent=true;
				}
			}
			if(!IalreadyPresent) {
				I = new Species(Species.I_SPECIESNAME, null, crn.getSpecies().size(), BigDecimal.ONE, "1",false);
			}
			
			if(computeRNEncoding){
				//I try to encode the CRN in a mass action RN. I store all reaction in rnReactions.
				for (ICRNReaction reaction : crn.getReactions()) {
					//Mass action reactions do not have to be modified
					if(!reaction.hasArbitraryKinetics()){
						rnReactions.add(reaction);
					}
					else{
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
								boolean anyReactionUsesI = GUICRNImporter.computeRNEncodingOfArbitraryReaction((CRNReactionArbitraryGUI)reaction,speciesNameToSpecies,crn.getMath(),I,rnReactions/*,null*/);
								anyReactionFromRNEncodingUsesI = anyReactionFromRNEncodingUsesI || anyReactionUsesI;
							}
							catch(UnsupportedReactionNetworkEncodingException e){
								computeRNEncoding=false;
								CRNReducerCommandLine.print(out,bwOut, "\n The model cannot be encoded as a mass action reaction network.\n I encode it as an arbitrary one...");
								break;
							}
						}
					}
				}
			}
			final boolean shouldAddI = (!IalreadyPresent) && anyReactionFromRNEncodingUsesI && computeRNEncoding;
			//End compute RN encoding
			
			
			//Start writing the CRN 
	
			if(crn.getSymbolicParameters()!=null && crn.getSymbolicParameters().size()>0){
				bw.write(" begin symbolic parameters\n");//sb.append(" begin symbolic parameters\n");
				for (String param : crn.getSymbolicParameters()) {
					bw.write("  ");
					bw.write(param);
					bw.write("\n");
				}
				bw.write(" end symbolic parameters\n");
			}
			if(crn.getParameters()!=null && crn.getParameters().size()>0){
				bw.write(" begin parameters\n");
				for (String param : crn.getParameters()) {
					int space = param.indexOf(' ');
					String parName=param.substring(0,space);
					String parExpr = param.substring(space+1);
					bw.write("  ");
					bw.write(parName.trim());
					bw.write(" = ");
					bw.write(parExpr.trim());
					bw.write("\n");
				}
				bw.write(" end parameters\n");
			}
			
			writeInitBlock(bw, crn.getSpecies(), originalNames, I, shouldAddI);
			
			if(crn.algebraicSpecies()>0) {
				bw.write(" begin alginit\n");
				for (ISpecies species : crn.getSpecies()) {
					if(species.isAlgebraic()) {
						bw.write("  ");
						bw.write(species.getName());
						
						if(species.getInitialConcentration().compareTo(BigDecimal.ZERO)!=0){
							bw.write(" = ");
							bw.write(species.getInitialConcentrationExpr());
						}
						
						if(species.getOriginalName()!=null){
							bw.write(" ( \"" + species.getOriginalName() + "\" ) ");
						}
						bw.write("\n");
					}
				}
				
				bw.write(" end alginit\n");
			}
			
			writeInitPartition(bw, crn.getUserDefinedPartition(), I, shouldAddI);
			

			if(type.equals(ODEorNET.ODE)){
				boolean ignoreOnes = false;
				boolean ignoreI=false;
				HashMap<ISpecies, StringBuilder> speciesToDrift = computeDrifts(crn,ignoreOnes,ignoreI);
				
				bw.write(" begin ODE\n");
				for(ISpecies species : crn.getSpecies()){
					if(!species.isAlgebraic()) {
						String speciesDrift="0";
						StringBuilder speciesDriftSB = speciesToDrift.get(species);
						if(speciesDriftSB!=null){
							speciesDrift=speciesDriftSB.toString();
						}
						bw.write("  d(");
						bw.write(species.getName());
						bw.write(") = ");
						bw.write(speciesDrift);
						bw.write("\n");
					}
				}
				bw.write(" end ODE\n");
				
				
				if(crn.algebraicSpecies()>0) {
					bw.write(" begin algebraic\n");
					for(ISpecies species : crn.getSpecies()){
						if(species.isAlgebraic()) {
							String speciesAlgebraicConstraint="0";
							StringBuilder speciesAlgebraicConstraintSB = speciesToDrift.get(species);
							if(speciesAlgebraicConstraintSB!=null){
								speciesAlgebraicConstraint=speciesAlgebraicConstraintSB.toString();
							}
							bw.write("  ");
							bw.write(species.getName());
							bw.write(" = ");
							bw.write(speciesAlgebraicConstraint);
							bw.write("\n");
						}
					}
					bw.write(" end algebraic\n");
				}
			}
			else{
				bw.write(" begin reactions\n");
				
				if(computeRNEncoding) {
					ICRN crnRN = new CRN(crn.getName()+"RN",crn.getMath(), out, bwOut);
					CRN.collapseAndCombineAndAddReactions(crnRN, rnReactions, out,bwOut);
					for (ICRNReaction reaction : crnRN.getReactions()) {
						writeReaction(bw, reaction,originalNames);
					}
				}
				else{
					//If no RN encoding was required, or if the RN encoding failed, I write the model as an arbitrary RN.
					for (ICRNReaction reaction : crn.getReactions()) {
						writeReaction(bw, reaction,originalNames);
					}
				}
				bw.write(" end reactions\n");
			}
			if(printView){
				bw.write(" begin views\n");
				for(int i=0; i<crn.getViewNames().length;i++){
					bw.write("  ");
					bw.write(crn.getViewNames()[i]);
					bw.write(" = ");
					bw.write(crn.getViewExpressions()[i]);
					bw.write("\n");
				}
				bw.write(" end views");
			}
		}
		else{
			bw.write("//UNDEFINED MODEL");
		}
	}
	
	public static void writeInitBlock(BufferedWriter bw, List<ISpecies> allSpecies, boolean originalNames, ISpecies I,
			final boolean shouldAddI) throws IOException {
		writeInitBlock(bw, allSpecies, originalNames, I, shouldAddI, null);
	}
	
	public static void writeInitBlock(BufferedWriter bw, List<ISpecies> allSpecies, boolean originalNames, ISpecies I,
			final boolean shouldAddI, LinkedHashMap<String, Integer> nameToMax) throws IOException {
		bw.write(" begin init\n");
		for (ISpecies species : allSpecies) {
			if(!species.isAlgebraic()) {
				bw.write("  ");
				if(shouldUseOriginalName(species, originalNames)) {
					bw.write(species.getOriginalName());
				}
				else {
					bw.write(species.getName());
				}
				
				if(nameToMax!=null) {
					Integer max=nameToMax.get(species.getName());
					if (max!=null && max!=1) {
						bw.write(" [ ");
						bw.write(""+max);
						bw.write(" ] ");
					}
				}
				
				if(species.getInitialConcentration().compareTo(BigDecimal.ZERO)!=0){
					bw.write(" = ");
					bw.write(species.getInitialConcentrationExpr());
				}
				
				if(!shouldUseOriginalName(species, originalNames)) {
					if(species.getOriginalName()!=null){
						bw.write(" ( \"" + species.getOriginalName() + "\" ) ");
					}
				}
				
				bw.write("\n");
			}
		}
		if(shouldAddI) {
			bw.write("  ");
			bw.write(I.getName());
			if(I.getInitialConcentration().compareTo(BigDecimal.ZERO)!=0){
				bw.write(" = ");
				bw.write(I.getInitialConcentrationExpr());
			}
			bw.write("\n");
		}
		
		bw.write(" end init\n");
	}
	public static void writeInitPartition(BufferedWriter bw, ArrayList<HashSet<ISpecies>> userDefinedPartition, ISpecies I, final boolean shouldAddI)
			throws IOException {
		if(userDefinedPartition.size()>0 || shouldAddI){
			bw.write(" begin partition\n");
			if(shouldAddI){
				bw.write("  {");
				bw.write(I.getName());
				if(userDefinedPartition.size()>0){
					bw.write("},");
				}
				else{
					bw.write("}");
				}
				bw.write("\n");
			}
			int b=0;
			for (HashSet<ISpecies> block : userDefinedPartition) {
				bw.write("  {");
				int i=0;
				for (ISpecies species : block) {
					bw.write(species.getName());
					i++;
					if(i<block.size()){
						bw.write(',');
					}
				}
				b++;
				if(b<userDefinedPartition.size()){
					bw.write("},");
				}
				else{
					bw.write("}");
				}
				bw.write("\n");
			}
			bw.write(" end partition\n");
		}
	}
	
	private static boolean shouldUseOriginalName(ISpecies species, boolean originalNames) {
		return originalNames && (species.getOriginalName()!=null && species.getOriginalName().length()>0);
	}
	
	
	public static boolean computeRNEncoding(CRNReactionArbitraryGUI reaction,ISpecies speciesOfODE, HashMap<String, ISpecies> speciesNameToSpecies,MathEval math,/*BufferedWriter bw,*/ISpecies I, ArrayList<ICRNReaction> rnReactions) throws UnsupportedReactionNetworkEncodingException, IOException {
		ArrayList<IMonomial> monomials= parseGUIPolynomialArbitraryRateExpression(reaction.getRateLaw(),speciesNameToSpecies,math);
		boolean usedI = false;
		for (IMonomial monomial : monomials) {
			//CRNReducerCommandLine.print(out,monomial+" + ");
			ICRNReactionAndBoolean newReactionAndUsedI = monomial.toReaction(speciesOfODE,I);
			ICRNReaction newReaction = newReactionAndUsedI.getReaction();
			if(!(newReaction.getRateExpression().equalsIgnoreCase("0")||newReaction.getRateExpression().equalsIgnoreCase("0.0"))){
				usedI = usedI || newReactionAndUsedI.isBoolValue();
				rnReactions.add(newReaction);
			}
			//writeReaction(bw, newReaction);
		}
		
		//Returns true if at least a reaction used I
		return usedI;
	}
	
	public static boolean computeRNEncodingOfArbitraryReaction(CRNReactionArbitraryGUI reaction, HashMap<String, ISpecies> speciesNameToSpecies,MathEval math,
			/*BufferedWriter bw,*/ISpecies I, ArrayList<ICRNReaction> rnReactions/*, HashMap<ISpecies, ISpecies> oldSpeciesToNewSpecies*/) throws UnsupportedReactionNetworkEncodingException, IOException {
		//AAA
		boolean usedI=false;
		ArrayList<IMonomial> monomials= parseGUIPolynomialArbitraryRateExpression(reaction.getRateLaw(),speciesNameToSpecies,math);
		/*
		for(IMonomial mon : monomials) {
			BigDecimal coeff = mon.getOrComputeCoefficient();
			if(coeff.compareTo(BigDecimal.ZERO) >0) {
				//forward reaction
				HashMap<ISpecies, Integer> reagentSpecies=new HashMap<ISpecies, Integer>(3);
				mon.computeSpecies(reagentSpecies);
				IComposite comp = new Composite(reagentSpecies);
				if(reaction.getReagents().equals(comp)) {
					CRNReaction maReaction = new CRNReaction(coeff,reaction.getReagents(),reaction.getProducts(),coeff.toPlainString(),null);
				}
			}
			else if(coeff.compareTo(BigDecimal.ZERO) <0){
				//reverse reaction
				HashMap<ISpecies, Integer> productSpecies=new HashMap<ISpecies, Integer>(3);
				mon.computeSpecies(productSpecies);
				IComposite comp = new Composite(productSpecies);
				if(reaction.getProducts().equals(comp)) {
					coeff = coeff.abs();
					CRNReaction maReaction = new CRNReaction(coeff,reaction.getProducts(),reaction.getReagents(),coeff.toPlainString(),null);
				}
			}
		}
		*/
		
		HashMap<ISpecies, Integer> netStoic = reaction.computeProductsMinusReagentsHashMap();
		/*if(oldSpeciesToNewSpecies!=null){
			HashMap<ISpecies, Integer> newNetStoic=new HashMap<>(netStoic.size());
			for (Entry<ISpecies, Integer> pair : netStoic.entrySet()) {
				newNetStoic.put(oldSpeciesToNewSpecies.get(pair.getKey()), pair.getValue());
			}
			netStoic=newNetStoic;
		}*/

		for (IMonomial monomial : monomials) {
			for(Entry<ISpecies, Integer> entry:netStoic.entrySet()){
				ISpecies species=entry.getKey();
				int stoic=entry.getValue();
				IMonomial monomialTimesStoic=null;
				if(stoic==1){
					monomialTimesStoic=monomial;
				}
				else if(stoic==0){
					continue;
				}
				else{
					monomialTimesStoic=new ProductMonomial(new NumberMonomial(BigDecimal.valueOf(stoic), String.valueOf(stoic)), monomial);
				}
				ICRNReactionAndBoolean newReactionAndUsedI = monomialTimesStoic.toReaction(species,I);
				ICRNReaction newReaction = newReactionAndUsedI.getReaction();
				if(!(newReaction.getRateExpression().equalsIgnoreCase("0")||newReaction.getRateExpression().equalsIgnoreCase("0.0"))){
					usedI = usedI || newReactionAndUsedI.isBoolValue();
					rnReactions.add(newReaction);
				}
			}
			//writeReaction(bw, newReaction);
		}
		
		return usedI;
	}
	
	public static ArrayList<IMonomial> parseGUIPolynomialArbitraryRateExpression(ASTNode node, HashMap<String, ISpecies> speciesNameToSpecies, MathEval math) throws UnsupportedReactionNetworkEncodingException {
		return parseGUIPolynomialODE(node, speciesNameToSpecies, math,null,null);
	}
	
	public static ArrayList<IMonomial> parseGUIPolynomialODE(ASTNode node, HashMap<String, ISpecies> speciesNameToSpecies, MathEval math, String varsName/*, ISpecies[] speciesIdToSpecies*/, ICRN crn) throws UnsupportedReactionNetworkEncodingException {
		//A variable is actually a parameter, while a function (e.g., varsName(1)) is an ODE variable (with id 0).
		if(node.isOperator() && node.getChildCount()==2){
			Type type = node.getType();
			ArrayList<IMonomial> monomialsLeft = parseGUIPolynomialODE(node.getLeftChild(),speciesNameToSpecies,math,varsName/*,speciesIdToSpecies*/,crn);
			ArrayList<IMonomial> monomialsRight = parseGUIPolynomialODE(node.getRightChild(),speciesNameToSpecies,math,varsName/*,speciesIdToSpecies*/,crn);
			if(type.equals(Type.TIMES)){	
				//combine the two
				ArrayList<IMonomial> products = new ArrayList<>(monomialsLeft.size()*monomialsRight.size());
				for (IMonomial left : monomialsLeft) {
					for (IMonomial right : monomialsRight) {
						products.add(new ProductMonomial(left, right));
					}
				}
				return products;
			}
			else if(type.equals(Type.MINUS) ){
				for (IMonomial right : monomialsRight) {
					monomialsLeft.add(new MinusMonomial(right));
				}
				return monomialsLeft;
			}
			else if(type.equals(Type.PLUS) ){
				monomialsLeft.addAll(monomialsRight);
				return monomialsLeft;
			}
			else if(type.equals(Type.POWER)){
				//I support only simple expressions like 'x^2' transforming it in x*x
				if(monomialsLeft.size()==1 && monomialsRight.size()==1) {
					if(monomialsLeft.get(0) instanceof SpeciesMonomial && monomialsRight.get(0) instanceof NumberMonomial) {
						BigDecimal exponent = monomialsRight.get(0).getOrComputeCoefficient();
						int expInt =0;
						try {
							expInt=exponent.intValueExact();
						}catch (ArithmeticException e) {
							//The exponent is not an int
							throw new UnsupportedReactionNetworkEncodingException("The POWER operator is currently supported only for very simple expressions (x^n), with n a natural: "+node.toString());
						}
						if(expInt<0) {
							throw new UnsupportedReactionNetworkEncodingException("The POWER operator is currently supported only for very simple expressions (x^n), with n a natural: "+node.toString());
						}

						ArrayList<IMonomial> ret = new ArrayList<IMonomial>(1);
						if(expInt==0) {
							ret.add(new NumberMonomial(BigDecimal.ONE, "1"));
							return ret;
						}
						else {
							IMonomial product=monomialsLeft.get(0);
							for(int i=2;i<=expInt;i++) {
								product = new ProductMonomial(product, monomialsLeft.get(0));
							}
							ret.add(product);

							return ret;
						}
					}
				}
				else {
					throw new UnsupportedReactionNetworkEncodingException("The POWER operator is currently supported only for very simple expressions: "+node.toString());
				}
				//I might add support for something like(x*y)^(3) -> (x*y)*(x*y)*(x*y)  
			}
			else if(type.equals(Type.DIVIDE)){
				//We support only the case in which we have 
					//(expr1+expr2...)/number
					//(expr1+expr2...)/parameter
				boolean supported=false;
				if(monomialsRight.size()==1 && (monomialsRight.get(0) instanceof NumberMonomial || monomialsRight.get(0) instanceof ParameterMonomial)) {
					IMonomial denominator = monomialsRight.get(0);
					BigDecimal bdd = denominator.getOrComputeCoefficient();
					IMonomial divisionResult = new NumberMonomial(BigDecimal.ONE.divide(bdd,CRNBisimulationsNAry.getSCALE(),CRNBisimulationsNAry.RM), "1"+"/("+denominator.getOrComputeCoefficientExpression()+")");
					ArrayList<IMonomial> ret = new ArrayList<IMonomial>(monomialsLeft.size());
					for(IMonomial numerator : monomialsLeft) {
						ProductMonomial numeratorTimesOneOverDenum = new ProductMonomial(numerator, divisionResult);
						ret.add(numeratorTimesOneOverDenum);
					}
					return ret;
				}
//				if(monomialsLeft.size()==1 &&monomialsRight.size()==1) {
//					IMonomial numerator = monomialsLeft.get(0);
//					IMonomial denominator = monomialsRight.get(0);
//					if(denominator instanceof NumberMonomial) {
//						BigDecimal bdn = numerator.getOrComputeCoefficient();
//						BigDecimal bdd = denominator.getOrComputeCoefficient();
//						IMonomial divisionResult = new NumberMonomial(bdn.divide(bdd,CRNBisimulationsNAry.getSCALE(),CRNBisimulationsNAry.RM), "("+numerator.getOrComputeCoefficientExpression()+")/("+denominator.getOrComputeCoefficientExpression()+")");
//						ArrayList<IMonomial> ret = new ArrayList<IMonomial>(1);
//						ProductMonomial numeratorTimesOneOverDenum = new ProductMonomial(numerator, divisionResult);
//						ret.add(numeratorTimesOneOverDenum);
//						supported=true;
//						return ret;
//					}
//				}
//				else if(monomialsRight.size()==1 && monomialsRight.get(0) instanceof NumberMonomial) {
//					if(((NumberMonomial)(monomialsRight.get(0))).getOrComputeCoefficient().compareTo(BigDecimal.ONE)==0) {
//						return monomialsLeft;
//					}
//				}
				if(!supported) {
					throw new UnsupportedReactionNetworkEncodingException("The DIVIDE operator is currently not supported: "+node.toString());
				}
				//(x*y)^(3)
				//I have to transform the power in a product (if possible, or rise an error)  
			}
		}
		else if(node.isOperator() && node.getChildCount()==1 && node.getType().equals(Type.MINUS)){

			ArrayList<IMonomial> monomials = parseGUIPolynomialODE(node.getLeftChild(),speciesNameToSpecies,math,varsName/*,speciesIdToSpecies*/,crn);

			ArrayList<IMonomial> ret = new ArrayList<>(monomials.size());
			for (IMonomial mon : monomials) {
				ret.add(new MinusMonomial(mon));
			}
			return ret;
		}
		else if(varsName!=null /*&& speciesIdToSpecies!=null*/ && node.isFunction() && node.getName().equals(varsName)){
			int id = node.getListOfNodes().get(0).getInteger();
			ArrayList<IMonomial> ret = new ArrayList<IMonomial>(1);
			//ret.add(new SpeciesMonomial(speciesIdToSpecies[id-1]));
			ret.add(new SpeciesMonomial(crn.getSpecies().get(id-1)));
			return ret;
		}
		else if(node.isVariable()){
			ISpecies species;
			if(node.getName().equals(EpsilonDifferentialEquivalences.NAME_OFSWAPPING_SPECIES)){
				species=EpsilonDifferentialEquivalences.SWAPPING_SPECIES;
			}
			else{
				species = speciesNameToSpecies.get(node.getName());
			}			
			if(species!=null){
				ArrayList<IMonomial> ret = new ArrayList<IMonomial>(1);
				ret.add(new SpeciesMonomial(species));
				return ret;
			}
			else{
				ArrayList<IMonomial> ret = new ArrayList<IMonomial>(1);
				double d = math.evaluate(node.getName());
				BigDecimal bd = BigDecimal.valueOf(d);
				ret.add(new ParameterMonomial(bd, node.getName()));
				return ret;
			}
		}
		/*else if(node.isVariable()){
			ArrayList<IMonomial> ret = new ArrayList<IMonomial>(1);
			ret.add(new NumberMonomial(getMath().evaluate(node.getName())));
			return ret;
		}*/
		else if(node.isNumber()){
			ArrayList<IMonomial> ret = new ArrayList<IMonomial>(1);
			ret.add(new NumberMonomial(BigDecimal.valueOf(node.getReal()),String.valueOf(node.getReal())));
			return ret;
		}

		throw new UnsupportedReactionNetworkEncodingException(node.toString());

	}
	
	
	
	private static void writeReaction(BufferedWriter bw, ICRNReaction reaction, boolean originalNames) throws IOException {
		bw.write("  ");
		if(originalNames) {
			bw.write(reaction.getReagents().toMultiSetWithStoichiometriesOrigNames(true));
		}
		else {
			bw.write(reaction.getReagents().toMultiSetWithStoichiometries(true));
		}
		

		bw.write(" -> ");
		if(originalNames) {
			bw.write(reaction.getProducts().toMultiSetWithStoichiometriesOrigNames(true));
		}
		else {
			bw.write(reaction.getProducts().toMultiSetWithStoichiometries(true));
		}

		bw.write(" , ");
		if(reaction.hasArbitraryKinetics()){
			bw.write("arbitrary ");
		}
		bw.write(reaction.getRateExpression());
		
		if(reaction.getID()!=null && !reaction.getID().equals("")){
			bw.write(" [");
			bw.write(reaction.getID());
			bw.write("]");
		}
		
		bw.write("\n");
	}
	
	public static String computeMassActionFiringRate(ICRNReaction reaction,boolean ignoreOnes,boolean ignoreIIfMassAction,boolean keepExpressionInMA){
		String firingRate=reaction.getReagents().getMassActionExpression(false,ignoreIIfMassAction);
		return multiplyMassActionByTheRate(reaction, ignoreOnes, firingRate,keepExpressionInMA);
	}
	
	private static String computeMassActionFiringRateReplacingSpeciesNames(ICRNReaction reaction,String variablePrefix, int idIncrement, String variableSuffix,HashMap<String, ISpecies> speciesNameToSpecies) throws ParseException{
		String firingRate=reaction.getReagents().getMassActionExpressionReplacingSpeciesNames(variablePrefix, idIncrement,variableSuffix);
		return multiplyMassActionByTheRate(reaction, firingRate,variablePrefix,idIncrement,variableSuffix,speciesNameToSpecies);
	}
	/*
	private static String computeMassActionFiringRateReplacingSpeciesNames(ICRNReaction reaction,boolean ignoreOnes, String variableAsfunction, int idIncrement, boolean writeParenthesis, String suffix){
		return computeMassActionFiringRateReplacingSpeciesNames(reaction,ignoreOnes,variableAsfunction,idIncrement,writeParenthesis,suffix,false);
	}
	*/
	private static String computeMassActionFiringRateReplacingSpeciesNames(ICRNReaction reaction,boolean ignoreOnes, String variableAsfunction, int idIncrement, boolean writeParenthesis, String suffix, boolean squaredParenthesis){
		String firingRate=reaction.getReagents().getMassActionExpressionReplacingSpeciesNames(variableAsfunction, idIncrement,writeParenthesis,suffix,squaredParenthesis);
		return multiplyMassActionByTheRate(reaction, ignoreOnes, firingRate,true);
	}
	protected static String multiplyMassActionByTheRate(ICRNReaction reaction, String firingRate,String variablePrefix,int idIncrement,String variableSuffix,HashMap<String, ISpecies> speciesNameToSpecies) throws ParseException {
		String rateExpression="";
		rateExpression = replaceSpeciesNameInRate(reaction, variablePrefix, idIncrement, variableSuffix,speciesNameToSpecies);
		
		firingRate = addParIfNecessary(rateExpression) +"*" +firingRate;
		
		return firingRate;
	}
	public static String replaceSpeciesNameInRate(ICRNReaction reaction, String variablePrefix, int idIncrement,
			String variableSuffix, HashMap<String, ISpecies> speciesNameToSpecies) throws ParseException {
		String rateExpression;
		if(reaction instanceof CRNReactionArbitraryAbstract){
			ASTNode node = ((CRNReactionArbitraryAbstract) reaction).replaceVar(variablePrefix, variableSuffix, idIncrement, speciesNameToSpecies);
			rateExpression=node.toFormula();
		}
		else{
			ASTNode node=ASTNode.parseFormula(reaction.getRateExpression());
			CRNReactionArbitraryGUI.replaceVar(node, variablePrefix, variableSuffix, idIncrement, speciesNameToSpecies);
			rateExpression=node.toFormula();
		}
		return rateExpression;
	}
	protected static String multiplyMassActionByTheRate(ICRNReaction reaction, boolean ignoreOnes, String firingRate, boolean keepExpression) {
		
		
		if(ignoreOnes && reaction.getRate().compareTo(BigDecimal.ONE)==0){
			//do nothing
		}
		else if(ignoreOnes && reaction.getRate().compareTo(BigDecimal.valueOf(-1))==0){
			firingRate = "-" +firingRate;
		} 
		else{
			if(keepExpression) {
				firingRate = addParIfNecessary(reaction.getRateExpression()) +"*" +firingRate;
			}
			else {
				firingRate = reaction.getRate() +"*" +firingRate;
			}
		}
		return firingRate;
	}
	
	public static HashMap<ISpecies, StringBuilder> computeDriftsReplacingSpeciesNames(ICRN crn,String variablePrefix, int idIncrement,  String variableSuffix, HashMap<String, ISpecies> speciesNameToSpecies) throws ParseException {
		HashMap<ISpecies, StringBuilder> speciesToDrift = new LinkedHashMap<ISpecies, StringBuilder>(crn.getSpecies().size());
		for (ICRNReaction reaction : crn.getReactions()) {
			String firingRate="0";
			if(reaction.hasArbitraryKinetics()){
				firingRate=replaceSpeciesNameInRate(reaction, variablePrefix, idIncrement, variableSuffix, speciesNameToSpecies);
			}
			else{
				firingRate=computeMassActionFiringRateReplacingSpeciesNames(reaction,variablePrefix,idIncrement,variableSuffix,speciesNameToSpecies);
			}
			addFiringRateToInvolvedODEs(speciesToDrift, reaction, firingRate);
		}
		return speciesToDrift;
	}
	/*
	 * protected static String multiplyMassActionByTheRate(ICRNReaction reaction, String firingRate,String variablePrefix,int idIncrement,String variableSuffix,HashMap<String, ISpecies> speciesNameToSpecies) throws ParseException {
		String rateExpression="";
		if(reaction instanceof CRNReactionArbitraryAbstract){
			ASTNode pippo = ((CRNReactionArbitraryAbstract) reaction).replaceVar(variablePrefix, variableSuffix, idIncrement, speciesNameToSpecies);
			rateExpression=pippo.toFormula();
		}
		else{
			ASTNode node=ASTNode.parseFormula(reaction.getRateExpression());
			CRNReactionArbitraryGUI.replaceVar(node, variablePrefix, variableSuffix, idIncrement, speciesNameToSpecies);
			rateExpression=node.toFormula();
		}
		
		firingRate = addParIfNecessary(rateExpression) +"*" +firingRate;
		
		return firingRate;
	}
	 */
	
	public static HashMap<ISpecies, StringBuilder> computeDriftsReplacingSpeciesNames(ICRN crn,boolean ignoreOnes, String variableAsfunction, int idIncrement, boolean writeParenthesis, String suffix) {
		return computeDriftsReplacingSpeciesNames(crn,ignoreOnes, variableAsfunction, idIncrement, writeParenthesis, suffix,false);
	}
	
	public static HashMap<ISpecies, StringBuilder> computeDriftsReplacingSpeciesNames(ICRN crn,boolean ignoreOnes, String variableAsfunction, int idIncrement, boolean writeParenthesis, String suffix,boolean squaredParenthesis) {
		if(!crn.isMassAction()){
			return null;
		}
		HashMap<ISpecies, StringBuilder> speciesToDrift = new LinkedHashMap<ISpecies, StringBuilder>(crn.getSpecies().size());
		for (ICRNReaction reaction : crn.getReactions()) {
			if(reaction.hasArbitraryKinetics()){
				return null;
			}
			String firingRate=computeMassActionFiringRateReplacingSpeciesNames(reaction,ignoreOnes,variableAsfunction,idIncrement,writeParenthesis,suffix,squaredParenthesis);
			addFiringRateToInvolvedODEs(speciesToDrift, reaction, firingRate);
		}
		
		
		return speciesToDrift;
	}
	
	public static String computeFiringRateForInvolvedODEs(IComposite net, int s,String firingRate) {
			int mult = net.getMultiplicities(s);
			
			if(mult==1){
				return firingRate;
			}
			else if(mult==-1){
				//drift.append("-("+firingRate+")");
				return "-"+addParIfNecessary(firingRate);
			}
			else {
				return String.valueOf(mult) + "*("+firingRate+")";
			}
	}
	
	protected static void addFiringRateToInvolvedODEs(HashMap<ISpecies, StringBuilder> speciesToDrift,
			ICRNReaction reaction, String firingRate) {
		IComposite net = reaction.computeProductsMinusReagents();
		for(int s=0;s<net.getNumberOfDifferentSpecies();s++){
			StringBuilder drift = speciesToDrift.get(net.getAllSpecies(s));
			if(drift==null){
				drift=new StringBuilder();
				speciesToDrift.put(net.getAllSpecies(s), drift);
			}
			else{
				drift.append(" + ");
			}
			
			String firingRateOfS=computeFiringRateForInvolvedODEs(net, s, firingRate);
			drift.append(firingRateOfS);
//			int mult = net.getMultiplicities(s);			
//			if(mult==1){
//				drift.append(firingRate);
//			}
//			else if(mult==-1){
//				//drift.append("-("+firingRate+")");
//				drift.append("-"+addParIfNecessary(firingRate));
//			}
//			else {
//				drift.append(String.valueOf(mult));
//				drift.append("*(");
//				drift.append(firingRate);
//				drift.append(")");
//			}
		}
	}
	
	public static LinkedHashMap<ISpecies, StringBuilder> computeDrifts(ICRN crn,boolean ignoreOnes,boolean ignoreIIfMassAction) {
		LinkedHashMap<ISpecies, StringBuilder> speciesToDrift = new LinkedHashMap<ISpecies, StringBuilder>(crn.getSpecies().size());
		for (ICRNReaction reaction : crn.getReactions()) {
			String firingRate=reaction.getRateExpression();
			if(!reaction.hasArbitraryKinetics()){
				//firingRate =  reaction.getRateExpression()+"*"+reaction.getReagents().getMassActionExpression(false);
				firingRate=computeMassActionFiringRate(reaction,ignoreOnes,ignoreIIfMassAction,true);
			}
			addFiringRateToInvolvedODEs(speciesToDrift, reaction, firingRate);
		}
		return speciesToDrift;
	}
	
	public static String addParIfNecessary(String expr) {
		boolean add=false;
		String exprCopy = expr;
		//I ignore the initial '-' for negative sign
		if(expr.startsWith("-")) {
			exprCopy=exprCopy.substring(1);
		}
		//String[] operators = new String[]{"+","*","-","/","^","max(","min(","abs("};
		String[] operators = new String[]{"+","-"};
		for (String operator : operators) {
			if(exprCopy.contains(operator)){
				add=true;
				break;
			}
		}
		if(add){
			return "("+expr+")";
		}
		else{
			return expr;
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
	
	
	private static List<String> sampleCommands;
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
			toBeFilled.add("ERODE's Manual");
		}
		else{
			toBeFilled=sampleCommandsShort;
			toBeFilled.add("ERODE's Help");
		}
	
		toBeFilled.add("Some sample commands:");
	
		toBeFilled.add(" To have a short help.");
		toBeFilled.add(" help\n");
	
		toBeFilled.add(" To have a detailed description of ERODE's commands and features.");
		toBeFilled.add(" man\n");
	
		toBeFilled.add(" Print minimal information about the current CRN.");
		toBeFilled.add(" print\n");
	
		toBeFilled.add(" Write a CRN in a .crn file."); 
		toBeFilled.add(" write({fileOut=outputfileName})"+suffix);
		if(detailed){
			toBeFilled.add("  [fileOut] Mandatory. The name of the file where to write the current CRN.");
		}
	
		toBeFilled.add(" Import a CRN from a .net file (supported by the BioNteGen tool, version 2.2.5).");
		toBeFilled.add(" importBNG({fileIn=./BNGNetworks/mmc1.net,copyInEditor=true})"+suffix);
		if(detailed){
			toBeFilled.add("  [fileIn] Mandatory. The file to be loaded.\n");
			toBeFilled.add("  [copyInEditor] Optional. If set to true, this command will be replaced by the .crn encoding of the imported model.\n");
		}
	
		toBeFilled.add(" Export a CRN in a .net file  (supported by the BioNteGen tool version 2.2.5)."); 
		toBeFilled.add(" exportBNG({fileOut=outputfileName})"+suffix);
		if(detailed){
			toBeFilled.add("  [fileOut] Mandatory. The name of the file where to write the current CRN.");
		}
	
		toBeFilled.add(" Import a model from an SBML file representing the BoolCube ODE system of a boolean circuit network (the SBML file is assumed to be generated by ODIFY).");
		toBeFilled.add(" importBoolCubeSBML({fileIn=./BoolCubeSBMLNetworks/feedForwardBooleCube.sbml,copyInEditor=true})"+suffix);
		if(detailed){
			toBeFilled.add("  [fileIn] Mandatory. The file to be loaded.\n");
			toBeFilled.add("  [copyInEditor] Optional. If set to true, this command will be replaced by the .crn encoding of the imported model.\n");
		}
	
		toBeFilled.add(" Import a model by encoding a system of polynomial differential equations written in Matlab.");
		toBeFilled.add(" importMatlabODEs({fileIn=./Matlab/model.m,polynomialODEs=true,copyInEditor=true})"+suffix);
		if(detailed){
			toBeFilled.add("  [fileIn] Mandatory. The file to be loaded.\n");
			toBeFilled.add("  [polynomialODEs] Optional. Specifies if the ODEs are \"polynomial ODEs\", that is if their drifts consist of sums of polinomials (products of reals and ODE variables). These ODEs will then be transformed in mass-action reaction networks, for which efficient partition refinement algorithms are implemented.\n");
			toBeFilled.add("  [copyInEditor] Optional. If set to true, this command will be replaced by the .crn encoding of the imported model.\n");
		}
	
		toBeFilled.add(" Import a CRN from a .inp file (ChemKin format).");
		toBeFilled.add(" importChemKin({fileIn=./ChemKinNetworks/example1.inp,thermoDynamicFile=./ChemKinNetworks/example1.dat,copyInEditor=true})"+suffix);
		if(detailed){
			toBeFilled.add("  [fileIn] Mandatory. The file to be loaded.\n");
			toBeFilled.add("  [thermoDynamicFile] Optional. If provided, it contains the name of the file containing thermodynamic data. If not provided, fileIn is used (after replacing .inp or .CKI with .dat)\n");
			toBeFilled.add("  [copyInEditor] Optional. If set to true, this command will be replaced by the .crn encoding of the imported model.\n");
		}
	
		toBeFilled.add(" Import a CRN from a .tra file (explpicit format for CTMCs used by MRMC).");
		toBeFilled.add(" importMRMC({fileIn=./MRMCMarkovChains/example1.tra,labellingFile=./MRMCMarkovChains/example1.lab,copyInEditor=true})"+suffix);
		if(detailed){
			toBeFilled.add("  [fileIn] Mandatory. The file to be loaded.\n");
			toBeFilled.add("  [labellingFile] Optional. If provided, it contains the name of the file containing the labelling function of the Markov chain. If the value \"same\" is provided, then fileIn is used (after replacing .tra with .label). Such information will be encoded as views, which can in turn be used to prepartition the initial partition\n");
			toBeFilled.add("  [copyInEditor] Optional. If set to true, this command will be replaced by the .crn encoding of the imported model.\n");
		}
	
		toBeFilled.add(" Import a model from a matrix in a compact .csv file (assuming it is a linear system,copyInEditor=true). ");
		toBeFilled.add(" importLinearSystemAsCCSVMatrix({fileIn=./CompactMatrices/example1.csv,form=PQ})"+suffix);
		if(detailed){
			toBeFilled.add("  [fileIn] Mandatory. The file to be loaded.\n");
			toBeFilled.add("  [form] Mandatory. Can be either A*X (linear systems), P*Q (Markov chain) or FEM (finite element methods).\n");
			toBeFilled.add("  [copyInEditor] Optional. If set to true, this command will be replaced by the .crn encoding of the imported model.\n");
		}
	
	
		toBeFilled.add(" Import a CRN from an LBS file.");
		toBeFilled.add(" importLBS({fileIn=./LBSNetworks/network1.lbs,copyInEditor=true})"+suffix);
		if(detailed){
			toBeFilled.add("  [fileIn] Mandatory. The file to be loaded.\n");
			toBeFilled.add("  [copyInEditor] Optional. If set to true, this command will be replaced by the .crn encoding of the imported model.\n");
		}
	
		toBeFilled.add(" Export a CRN in a LBS file."); 
		toBeFilled.add(" exportLBS({fileOut=outputfileName})"+suffix);
		if(detailed){
			toBeFilled.add("  [fileOut] Mandatory. The name of the file where to write the current CRN.");
		}
	
		toBeFilled.add(" Export a CRN in a SBML file."); 
		toBeFilled.add(" exportSBML({fileOut=outputfileName})"+suffix);
		if(detailed){
			toBeFilled.add("  [fileOut] Mandatory. The name of the file where to write the current CRN.");
		}
	
		toBeFilled.add(" Export a CRN in a FlyFast file."); 
		toBeFilled.add(" exportFlyFast({fileOut=outputfileName})"+suffix);
		if(detailed){
			toBeFilled.add("  [fileOut] Mandatory. The name of the file where to write the current CRN.");
		}
	
		toBeFilled.add(" Reduce a model using Forward CRN Bisimulation. If it is prefixed by \"this=\", then the loaded CRN is updated to the obtained one.");
		//toBeFilled.add(" reduce({fileIn=inputfileName.net,technique=dsb,reducedFile=outputFileNameOfReducedCRN.net,groupedFile=outputFileNameOfGroupedCRN.net,sameICFile=outputFileNameOfCRNWithSameICPerBlock.net})");
		toBeFilled.add(" reduceFB({reducedFile=outputFileNameOfReducedCRN.crn,prePartition=NONE,groupedFile=outputFileNameOfGroupedCRN.crn,sameICFile=outputFileNameOfCRNWithSameICPerBlock.crn,computeOnlyPartition=false,fileWhereToStorePartition=outputFileToStorePartition.txt,printInfo=true})"+suffix);
		if(detailed){
			toBeFilled.add("  [prePartition] Optional (default value=NONE). If set to IC, the initial partition is first refined in blocks of species with same initial concentrantions. If set to VIEWS, the initial partition is coherent with the specified groups/views in the original file (if they correspond to a partition, i.e. views are just disjoint sums of distinct species)");
			toBeFilled.add("  [reducedFile] Optional. If this parameter is specified, the reduced model is written in the file with the provided name.");
			toBeFilled.add("  [groupedFile] Optional. If this parameter is specified, the original model is written in the file with the provided name. Also, a view per block of the obtained partition is created, grouping (i.e., summing) all species belonging to the block.");
			toBeFilled.add("  [sameICFile] Optional. If this parameter is specified, the original model is written in the file with the provided name. However, the same concentration is assigned to all the species of each block of the obtained partition.");
			toBeFilled.add("  [computeOnlyPartition] Optional (default value=false). If true, the reduced model is not actually computed, but only the coarsest partition.\n");
			toBeFilled.add("  [fileWhereToStorePartition] Optional. If specified, information on the computed partition will be stored in the given text file.\n");
			toBeFilled.add("  [printInfo] Optional (default value=true). If set to true, some information is print in the console.\n");
		}
	
		toBeFilled.add(" Reduce a CRN using N-ary Forward CRN Bisimulation. If it is prefixed by \"this=\", then the loaded CRN is updated to the obtained one.");
		//toBeFilled.add(" reduce({fileIn=inputfileName.net,technique=dsb,reducedFile=outputFileNameOfReducedCRN.net,groupedFile=outputFileNameOfGroupedCRN.net,sameICFile=outputFileNameOfCRNWithSameICPerBlock.net})");
		toBeFilled.add(" reduceFE({reducedFile=outputFileNameOfReducedCRN.crn,prePartition=NONE,groupedFile=outputFileNameOfGroupedCRN.crn,sameICFile=outputFileNameOfCRNWithSameICPerBlock.crn,computeOnlyPartition=false,fileWhereToStorePartition=outputFileToStorePartition.txt,printInfo=true})"+suffix);
		if(detailed){
			toBeFilled.add("  [prePartition] Optional (default value=NONE). If set to IC, the initial partition is first refined in blocks of species with same initial concentrantions. If set to VIEWS, the initial partition is coherent with the specified groups/views in the original file (if they correspond to a partition, i.e. views are just disjoint sums of distinct species)");
			toBeFilled.add("  [reducedFile] Optional. If this parameter is specified, the reduced model is written in the file with the provided name.");
			toBeFilled.add("  [groupedFile] Optional. If this parameter is specified, the original model is written in the file with the provided name. Also, a view per block of the obtained partition is created, grouping (i.e., summing) all species belonging to the block.");
			toBeFilled.add("  [sameICFile] Optional. If this parameter is specified, the original model is written in the file with the provided name. However, the same concentration is assigned to all the species of each block of the obtained partition.");
			toBeFilled.add("  [computeOnlyPartition] Optional (default value=false). If true, the reduced model is not actually computed, but only the coarsest partition.\n");
			toBeFilled.add("  [fileWhereToStorePartition] Optional. If specified, information on the computed partition will be stored in the given text file.\n");
			toBeFilled.add("  [printInfo] Optional (default value=true). If set to true, some information is print in the console.\n");
		}
	
		toBeFilled.add(" Reduce a CRN using Syntactc Markovian Bisimulation. If it is prefixed by \"this=\", then the loaded CRN is updated to the obtained one.");
		toBeFilled.add(" reduceSMB({reducedFile=outputFileNameOfReducedCRN.crn,prePartition=NONE,groupedFile=outputFileNameOfGroupedCRN.crn,sameICFile=outputFileNameOfCRNWithSameICPerBlock.crn,computeOnlyPartition=false,fileWhereToStorePartition=outputFileToStorePartition.txt,printInfo=true})"+suffix);
		if(detailed){
			toBeFilled.add("  [prePartition] Optional (default value=NONE). If set to IC, the initial partition is first refined in blocks of species with same initial concentrantions. If set to VIEWS, the initial partition is coherent with the specified groups/views in the original file (if they correspond to a partition, i.e. views are just disjoint sums of distinct species)");
			toBeFilled.add("  [reducedFile] Optional. If this parameter is specified, the reduced model is written in the file with the provided name.");
			toBeFilled.add("  [groupedFile] Optional. If this parameter is specified, the original model is written in the file with the provided name. Also, a view per block of the obtained partition is created, grouping (i.e., summing) all species belonging to the block.");
			toBeFilled.add("  [sameICFile] Optional. If this parameter is specified, the original model is written in the file with the provided name. However, the same concentration is assigned to all the species of each block of the obtained partition.");
			toBeFilled.add("  [computeOnlyPartition] Optional (default value=false). If true, the reduced model is not actually computed, but only the coarsest partition.\n");
			toBeFilled.add("  [fileWhereToStorePartition] Optional. If specified, information on the computed partition will be stored in the given text file.\n");
			toBeFilled.add("  [printInfo] Optional (default value=true). If set to true, some information is print in the console.\n");
		}
	
	
		toBeFilled.add(" Reduce a CRN using Backward Bisimulation. If it is prefixed by \"this=\", then the loaded CRN is updated to the obtained one.");
		toBeFilled.add(" reduceBB({reducedFile=outputFileNameOfReducedCRN.crn,prePartition=NONE,groupedFile=outputFileNameOfGroupedCRN.crn,sameICFile=outputFileNameOfCRNWithSameICPerBlock.crn,computeOnlyPartition=false,fileWhereToStorePartition=outputFileToStorePartition.txt,printInfo=true})"+suffix);
		if(detailed){
			toBeFilled.add("  [prePartition] Optional (default value=IC). If set to IC, the initial partition is first refined in blocks of species with same initial concentrantions. If set to VIEWS, the initial partition is coherent with the specified groups/views in the original file (if they correspond to a partition, i.e. views are just disjoint sums of distinct species)");
			toBeFilled.add("  [reducedFile] Optional. If this parameter is specified, the reduced model is written in the file with the provided name.");
			toBeFilled.add("  [groupedFile] Optional. If this parameter is specified, the original model is written in the file with the provided name. Also, a view per block of the obtained partition is created, grouping (i.e., summing) all species belonging to the block.");
			toBeFilled.add("  [sameICFile] Optional. If this parameter is specified, the original model is written in the file with the provided name. However, the same concentration is assigned to all the species of each block of the obtained partition.");
			toBeFilled.add("  [computeOnlyPartition] Optional (default value=false). If true, the reduced model is not actually computed, but only the coarsest partition.\n");
			toBeFilled.add("  [fileWhereToStorePartition] Optional. If specified, information on the computed partition will be stored in the given text file.\n");
			toBeFilled.add("  [printInfo] Optional (default value=true). If set to true, some information is print in the console.\n");
		}
	
		toBeFilled.add(" Reduce a CRN using N-ary Backward Bisimulation. If it is prefixed by \"this=\", then the loaded CRN is updated to the obtained one.");
		toBeFilled.add(" reduceBE({reducedFile=outputFileNameOfReducedCRN.crn,prePartition=NONE,groupedFile=outputFileNameOfGroupedCRN.crn,sameICFile=outputFileNameOfCRNWithSameICPerBlock.crn,computeOnlyPartition=false,fileWhereToStorePartition=outputFileToStorePartition.txt,printInfo=true})"+suffix);
		if(detailed){
			toBeFilled.add("  [prePartition] Optional (default value=IC). If set to IC, the initial partition is first refined in blocks of species with same initial concentrantions. If set to VIEWS, the initial partition is coherent with the specified groups/views in the original file (if they correspond to a partition, i.e. views are just disjoint sums of distinct species)");
			toBeFilled.add("  [reducedFile] Optional. If this parameter is specified, the reduced model is written in the file with the provided name.");
			toBeFilled.add("  [groupedFile] Optional. If this parameter is specified, the original model is written in the file with the provided name. Also, a view per block of the obtained partition is created, grouping (i.e., summing) all species belonging to the block.");
			toBeFilled.add("  [sameICFile] Optional. If this parameter is specified, the original model is written in the file with the provided name. However, the same concentration is assigned to all the species of each block of the obtained partition.");
			toBeFilled.add("  [computeOnlyPartition] Optional (default value=false). If true, the reduced model is not actually computed, but only the coarsest partition.\n");
			toBeFilled.add("  [fileWhereToStorePartition] Optional. If specified, information on the computed partition will be stored in the given text file.\n");
			toBeFilled.add("  [printInfo] Optional (default value=true). If set to true, some information is print in the console.\n");
		}
	
		toBeFilled.add(" Reduce a CRN using Backward Differntial Equivalence (Exact Fluid Lumpability). The SMT solver Microsoft z3 is exploited. If it is prefixed by \"this=\", then the loaded CRN is updated to the obtained one.");
		toBeFilled.add(" reduceBDE({reducedFile=outputFileNameOfReducedCRN.crn,prePartition=NONE,groupedFile=outputFileNameOfGroupedCRN.crn,sameICFile=outputFileNameOfCRNWithSameICPerBlock.crn,computeOnlyPartition=false,fileWhereToStorePartition=outputFileToStorePartition.txt,printInfo=true})"+suffix);
		if(detailed){
			toBeFilled.add("  [prePartition] Optional (default value=IC). If set to IC, the initial partition is first refined in blocks of species with same initial concentrantions. If set to VIEWS, the initial partition is coherent with the specified groups/views in the original file (if they correspond to a partition, i.e. views are just disjoint sums of distinct species)");
			toBeFilled.add("  [reducedFile] Optional. If this parameter is specified, the reduced model is written in the file with the provided name.");
			toBeFilled.add("  [groupedFile] Optional. If this parameter is specified, the original model is written in the file with the provided name. Also, a view per block of the obtained partition is created, grouping (i.e., summing) all species belonging to the block.");
			toBeFilled.add("  [sameICFile] Optional. If this parameter is specified, the original model is written in the file with the provided name. However, the same concentration is assigned to all the species of each block of the obtained partition.");
			toBeFilled.add("  [computeOnlyPartition] Optional (default value=false). If true, the reduced model is not actually computed, but only the coarsest partition.\n");
			toBeFilled.add("  [fileWhereToStorePartition] Optional. If specified, information on the computed partition will be stored in the given text file.\n");
			toBeFilled.add("  [printInfo] Optional (default value=true). If set to true, some information is print in the console.\n");
		}
	
		toBeFilled.add(" Reduce a CRN using Forward Differential Equivalence 0(Ordinary Fluid Lumpability). The SMT solver Microsoft z3 is exploited. If it is prefixed by \"this=\", then the loaded CRN is updated to the obtained one.");
		toBeFilled.add(" reduceFDE({reducedFile=outputFileNameOfReducedCRN.crn,prePartition=NONE,groupedFile=outputFileNameOfGroupedCRN.crn,sameICFile=outputFileNameOfCRNWithSameICPerBlock.crn,computeOnlyPartition=false,fileWhereToStorePartition=outputFileToStorePartition.txt,printInfo=true})"+suffix);
		if(detailed){
			toBeFilled.add("  [prePartition] Optional (default value=NONE). If set to IC, the initial partition is first refined in blocks of species with same initial concentrantions. If set to VIEWS, the initial partition is coherent with the specified groups/views in the original file (if they correspond to a partition, i.e. views are just disjoint sums of distinct species)");
			toBeFilled.add("  [reducedFile] Optional. If this parameter is specified, the reduced model is written in the file with the provided name.");
			toBeFilled.add("  [groupedFile] Optional. If this parameter is specified, the original model is written in the file with the provided name. Also, a view per block of the obtained partition is created, grouping (i.e., summing) all species belonging to the block.");
			toBeFilled.add("  [sameICFile] Optional. If this parameter is specified, the original model is written in the file with the provided name. However, the same concentration is assigned to all the species of each block of the obtained partition.");
			toBeFilled.add("  [computeOnlyPartition] Optional (default value=false). If true, the reduced model is not actually computed, but only the coarsest partition.\n");
			toBeFilled.add("  [fileWhereToStorePartition] Optional. If specified, information on the computed partition will be stored in the given text file.\n");
			toBeFilled.add("  [printInfo] Optional (default value=true). If set to true, some information is print in the console.\n");
		}
	
	
		toBeFilled.add(" Simulate the ODEs. ");
		toBeFilled.add(" simulateODE({tEnd=200,steps=10,minStep=1.0e-8,maxStep=100.0,absTol=1.0e-10,relTol=1.0e-10,visualizePlot=false,showLabels=false,csvFile=fileNameWhereToSaveCSVValues});"+suffix);//,imageFile=fileNameWhereToPlot
		if(detailed){
			toBeFilled.add("  [tEnd] Mandatory. End time of simulation.");
			toBeFilled.add("  [steps] Optional (default value=100). Number of observed points.");
			toBeFilled.add("  [visualizePlot] Optional (default value=false). If true, an interactive plot is visualized.");
			toBeFilled.add("  [minStep] Optional (default value=1.0e-8). Minimal step (sign is irrelevant), the last step can be smaller than this.");
			toBeFilled.add("  [maxStep] Optional (default value=100.0). Maximal step (sign is irrelevant), the last step can be smaller than this.");
			toBeFilled.add("  [absTol] Optional (default value=1.0e-8). Allowed absolute error.");
			toBeFilled.add("  [relTol] Optional (default value=1.0e-8). Allowed relative error.");
			toBeFilled.add("  [visualizePlot] Optional (default value=true). If true, an interactive plot is visualized.");
			toBeFilled.add("  [showLabels] Optional (default value=true). If true, the labels of the interactive plot will be visualized.");
			toBeFilled.add("  [csvFile] Optional. The name of the file where to save results of the simulation, in format comma-separated value.");
			//toBeFilled.add("  [covariances] Optional (default value=false). If set to true, not just the means of the concentrations are computed, but also their variances.\n");
		}
	
		toBeFilled.add(" Simulate the CTMC of a CRN. ");
		toBeFilled.add(" simulateCTMC({tEnd=200,steps=10,method=ssa,repeats=100,visualizePlot=false,showLabels=false,csvFile=fileNameWhereToSaveCSVValues})."+suffix);//imageFile=fileNameWhereToPlot,
		if(detailed){
			toBeFilled.add("  [tEnd] Mandatory. End time of simulation.");
			toBeFilled.add("  [steps] Optional (default value=100). Number of observed points.");
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
			toBeFilled.add("  [csvFile] Optional. The name of the file where to save results of the simulation, in format comma-separated value.");
		}
	
//		toBeFilled.add(" Estimate the means of the species populations, or of more complex queries, resorting to the statistical model checker MultiVeStA. ");
//		toBeFilled.add(" multivestaSMC({alpha=0.05,delta=0.2,maxTime=200,query=query.quatex,steps=10,method=ssa,parallelism=2,visualizePlot=false,showLabels=false,csvFile=fileNameWhereToSaveCSVValues})."+suffix);//imageFile=fileNameWhereToPlot,
//		//multivestaSMC({fileIn=./CRNNetworks/amSameRates.crn,maxTime=5,steps=50,parallelism=2,alpha=0.05,delta=0.2,visualizePlot=true}).
//		if(detailed){
//			toBeFilled.add("  [alpha] and [delta] Mandatory. The confidence interval which the estimations must respect: for each estimated mean \"x\", the actual mean belong to x+-delta/2 with probability (1-alpha)*100. ");
//			toBeFilled.add("  [maxTime] Mandatory. Maximum simulated time."); //TODO Why mandatory?
//			//toBeFilled.add("  [query] Optinal (default value=allSpecies). The MultiQuaTEx expression specifying the properties to be estimated. The three keywords \"allSpecies,allViews,allSpeciesAndViews\" trigger the automatic generation of a query studying the mean population of each species, view or both, respectively, at the varying of time.");
//			toBeFilled.add("  [query] Optinal (default value=allSpecies). The MultiQuaTEx expression specifying the properties to be estimated. The three keywords \""+
//					FERNState.allPopulationsMultiQuaTExExpressionFileNameWithoutExtension+","+
//					FERNState.allViewsMultiQuaTExExpressionFileNameWithoutExtension+","+
//					FERNState.allPopulationsAndViewsMultiQuaTExExpressionFileNameWithoutExtension+"\" trigger the automatic generation of a query studying the mean population of each species, view or both, respectively, at the varying of time.");
//			toBeFilled.add("  [steps] Optional (min value=2). Number of observed points. Used to generate the observation points of the three automatically generated queries. It is ognored if an actual query is provided.");
//			toBeFilled.add("  [method] Optional (default value=nextReaction) Seven different simulation methods are supported: ");
//			toBeFilled.add("   ssa               (direct method by Gillepsie).");
//			toBeFilled.add("   ssa+              (uses dependency graphs to improve the runtime of the original Gillepsie algorithm).");
//			toBeFilled.add("   nextReaction      (next reaction method by Gibson and Bruck).");
//			toBeFilled.add("   tauLeapingAbs     (Tau-leaping algorithm. The error is bounded by the sum of all propensity functions).");
//			toBeFilled.add("   tauLeapingRelProp (Tau-leaping algorithm. The error is bounded by the relative change in the individual propensity functions).");
//			toBeFilled.add("   tauLeapingRelPop  (Tau-leaping algorithm. The error is bounded by the relative changes in the molecular populations).");
//			toBeFilled.add("   maximalTimeStep   (Maximal time step method by Puchalka. Automatic paritioning into slow and fast reactions, which are fired according to an exact and tau leaping method, respectively).");
//			toBeFilled.add("  [parallelism] Optional (default value=1). Distribute the simulations in different processes, allowing to exploit Multi-Core architectures.");
//			toBeFilled.add("  [visualizePlot] Optional (default value=true). If true, an interactive plot containing the estimated means and variances is visualized.");
//			toBeFilled.add("  [showLabels] Optional (default value=true). If true, the labels of the interactive plot will be visualized.");
//			//toBeFilled.add("  [imageFile] Optional. The name of the file where to save the plot. The plot is drawn only if the name is provided.");
//			toBeFilled.add("  [csvFile] Optional. The name of the file where to save results of the simulation, in format comma-separated value.");
//		}
	
	
	
		toBeFilled.add(" Set the value of a parameter. Note that parameters are specific to a CRN, and thus this command must be invoked after the loading of a CRN.");
		toBeFilled.add(" setParam({param=p1,expr=5})"+suffix);
		if(detailed){
			toBeFilled.add("  [param] Mandatory. The name of the parameter.");
			toBeFilled.add("  [expr] Mandatory. The new expression to assign to the parameter.\n");
		}
	
		toBeFilled.add(" Set the initial concentration of a species.");
		toBeFilled.add(" setIC({species=s1,expr=5})"+suffix);
		if(detailed){
			toBeFilled.add("  [species] Mandatory. The name of the species.");
			toBeFilled.add("  [expr] Mandatory. The new expression to assign as IC of the species.\n");
		}
	
		/*toBeFilled.add(" Change the rate of a reaction.");
			toBeFilled.add(" setRate({reaction=5,expr=5})");
			toBeFilled.add("  [reaction] Mandatory. The position of the reaction (i.e., 1 if it is the first defined).");
			toBeFilled.add("  [expr] Mandatory. The new expression to assign as rate of the reaction.\n");*/
	
		return toBeFilled;
	}


	private static List<String> sampleCommandsShort;

	
}
