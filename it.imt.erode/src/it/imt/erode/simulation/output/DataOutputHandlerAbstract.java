package it.imt.erode.simulation.output;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
//import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.ImageIcon;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.DialogType;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.commandline.SimulationSolutions;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.expression.evaluator.MathEval;
import it.imt.erode.importing.AbstractImporter;
import it.imt.erode.importing.GUICRNImporter;
//import vesta.mc.InfoMultiQuery;
import vesta.mc.InfoMultiQuery;

public abstract class DataOutputHandlerAbstract implements IDataOutputHandler {

	protected double[] x;
	//A matrix speciesName * speciesPlot (e.g., plots[0] is the plot of the first species)
	protected double[][] plotsAll;
	protected String[] labelsAll;
	protected double[][] plotsViews;
	protected String[] labelsViews;
	protected String[] viewsExpressions;
	protected boolean hasViews;
	private MathEval math;
	protected ICRN crn;
	protected boolean covariances=false;
	protected boolean hasSMCVariances=false;
	protected double[][] smcVariances;

	public static final String allGifName="images/species.gif";
	public static final String viewsGifName="images/views.gif";
	public  ImageIcon iconAll;
	public ImageIcon iconViews;
	public String mainTabLabel;//="Concentrations of the species";

	protected String messageSuffix=" - All species/variables";
	protected String xLabel="Time";
	protected String yLabel="Species/variable concentrations";

	protected String minimalDescription;
	protected boolean showLabels=true;//private static final boolean showLabels=true;
	protected String command;
	private boolean computeJacobian;
	private MessageConsoleStream out;
	private BufferedWriter bwOut;
	private IMessageDialogShower msgDialogShower;
	public boolean getComputeJacobian(){
		return computeJacobian;
	}

	//	double[] getX();
	//
	//	double[][] getPlotsAll();
	//
	//	double[][] getPlotsViews();

	@Override
	public SimulationSolutions getSimulationSolutions(){
		return new SimulationSolutions(x, plotsAll, plotsViews, labelsAll, labelsViews);
	}

	private static final String csvSeparator=", ";


	/** Returns an ImageIcon, or null if the path was invalid. */
	protected ImageIcon createImageIcon(String path) {
		//return new ImageIcon(path);
		//return new ImageIcon(getClass().getResource(path));
		//return new ImageIcon(getClass().getResource(path));
		java.net.URL imgURL = DataOutputHandler.class.getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL);
		} else {
			//System.err.println("Couldn't find file: " + path);
			return null;
		}
	}

	@Override
	public void setShowLabels(boolean showLabels){
		this.showLabels=showLabels;
	}

	@Override
	public MessageConsoleStream getOut(){
		return out;
	}
	@Override
	public BufferedWriter getBWOut(){
		return bwOut;
	}
	@Override
	public IMessageDialogShower getMsgDialogShower(){
		return msgDialogShower;
	}
	@Override
	public void setOut(MessageConsoleStream out){
		this.out=out;
	}
	@Override
	public void setBWOut(BufferedWriter bwOut){
		this.bwOut=bwOut;
	}

	@Override
	public void setMsgDialogShower(IMessageDialogShower msgDialogShower){
		this.msgDialogShower=msgDialogShower;
	}

	@Override
	public void writeCSV(String csvFile/*,MessageConsoleStream out,IMessageDialogShower msgDialogShower*/) {

		try {
			if(!csvFile.endsWith(".cdat")) {
				csvFile=csvFile+".cdat";
			}
			
			FileOutputStream fos = new FileOutputStream(csvFile);
			CRNReducerCommandLine.print(out,bwOut,"Writing the csvFile "+csvFile+" ... ");

			PrintStream Output = new PrintStream(fos);
			StringBuilder caption = new StringBuilder();
			caption.append("# time"+csvSeparator);
			for(String speciesName : this.labelsAll){
				caption.append(speciesName + csvSeparator);
			}

			//covariances
			/*boolean printAllCovariances=false;
			for(int covariance=labelsAll.length;covariance<plotsAll.length;covariance++){
				int cov=(covariance-labelsAll.length);//this is the cov-th covariance. I.e., the C_{species1,species2}
				int species1 = cov / labelsAll.length;
				int species2 = cov % labelsAll.length;
				if(species1==species2){//with this if I only print the variances
					caption.append("V_"+ labelsAll[species1] + csvSeparator);
				}
				else{
					if(printAllCovariances){
						caption.append("C_"+ labelsAll[species1]+"-"+labelsAll[species2] + csvSeparator);
					}
				}
			}*/

			Output.println(caption.toString());

			for(int step=0;step<x.length;step++){
				StringBuilder concentrationsAtStep = new StringBuilder();
				concentrationsAtStep.append(x[step]+ csvSeparator);
				for(int species=0;species<labelsAll.length-1;species++){
					concentrationsAtStep.append(plotsAll[species][step]+ csvSeparator);
				}

				/*//covariances
				for(int covariance=labelsAll.length;covariance<plotsAll.length;covariance++){
					int cov=(covariance-labelsAll.length);//this is the cov-th covariance. I.e., the C_{species1,species2}
					int species1 = cov / labelsAll.length;
					int species2 = cov % labelsAll.length;
					if(species1==species2){//with this if I only print the variances
						concentrationsAtStep.append(plotsAll[covariance][step]+ csvSeparator);
					}
					else{
						if(printAllCovariances){
							concentrationsAtStep.append(plotsAll[covariance][step]+ csvSeparator);
						}
					}
				}*/


				concentrationsAtStep.append(plotsAll[labelsAll.length-1][step]);
				Output.println(concentrationsAtStep.toString());
			}
			Output.close();
			CRNReducerCommandLine.println(out,bwOut,"completed.");
		} catch (FileNotFoundException e) {
			CRNReducerCommandLine.println(out,bwOut,"Could not create the file "+csvFile+".cdat");
		}
		if(hasViews){
			try {
				FileOutputStream fos = new FileOutputStream(csvFile+".vdat");
				CRNReducerCommandLine.print(out,bwOut,"Writing the csvFile "+csvFile+".vdat ... ");

				PrintStream Output = new PrintStream(fos);
				StringBuilder caption = new StringBuilder();
				caption.append("# time"+csvSeparator);
				for(String viewsName : this.labelsViews){
					caption.append(viewsName + csvSeparator);
				}
				Output.println(caption.toString());

				for(int step=0;step<x.length;step++){
					StringBuilder concentrationsOfViewsAtStep = new StringBuilder();
					concentrationsOfViewsAtStep.append(x[step]+ csvSeparator);
					for(int view=0;view<labelsViews.length-1;view++){
						concentrationsOfViewsAtStep.append(plotsViews[view][step]+ csvSeparator);
					}
					concentrationsOfViewsAtStep.append(plotsViews[labelsViews.length-1][step]);
					Output.println(concentrationsOfViewsAtStep.toString());
				}
				Output.close();
				CRNReducerCommandLine.println(out,bwOut,"completed.");
			} catch (FileNotFoundException e) {
				CRNReducerCommandLine.println(out,bwOut,"Could not create the file "+csvFile+".vdat");
			}
		}
		if(computeJacobian){

			//Build the jump vector (and its transpose) of each reaction. This is very space demanding: numberOfReactions*numberOfSpecies. Check if I can instead store an array of composites (the jvComposite).
			int[][] jumps = computeJumps(crn);

			try {
				FileOutputStream fos = new FileOutputStream(csvFile+".jdat");
				CRNReducerCommandLine.print(out,bwOut,"Writing the csvFile "+csvFile+".jdat ... ");

				//int numberOfSpecieFs = crn.getSpecies().size();I use labelsAll.length
				PrintStream Output = new PrintStream(fos);
				StringBuilder caption = new StringBuilder();
				caption.append("# time"+csvSeparator);

				int n2 = crn.getSpecies().size()*crn.getSpecies().size();
				for(int jacobianentry=0;jacobianentry<n2-1;jacobianentry++){
					//int cov=(jacobianentry-labelsAll.length);
					//this is the j-th jacobian entry. I.e., the J_{species1,species2}
					int species1 = jacobianentry / labelsAll.length;
					int species2 = jacobianentry % labelsAll.length;
					//caption.append("J("+(species1+1)+","+(species2+1)+")" + csvSeparator);
					caption.append("J("+(labelsAll[species1])+","+(labelsAll[species2])+")" + csvSeparator);
				}
				//caption.append("J("+(crn.getSpecies().size()-1)+","+(crn.getSpecies().size()-1)+")");
				caption.append("J("+(labelsAll[crn.getSpecies().size()-1])+","+(labelsAll[crn.getSpecies().size()-1])+")");
				Output.println(caption.toString());

				MutableBoolean warned = new MutableBoolean();

				for(int step=0;step<x.length;step++){
					StringBuilder jacobianAtStep = new StringBuilder();
					jacobianAtStep.append(x[step]+ csvSeparator);
					double[][] je = computeCurrentJacobian(plotsAll, step, crn,jumps,msgDialogShower,out,bwOut,warned);
					for(int jacobianentry=0;jacobianentry<n2-1;jacobianentry++){
						//for(int jacobianentry=labelsAll.length;jacobianentry<plotsAll.length;jacobianentry++){
						int species1 = jacobianentry / labelsAll.length;
						int species2 = jacobianentry % labelsAll.length;
						jacobianAtStep.append(je[species1][species2]+ csvSeparator);
					}
					jacobianAtStep.append(je[crn.getSpecies().size()-1][crn.getSpecies().size()-1]);
					Output.println(jacobianAtStep.toString());
				}
				Output.close();
				CRNReducerCommandLine.println(out,bwOut,"completed.");
			} catch (FileNotFoundException e) {
				CRNReducerCommandLine.println(out,bwOut,"Could not create the file "+csvFile+".jdat");
			}
		}


	}

	//@Override
	public static void writeCSV(String csvFile,String extension, String stepLabel, String valueLabel, int startingStep, int stepIncrement, List<Double> valuesAtPoint,MessageConsoleStream out,BufferedWriter bwOut) {
		csvFile=AbstractImporter.overwriteExtensionIfEnabled(csvFile, "", true);
		try {
			FileOutputStream fos = new FileOutputStream(csvFile+"."+extension);
			CRNReducerCommandLine.print(out,bwOut,"Writing the csvFile "+csvFile+"."+extension +"... ");

			PrintStream Output = new PrintStream(fos);
			Output.println("# "+stepLabel+csvSeparator+valueLabel);

			int step = startingStep;
			for (Double value : valuesAtPoint) {
				Output.println(step+csvSeparator+value);
				step+=stepIncrement;
			}
			Output.close();
			CRNReducerCommandLine.println(out,bwOut,"completed.");
		} catch (FileNotFoundException e) {
			CRNReducerCommandLine.println(out,bwOut,"Could not create the file "+csvFile+".cdat");
		}
	}
	
	//@Override
		public static void writeOneLineCSV(String csvFile,String extension, List<String> labels, List<String> values,MessageConsoleStream out,BufferedWriter bwOut) {
			csvFile=AbstractImporter.overwriteExtensionIfEnabled(csvFile, "", true);
			csvFile = csvFile+"."+extension;
			AbstractImporter.createParentDirectories(csvFile);
			boolean writeLabels = true;
			
			File f = new File(csvFile);
			if(f.exists()) {
				writeLabels=false;
			}
			f=null;
			
			try {
				boolean append=true;
				FileOutputStream fos = new FileOutputStream(csvFile,append);
				CRNReducerCommandLine.print(out,bwOut,"Writing the csvFile "+csvFile +"... ");

				PrintStream Output = new PrintStream(fos);
				
				if(writeLabels) {
					for(String label : labels) {
						Output.print(label+csvSeparator);
					}
					Output.println();
				}
				
				for(String value : values) {
					Output.print(value+csvSeparator);
				}
				Output.println();
				Output.close();
				CRNReducerCommandLine.println(out,bwOut,"completed.");
			} catch (FileNotFoundException e) {
				CRNReducerCommandLine.println(out,bwOut,"Could not create the file "+csvFile+"."+extension);
			}
		}

	public static int[][] computeJumps(ICRN crn) {
		int[][] jumps = new int[crn.getReactions().size()][crn.getSpecies().size()];
		int r=0;
		for(ICRNReaction reaction : crn.getReactions()){
			IComposite jvComposite = reaction.computeProductsMinusReagents();
			for(int s =0;s<jvComposite.getNumberOfDifferentSpecies();s++){
				int sId=jvComposite.getAllSpecies(s).getID();
				int mult = jvComposite.getMultiplicities(s);
				jumps[r][sId]=mult;
			}
			r++;
		}
		return jumps;
	}

	/*public static double[][] computeCurrentJacobian(double[][] plotsAll, int step, ICRN crn, int[][] jumps) {
		return computeCurrentJacobian(plotsAll,step,crn,jumps,null,null);
	}*/

	public static double[][] computeCurrentJacobian(double[][] plotsAll, int step, ICRN crn, int[][] jumps,IMessageDialogShower msgDialogShower,MessageConsoleStream out,BufferedWriter bwOut,MutableBoolean warned) {
		//private double[][] computeCurrentJacobian(double[] y, double[] nv, ICRN crn) {

		List<ICRNReaction> reactions = crn.getReactions();
		//Compute nv: the actual firing rate of each reaction
		double[] nv = computeFiringRates(plotsAll, step, reactions);

		double[][] Je = new double[crn.getSpecies().size()][crn.getSpecies().size()];//do I need to init each line to zero?
		int j=0;
		//boolean warned=false;
		for(ICRNReaction reaction : crn.getReactions()){
			for(int i=0;i<crn.getSpecies().size();i++){
				for(int k=0;k<crn.getSpecies().size();k++){
					//TODO: fix division by zero problem!
					if(plotsAll[k][step]!=0){
						Je[i][k]+= jumps[j][i] * reaction.getReagents().getMultiplicityOfSpeciesWithId(k) * nv[j] / plotsAll[k][step];
					}
					else{
						if(!warned.getValue()){
							warned.setValue(true);
							CRNReducerCommandLine.printWarning(out,bwOut, true, msgDialogShower, "Some species have 0-concentration (e.g.,"+crn.getSpecies().get(k).getName()+").\nThe Jacobian-columns of those species are not computed to avoid divisions by 0.",true,DialogType.Warning);
						}

					}
				}
			}
			j++;
		}
		return Je;
	}

	public static double[] computeFiringRates(double[][] plotsAll, int step, List<ICRNReaction> reactions) {
		double[] nv = new double[reactions.size()];
		int r=0; 
		for (ICRNReaction reaction : reactions) {
			IComposite lhs = reaction.getReagents();
			// get the total rate in normal mass action
			nv[r] = reaction.getRate().doubleValue();
			for(int i=0;i<lhs.getNumberOfDifferentSpecies();i++){
				ISpecies s = lhs.getAllSpecies(i);
				int mult = lhs.getMultiplicities(i);
				//nv[r] *= mult; //Math.pow(y[s.getLabelID()], mult);
				nv[r] *= Math.pow(plotsAll[s.getID()][step], mult);//nv[r] *= Math.pow(NonZeroorMinDouble(populations[s.getID()]), mult);
			}
			r++;
		}
		return nv;
	}

	
	
	public static String[][] computeJacobianFunction(ICRN crn, int[][] jumps,IMessageDialogShower msgDialogShower,MessageConsoleStream out, int idIncrement, boolean ignoreZeros/*,ISpecies[] speciesIDToSpecies*/,boolean resolveParams) {
		return computeJacobianFunction(crn, jumps, msgDialogShower,out, idIncrement, ignoreZeros,/*speciesIDToSpecies,*/resolveParams, true);
	}
	public static String[][] computeJacobianFunction(ICRN crn, int[][] jumps,IMessageDialogShower msgDialogShower,MessageConsoleStream out, boolean ignoreZeros/*,ISpecies[] speciesIDToSpecies*/,boolean resolveParams) {
		return computeJacobianFunction(crn, jumps, msgDialogShower,out, Integer.MIN_VALUE, ignoreZeros/*,speciesIDToSpecies*/, resolveParams,false);
	}
	
	public static String[][] computeJacobianFunction(ICRN crn, int[][] jumps,IMessageDialogShower msgDialogShower,MessageConsoleStream out, int idIncrement, boolean ignoreZeros/*,ISpecies[] speciesIDToSpecies*/, boolean resolveParams, boolean replaceSpeciesWithYid) {

		//List<ICRNReaction> reactions = crn.getReactions();
		//Compute nv: the actual firing rate of each reaction
		//String[] nv = computeFiringRateExperessions(reactions,idIncrement);


		String[][] Je = new String[crn.getSpecies().size()][crn.getSpecies().size()];//do I need to init each line to zero?
		int j=0;
		for(ICRNReaction reaction : crn.getReactions()){
			IComposite netStoichiometry = reaction.computeProductsMinusReagents();
			IComposite reagents = reaction.getReagents();
			//I have to check if all species i should be considered for this reaction, or if I have to consider only those with non-zero net stoichiometry
			//for(int i=0;i<crn.getSpecies().size();i++){
			for(int si=0; si<netStoichiometry.getNumberOfDifferentSpecies();si++) {
				ISpecies speciesi = netStoichiometry.getAllSpecies(si);
				int i = speciesi.getID();
				//This for loop on k is way too inefficient. We will consider only those k appearing as reagents of reaction 
				//for(int k=0;k<crn.getSpecies().size();k++){
				for(int s=0;s<reagents.getNumberOfDifferentSpecies();s++) {
					ISpecies reagent = reagents.getAllSpecies(s);
					int mult = reagents.getMultiplicities(s);
					int k = reagent.getID();
					//int id = k+idIncrement;
					//String sp = "y("+id+")";
					//jumps[i][j] is the net stoichiometry of species i in reaction j
					int jumpAndMult = jumps[j][i] * mult; //reaction.getReagents().getMultiplicityOfSpeciesWithId(k);

					if(jumpAndMult!=0){
						String nv = computeFiringRateExperessionDecreasingMultiplicityOfSpeciesOfTheRow(reaction,idIncrement,/*speciesIDToSpecies[k],*//*crn.getSpecies().get(k)*/reagent, replaceSpeciesWithYid,resolveParams);
						String toWrite = jumpAndMult +"*" +nv; // +"/" + sp;
						if(jumpAndMult==1){
							toWrite = nv; // +"/" + sp;
						}
						else if(jumpAndMult==-1){
							toWrite = "-" + nv; //[j] +"/" + sp;
						} 

						if(Je[i][k]==null){
							Je[i][k]= toWrite;
						}
						else{
							Je[i][k]+= " + " + toWrite;
						}
					}
				}
			}
			j++;
		}
		/*
		for(int i=0;i<crn.getSpecies().size();i++){
			for(int k=0;k<crn.getSpecies().size();k++){
				if(Je[i][k]==null) {
					Je[i][k]="0";
				}
			}
		}
		*/
		return Je;
	}

	public static String[][] computeJacobianFunctionOLD(ICRN crn, int[][] jumps,IMessageDialogShower msgDialogShower,MessageConsoleStream out, int idIncrement, boolean ignoreZeros) {

		List<ICRNReaction> reactions = crn.getReactions();
		//Compute nv: the actual firing rate of each reaction
		String[] nv = computeFiringRateExperessions(reactions,idIncrement);

		String[][] Je = new String[crn.getSpecies().size()][crn.getSpecies().size()];//do I need to init each line to zero?
		int j=0;
		for(ICRNReaction reaction : crn.getReactions()){
			for(int i=0;i<crn.getSpecies().size();i++){
				for(int k=0;k<crn.getSpecies().size();k++){
					int id = k+idIncrement;
					String sp = "y("+id+")";
					//jumps[i][j] is the net stoichiometry of species i in reaction j
					int jumpAndMult = jumps[j][i] * reaction.getReagents().getMultiplicityOfSpeciesWithId(k);
					if(jumpAndMult!=0){
						String toWrite = jumpAndMult +"*" +nv[j] +"/" + sp;
						if(jumpAndMult==1){
							toWrite = nv[j] +"/" + sp;
						}
						else if(jumpAndMult==-1){
							toWrite = "-" + nv[j] +"/" + sp;
						} 

						if(Je[i][k]==null){
							Je[i][k]= toWrite;
						}
						else{
							Je[i][k]+= " + " + toWrite;
						}
					}
				}
			}
			j++;
		}
		return Je;
	}

	public static String[] computeFiringRateExperessions(List<ICRNReaction> reactions, int idIncrement) {
		String[] nv = new String[reactions.size()];
		int r=0; 
		for (ICRNReaction reaction : reactions) {
			IComposite lhs = reaction.getReagents();
			// get the total rate in normal mass action
			//nv[r] = reaction.getRate().toPlainString();// reaction.getRate().doubleValue();
			nv[r] = GUICRNImporter.addParIfNecessary(reaction.getRateExpression());
			for(int i=0;i<lhs.getNumberOfDifferentSpecies();i++){
				ISpecies s = lhs.getAllSpecies(i);
				int mult = lhs.getMultiplicities(i);
				int id = s.getID()+idIncrement;
				String sp = "y("+id+")";
				for(int m=0; m< mult;m++){
					nv[r] = nv[r] + "*" + sp;
				}				
			}
			r++;
		}
		return nv;
	}


//	public static String computeFiringRateExperessionDecreasingMultiplicityOfSpeciesOfTheRow(ICRNReaction reaction, ISpecies toIgnore) {
//		return computeFiringRateExperessionDecreasingMultiplicityOfSpeciesOfTheRow(reaction, Integer.MIN_VALUE,toIgnore,false);
//	}

//	public static String computeFiringRateExperessionDecreasingMultiplicityOfSpeciesOfTheRow(ICRNReaction reaction, int idIncrement, ISpecies toIgnore) {
//		return computeFiringRateExperessionDecreasingMultiplicityOfSpeciesOfTheRow(reaction, idIncrement,toIgnore,true);
//	}

	public static String computeFiringRateExperessionDecreasingMultiplicityOfSpeciesOfTheRow(ICRNReaction reaction, int idIncrement, ISpecies toIgnore, boolean replaceSpeciesWithYid,boolean resolveParams) {


		IComposite lhs = reaction.getReagents();
		// get the total rate in normal mass action
		String nv = (resolveParams)? reaction.getRate().toPlainString() : GUICRNImporter.addParIfNecessary(reaction.getRateExpression());
		for(int i=0;i<lhs.getNumberOfDifferentSpecies();i++){
			ISpecies s = lhs.getAllSpecies(i);
			int mult = lhs.getMultiplicities(i);
			if(s.equals(toIgnore)){
				mult=mult-1;
			}
			String sp = s.getName();
			if(replaceSpeciesWithYid) {
				int id = s.getID()+idIncrement;
				sp = "y("+id+")";
			}
			for(int m=0; m< mult;m++){
				nv = nv + "*" + sp;
			}				
		}

		return nv;
	}


	private void setData(ICRN crn, String minimalDescription, String mainTabLabel, String command){
		this.mainTabLabel=mainTabLabel;
		this.command=command;
		iconAll=createImageIcon(allGifName);
		iconViews=createImageIcon(viewsGifName);
		this.labelsAll= crn.createArrayOfAllSpeciesNames();
		this.labelsViews=crn.getViewNames();
		this.viewsExpressions=crn.getViewExpressionsSupportedByMathEval();

		this.math=new MathEval(crn.getMath());
		this.crn=crn;
		this.minimalDescription=minimalDescription;
	}

	@Override
	/**
	 * Note that this method only supports the case stepPerSpecies=false. The case true is supported by the other constructor. I leave the flag to stress the different input encoding
	 * @param result the result of the simulation. For each species, we have its concentration at each step. This can be either result[step][species], or result[step][species], specified by the following flag
	 * @param stepPerSpecies if true, the first parameter of result is the step and the second is the species (and thus result[i] contains the concentrations of all species at step i). If false, we have that result[i] contains all the concentrations of the species with id i. 
	 * @param x
	 * @param crn
	 */
	public void setData(String minimalDescription, double[][] result, double[][] resultViews, boolean stepPerSpecies, double[] x, ICRN crn,boolean skipFirstPosOfResult, String command) {
		setData(crn,minimalDescription,"Concentrations of the species",command);
		if(stepPerSpecies){
			throw new UnsupportedOperationException("The contructor DataOutputHandler(double[][] result, double[][] resultViews, boolean stepPerSpecies, double[] x, ICRN crn) cannot be invoked with the flag to true");
		}
		this.x=x;
		this.covariances=false;
		this.hasViews= !(labelsViews==null || labelsViews.length==0 || resultViews==null || resultViews.length==0);
		if(hasViews && !covariances){
			if(onlyCovariancesViews()){
				hasViews = false;
			}
		}

		//this.solution=solution;
		initData(result,resultViews,stepPerSpecies,skipFirstPosOfResult);
		//initPlot();
	}

	private void initData(double[][] result, double[][] resultViews, boolean stepPerSpecies,boolean skipFirstPosOfResult){
		plotsAll= new double[labelsAll.length][];
		if(hasViews){
			plotsViews= new double[labelsViews.length][];
		}

		int incr=0;
		if(skipFirstPosOfResult){
			incr=1;
		}

		if(stepPerSpecies){
			//double[] line = result[i]; contains all concentrations at ith step, while line[5] is the concentration at ith step of the species with id 5.
			throw new UnsupportedOperationException("The method initData(double[][] result, double[][] resultViews, boolean stepPerSpecies) cannot be invoked with the flag to false");
		}
		else{
			//double[] line = result[i]; contains all concentrations of the species with id i (one per step). 
			for(int species=0;species<labelsAll.length;species++){
				plotsAll[species]=Arrays.copyOf(result[species+incr], result[species+incr].length);
			}
			if(hasViews){
				for(int view=0;view<labelsViews.length;view++){
					plotsViews[view]=Arrays.copyOf(resultViews[view+incr], resultViews[view+incr].length);
				}
			}
		}
	}

	@Override
	/**
	 * Invoked after solving the odes of the concentrations of the species of a CRN or the variables of an ODE system
	 * Note that this method only supports the case stepPerSpecies=true. The case false is supported by the other constructor. I leave the flag to stress the different input encoding
	 * @param result the result of the simulation. For each species, we have its concentration at each step. This can be either result[step][species], or result[species][step], specified by the following flag
	 * @param stepPerSpecies if true, the first parameter of result is the step and the second is the species (and thus result[i] contains the concentrations of all species at step i). If false, we have that result[i] contains all the concentrations of the species with id i. 
	 * @param x
	 * @param crn
	 */
	public void setData(String minimalDescription, double[][] result, boolean stepPerSpecies, double[] x, ICRN crn,boolean skipFirstPosOfResult, boolean covariances, boolean computeJacobian,String command) {
		setData(crn,minimalDescription,"Concentrations of the species",command);
		this.covariances=covariances;
		this.computeJacobian=computeJacobian;

		if(!stepPerSpecies){
			throw new UnsupportedOperationException("The contructor DataOutputHandler(double[][] result, boolean stepPerSpecies, double[] x, ICRN crn) cannot be invoked with the flag to false");
		}
		this.x=x;


		this.hasViews= !(labelsViews==null || labelsViews.length==0);
		if(hasViews && !covariances){
			if(onlyCovariancesViews()){
				hasViews = false;
			}
		}

		//this.solution=solution;
		initData(result,stepPerSpecies,skipFirstPosOfResult);
		//initPlot();
	}

	private void initData(double[][] result, boolean stepPerSpecies, boolean skipFirstPosOfResult){
		if(covariances){
			plotsAll= new double[labelsAll.length+(labelsAll.length*labelsAll.length)][];
		}
		else{
			plotsAll= new double[labelsAll.length][];
		}
		if(hasViews){
			plotsViews= new double[labelsViews.length][];
		}

		int incr=0;
		if(skipFirstPosOfResult){
			incr=1;
		}

		if(stepPerSpecies){
			//double[] line = result[i]; contains all concentrations at ith step, while line[5] is the concentration at ith step of the species with id 5.
			//for(int species=0;species<labelsAll.length;species++){
			//means
			for(int species=0;species<labelsAll.length;species++){
				plotsAll[species]=new double[x.length];
			}
			if(covariances){
				//covariances
				for(int covariance=labelsAll.length;covariance<plotsAll.length;covariance++){
					plotsAll[covariance]=new double[x.length];
				}
			}

			if(hasViews){
				for(int view=0;view<labelsViews.length;view++){
					plotsViews[view]=new double[x.length];
				}
			}
			for(int step=0;step<x.length;step++){
				double[] concentrationsAtStep=result[step];
				for(int species=0;species<labelsAll.length;species++){
					plotsAll[species][step]=concentrationsAtStep[species+incr];
					if(hasViews){
						math.setVariable(crn.getSpecies().get(species).getNameAlphanumeric(), concentrationsAtStep[species+incr]);//TODO: very inefficient access to species
					}
				}
				if(covariances){
					for(int covariance=labelsAll.length;covariance<plotsAll.length;covariance++){
						plotsAll[covariance][step]=concentrationsAtStep[covariance+incr];
						if(hasViews){
							int cov=(covariance-labelsAll.length);//this is the cov-th covariance. I.e., the C_{species1,species2}
							int species1 = cov / labelsAll.length;
							int species2 = cov % labelsAll.length;
							/*
							if(species1==species2){//with this if I only print the variances
								math.setVariable("V_"+ MathEval.getCorrespondingStringSupportedByMathEval(labelsAll[species1]),plotsAll[covariance][step]);
							}
							else{
								math.setVariable("C_"+ MathEval.getCorrespondingStringSupportedByMathEval(labelsAll[species1])+"_"+MathEval.getCorrespondingStringSupportedByMathEval(labelsAll[species2]),plotsAll[covariance][step]);
							}
							 */
							//Like this I support both covar(x0,x0) and var(x0)
							if(species1==species2){//with this if I only print the variances
								math.setVariable("V_"+ MathEval.getCorrespondingStringSupportedByMathEval(labelsAll[species1]),plotsAll[covariance][step]);
							}
							math.setVariable("C_"+ MathEval.getCorrespondingStringSupportedByMathEval(labelsAll[species1])+"_"+MathEval.getCorrespondingStringSupportedByMathEval(labelsAll[species2]),plotsAll[covariance][step]);
						}
					}
				}

				if(hasViews){
					for(int view=0;view<labelsViews.length;view++){
						if((!covariances) && crn.getViewExpressionsUsesCovariances()[view]){
							continue;
						}
						else{
							double viewValue = math.evaluate(viewsExpressions[view]);
							plotsViews[view][step]=viewValue;
						}
					}
				}
			}
		}
		else{
			throw new UnsupportedOperationException("The method initData(double[][] result, boolean stepPerSpecies) cannot be invoked with the flag to false");
			/*//double[] line = result[i]; contains all concentrations of the species with id i (one per step). 
			for(int species=0;species<labelsAll.length;species++){
				plotsAll[species]=Arrays.copyOf(result[species], result[species].length);
			}
			//Pay attention: I never checked this, because the flag stepPerSpecies is false only for CTMC simulation, for which I have the views already computed.
			if(hasViews){
				for(int step=0;step<result.length;step++){
					for(int species=0;species<labelsAll.length;species++){
						math.setVariable(crn.getSpecies().get(species).getNameSupportedByMathEval(), plotsAll[species][step]);
					}
					for(int view=0;view<labelsViews.length;view++){
						plotsViews[view][step]=math.evaluate(viewsExpressions[view]);
					}
				}
			}*/
		}
	}

	private boolean onlyCovariancesViews() {
		for(int i=0;i<crn.getViewExpressionsUsesCovariances().length;i++){
			if(!crn.getViewExpressionsUsesCovariances()[i]){
				return false;
			}
		}
		return true;
	}

	@Override
	/**
	 * Invoked after solving the statistical analysis with MultiVeStA 
	 */ 
	public void setData(String minimalDescription, ICRN crn, InfoMultiQuery infoMultiQuery,double alpha, double delta, String command){
		setData(crn,minimalDescription,"Means estimations",command);
		labelsAll= null;
		labelsViews=null;
		viewsExpressions=null;
		covariances=false;
		hasViews= false;
		hasSMCVariances=true;

		messageSuffix= ". Means estimations. CI=("+alpha+","+delta+")";;
		xLabel=infoMultiQuery.getxVariableName();
		yLabel="Means estimations";

		//Init x
		ArrayList<Double> xList = infoMultiQuery.getX();
		this.x=new double[xList.size()];
		for(int i = 0; i < xList.size();i++){
			x[i]=xList.get(i);
		}

		//init data
		labelsAll = new String[infoMultiQuery.getNumberOfYsForEachX()];
		plotsAll= new double[labelsAll.length][];
		smcVariances = new double[labelsAll.length][];
		for(int query=0;query<labelsAll.length;query++){
			labelsAll[query]=infoMultiQuery.getLabel(query);
			plotsAll[query]=new double[x.length];
			smcVariances[query]=new double[x.length];
			for(int i = 0; i < x.length;i++){
				ArrayList<Double> yi = infoMultiQuery.getY(i);
				ArrayList<Double> yVari = infoMultiQuery.getYVar(i);
				plotsAll[query][i]=yi.get(query);
				smcVariances[query][i]=yVari.get(query);
			}
		}

	}

}
