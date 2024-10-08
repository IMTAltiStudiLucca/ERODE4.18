package it.imt.erode.importing.probprog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.importing.AbstractImporter;
import it.imt.erode.importing.CRNImporter;
import it.imt.erode.importing.GUICRNImporter;
import it.imt.erode.importing.InfoCRNImporting;
import it.imt.erode.importing.ODEorNET;
import it.imt.erode.partition.implementations.Block;
import it.imt.erode.partition.implementations.Partition;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;

public class GUIProbProgImporter extends AbstractImporter{

	private ArrayList<ArrayList<ICRNReaction>> reactionsOfEachGuard;
	private ArrayList<ArrayList<ICRNReaction>> reactionsOfEachClause;
	private ArrayList<String> probProgParameters;

	public ArrayList<ArrayList<ICRNReaction>> getReactionsOfEachClause() {
		return reactionsOfEachClause;
	}
	public ArrayList<ArrayList<ICRNReaction>> getReactionsOfEachGuard() {
		return reactionsOfEachGuard;
	}
	public ArrayList<String> getProbProgParameters() {
		return probProgParameters;
	}


	protected void createInitialPartition(ISpecies fakeSpeciesForGuards) {
		//		IBlock uniqueBlock = new Block();
		//		setInitialPartition(new Partition(uniqueBlock,getCRN().getSpecies().size()));
		//		for (ISpecies species : getCRN().getSpecies()) {
		//			uniqueBlock.addSpecies(species);
		//		}

		IBlock uniqueBlock = new Block();
		IBlock uniqueClauseGuardBlock = new Block();
		IPartition partition = new Partition(uniqueBlock,getCRN().getSpecies().size());
		partition.add(uniqueClauseGuardBlock);
		setInitialPartition(partition);


		uniqueClauseGuardBlock.addSpecies(fakeSpeciesForGuards);
		for (ISpecies species : getCRN().getSpecies()) {
			if(!species.equals(fakeSpeciesForGuards)) {
				uniqueBlock.addSpecies(species);
			}
		}
	}

	@Override
	protected void addParameter(String parameterName, String parameterExpression, boolean evaluate) {
		//addParameter(parameterName, parameterExpression,evaluate,crn);
		if(probProgParameters==null)
			probProgParameters=new ArrayList<>();
		probProgParameters.add(parameterName);
	}

	public GUIProbProgImporter(String fileName,MessageConsoleStream out,BufferedWriter bwOut,IMessageDialogShower msgDialogShower){
		super(fileName,out,bwOut,msgDialogShower);
	}
	public GUIProbProgImporter(MessageConsoleStream out,BufferedWriter bwOut,IMessageDialogShower msgDialogShower) {
		super(out,bwOut,msgDialogShower);
	}

	public void importProbProg(boolean printInfo, boolean printCRN,boolean print, String modelName,
			ArrayList<String> probProgParameters,
			List<ArrayList<String>> conditionsOfEachClauseProbProg, 
			ArrayList<ArrayList<LinkedHashMap<String,String>>> reactionsOfEachClauseProbProg, 
			ArrayList<ArrayList<String>> initialConcentrations,
			ArrayList<ArrayList<String>> userPartition, ODEorNET modelDefKind, MessageConsoleStream consoleOut) throws IOException{
		initInfoImporting(modelName +" from editor");
		initMath();

		this.probProgParameters=probProgParameters;

		initCRN(modelName);
		getInfoImporting().setLoadedCRN(true);
		getCRN().setMdelDefKind(modelDefKind);
		getInfoImporting().setLoadedCRNFormat(modelDefKind);

		HashMap<String, ISpecies> speciesStoredInHashMap = addAllSpecies(initialConcentrations);

		/*After adding else this does not necessarily hold.
		if(reactionsOfEachClauseProbProg.size()!=conditionsOfEachClauseProbProg.size()) {
			String msg="Each if clause must have a guard specification and a dynamics specification. We have "+conditionsOfEachClauseProbProg.size()+" guards, and "+ reactionsOfEachClauseProbProg.size() +" dynamics. I terminate.";
			throw new UnsupportedOperationException(msg);
		}
		 */


		//We create a fake species, zeroSpecies, 
		//ISpecies fakeSpeciesForGuards = //crn.getCreatingIfNecessaryTheZeroSpecies();
		ISpecies fakeSpeciesForGuards = addSpecies("fakeForGuards", null, "0.0");
		speciesStoredInHashMap.put("fakeForGuards", fakeSpeciesForGuards);

		reactionsOfEachGuard = new ArrayList<ArrayList<ICRNReaction>>(conditionsOfEachClauseProbProg.size());
		reactionsOfEachClause = new ArrayList<ArrayList<ICRNReaction>>(reactionsOfEachClauseProbProg.size());



		String[] reagentsArray_guards=new String[1];
		reagentsArray_guards[0]=fakeSpeciesForGuards.getName();
		for(int clause=0;clause<conditionsOfEachClauseProbProg.size();clause++) {
			ArrayList<String> conditionsOfGuard = conditionsOfEachClauseProbProg.get(clause);

			ArrayList<ICRNReaction> reactionsOfThisGuard = new ArrayList<ICRNReaction>(conditionsOfGuard.size());
			reactionsOfEachGuard.add(reactionsOfThisGuard);

			int cond=0;
			for(String body : conditionsOfGuard) {
				//I need to create a fake
				String id="clause"+clause+"_cond"+cond;
				ICRNReaction ode = GUICRNImporter.parseODEReaction_AddSpeciesIfNecessary(speciesStoredInHashMap, reagentsArray_guards, body,id,getCRN());
				reactionsOfThisGuard.add(ode);
				cond++;
			}
		}


		for(int clause=0;clause<reactionsOfEachClauseProbProg.size();clause++) {
			ArrayList<LinkedHashMap<String, String>> dynamics = reactionsOfEachClauseProbProg.get(clause);
			ArrayList<ICRNReaction> reactionsOfThisClause = new ArrayList<ICRNReaction>(dynamics.size());
			reactionsOfEachClause.add(reactionsOfThisClause);
			for(LinkedHashMap<String, String> react : dynamics) {
				String[] reagentsArray = react.get("reagents").split("\\+");
				//String[] productsArray = react.get("products").split("\\+");
				String rateExpression = react.get("rate");
				//String kind = react.get("kind");
				String id = react.get("id");

				ICRNReaction ode = GUICRNImporter.parseODEReaction_AddSpeciesIfNecessary(speciesStoredInHashMap, reagentsArray, rateExpression,id,getCRN());
				reactionsOfThisClause.add(ode);
			}
		}


		createInitialPartition(fakeSpeciesForGuards);


		//If a user partition is provided, I add a block with the 'unique clause guard block'
		ArrayList<ArrayList<String>> userPartitionWithGuardSpecies=userPartition;
		if(!(userPartition.size()==0 ||(userPartition.size()==1 && userPartition.get(0).size()==0))){
			userPartitionWithGuardSpecies = new ArrayList<>(userPartition);
			ArrayList<String> blockWithGuardSpecies=new ArrayList<>(1);
			blockWithGuardSpecies.add(fakeSpeciesForGuards.getName());
			userPartitionWithGuardSpecies.add(blockWithGuardSpecies);
		}

		GUICRNImporter.readPartition(userPartitionWithGuardSpecies, speciesStoredInHashMap,getCRN());

	}


	public InfoCRNImporting importProbProgNetwork(boolean printInfo, boolean printCRN,boolean print, ODEorNET modelDefKind) throws FileNotFoundException, IOException{

		if(print){
			CRNReducerCommandLine.println(out,bwOut,"\nImporting: "+getFileName());
		}


		initMath();
		initInfoImporting();
		getInfoImporting().setLoadedCRNFormat(modelDefKind);


		BufferedReader br = getBufferedReader();
		String line;

		boolean crnOpen=false;
		//boolean parametersLoaded=false;
		boolean whileOpen=false;
		boolean ifOpen=false;

		//long beginImp = System.currentTimeMillis();
		initMath();

		HashMap<String, ISpecies> speciesNameToSpecies=null;
		ISpecies fakeSpeciesForGuards =null;

		reactionsOfEachGuard = new ArrayList<ArrayList<ICRNReaction>>();
		reactionsOfEachClause = new ArrayList<ArrayList<ICRNReaction>>();

		ArrayList<ArrayList<String>> userPartition=null;

		String[] reagentsArray_guards=new String[1];

		int clause=0;

		while ((line = br.readLine()) != null) {
			line=line.trim();
			line=removeCommentAtTheEnd(line,'#');
			line=removeCommentAtTheEnd(line,"//");

			//Skip comments and empty lines
			if(line.equals("")||line.startsWith("#")||line.startsWith("//")||line.startsWith("end Probabilistic Program")){
				continue;
			}

			if(!crnOpen){
				if(line.startsWith("begin Probabilistic Program")){
					crnOpen=true;

					getInfoImporting().setLoadedCRN(true);
					initCRN();
					getCRN().setMdelDefKind(modelDefKind);
				}
				else{
					CRNReducerCommandLine.printWarning(out,bwOut,"Unknown line: "+line+"\nI skip this line.");
				}
			}
			else {
				if(!whileOpen) {
					//model open, while not open
					if(line.startsWith("begin init")){
						speciesNameToSpecies = loadSpecies(br/*,line.startsWith("begin init")*/);
						fakeSpeciesForGuards = addSpecies("fakeForGuards", null, "0.0");
						speciesNameToSpecies.put(fakeSpeciesForGuards.getName(), fakeSpeciesForGuards);
						reagentsArray_guards[0]=fakeSpeciesForGuards.getName();
					}
					else if(line.startsWith("begin partition")){
						userPartition=CRNImporter.loadPartition(br,speciesNameToSpecies,getCRN());//AAAAA
					}
					else if((!whileOpen)&&line.startsWith("while true do")){
						whileOpen=true;
					}
					else if(startsWithASupportedCommand(line) /*|| line.startsWith("this")*/){
						//else if(line.startsWith("reduce") || line.startsWith("simulate") || line.startsWith("load")|| line.startsWith("write")|| line.startsWith("quit") || line.startsWith("import") || line.startsWith("export") || line.startsWith("this") ){
						//remove comments at the end of the command
						int commentPos=line.indexOf('#');
						if(commentPos==-1) {
							commentPos=line.indexOf("//");
						}
						if(commentPos!=-1){
							line=line.substring(0, commentPos);
						}
						addCommand(line);
						//getInfoImporting().increaseCommandsCounter();
					}
					else{
						CRNReducerCommandLine.printWarning(out,bwOut,"Unknown line: "+line+"\nI skip this line.");
					}
				}
				else{
					if(!ifOpen) {
						//model open, while open, if not open
						if(line.startsWith("begin parameters")){
							initMath();//I throw away the old math
							loadParameters(br,false,true,false);
							//parametersLoaded = true;
						}
						else if(line.startsWith("if ")) {
							ifOpen=true;

							//take guard
							int lengthPrefix="if".length();
							clause = parseGuardAsODEs(line, speciesNameToSpecies, reagentsArray_guards, clause,lengthPrefix);


							//if c_odd = 0 and c - 3.0/0.1 < 0 then
						}
						else if(line.startsWith("end while")) {
							whileOpen=false;
						}
						else {
							CRNReducerCommandLine.printWarning(out,bwOut,"Unknown line: "+line+"\nI skip this line.");
						}
					}
					else {
						//model open, while open, if open
							//I opened a while in the previous line
							line=handleDynamicsOfAThen(line, speciesNameToSpecies,clause,br);
							line=line.trim();
							while(line.startsWith("elif")) {
								int lengthPrefix="elif".length();
								clause = parseGuardAsODEs(line, speciesNameToSpecies, reagentsArray_guards, clause,lengthPrefix);
								line=br.readLine();
								line=handleDynamicsOfAThen(line, speciesNameToSpecies,clause,br);
								line=line.trim();
							}
							if(line.startsWith("else")) {
								line=handleDynamicsOfAThen(line, speciesNameToSpecies,clause,br);
								clause++;
							}
							if(line.startsWith("end if")) {
								ifOpen=false;
							}						
						
					}
				}
			}
		}

		createInitialPartition(fakeSpeciesForGuards);

		if(userPartition!=null) {
			//If a user partition is provided, I add a block with the 'unique clause guard block'
			ArrayList<ArrayList<String>> userPartitionWithGuardSpecies=userPartition;
			if(!(userPartition.size()==0 ||(userPartition.size()==1 && userPartition.get(0).size()==0))){
				userPartitionWithGuardSpecies = new ArrayList<>(userPartition);
				ArrayList<String> blockWithGuardSpecies=new ArrayList<>(1);
				blockWithGuardSpecies.add(fakeSpeciesForGuards.getName());
				userPartitionWithGuardSpecies.add(blockWithGuardSpecies);
			}

			GUICRNImporter.readPartition(userPartitionWithGuardSpecies, speciesNameToSpecies,getCRN());
		}

		return getInfoImporting();
	}
	public String handleDynamicsOfAThen(String line, HashMap<String, ISpecies> speciesNameToSpecies,int ifClause, BufferedReader br) throws IOException {
		ArrayList<ICRNReaction> reactionsOfThisClause = new ArrayList<ICRNReaction>();
		reactionsOfEachClause.add(reactionsOfThisClause);
		line=line.trim();
		while ((line != null) && (! line.startsWith("elif"))
				&& (! line.startsWith("else"))
				&& (! line.startsWith("end if"))		) {
			if(!(line.equals("") || line.startsWith("#") || line.startsWith("//"))){
				line = removeCommentAtTheEnd(line,'#');
				line = removeCommentAtTheEnd(line,"//");

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
				ICRNReaction ode = GUICRNImporter.parseODEReaction_AddSpeciesIfNecessary(speciesNameToSpecies, reagentsArray, rateString,"ifClause"+ifClause+"_"+reactionsOfThisClause.size(),getCRN());
				reactionsOfThisClause.add(ode);
			}
			line=br.readLine();
			if(line!=null)
				line=line.trim();
		}
		return line;
	}
	public int parseGuardAsODEs(String line, HashMap<String, ISpecies> speciesNameToSpecies,
			String[] reagentsArray_guards, int clause, int lengthPrefix) throws IOException {
		ArrayList<ICRNReaction> reactionsOfThisGuard = new ArrayList<ICRNReaction>();
		reactionsOfEachGuard.add(reactionsOfThisGuard);

		line=line.substring(lengthPrefix, line.indexOf("then"));
		String[] guards=line.split("and");//TODO only accepts and of clauses
		int cond=0;
		for(String guard : guards) {
			int index=guard.indexOf("<");
			if(index==-1) {
				index=guard.indexOf(">");
				if(index==-1) {
					index=guard.indexOf("=");
				}
			}
			guard=guard.substring(0,index);
			//I need to create a fake
			String id="clause"+clause+"_cond"+cond;
			ICRNReaction ode = GUICRNImporter.parseODEReaction_AddSpeciesIfNecessary(speciesNameToSpecies, reagentsArray_guards, guard,id,getCRN());
			reactionsOfThisGuard.add(ode);
			cond++;
		}
		clause++;
		return clause;
	}

	/**
	 * This method is used to load the optional list of species. It is useful in case one wants to give an order on the species
	 * I now impose that there must be an = if you want to specify the IC
	 * @param br
	 * @return
	 * @throws IOException
	 */
	public HashMap<String, ISpecies> loadSpecies(BufferedReader br/*, boolean erodeFormat*/)  throws IOException {

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

	private static ArrayList<String> supportedCommands;

	public static ArrayList<String> getSupportedCommands(){
		if(supportedCommands==null){
			supportedCommands=new ArrayList<>();
			//supportedCommands.add("reduceFPE");
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


}
