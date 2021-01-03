package it.imt.erode.cage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.eclipse.ui.console.MessageConsoleStream;
import org.sbml.jsbml.text.parser.ParseException;

import it.imt.erode.auxiliarydatastructures.CRNandPartition;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.DialogType;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.commandline.Terminator;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.importing.AbstractImporter;
import it.imt.erode.importing.GUICRNImporter;
import it.imt.erode.importing.MatlabODEsImporter;
import it.imt.erode.importing.UnsupportedFormatException;
import it.imt.erode.utopic.MatlabODEPontryaginExporter;



public class CreateCAGEScript {

	public static final boolean innerSymbolicJacobian = false;
	
	
	public void printCAGEScriptToMatlabFIle(ICRN crn, String name, boolean verbose,
			MessageConsoleStream out, BufferedWriter bwOut, IMessageDialogShower msgDialogShower, 
			Terminator terminator, boolean writeSymbolicJacobian,
			int compBound,boolean printDES,boolean backward,
			String sourceFileName
			)  throws UnsupportedFormatException{
		printCAGEScriptToMatlabFIle(crn, name, verbose,out,bwOut,msgDialogShower,terminator,writeSymbolicJacobian,compBound,printDES,backward,null);
	}

	//@SuppressWarnings("unused")
	public void printCAGEScriptToMatlabFIle(ICRN crn, String name, boolean verbose,
			MessageConsoleStream out, BufferedWriter bwOut, IMessageDialogShower msgDialogShower, 
			Terminator terminator, boolean writeSymbolicJacobian,
			int compBound,boolean printDES,boolean backward,
			String sourceFileName,String unionFileName
			)  throws UnsupportedFormatException{


		String fileName = name;
		if(crn.isSymbolic()){
			CRNReducerCommandLine.printWarning(out,bwOut, true, msgDialogShower, "The CAGE script can be created only for mass-action CRNs without symbolic parameters.", DialogType.Error);
			return;
		}

		HashMap<String,ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpecies().size());
		for (ISpecies species : crn.getSpecies()) {
			speciesNameToSpecies.put(species.getName(), species);
		}

		//if(!writeSymbolicJacobian){
			if(!crn.isMassAction()){
				//CRNReducerCommandLine.print(out, bwOut, "... (converting the arbitrary CRN in an mass-action CRN ...");
				CRNandPartition crnAndSpecies=MatlabODEPontryaginExporter.computeRNEncoding(crn, out, bwOut, null,true);
				if(crnAndSpecies==null){
					return;
				}
				crn = crnAndSpecies.getCRN();
				if(crn==null){
					return;
				}
			}	
		//}

//		ISpecies[] speciesIdToSpecies=new ISpecies[crn.getSpecies().size()];
//		int s=0;
//		for (ISpecies species : crn.getSpecies()) {
//			speciesIdToSpecies[s]=species;
//			s++;
//		}


		fileName=AbstractImporter.overwriteExtensionIfEnabled(fileName,".m");
		if(verbose){
			CRNReducerCommandLine.print(out,bwOut,"Writing the CAGE script in file "+fileName);
		}

		AbstractImporter.createParentDirectories(fileName);
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printCAGEScriptToMatlabFIle, exception raised while creating the filewriter for file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}


		try {

			String modelName = " ";
			if(crn.getName()!=null&&crn.getName()!=""){
				modelName = " "+GUICRNImporter.getModelName(crn.getName())+" ";
			}

			String message="% CAGE script for the system"+modelName+" %";

			StringBuilder sb = new StringBuilder(message.length());
			for(int i=0;i<message.length();i++){
				sb.append('%');
			}
			String caption=sb.toString();
			bw.write(caption+"\n");
			bw.write(message+"\n");
			bw.write(caption+"\n");

			bw.write("\n");

			bw.write("% Automatically generated from "+crn.getName()+".\n");
			bw.write("% Number of species: "+crn.getSpecies().size()+".\n");
			bw.write("% Number of reactions: "+crn.getReactions().size()+".\n");
			bw.write("\n");
			bw.write("\n");
			bw.write("\n% Correspondence with original names:\n");
			int incr=1;
			for (ISpecies species : crn.getSpecies()) {
				bw.write("%     y(" +(species.getID()+incr)+") = " + ((species.getOriginalName()==null)?species.getName():species.getOriginalName())+"\n");
			}
			bw.write("\n\n");

			String prefixSpace="\t";

			String ponitryaginFunctionName = AbstractImporter.overwriteExtensionIfEnabled(fileName,"",true);
			int indexOfLastSep = ponitryaginFunctionName.lastIndexOf(File.separator);
			if(indexOfLastSep>=0){
				ponitryaginFunctionName=ponitryaginFunctionName.substring(indexOfLastSep+1);
			}

			bw.write("function des="+ponitryaginFunctionName+"()\n\n");	

			InputStream is = getClass().getResourceAsStream("begin.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line=br.readLine();
			while(line!=null){
				bw.write(line);
				bw.write("\n");
				line=br.readLine();
			}
			br.close();

			if(compBound<=0){
				bw.write(prefixSpace+"comp_bound = 0;\n");
			}
			else{
				bw.write(prefixSpace+"comp_bound = "+compBound+";\n");
			}

			bw.write(prefixSpace+"% Set this flag if you want to calculate all BDEs. Instead, set it to\n");
			bw.write(prefixSpace+"% zero if you want to calculate all FDEs\n");
			if(backward){
				bw.write(prefixSpace+"mode_bde = 1;\n");
			}
			else{
				bw.write(prefixSpace+"mode_bde = 0;\n");
			}

			bw.write(prefixSpace+"\n");
			bw.write(prefixSpace+"% Set this flag if you want the DE partitions to be printed\n");
			if(printDES){
				bw.write(prefixSpace+"print_des = 1;\n");
			}
			else{
				bw.write(prefixSpace+"print_des = 0;\n");
			}

			bw.write(prefixSpace+"% This string will be initialized later in the case we want to\n");
			bw.write(prefixSpace+"% calculate emulations\n");
			bw.write(prefixSpace+"crnunion = '';\n");
			bw.write(prefixSpace+"\n");
			
			//boolean writeSymbolicJacobian=true;
			if(writeSymbolicJacobian && !innerSymbolicJacobian){
				bw.write(prefixSpace+"% Compute Jacobian\n");
				bw.write(prefixSpace+"initJaco();\n");
				bw.write(prefixSpace+"\n");
			}
			
			bw.write(prefixSpace+"%Create an instance of ERODE\n");
			bw.write(prefixSpace+"crnreducer = it.imt.erode.commandline.EntryPointForMatlab(false,true);\n");

			bw.write(prefixSpace+"%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
			bw.write(prefixSpace+"%%%%%% LOADING OF THE NETWORK %%%%%%\n");
			bw.write(prefixSpace+"%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");

			bw.write(prefixSpace+"n = javaMethod('loadCRN', crnreducer, '"+sourceFileName+"');\n");        
			bw.write(prefixSpace+"H = javaMethod('computeBB', crnreducer, ones(1,n))';\n");
			bw.write(prefixSpace+"H2 = javaMethod('zip', crnreducer, H)';\n");
			//bw.write(prefixSpace+"A = javaMethod('computeJacobian', crnreducer, H2);\n");
			bw.write(prefixSpace+"A = myjacobian(H2)\n");
			if(unionFileName!=null && !unionFileName.equals("")){
				bw.write(prefixSpace+"crnunion = '"+unionFileName+"';\n");
			}

			is = getClass().getResourceAsStream("algorithm.txt");
			br = new BufferedReader(new InputStreamReader(is));
			line=br.readLine();
			while(line!=null){
				bw.write(line);
				bw.write("\n");
				line=br.readLine();
			}
			br.close();
			bw.write("\n\n");
			
			if(writeSymbolicJacobian){
				if(innerSymbolicJacobian){
//					bw.write("function J=myjacobian(y)\n");
//					//writeUncertainParams(crn, bw, prefixSpace,parameterToPerturbToItsPosition,UNCERTAINPARAMETERSTYPE.JACOBIAN,true);
//					bw.write("\n");
//					MatlabODEsImporter.writeSymjaJacobianFunction(crn, out, msgDialogShower, bw, null, false, false,true,false,prefixSpace);
//					//bw.write(prefixSpace+"J=J';\n");
//					bw.write("end\n");
					throw new UnsupportedFormatException("Symja 'symbolic Jacobian' support has been removed");
				}
				else{
					//write symbolic Jacobian
					try {
						MatlabODEsImporter.writeInitJaco(crn, out, msgDialogShower, bw,prefixSpace, speciesNameToSpecies);
					} catch (ParseException e) {
						throw new UnsupportedFormatException(e.getMessage());
					}

					bw.write("function J=myjacobian(y)\n");
					bw.write(prefixSpace+"global jaco;\n\n");
					//writeUncertainParams(crn, bw, prefixSpace,parameterToPerturbToItsPosition,UNCERTAINPARAMETERSTYPE.JACOBIAN,false);
					bw.write("\n");
					MatlabODEsImporter.writeSymbolicJacobianFunction(crn, out, msgDialogShower, bw, null, false, false,true,false,prefixSpace);
					//bw.write(prefixSpace+"J=J';\n");
					bw.write("end\n");
				}
			}
			else{
				//write Jacobian
				bw.write("function J=myjacobian(y)\n");
				//writeUncertainParams(crn, bw, prefixSpace,parameterToPerturbToItsPosition,UNCERTAINPARAMETERSTYPE.JACOBIAN,false);
				bw.write("\n");
				MatlabODEsImporter.writeJacobianFunction(crn, out, msgDialogShower, bw, null, false, false,true,false,prefixSpace/*,speciesIdToSpecies*/);
				//bw.write(prefixSpace+"J=J';\n");
				bw.write("end\n");
			}

			

		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printCAGEScriptToMatlabFIle, exception raised while writing in the file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		finally{
			if(verbose){
				CRNReducerCommandLine.println(out,bwOut,"Writing the CAGE script in file "+fileName+" completed");
			}
			try {
				bw.flush();
				bw.close();
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in printCAGEScriptToMatlabFIle, exception raised while closing the bufferedwriter of the file: "+fileName);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}

	}
}