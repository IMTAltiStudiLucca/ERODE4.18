package it.imt.erode.importing.chemkin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.auxiliarydatastructures.StringAndBigDecimal;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.DialogType;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.crn.implementations.CRN;
import it.imt.erode.crn.implementations.CRNReaction;
import it.imt.erode.crn.implementations.Composite;
import it.imt.erode.crn.implementations.Species;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.expression.evaluator.Expression;
import it.imt.erode.importing.AbstractImporter;
import it.imt.erode.importing.InfoCRNImporting;
import it.imt.erode.importing.UnsupportedFormatException;
import it.imt.erode.partition.implementations.Block;
import it.imt.erode.partition.implementations.Partition;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partitionrefinement.algorithms.CRNBisimulationsNAry;

//import jing.chemParser.ChemParser;
//import jing.rxnSys.ReactionModelGenerator;










//import com.oracle.xmlns.internal.webservices.jaxws_databinding.WebParamMode;
//import com.sun.org.apache.bcel.internal.generic.GETSTATIC;

		//TODO: for some reason the found code does not consider v_{k_i} but -v_{k_i} when computing the reverse rates.  
		//TODO: hv: currently models containing reactions with are discarded

/**
 * 
 * @author Andrea Vandin
 * This class is used to import reaction networks written in the ChemKin format (file extension: .inp).
 * The implementation has been double checked using the source code of the project RMG-Java (check CalculateReverseRateCoefficients.java and CheckForwardAndReverseRateCoefficients)
 */
public class ChemKinImporter extends AbstractImporter{
	
	public static final int PRECISION = 25;

	public static final String ChemKinNetworksFolder = "."+File.separator+"ChemKinNetworks"+File.separator;
	//private boolean previouslyUnsupportedReaction=false;
	//It is often assumed that T=1000K
	//private BigDecimal T = BigDecimal.valueOf(1000);
	private BigDecimal T = null;
	public static final String TName = "T";
	
	//public HashSet<String> unknownSpecies = new HashSet<String>();

	/*//Universal gas constant [ergs/(mole K) THAT IS erg K−1 mol−1]
	public static final BigDecimal R = BigDecimal.valueOf(8.3144621E+7);
	public static final String RName = "R";
	//Universal gas constant [cal / (mole K) THAT IS cal K−1 mol−1]
	public static final BigDecimal Rc = BigDecimal.valueOf(1.9872041); 
	public static final String RCName = "Rc";*/
	
	private BigDecimal R;
	private final String RName="R";
	private final BigDecimal RinJoverMol=BigDecimal.valueOf(8.3144621);

	//TODO set proper value
	//Pressure of one standard atmosphere
	//public static final BigDecimal Patm = BigDecimal.valueOf(1);
	//public static final String PatmName = "Patm";
	
	private BigDecimal lowestT;
	private BigDecimal commonT;
	private BigDecimal highestT;
	private String thermoDynamiFileName;
	private LinkedHashMap<ISpecies, ThermoDynamicInfoChemKin> thermoDynamicInfo; 
	//private int counter=0;
	
	

	private HashMap<String, ISpecies> speciesStoredInHashMap;

	public ChemKinImporter(String fileName, String[] thermoDynamicFileName,MessageConsoleStream out,BufferedWriter bwOut,IMessageDialogShower msgDialogShower) {
		super(fileName,out,bwOut,msgDialogShower);
		if(thermoDynamicFileName!=null && thermoDynamicFileName.length>0){
			this.thermoDynamiFileName=thermoDynamicFileName[0];
		}
	}

	public InfoCRNImporting importChemKinNetwork(boolean printInfo, boolean printCRN,boolean print) throws FileNotFoundException, IOException, UnsupportedFormatException{
		//previouslyUnsupportedReaction=false;
		if(print){
			CRNReducerCommandLine.println(out,bwOut,"\nImporting: "+getFileName());
		}

		initInfoImporting();
		initCRNAndMath();
		ICRN crn = getCRN();
		getInfoImporting().setLoadedCRN(true);

		addDefaultParameters();

		BufferedReader br = getBufferedReader();
		String line;
		//I assume that first the species are generated, then the reactions.
		boolean speciesGenerated=false;
		boolean reactionsLoaded=false;

		while ((line = br.readLine()) != null) {
			line=line.trim();

			if(line.startsWith("SPEC")||line.startsWith("spec")||line.startsWith("Spec")){
				/*SPECIES H2 O2 H O OH HO2 N2 N NO END*/
				/*SPEC ! SPEC is equivalent to SPECIES 
				  H2 O2
				  H  O  OH  HO2  N2  N  NO
				END*/
				/*SPEC H2*/
				/*spec O2*/
				loadSpecies(br,line);
				thermoDynamicInfo=new LinkedHashMap<ISpecies, ThermoDynamicInfoChemKin>(getCRN().getSpecies().size());
				importThermodynamicData(true);
				speciesGenerated=true;
			}
			else if(speciesGenerated && (!reactionsLoaded) && ((line.startsWith("REACTIONS")||line.startsWith("reactions")||line.startsWith("Reactions")))){	
				HashMap<Double, String> createdKineticConstants = loadReactions(br,line);
				Double[] sortedParams = new Double[createdKineticConstants.size()];
				int i=0;
				for (Double d : createdKineticConstants.keySet()) {
					sortedParams[i]=d;
					i++;
				}
				Arrays.sort(sortedParams);
				/*System.out.println("The "+sortedParams.length+" added parameters:");
				for(i=0;i<sortedParams.length;i++){
					System.out.println(String.valueOf(sortedParams[i]));
				}*/
				
				IBlock uniqueBlock = new Block();
				setInitialPartition(new Partition(uniqueBlock,getCRN().getSpecies().size()));
				for (ISpecies species : getCRN().getSpecies()) {
					uniqueBlock.addSpecies(species);
				}
				reactionsLoaded=true;
				getInfoImporting().setReadParameters(crn.getParameters().size());
			}
			//Any other line is ignored (including comments)
			else if(line.startsWith("!")){
				//if this is a comment line, skip to the next iteration of the loop (i.e. to the next line)
				continue;
			}
			else if(line.startsWith("THERMO") || line.startsWith("Thermo") || line.startsWith("thermo")){
				//CRNReducerCommandLine.println(out,bwOut"!!!!!THERMO in the .inp: IGNORED!!!!!");
				line = line.toLowerCase();
				boolean updateTemperatures=false;
				if(line.startsWith("thermo all")){
					updateTemperatures=true;
				}
				importThermodynamicData(true,br,updateTemperatures);
			}
		}
		br.close();
		//CRNReducerCommandLine.println(out,bwOut"BioNetGenImporter.importBioNetGenNetwork: completed importing of model "+fileName);
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
							CRNReducerCommandLine.println(out,bwOutcrn.getName()+": It is not possible to derive the atomic CTMC because its species are not monomers-based");
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
			CRNReducerCommandLine.println(out,bwOut"We have "+monomers.size()+" monomers. These are monomers for which it is not possible to generate the atomic CTMC.");
			LinkedHashSet<String> unsupportedMonomers = new LinkedHashSet<String>();
			MutableInteger counterUnsupportedMonomers=new MutableInteger(0);			
			for (String mon : monomers) {
				if(!crn.isAtomicCTMCDerivable(mon,counterUnsupportedMonomers)){
					allAtomicCTMCsDerivable=false;
					unsupportedMonomers.add(mon);
					//CRNReducerCommandLine.println(out,bwOutcrn.getName()+": It is not possible to derive the atomic CTMC for the monomer "+mon);
					//break;
				}
			}
		}
		if(allAtomicCTMCsDerivable){
			CRNReducerCommandLine.println(out,bwOut"It is possible to derive the atomic CTMC for all monomers.");
		}
		//else {
		//	CRNReducerCommandLine.println(out,bwOutcrn.getName()+": It not is possible to derive the atomic CTMC for some monomer.");
		//}

		CRNReducerCommandLine.println(out,bwOut"");
		CRNReducerCommandLine.println(out,bwOut"");

		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		/////////////////////////////////////////////////////END ATOMIC CTMC////////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		 */

		/*if(getCRN().isElementary()){
			CRNReducerCommandLine.printWarning(out,bwOut,"Elementary CRN");
		}*/
		CRNReducerCommandLine.println(out,bwOut,"Max arity: "+getCRN().getMaxArity());
		//CRNReducerCommandLine.println(out,bwOut"unknown species: "+unknownSpecies.toString().replace(", ", " "));
		return getInfoImporting();
	}

	public boolean importThermodynamicData(boolean print) throws IOException, UnsupportedFormatException{
		
		//If the name of the thermodynamic file has not been specified, I assume that it is the same of the mechanism (but with different extension)
		if(thermoDynamiFileName==null){
			thermoDynamiFileName = getFileName().replace(".inp", "");
			thermoDynamiFileName = thermoDynamiFileName.replace(".CKI", "");
			thermoDynamiFileName = thermoDynamiFileName+".dat";
		}

		if(print){
			CRNReducerCommandLine.println(out,bwOut,"\nImporting: "+thermoDynamiFileName);
		}

		BufferedReader br=null;
		try{
			br = getBufferedReader(thermoDynamiFileName);
		}catch(FileNotFoundException e){
			CRNReducerCommandLine.printWarning(out,bwOut,true,msgDialogShower,"File with termodynamic data not provided.",DialogType.Error);
			return false;
		}
		
		String line;
		boolean stop=false;

		while ((!stop)&&(line = br.readLine()) != null) {
			line=line.trim();
			//CRNReducerCommandLine.println(out,bwOut"["+line+"]");

			if(line.startsWith("THERMO")||line.startsWith("thermo")||line.startsWith("Thermo")){
				stop=true;
				boolean b = importThermodynamicData(print,br,true);
				br.close();
				return b;
			}
		}
		
		br.close();
		return false;
	} 
	

	public boolean importThermodynamicData(boolean print, BufferedReader br, boolean updateTemperatures) throws IOException, UnsupportedFormatException{

		String line;

		//		while ((line = br.readLine()) != null) {
		line = br.readLine();
		line=line.trim();

		//if(line.startsWith("THERMO")||line.startsWith("thermo")||line.startsWith("Thermo")){

		//Skip comments
		while(line.startsWith("!")||line.equals("")){
			line = br.readLine();
			line=line.trim();
		}
		if(updateTemperatures){
			line= removeCommentAtTheEnd(line,'!');
			//Read mandatory T ranges 300.000  1000.000  5000.000
			String[] TRanges = line.split("\\s+");
			if(TRanges.length!=3){
				String message="Could not parse the temperature ranges from the thermodynamic file: "+line;
				//CRNReducerCommandLine.printWarning(out,bwOut,message);
				throw new UnsupportedFormatException(message);
			}

			lowestT	= cleanAndGetBigDecimal(TRanges[0],false);
			commonT	= cleanAndGetBigDecimal(TRanges[1],false);
			highestT= cleanAndGetBigDecimal(TRanges[2],false);
			if(lowestT==null||commonT==null||highestT==null){
				String message="Could not parse the temperature ranges from the thermodynamic file: "+line;
				//CRNReducerCommandLine.printWarning(out,bwOut,message);
				throw new UnsupportedFormatException(message);
			}

			//I set the value of T to commonT
			T = commonT;
			//getCRN().addParameter(TName,T.toPlainString());
			getMath().setVariable(TName,T.doubleValue());

			//Read thermodynamic info of the species
			line = br.readLine();
		}
		else{
			//Check if this line contains the temperatues of the model, or already talks about species
			String[] TRanges = line.split("\\s+");
			BigDecimal bd=null;
			boolean read=true;
			try{
				bd = cleanAndGetBigDecimal(TRanges[0],false);
			}catch(NumberFormatException e){
				//The line does not contain the temperatues of the model, but already talks about species
				read=false;
			}
			if(bd==null){
				read=false;
			}
			if(read){
				//Read thermodynamic info of the species
				line = br.readLine();
			}

		}



		String trimmedLine = line;
		if(line!=null){
			trimmedLine=line.trim();
		}
		while ((trimmedLine != null) && (! (trimmedLine.startsWith("END") || trimmedLine.startsWith("end")|| trimmedLine.startsWith("End")) ) ) {
			//I accept comments between species, but not between the four lines of each species
			if(!(trimmedLine.equals("") || trimmedLine.startsWith("!"))){
				
				
				//line = removeCommentAtTheEnd(line,'!');

				/*if(trimmedLine.startsWith("0.")||trimmedLine.startsWith("1.")||trimmedLine.startsWith("2.")||
								trimmedLine.startsWith("3.")||trimmedLine.startsWith("4.")||trimmedLine.startsWith("5.")||
								trimmedLine.startsWith("6.")||trimmedLine.startsWith("7.")||trimmedLine.startsWith("8.")||trimmedLine.startsWith("9.")){
							CRNReducerCommandLine.println(out,bwOut"ciao");
						}

						if(trimmedLine.startsWith("mb2oh3oo  11/28/99      c   5h   9o   5    0g   300.000  5000.000 1392.000    71")){
							CRNReducerCommandLine.println(out,bwOut"ciao");
						}*/

				



				//First line: name and ranges
				//CRNReducerCommandLine.println(out,bwOutline);
				String speciesName = line.substring(0, 17).trim();
				speciesName = removeSpecialCharacters(speciesName.split("\\s+")[0]);
				
				//CRNReducerCommandLine.println(out,bwOut"["+speciesName+"] in "+line);
				/*if(speciesName.equals("C6H5CH2O")){
							CRNReducerCommandLine.println(out,bwOut"["+speciesName+"] in "+line);
						}
						if(speciesName.startsWith("C5H6")){
							CRNReducerCommandLine.println(out,bwOut"["+speciesName+"] in "+line);
						}
						if(speciesName.startsWith("C14H13")){
							CRNReducerCommandLine.println(out,bwOut"["+speciesName+"] in "+line);
						}
						if(speciesName.startsWith("C6H5CH2O")){
							CRNReducerCommandLine.println(out,bwOut"["+speciesName+"] in "+line);
						}*/


				ISpecies species = speciesStoredInHashMap.get(speciesName);
				if(species==null){
					/*if(speciesName.startsWith("0.")||speciesName.startsWith("1.")||speciesName.startsWith("2.")||
							speciesName.startsWith("3.")||speciesName.startsWith("4.")||speciesName.startsWith("5.")||
							speciesName.startsWith("6.")||speciesName.startsWith("7.")||speciesName.startsWith("8.")||
							speciesName.startsWith("9.")){
						CRNReducerCommandLine.println(out,bwOut"ciao");
					}*/
					//String message="Warning: unknown species "+ speciesName +" in thermoDynamic file";
					//CRNReducerCommandLine.printWarning(out,bwOut,message);
					//throw new IOException(message);
					//CRNReducerCommandLine.println(out,bwOut"AAA "+line);
					line=readOneLineIgnoringCommentsAndEmptyLines(br);
					//CRNReducerCommandLine.println(out,bwOut"BBB "+line);
					line=readOneLineIgnoringCommentsAndEmptyLines(br);
					//CRNReducerCommandLine.println(out,bwOut"CCC "+line);
					line=readOneLineIgnoringCommentsAndEmptyLines(br);
					//CRNReducerCommandLine.println(out,bwOut"DDD "+line);
					//CRNReducerCommandLine.println(out,bwOut"DDD "+line);
				}
				else{
					//CRNReducerCommandLine.println(out,bwOutline.substring(45, 54)+" at line"+line);
					BigDecimal lowTOfSpecies = cleanAndGetBigDecimal(line.substring(45, 54),true);
					//CRNReducerCommandLine.println(out,bwOutline.substring(55, 64)+" in line "+line);
					BigDecimal highTOfSpecies = cleanAndGetBigDecimal(line.substring(55, 64),true);
					BigDecimal commonTOfSpecies = cleanAndGetBigDecimal(line.substring(65, 72),true);
					if(commonTOfSpecies==null){
						commonTOfSpecies=commonT;
					}

					//Second line: a1-a5 of the upper interval
					line=readOneLineIgnoringCommentsAndEmptyLines(br);//line = br.readLine();
					BigDecimal[] upperIntervalCoefficients = new BigDecimal[7];
					int coeffLength=15;
					int begin = 0;
					int end = coeffLength;
					// 0.02500000e+02 0.00000000e+00 0.00000000e+00 0.00000000e+00 0.00000000e+00    2
					for(int i=1;i<=5;i++){
						String a = line.substring(begin, end);
						//CRNReducerCommandLine.println(out,bwOuta+" in line: "+line);
						upperIntervalCoefficients[i-1]= cleanAndGetBigDecimal(a,true);
						begin += coeffLength;
						end += coeffLength;
					}

					//Third line a6-a7 of the upper interval, a1-a3 of the lower interval
					line=readOneLineIgnoringCommentsAndEmptyLines(br);//line = br.readLine();//-0.07453750e+04 0.04366001e+02 0.02500000e+02 0.00000000e+00 0.00000000e+00    3
					String a = line.substring(0, coeffLength);
					upperIntervalCoefficients[5]=cleanAndGetBigDecimal(a,true);
					a = line.substring(coeffLength, 2*coeffLength);
					upperIntervalCoefficients[6]=cleanAndGetBigDecimal(a,true);

					BigDecimal[] lowerIntervalCoefficients = new BigDecimal[7];
					begin=2*coeffLength;
					end=3*coeffLength;
					for(int i=1;i<=3;i++){
						a = line.substring(begin, end);
						//CRNReducerCommandLine.println(out,bwOuta+" in line "+line);
						lowerIntervalCoefficients[i-1]=cleanAndGetBigDecimal(a,true);
						begin += coeffLength;
						end += coeffLength;
					}


					//Fourth line a4-a7 of lower interval
					line=readOneLineIgnoringCommentsAndEmptyLines(br);//line = br.readLine();
					begin=0;
					end=coeffLength;
					for(int i=4;i<=7;i++){
						a = line.substring(begin, end);
						//CRNReducerCommandLine.println(out,bwOuta+" at line "+line);
						lowerIntervalCoefficients[i-1]=cleanAndGetBigDecimal(a,true);
						begin += coeffLength;
						end += coeffLength;
					}

					//counter++;
					thermoDynamicInfo.put(species, new ThermoDynamicInfoChemKin(lowTOfSpecies, highTOfSpecies, commonTOfSpecies, upperIntervalCoefficients, lowerIntervalCoefficients));
				}
			}
			line=br.readLine();
			trimmedLine = line;
			if(line!=null){
				trimmedLine=line.trim();
			}
		}


		//}
		//Any other line is ignored (including comments)
		/*else if(line.startsWith("!")){
				//if this is a comment line, skip to the next iteration of the loop (i.e. to the next line)
				continue;
			}*/
		//}

		/*for (ISpecies species : this.thermoDynamicInfo.keySet()) {
			CRNReducerCommandLine.println(out,bwOut"thermo info for:"+species.getName());
		}*/
		
		return true;
	} 

	private String readOneLineIgnoringCommentsAndEmptyLines(BufferedReader br) throws IOException {
		String line = br.readLine();
		//if(line!=null){
			String trimmedLine = line.trim();

			while(trimmedLine.startsWith("!")||trimmedLine.equals("")){
				line = br.readLine();
				trimmedLine = line.trim();
			}
		//}
		return line;
	}

	private BigDecimal cleanAndGetBigDecimal(String number, boolean addE) {
		number=number.trim();
		if(number.contains("E ")||number.contains("e ")){
			number=number.replace("E ","E+");
			number=number.replace("e ","e+");
		}
		if(addE){
			int posOfMinus = number.indexOf("-",1);
			if(posOfMinus>0){
				if(!(number.substring(posOfMinus-1, posOfMinus).equals("E")||number.substring(posOfMinus-1, posOfMinus).equals("e"))){
					number = number.substring(0, posOfMinus)+"E"+number.substring(posOfMinus,number.length());
				}
			}
			/*else{
				int posOfPlus = number.indexOf("+",1);
				if(posOfPlus>0){
					if(!(number.substring(posOfPlus-1, posOfPlus).equals("E")||number.substring(posOfPlus-1, posOfPlus).equals("e"))){
						number = number.substring(0, posOfPlus)+"E"+number.substring(posOfPlus,number.length());
					}
				}
			}*/
		}
		/*if(number.endsWith(".")){
			CRNReducerCommandLine.print(out,number +" ");
			number=number.substring(0,number.length()-1);
			CRNReducerCommandLine.println(out,bwOutnumber);
		}*/

		number=number.trim();
		//CRNReducerCommandLine.println(out,bwOut"["+number+"]");
		if(number.equals("")){
			return null;
		}
		return BigDecimal.valueOf(Double.valueOf(number));
	}

	private void addDefaultParameters() {
		/*getCRN().addParameter(TName,T.toPlainString());
		getMath().setVariable(TName,T.doubleValue());*/
		/*getCRN().addParameter(RName,R.toPlainString());
		getMath().setVariable(RName,R.doubleValue());
		getCRN().addParameter(RCName,Rc.toPlainString());
		getMath().setVariable(RCName,Rc.doubleValue());*/
		//getCRN().addParameter(PatmName,Patm.toPlainString());
		//getMath().setVariable(PatmName,Patm.doubleValue());
	}

	/**
	 * 
	 * @param br
	 * @param math
	 * @param crn
	 * @param uniqueBlock
	 * @param infoImporting
	 * @throws IOException
	 * It is assumed that the id of a species corresponds to the order with which it has been inserted: the i_th inserted species has id "i". 
	 * Also, we start counting from 0 rather than from 1, and thus we decrease by one the ids of all species.
	 */
	private void loadSpecies(BufferedReader br,String line) throws IOException {
		/*SPEC ! SPEC is equivalent to SPECIES 
		  H2 O2
		  H  O  OH  HO2  N2  N  NO
		END*/
		/*SPECIES H2 O2 H O OH HO2 N2 N NO END*/
		/*SPEC H2*/
		/*spec O2*/

		//TODO I only consider the first case (spec \n ...  \n end )

		//I use this data structure to efficiently to retrieve a species from its name
		speciesStoredInHashMap = new HashMap<String, ISpecies>();

		int id=0;


		///line = br.readLine();
		/*if(line!=null){
			line=line.trim();
		}*/
		boolean end=false;
		while ((line != null) && (! (line.startsWith("END") || line.startsWith("end")) ) && !end ) {
			if(!(line.equals("") || line.startsWith("!"))){
				line = removeCommentAtTheEnd(line,'!');

				//String[] speciesInLine = line.split(" +");
				//CRNReducerCommandLine.println(out,bwOutline);
				String[] speciesInLine = line.split("\\s+");
				//CRNReducerCommandLine.println(out,bwOutArrays.toString(speciesInLine));
				//speciesInLine.length!=0
				int s = 0; 
				if(speciesInLine[0].equalsIgnoreCase("spec")||speciesInLine[0].equalsIgnoreCase("species")){
					s=1;
				}
				int skip=0;
				if((speciesInLine[speciesInLine.length-1].equalsIgnoreCase("END")||speciesInLine[speciesInLine.length-1].equalsIgnoreCase("end"))){
					skip=1;
					end=true;
				}
				for(;s<speciesInLine.length-skip;s++){
					speciesInLine[s]=removeSpecialCharacters(speciesInLine[s]);
					if(!(speciesInLine[s].equals("")||(speciesInLine[s].equals(" ")))){
						if(speciesStoredInHashMap.containsKey(speciesInLine[s])){
							CRNReducerCommandLine.printWarning(out,bwOut,false,msgDialogShower,"Multiple declaration of species ["+speciesInLine[s]+"] ignored",DialogType.Warning);
						}
						else{
							/*if(speciesInLine[s].equals("C6H5CH2O")){
								CRNReducerCommandLine.println(out,bwOut"["+speciesInLine[s]+"] in "+line);
							}*/
							ISpecies species = new Species(speciesInLine[s], id, BigDecimal.ZERO,"0.0",false);
							getCRN().addSpecies(species);
							speciesStoredInHashMap.put(speciesInLine[s], species);
							id++;
						}
					}
				}
			}
			line = br.readLine();
			if(line!=null){
				line=line.trim();
			}
		}

		getInfoImporting().setReadSpecies(getCRN().getSpecies().size());
	}

	private String removeSpecialCharacters(String string) {
		string = string.replace("*", "_st_");
		string = string.replace("-", "_mi_");
		string = string.replace(",", "_co_");
		string = string.replace("#", "_sh_");
		string = string.replace("[", "_spo_");
		string = string.replace("]", "_spc_");
		
		
		boolean startsWithDot = false;
		boolean startsWithZeroDot = false;
		if(string.startsWith(".")){
			string=string.substring(1);
			startsWithDot=true;
		}
		else if(string.startsWith("0.")){
			string=string.substring(2);
			startsWithZeroDot=true;
		}
		
		string = string.replace(".", "_dot_");
		if(startsWithDot){
			string="."+string;
		}
		else if(startsWithZeroDot){
			string="0."+string;
		}
		
		
		
		
		if(string.contains("(")||string.contains(")")){
			//System.out.print(string+"  ");
			boolean endsWithPar=false;
			boolean endsWithMPar=false;
			boolean endsWithmPar=false;
			if(string.endsWith("(")){
				string = string.substring(0,string.length()-1);
				endsWithPar=true;
			}
			else if(string.endsWith("m)")){
				string = string.substring(0,string.length()-2);
				endsWithmPar=true;
			}
			else if(string.endsWith("M)")){
				string = string.substring(0,string.length()-2);
				endsWithMPar=true;
			}
			else if(string.endsWith(")") && !string.contains("(")){
				//System.out.println(string);
				return string;
			}
			//string = string.replace("(+", "__[+__");
			string = string.replace("(", "_po_");
			string = string.replace(")", "_pc_");
			//string = string.replace("__[+__","(+");
			if(endsWithPar){
				string=string+"(";
			}
			else if(endsWithMPar){
				string=string+"M)";
			}
			else  if(endsWithmPar){
				string=string+"m)";
			} 
			//System.out.println(string);
		}
		//string = string.replace("(", "po");
		//string = string.replace(")", "pc");
		return string;
	}

	/**
	 * It is assumed that at least two blank spaces separate the last product from the rates 
	 * @param br
	 * @param uniqueBlock 
	 * @param crn
	 * @param math
	 * @param infoImporting
	 * @throws IOException
	 * @throws UnsupportedFormatException 
	 */
	private HashMap<Double, String> loadReactions(BufferedReader br, String unitsLine) throws IOException, UnsupportedFormatException {
		HashMap<Double, String> createdKineticConstants=new HashMap<>();

		//We now check if units are defined for the reactions. If they are not defined, then it is assumed that they are cal/mol
		//reactions        cal/mole
		//If they are not defined, then it is assumed that they are cal/mol
		R=BigDecimal.valueOf(1.9872041);
		unitsLine=removeCommentAtTheEnd(unitsLine,'!');
		String[] tokens = unitsLine.split("\\s+");
		for(int t=0;t<tokens.length;t++){
			tokens[t]=tokens[t].toLowerCase();
			if (tokens[t].startsWith("kcal/mol")){
				R = BigDecimal.valueOf(1.9872041e-3);//1.987e-3;
				CRNReducerCommandLine.println(out,bwOut,tokens[t]);
			}
			else if (tokens[t].startsWith("kj/mol")){
				R = BigDecimal.valueOf(8.3144621E-3);//8.314e-3;
				CRNReducerCommandLine.println(out,bwOut,tokens[t]);
			}
			else if (tokens[t].startsWith("j/mol")){
				R = BigDecimal.valueOf(8.3144621);// 8.314;
				CRNReducerCommandLine.println(out,bwOut,tokens[t]);
			}
		}
		//getCRN().addParameter(RName,R.toPlainString());
		getMath().setVariable(RName,R.doubleValue());


		boolean checkThermoData=true;


		ArrayList<ThreeBodyAndPressureDependentReactionInfo> threeBodyAndPressureDependentReactions = new ArrayList<ThreeBodyAndPressureDependentReactionInfo>();
		boolean previouslyUnsupportedReaction=false;
		String lineToReconsider=null;
		//H2+O2 = 2OH  A b E
		//H2+O2 <=> 2OH  A b E
		//H2+O2 => 2OH  A b E
		//Where 2OH is equal to OH + OH
		String line = br.readLine(); 
		if(line!=null){
			line=line.trim();
		}
		while ((line != null) && (!(line.startsWith("END")||line.startsWith("end")))) {
			//CRNReducerCommandLine.println(out,bwOutline);
			boolean thirdBodyReaction=false;
			boolean pressureDependentReaction=false;
			//Skip comments or empty lines
			if(!(line.equals("") || line.startsWith("!")||line.startsWith("DUP")||line.startsWith("dup")||line.startsWith("Dup")||line.startsWith("DUp"))){

				line=removeCommentAtTheEnd(line,'!');


				if((!line.contains("/"))){
					previouslyUnsupportedReaction=false;
					//Read the reaction
					boolean reversible = false;
					int arrowLength=-1;
					int arrowPos = line.indexOf("<=>");
					if(arrowPos!=-1){
						reversible = true;
						arrowLength=3;
					}
					else{
						arrowPos = line.indexOf("=>");
						if(arrowPos!=-1){
							reversible = false;
							arrowLength=2;
						}
						else{
							arrowPos = line.indexOf("=");
							if(arrowPos!=-1){
								reversible=true;
								arrowLength=1;
							}
							else{
								String message="Cannot find any of <=>, = or => in the following reaction: "+line;
								//CRNReducerCommandLine.printWarning(out,bwOut,true,msgDialogShower,"Cannot find any of <=>, = or => in the following reaction: "+line,DialogType.Error);
								throw new UnsupportedFormatException(message);
							}
						}
					}
					
					if(reversible &&checkThermoData){
						checkThermoData=false;
						if(thermoDynamicInfo.size()!=getCRN().getSpecies().size()){
							String message= "A thermodynamic entry per species should be provided. While we have "+getCRN().getSpecies().size()+" species and " + thermoDynamicInfo.size()+" entries";
							//CRNReducerCommandLine.printWarning(out,bwOut,message);
							
							//CRNReducerCommandLine.println(out,bwOutcounter);
							//CRNReducerCommandLine.println(out,bwOutthermoDynamicInfo.keySet());
							int counter=1;
							Set<ISpecies> thermoDynamicInfoKeys = thermoDynamicInfo.keySet();
							for (ISpecies iSpecies : getCRN().getSpecies()) {
								if(!thermoDynamicInfoKeys.contains(iSpecies)){
									CRNReducerCommandLine.println(out,bwOut,counter+") The species ["+iSpecies.getName()+"] does not have thermoDynamic info");
									counter++;
								}
							}
							throw new UnsupportedFormatException(message);
						}
					}

					String reagentsString = line.substring(0, arrowPos).trim();
					//Contains also the AArhanius rate
					String productsString = line.substring(arrowPos+arrowLength,line.length()).trim();
					//String[] arrhenius = rateString.split("\\s+");
					String[] searchArrhenius = productsString.split("\\s+");
					if(searchArrhenius.length<3){
						String message = "Cannot find the three arrhenius coefficients in the following reaction: "+line;
						//CRNReducerCommandLine.printWarning(out,bwOut,message);
						throw new UnsupportedFormatException(message);
					}
					String[] arrhenius = new String[3];
					arrhenius[0]=searchArrhenius[searchArrhenius.length-3];
					arrhenius[1]=searchArrhenius[searchArrhenius.length-2];
					arrhenius[2]=searchArrhenius[searchArrhenius.length-1];
					if(arrhenius[2].endsWith(".")){
						arrhenius[2]=arrhenius[2].substring(0, arrhenius[2].length()-1);
					}
					productsString=productsString.substring(0, productsString.indexOf(arrhenius[0])).trim();
					for(int i=0;i<arrhenius.length;i++){
						arrhenius[i]=arrhenius[i].replace(".e+", "e+");
						arrhenius[i]=arrhenius[i].replace(".E+", "E+");
						arrhenius[i]=arrhenius[i].replace(".e-", "e-");
						arrhenius[i]=arrhenius[i].replace(".E-", "E-");
					}




					/*int doubleSpacePos = productsString.indexOf("  ");
					if(doubleSpacePos==-1){
						doubleSpacePos = productsString.indexOf("\t ");
						if(doubleSpacePos==-1){
							doubleSpacePos = productsString.indexOf(" \t");
						}
						if(doubleSpacePos==-1){
							doubleSpacePos = productsString.indexOf("\t\t");
						}
					}
					if(doubleSpacePos==-1){
						String message = "Warning: it is assumed that at least two blank spaces or tabs separate the last product from the rate: "+line;
						CRNReducerCommandLine.printWarning(out,bwOut,message);
						throw new IOException(message);
					}
					String rateString = productsString.substring(doubleSpacePos, productsString.length()).trim();
					if(rateString.endsWith(".")){
						rateString=rateString.substring(0, rateString.length()-1);
					}
					productsString=productsString.substring(0, doubleSpacePos).trim();*/

					String IONIZED = "{IONIZED}";
					//TODO: check if correct
					reagentsString = replaceIonizedPLusWithIonizedString(reagentsString,IONIZED);
					productsString = replaceIonizedPLusWithIonizedString(productsString,IONIZED);

					String[] reagentsArray = reagentsString.split("\\+");
					String[] productsArray = productsString.split("\\+");
					for(int i=0;i<reagentsArray.length;i++){
						reagentsArray[i]=removeSpecialCharacters(reagentsArray[i].trim());
						
					}
					for(int i=0;i<productsArray.length;i++){
						productsArray[i]=removeSpecialCharacters(productsArray[i].trim());
					}


					CompositeHashMapAndInfoOnReaction compositeReagentsHMAndInfo = createCompositeHashMapAndRestoreIonizedName(reagentsArray,IONIZED,line);
					CompositeHashMapAndInfoOnReaction compositeProductsHMAndInfo = createCompositeHashMapAndRestoreIonizedName(productsArray,IONIZED,line,compositeReagentsHMAndInfo.getNote());
					HashMap<ISpecies, Integer> compositeReagentsHM = compositeReagentsHMAndInfo.getCompositeHM();
					HashMap<ISpecies, Integer> compositeProductsHM = compositeProductsHMAndInfo.getCompositeHM();

					thirdBodyReaction=
							compositeReagentsHMAndInfo.getNote().equals(ChemKinReactionNote.THIRDBODY) || 
							compositeProductsHMAndInfo.getNote().equals(ChemKinReactionNote.THIRDBODY);

					pressureDependentReaction=
							compositeReagentsHMAndInfo.getNote().equals(ChemKinReactionNote.PRESSUREDEPENDENT) || 
							compositeProductsHMAndInfo.getNote().equals(ChemKinReactionNote.PRESSUREDEPENDENT);

					boolean unsupportedReaction=
							compositeReagentsHMAndInfo.getNote().equals(ChemKinReactionNote.UNSUPPORTED) || 
							compositeProductsHMAndInfo.getNote().equals(ChemKinReactionNote.UNSUPPORTED);


					if(!unsupportedReaction){
						if(!(thirdBodyReaction ||pressureDependentReaction)){							
							lineToReconsider=handleSupportedSimpleReaction(br,compositeReagentsHM,compositeProductsHM,arrhenius,line,reversible,true,createdKineticConstants);
						}
						else if(thirdBodyReaction){
							//TODO
							lineToReconsider= handleSupportedThirdBodyORPressureDependentReaction(compositeReagentsHM,compositeProductsHM,arrhenius,line,reversible,br,threeBodyAndPressureDependentReactions,true,createdKineticConstants);
						}
						else {
							//TODO
							//pressureDependentReaction
							lineToReconsider=handleSupportedThirdBodyORPressureDependentReaction(compositeReagentsHM,compositeProductsHM,arrhenius,line,reversible,br,threeBodyAndPressureDependentReactions,false,createdKineticConstants);
						}
					}
					else{
						//unsupported reaction
						previouslyUnsupportedReaction=true;
						getInfoImporting().setLoadingCRNFailed();
					}
				}
				else //line.contains("/") 
					if(!previouslyUnsupportedReaction){
						if(!getInfoImporting().getLoadingCRNFailed()){
							String message="Found unexpected line with \"/\" symbol: "+line;
							CRNReducerCommandLine.printWarning(out,bwOut,true,msgDialogShower,message,DialogType.Warning);
						}
						getInfoImporting().setLoadingCRNFailed();
					}
			}

			//if(thirdBodyReaction || pressureDependentReaction){
			if(lineToReconsider!=null){
				line = lineToReconsider;
				lineToReconsider=null;
				line=line.trim();
			}
			else{
				line = br.readLine();
				if(line!=null){
					line=line.trim();
				}
			}
		}

		CRNReducerCommandLine.println(out,bwOut,threeBodyAndPressureDependentReactions.size()+" reactions to expand.");
		for (ThreeBodyAndPressureDependentReactionInfo threeBodyAndPressureDependentReactionInfo : threeBodyAndPressureDependentReactions) {
			expandThreeBodyAndPressureDependentReactions(threeBodyAndPressureDependentReactionInfo);
		}
		
		for (ICRNReaction reaction : getCRN().getReactions()) {
			reaction.setRate(reaction.getRate().setScale(CRNBisimulationsNAry.getSCALE(),CRNBisimulationsNAry.RM), reaction.getRateExpression());
		}

		getInfoImporting().setReadSpecies(getCRN().getSpecies().size());
		getInfoImporting().setReadCRNReactions(getCRN().getReactions().size());
		//getInfoImporting().setReadReagents(getCRN().getReagents().size());
		//getInfoImporting().setReadProducts(getCRN().getProducts().size());
		return createdKineticConstants;
	}

	private void expandThreeBodyAndPressureDependentReactions(ThreeBodyAndPressureDependentReactionInfo threeBodyAndPressureDependentReactionInfo) {
		LinkedHashMap<ISpecies, Double> weights = threeBodyAndPressureDependentReactionInfo.getWeights();
		HashMap<ISpecies, Integer> compositeReagentsHM = threeBodyAndPressureDependentReactionInfo.getCompositeReagentsHM();
		HashMap<ISpecies, Integer> compositeProductsHM = threeBodyAndPressureDependentReactionInfo.getCompositeProductsHM();
		String rateExpression = threeBodyAndPressureDependentReactionInfo.getRateExpression();
		BigDecimal reactionRate = threeBodyAndPressureDependentReactionInfo.getReactionRate();
		String reverseRateExpression = threeBodyAndPressureDependentReactionInfo.getReverseRateExpression();
		BigDecimal reverseReactionRate = threeBodyAndPressureDependentReactionInfo.getReverseReactionRate();
		boolean reversible=threeBodyAndPressureDependentReactionInfo.isReversible();

		for (ISpecies catalyst : getCRN().getSpecies()) {

			Double weightDouble =weights.get(catalyst);
			if(weightDouble==null){
				weightDouble=1.0;
			}
			BigDecimal weight = BigDecimal.valueOf(weightDouble);
			if(weight.compareTo(BigDecimal.ZERO)!=0){
				HashMap<ISpecies, Integer> compositeReagentsHMExpanded = copyAndAddSpecies(compositeReagentsHM,catalyst);
				HashMap<ISpecies, Integer> compositeProductsHMExpanded = copyAndAddSpecies(compositeProductsHM,catalyst);

				//IComposite compositeReagents = new Composite(compositeReagentsHMExpanded);
				IComposite compositeReagents = CRN.compositeOrSpecies(compositeReagentsHMExpanded);
				//IComposite compositeProducts = new Composite(compositeProductsHMExpanded);
				IComposite compositeProducts = CRN.compositeOrSpecies(compositeProductsHMExpanded);
				//int arity = compositeReagents.getTotalMultiplicity();

				BigDecimal rate = reactionRate.multiply(weight);
				if(rate.compareTo(BigDecimal.ZERO)!=0){
					//compositeReagents = getCRN().addReagentsIfNew(compositeReagents);
					//compositeProducts = getCRN().addProductIfNew(compositeProducts);
					ICRNReaction reaction = new CRNReaction(rate, compositeReagents, compositeProducts, rateExpression+"*"+weight.toPlainString(),null);
					getCRN().addReaction(reaction);
					/*
					addToIncomingReactionsOfProducts(arity,compositeProducts, reaction,CRNReducerCommandLine.addReactionToComposites);
					addToOutgoingReactionsOfReagents(arity, compositeReagents, reaction,CRNReducerCommandLine.addReactionToComposites);
					addToReactionsWithNonZeroStoichiometry(arity, reaction.computeProductsMinusReagentsHashMap(),reaction);
					*/
					/*if(arity==2 && reaction.getReagents().getFirstReagent().equals(reaction.getReagents().getSecondReagent())){
						CRNReducerCommandLine.ModelsWithHomeoReactions.add(getFileName());
					}*/
				}

				if(reversible){
					//arity = compositeProducts.getTotalMultiplicity();
					BigDecimal reverseRate = reverseReactionRate.multiply(weight);
					if(reverseReactionRate.multiply(weight).compareTo(BigDecimal.ZERO)!=0){
						//compositeReagents = getCRN().addReagentsIfNew(compositeReagents);
						//compositeProducts = getCRN().addProductIfNew(compositeProducts);

						ICRNReaction reaction = new CRNReaction(reverseRate, compositeProducts, compositeReagents, reverseRateExpression+"*"+weight.toPlainString(),null);
						getCRN().addReaction(reaction);
						/*
						addToIncomingReactionsOfProducts(arity,compositeReagents, reaction,CRNReducerCommandLine.addReactionToComposites);
						addToOutgoingReactionsOfReagents(arity, compositeProducts, reaction,CRNReducerCommandLine.addReactionToComposites);
						addToReactionsWithNonZeroStoichiometry(arity, reaction.computeProductsMinusReagentsHashMap(),reaction);
						*/
						/*if(arity==2 && reaction.getReagents().getFirstReagent().equals(reaction.getReagents().getSecondReagent())){
							CRNReducerCommandLine.ModelsWithHomeoReactions.add(getFileName());
						}*/
					}
				}
			}
		}
	}

	private String handleSupportedSimpleReaction(BufferedReader br,
			HashMap<ISpecies, Integer> compositeReagentsHM,
			HashMap<ISpecies, Integer> compositeProductsHM, String[] arrhenius, String line, boolean reversible,boolean searchForRev, HashMap<Double,String> createdKineticConstants) throws IOException, UnsupportedFormatException {
		return handleSupportedSimpleReaction(br,compositeReagentsHM,compositeProductsHM,arrhenius,line,reversible,null,null,searchForRev,createdKineticConstants);
	}

	private String handleSupportedSimpleReaction(BufferedReader br,
			HashMap<ISpecies, Integer> compositeReagentsHM,
			HashMap<ISpecies, Integer> compositeProductsHM, String[] arrhenius, String line, boolean reversible, BigDecimal reverseReactionRate, String reverseRateExpression, boolean searchForRev, HashMap<Double,String> createdKineticConstants) throws IOException, UnsupportedFormatException {

		String lineToReconsider=null;
		//IComposite compositeReagents = new Composite(compositeReagentsHM);
		//IComposite compositeProducts = new Composite(compositeProductsHM);
		IComposite compositeReagents = CRN.compositeOrSpecies(compositeReagentsHM);
		IComposite compositeProducts = CRN.compositeOrSpecies(compositeProductsHM);

		//int arity = compositeReagents.getTotalMultiplicity();

		//CRNReducerCommandLine.println(out,bwOutline);
		//StringAndBigDecimal reactionRateAndExpression = computeArrheniusRateLOG(arrhenius);
		StringAndBigDecimal reactionRateAndExpression = computeArrheniusRate(arrhenius);
		//StringAndBigDecimal reactionRateAndExpression = computeArrheniusRateDirect(arrhenius);
		BigDecimal reactionRate = reactionRateAndExpression.getBigDecimal();
		String rateExpression = reactionRateAndExpression.getString();

		if(reactionRate.compareTo(BigDecimal.ZERO)!=0){
			rateExpression=convertToParameter(reactionRate,rateExpression,createdKineticConstants);
			//compositeReagents = getCRN().addReagentsIfNew(compositeReagents);
			//IComposite compositeProducts = getCRN().addReagentsIfNew(new Composite(compositeProductsHM));
			//compositeProducts = getCRN().addProductIfNew(compositeProducts);
			ICRNReaction reaction = new CRNReaction(reactionRate, compositeReagents, compositeProducts, rateExpression,null);
			getCRN().addReaction(reaction);
			/*
			addToIncomingReactionsOfProducts(arity,compositeProducts, reaction,CRNReducerCommandLine.addReactionToComposites);
			addToOutgoingReactionsOfReagents(arity, compositeReagents, reaction,CRNReducerCommandLine.addReactionToComposites);
			addToReactionsWithNonZeroStoichiometry(arity, reaction.computeProductsMinusReagentsHashMap(),reaction);
			*/
			/*if(arity==2 && reaction.getReagents().getFirstReagent().equals(reaction.getReagents().getSecondReagent())){
				CRNReducerCommandLine.ModelsWithHomeoReactions.add(getFileName());
			}*/
		}

		if(reversible){
			//arity = compositeProducts.getTotalMultiplicity();

			if((reverseReactionRate==null || reverseRateExpression==null) && searchForRev){
				//I have to search for the keyword REV. If I find it, I have to use directly that arrhenius rate. Otherwise I compute the reversed one
				boolean stop=false;
				String lineForRev=br.readLine();
				if(lineForRev!=null){
					lineForRev=lineForRev.trim();
				}
				while (lineForRev != null && !stop) {
					if(!(lineForRev.equals("")||lineForRev.startsWith("!"))){
						lineForRev=removeCommentAtTheEnd(lineForRev,'!');
						if(lineForRev.contains("/")){
							StringAndBigDecimal reverseReactionRateAndExpression = checkIfContainsExplicitReversedArrhenius(lineForRev, line); 
							if(reverseReactionRateAndExpression!=null){
								reverseReactionRate = reverseReactionRateAndExpression.getBigDecimal();
								reverseRateExpression = reverseReactionRateAndExpression.getString();
							}
						}
						else{
							stop=true;
							lineToReconsider=lineForRev;
						}
					}
					if(!stop){
						lineForRev = br.readLine();
						if(lineForRev!=null){
							lineForRev=lineForRev.trim();
						}
					}
				}
			}

			if(reverseReactionRate==null || reverseRateExpression==null){
				//I did not find the ref keyword. Hence, I compute the reverse arrhenius rate. 
				//StringAndBigDecimal reverseReactionRateAndExpression = computeReverseArrheniusRateLOG(reactionRate,rateExpression,line,compositeReagents,compositeProducts);
				StringAndBigDecimal reverseReactionRateAndExpression = computeReverseArrheniusRate(reactionRate,rateExpression,line,compositeReagents,compositeProducts);
				//StringAndBigDecimal reverseReactionRateAndExpression = computeReverseArrheniusRateDirect(reactionRate,rateExpression,line,compositeReagents,compositeProducts);
				reverseReactionRate = reverseReactionRateAndExpression.getBigDecimal();
				reverseRateExpression = reverseReactionRateAndExpression.getString();
			}

			if(reverseReactionRate.compareTo(BigDecimal.ZERO)!=0){
				reverseRateExpression = convertToParameter(reverseReactionRate, reverseRateExpression, createdKineticConstants);
				//compositeReagents = getCRN().addProductIfNew(compositeReagents);
				//compositeProducts = getCRN().addReagentsIfNew(compositeProducts);
				ICRNReaction reaction = new CRNReaction(reverseReactionRate, compositeProducts, compositeReagents, reverseRateExpression,null);
				getCRN().addReaction(reaction);
				/*
				addToIncomingReactionsOfProducts(arity,compositeReagents, reaction,CRNReducerCommandLine.addReactionToComposites);
				addToOutgoingReactionsOfReagents(arity, compositeProducts, reaction,CRNReducerCommandLine.addReactionToComposites);
				addToReactionsWithNonZeroStoichiometry(arity, reaction.computeProductsMinusReagentsHashMap(),reaction);
				*/
				/*if(arity==2 && reaction.getReagents().getFirstReagent().equals(reaction.getReagents().getSecondReagent())){
					CRNReducerCommandLine.ModelsWithHomeoReactions.add(getFileName());
				}*/
			}
		}

		return lineToReconsider;

	}

	private String convertToParameter(BigDecimal reactionRate, String rateExpression,
			HashMap<Double, String> createdKineticConstants) {
		if(reactionRate==null || rateExpression == null){
			return null;
		}
		Double d = reactionRate.doubleValue();
		String param = createdKineticConstants.get(d);
		if(param==null){
			param = "cp"+createdKineticConstants.size();
			//getCRN().addParameter(param, rateExpression);
			addParameter(param, reactionRate.toPlainString());
			createdKineticConstants.put(d, param);
		}
		return param;
	}

	/*private ISpecies addNewSpecies(String speciesName){
		//CRNReducerCommandLine.printWarning(out,bwOut,"Warning: I cannot find the species: "+speciesName+". I add it.");
		//It is necessary for JP10sandiego20021001.mec.inp
		ISpecies species = new Species(speciesName, getCRN().getSpecies().size(), BigDecimal.ZERO,"0.0");
		getCRN().addSpecies(species);
		speciesStoredInHashMap.put(speciesName, species);
		getInfoImporting().setReadSpecies(getCRN().getSpecies().size());
		return species;
	}*/

	/*private String handleSupportedThirdBodyReaction(
			HashMap<ISpecies, Integer> compositeReagentsHM,
			HashMap<ISpecies, Integer> compositeProductsHM, String[] arrhenius, String lineOfReaction, boolean reversible, BufferedReader br,ArrayList<ThreeBodyAndPressureDependentReactionInfo> threeBodyAndPressureDependentReactions) throws IOException {


		StringAndBigDecimal reactionRateAndExpression = computeArrheniusRate(arrhenius);
		BigDecimal reactionRate = reactionRateAndExpression.getBigDecimal();
		String rateExpression = reactionRateAndExpression.getString();

		StringAndBigDecimal reverseReactionRateAndExpression = computeReverseArrheniusRate(reactionRate,rateExpression,lineOfReaction);
		BigDecimal reverseReactionRate = reverseReactionRateAndExpression.getBigDecimal();
		String reverseRateExpression = reverseReactionRateAndExpression.getString();


		//I first read the weights of all species (which tell us how each species acts as third body).
		String line = br.readLine(); 
		if(line!=null){
			line=line.trim();
		}
		boolean stop=false;
		LinkedHashMap<ISpecies, Double> weights = new LinkedHashMap<ISpecies, Double>();
		while (line != null && !stop) {
			if(!(line.equals("")||line.startsWith("!"))){
				line=removeCommentAtTheEnd(line,'!');
				if(line.contains("/")){

					// H2/2.5/ H2O/12/
   					// AR/0.0/  HE/0.0/
   					// CO/1.9/ CO2/3.8/ 

					int from =0; 
					while(from<=line.length()){
						//TODO: handle -1 cases
						int firstSlash = line.indexOf("/",from);

						if(firstSlash==-1){
							from = line.length()+1;
						}
						else{
							int secondSlash = line.indexOf("/", firstSlash+1);
							String speciesName = line.substring(from, firstSlash).trim();

							if(speciesName.startsWith("rev")||speciesName.startsWith("REV")||speciesName.startsWith("Rev")){
								String reverseArrheniusString = line.substring(firstSlash+1, secondSlash).trim();
								String[] reverseArrhenius = reverseArrheniusString.split("\\s+");
								if(reverseArrhenius.length!=3){
									throw new IOException("I could not parse the arrhenius coefficients "+ line +" of the reverse reaction: "+lineOfReaction);
								}
								if(reverseArrhenius[2].endsWith(".")){
									reverseArrhenius[2]=reverseArrhenius[2].substring(0, reverseArrhenius[2].length()-1);
								}
								reverseReactionRateAndExpression = computeArrheniusRate(reverseArrhenius);
								reverseReactionRate = reverseReactionRateAndExpression.getBigDecimal();
								reverseRateExpression = reverseReactionRateAndExpression.getString();	
								//explitictReverseArrhenius=true;
							}
							else{
								ISpecies species = speciesStoredInHashMap.get(speciesName);
								if(species==null){
									if(speciesName.startsWith(".")){
										String message="Warning: double stochiometry: "+line;
										CRNReducerCommandLine.printWarning(out,bwOut,message);
										throw new IOException(message);
									}
									else{
										//species = addNewSpecies(speciesName);
										String message="Warning: unknown species "+ speciesName +" in the auxiliary data: "+line+" of reaction "+lineOfReaction;
										CRNReducerCommandLine.printWarning(out,bwOut,message);
										throw new IOException(message);
									}
								}

								Double prevWeight = weights.get(species);
								if(prevWeight==null){
									prevWeight=0.0;
								}
								String weightString = line.substring(firstSlash+1, secondSlash);
								double weight = Double.valueOf(weightString);

								weights.put(species, weight+prevWeight);
							}
							from=secondSlash+1;
						}
					}
				}
				else{
					stop=true;
				}
			}
			if(!stop){
				line=br.readLine();
				if(line!=null){
					line=line.trim();
				}
			}
		}

		threeBodyAndPressureDependentReactions.add(new ThreeBodyAndPressureDependentReactionInfo(compositeReagentsHM,compositeProductsHM,reactionRate,rateExpression,reverseReactionRate,reverseRateExpression,weights,reversible));


//		for (ISpecies catalyst : getCRN().getSpecies()) {
//			Double weightDouble =weights.get(catalyst);
//			if(weightDouble==null){
//				weightDouble=1.0;
//			}
//			BigDecimal weight = BigDecimal.valueOf(weightDouble);
//			if(weight.compareTo(BigDecimal.ZERO)!=0){
//				HashMap<ISpecies, Integer> compositeReagentsHMExpanded = copyAndAddSpecies(compositeReagentsHM,catalyst);
//				HashMap<ISpecies, Integer> compositeProductsHMExpanded = copyAndAddSpecies(compositeProductsHM,catalyst);
//
//				IComposite compositeReagents = getCRN().addReagentsIfNew(new Composite(compositeReagentsHMExpanded));
//				IComposite compositeProducts = getCRN().addReagentsIfNew(new Composite(compositeProductsHMExpanded));
//				int arity = compositeReagents.getTotalMultiplicity();
//
//				ICRNReaction reaction = new CRNReaction(reactionRate.multiply(weight), compositeReagents, compositeProducts, rateExpression+"*"+weight.toPlainString());
//				getCRN().addReaction(reaction);
//				addToIncomingReactionsOfProducts(arity,compositeProducts, reaction);
//				addToReactionsWithNonZeroStoichiometry(arity, reaction.computeProductsMinusReagents(),reaction);
//				if(arity==2 && reaction.getReagents().getFirstReagent().equals(reaction.getReagents().getSecondReagent())){
//					CRNReducerCommandLine.ModelsWithHomeoReactions.add(getFileName());
//				}
//
//				if(reversible){
//					arity = compositeProducts.getTotalMultiplicity();
//
//					reaction = new CRNReaction(reverseReactionRate.multiply(weight), compositeProducts, compositeReagents, reverseRateExpression+"*"+weight.toPlainString());
//					getCRN().addReaction(reaction);
//					addToIncomingReactionsOfProducts(arity,compositeReagents, reaction);
//					addToReactionsWithNonZeroStoichiometry(arity, reaction.computeProductsMinusReagents(),reaction);
//					if(arity==2 && reaction.getReagents().getFirstReagent().equals(reaction.getReagents().getSecondReagent())){
//						CRNReducerCommandLine.ModelsWithHomeoReactions.add(getFileName());
//					}
//				}
//			}
//		}

		//I have to put back the line read in excess....
		return line;
	}*/


	/**
	 * This method checks if the reaction is a falloff one (by searching for "LOW" in the extra info). 
	 * If this is a falloff reaction, then it is treated it in the high-pressure limit: it is treated as a normal third body reaction (i.e., (+M) is considered as +M), using the arrhenius rate of the high-pressure limit.
	 * @param compositeReagentsHM
	 * @param compositeProductsHM
	 * @param arrhenius
	 * @param lineOfReaction
	 * @param reversible
	 * @param br
	 * @param threeBodyAndPressureDependentReactions
	 * @param threeBody if true, the method expects to handle a third body reaction. Otherwise a pressureDependent one is expected
	 * @param createdKineticConstants 
	 * @return a representation of the reaction to be later expanded in a reaction per species of the crn (the catalyst)
	 * @throws IOException
	 * @throws UnsupportedFormatException 
	 */
	private String handleSupportedThirdBodyORPressureDependentReaction(
			HashMap<ISpecies, Integer> compositeReagentsHM,
			HashMap<ISpecies, Integer> compositeProductsHM, String[] arrhenius, String lineOfReaction, boolean reversible, BufferedReader br,ArrayList<ThreeBodyAndPressureDependentReactionInfo> threeBodyAndPressureDependentReactions, boolean threeBody, HashMap<Double, String> createdKineticConstants) throws IOException, UnsupportedFormatException {


		//StringAndBigDecimal reactionRateAndExpression = computeArrheniusRateLOG(arrhenius);
		StringAndBigDecimal reactionRateAndExpression = computeArrheniusRate(arrhenius);
		//StringAndBigDecimal reactionRateAndExpression = computeArrheniusRateDirect(arrhenius);
		BigDecimal reactionRate = reactionRateAndExpression.getBigDecimal();
		String rateExpression = reactionRateAndExpression.getString();

		//Even if the reaction will have to be expanded, the arrhenius rate will not change (apart from the weights): addind a catalyst does not change the reverse arrhenius rate.
		StringAndBigDecimal reverseReactionRateAndExpression = null;
		BigDecimal reverseReactionRate = null;
		String reverseRateExpression = null;


		boolean falloff=false;
		boolean chemicalActivated=false;

		//I first read the weights of all species (which tell us how each species acts as third body).
		String line = br.readLine(); 
		if(line!=null){
			line=line.trim();
		}
		boolean stop=false;
		LinkedHashMap<ISpecies, Double> weights = new LinkedHashMap<ISpecies, Double>();

		while (line != null && !stop) {
			if(!(line.equals("")||line.startsWith("!"))){
				line=removeCommentAtTheEnd(line,'!');
				if(line.contains("/")){
					/*
					 H2/2.5/ H2O/12/
   					 AR/0.0/  HE/0.0/
   					 CO/1.9/ CO2/3.8/ 
					 */
					int from =0; 
					while(from<=line.length()){
						//TODO: handle -1 cases
						int firstSlash = line.indexOf("/",from);

						if(firstSlash==-1){
							from = line.length()+1;
						}
						else{
							int secondSlash = line.indexOf("/", firstSlash+1);
							if(secondSlash==-1){
								String message = "I could not parse the additional information (because there is no closing \"/\") of line "+ line +" of the reaction: "+lineOfReaction;
								//CRNReducerCommandLine.printWarning(out,bwOut,message);
								throw new UnsupportedFormatException(message);
							}
							String speciesName = line.substring(from, firstSlash).trim();

							if(speciesName.startsWith("rev")||speciesName.startsWith("REV")||speciesName.startsWith("Rev")){
								String reverseArrheniusString = line.substring(firstSlash+1, secondSlash).trim();
								String[] reverseArrhenius = reverseArrheniusString.split("\\s+");
								if(reverseArrhenius.length!=3){
									throw new UnsupportedFormatException("I could not parse the arrhenius coefficients "+ line +" of the reverse reaction: "+lineOfReaction);
								}
								if(reverseArrhenius[2].endsWith(".")){
									reverseArrhenius[2]=reverseArrhenius[2].substring(0, reverseArrhenius[2].length()-1);
								}
								//reverseReactionRateAndExpression = computeArrheniusRateLOG(reverseArrhenius);
								reverseReactionRateAndExpression = computeArrheniusRate(reverseArrhenius);
								//reverseReactionRateAndExpression = computeArrheniusRateDirect(reverseArrhenius);
								reverseReactionRate = reverseReactionRateAndExpression.getBigDecimal();
								reverseRateExpression = reverseReactionRateAndExpression.getString();	
								//explitictReverseArrhenius=true;
							}
							else if(speciesName.startsWith("LOW")||speciesName.startsWith("low")||speciesName.startsWith("Low")||speciesName.startsWith("LoW")){
								falloff = true;
							}
							else if(speciesName.startsWith("HIGH")||speciesName.startsWith("high")||speciesName.startsWith("High")){
								chemicalActivated=true;
							}
							else if((!threeBody) && 
									(  speciesName.startsWith("TROE")||speciesName.startsWith("troe")||speciesName.startsWith("Troe") 
									|| speciesName.startsWith("SRI")||speciesName.startsWith("sri")||speciesName.startsWith("Sri")
									|| speciesName.startsWith("FC")||speciesName.startsWith("fc")||speciesName.startsWith("Fc")
									|| speciesName.startsWith("PLOG")||speciesName.startsWith("plog")||speciesName.startsWith("Plog")
									
											)){
								//ok, I can skip this
							}
							else{
								//This is not a keyword, but the name of a species
								ISpecies species = speciesStoredInHashMap.get(speciesName);
								if(species==null){
									if(speciesName.startsWith(".")){
										String message="Double stochiometry: "+line;
										//CRNReducerCommandLine.printWarning(out,bwOut,message);
										throw new UnsupportedFormatException(message);
									}
									else{
										//species = addNewSpecies(speciesName);
										String message="Unknown species "+ speciesName +" in the auxiliary data: "+line+" of reaction "+lineOfReaction;
										//CRNReducerCommandLine.printWarning(out,bwOut,message);
										//unknownSpecies.add(speciesName);
										throw new UnsupportedFormatException(message);
									}
								}

								Double prevWeight = weights.get(species);
								if(prevWeight==null){
									prevWeight=0.0;
								}
								String weightString = line.substring(firstSlash+1, secondSlash);
								double weight = Double.valueOf(weightString);

								weights.put(species, weight+prevWeight);
							}
							from=secondSlash+1;
						}
					}
				}
				else{
					String trimmedLine=line.trim();
					//Ignore dup lines
					if(!(trimmedLine.startsWith("DUP")||trimmedLine.startsWith("dup")||trimmedLine.startsWith("Dup"))){
						stop=true;
					}
				}
			}
			if(!stop){
				line=br.readLine();
				if(line!=null){
					line=line.trim();
				}
			}
		}

		if(reverseReactionRate==null ||reverseRateExpression==null){
			//Even if the reaction will have to be expanded, the arrhenius rate will not change (apart from the weights): addind a catalyst does not change the reverse arrhenius rate.
			//reverseReactionRateAndExpression = computeReverseArrheniusRateLOG(reactionRate,rateExpression,lineOfReaction,new Composite(compositeReagentsHM),new Composite(compositeProductsHM));
			reverseReactionRateAndExpression = computeReverseArrheniusRate(reactionRate,rateExpression,lineOfReaction,new Composite(compositeReagentsHM),new Composite(compositeProductsHM));
			//reverseReactionRateAndExpression = computeReverseArrheniusRateDirect(reactionRate,rateExpression,lineOfReaction,new Composite(compositeReagentsHM),new Composite(compositeProductsHM));
			reverseReactionRate = reverseReactionRateAndExpression.getBigDecimal();
			reverseRateExpression = reverseReactionRateAndExpression.getString();
		}

		if(threeBody){
			if(falloff || chemicalActivated){
				throw new UnsupportedFormatException("Unexpexted LOW or HIGH keyword found in the non-pressure-dependent third body reaction : "+lineOfReaction);
			}
			else{
				rateExpression = convertToParameter(reactionRate, rateExpression, createdKineticConstants);
				if(reversible){
					reverseRateExpression = convertToParameter(reverseReactionRate, reverseRateExpression, createdKineticConstants);
				}
				threeBodyAndPressureDependentReactions.add(new ThreeBodyAndPressureDependentReactionInfo(compositeReagentsHM,compositeProductsHM,reactionRate,rateExpression,reverseReactionRate,reverseRateExpression,weights,reversible));
				//I have to put back the line read in excess....
				return line;
			}
		}
		else{
			//Pressure dependent reaction
			if(falloff && chemicalActivated){
				throw new UnsupportedFormatException("Both chemically activated and falloff reaction: "+lineOfReaction);
			}
			else if(!(falloff || chemicalActivated)){
				throw new UnsupportedFormatException("Neither chemically activated nor falloff reaction: "+lineOfReaction);
			}
			else if(chemicalActivated){
				throw new UnsupportedFormatException("Chemically activated reaction: "+lineOfReaction);
			}
			else{
				//fallOfReaction
				//handleSupportedSimpleReaction(br,compositeReagentsHM, compositeProductsHM, arrhenius, line, reversible,reverseReactionRate,reverseRateExpression,false);
				if(reactionRate.compareTo(BigDecimal.ZERO)!=0){
					rateExpression = convertToParameter(reactionRate, rateExpression, createdKineticConstants);
					if(reversible){
						reverseRateExpression = convertToParameter(reverseReactionRate, reverseRateExpression, createdKineticConstants);
					}
					threeBodyAndPressureDependentReactions.add(new ThreeBodyAndPressureDependentReactionInfo(compositeReagentsHM,compositeProductsHM,reactionRate,rateExpression,reverseReactionRate,reverseRateExpression,weights,reversible));
				}
				//I have to put back the line read in excess....
				return line;
			}
		}
	}


	/**
	 * This method checks if the reaction is a falloff one (by searching for "LOW" in the extra info). If this is a falloff reaction, then it treats it in the high-pressure limit: it is treated as a normal reaction, and (+M) is thrown away
	 * @param compositeReagentsHM
	 * @param compositeProductsHM
	 * @param arrhenius
	 * @param lineOfReaction
	 * @param reversible
	 * @param br
	 * @param threeBodyAndPressureDependentReactions 
	 * @return
	 * @throws IOException
	 * @throws UnsupportedFormatException 
	 */
	/*private String handleSupportedPressureDependentReaction(
			HashMap<ISpecies, Integer> compositeReagentsHM,
			HashMap<ISpecies, Integer> compositeProductsHM, String[] arrhenius, String lineOfReaction, boolean reversible, BufferedReader br, ArrayList<ThreeBodyAndPressureDependentReactionInfo> threeBodyAndPressureDependentReactions) throws IOException {

		//I first read the weights of all species (which tell us how each species acts as third body).
		String line = br.readLine(); 
		if(line!=null){
			line=line.trim();
		}
		boolean stop=false;
		boolean fallOff =false;
		boolean chemicalActivated=false;
		BigDecimal reverseReactionRate=null;
		String reverseRateExpression=null;
		while (line != null && !stop) {
			if(!(line.equals("")||line.startsWith("!"))){
				if(line.contains("/")){
					line=removeCommentAtTheEnd(line,'!');
					String lineNoSpace=line.replace(" ", "");
					lineNoSpace=lineNoSpace.replace("\t", "");
					if(lineNoSpace.contains("LOW/")||lineNoSpace.contains("low/")||lineNoSpace.contains("Low/")||lineNoSpace.contains("LoW/")){
						//stop=true;
						fallOff=true;
					}
					else if(lineNoSpace.contains("HIGH/")||lineNoSpace.contains("high/")||lineNoSpace.contains("High/")){
						//stop=true;
						chemicalActivated=true;
					} 

					StringAndBigDecimal reverseReactionRateAndExpression = checkIfContainsExplicitReversedArrhenius(line,lineOfReaction);
					if(reverseReactionRateAndExpression!=null){
						reverseReactionRate = reverseReactionRateAndExpression.getBigDecimal();
						reverseRateExpression = reverseReactionRateAndExpression.getString();
					}
				}
				else{
					//no more extra parameters
					stop=true;
				}
			}
			if(!stop){
				line=br.readLine();
				if(line!=null){
					line=line.trim();
				}
			}
		}

		if(fallOff && chemicalActivated){
			throw new IOException("Both chemically activated and falloff reaction: "+lineOfReaction);
		}
		else if(!(fallOff || chemicalActivated)){
			throw new IOException("Neither chemically activated nor falloff reaction: "+lineOfReaction);
		}
		else if(chemicalActivated){
			throw new IOException("Chemically activated reaction: "+lineOfReaction);
		}
		else{
			//fallOfReaction
			handleSupportedSimpleReaction(br,compositeReagentsHM, compositeProductsHM, arrhenius, line, reversible,reverseReactionRate,reverseRateExpression,false);
			//I have to put back the line read in excess....
			return line;
		}
	}*/


	private StringAndBigDecimal checkIfContainsExplicitReversedArrhenius(
			String line,String lineOfReaction) throws IOException, UnsupportedFormatException {

		StringAndBigDecimal reverseReactionRateAndExpression=null;

		//line=line.replace(" ", "");
		//line=line.replace("\t", "");
		int posOfRev=line.indexOf("rev");
		if(posOfRev==-1){
			posOfRev=line.indexOf("Rev");
			if(posOfRev==-1){
				posOfRev=line.indexOf("REV");
			}
		}
		if(posOfRev!=-1){
			//CRNReducerCommandLine.println(out,bwOut"Line for reverse:" +line);
			int firstSlash = line.indexOf("/",posOfRev+3); //posOfRev+4;
			int secondSlash= line.indexOf("/",firstSlash+1);
			String reverseArrheniusString=line.substring(firstSlash+1, secondSlash).trim();
			String[] reverseArrhenius = reverseArrheniusString.split("\\s+");
			if(reverseArrhenius.length!=3){
				throw new UnsupportedFormatException("I could not parse the arrhenius coefficients "+ line +" of the reverse reaction: "+lineOfReaction);
			}
			if(reverseArrhenius[2].endsWith(".")){
				reverseArrhenius[2]=reverseArrhenius[2].substring(0, reverseArrhenius[2].length()-1);
			}

			for(int i=0;i<reverseArrhenius.length;i++){
				reverseArrhenius[i]=reverseArrhenius[i].replace(".e+", "e+");
				reverseArrhenius[i]=reverseArrhenius[i].replace(".E+", "E+");
				reverseArrhenius[i]=reverseArrhenius[i].replace(".e-", "e-");
				reverseArrhenius[i]=reverseArrhenius[i].replace(".E-", "E-");
			}

			//reverseReactionRateAndExpression = computeArrheniusRateLOG(reverseArrhenius);
			reverseReactionRateAndExpression = computeArrheniusRate(reverseArrhenius);
			//reverseReactionRateAndExpression = computeArrheniusRateDirect(reverseArrhenius);
		}

		return reverseReactionRateAndExpression;
	}

	/*private StringAndBigDecimal computeReverseArrheniusRate(
			BigDecimal reactionRate, String rateExpression,
			String lineOfReaction) {
		//TODO 
		String scaleFactor="2.0";
		rateExpression = "("+rateExpression+")/("+scaleFactor+")";
		Expression evaluator = new Expression(rateExpression);
		evaluator.setPrecision(ChemKinImporter.PRECISION);
		evaluator.setRoundingMode(RoundingMode.HALF_DOWN);

		reactionRate = evaluator.eval();

		return new StringAndBigDecimal(rateExpression, reactionRate);
	}*/
	

	

	private StringAndBigDecimal computeReverseArrheniusRate(BigDecimal reactionRate, String rateExpression, String lineOfReaction,
			IComposite reagents, IComposite products) {

		if(reactionRate.compareTo(BigDecimal.ZERO)==0){
			return new StringAndBigDecimal("0.0", BigDecimal.ZERO);
		}

		/*CRNReducerCommandLine.println(out,bwOutlineOfReaction);
		if(reagents.getFirstReagent().getName().equals("ch3oh")){
			CRNReducerCommandLine.println(out,bwOut"ciao");
		}*/
		/*if(reagents.getFirstReagent().getName().equals("ch2co")){
			CRNReducerCommandLine.println(out,bwOut"ciao");
		}*/

		IComposite reagentsMinusProducts = Composite.createFirstMinusSecond(reagents, products);

		boolean invertNetStoic=true;
		//int[] multiplicities = reagentsMinusProducts.getMultiplicities();
		if(invertNetStoic){
			for(int i=0;i<reagentsMinusProducts.getNumberOfDifferentSpecies();i++){
				reagentsMinusProducts.setMultiplicities(i,0-reagentsMinusProducts.getMultiplicities(i));
				//multiplicities[i]=0-multiplicities[i];
			}
		}
		int cumulativeStoichiometryCoefficient = reagentsMinusProducts.getTotalMultiplicity();
		int minusCumulativeStoichiometryCoefficient = 0-cumulativeStoichiometryCoefficient;




		//(Patm/(R*T))^sum_{1<k<K} vki
		//String reverseArrheniusExpr = "("+Patm.toPlainString() + " / ( "+R.toPlainString()+"*"+T.toPlainString()+"))^("+cumulativeStoichiometryCoefficient+")";
		//From the found code: RT/P = (8.314 J/molK* T K)/(10^5 N/m2)*10^6 (cm3/m3)		
		//Math.pow( ((8.314 * Temperature / Math.pow(10, 5)) * Math.pow(10, 6)), (numR - numP))
		// "(8.314 * T * 10) ^ minusCumulativeStoichiometryCoefficient";
		//I think that there is a typo in the other implementatin. "v_{ki}" should be numR-numP, which then becomes numP-numR. The other implementation does the opposite.
		String PatmoverRTToTheNetStoich = "("+RinJoverMol.multiply(T).multiply(BigDecimal.TEN).toPlainString() +"^"+minusCumulativeStoichiometryCoefficient+") ";

		//(Delta S_k^0 / R) and (Delta H_k^0 / R) 		
		StringBuilder deltaHSiOoverRT_SB = new StringBuilder("0");
		StringBuilder deltaSiOoverR_SB = new StringBuilder("0");
		for(int s=0;s<reagentsMinusProducts.getNumberOfDifferentSpecies();s++){
			ISpecies species = reagentsMinusProducts.getAllSpecies(s);
			ThermoDynamicInfoChemKin tdInfo = this.thermoDynamicInfo.get(species);
			BigDecimal[] intervalCoefficients = tdInfo.getAppropriateIntervalCoefficients(T);

			deltaSiOoverR_SB.append(" + ");
			deltaSiOoverR_SB.append(reagentsMinusProducts.getMultiplicities(s));
			deltaSiOoverR_SB.append("*(");//S_k^0 / R, with k=s
			deltaSiOoverR_SB.append(intervalCoefficients[0].toPlainString() +" * log("+ T.toPlainString()+") + ");
			deltaSiOoverR_SB.append(intervalCoefficients[1].toPlainString() +" * "+ T.toPlainString()+" + ");
			deltaSiOoverR_SB.append("("+intervalCoefficients[2].toPlainString() +"/ 2)" + " * ("+ T.toPlainString()+"^2) + ");
			deltaSiOoverR_SB.append("("+intervalCoefficients[3].toPlainString() +"/ 3)" + " * ("+ T.toPlainString()+"^3) + ");
			deltaSiOoverR_SB.append("("+intervalCoefficients[4].toPlainString() +"/ 4)" + " * ("+ T.toPlainString()+"^4) + ");
			deltaSiOoverR_SB.append(intervalCoefficients[6].toPlainString());
			deltaSiOoverR_SB.append(")");

			deltaHSiOoverRT_SB.append(" + ");
			deltaHSiOoverRT_SB.append(reagentsMinusProducts.getMultiplicities(s));
			deltaHSiOoverRT_SB.append("*(");//H_k^0 / R, with k=s
			deltaHSiOoverRT_SB.append(intervalCoefficients[0].toPlainString() + " + ");
			deltaHSiOoverRT_SB.append("("+intervalCoefficients[1].toPlainString()+"/ 2)" +" * "+ T.toPlainString()+" + ");
			deltaHSiOoverRT_SB.append("("+intervalCoefficients[2].toPlainString()+"/ 3)" + " * ("+ T.toPlainString()+"^2) + ");
			deltaHSiOoverRT_SB.append("("+intervalCoefficients[3].toPlainString()+"/ 4)" + " * ("+ T.toPlainString()+"^3) + ");
			deltaHSiOoverRT_SB.append("("+intervalCoefficients[4].toPlainString()+"/ 5)" + " * ("+ T.toPlainString()+"^4) + ");
			deltaHSiOoverRT_SB.append(intervalCoefficients[5].toPlainString() +" / "+ T.toPlainString());
			deltaHSiOoverRT_SB.append(")");
		}

		//CRNReducerCommandLine.println(out,bwOutlineOfReaction);
		Expression evaluatorDeltaS = new Expression(deltaSiOoverR_SB.toString());
		evaluatorDeltaS.setPrecision(ChemKinImporter.PRECISION);
		evaluatorDeltaS.setRoundingMode(CRNBisimulationsNAry.RM);
		//BigDecimal deltaS=evaluatorDeltaS.eval();
		Expression evaluatorDeltaH = new Expression(deltaHSiOoverRT_SB.toString());
		evaluatorDeltaH.setPrecision(ChemKinImporter.PRECISION);
		evaluatorDeltaH.setRoundingMode(CRNBisimulationsNAry.RM);
		//BigDecimal deltaH=evaluatorDeltaH.eval();

		Expression evaluator = new Expression(deltaSiOoverR_SB.toString() + " - "+ deltaHSiOoverRT_SB.toString());
		evaluator.setPrecision(ChemKinImporter.PRECISION);
		evaluator.setRoundingMode(CRNBisimulationsNAry.RM);

		BigDecimal argument = evaluator.eval();
		//BigDecimal Kpi = BigDecimal.valueOf(Math.expm1(argument.doubleValue())+1);
		BigDecimal Kpi = BigDecimal.valueOf(Math.exp(argument.doubleValue()));
		if(Kpi.compareTo(BigDecimal.ZERO)==0){
			CRNReducerCommandLine.println(out,bwOut,"Kpi is zero: "+lineOfReaction);
		}

		//Kpi * (Patm/(R*T))^sum_{1<k<K} vki
		String Kci = Kpi.toPlainString() + " * ("+ PatmoverRTToTheNetStoich +") ";

		String reverseArrheniusExpr = reactionRate.toPlainString()+ " / (" + Kci + ")";

		evaluator = new Expression(reverseArrheniusExpr);
		evaluator.setPrecision(ChemKinImporter.PRECISION);
		evaluator.setRoundingMode(CRNBisimulationsNAry.RM);

		//CRNReducerCommandLine.println(out,bwOutevaluator.getExpression());
		BigDecimal reverserArrheniusRate =  evaluator.eval();
		//CRNReducerCommandLine.println(out,bwOut"Log of reverse rate: "+Math.log(reverserArrheniusRate.doubleValue()));

		return new StringAndBigDecimal(reverseArrheniusExpr, reverserArrheniusRate);
	}
	

	private StringAndBigDecimal computeArrheniusRate(String[] arrhenius) throws IOException {

		//CRNReducerCommandLine.println(out,bwOutArrays.toString(arrhenius));

		BigDecimal A = BigDecimal.valueOf(evaluate(arrhenius[0]));
		BigDecimal beta = BigDecimal.valueOf(evaluate(arrhenius[1]));
		BigDecimal E = BigDecimal.valueOf(evaluate(arrhenius[2]));
		//TODO? 
		//String rateExpression = A.toPlainString()+"*"+TName+"^("+beta.toPlainString()+")+"+E.toPlainString();
		//CRNReducerCommandLine.println(out,bwOutevaluate(rateExpression));

		String argumentOfExponentialString = "(0.0 -" + E.toPlainString() + ")/( "+R.toPlainString()+"*"+T.toPlainString()+")";
		Expression evaluator = new Expression(argumentOfExponentialString);
		evaluator.setPrecision(ChemKinImporter.PRECISION);
		evaluator.setRoundingMode(CRNBisimulationsNAry.RM);
		//TODO: can we do the exponential using the evaluator?
		//double exp = Math.expm1(evaluator.eval().doubleValue())+1;
		double exp = Math.exp(evaluator.eval().doubleValue());

		String expr = A.toPlainString() + "*(" + T.toPlainString()+"^("+beta.toPlainString()+"))*"+ BigDecimal.valueOf(exp).toPlainString();
		evaluator = new Expression(expr);
		evaluator.setPrecision(ChemKinImporter.PRECISION);
		evaluator.setRoundingMode(CRNBisimulationsNAry.RM);
		BigDecimal reactionRate = evaluator.eval();
		//CRNReducerCommandLine.println(out,bwOut"Log of forward rate: "+Math.log(reactionRate.doubleValue()));
		String rateExpression = reactionRate.toPlainString();

		/*double exp = (Math.expm1((0.0 - E.doubleValue())/ (R.doubleValue()*T.doubleValue()))+1);
		double reactionRateDouble = A.doubleValue() * Math.pow(T.doubleValue(), beta.doubleValue()) * exp;
		BigDecimal reactionRate = BigDecimal.valueOf(reactionRateDouble);
		String rateExpression = reactionRate.toPlainString();*/

		return new StringAndBigDecimal(rateExpression, reactionRate);
	}
	

	private HashMap<ISpecies, Integer> copyAndAddSpecies(
			HashMap<ISpecies, Integer> compositeHM, ISpecies catalyst) {

		HashMap<ISpecies, Integer> expandedCopy = new HashMap<ISpecies, Integer>(compositeHM);

		Integer prev = expandedCopy.get(catalyst);
		if(prev==null){
			prev=0;
		}

		expandedCopy.put(catalyst, prev+1);

		return expandedCopy;
	}

	private CompositeHashMapAndInfoOnReaction createCompositeHashMapAndRestoreIonizedName(String[] reagentsOrProducts, String ionized, String line) throws IOException, UnsupportedFormatException {
		return createCompositeHashMapAndRestoreIonizedName(reagentsOrProducts, ionized, line,ChemKinReactionNote.NONE);
	}

	private CompositeHashMapAndInfoOnReaction createCompositeHashMapAndRestoreIonizedName(String[] reagentsOrProducts, String ionized, String line, ChemKinReactionNote previousNote) throws IOException, UnsupportedFormatException {

		boolean thirdBodyReaction=false;
		boolean pressureDependentReaction=false; //If true, it is either fall-off, or chemically activated
		HashMap<ISpecies, Integer> compositeHM = new HashMap<ISpecies, Integer>();
		for(int i=0;i<reagentsOrProducts.length;i++){
			reagentsOrProducts[i]=reagentsOrProducts[i].replace(ionized, "+");
			int multOfSpecies = multiplicity(reagentsOrProducts[i]);
			if(reagentsOrProducts[i].substring(1,reagentsOrProducts[i].length()).startsWith(".")){
				String message="Double stochiometry: "+line;
				//CRNReducerCommandLine.printWarning(out,bwOut,message);
				throw new UnsupportedFormatException(message);
			}
			if(multOfSpecies!=1){
				reagentsOrProducts[i]=reagentsOrProducts[i].substring(1, reagentsOrProducts[i].length());
			}


			ISpecies species = speciesStoredInHashMap.get(reagentsOrProducts[i]);
			if(species==null){
				if(reagentsOrProducts[i].equalsIgnoreCase("M") ){
					/*String message="Warning: Three-body reaction: "+line;
					CRNReducerCommandLine.printWarning(out,bwOut,message);*/
					thirdBodyReaction=true;
					//unsupportedReaction=true;
					//throw new IOException(message);
					//return null;
				}
				else if(reagentsOrProducts[i].endsWith("(") || reagentsOrProducts[i].endsWith(")") ){
					if(		(!getInfoImporting().getLoadingCRNFailed()) && 
							(!(previousNote.equals(ChemKinReactionNote.UNSUPPORTED)||previousNote.equals(ChemKinReactionNote.PRESSUREDEPENDENT)))){
						/*String message="Warning: Pressure-dependent reaction: "+line;
						CRNReducerCommandLine.printWarning(out,bwOut,message);*/
					}
					if(reagentsOrProducts[i].endsWith("(")){
						reagentsOrProducts[i]=reagentsOrProducts[i].substring(0, reagentsOrProducts[i].length()-1);
						reagentsOrProducts[i+1]="(+"+reagentsOrProducts[i+1];
						species = speciesStoredInHashMap.get(reagentsOrProducts[i]);
						pressureDependentReaction=true;
					}

					//throw new IOException(message);
					//return new CompositeHashMapAndInfoOnReaction(null, ChemKinReactionNote.UNSUPPORTED);
				} 
				else{
					if(reagentsOrProducts[i].startsWith(".")){
						String message="Double stochiometry: "+line;
						//CRNReducerCommandLine.printWarning(out,bwOut,message);
						throw new UnsupportedFormatException(message);
					}
					else{
						if(reagentsOrProducts[i].equalsIgnoreCase("hv")){
							String message="Reaction with photon radiations are not supported ("+ reagentsOrProducts[i] +") in reaction: "+line;
							//CRNReducerCommandLine.printWarning(out,bwOut,message);
							throw new UnsupportedFormatException(message);
						}
						else{
							//species = addNewSpecies(reagentsOrProducts[i]);
							String message="Unknown species ["+ reagentsOrProducts[i] +"] in reaction: "+line;
							CRNReducerCommandLine.printWarning(out,bwOut,true,msgDialogShower,message,DialogType.Warning);
							//unknownSpecies.add(reagentsOrProducts[i]);
							//throw new IOException(message);
						}
					}
				}
			}

			if(species!=null){
				Integer prevMult = compositeHM.get(species);
				if(prevMult==null){
					prevMult=0;
				}
				compositeHM.put(species, prevMult+multOfSpecies);
			}
		}

		ChemKinReactionNote note = ChemKinReactionNote.NONE;
		if(thirdBodyReaction){
			note=ChemKinReactionNote.THIRDBODY;
		}
		if(pressureDependentReaction){
			note=ChemKinReactionNote.PRESSUREDEPENDENT;
		}

		return new CompositeHashMapAndInfoOnReaction(compositeHM, note);
	}

	private int multiplicity(String species) {
		String multStr = species.substring(0, 1);
		int mult =1;
		try{
			mult = Integer.valueOf(multStr);
		} catch(NumberFormatException e){
			return 1;
		}
		return mult;
	}

	private String replaceIonizedPLusWithIonizedString(String reagentsOrProducts, String ionized) {
		reagentsOrProducts=reagentsOrProducts.replace(" ", "");

		//reagentsOrProducts.replace("++", ionized+"+");
		reagentsOrProducts.replace("(+)", ionized);
		if(reagentsOrProducts.endsWith("+")){
			reagentsOrProducts=reagentsOrProducts.substring(0, reagentsOrProducts.length()-1) + ionized;
		}

		return reagentsOrProducts;
	}
}
