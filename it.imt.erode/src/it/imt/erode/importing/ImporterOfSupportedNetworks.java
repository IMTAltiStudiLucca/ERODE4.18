package it.imt.erode.importing;

import java.io.BufferedWriter;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.importing.chemkin.ChemKinImporter;
import it.imt.erode.importing.cnf.CNFImporter;
import it.imt.erode.importing.csv.CSVMatrixAsLinearSystemImporter;
import it.imt.erode.importing.csv.CompactCSVMatrixImporter;
import it.imt.erode.importing.konect.KonectNetworksImporter;
import it.imt.erode.importing.sbml.FluxBalanceAnalysisModel;
import it.imt.erode.importing.sbml.SBMLImporter;
import it.imt.erode.importing.spaceex.SpaceExImporter;
import it.imt.erode.importing.astrochemistry.OSUImporter;
import it.imt.erode.importing.astrochemistry.UMISTImporter;

public class ImporterOfSupportedNetworks {
	public AbstractImporter importSupportedNetwork(String fileName, boolean printInfo, boolean printCRN, SupportedFormats format, boolean print,MessageConsoleStream out,BufferedWriter bwOut,IMessageDialogShower msgDialogShower,boolean compactBNGNames,boolean writeFileWithSpeciesNameCorrespondences) throws UnsupportedFormatException, IOException, XMLStreamException, FluxBalanceAnalysisModel {
		return importSupportedNetwork(fileName, printInfo, printCRN, format, print, null,out,bwOut,msgDialogShower,compactBNGNames,writeFileWithSpeciesNameCorrespondences);
	}
	
	public AbstractImporter importSupportedNetwork(String fileName, boolean printInfo, boolean printCRN, SupportedFormats format, boolean print, String[] optionalParameters,MessageConsoleStream out,BufferedWriter bwOut,IMessageDialogShower msgDialogShower,boolean compactBNGNames,boolean writeFileWithSpeciesNameCorrespondences) throws UnsupportedFormatException, IOException, XMLStreamException {
		AbstractImporter importer;
		
		long beginImp = System.currentTimeMillis();
		
		//if(fileName.endsWith(".net")){
		if(format.equals(SupportedFormats.BNG)){
			//System.out,bwOut.println("We are goind to load a BioNetGen CRN (.net)");
			importer = new BioNetGenImporter(fileName,out,bwOut,msgDialogShower,false);
			((BioNetGenImporter)importer).importBioNetGenNetwork(printInfo,printCRN,print,compactBNGNames,writeFileWithSpeciesNameCorrespondences);
		}
		else if(format.equals(SupportedFormats.SBML)){
			//System.out,bwOut.println("We are goind to load a BioNetGen CRN (.net)");
			boolean forceMA=false;
			if(optionalParameters!=null && optionalParameters.length>0) {
				String v = optionalParameters[0];
				if(v!=null) {
					forceMA=Boolean.valueOf(v);
				}
			}
			importer = new SBMLImporter(fileName,out,bwOut,msgDialogShower);
			((SBMLImporter)importer).importSBMLNetwork(printInfo, printCRN, print,forceMA);
		}
		else if(format.equals(SupportedFormats.CompactBNG)){
			//System.out,bwOut.println("We are goind to load a BioNetGen CRN (.net)");
			importer = new BioNetGenImporter(fileName,out,bwOut,msgDialogShower,true);
			((BioNetGenImporter)importer).importBioNetGenNetwork(printInfo,printCRN,print,compactBNGNames,writeFileWithSpeciesNameCorrespondences);
		}
		else if(format.equals(SupportedFormats.BioLayout)){
			importer = new BioLayoutImporter(fileName,out,bwOut,msgDialogShower);
			((BioLayoutImporter)importer).importBioLayoutSTG(printInfo,printCRN,print);
		}
		else if(format.equals(SupportedFormats.SpaceEx)){
			importer = new SpaceExImporter(fileName,out,bwOut,msgDialogShower);
			((SpaceExImporter)importer).importSpaceExXML(printInfo,printCRN,print,optionalParameters==null?null:optionalParameters[0],optionalParameters==null?null:optionalParameters[1]);
		}
		else if(format.equals(SupportedFormats.BoolCubeSBML)){
			//System.out,bwOut.println("We are goind to load a BoolCube ODE systen");
			importer = new BoolCubeImporter(fileName,out,bwOut,msgDialogShower);
			((BoolCubeImporter)importer).importBoolCubeSBMLNetwork(printInfo, printCRN, print);
		}
		else if(format.equals(SupportedFormats.MatlabPolynomialODEs)){
			//System.out,bwOut.println("We are goind to a systems of ODEs from a Matlab specification");
			importer = new MatlabODEsImporter(fileName,out,bwOut,msgDialogShower);
			((MatlabODEsImporter)importer).importMatlabODEs(printInfo, printCRN, print,true);
		}
		else if(format.equals(SupportedFormats.MatlabArbitraryODEs)){
			//System.out,bwOut.println("We are goind to a systems of ODEs from a Matlab specification");
			importer = new MatlabODEsImporter(fileName,out,bwOut,msgDialogShower);
			((MatlabODEsImporter)importer).importMatlabODEs(printInfo, printCRN, print,false);
		}
		else if(format.equals(SupportedFormats.PALOMAMomentClosure)){
			//System.out,bwOut.println("We are goind to a systems of ODEs from a Matlab specification");
			importer = new PALOMAMomentClosureImporter(fileName,out,bwOut,msgDialogShower);
			((PALOMAMomentClosureImporter)importer).importPalomaMomentClosures(printInfo, printCRN, print,optionalParameters);
		}
		else if(format.equals(SupportedFormats.CRN)){
			//if(fileName.endsWith(".crn")){
			//System.out,bwOut.println("We are goind to load an explicit CRN (.crn)");
			importer = new CRNImporter(fileName,out,bwOut,msgDialogShower);
			((CRNImporter)importer).importCRNNetwork(printInfo, printCRN,print);
		}
		else if(format.equals(SupportedFormats.KonectDoNotEnsureUndirGraph)){
			importer = new KonectNetworksImporter(fileName,out,bwOut,msgDialogShower);
			((KonectNetworksImporter)importer).importKonectNetwork(printInfo, printCRN,print,false);
		}
		else if(format.equals(SupportedFormats.KonectEnsureUndirGraph)){
			importer = new KonectNetworksImporter(fileName,out,bwOut,msgDialogShower);
			((KonectNetworksImporter)importer).importKonectNetwork(printInfo, printCRN,print,true);
		}
		else if(format.equals(SupportedFormats.LBS)){
			//if(fileName.endsWith(".crn")){
			//System.out,bwOut.println("We are goind to load an explicit CRN (.crn)");
			importer = new LBSImporter(fileName,out,bwOut,msgDialogShower);
			((LBSImporter)importer).importLBSNetwork(printInfo, printCRN,print);
		}
		/*else if(format.equals(SupportedFormats.BNGL)){
			//if(fileName.endsWith(".crn")){
			//System.out,bwOut.println("We are goind to load a BNGL file (.crn)");
			importer = new BNGLImporter(fileName);
			((BNGLImporter)importer).importBNGLNetwork(printInfo, printCRN,print);
		}*/
		else if(format.equals(SupportedFormats.ChemKin)){
			importer = new ChemKinImporter(fileName,optionalParameters,out,bwOut,msgDialogShower);
			((ChemKinImporter)importer).importChemKinNetwork(printInfo, printCRN,print);
		}
		else if(format.equals(SupportedFormats.UMIST)){
			importer = new UMISTImporter(fileName,optionalParameters[0],out,bwOut,msgDialogShower);
			((UMISTImporter)importer).importUMISTNetwork(printInfo, printCRN, print);
		}
		else if(format.equals(SupportedFormats.OSU)){
			importer = new OSUImporter(fileName,Integer.valueOf(optionalParameters[0]),out,bwOut,msgDialogShower);
			((OSUImporter)importer).importOSUNetwork(printInfo, printCRN, print);
		}
		else if(format.equals(SupportedFormats.MRMC)){
			importer = new MRMCMarkovChainsImporter(fileName,optionalParameters,out,bwOut,msgDialogShower);
			((MRMCMarkovChainsImporter)importer).importMRMCMarkovChain(printInfo, printCRN,print);
		}
		else if(format.equals(SupportedFormats.CSV)){
			importer = new CSVMatrixAsLinearSystemImporter(fileName,out,bwOut,msgDialogShower);
			((CSVMatrixAsLinearSystemImporter)importer).importLinearSystemAsCSVMatrix(printInfo, printCRN, print);
		}
		else if(format.equals(SupportedFormats.CCSV)){
			importer = new CompactCSVMatrixImporter(fileName,optionalParameters,out,bwOut,msgDialogShower);
			((CompactCSVMatrixImporter)importer).importCSVMatrix(printInfo, printCRN, print);
		}
		else if(format.equals(SupportedFormats.Affine)){
			importer = new CompactCSVMatrixImporter(fileName,optionalParameters,out,bwOut,msgDialogShower);
			((CompactCSVMatrixImporter)importer).importAffineSystem(printInfo, printCRN, print);
		}
		else if(format.equals(SupportedFormats.CNFasPoly)){
			importer = new CNFImporter(fileName, out, bwOut, msgDialogShower);
			((CNFImporter)importer).readCNFandPolynomiaze(print);
		}
//		else if(format.equals(SupportedFormats.CNFasQuantumOptSAT)){
//			importer = new CNFImporter(fileName, out, bwOut, msgDialogShower);
//			((CNFImporter)importer).readCNFandMakeQuantumOptimization(print);
//		}
		else if(format.equals(SupportedFormats.LinearWithInputs)){
			//TODO
			importer = new CompactCSVMatrixImporter(fileName,optionalParameters,out,bwOut,msgDialogShower);
			//((CompactCSVMatrixImporter)importer).importAffineSystem(printInfo, printCRN, print);
			((CompactCSVMatrixImporter)importer).importLinearSystemWithInputs(printInfo, printCRN, print);
		}
		/*else if(fileName.endsWith(".xml")){
			//System.out,bwOut.println("We are goind to load a FERN CRN (.xml), written in FERNML");
			importer = new FERNMLImporter(fileName);
			((FERNMLImporter)importer).importFERNMLNetwork(printInfo, printCRN);
		}*/
		//else throw new UnsupportedFormatException("Currently supported networks formats are the compiled networks of BioNetGen (extension .net), or a simple list o reactions, possibly preceded by a list of parameters (extension .net). File nmae provided: "+fileName);
		else throw new UnsupportedFormatException("Unsupported format. Provided file nmae: "+fileName);
		
		long endImp = System.currentTimeMillis();
		importer.getInfoImporting().setRequiredMS(endImp-beginImp);
		
		
		
		return importer;
	}

}
