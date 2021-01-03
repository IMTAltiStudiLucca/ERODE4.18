package it.imt.erode.importing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;


/**
 * 
 * @author Andrea Vandin
 * This class is used to export reaction networks in the input format of the tool FlyFast (file extension: .pop).
 * We restrict to reaction networks where it is always possible to implicitly assign a mapping among reagents and products of each reaction 
 */
public class FlyFastImporter extends AbstractImporter{

	public static final String FlyFastModelsFolder = "."+File.separator+"FlyFastModels"+File.separator;

	public FlyFastImporter(String fileName,MessageConsoleStream out,BufferedWriter bwOut,IMessageDialogShower msgDialogShower) {
		super(fileName,out,bwOut,msgDialogShower);
	}

	public static void printCRNToFlyFastFile(ICRN crn, boolean verbose,MessageConsoleStream out,BufferedWriter bwOut) throws UnsupportedFormatException{
		printCRNToFlyFastFile(crn, crn.getName(),verbose,out,bwOut);
	}
	
	private static ISpecies getCatalyst(IComposite lhs, IComposite rhs) {
		//ISpecies[] ls = lhs.getAllSpecies();
		//ISpecies[] rs = rhs.getAllSpecies();
		for(int l=0;l<lhs.getNumberOfDifferentSpecies();l++){
			for(int r=0;r<rhs.getNumberOfDifferentSpecies();r++){
				if(lhs.getAllSpecies(l).equals(rhs.getAllSpecies(r))){
					return lhs.getAllSpecies(l); 
				}
			}
		}
		return null;
	}
	
	/**
	 * A new id is assigned to the species, considering their order.
	 * @param crn
	 * @param partition
	 * @param name
	 * @param assignPopulationOfRepresentative
	 * @param groupAccordingToCurrentPartition
	 * @throws UnsupportedFormatException 
	 */
	public static void printCRNToFlyFastFile(ICRN crn, String name, boolean verbose,MessageConsoleStream out,BufferedWriter bwOut) throws UnsupportedFormatException{
		String fileName = name;
		fileName=overwriteExtensionIfEnabled(fileName,".pop");
		
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"Writing model in file "+fileName);
		}

		//Build the transitions of each species
		HashMap<ISpecies,ArrayList<String>> statesTransitions = new HashMap<ISpecies,ArrayList<String>>(crn.getSpecies().size());
		for(ISpecies species : crn.getSpecies()){
			statesTransitions.put(species, new ArrayList<String>());
		}
		ArrayList<String> actionDefs = new ArrayList<String>();
		
		int a=1;
		for(ICRNReaction reaction : crn.getReactions()){
			IComposite reagents = reaction.getReagents();
			IComposite products = reaction.getProducts();
			
			//special case: 1->1: I assume that the reagent evolves in the product 
			if(reagents.isUnary() && products.isUnary()){
				ISpecies reagent=reagents.getFirstReagent();
				ISpecies product=products.getFirstReagent();
				//I assume that the reagent species evolves in the product one
				ArrayList<String> transitions = statesTransitions.get(reagent);
				//zab
				String actionName = reagent.getNameAlphanumeric()+"to"+product.getNameAlphanumeric()+a++;
				//action zab : (1.0)/q; /* transition from Z0 to Z1 */
				String actionDef = "action "+actionName+" : ("+reaction.getRateExpression()+")"+"/q; /* unary transition from "+reagent.getName() +" to "+product.getName() +" */";
				actionDefs.add(actionDef);
				//zab.Z1
				transitions.add(actionName+"."+product.getNameAlphanumeric());
			}
			//special case: 2->2:  I assume that one reagent evolves in one of the products, while the other one is a catalyst and does not change
			else  if(reagents.isBinary() && products.isBinary()){
				ISpecies catalyst = getCatalyst(reagents, products);
				if(catalyst==null){
					throw new UnsupportedFormatException("The exporting to FlyFast is currently limited to 1->1 reacitons or 2->2 catalytic reactions: "+reaction.toString());
				}
				
				ISpecies reagent= (catalyst.equals(reagents.getFirstReagent())) ? reagents.getSecondReagent() : reagents.getFirstReagent();
				ISpecies product= (catalyst.equals(products.getFirstReagent())) ? products.getSecondReagent() : products.getFirstReagent();
				//I assume that the reagent species evolves in the product one (as the other reagent and product are the catalyst)
				ArrayList<String> transitions = statesTransitions.get(reagent);
				//zab
				String actionName = reagent.getNameAlphanumeric()+"to"+product.getNameAlphanumeric()+a++;
				//action zab : 1.0*frc (S0)/q; /* transition from Z0 to Z1 */
				String actionDef = "action "+actionName+" : ("+reaction.getRateExpression()+")*frc ("+catalyst.getNameAlphanumeric()+")/q; /* transition from "+reagent.getName() +" to "+product.getName() +" with catalyst "+catalyst.getName()+" */";
				actionDefs.add(actionDef);
				//zab.Z1
				transitions.add(actionName+"."+product.getNameAlphanumeric());
			}
			else{
				throw new UnsupportedFormatException("The exporting to FlyFast is currently limited to 1->1 reacitons or 2->2 catalytic reactions: "+reaction.toString());
			}
		}
		
		//We can now actually write the converted model in a file
		
		createParentDirectories(fileName);
		BufferedWriter br;
		try {
			br = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printCRNToFlyFastFile, exception raised while creating the filewriter for file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		
		try {
			
			DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
			Date date = new Date();
			//CRNReducerCommandLine.println(out,dateFormat.format(date)); //2014/08/06 15:59:48
			br.write("/* Date: "+dateFormat.format(date)+"\n");
			if(name!=null && !name.equals("")){
				br.write(" * Automatically generated by "+CRNReducerCommandLine.TOOLNAME+" starting from the CRN "+name+"\n");
			}
			else{
				br.write(" * Generated by "+CRNReducerCommandLine.TOOLNAME+"\n");
			}
			br.write(" *\n");
			br.write(" * "+crn.getSpecies().size()+" species, "+crn.getReactions().size()+" reactions\n");
			br.write(" *\n");
			br.write(" */\n");
			
			br.write("\n");
			
			br.write("const q = 10; /* PLEASE PROVIDE THE CORRECT UNIFORMISATION RATE */\n");
			br.write("\n");
			
			for(String param : crn.getParameters()){
				br.write("const "+param.replace(" ", " = ")+";\n");
			}
			
			br.write("\n");
			
			for(String actionDef : actionDefs){
				br.write(actionDef+"\n");
			}
			
			br.write("\n");
			br.write("\n");
			
			StringBuilder initConf = new StringBuilder();
			for(ISpecies species : crn.getSpecies()){
				//this is to build the initial status
				//Z0[1000],S0[1000],R0[1000],Y0[1000],Q0[1000],P0[1000]
				if(species.getInitialConcentration().compareTo(BigDecimal.ZERO)!=0){
					//initConf.append(species.getNameSupportedByMathEval()+"["+species.getInitialConcentration().doubleValue()+"],");
					initConf.append(species.getNameAlphanumeric()+"["+species.getInitialConcentrationExpr()+"],");
				}
				
				//we now write the transitions of this species 
				/*
				 * state Z1 {
					zbc.Z2 + zba.Z0
				   }
				 */
				ArrayList<String> transitions = statesTransitions.get(species);
				br.write("state "+species.getNameAlphanumeric()+" {\n");
				br.write("\n");
				br.write("\t");
				StringBuilder sb = new StringBuilder();
				boolean first=true;
				for(String transition : transitions){
					if(!first){
						sb.append(" + ");
					}
					else{
						first=false;
					}
					sb.append(transition);
				}
				br.write(sb.toString()+"\n");
				br.write("\n");
				br.write("}\n");
				br.write("\n");
			}
			
			/* With this initial values there are no three equivalence classes. This is because here initially states
			 * that are in the same class get different initial values.
			 */
			//system main = <Z0[1000],S0[1000],R0[1000],Y0[1000],Q0[1000],P0[1000]>;
			br.write("\n");
			if(initConf.length()>0){
				br.write("/*\n");
				br.write(" *Initial configuration built resorting to the initial concentrations of the species as defined in the model\n");
				br.write(" */\n");
				br.write("system main = <");
				initConf.deleteCharAt(initConf.length()-1);
				br.write(initConf.toString());
				br.write(">;\n");
			}
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printCRNToFlyFastFile, exception raised while writing in the file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		finally{
			if(verbose){
				CRNReducerCommandLine.println(out,bwOut,"Writing reactions in file "+fileName+" completed");
			}
			try {
				br.close();
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in printCRNToFlyFastFile, exception raised while closing the bufferedwriter of the file: "+fileName);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}

	}
	
	public static String speciesNameSupportedByFlyFast(String name){
		return name;
	}

}
