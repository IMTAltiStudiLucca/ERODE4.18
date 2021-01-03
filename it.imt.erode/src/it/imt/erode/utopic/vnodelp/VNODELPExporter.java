package it.imt.erode.utopic.vnodelp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.DialogType;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.importing.AbstractImporter;
import it.imt.erode.importing.GUICRNImporter;
import it.imt.erode.importing.MatlabODEsImporter;
import it.imt.erode.importing.UnsupportedFormatException;
import it.imt.erode.utopic.MatlabODEPontryaginExporter;

public class VNODELPExporter {
	
	//vnodePath="/home/utopia/Desktop/VNODE/"
	public static final String FILEWITHVNODEROOT="pathOfMVNODEROOT.txt";// /Applications/Eclipse-SDK-4.7.1a.app/Contents/MacOS/pathOfMVNODEROOT.txt
	public static final String VNODE_LOCATEMESSAGE = "Locate the main folder of VNODE containing \n    - installation\n    - src";
	
	public boolean printConverginPontryaginToVNodeLPFIle(ICRN crn, String name, boolean verbose,
			MessageConsoleStream out, BufferedWriter bwOut, IMessageDialogShower msgDialogShower, double tEnd,
			HashMap<String, Integer> parameterToPerturbToItsPosition, String[] paramsToPerturb, double[] lows, double[] highs,
			HashMap<String, Double> speciesToCoefficient
			)  throws UnsupportedFormatException{
		
	String varName = "y"; 
	String derivName = "yp";
	
		String fileName = name;
		if(crn.isSymbolic()){
			CRNReducerCommandLine.printWarning(out,bwOut, true, msgDialogShower, "The Pontryagin script can be created only for mass-action CRNs without symbolic parameters.", DialogType.Error);
			return false;
		}
		
		
		HashMap<String,ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpecies().size());
		for (ISpecies species : crn.getSpecies()) {
			speciesNameToSpecies.put(species.getName(), species);
		}
		
		
		
		ISpecies[] speciesIdToSpecies=new ISpecies[crn.getSpecies().size()];
		int s=0;
		for (ISpecies species : crn.getSpecies()) {
			speciesIdToSpecies[s]=species;
			s++;
		}
		
		fileName=AbstractImporter.overwriteExtensionIfEnabled(fileName,".cc",true);
		if(verbose){
			CRNReducerCommandLine.print(out,bwOut,"Writing the Pontryagin script in file "+fileName);
		}
		
		AbstractImporter.createParentDirectories(fileName);
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printConverginPontryaginToVNodeLPFIle, exception raised while creating the filewriter for file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return false;
		}
		
		try {
			
			String modelName = " ";
			if(crn.getName()!=null&&crn.getName()!=""){
				modelName = " "+GUICRNImporter.getModelName(crn.getName())+" ";
			}
			
			String message="";
			if(paramsToPerturb.length>1){
				message="// Reachability analysis of the system"+modelName+"with "+paramsToPerturb.length+" uncertain parameters //";
			} 
			else{
				message="// Reachability analysis of the system"+modelName+"with 1 uncertain parameter //";
			}
			StringBuilder sb = new StringBuilder(message.length());
			for(int i=0;i<message.length();i++){
				sb.append('/');
			}
			String caption=sb.toString();
			bw.write(caption+"\n");
			bw.write(message+"\n");
			//bw.write("% Parameters that are common for CORA and Pontryagin's algo are set here.\n");
			bw.write(caption+"\n");
			
			bw.write("\n");
			
			bw.write("// Automatically generated from "+crn.getName()+".\n");
			bw.write("//// Number of species: "+crn.getSpecies().size()+".\n");
			bw.write("// Number of reactions: "+crn.getReactions().size()+".\n");
			bw.write("// Coefficients: ");
			MatlabODEPontryaginExporter.writeCoefficients(crn, speciesToCoefficient, bw);
			bw.write(".\n");
			bw.write("// Perturbed parameters: ");
			for(int i=0;i<paramsToPerturb.length;i++){
				bw.write(paramsToPerturb[i]+" in ["+lows[i]+","+highs[i]+"]");
				if(i<paramsToPerturb.length-1){
					bw.write(", ");
				}
			}
			bw.write("\n");
			bw.write("\n// Correspondence with original names:\n");
			int incr=1;
			for (ISpecies species : crn.getSpecies()) {
				bw.write("//     "+varName+"(" +(species.getID()+incr)+") = " + ((species.getOriginalName()==null)?species.getName():species.getOriginalName())+"\n");
			}
			bw.write("\n\n");
			
			String prefixSpace="\t";
			int nVars = crn.getSpecies().size();
			//Parameters to perturb
			int nControls=paramsToPerturb.length;
			
			//String ponitryaginFunctionName = fileName.replace(".m", "");
			String ponitryaginFunctionName = AbstractImporter.overwriteExtensionIfEnabled(fileName,"",true);
			int indexOfLastSep = ponitryaginFunctionName.lastIndexOf(File.separator);
			if(indexOfLastSep>=0){
				ponitryaginFunctionName=ponitryaginFunctionName.substring(indexOfLastSep+1);
			}
			

			//bw.write("/*74:*/\n");
			//bw.write("#line 54 \"./vanderpol.w\"\n");
			bw.write("\n");
			bw.write("#include <fstream>\n"); 
			bw.write("#include <iomanip>\n"); 
			bw.write("#include <sstream>\n"); 
			bw.write("#include <string>\n"); 
			bw.write("#include <string.h>\n");
			bw.write("#include <stdlib.h>\n");
			bw.write("\n");
			bw.write("#include \"vnode.h\"\n");
			bw.write("\n");
			bw.write("struct\n");
			bw.write("UncertainParameters{interval val["+nControls+"];};\n");
			bw.write("\n\n");
			
			bw.write("using namespace std;\n");
			bw.write("using namespace vnodelp;\n");
			bw.write("\n");
			bw.write("template<typename var_type> \n");
			bw.write("void drift(int n,var_type*yp,const var_type*y,var_type t,void*param)\n");
			bw.write("{\n");
			bw.write(prefixSpace+"UncertainParameters*p= (UncertainParameters*)param;\n");
			bw.write("\n\n");
			
			writeUncertainParams(crn, bw, prefixSpace, parameterToPerturbToItsPosition);
			
			bw.write("\n");
			
			//MatlabODEPontryaginExporter.writeODEFunction(crn, bw, speciesNameToSpecies,"\t",false,false,false,false,true,null,false,varName,derivName,parameterToPerturbToItsPosition, paramsToPerturb);
			writeODEFunction(crn, bw, speciesNameToSpecies, prefixSpace, varName, derivName);
			bw.write("}\n");
			bw.write("\n\n");
			
			
			bw.write("int main()\n");
			bw.write("{\n");
			bw.write("// size of the system\n");
			bw.write("const int n= "+nVars+";\n");
			
			bw.write("\n\n");
			
			bw.write("/*************************************************************************\n");
			bw.write("*\n");
			bw.write("* Computing maximum of the reachable set\n"); 
			bw.write("*\n");
			bw.write("*************************************************************************/\n");
			bw.write("\n");
			bw.write("\n");
			bw.write("\n");
			bw.write("// number of changes of u_max control\n");
			bw.write("\n");
			//bw.write("// number of changes of u_max control\n");
			bw.write("int m=0;\n");
			bw.write("FILE *fp = fopen(\""+ponitryaginFunctionName+"_max.csv\",\"r\");\n");
		
			MatlabODEPontryaginExporter.printResource("first.txt", bw,getClass());
			bw.write("ifstream file(\""+ponitryaginFunctionName+"_max.csv\");\n");
			MatlabODEPontryaginExporter.printResource("firstToSecond.txt", bw,getClass());
			
			bw.write("t_[m-1]="+tEnd+"; // horizon time value\n\n");
			
			bw.write("// reachability problem parameters\n");
			bw.write("iVector y(n), coef(n); \n");
			for(ISpecies species : crn.getSpecies()){
				bw.write(varName+"["+species.getID()+"]="+crn.getMath().evaluate(species.getInitialConcentrationExpr())+";\n");
			}
			for(ISpecies species : crn.getSpecies()){
				Double coeff = speciesToCoefficient.get(species.getName());
				if(coeff==null){
					coeff=0.0;
				}
				bw.write("coef["+species.getID()+"]="+coeff+";\n");
			}
			bw.write("\n");	
			
			
			MatlabODEPontryaginExporter.printResource("second.txt", bw,getClass());
			
			bw.write("fp = fopen(\""+ponitryaginFunctionName+"_min.csv\",\"r\");\n");
			
			MatlabODEPontryaginExporter.printResource("third.txt", bw,getClass());
			bw.write("ifstream file2(\""+ponitryaginFunctionName+"_min.csv\");\n");
			MatlabODEPontryaginExporter.printResource("thirdToFourth.txt", bw,getClass());
			
			bw.write("t2[m-1]="+tEnd+"; // horizon time value\n\n");

			bw.write("// reachability problem parameters\n");
			for(ISpecies species : crn.getSpecies()){
				bw.write(varName+"["+species.getID()+"]="+crn.getMath().evaluate(species.getInitialConcentrationExpr())+";\n");
			}
			
			MatlabODEPontryaginExporter.printResource("fourth.txt", bw,getClass());
			
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printPontryaginToMatlabFIle, exception raised while writing in the file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return false;
		}
		finally{
			if(verbose){
				CRNReducerCommandLine.println(out,bwOut,"Writing the Pontryagin script in file "+fileName+" completed");
			}
			try {
				bw.flush();
				bw.close();
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in printPontryaginToMatlabFIle, exception raised while closing the bufferedwriter of the file: "+fileName);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
				return false;
			}
		}
		
		return true;
		
	}
	
	
	private static void writeUncertainParams(ICRN crn, BufferedWriter bw, String prefixSpace,
			HashMap<String, Integer> parameterToPerturbToItsPosition) throws IOException{
		for (String param : crn.getParameters()) {
			int space = param.indexOf(' ');
			String paramName = param.substring(0, space).trim();
			Integer pos = parameterToPerturbToItsPosition.get(paramName);
			if(pos!=null&&pos>=0){
				bw.write(prefixSpace+"interval "+paramName +" = p->val["+pos+"];\n");
			}
			else{
				String paramExpr = param.substring(space,param.length()).trim();
				bw.write(prefixSpace+"interval "+paramName +" = "+crn.getMath().evaluate(paramExpr)+";\n");
			}
		}
	}
	
	private void writeODEFunction(ICRN crn, BufferedWriter bw, HashMap<String, ISpecies> speciesNameToSpecies,String prefixSpace, String varName,String derivName) throws IOException, UnsupportedFormatException {
		
		HashMap<ISpecies, StringBuilder> speciesToDrift=null;
		if(crn.isMassAction()){
			speciesToDrift = GUICRNImporter.computeDriftsReplacingSpeciesNames(crn, false, varName,0,true,"",true);
			MatlabODEsImporter.writeDriftsOfMassActionWithSpeciesNameAlreadyReplacedWithY(derivName,speciesToDrift, crn, bw, prefixSpace,true);
		}
		else{
			boolean ignoreI=false;
			speciesToDrift = GUICRNImporter.computeDrifts(crn,false,ignoreI);
			MatlabODEsImporter.writeDriftsOfArbitraryODEReplacingSpeciesNamesWithY(0,varName,derivName,speciesToDrift, crn, bw, speciesNameToSpecies,prefixSpace,true,true);
		}	
	}
	
	public boolean compileVNODELPScript(String cFileName, String vnodePath, MessageConsoleStream out, BufferedWriter bwOut,IMessageDialogShower msgDialogShower) {
		String oFileName = AbstractImporter.overwriteExtensionIfEnabled(cFileName,".o",true);
		String logFileName = AbstractImporter.overwriteExtensionIfEnabled(cFileName,"",true);
		logFileName = logFileName+"COMPILATION.log";
		String execFileName = AbstractImporter.overwriteExtensionIfEnabled(cFileName,"",true);
		//g++ test.cpp -o test.o
		//String[] myArgs = new String[] {"g++", "test.cpp","-o","test.o"};
		//String[] myArgs = new String[] {"g++", cFileName,"-o", oFileName};
		
		/*
		g++ -g -O2 -Wno-deprecated -DNDEBUG -DFILIB_VNODE -DMAXORDER=50 -I /home/utopia/Escritorio/VNODE/installation/include -I /home/utopia/Desktop/UTOPIC/VNODE/src/vnodelp/FADBAD++  -I /home/utopia/Desktop/UTOPIC/VNODE/src/vnodelp/include   -c -o vanderPol_Pontryagin.o vanderPol_Pontryagin.cc

		g++ -L /home/utopia/Escritorio/VNODE/installation/lib -Le -L/home/utopia/Desktop/UTOPIC/VNODE/src/vnodelp/lib		 -o vanderPol_Pontryagin vanderPol_Pontryagin.o -lvnode -lfi -lprim -lieee -llapack -lblas -lstdc++ -lg2c
		 */
		
		//vnodePath="/home/utopia/Desktop/VNODE/"
		String[] myArgs = new String[] {"g++", "-g", "-O2", "-Wno-deprecated", "-DNDEBUG", "-DFILIB_VNODE", "-DMAXORDER=50", /*"-I", vnodePath+"installation"+File.separator+"include",*/ "-I", vnodePath+"src"+File.separator+"vnodelp"+File.separator+"FADBAD++", "-I", vnodePath+"src"+File.separator+"vnodelp"+File.separator+"include", "-c", "-o", oFileName, cFileName};
		//String[] myArgs = new String[] {"g++", "/Users/anvan/OneDrive/OneDrive - Danmarks Tekniske Universitet/runtimes/runtime-ERODE.product(4)/VanderPol/test.cc","-o","/Users/anvan/OneDrive/OneDrive - Danmarks Tekniske Universitet/runtimes/runtime-ERODE.product(4)/VanderPol/testcc.o"};
		boolean ok = executeExternalProcess(cFileName, out, bwOut, msgDialogShower, "compiling", "VNODE-LP script",myArgs,logFileName,false,false);
		if(!ok) {
			return false;
		}
		//g++ -L /home/utopia/Escritorio/VNODE/installation/lib -Le -L/home/utopia/Desktop/UTOPIC/VNODE/src/vnodelp/lib		 -o vanderPol_Pontryagin vanderPol_Pontryagin.o -lvnode -lfi -lprim -lieee -llapack -lblas -lstdc++ -lg2c
		myArgs = new String[] {"g++", /*"-L", vnodePath+"installation"+File.separator+"lib",*/ "-Le", "-L", vnodePath+"src"+File.separator+"vnodelp"+File.separator+"lib", "-o", execFileName, oFileName, "-lvnode", "-lfi", "-lprim", "-lieee", "-llapack", "-lblas", "-lstdc++", "-lg2c"};
		//myArgs = new String[] {"g++", "/Users/anvan/OneDrive/OneDrive - Danmarks Tekniske Universitet/runtimes/runtime-ERODE.product(4)/VanderPol/test.cpp","-o","/Users/anvan/OneDrive/OneDrive - Danmarks Tekniske Universitet/runtimes/runtime-ERODE.product(4)/VanderPol/testcpp.o"};
		//CRNReducerCommandLine.print(out,bwOut," (1st compilation completed)");
		
		ok = executeExternalProcess(cFileName, out, bwOut, msgDialogShower, "compiling", "VNODE-LP script",myArgs,logFileName,true,false);
		
		
		return ok;
		
		/*
		Compile - FULL PATHS - DIFFERENT MODEL
		g++ -g -O2 -Wno-deprecated -DNDEBUG -DFILIB_VNODE -DMAXORDER=50 -I/home/utopia/Desktop/UTOPIC/VNODE/src/vnodelp/FADBAD++  -I/home/utopia/Desktop/UTOPIC/VNODE/src/vnodelp/include   -c -o vanderPol_Pontryagin.o vanderPol_Pontryagin.cc
		g++ -Le -L/home/utopia/Desktop/UTOPIC/VNODE/src/vnodelp/lib		 -o vanderPol_Pontryagin vanderPol_Pontryagin.o -lvnode -lfi -lprim -lieee -llapack -lblas -lstdc++ -lg2c



		Compile - FULL PATHS
		g++ -g -O2 -Wno-deprecated -DNDEBUG -DFILIB_VNODE -DMAXORDER=50  -I/home/utopia/Desktop/UTOPIC/VNODE/src/vnodelp/FADBAD++  -I/home/utopia/Desktop/UTOPIC/VNODE/src/vnodelp/include   -c -o vanderpol.o vanderpol.cc
		g++  -Le -L/home/utopia/Desktop/UTOPIC/VNODE/src/vnodelp/lib		 -o vanderpol vanderpol.o -lvnode -lfi -lprim -lieee -llapack -lblas -lstdc++ -lg2c

		Using Josu's .sh
		Maximum reachable point enclosure at t = [1, 1]
		[1.11, 1.11]
		Minimum reachable point enclosure at t = [1, 1]
		[0.905, 0.905]

		Using the explicit compilation commands from the console 
		Maximum reachable point enclosure at t = [1, 1]
		[1.23, 1.23]
		Minimum reachable point enclosure at t = [ UNDEFINED ]
		[0.993, 0.993]





		Compile - LOCAL PATHS

		g++ -g -O2 -Wno-deprecated -DNDEBUG -DFILIB_VNODE -DMAXORDER=50 -I/home/utopia/Escritorio/VNODE/installation/include -I/home/utopia/Desktop/UTOPIC/VNODE/src/vnodelp/FADBAD++  -I../include   -c -o vanderpol.o vanderpol.cc


		g++ -L/home/utopia/Escritorio/VNODE/installation/lib -Le -L../lib		 -o vanderpol vanderpol.o -lvnode -lfi -lprim -lieee -llapack -lblas -lstdc++ -lg2c
		*/
	}
	
	public boolean executeVNODELPScript(String cFileName, MessageConsoleStream out, BufferedWriter bwOut,IMessageDialogShower msgDialogShower) {
		//String oFileName = AbstractImporter.overwriteExtensionIfEnabled(cFileName,".o",true);
		String execFileName = AbstractImporter.overwriteExtensionIfEnabled(cFileName,"",true);
		String logFileName = AbstractImporter.overwriteExtensionIfEnabled(cFileName,"",true);
		logFileName = logFileName+"EXECUTION.log";
		//g++ test.cpp -o test.o
		//String[] myArgs = new String[] {"test.o"};
		String[] myArgs = new String[] {execFileName};
		//String[] myArgs = new String[] {"/Users/anvan/OneDrive/OneDrive - Danmarks Tekniske Universitet/runtimes/runtime-ERODE.product(4)/VanderPol/testcc.o"};
		boolean ok = executeExternalProcess(cFileName, out, bwOut, msgDialogShower, "executing", "VNODE-LP script",myArgs,logFileName,false,true);
		return ok;
	}
		
	private boolean executeExternalProcess(String cFileName, MessageConsoleStream out, BufferedWriter bwOut,
			IMessageDialogShower msgDialogShower, String operation, String onWhat, String[] myArgs, String logFile, boolean appendOnLog, boolean printOutput) {
		
		//String oFileName = AbstractImporter.overwriteExtensionIfEnabled(cFileName,".o",true);
		
		//g++ test.cpp -o test.o
		//String[] myArgs = new String[] {"g++", "test.cpp","-o","test.o"};
		//String[] myArgs = new String[] {"g++", cFileName,"-o", oFileName};
		ProcessBuilder pb = new ProcessBuilder(myArgs);
		//ProcessBuilder pb = new ProcessBuilder("g++", "test2.cpp","-o","test.o");
		//ProcessBuilder pb = new ProcessBuilder("cat", "test.cpp");
		//pb = new ProcessBuilder(List<String> command)
		
		//String usrDir = System.getProperty("usr.dir");
		int lastSep = cFileName.lastIndexOf(File.separator);
		String directory = File.separator;
		if(lastSep!=-1) {
			directory = cFileName.substring(0,lastSep);
		}
		File dir = new File(directory);
		pb.directory(dir);
		
		Process process;
		try {
			process = pb.start();
		} catch (IOException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,msgDialogShower,"Problems in "+operation+" the "+onWhat+" "+cFileName+".\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return false;
		}
		
		int errCode;
		try {
			errCode = process.waitFor();
			if(errCode!=0) {
				//PROBLEM!
			}
		} catch (InterruptedException e2) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,msgDialogShower,"Problems in "+operation+" the "+onWhat+" "+cFileName+".\nError message:\n"+e2.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e2);
			return false;
		}

		InputStream is = process.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line;

		InputStream is_err = process.getErrorStream();
		InputStreamReader isr_err = new InputStreamReader(is_err);
		BufferedReader br_err = new BufferedReader(isr_err);
		
		Vector<String> output = new Vector<>();
		boolean hasOutput=false;
		try {
			while ((line = br.readLine()) != null) {
				hasOutput=true;
				output.add(line);
			}
			br.close();
			isr.close();
		} catch (IOException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,msgDialogShower,"Problems in reading the output of the process.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return false;
		}

		boolean hasErrorOutput=false;
		Vector<String> errorOutput = new Vector<>();
		try {
		while ((line = br_err.readLine()) != null) {
			hasErrorOutput=true;
			errorOutput.add(line);
		}
		br_err.close();
		isr_err.close();
		} catch (IOException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,msgDialogShower,"Problems in reading the error output of the process.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return false;
		}
		
		
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(new File(logFile),appendOnLog));
		} catch (IOException e1) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,msgDialogShower,"Problems in opening the log file "+logFile+"\nError message:\n"+e1.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e1);
			return false;
		}

		PrintWriter logFileOut = new PrintWriter(bw);

		if(appendOnLog) {
			logFileOut.println("\n\n");	
		}

		if(hasOutput) {
			if(printOutput) {
				CRNReducerCommandLine.println(out, bwOut);
			}
			//logFileOut.println("There is output\n");
			logFileOut.printf("Output of running %s is:\n\n", Arrays.toString(myArgs));
			for(String l : output) {
				logFileOut.println(l);
				if(printOutput) {
					CRNReducerCommandLine.print(out, bwOut, "\t");
					CRNReducerCommandLine.println(out, bwOut, l);
				}
			}
			logFileOut.println("\nEND OUTPUT\n\n");
//			if(printOutput) {
//				CRNReducerCommandLine.println(out, bwOut, "\nEND OUTPUT\n");
//			}
		}
		else {
			logFileOut.printf("No output produced running:\n%s\n", Arrays.toString(myArgs));
		}
		logFileOut.println("");
		if(errCode!=0 || hasErrorOutput) {
			if(hasErrorOutput) {
				//logFileOut.println("There is error output");
				logFileOut.printf("Error output of running %s is:\n\n", Arrays.toString(myArgs));
				for(String l : errorOutput) {
					logFileOut.println(l);
				}
				logFileOut.println("\nEND ERROR OUTPUT\n");
				logFileOut.println("");
				if(errCode!=0) {
					logFileOut.printf("The process terminated with Error code %s (0=successful termination)\n\n", String.valueOf(errCode));
				}
				//CRNReducerCommandLine.print(out, bwOut, " (ERRORS - check log file in "+onWhat+" folder)");
			}
			CRNReducerCommandLine.print(out, bwOut, " (ERROR CODE " + errCode+ " - check log file in "+onWhat+" folder)");
		}
		else {
			logFileOut.printf("No error output produced running:\n%s\n", Arrays.toString(myArgs));
		}

		logFileOut.close();
		try {
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		//ok if I have no error output, and error code is 0
		return (!hasErrorOutput) && errCode==0;
	}



}
