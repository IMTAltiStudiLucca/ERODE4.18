package it.imt.erode.importing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
//import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.eclipse.ui.console.MessageConsoleStream;

//import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.crn.implementations.CRN;
//import it.imt.erode.crn.implementations.Composite;
import it.imt.erode.crn.implementations.Species;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
//import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.crn.symbolic.constraints.IConstraint;
import it.imt.erode.expression.evaluator.MathEval;
import it.imt.erode.partition.implementations.Block;
import it.imt.erode.partition.implementations.Partition;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SpeciesSplayBST;

public abstract class AbstractImporter {

	public static final boolean OVERWRITEEXTENSIONS=false;
	
	private InfoCRNImporting infoImporting;
	private ICRN crn;
	private IPartition initialPartition;
	private String fileName;
	private MathEval math;
	private List<String> furtherCommands;
	private static final Pattern stocAndName = Pattern.compile("[0-9]*\\*.*");
	protected MessageConsoleStream out;
	protected BufferedWriter bwOut;
	protected IMessageDialogShower msgDialogShower;
	
	protected void createInitialPartition() {
		IBlock uniqueBlock = new Block();
		setInitialPartition(new Partition(uniqueBlock,getCRN().getSpecies().size()));
		for (ISpecies species : getCRN().getSpecies()) {
			uniqueBlock.addSpecies(species);
		}
	}
	
	public static void addReaction(ICRN crn, int arity, 
			ICRNReaction reaction) {
		crn.addReaction(reaction);
//		addToIncomingReactionsOfProducts(arity,reaction.getProducts(), reaction,CRNReducerCommandLine.addReactionToComposites);
//		addToOutgoingReactionsOfReagents(arity, reaction.getReagents(), reaction,CRNReducerCommandLine.addReactionToComposites);
//		if(CRNReducerCommandLine.hasToAddToReactionsWithNonZeroStoichiometry){
//			addToReactionsWithNonZeroStoichiometry(arity, reaction.computeProductsMinusReagentsHashMap(),reaction);
//		}
	}
	
	protected AbstractImporter(MessageConsoleStream out,BufferedWriter bwOut,IMessageDialogShower msgDialogShower) {
		this.out=out;
		this.bwOut=bwOut;
		furtherCommands=new ArrayList<String>();
		this.msgDialogShower=msgDialogShower;
	}
	
	public AbstractImporter(String fileName,MessageConsoleStream out,BufferedWriter bwOut,IMessageDialogShower msgDialogShower) {
		this(out,bwOut,msgDialogShower);
		this.fileName=fileName;
	}
	
	public InfoCRNImporting getInfoImporting() {
		return infoImporting;
	}

	public ICRN getCRN() {
		return crn;
	}

	public IPartition getInitialPartition() {
		return initialPartition;
	}
	
	public static void createParentDirectories(String fileName) {
		File f = new File(fileName);
		File parent= f.getParentFile();
		if(parent!=null){
			parent.mkdirs();
		}
	}
	
	public static String overwriteExtensionIfEnabled(String name,String newExtension,boolean enabled){
		if(enabled){
			String fileName = name;
			String[] knownExtensions = {".net", ".crn",".ode",".lbs", ".xml", ".hyxml", ".bngl", ".txt", ".pop", ".maude", ".z3",".inp", ".CKI",".dat", ".tra", ".csv",".mo", ".m", ".cc",".bnet"};
			
			for(int i=0;i<knownExtensions.length;i++) {
				if(fileName.endsWith(knownExtensions[i])) {
					fileName=fileName.substring(0,fileName.length()-knownExtensions[i].length());
					break;
				}
				//fileName=fileName.replace(".CKI", "");
			}
			
			if((!newExtension.equals("")) && (!newExtension.startsWith("."))){
				newExtension="."+newExtension;
			}
			return fileName+newExtension;
		}
		else{
			return name;
		}
	}
	
	public static String overwriteExtensionIfEnabled(String name,String newExtension){
		return overwriteExtensionIfEnabled(name,newExtension,OVERWRITEEXTENSIONS);
	}
	
	public MathEval getMath(){
		return math;
	}
	
	public double evaluate(String expression){
		return math.evaluate(expression);
	}
	
	public String getFileName(){
		return fileName;
	}
	
	
	protected void initInfoImporting() {
		this.infoImporting = new InfoCRNImporting(0, 0, 0, fileName,0);
	}
	
	protected void initInfoImporting(String name) {
		this.infoImporting = new InfoCRNImporting(0, 0, 0, name,0);
	}

	protected void initCRNAndMath() {
		math = new MathEval();
		crn = new CRN(fileName,math,out,bwOut);
	}
	
	protected void initMath() {
		math = new MathEval();
		if(crn!=null){
			crn.setNewMath(math);
		}
	}
	protected void initCRN() {
		String name = fileName;
		int fileSep=name.indexOf(File.separator);
		if(name!=null && fileSep>=0){
			name =name.substring(name.lastIndexOf(File.separator)+1);
			name=overwriteExtensionIfEnabled(name, "",true);
		}
		crn = new CRN(name,math,out,bwOut);
	}
	
	protected void initCRN(String name) {
		crn = new CRN(name,math,out,bwOut);
	}

	protected void setInitialPartition(IPartition initialPartition) {
		this.initialPartition = initialPartition;
	}

	protected void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	protected static String removeCommentAtTheEnd(String line, char com){
		//throw away comments at the end of the line
		int commentsPos=line.indexOf(com);
		if(commentsPos!=-1&&commentsPos!=0){
			line = line.substring(0, commentsPos).trim();
		}
		return line;
	}
	
	protected static String removeCommentAtTheEnd(String line, String com){
		//throw away comments at the end of the line
		int commentsPos=line.indexOf(com);
		if(commentsPos!=-1){
			line = line.substring(0, commentsPos).trim();
		}
		return line;
	}
	
	
	/**
	 * 
	 * @param br
	 * @param infoImporting 
	 * @return 
	 * @throws IOException
	 * Assumnptions: the expression defining the value of a parameter contains only parameters defined in the previous lines of the file, and not in the following.    
	 */
	protected void loadParameters(BufferedReader br, boolean parametersHaveId, boolean hasEqual) throws IOException {
		String line = br.readLine();

		while ((line != null) && (!(line=line.trim()).startsWith("end parameters"))) {
			//Skip comments or empty lines
			if(!(line.equals("") || line.startsWith("#") || line.startsWith("//"))){
				line=removeCommentAtTheEnd(line,'#');
				line=removeCommentAtTheEnd(line,"//");
				String parameterName=null;
				String parameterExpression=null;
				/*if(plus){
					if(parametersHaveId){
						line =line.substring(line.indexOf(' '));
					}
					String[] p = line.split("\\=");
					parameterName = p[0];
					parameterExpression = p[1];
				}*/
				StringTokenizer st = new StringTokenizer(line);
				if(parametersHaveId){
					//throw away the id of the parameter
					st.nextToken();
				}
				parameterName = st.nextToken();
				
				if(parameterName.endsWith("=")) {
					//This solves the problem: p86= 4 in which parameterName is p86=
					parameterName=parameterName.substring(0, parameterName.length()-1).trim();
				}
				
				if(parameterName.contains("=")){
					String[] app=parameterName.split("\\=");
					parameterName=app[0];
					System.out.println(parameterName);
					parameterExpression=app[1];
				}
				else{
					parameterExpression=st.nextToken();
					if(parameterExpression.equals("=")){
						parameterExpression=st.nextToken();
					}
					while(st.hasMoreTokens()) {
						parameterExpression+=st.nextToken();
					}
				}
				
				
				
				/*
				if(hasEqual){
					//throw away the equal sign
					st.nextToken();
				}
				parameterExpression = st.nextToken();
				*/
				/*double parameterValue = Double.valueOf(parameterExpression);
				double truncated=C2E2Exporter.setNumberOfDigits(parameterValue, 2);
				parameterExpression=String.valueOf(truncated);*/

				addParameter(parameterName, parameterExpression);
			}
			line = br.readLine();
		}
		infoImporting.setReadParameters(crn.getParameters().size());
	}

	protected void addParameter(String parameterName, String parameterExpression) {
		addParameter(parameterName, parameterExpression,true,crn);
	}
	
	protected void addParameter(String parameterName, String parameterExpression, ICRN crn) {
		addParameter(parameterName, parameterExpression,true,crn);
	}
	
	protected void addSymbolicParameter(String parameterName) {
		crn.addSymbolicParameter(parameterName);
	}
	protected void addConstraint(IConstraint constraint) {
		crn.addConstraint(constraint);
	}
	
//	protected void addParameterWithoutEvaluatingIt(String parameterName, String parameterExpression) {
//		addParameter(parameterName, parameterExpression,false);
//	}
		
	public static void addParameter(String parameterName, String parameterExpression,boolean evaluate, ICRN crn){
		/*if(parameterName.equalsIgnoreCase("e")){
		CRNReducerCommandLine.printWarning("The use of \"e\" or \"E\" as name of a parameter is deprecated, as expressions containing it might be erroneously considered as numbers in scientific notation.");
	}*/
		crn.addParameter(parameterName,parameterExpression);
		updateMathEval(parameterName, parameterExpression, evaluate,crn.getMath());
	}

	public static void updateMathEval(String parameterName, String parameterExpression, boolean evaluate, MathEval math) {
		double parameterValue;
		try  
		{  
			parameterValue = Double.valueOf(parameterExpression);  
		}  
		catch(NumberFormatException nfe)  
		{  
			parameterValue = math.evaluate(parameterExpression);  
		}  		
		if(evaluate){
			//System.out.println("parameterName: "+parameterName);
			math.setVariable(parameterName, parameterValue);
		}
	}

	protected BufferedReader getBufferedReader() throws FileNotFoundException{
		return new BufferedReader(new FileReader(fileName)); 
	}
	
	protected BufferedReader getBufferedReader(String aFile) throws FileNotFoundException{
		return new BufferedReader(new FileReader(aFile)); 
	}
	
//	protected int[] generateNewSpeciesAndBuildArrayOfIDs(HashMap<String, ISpecies> speciesStoredInHashMap,String[] speciesNames) {
//		int[] speciesIds = new int[speciesNames.length];
//		for (int i=0; i<speciesNames.length;i++) {
//			String speciesName = speciesNames[i].trim();
//			ISpecies existingSpecies = speciesStoredInHashMap.get(speciesName);
//			if(existingSpecies==null){
//				int id = crn.getSpecies().size();
//				ISpecies species = new  Species(speciesName, id, BigDecimal.ZERO,"0");
//				speciesStoredInHashMap.put(speciesName, species);
//				crn.addSpecies(species);
//				speciesIds[i]=id;
//			}
//			else{
//				speciesIds[i]=existingSpecies.getID();
//			}
//		}
//		return speciesIds;
//	}
	
	public static boolean isSpecialNullSpecies(String name){
		if(name.equals("")|| name.equalsIgnoreCase(Species.ZEROSPECIESNAME)){
			return true;
		}
		if(name.equalsIgnoreCase("trash")||name.equalsIgnoreCase("trash()")){
			return true;
		}
		if(name.equalsIgnoreCase("$sink")||name.equalsIgnoreCase("$sink()")){
			return true;
		}
		if(name.equalsIgnoreCase("Sink")||name.equalsIgnoreCase("Sink()")){
			return true;
		}
		if(name.equalsIgnoreCase("null")||name.equalsIgnoreCase("null()")){
			return true;
		}
		return false;
	}
	
	private static void addSpeciesIfNecessary(HashMap<String, ISpecies> speciesStoredInHashMap, HashMap<ISpecies, Integer> compositeHM, String name, int stoc, ICRN crn){
		if(isSpecialNullSpecies(name)){
			boolean alreadyContainsTheZeroSpecies=crn.containsTheZeroSpecies();
			ISpecies species = crn.getCreatingIfNecessaryTheZeroSpecies();
			if(!alreadyContainsTheZeroSpecies){
				speciesStoredInHashMap.put(species.getName(), species);
			}
			
			int prev=0;
			Integer p = compositeHM.get(species);
			if(p!=null){
				prev=p;
			}
			compositeHM.put(species, stoc + prev);
		}
		else{
			//we have a non-empty composite
			ISpecies existingSpecies = speciesStoredInHashMap.get(name);
			if(existingSpecies==null){
				int id = crn.getSpecies().size();
				existingSpecies = new  Species(name, null, id, BigDecimal.ZERO,"0",false);
				speciesStoredInHashMap.put(name, existingSpecies);
				crn.addSpecies(existingSpecies);
			}
			int prev=0;
			Integer p = compositeHM.get(existingSpecies);
			if(p!=null){
				prev=p;
			}
			compositeHM.put(existingSpecies, stoc + prev);
		}
	}
	
	protected ISpecies addSpecies(String name,String originalName,String icExpr){
		return addSpecies(name,originalName,icExpr, null,false);
	}
	protected ISpecies addSpecies(String name,String originalName,String icExpr, HashMap<String, ISpecies> speciesStoredInHashMap){
		return addSpecies(name,originalName,icExpr, speciesStoredInHashMap,false);
	}
	protected ISpecies addSpecies(String name,String originalName,String icExpr, HashMap<String, ISpecies> speciesStoredInHashMap, boolean isAlgebraic){
		if(isSpecialNullSpecies(name)){
			boolean alreadyContainsTheZeroSpecies=crn.containsTheZeroSpecies();
			ISpecies species = crn.getCreatingIfNecessaryTheZeroSpecies(name);
			if(!alreadyContainsTheZeroSpecies){
				if(speciesStoredInHashMap!=null) {
					speciesStoredInHashMap.put(species.getName(), species);
				}
			}
			species.setInitialConcentration(BigDecimal.valueOf(evaluate(icExpr)),icExpr);
			species.setIsAlgebraic(isAlgebraic);
			return species;
		}
		else{
			int id = crn.getSpecies().size();
			ISpecies species = new  Species(name, originalName,id, BigDecimal.valueOf(evaluate(icExpr)),icExpr,isAlgebraic);
			//species.setIsAlgebraic(isAlgebraic);
			if(speciesStoredInHashMap!=null) {
				speciesStoredInHashMap.put(name, species);
			}
			crn.addSpecies(species);
			return species;
		}
	}
	
	protected static HashMap<ISpecies, Integer> generateNewSpeciesAndBuildArrayOfNames(HashMap<String, ISpecies> speciesStoredInHashMap,String[] speciesNamesAndStochiometry, ICRN crn) {
		HashMap<ISpecies, Integer> compositeHM = new LinkedHashMap<ISpecies, Integer>(speciesNamesAndStochiometry.length);
		for(String nameAndStoc : speciesNamesAndStochiometry){
			String s = nameAndStoc.trim();
			if(stocAndName.matcher(s).matches()){				
				int posStar = s.indexOf("*");
				String stocS = s.substring(0,posStar);
				int stoc = Integer.valueOf(stocS);
				String name = s.substring(posStar+1);
				addSpeciesIfNecessary(speciesStoredInHashMap, compositeHM,name.trim(),stoc,crn);
			}
			else{
				addSpeciesIfNecessary(speciesStoredInHashMap, compositeHM,s.trim(),1,crn);
			}
		}
		return compositeHM;
	}
		
	
	/*public static void addToIncomingReactionsOfProducts(int arity, IComposite compositeProducts, ICRNReaction reaction) {
		addToIncomingReactionsOfProducts(arity, compositeProducts, reaction,true);
	}*/
	
	
//	public static void addToIncomingReactionsOfProducts(int arity, IComposite compositeProducts, ICRNReaction reaction, boolean addToComposite) {
//
//		if(compositeProducts instanceof Composite){
//			if(addToComposite){
//				compositeProducts.addIncomingReactions(reaction);
//			}
//			for(int i=0;i<compositeProducts.getNumberOfDifferentSpecies();i++){
//				ISpecies species = compositeProducts.getAllSpecies(i);
//				species.addIncomingReactions(reaction);
//			}
//		}
//		else if(compositeProducts instanceof Species){
//			ISpecies species = (ISpecies)compositeProducts;
//			species.addIncomingReactions(reaction);
//		}
//		else{
//			throw new UnsupportedOperationException();
//		}
//	}
//	
//	public static void addToOutgoingReactionsOfReagents(int arity, IComposite compositeReagents, ICRNReaction reaction,boolean addToComposite) {
//
//		if(compositeReagents instanceof Composite){
//			if(addToComposite){
//				compositeReagents.addOutgoingReactions(reaction);
//			}
//			for(int i=0;i<compositeReagents.getNumberOfDifferentSpecies();i++){
//				ISpecies species = compositeReagents.getAllSpecies(i);
//				species.addOutgoingReactions(reaction);
//			}
//		}
//		else if(compositeReagents instanceof Species){
//			ISpecies species = (ISpecies)compositeReagents;
//			species.addOutgoingReactions(reaction);
//		}
//		else{
//			throw new UnsupportedOperationException();
//		}
//	}
//	
//	
//	public static void addToReactionsWithNonZeroStoichiometry(int arity, HashMap<ISpecies, Integer> hashMap, ICRNReaction reaction) {
//		for (Entry<ISpecies, Integer> entry : hashMap.entrySet()) {
//			entry.getKey().addReactionsWithNonZeroStoichiometry(reaction);
//		}
//	}

	public List<String> getFurtherCommands() {
		return furtherCommands;
	}
	
	public void addCommand(String command) {
		command=command.replace(" ", "");
		furtherCommands.add(command);
		getInfoImporting().increaseCommandsCounter();
	}
	
	public static void writeCommentLine(BufferedWriter br, String comment,String commentSymbol) throws IOException {
		br.write(commentSymbol);
		br.write(comment);
		br.write("\n");
	}
	
	protected static ArrayList<HashSet<ISpecies>> viewsAsMultisetsToUserPartition(List<HashMap<ISpecies, Integer>> setsOfSpeciesOfViews, ICRN crn) {
		IPartition refinement = new Partition(crn.getSpecies().size());
		IBlock block = new Block();
		refinement.add(block);
		for (ISpecies species : crn.getSpecies()) {
			block.addSpecies(species);
		}
		
		for (HashMap<ISpecies,Integer> speciesOfView : setsOfSpeciesOfViews) {
			IPartition prevPartition=refinement; 
			refinement = new Partition(crn.getSpecies().size());
			IBlock currentBlock = prevPartition.getFirstBlock();
			while(currentBlock!=null){
				//The binary search tree used to split each block
				SpeciesSplayBST<Integer, IBlock> bst = new SpeciesSplayBST<Integer, IBlock>();
				for (ISpecies species : currentBlock.getSpecies()) {
					// Insert the species "species" in the global binary search tree created, so to partition each block of the current partition according to the initial concentrantions. 
					// This may cause the creation of a new block, in which case it is automatically added to refinement.
					Integer mult = speciesOfView.get(species);
					if(mult==null){
						mult=0;
					}
					bst.put(mult, species, refinement);
				}
				currentBlock=currentBlock.getNext();
			}
		}
		
		ArrayList<HashSet<ISpecies>> userPartition = new ArrayList<HashSet<ISpecies>>(refinement.size());
		IBlock currentBlock = refinement.getFirstBlock();
		while(currentBlock!=null){
			//I add this block only if it is not the 'dummy block with all species not appearing in any view
			ISpecies sp = currentBlock.getSpecies().iterator().next();
			boolean appearsInViews=belongsToASet(sp,setsOfSpeciesOfViews);
			if(appearsInViews) {
				HashSet<ISpecies> currentUserBlock = new HashSet<>(currentBlock.getSpecies());
				userPartition.add(currentUserBlock);
			}
			currentBlock=currentBlock.getNext();
		}
		return userPartition;
	}

	private static boolean belongsToASet(ISpecies sp, List<HashMap<ISpecies, Integer>> setsOfSpeciesOfViews) {
		if(setsOfSpeciesOfViews!=null) {
			for(HashMap<ISpecies, Integer> map : setsOfSpeciesOfViews) {
				Integer v=map.get(sp);
				if(v!=null && v!=0) {
					return true;
				}
			}
		}
		return false;
	}

	public void setRequiredMS(long ms) {
		this.infoImporting.setRequiredMS(ms); 		
	}
	
}
