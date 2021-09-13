package it.imt.erode.utopic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
//import java.util.concurrent.Future;

import org.eclipse.ui.console.MessageConsoleStream;
import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.ASTNode.Type;
import org.sbml.jsbml.text.parser.ParseException;

import com.mathworks.engine.EngineException;
import com.mathworks.engine.MatlabEngine;

import it.imt.erode.auxiliarydatastructures.CRNandPartition;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.DialogType;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.commandline.Terminator;
import it.imt.erode.crn.implementations.CRN;
import it.imt.erode.crn.implementations.CRNReactionArbitraryGUI;
import it.imt.erode.crn.implementations.Species;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.importing.AbstractImporter;
import it.imt.erode.importing.GUICRNImporter;
import it.imt.erode.importing.MatlabODEsImporter;
import it.imt.erode.importing.UnsupportedFormatException;
import it.imt.erode.importing.UnsupportedReactionNetworkEncodingException;
import it.imt.erode.partition.implementations.Block;
import it.imt.erode.partition.implementations.Partition;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;

public class MatlabODEPontryaginExporter {
	
	public static final boolean innerSymbolicJacobian = false; 
	
	
	/*
	 boolean containsMatlabPath = false;
		try {
			containsMatlabPath = JavaLibraryPathHandler.checkForPathContaining("MATLAB");
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e2) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,msgDialogShower,"Problems in checking if the path \"matlabroot/bin/<arch>\" appears in the user paths.\nError message:\n"+e2.getMessage(),DialogType.Warning);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e2);
			//return false;
		}
	 
		if(cora && (coraLibrary==null || coraLibrary.equals(UNSPECIFIEDPATH))){
			try {
				coraLibrary=getRequiredLibrary(MatlabODEPontryaginExporter.CORADOWNLOADMESSAGE, MatlabODEPontryaginExporter.CORALOCATEMESSAGE, MatlabODEPontryaginExporter.FILEWITHCORA_2016LOCATION, MatlabODEPontryaginExporter.CORADOWNLOADPATH, MatlabODEPontryaginExporter.CORADOWNLOADPATHSHORT,out,bwOut);
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Exception while retrieving the path of the CORA library.");
				CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
				CRNReducerCommandLine.printExceptionShort(out, bwOut, e);
				return;
			}
			if(coraLibrary==null || coraLibrary.equals(UNSPECIFIEDPATH)){
				CRNReducerCommandLine.println(out,bwOut,"Could not retrieve the path of the CORA library.");
				CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
				return;
			}
		} 
	  
	 */
	
	public boolean executeMatlabScript(String fileName, MessageConsoleStream out, BufferedWriter bwOut, IMessageDialogShower msgDialogShower) /*throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException*/ {

		/*
		 * see code in CRNReducerCommandLine for how to check if the required matlab path is in the java library path, and how to add and store it in a file if necessary  
		boolean containsMatlabPath = false;
		try {
			containsMatlabPath = JavaLibraryPathHandler.checkForPathContaining("MATLAB");
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e2) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,msgDialogShower,"Problems in checking if the path \"matlabroot/bin/<arch>\" appears in the user paths.\nError message:\n"+e2.getMessage(),DialogType.Warning);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e2);
			//return false;
		}
		
		if(!containsMatlabPath) {
			try {
				JavaLibraryPathHandler.addLibraryPath("/Applications/MATLAB_R2018a.app/bin/maci64");
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				CRNReducerCommandLine.printWarning(out, bwOut,true,msgDialogShower,"Problems in adding  \"matlabroot/bin/<arch>\" to the java library path.\nError message:\n"+e.getMessage(),DialogType.Error);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
				return false;
			}
		}
		*/

		MatlabEngine eng;
		try {
			eng = MatlabEngine.startMatlab();
		} catch (EngineException | IllegalArgumentException | IllegalStateException | InterruptedException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,msgDialogShower,"Problems in starting the Matlab engine .\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return false;
		}
		/*
		double[] a = {2.0 ,4.0, 6.0};
		double[] roots = eng.feval("sqrt", a);
		for (double e: roots) {
			System.out.println(e);
		}
		 */

		//run("fileName")
		String run = "run('"+ fileName+"')";
		try {
			eng.eval(run);
			//Future<Void> future = eng.evalAsync(run);
		} catch (CancellationException | InterruptedException | ExecutionException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,msgDialogShower,"Problems in executing "+run+" using the Matlab engine .\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			try {
				eng.close();
			} catch (EngineException e1) {
				CRNReducerCommandLine.printWarning(out, bwOut,true,msgDialogShower,"Problems in closing the Matlab engine .\nError message:\n"+e.getMessage(),DialogType.Warning);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
			return false;
		}

		try {
			eng.close();
		} catch (EngineException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,msgDialogShower,"Problems in closing the Matlab engine .\nError message:\n"+e.getMessage(),DialogType.Warning);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}

		return true;
	}
	
	public boolean printConverginPontryaginToMatlabFIle(ICRN crn, String name, boolean verbose,
				MessageConsoleStream out, BufferedWriter bwOut, IMessageDialogShower msgDialogShower, double tEnd,
				//
				HashMap<String, Integer> parameterToPerturbToItsPosition, String[] paramsToPerturb, double[] lows, double[] highs,
				//
				Terminator terminator, HashMap<String, Double> speciesToCoefficient,boolean writeSymbolicJacobian,
				int maxNumberOfIterations, //\nu in the paper, kMax
				double delta, // the threshold
				double integrationStep //the maximum time step for ODE integration
				,double step, boolean plot, boolean exitMatlab, boolean runMatlab, boolean compileAndRunVNODE
				)  throws UnsupportedFormatException{
			
		String varName = "x"; 
		String derivName = "f";
		String controlName="u";
		
			String fileName = name;
			if(crn.isSymbolic()){
				CRNReducerCommandLine.printWarning(out,bwOut, true, msgDialogShower, "The Pontryagin script can be created only for mass-action CRNs without symbolic parameters.", DialogType.Error);
				return false;
			}
			
			
			HashMap<String,ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpecies().size());
			for (ISpecies species : crn.getSpecies()) {
				speciesNameToSpecies.put(species.getName(), species);
			}
			
			/*if(!writeSymbolicJacobian){
				if(!crn.isMassAction()){
					CRNandSpeciesAndPartition crnAndSpecies=computeRNEncoding(crn, out, bwOut, null);
					if(crnAndSpecies==null){
						return;
					}
					crn = crnAndSpecies.getCRN();
					if(crn==null){
						return;
					}
				}	
			}*/
			
			ISpecies[] speciesIdToSpecies=new ISpecies[crn.getSpecies().size()];
			int s=0;
			for (ISpecies species : crn.getSpecies()) {
				speciesIdToSpecies[s]=species;
				s++;
			}
			
			fileName=AbstractImporter.overwriteExtensionIfEnabled(fileName,".m",true);
			if(verbose){
				CRNReducerCommandLine.print(out,bwOut,"Writing the Pontryagin script in file "+fileName);
			}
			
			AbstractImporter.createParentDirectories(fileName);
			BufferedWriter bw;
			try {
				bw = new BufferedWriter(new FileWriter(fileName));
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in printConverginPontryaginToMatlabFIle, exception raised while creating the filewriter for file: "+fileName);
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
					message="% Reachability analysis of the system"+modelName+"with "+paramsToPerturb.length+" uncertain parameters %";
				} 
				else{
					message="% Reachability analysis of the system"+modelName+"with 1 uncertain parameter %";
				}
				StringBuilder sb = new StringBuilder(message.length());
				for(int i=0;i<message.length();i++){
					sb.append('%');
				}
				String caption=sb.toString();
				bw.write(caption+"\n");
				bw.write(message+"\n");
				//bw.write("% Parameters that are common for CORA and Pontryagin's algo are set here.\n");
				bw.write(caption+"\n");
				
				bw.write("\n");
				
				bw.write("% Automatically generated from "+crn.getName()+".\n");
				bw.write("% Number of species: "+crn.getSpecies().size()+".\n");
				bw.write("% Number of reactions: "+crn.getReactions().size()+".\n");
				bw.write("% Coefficients: ");
				writeCoefficients(crn, speciesToCoefficient, bw);
				bw.write(".\n");
				bw.write("% Perturbed parameters: ");
				for(int i=0;i<paramsToPerturb.length;i++){
					bw.write(paramsToPerturb[i]+" in ["+lows[i]+","+highs[i]+"]");
					if(i<paramsToPerturb.length-1){
						bw.write(", ");
					}
				}
				bw.write("\n");
				bw.write("\n% Correspondence with original names:\n");
				int incr=1;
				for (ISpecies species : crn.getSpecies()) {
					bw.write("%     "+varName+"(" +(species.getID()+incr)+") = " + ((species.getOriginalName()==null)?species.getName():species.getOriginalName())+"\n");
				}
				bw.write("\n\n");
				
				String prefixSpace="\t";
				
				//String ponitryaginFunctionName = fileName.replace(".m", "");
				String ponitryaginFunctionName = AbstractImporter.overwriteExtensionIfEnabled(fileName,"",true);
				int indexOfLastSep = ponitryaginFunctionName.lastIndexOf(File.separator);
				if(indexOfLastSep>=0){
					ponitryaginFunctionName=ponitryaginFunctionName.substring(indexOfLastSep+1);
				}
				
				bw.write("function "+ponitryaginFunctionName+"()\n\n");

				/////////////////////////////////////////
//				bw.write(prefixSpace+"global u_num\n");
//				bw.write(prefixSpace+"global x_num\n");
//				bw.write(prefixSpace+"global htHx\n");
//				bw.write(prefixSpace+"global htHu\n");
//				bw.write(prefixSpace+"global htf\n");
//				bw.write(prefixSpace+"global x_ind_Hx\n");    
//				bw.write(prefixSpace+"global u_ind_Hx\n");        
//				bw.write(prefixSpace+"global x_ind_Hu\n");    
//				bw.write(prefixSpace+"global p_ind_Hu\n");           
				bw.write(prefixSpace+"global dt \n");
				bw.write("\n");	
				
				
				
				
				
				
				int nVars = crn.getSpecies().size();
				//Parameters to perturb
				int nControls=paramsToPerturb.length;
				
				
				
				

				bw.write("\n");			
				bw.write(prefixSpace+"% Number of variables\n");
				bw.write(prefixSpace+"sVars = "+nVars+"; \n");
				bw.write(prefixSpace+"% Number of controls\n");
				bw.write(prefixSpace+"uVars = "+nControls+"; \n");
				bw.write(prefixSpace+"% Plot flag\n");
				if(plot){
					bw.write(prefixSpace+"doPlot = 1;\n");
				}
				else{
					bw.write(prefixSpace+"doPlot = 0;\n");
				}
				/*
				bw.write(prefixSpace+"% Upper control boundaries\n");
				//up limit of uncertainty interval
				bw.write(prefixSpace+"u_max = 2 .* ones(1,uVars);\n");
				//down limit of uncertainty interval
				bw.write(prefixSpace+"% Lower control boundaries\n");
				bw.write(prefixSpace+"u_min = ones(1,uVars);\n");
				bw.write("\n");
				*/
				if(paramsToPerturb.length>1){
					StringBuffer thetamin=new StringBuffer(prefixSpace+"u_min=[");
					StringBuffer thetamax=new StringBuffer(prefixSpace+"u_max=[");
					for(int i=0;i<paramsToPerturb.length;i++){
						bw.write(prefixSpace+"% \tThe uncertain parameter "+paramsToPerturb[i]+" belongs to ["+lows[i]+","+highs[i]+"];\n");
						thetamin.append(" "+lows[i]);
						thetamax.append(" "+highs[i]);
					}
					thetamin.append(" ];\n");
					thetamax.append(" ];\n");
					bw.write(prefixSpace+"% Upper control boundaries\n");
					bw.write(thetamin.toString());
					bw.write(prefixSpace+"% Lower control boundaries\n");
					bw.write(thetamax.toString());
				}
				else{
					bw.write(prefixSpace+"% The only uncertain parameter ("+paramsToPerturb[0]+") belongs to ["+lows[0]+","+highs[0]+"];\n");
					bw.write(prefixSpace+"% Upper control boundaries\n");
					bw.write(prefixSpace+"u_min="+lows[0]+";\n");
					bw.write(prefixSpace+"% Lower control boundaries\n");
					bw.write(prefixSpace+"u_max="+highs[0]+";\n");
				}
				bw.write("\n");
				
				
				
				
				
				bw.write(prefixSpace+"%Initial condition\n");			
				bw.write(prefixSpace+"x0=[ ");
				int sp=0;
				for (ISpecies species : crn.getSpecies()) {
					bw.write(" "+species.getInitialConcentration().doubleValue());
					if(sp<crn.getSpecies().size()-1){
						bw.write(",");
					}
					else{
						bw.write(" ");
					}
					sp++;
					//speciesNameToSpecies.put(species.getName(), species);
				}
				bw.write("];\n");
				
				//%pt = -alphas
				bw.write(prefixSpace+"% Vector of coefficients of the sum of variables\n");
				bw.write(prefixSpace+"vcoeff=[");
				writeCoefficients(crn, speciesToCoefficient, bw);
				bw.write(" ];\n");
//				bw.write(prefixSpace+"% Boundary condition of the costate\n");
//				if(maximize){
//					bw.write(prefixSpace+"pT = - vcoeff;\n");
//					bw.write(prefixSpace+"%Use the following if interested in minimizing\n");
//					bw.write(prefixSpace+"%pT = vcoeff;\n");
//				}
//				else{
//					bw.write(prefixSpace+"pT = vcoeff;\n");
//					bw.write(prefixSpace+"%Use the following if interested in maximizing\n");
//					bw.write(prefixSpace+"%pT = - vcoeff;\n");
//				}
				
				
				bw.write(prefixSpace+"% Finite time horizon\n");
				bw.write(prefixSpace+"T="+tEnd+";\n");
				bw.write(prefixSpace+"% Integration time step\n");
				bw.write(prefixSpace+"dt="+integrationStep+";\n");
				
				//BEGIN IT SEEMS THAT THIS TEXT IS ALWAYS THE SAME
				bw.write(prefixSpace+"% Intergation intervals\n");
				bw.write(prefixSpace+"tspan = 0 : dt : T;\n");
				bw.write(prefixSpace+"tspanRev = T : -dt : 0;\n");
//				bw.write(prefixSpace+"% Compute initial control guess\n");
//				bw.write(prefixSpace+"u_num = zeros(uVars,size(tspan,2));\n");        
//				bw.write(prefixSpace+"delta_u_num = zeros(uVars,size(tspan,2));\n");        
//				bw.write(prefixSpace+"new_u_num = zeros(uVars,size(tspan,2));\n");            
//				bw.write(prefixSpace+"for i = 1 : uVars        \n");
//				bw.write(prefixSpace+"\tu_num(i,:) = ((u_max(i) + u_min(i)) / 2) .* ones(1,size(tspan,2));\n");
//				bw.write(prefixSpace+"end\n\n\n");
				//END IT SEEMS THAT THIS TEXT IS ALWAYS THE SAME
				
				bw.write("\n\n");
				
				bw.write(prefixSpace+"% Algorithm parameters\n");
				//Delta should be a real. If delta<0 will not stop, but will do the maximum number of iterations. 
				bw.write(prefixSpace+"maximize = 1;\n");
				bw.write(prefixSpace+"minimize = 0;\n");
				bw.write(prefixSpace+"delta = "+delta+"; % threshold\n");
				bw.write(prefixSpace+"max_iter = "+maxNumberOfIterations+"; % maximal number of algorithm iterations\n");
				bw.write(prefixSpace+"step = "+step+";\n");
				bw.write(prefixSpace+"gamma = step*ones(1,max_iter); % step-sizes of the correction algorithm\n\n");

				//BEGIN IT SEEMS THAT THIS TEXT IS ALWAYS THE SAME
				bw.write(prefixSpace+"x = sym('x', [sVars 1]);\n");
				bw.write(prefixSpace+"u = sym('u', [uVars 1]);\n");
				bw.write(prefixSpace+"p = sym('p', [sVars 1]);\n");
				bw.write(prefixSpace+"f = sym('f', [sVars 1]);\n\n\n");
				//END IT SEEMS THAT THIS TEXT IS ALWAYS THE SAME

				
				/*
				-u are the parameters to perturb
				-x are the ode variables
				% Symbolic ODE drift vector            
			    f(1) = -u(1)*x(1)*x(2) + u(3)*x(3);
			    f(2) = u(1)*x(1)*x(2) - u(2)*x(2);
			    f(3) = u(2)*x(2) - u(3)*x(3);
			    */
				bw.write(prefixSpace+"% Symbolic ODE drift vector\n");
				//write ODE: Write the drifts
				writeODEFunction(crn, bw, speciesNameToSpecies,"\t",false,false,false,false,true,null,false,varName,derivName,parameterToPerturbToItsPosition, paramsToPerturb,controlName);
				bw.write("\n\n");
				
				bw.write(prefixSpace+"initpontryagin(f,p,x,u);\n\n");
				
				bw.write(prefixSpace+"cHeader = {");
				for(int p=0; p<paramsToPerturb.length;p++) {
					bw.write(" '"+paramsToPerturb[p]+"'");
				}
				bw.write(prefixSpace+" };\n");
				
				bw.write(prefixSpace+"pontryagin(maximize,vcoeff,max_iter,delta,uVars,cHeader,gamma,tspan,tspanRev,x0,u_max,u_min,'"+ponitryaginFunctionName+"_max.csv',doPlot,false);\n");
				bw.write(prefixSpace+"pontryagin(minimize,vcoeff,max_iter,delta,uVars,cHeader,gamma,tspan,tspanRev,x0,u_max,u_min,'"+ponitryaginFunctionName+"_min.csv',doPlot,true);\n");
				
				bw.write("\n");
				if(exitMatlab){
					bw.write(prefixSpace+"exit;\n");
				}
				else{
					bw.write(prefixSpace+"%exit;\n");
				}
				
				bw.write("\n\n");
				bw.write("end\n");
				
				//printResource("converginpontryiagin"+File.separator+"initpontryagin.txt", bw,getClass());
				printResource("initpontryagin.txt", bw,getClass());
				bw.write("\n\n");
				printResource("pontryagin.txt", bw,getClass());
				
				printResource("bottomFunctions.txt", bw,getClass());
				bw.write("\n\n");
				
				


//				printResource("converginpontryiagin"+File.separator+"endOfMainFunction.txt", bw);
//				bw.write(prefixSpace+"\n");
//				bw.write(prefixSpace+"\n");
//				bw.write(prefixSpace+"u_numTransp = u_num';\n");
//				//bw.write(prefixSpace+"csvwrite('"+ponitryaginFunctionName+".dat',u_numTransp);\n");
//				//bw.write(prefixSpace+"dlmwrite('"+ponitryaginFunctionName+".dat',u_numTransp, 'precision', '%i');\n");
//				
//
//				bw.write(prefixSpace+"cHeader = {");
//				for(int p=0;p<paramsToPerturb.length;p++){
//					bw.write(" '"+paramsToPerturb[p]+"'");
//				}
//				bw.write(prefixSpace+" }; \n");
//				bw.write(prefixSpace+"commaHeader = [cHeader;repmat({','},1,numel(cHeader))]; %insert commaas\n");
//				bw.write(prefixSpace+"commaHeader = commaHeader(:)';\n");
//				bw.write(prefixSpace+"textHeader = cell2mat(commaHeader); %cHeader in text with commas\n");
//				bw.write(prefixSpace+"%write header to file\n");
//				bw.write(prefixSpace+"fid = fopen('"+ponitryaginFunctionName+".csv','w');\n"); 
//				bw.write(prefixSpace+"fprintf(fid,'%s\\n',textHeader);\n");
//				bw.write(prefixSpace+"fclose(fid);\n");
//				bw.write(prefixSpace+"%write data to end of file\n");
//				if(maximize){
//					bw.write(prefixSpace+"dlmwrite('"+ponitryaginFunctionName+"_max.csv',u_numTransp, 'precision', '%i','-append');\n");
//				}
//				else{
//					bw.write(prefixSpace+"dlmwrite('"+ponitryaginFunctionName+"_min.csv',u_numTransp, 'precision', '%i','-append');\n");
//				}
//				
//				
//				
//				bw.write(prefixSpace+"\n");
//				bw.write(prefixSpace+"\n");
//				if(plot){
//					printResource("converginpontryiagin"+File.separator+"plot.txt", bw);
//				}
//				
//				bw.write("end\n\n\n");
//				
//				printResource("converginpontryiagin"+File.separator+"bottomFunctions.txt", bw);
//				
//				
//				
//				
//				bw.write("\n\n");
				
				
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
	
	
	private void writeODEFunction(ICRN crn, BufferedWriter bw, HashMap<String, ISpecies> speciesNameToSpecies,String prefixSpace,boolean writeParameters, boolean writeT,
			boolean writeFunction, boolean initZeros,boolean writeParenthesis,String suffix,boolean writEnd,String varName,String derivName,
			HashMap<String, Integer> parameterToPerturbToItsPosition,String[] paramsToPerturb, String controlName) throws IOException, UnsupportedFormatException {
		
		HashMap<ISpecies, StringBuilder> speciesToDrift=null;
		if(crn.isMassAction()){
			speciesToDrift = GUICRNImporter.computeDriftsReplacingSpeciesNames(crn, false, varName, 1,writeParenthesis,suffix);
			speciesToDrift = replaceParameterWithControlOrConstant(speciesToDrift,parameterToPerturbToItsPosition,controlName,speciesNameToSpecies,crn);
			MatlabODEsImporter.writeDriftsOfMassActionWithSpeciesNameAlreadyReplacedWithY(derivName,speciesToDrift, crn, bw, prefixSpace);
		}
		else{
			boolean ignoreI=false;
			speciesToDrift = GUICRNImporter.computeDrifts(crn,false,ignoreI);
			speciesToDrift = replaceParameterWithControlOrConstant(speciesToDrift,parameterToPerturbToItsPosition,controlName,speciesNameToSpecies,crn);
			int idIncrement=1;
			MatlabODEsImporter.writeDriftsOfArbitraryODEReplacingSpeciesNamesWithY(idIncrement,varName,derivName,speciesToDrift, crn, bw, speciesNameToSpecies,prefixSpace);
		}
		
		
	}


	private static HashMap<ISpecies, StringBuilder> replaceParameterWithControlOrConstant(
			HashMap<ISpecies, StringBuilder> speciesToDrift, HashMap<String, Integer> parameterToPerturbToItsPosition, String controlName,
			HashMap<String, ISpecies> speciesNameToSpecies,ICRN crn) throws UnsupportedFormatException {
		
		HashMap<ISpecies, StringBuilder> retSpeciesToDrift=new HashMap<>(speciesToDrift.size());
		for (Entry<ISpecies, StringBuilder> entry : speciesToDrift.entrySet()) {
			ASTNode ratelaw=null;
			String s = entry.getValue().toString();
			try {
				//System.out.println("Parsing: " +entry.getKey().getName());
				//System.out.println(s);
				ratelaw = ASTNode.parseFormula(s);
			} catch (ParseException e) {
				System.out.println(s);
				throw new UnsupportedFormatException("Problems in parsing the rate expression "+s);
			}
			replaceParameterWithControlOrConstant(ratelaw,parameterToPerturbToItsPosition,controlName,speciesNameToSpecies,crn);
			retSpeciesToDrift.put(entry.getKey(), new StringBuilder(ratelaw.toFormula()));
		}
		return retSpeciesToDrift;
		
	}
	
	

	private static void replaceParameterWithControlOrConstant(ASTNode node,
			HashMap<String, Integer> parameterToPerturbToItsPosition,String controlName, HashMap<String, ISpecies> speciesNameToSpecies,ICRN crn) {
		int idIncrement = 1;
		//A variable is actually a parameter, while a function (e.g., varsName(1)) is an ODE variable (with id 0).
		//System.out.println(node);
		if(node.isOperator()){
			for(int i=0;i<node.getChildCount();i++){
				replaceParameterWithControlOrConstant(node.getChild(i), parameterToPerturbToItsPosition,controlName,speciesNameToSpecies,crn);
			}
		}
		else if(node.isFunction() ){
			for(int i=0;i<node.getChildCount();i++){
				replaceParameterWithControlOrConstant(node.getChild(i), parameterToPerturbToItsPosition,controlName,speciesNameToSpecies,crn);
			}
		}
		/*else if(node.isFunction() && node.getName().equals(varsName)){
			int id = node.getListOfNodes().get(0).getInteger();
			int actualId = id-idIncrement;
			node.removeChild(0);
			node.setType(ASTNode.Type.NAME);
			node.setName(speciesIdToSpecies[actualId].getName());
		}*/
		else if(node.isVariable()){
			ISpecies species = speciesNameToSpecies.get(node.getName());
			//If it is not a species, then it is a parameter
			if(species==null){
				Integer pos = parameterToPerturbToItsPosition.get(node.getName());
				if(pos!=null){
					node.addChild(new ASTNode(pos+idIncrement));
					node.setName(controlName);

					node.setType(Type.FUNCTION);
				}
				else{
					double par = crn.getMath().evaluate(node.getName());
					node.setValue(par);
				}
			}
			else{
				//DO NOTHING
			}
			//DO NOTHING	
			//Pay attention: functions are considered variables  in isVariables()...
		}
		else if(node.isNumber()){
			//DO NOTHING
		}
		else{
			throw new UnsupportedOperationException(node.toString());
		}
		
	}


	public static void printResource(String path, BufferedWriter bw, Class<? extends Object> myClass) throws IOException{
		//InputStream is = getClass().getResourceAsStream("pointryaginScript.txt");
		InputStream is = myClass.getResourceAsStream(path);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line=br.readLine();
		while(line!=null){
			bw.write(line);
			bw.write("\n");
			line=br.readLine();
		}
		br.close();
	}
	

	//@SuppressWarnings("unused")
	public void printPontryaginToMatlabFIle(ICRN crn, String name, boolean verbose,
			MessageConsoleStream out, BufferedWriter bwOut, IMessageDialogShower msgDialogShower, double tEnd,
			//
			HashMap<String, Integer> parameterToPerturbToItsPosition, String[] paramsToPerturb, double[] lows, double[] highs,
			//
			Terminator terminator, HashMap<String, Double> speciesToCoefficient,boolean writeSymbolicJacobian,
			int maxNumberOfIterations, //\nu in the paper
			double epsilon, // the threshold
			double maxStep //the maximum time step for ODE integration
			)  throws UnsupportedFormatException{
		
		String fileName = name;
		if(crn.isSymbolic()){
			CRNReducerCommandLine.printWarning(out,bwOut, true, msgDialogShower, "The Pontryagin script can be created only for mass-action CRNs without symbolic parameters.", DialogType.Error);
			return;
		}
		
		
		HashMap<String,ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpecies().size());
		for (ISpecies species : crn.getSpecies()) {
			speciesNameToSpecies.put(species.getName(), species);
		}
		
		if(!writeSymbolicJacobian){
			if(!crn.isMassAction()){
				CRNandPartition crnAndSpecies=computeRNEncoding(crn, out, bwOut, null,true);
				if(crnAndSpecies==null){
					return;
				}
				crn = crnAndSpecies.getCRN();
				if(crn==null){
					return;
				}
			}	
		}
		
//		ISpecies[] speciesIdToSpecies=new ISpecies[crn.getSpecies().size()];
//		int s=0;
//		for (ISpecies species : crn.getSpecies()) {
//			speciesIdToSpecies[s]=species;
//			s++;
//		}
		
		fileName=AbstractImporter.overwriteExtensionIfEnabled(fileName,".m");
		if(verbose){
			CRNReducerCommandLine.print(out,bwOut,"Writing the Pontryagin script in file "+fileName);
		}
		
		AbstractImporter.createParentDirectories(fileName);
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printPontryaginToMatlabFIle, exception raised while creating the filewriter for file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		
		try {
			
			String modelName = " ";
			if(crn.getName()!=null&&crn.getName()!=""){
				modelName = " "+GUICRNImporter.getModelName(crn.getName())+" ";
			}
			
			String message="";
			if(paramsToPerturb.length>1){
				message="% Reachability analysis of the system"+modelName+"with "+paramsToPerturb.length+" uncertain parameters %";
			} 
			else{
				message="% Reachability analysis of the system"+modelName+"with 1 uncertain parameter %";
			}
			StringBuilder sb = new StringBuilder(message.length());
			for(int i=0;i<message.length();i++){
				sb.append('%');
			}
			String caption=sb.toString();
			bw.write(caption+"\n");
			bw.write(message+"\n");
			//bw.write("% Parameters that are common for CORA and Pontryagin's algo are set here.\n");
			bw.write(caption+"\n");
			
			bw.write("\n");
			
			bw.write("% Automatically generated from "+crn.getName()+".\n");
			bw.write("% Number of species: "+crn.getSpecies().size()+".\n");
			bw.write("% Number of reactions: "+crn.getReactions().size()+".\n");
			bw.write("% Coefficients: ");
			writeCoefficients(crn, speciesToCoefficient, bw);
			bw.write(".\n");
			bw.write("% Perturbed parameters: ");
			for(int i=0;i<paramsToPerturb.length;i++){
				bw.write(paramsToPerturb[i]+" in ["+lows[i]+","+highs[i]+"]");
				if(i<paramsToPerturb.length-1){
					bw.write(", ");
				}
			}
			bw.write("\n");
			bw.write("\n% Correspondence with original names:\n");
			int incr=1;
			for (ISpecies species : crn.getSpecies()) {
				bw.write("%     y(" +(species.getID()+incr)+") = " + ((species.getOriginalName()==null)?species.getName():species.getOriginalName())+"\n");
			}
			bw.write("\n\n");
			
			String prefixSpace="\t";
			
			//String ponitryaginFunctionName = fileName.replace(".m", "");
			String ponitryaginFunctionName = AbstractImporter.overwriteExtensionIfEnabled(fileName,"",true);
			int indexOfLastSep = ponitryaginFunctionName.lastIndexOf(File.separator);
			if(indexOfLastSep>=0){
				ponitryaginFunctionName=ponitryaginFunctionName.substring(indexOfLastSep+1);
			}
			
			bw.write("function "+ponitryaginFunctionName+"()\n\n");
			
			bw.write(prefixSpace+"clear all\n");
			
			/*T=.1; % finite horizon
			% The first uncertain parameter belongs to [3,13] and the second to [8,10]. 
			theta_min=[3 8]; 
			theta_max=[13 10]; 
			x0=[0.0343 0.0979 0.1922 0.3365 0.3391]'; % starting point*/
			
			bw.write(prefixSpace+"T="+tEnd+"; % finite horizon\n\n");
			if(paramsToPerturb.length>1){
				StringBuffer thetamin=new StringBuffer(prefixSpace+"theta_min=[");
				StringBuffer thetamax=new StringBuffer(prefixSpace+"theta_max=[");
				for(int i=0;i<paramsToPerturb.length;i++){
					//bw.write(prefixSpace+"% The uncertain parameter "+paramsToPerturb[i]+" belongs to ["+lows[i]+","+highs[i]+"];\n");
					thetamin.append(" "+lows[i]);
					thetamax.append(" "+highs[i]);
				}
				thetamin.append(" ];\n");
				thetamax.append(" ];\n");
				bw.write(thetamin.toString());
				bw.write(thetamax.toString());
			}
			else{
				bw.write(prefixSpace+"% The only uncertain parameter ("+paramsToPerturb[0]+") belongs to ["+lows[0]+","+highs[0]+"];\n");
				bw.write(prefixSpace+"theta_min="+lows[0]+";\n");
				bw.write(prefixSpace+"theta_max="+highs[0]+";\n");
			}
			
			bw.write("\n");
			
			//HashMap<String,ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpecies().size());
			bw.write(prefixSpace+"x0=[ ");
			for (ISpecies species : crn.getSpecies()) {
				bw.write(species.getInitialConcentration().doubleValue()+" ");
				//speciesNameToSpecies.put(species.getName(), species);
			}
			bw.write("]'; % starting point\n");
			
			//vcoeff=[1 .2 .5 1 1]; % vector of coefficients of the sum of variables
			bw.write(prefixSpace+"vcoeff=[");
			writeCoefficients(crn, speciesToCoefficient, bw);
			bw.write(" ]; % vector of coefficients of the sum of variables\n");
			
			
			bw.write("\n");
			bw.write(prefixSpace+"tic\n");
			bw.write("\n");
			
			//boolean writeSymbolicJacobian=true;
			if(writeSymbolicJacobian && !innerSymbolicJacobian){
				bw.write(prefixSpace+"% Compute Jacobian\n");
				bw.write(prefixSpace+"initJaco();\n");
			}
			
			
			bw.write("\n");
			bw.write(prefixSpace+"maxStep="+maxStep+";\n");
			bw.write(prefixSpace+"epsilon="+epsilon+";\n");
			bw.write(prefixSpace+"KMAX="+maxNumberOfIterations+";\n");
			bw.write("\n");
			
			bw.write(prefixSpace+"% Calculate the maximum and minimum of sum_i vcoeff(i)*x_i(T)\n");
			bw.write(prefixSpace+"xmax=algo_pointryagin(x0,T,theta_min,theta_max,vcoeff,1,KMAX,epsilon,maxStep);\n");
			bw.write(prefixSpace+"xmin=algo_pointryagin(x0,T,theta_min,theta_max,vcoeff,0,KMAX,epsilon,maxStep);\n");
			bw.write("\n");
			bw.write(prefixSpace+"tComp = toc;\n");
			bw.write(prefixSpace+"disp(['computation time of reachable set: ',num2str(tComp)]);\n");
			bw.write("\n");
			bw.write(prefixSpace+"disp(['The output interval is [',num2str(sum(xmin.*vcoeff)),', ',num2str(sum(xmax.*vcoeff)),']']);\n");
			bw.write("\n");
			
			bw.write("end\n\n");
			
			writeDriftsAndJacobian(crn, out, msgDialogShower, parameterToPerturbToItsPosition, speciesNameToSpecies, bw,prefixSpace,writeSymbolicJacobian/*,speciesIdToSpecies*/);
			
			//write Pontryagin
			InputStream is = getClass().getResourceAsStream("pointryaginScript.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line=br.readLine();
			while(line!=null){
				bw.write(line);
				bw.write("\n");
				line=br.readLine();
			}
			br.close();
			
			bw.write("\n\n");
			
			
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printPontryaginToMatlabFIle, exception raised while writing in the file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
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
			}
		}
		
	}


	public static CRNandPartition computeRNEncoding(ICRN crn, MessageConsoleStream out, BufferedWriter bwOut,
			IPartition partition,boolean print) {
		return computeRNEncoding(crn, out, bwOut,partition,crn.getUserDefinedPartition(),print);
	}
	
	public static CRNandPartition computeRNEncoding(ICRN crn, MessageConsoleStream out, BufferedWriter bwOut,
			IPartition partition, ArrayList<HashSet<ISpecies>> userPartition,boolean print) {
		return computeRNEncoding(crn, out, bwOut, true,partition,userPartition,print);
	}
	
	public static CRNandPartition computeRNEncoding(ICRN crn, MessageConsoleStream out, BufferedWriter bwOut,
			/*HashMap<String, ISpecies> speciesNameToSpecies,*/boolean collapseReactions,IPartition partition,boolean print) {
		return computeRNEncoding(crn, out, bwOut, collapseReactions, partition,crn.getUserDefinedPartition(),print);
	}
	
	public static CRNandPartition computeRNEncoding(ICRN crn, MessageConsoleStream out, BufferedWriter bwOut,
			/*HashMap<String, ISpecies> speciesNameToSpecies,*/boolean collapseReactions,IPartition partition, ArrayList<HashSet<ISpecies>> userPartition,boolean print) {
		boolean computeRNEncoding=true;
		ArrayList<ICRNReaction> rnReactions = new ArrayList<>(crn.getReactions().size());
		if(print) {
			CRNReducerCommandLine.print(out, bwOut, " ( converting the ODEs in reaction network form ... ");
		}
		String errorMessage="";
		ISpecies I = new Species(Species.I_SPECIESNAME, null, crn.getSpecies().size(), BigDecimal.ONE, "1",false);
		
		ICRN polynomialCRN = new CRN(crn.getName(), crn.getSymbolicParameters(), crn.getConstraints(), crn.getParameters(), crn.getMath(), out, bwOut);
		HashMap<String, ISpecies> speciesNameToSpecies=new HashMap<String, ISpecies>(crn.getSpecies().size());
		//HashMap<ISpecies, ISpecies> oldSpeciesToNewSpecies=new HashMap<ISpecies, ISpecies>(crn.getSpecies().size());
		for(ISpecies species : crn.getSpecies()){
			/*
			ISpecies newSpecies=species.cloneWithoutReactions();
			oldSpeciesToNewSpecies.put(species, newSpecies);
			speciesNameToSpecies.put(newSpecies.getName(), newSpecies);
			polynomialCRN.addSpecies(newSpecies);
			*/
			speciesNameToSpecies.put(species.getName(), species);
			polynomialCRN.addSpecies(species);
		}
		
		//polynomialCRN.addSpecies(I);
		boolean usedI = false;
		
		for(ICRNReaction reaction : crn.getReactions()){
			if(!reaction.hasArbitraryKinetics()) {
				rnReactions.add(reaction);
			}
			//If the reaction is X -> X+X, arbitrary drift
			else if(reaction.isODELike()){
				try{
					ISpecies speciesOfODE = reaction.getReagents().getFirstReagent(); //oldSpeciesToNewSpecies.get(reaction.getReagents().getFirstReagent());
					boolean anyReactionsUsedI = GUICRNImporter.computeRNEncoding((CRNReactionArbitraryGUI)reaction,speciesOfODE,speciesNameToSpecies,crn.getMath(),I,rnReactions);
					usedI = usedI || anyReactionsUsedI;
				}
				catch(UnsupportedReactionNetworkEncodingException e){
					computeRNEncoding=false;
					break;
				} catch (IOException e) {
					computeRNEncoding=false;
					break;
				}
			}
			else{
				//computeRNEncoding=false;
				//break;
				try{
					boolean anyReactionsUsedI = GUICRNImporter.computeRNEncodingOfArbitraryReaction((CRNReactionArbitraryGUI)reaction,speciesNameToSpecies,crn.getMath(),I,rnReactions/*,oldSpeciesToNewSpecies*/);
					usedI = usedI || anyReactionsUsedI;
				}
				catch(UnsupportedReactionNetworkEncodingException e){
					computeRNEncoding=false;
					errorMessage=e.getMessage();
					break;
				} catch (IOException e) {
					computeRNEncoding=false;
					errorMessage=e.getMessage();
					break;
				}
				
			}
		}
		
		if(usedI) {
			polynomialCRN.addSpecies(I);
		}
		
		if(!computeRNEncoding){
			//CRNReducerCommandLine.print(out,bwOut, "\n The model cannot be encoded as a mass action reaction network.\n I ignore this command");
			if(print) {
				CRNReducerCommandLine.println(out,bwOut, "). ");
			}
			CRNReducerCommandLine.print(out,bwOut, "\tThe model cannot be encoded as a mass action reaction network:\n  "+errorMessage);
			return null;
		}
		else{
			if(collapseReactions){
				CRN.collapseAndCombineAndAddReactions(polynomialCRN, rnReactions, out, bwOut);
			}
			else{
				CRN.addReactions(polynomialCRN, rnReactions);
			}
			
			
			
			/////////////
			//ArrayList<HashSet<ISpecies>> userPartition = crn.getUserDefinedPartition();
			if(userPartition!=null&&userPartition.size()>0){
				/*
				ArrayList<HashSet<ISpecies>> newUserPartition=new ArrayList<HashSet<ISpecies>>(userPartition.size());
				for (HashSet<ISpecies> hashSet : userPartition) {
					HashSet<ISpecies> newHashSet=new HashSet<>(hashSet.size());
					newUserPartition.add(newHashSet);
					for (ISpecies species : hashSet) {
						newHashSet.add(oldSpeciesToNewSpecies.get(species));
					}
				}
				
				if(usedI) {
					HashSet<ISpecies> newHashSet=new HashSet<>(1);
					newUserPartition.add(newHashSet);
					newHashSet.add(I);
				}
				polynomialCRN.setUserDefinedPartition(newUserPartition);
				*/
				
				polynomialCRN.setUserDefinedPartition(userPartition);
			}



			IPartition newInitial = new Partition(polynomialCRN.getSpecies().size());
			if(partition!=null){
				IBlock current = partition.getFirstBlock();
				while(current!=null){
					IBlock newCurrent=new Block();
					newInitial.add(newCurrent);
					for (ISpecies species : current.getSpecies()) {
						newCurrent.addSpecies(species);  //newCurrent.addSpecies(oldSpeciesToNewSpecies.get(species));
					}
					current=current.getNext();
				}
				if(usedI) {
					IBlock blockOfI = new Block();
					newInitial.add(blockOfI);
					blockOfI.addSpecies(I);
				}
			}
			else{
				IBlock block = new Block();
				newInitial.add(block);
				for (ISpecies species : polynomialCRN.getSpecies()) {
					block.addSpecies(species);
				}
				if(usedI) {
					IBlock blockOfI = new Block();
					newInitial.add(blockOfI);
					blockOfI.addSpecies(I);
				}
			}
			/////////////
			
			
			if(print) {
				CRNReducerCommandLine.print(out, bwOut, "completed ) ...");
			}
			
			return new CRNandPartition(polynomialCRN, newInitial);	
		}
	}


	public static void writeCoefficients(ICRN crn, HashMap<String, Double> speciesToCoefficient, BufferedWriter bw)
			throws IOException {
		for (ISpecies species : crn.getSpecies()) {
			Double val = speciesToCoefficient.get(species.getName());
			if(val==null){
				bw.write(" 0");
			}
			else{
				bw.write(" "+val);
			}
		}
	}


	private HashMap<ISpecies, StringBuilder> writeDriftsAndJacobian(ICRN crn, MessageConsoleStream out, IMessageDialogShower msgDialogShower,
			HashMap<String, Integer> parameterToPerturbToItsPosition, HashMap<String, ISpecies> speciesNameToSpecies,
			BufferedWriter bw, String prefixSpace,boolean writeSymbolicJacobian/*,ISpecies[] speciesIdToSpecies*/) throws IOException, UnsupportedFormatException {
		//write ODE
		bw.write("function dy = ode(t,y,theta,int)\n");
		bw.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
		bw.write("%\n");
		bw.write("% Modification of the ODE system of "+crn.getName()+"\n");
		bw.write("% Used by ODE solvers\n");
		bw.write("%\n");
		bw.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n\n");
		bw.write(prefixSpace+"% We want p=theta(t) for each uncertain parameter p.\n");
		bw.write(prefixSpace+"% However, theta is a vector and its value in t may not be defined.\n");
		bw.write(prefixSpace+"% Hence, we set each uncertain parameter as the nearest value of theta(t).\n");
		//write ODE: First write the parameters
		writeUncertainParams(crn, bw, prefixSpace,parameterToPerturbToItsPosition,UNCERTAINPARAMETERSTYPE.ODE,false);
					
		//write ODE: Write the drifts
		int idIncrement=1;
		HashMap<ISpecies, StringBuilder> speciesToExpandedDriftSB = MatlabODEsImporter.writeODEFunction(idIncrement,crn, bw, speciesNameToSpecies,"\t",false,false,false,true,true,null,"ode");
		
		bw.write("\n\n");
		
		if(writeSymbolicJacobian){
			if(innerSymbolicJacobian){
//				bw.write("function J=myjacobian(y,theta)\n");
//				writeUncertainParams(crn, bw, prefixSpace,parameterToPerturbToItsPosition,UNCERTAINPARAMETERSTYPE.JACOBIAN,true);
//				bw.write("\n");
//				MatlabODEsImporter.writeSymjaJacobianFunction(crn, out, msgDialogShower, bw, null, false, false,false,false,prefixSpace);
//				bw.write(prefixSpace+"J=J';\n");
//				bw.write("end\n");
				throw new UnsupportedFormatException("Symja 'symbolic Jacobian' support has been removed");
			}
			else{
				//write symbolic Jacobian
				try {
					MatlabODEsImporter.writeInitJaco(crn, out, msgDialogShower, bw,prefixSpace, speciesNameToSpecies);
				} catch (ParseException e) {
					throw new UnsupportedFormatException(e.getMessage());
				}

				bw.write("function J=myjacobian(y,theta)\n");
				bw.write(prefixSpace+"global jaco;\n\n");
				writeUncertainParams(crn, bw, prefixSpace,parameterToPerturbToItsPosition,UNCERTAINPARAMETERSTYPE.JACOBIAN,false);
				bw.write("\n");
				MatlabODEsImporter.writeSymbolicJacobianFunction(crn, out, msgDialogShower, bw, null, false, false,false,false,prefixSpace);
				bw.write(prefixSpace+"J=J';\n");
				bw.write("end\n");
			}
		}
		else{
			//write Jacobian
			bw.write("function J=myjacobian(y,theta)\n");
			writeUncertainParams(crn, bw, prefixSpace,parameterToPerturbToItsPosition,UNCERTAINPARAMETERSTYPE.JACOBIAN,false);
			bw.write("\n");
			MatlabODEsImporter.writeJacobianFunction(crn, out, msgDialogShower, bw, null, false, false,false,false,prefixSpace/*,speciesIdToSpecies*/);
			bw.write(prefixSpace+"J=J';\n");
			bw.write("end\n");
		}
		
		bw.write("\n\n");
		
		//write ODE2
		bw.write("function dy = ode2(y,v)\n");
		bw.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
		bw.write("%\n");
		bw.write("% Modification of the ODE system of "+crn.getName()+"\n");
		bw.write("% Used by theta maximization function\n");
		bw.write("%\n");
		bw.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n\n");
		
		//write ODE2: First write the parameters
		writeUncertainParams(crn, bw, prefixSpace,parameterToPerturbToItsPosition,UNCERTAINPARAMETERSTYPE.ODETWO,false);
					
		//write ODE2: Write the drifts
		//MatlabODEsImporter.writeODEFunction(crn, bw, speciesNameToSpecies,"\t",false,false,false,false);
		writeODEFunction(speciesToExpandedDriftSB,crn, bw, speciesNameToSpecies,"\t",false,false,false,false,"");	
		
		bw.write("\n\n\n\n");
		
		return speciesToExpandedDriftSB;
		
		
		/*
		 * //write ODE
			bw.write("function dy = ode(t,y,theta,int)\n");
			bw.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
			bw.write("%\n");
			bw.write("% Modification of the ODE system of "+crn.getName()+"\n");
			bw.write("% Used by ODE solvers\n");
			bw.write("%\n");
			bw.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n\n");
			bw.write(prefixSpace+"% We want p=theta(t) for each uncertain parameter p.\n");
			bw.write(prefixSpace+"% However, theta is a vector and its value in t may not be defined.\n");
			bw.write(prefixSpace+"% Hence, we set each uncertain parameter as the nearest value of theta(t).\n");
			//write ODE: First write the parameters
			writeUncertainParams(crn, bw, prefixSpace,parameterToPerturbToItsPosition,UNCERTAINPARAMETERSTYPE.ODE);
						
			//write ODE: Write the drifts
			HashMap<ISpecies, StringBuilder> speciesToExpandedDriftSB = MatlabODEsImporter.writeODEFunction(crn, bw, speciesNameToSpecies,"\t",false,false,false,true);
			
			bw.write("\n\n");
			
			//write Jacobian
			bw.write("function J=jacobian(y,theta)\n");
			writeUncertainParams(crn, bw, prefixSpace,parameterToPerturbToItsPosition,UNCERTAINPARAMETERSTYPE.JACOBIAN);
			bw.write("\n");
			MatlabODEsImporter.writeJacobianFunction(crn, out, msgDialogShower, bw, null, false, false,false,false,prefixSpace);
			bw.write(prefixSpace+"J=J';\n");
			bw.write("end\n");
			
			bw.write("\n\n");
			
			//write ODE2
			bw.write("function dy = ode2(y,v)\n");
			bw.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
			bw.write("%\n");
			bw.write("% Modification of the ODE system of "+crn.getName()+"\n");
			bw.write("% Used by theta maximization function\n");
			bw.write("%\n");
			bw.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n\n");
			
			//write ODE2: First write the parameters
			writeUncertainParams(crn, bw, prefixSpace,parameterToPerturbToItsPosition,UNCERTAINPARAMETERSTYPE.ODETWO);
						
			//write ODE2: Write the drifts
			//MatlabODEsImporter.writeODEFunction(crn, bw, speciesNameToSpecies,"\t",false,false,false,false);
			writeODEFunction(speciesToExpandedDriftSB,crn, bw, speciesNameToSpecies,"\t",false,false,false,false,"");	
		 */
	}
	
	public static final String FILEWITHMATLABROOT_ARCH="pathOfMATLABROOT_ARCH.txt";// /Applications/Eclipse-SDK-4.7.1a.app/Contents/MacOS/pathOfMATLABROOT_ARCH.txt
	public static final String MATLABROOT_ARCH_LOCATEMESSAGE = "Locate matlabroot/bin/<arch>, where:\n  - matlabroot is Matlab's installation folder\n  - <arch> is the computer architecture:\n    - win64 for Windows 64\n    - maci64 for Mac\n    - glnxa64 for Linux 64";
	//public static final String MATLABROOT_ARCH_LOCATEMESSAGE = "Locate \"matlabroot/bin/<arch>\", where matlabroot is Matlab's installation folder and arch is win64, -maci64, or -glnxa64)";
	
	
	public static final String FILEWITHCORA_2016LOCATION="pathOfCORA_2016.txt";
	public static final String CORADOWNLOADPATHSHORT = "CORA_2016.zip";
	public static final String CORADOWNLOADPATH = "http://www.i6.in.tum.de/pub/Main/SoftwareCORA/CORA_2016.zip";
	public static final String CORADOWNLOADMESSAGE = "Please download CORA_2016 from:";
	public static final String CORALOCATEMESSAGE = "Decompress the archive and locate the folder CORA_2016:";
	
	/*public MatlabODEPontryaginExporter(String fileName,MessageConsoleStream out,BufferedWriter bwOut,IMessageDialogShower msgDialogShower) {
		super(fileName,out,bwOut,msgDialogShower);
	}	*/
	
	//@SuppressWarnings("unused")
	public void printPontryaginPolygonToMatlabFIle(ICRN crn, String name, boolean verbose,
			MessageConsoleStream out, BufferedWriter bwOut, IMessageDialogShower msgDialogShower, double tEnd,
			HashMap<String, Integer> parameterToPerturbToItsPosition, String[] paramsToPerturb, double[] lows, double[] highs, Terminator terminator, boolean CORA, String CORAPATH,double sl1, double sl2, String firstSpecies, String secondSpecies,boolean writeSymbolicJacobian)  throws UnsupportedFormatException{
		String fileName = name;
		if(crn.isSymbolic()){
			CRNReducerCommandLine.printWarning(out,bwOut, true, msgDialogShower, "The Pontryagin script can be created only for mass-action CRNs without symbolic parameters.", DialogType.Error);
			return;
		}
		
		
		//ISpecies[] speciesIDToSpecies = new ISpecies[crn.getSpecies().size()];
		HashMap<String,ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpecies().size());
		//int s=0;
		for (ISpecies species : crn.getSpecies()) {
			speciesNameToSpecies.put(species.getName(), species);
			//speciesIDToSpecies[s]=species;
			//s++;
		}
		
		ISpecies first = speciesNameToSpecies.get(firstSpecies);
		ISpecies second = speciesNameToSpecies.get(secondSpecies);
		if(first==null||second==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in printPontryaginToMatlabFIle: could not find the species: "+firstSpecies+ " or " +secondSpecies);
			return;
		}
		
		if(!writeSymbolicJacobian){
			if(!crn.isMassAction()){
				CRNandPartition crnAndSpecies =computeRNEncoding(crn, out, bwOut, null,true);
				if(crnAndSpecies==null){
					return;
				}
				crn = crnAndSpecies.getCRN();
				if(crn==null){
					return;
				}
				
				speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpecies().size());
				for (ISpecies species : crn.getSpecies()) {
					speciesNameToSpecies.put(species.getName(), species);
				}
				
				first = speciesNameToSpecies.get(firstSpecies);
				second = speciesNameToSpecies.get(secondSpecies);
			}
		}
		
		fileName=AbstractImporter.overwriteExtensionIfEnabled(fileName,".m");
		if(verbose){
			CRNReducerCommandLine.print(out,bwOut,"Writing the Pontryagin script in file "+fileName);
		}
		
		AbstractImporter.createParentDirectories(fileName);
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printPontryaginToMatlabFIle, exception raised while creating the filewriter for file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		
		try {
			
			/*
			if(preambleCommentLines!=null && preambleCommentLines.size()>0){
				for (String comment : preambleCommentLines) {
					bw.write("%"+comment+"\n");
				}
			}
			bw.write("\n\n");
			*/
			
					
			//bw.write("\n\n");
			
			String modelName = " ";
			if(crn.getName()!=null&&crn.getName()!=""){
				modelName = " "+GUICRNImporter.getModelName(crn.getName())+" ";
			}
			
			String message="";
			if(paramsToPerturb.length>1){
				message="% Reachability analysis of the system"+modelName+"with "+paramsToPerturb.length+" uncertain parameters %";
			} 
			else{
				message="% Reachability analysis of the system"+modelName+"with 1 uncertain parameter %";
			}
			StringBuilder sb = new StringBuilder(message.length());
			for(int i=0;i<message.length();i++){
				sb.append('%');
			}
			String caption=sb.toString();
			bw.write(caption+"\n");
			bw.write(message+"\n");
			//bw.write("% Parameters that are common for CORA and Pontryagin's algo are set here.\n");
			bw.write(caption+"\n");
			
			bw.write("\n");
			
			bw.write("% Automatically generated from "+crn.getName()+".\n");
			bw.write("% Number of species: "+crn.getSpecies().size()+".\n");
			bw.write("% Number of reactions: "+crn.getReactions().size()+".\n");
			bw.write("% Slopes: "+sl1+", "+sl2+".\n");
			bw.write("% Perturbed parameters: ");
			for(int i=0;i<paramsToPerturb.length;i++){
				bw.write(paramsToPerturb[i]+" in ["+lows[i]+","+highs[i]+"]");
				if(i<paramsToPerturb.length-1){
					bw.write(", ");
				}
			}
			bw.write("\n");
			bw.write("\n% Correspondence with original names:\n");
			int incr=1;
			for (ISpecies species : crn.getSpecies()) {
				bw.write("%     y(" +(species.getID()+incr)+") = " + ((species.getOriginalName()==null)?species.getName():species.getOriginalName())+"\n");
			}
			bw.write("\n\n");
			
			String prefixSpace="\t";
			
			/*
			function reach_set_replicatedBindingSite2Asymm_1uncertain()
			
			%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
			% Reachability analysis of the system replicatedBindingSite2Asymm with one 
			%   uncertain parameter.
			% Parameters that are common for CORA and Pontryagin's algo are set here.
			%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
			*/
			
			//String ponitryaginFunctionName = fileName.replace(".m", "");
			String ponitryaginFunctionName = AbstractImporter.overwriteExtensionIfEnabled(fileName,"",true);
			String figureNames=ponitryaginFunctionName;
			int indexOfLastSep = ponitryaginFunctionName.lastIndexOf(File.separator);
			if(indexOfLastSep>=0){
				ponitryaginFunctionName=ponitryaginFunctionName.substring(indexOfLastSep+1);
			}
			
			bw.write("function "+ponitryaginFunctionName+"()\n\n");
			
			bw.write(prefixSpace+"clear all\n");
			
			if(writeSymbolicJacobian&&!innerSymbolicJacobian){
				bw.write("\n"+prefixSpace+"% Compute Jacobian\n");
				bw.write(prefixSpace+"initJaco();\n\n");
			}
			
			/*T=.1; % finite horizon
			% The first uncertain parameter belongs to [3,13] and the second to [8,10]. 
			theta_min=[3 8]; 
			theta_max=[13 10]; 
			x0=[0.0343 0.0979 0.1922 0.3365 0.3391]'; % starting point*/
			
			bw.write(prefixSpace+"T="+tEnd+"; % finite horizon\n\n");
			if(paramsToPerturb.length>1){
				StringBuffer thetamin=new StringBuffer(prefixSpace+"theta_min=[");
				StringBuffer thetamax=new StringBuffer(prefixSpace+"theta_max=[");
				for(int i=0;i<paramsToPerturb.length;i++){
					bw.write(prefixSpace+"% The uncertain parameter "+paramsToPerturb[i]+" belongs to ["+lows[i]+","+highs[i]+"];\n");
					thetamin.append(" "+lows[i]);
					thetamax.append(" "+highs[i]);
				}
				thetamin.append(" ];\n");
				thetamax.append(" ];\n");
				bw.write(thetamin.toString());
				bw.write(thetamax.toString());
			}
			else{
				bw.write(prefixSpace+"% The only uncertain parameter ("+paramsToPerturb[0]+") belongs to ["+lows[0]+","+highs[0]+"];\n");
				bw.write(prefixSpace+"theta_min="+lows[0]+";\n");
				bw.write(prefixSpace+"theta_max="+highs[0]+";\n");
			}
			
			bw.write("\n");
			
			String idOfFirst=""+(first.getID()+1);
			String idOfSecond=""+(second.getID()+1);
			
			String vectorOfTheTwoSpeciesToPerturb = "[ "+ idOfFirst +" "+ idOfSecond +" ]";
			bw.write(prefixSpace+"% We represent in the x axis variable "+idOfFirst+" ("+((first.getOriginalName()==null)?first.getName():first.getOriginalName())+") and in the y axis variable "+idOfSecond+" ("+((second.getOriginalName()==null)?second.getName():second.getOriginalName())+")\n");
			bw.write(prefixSpace+"species = "+vectorOfTheTwoSpeciesToPerturb+";\n");
			
			bw.write("\n");
			
			//HashMap<String,ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpecies().size());
			bw.write(prefixSpace+"x0=[ ");
			for (ISpecies species : crn.getSpecies()) {
				bw.write(species.getInitialConcentration().doubleValue()+" ");
				//speciesNameToSpecies.put(species.getName(), species);
			}
			bw.write("]'; % starting point\n");
			
			
			bw.write("\n");
			bw.write(prefixSpace+"figure;\n");
			bw.write(prefixSpace+"hold on;\n");
			bw.write("\n");
			

			//http://www6.in.tum.de/pub/Main/SoftwareCORA/CORA_2016.zip
			if(CORA){
				bw.write(prefixSpace+"% compute the reachable set using CORA\n");
				//bw.write(prefixSpace+"cd CORA/\n");
				//bw.write(prefixSpace+"CORA_reach_set(x0,T,theta_min,theta_max,'replicatedBindingSite2Asymm_1uncertain');\n");
				bw.write(prefixSpace+"CORA_reach_set(x0,species,T,theta_min,theta_max);\n");
			}
			
			/*
			% compute the reachable set using Pontryagin's Maximum Principle algo
			cd ../Pontryagin/
			Pointry_reach_set(x0,T,theta_min,theta_max,.5,4,'replicatedBindingSite2Asymm_1uncertain');
			 */
			
			//String vectorOfTheTwoSpeciesToPerturb = "[ "+ (first.getID()+1) +" "+ (second.getID()+1) +" ]";
			
			bw.write(prefixSpace+"% compute the reachable set using Pontryagin's Maximum Principle algorithm.\n");
			//bw.write(prefixSpace+"Pointry_reach_set(x0,T,theta_min,theta_max,.5,4,'replicatedBindingSite2Asymm_1uncertain');");
			bw.write(prefixSpace+"Pointry_reach_set(x0,flip(species),T,theta_min,theta_max,"+sl1+","+sl2+");");
			
			
			bw.write("\n\n");
			bw.write(prefixSpace+"% save figure in figs folder\n");
			//cd ../figs/
			bw.write(prefixSpace+"savefig('"+figureNames+".fig');\n");
			bw.write(prefixSpace+"saveas(gcf,'"+figureNames+",pdf')\n\n");
			
			bw.write("end\n\n");
			
			HashMap<ISpecies, StringBuilder> speciesToExpandedDriftSB = writeDriftsAndJacobian(crn, out, msgDialogShower, parameterToPerturbToItsPosition, speciesNameToSpecies, bw, prefixSpace,writeSymbolicJacobian/*,speciesIDToSpecies*/);
			
			if(CORA){
				
				bw.write("\n\n");
				
				//write ODE_cora
				bw.write("function dy = ode_cora(t,y,v)\n");
				bw.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
				bw.write("%\n");
				bw.write("% Modification of the ODE system of "+crn.getName()+"\n");
				bw.write("% Used by ODE solvers in CORA\n");
				bw.write("%\n");
				bw.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n\n");

				//write ODE_cora: First write the parameters
				writeUncertainParams(crn, bw, prefixSpace,parameterToPerturbToItsPosition,UNCERTAINPARAMETERSTYPE.ODECORA,false);

				//write ODE_cora: Write the drifts
				//MatlabODEsImporter.writeODEFunction(crn, bw, speciesNameToSpecies,"\t",false,false,false,false);
				writeODEFunction(speciesToExpandedDriftSB,crn, bw, speciesNameToSpecies,"\t",false,false,false,false,",1");
			}
			
			
			bw.write("\n\n\n\n");
			
			bw.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
			bw.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
			bw.write("%                                                 %\n");
			bw.write("% Model independent script for Pontryagin method %\n");
			bw.write("%                                                 %\n");
			bw.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
			bw.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
			bw.write("\n\n");
			
			//write Pontryagin
			InputStream is = getClass().getResourceAsStream("Pointry_reach_set.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line=br.readLine();
			while(line!=null){
				bw.write(line);
				bw.write("\n");
				line=br.readLine();
			}
			br.close();
			
			bw.write("\n\n");
			
			//write Pontryagin algo
			is = getClass().getResourceAsStream("algo_pointryagin.txt");
			br = new BufferedReader(new InputStreamReader(is));
			line=br.readLine();
			while(line!=null){
				bw.write(line);
				bw.write("\n");
				line=br.readLine();
			}
			br.close();
			
			if(CORA){
				bw.write("\n\n\n\n");

				bw.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
				bw.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
				bw.write("%                                                            %\n");
				bw.write("% Model independent script to compare our approach with CORA %\n");
				bw.write("%                                                            %\n");
				bw.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
				bw.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
				bw.write("\n\n");


				//write CORA interface
				bw.write("function tComp=CORA_reach_set(x0_,proyDim,T,theta_min,theta_max)\n");
				bw.write("\n");
				bw.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
				bw.write("% Reachability analysis for systems with uncertain parameters using CORA\n");
				bw.write("%\n");
				bw.write("% Inputs:\n");
				bw.write("%   - x0_:intial state\n");
				bw.write("%   - T: time horizon\n");
				bw.write("%   - theta_min,theta_max: bounds of the unknown parameter.\n");
				bw.write("%\n");
				bw.write("% Modified from the example example_nonlinear_reach_01_tank.m provided by CORA\n");
				bw.write("%\n");
				bw.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
				bw.write("\n");
				bw.write("corapath = '"+CORAPATH+"';\n");
				//bw.write(" corapath = '/Users/andrea/Downloads/CORA_2016_2';\n");

				is = getClass().getResourceAsStream("CORA_reach_set.txt");
				br = new BufferedReader(new InputStreamReader(is));
				line=br.readLine();
				while(line!=null){
					bw.write(line);
					bw.write("\n");
					line=br.readLine();
				}
				br.close();
			}
			
			//CRNReducerCommandLine.println(out,bwOut," completed");
			
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printPontryaginToMatlabFIle, exception raised while writing in the file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
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
			}
		}
		
	}

	private static void writeUncertainParams(ICRN crn, BufferedWriter bw, String prefixSpace,
			HashMap<String, Integer> parameterToPerturbToItsPosition, UNCERTAINPARAMETERSTYPE type, boolean symJaName) throws IOException{
		for (String param : crn.getParameters()) {
			int space = param.indexOf(' ');
			String paramName = param.substring(0, space).trim();
			Integer pos = parameterToPerturbToItsPosition.get(paramName);
			if(symJaName){
				paramName=MatlabODEsImporter.symjaName(paramName);
			}
			if(pos!=null&&pos>=0){
				if(type.equals(UNCERTAINPARAMETERSTYPE.JACOBIAN)){
					bw.write(prefixSpace+paramName +" = theta("+(pos+1)+");\n");
				}
				else if(type.equals(UNCERTAINPARAMETERSTYPE.ODE)){
					bw.write(prefixSpace+paramName +" = interp1(int, theta(:,"+(pos+1)+"), t, 'nearest');\n");
				}
				else{
					//ODETWO or ODECORA
					bw.write(prefixSpace+paramName +" = v("+(pos+1)+");\n");
				}
			}
			else{
				String paramExpr = param.substring(space,param.length()).trim();
				//p=0.5;
				//if(!paramName.equalsIgnoreCase("eps")){
				bw.write(prefixSpace+paramName +" = "+paramExpr+";\n");
				//}
			}
		}
	}
	
	public static void writeODEFunction(HashMap<ISpecies, StringBuilder> speciesToDrift, ICRN crn, BufferedWriter bw, HashMap<String, ISpecies> speciesNameToSpecies,String prefixSpace,
			boolean writeParameters, boolean writeT, boolean writeFunction, boolean initZeros, String secondEntry)
			throws IOException, UnsupportedFormatException {
		MatlabODEsImporter.writeFunctionAndParamsAndInitZeros(crn, bw, prefixSpace, writeParameters, writeT, writeFunction, initZeros);
		int idIncrement=1;
		//HashMap<ISpecies, StringBuilder> speciesToDrift=null;
		if(crn.isMassAction()){
			//speciesToDrift = GUICRNImporter.computeDriftsReplacingSpeciesNames(crn, false, "y", 1);
			//speciesToDrift = GUICRNImporter.computeDrifts(crn,false);
			MatlabODEsImporter.writeDriftsOfMassActionWithSpeciesNameAlreadyReplacedWithY("dy",speciesToDrift, crn, bw, prefixSpace,secondEntry);
		}
		else{
			//speciesToDrift = GUICRNImporter.computeDrifts(crn,false);
			
			MatlabODEsImporter.writeDriftsOfArbitraryODEReplacingSpeciesNamesWithY(idIncrement,"y","dy",speciesToDrift, crn, bw, speciesNameToSpecies,prefixSpace,secondEntry);
		}
		
		bw.write("end\n");
	}
	
	
}
