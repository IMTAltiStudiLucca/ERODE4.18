package it.imt.erode.partitionrefinement.algorithms.corn;

import java.io.BufferedWriter;
import java.math.BigDecimal;
//import java.math.MathContext;
//import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.auxiliarydatastructures.CRNandPartition;
import it.imt.erode.auxiliarydatastructures.IPartitionAndBoolean;
import it.imt.erode.auxiliarydatastructures.Reduction;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.commandline.Terminator;
import it.imt.erode.crn.implementations.Composite;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.crn.label.EmptySetLabel;
import it.imt.erode.crn.label.ILabel;
import it.imt.erode.crn.label.MutableNAryLabel;
import it.imt.erode.crn.label.NAryLabelBuilder;
import it.imt.erode.partition.implementations.Partition;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.algorithms.CRNBisimulationsNAry;
import it.imt.erode.partitionrefinement.splittersbstandcounters.ISpeciesCounterHandler;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SpeciesSplayBST;
import it.imt.erode.utopic.MatlabODEPontryaginExporter;

public class CORN_LumpabilityForControlRN {

	//private static final BigDecimal DELTA_ABSOLUTE = BigDecimal.valueOf(0.1);
	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
	private static final BigDecimal TWO = BigDecimal.valueOf(2);
	private static final BigDecimal ZERO = BigDecimal.ZERO;
	//private static final MathContext MC = new MathContext(CRNBisimulationsNAry.getSCALE(), RoundingMode.HALF_DOWN);

	private HashMap<ICRNReaction, BigDecimal> m,M;
	

	public static boolean isNumeric(String strNum) {
	    if (strNum == null) {
	        return false;
	    }
	    try {
	        Double.parseDouble(strNum);
	    } catch (NumberFormatException nfe) {
	        return false;
	    }
	    return true;
	}
	
	/**
	 * 
	 * @param crn
	 * @param partition the partition to be refined. It is not modified, but instead a new one is created and returned
	 * @param initializeCounters
	 * @param verbose
	 * @param terminator 
	 * @param absolutePertCoRN 
	 * @param percentagePertCoRN 
	 * @param hotToComputeMm 
	 * @param absoluteClosureCoRN 
	 * @param upperBoundFactorCoRN 
	 * @param lowerBoundFactorCoRN 
	 * @param certainConstants true if the constants rates should be considered certain. False otherwise
	 * @return
	 */
	public IPartitionAndBoolean computeCoarsest(ICRN crn, IPartition partition, boolean verbose,MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator,
			IMessageDialogShower msgDialogShower, 
			double percentagePertCoRN, double absolutePertCoRN, 
			double percentageClosureCoRN, double absoluteClosureCoRN, 
			double lowerBoundFactorCoRN, double upperBoundFactorCoRN, HowToComputeMm hotToComputeMm,
			boolean certainConstants,LinkedHashMap<String, String> extraColumnsForCSV
			){
		Reduction red=Reduction.CoRN;
		BigDecimal absolutePert=BigDecimal.valueOf(absolutePertCoRN/2);
		
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,red.toString()+" Reducing: "+crn.getName());
		}
		IPartition obtainedPartition = partition.copy();

		if(!crn.isMassAction()){
			CRNandPartition crnAndSpeciesAndPartition=MatlabODEPontryaginExporter.computeRNEncoding(crn, out, bwOut, partition,true);
			if(crnAndSpeciesAndPartition==null){
				CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it is not a mass action CRN (i.e., it has reactions with arbitrary rates). I terminate.");
				return new IPartitionAndBoolean(obtainedPartition, false);
			}
			crn = crnAndSpeciesAndPartition.getCRN();
			if(crn==null){
				CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it is not a mass action CRN (i.e., it has reactions with arbitrary rates). I terminate.");
				return new IPartitionAndBoolean(obtainedPartition, false);
			}
			obtainedPartition=crnAndSpeciesAndPartition.getPartition();
		}
		else if(crn.isSymbolic()){
			CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it contains symbolic parameters (i.e. parameters with no acutal value assigned) I terminate.");
			return new IPartitionAndBoolean(obtainedPartition, false);
		}
		if(!(red.equals(Reduction.CoRN))) {
			CRNReducerCommandLine.printWarning(out,bwOut,"Please invoke this method using CoRN.  I terminate.");
			return new IPartitionAndBoolean(obtainedPartition, false);
		}

		if(verbose){
			String blocks = " blocks";
			if(obtainedPartition.size()==1){
				blocks = " block";
			}
			CRNReducerCommandLine.println(out,bwOut,"Before "+red.toString()+" partitioning we have "+ obtainedPartition.size() + blocks +" and "+crn.getSpeciesSize()+ " species.");
		}
		
		//ICRN crnToConsider=crn;
		m=new HashMap<>(crn.getReactions().size());
		M=new HashMap<>(crn.getReactions().size());
		HashMap<ICRNReaction, BigDecimal> mMcenter=new HashMap<>(crn.getReactions().size());
		long begin = System.currentTimeMillis();
		if(hotToComputeMm.equals(HowToComputeMm.ABSEPSCLOSURE)||hotToComputeMm.equals(HowToComputeMm.PERCEPSCLOSURE)) {
			CRNReducerCommandLine.print(out,bwOut,"\n\tComputing m and M closing reaction kinetics up to ");
			if(hotToComputeMm.equals(HowToComputeMm.ABSEPSCLOSURE)) {
				CRNReducerCommandLine.print(out,bwOut,absoluteClosureCoRN+"...");
			}
			else {
				CRNReducerCommandLine.print(out,bwOut,percentageClosureCoRN+"%...");
			}
			
//			try {
//				crnToConsider=CRN.copyCRN(crn, out, bwOut,true,true,false);
//			} catch (IOException e) {
//				CRNReducerCommandLine.println(out,bwOut,"Problems in homogeneizing parameters. I terminate");
//				return new IPartitionAndBoolean(obtainedPartition, false);
//			}
			ArrayList<ArrayList<ICRNReaction>> reactionsWithEpsEquivalentRates = performClosureAndComputemM(crn, percentageClosureCoRN,
					absoluteClosureCoRN, hotToComputeMm, m, M,mMcenter);
			
			long end = System.currentTimeMillis();
			CRNReducerCommandLine.println(out,bwOut," completed in "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)"+".");
			CRNReducerCommandLine.print(out,bwOut,"\t  The kinetics of the "+crn.getReactions().size()+" reactions have been grouped in "+reactionsWithEpsEquivalentRates.size()+" blocks.");
		}
		else { 

			if(hotToComputeMm.equals(HowToComputeMm.ABSPERT)){
				CRNReducerCommandLine.print(out,bwOut,"\n\tComputing m and M with intervals of size "+absolutePertCoRN+" around each uncertain reaction kinetics.");
			}else if(hotToComputeMm.equals(HowToComputeMm.PERCPERT)){
				CRNReducerCommandLine.print(out,bwOut,"\n\tComputing m and M with intervals of size "+percentagePertCoRN+"% around each uncertain reaction kinetics.");
			} else if(hotToComputeMm.equals(HowToComputeMm.LowerUpperBoundRatios)){
				CRNReducerCommandLine.print(out,bwOut,"\n\tComputing m and M with intervals ["+lowerBoundFactorCoRN+"*k,"+upperBoundFactorCoRN+"*k] for each reaction kinetics k.");
			}

			BigDecimal lowerBoundFactorCoRNBD=BigDecimal.valueOf(lowerBoundFactorCoRN);
			BigDecimal upperBoundFactorCoRNBD=BigDecimal.valueOf(upperBoundFactorCoRN);
			
			for(ICRNReaction r:crn.getReactions()) {
				BigDecimal Mr=ZERO;
				BigDecimal mr=ZERO;
				if(certainConstants && isNumeric(r.getRateExpression())) {
					Mr = r.getRate();
					mr = r.getRate();
				}
				else {
					if(hotToComputeMm.equals(HowToComputeMm.PERCPERT)) {
						//perturb by percentage DELTA
						//BigDecimal oneTenth=divideWithScale(r.getRate(), DELTA_PERCENTAGE);//here I add/remove 10%
						double perc=(percentagePertCoRN/2)/100.0;


						BigDecimal percentageRate=r.getRate().multiply(BigDecimal.valueOf(perc));//divideWithScale(r.getRate(), BigDecimal.valueOf(percentagePertCoRN));
						percentageRate=percentageRate.abs();//rates can be negative...
						Mr=r.getRate().add(percentageRate);
						mr=r.getRate().subtract(percentageRate);
					}
					else if(hotToComputeMm.equals(HowToComputeMm.ABSPERT)) {
						//perturb by absolute value DELTA
						//BigDecimal absolutePert=BigDecimal.valueOf(absolutePertCoRN/2);
						Mr=r.getRate().add(absolutePert);
						mr=r.getRate().subtract(absolutePert);
					}
					else if(hotToComputeMm.equals(HowToComputeMm.LowerUpperBoundRatios)) {
						Mr=r.getRate().multiply(upperBoundFactorCoRNBD);
						mr=r.getRate().multiply(lowerBoundFactorCoRNBD);
					}
				}
				m.put(r, mr);
				M.put(r, Mr);
				if(mr.compareTo(Mr)==0) {
					mMcenter.put(r, mr);
				}
				else {
					mMcenter.put(r, (mr.add(Mr)).divide(TWO));
				}
			}
			long end = System.currentTimeMillis();
			CRNReducerCommandLine.print(out,bwOut," completed in "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)"+".");
		}
		
		CRNReducerCommandLine.print(out,bwOut,"\n\tComputing FE on the centers of the bounds...");
		long beginfe=System.currentTimeMillis();
		HashMap<ISpecies, ISpeciesCounterHandler> speciesCountersHM=new HashMap<>();
		CRNBisimulationsNAry.refine(Reduction.FE,  crn, obtainedPartition, null,speciesCountersHM, terminator,out,bwOut,false,false,null,mMcenter);
		long endfe=System.currentTimeMillis();
		CRNReducerCommandLine.print(out,bwOut," completed in: "+String.format( CRNReducerCommandLine.MSFORMAT, ((endfe-beginfe)/1000.0) )+ " (s) with "+obtainedPartition.size()+" blocks.");
		
		begin = System.currentTimeMillis();

		boolean extraTab=false;
		boolean print=true;
		obtainedPartition=refine(red,crn,obtainedPartition,terminator,out,bwOut,extraTab,print
				/*,
				percentagePertCoRN, 
				absolutePertCoRN,
				m,M,
				lowerBoundFactorCoRN,upperBoundFactorCoRN,
				hotToComputeMm*/);

		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"The final partition:");
			CRNReducerCommandLine.println(out,bwOut,obtainedPartition);
		}
		
		if(extraColumnsForCSV!=null) {
			driverNodes_dn(out, bwOut, extraColumnsForCSV, obtainedPartition);
		}
		

		long end = System.currentTimeMillis();
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,red+" Partitioning completed. From "+ crn.getSpeciesSize() +" species to "+ obtainedPartition.size() + " blocks. Time necessary: "+(end-begin)+ " (ms)");
			CRNReducerCommandLine.println(out,bwOut,"");
		}
		return new IPartitionAndBoolean(obtainedPartition,true);
	}

	public static void driverNodes_dn(MessageConsoleStream out, BufferedWriter bwOut,
			LinkedHashMap<String, String> extraColumnsForCSV, IPartition obtainedPartition) {
		int blocks_with_drivers=0;
		int driver_nodes=0;
		IBlock current =obtainedPartition.getFirstBlock();
		while(current!=null) {
			if(current.getSpecies().iterator().next().getName().endsWith("_dn")) {
				blocks_with_drivers++;
				driver_nodes+=current.getSpecies().size();
			}
			current=current.getNext();
		}
		CRNReducerCommandLine.print(out,bwOut,"\n\tThe "+driver_nodes+" driver nodes have been partitioned in "+blocks_with_drivers+" blocks (assuming that the initial partition separated driver/non-driver nodes).");
		extraColumnsForCSV.put("driver_nodes", String.valueOf(driver_nodes));
		extraColumnsForCSV.put("blocks_driver_nodes", String.valueOf(blocks_with_drivers));
	}

	private ArrayList<ArrayList<ICRNReaction>> performClosureAndComputemM(ICRN crn, double percentageClosureCoRN,
			double absoluteClosureCoRN, HowToComputeMm hotToComputeMm, HashMap<ICRNReaction, BigDecimal> m,
			HashMap<ICRNReaction, BigDecimal> M, HashMap<ICRNReaction, BigDecimal> mMcenter) {
		ArrayList<ICRNReaction> reactions=new ArrayList<>(crn.getReactions());
		Collections.sort(reactions, new MAReactionComparator_OnlyParams());
		
		ArrayList<ArrayList<ICRNReaction>> reactionsWithEpsEquivalentRates=new ArrayList<>();
		ArrayList<ICRNReaction> currentBlock=new ArrayList<>();
		currentBlock.add(reactions.get(0));
		reactionsWithEpsEquivalentRates.add(currentBlock);
		BigDecimal eps=new BigDecimal(absoluteClosureCoRN);
		BigDecimal ratioPerc=new BigDecimal(percentageClosureCoRN).divide(HUNDRED);
		BigDecimal prev=reactions.get(0).getRate();
		
		for(int i=1;i<reactions.size();i++) {
			ICRNReaction r=reactions.get(i);
			BigDecimal rate=r.getRate();
			BigDecimal limit;
			if(hotToComputeMm.equals(HowToComputeMm.ABSEPSCLOSURE)) {
				limit=prev.add(eps);
			}
			else {
				BigDecimal toAdd=prev.multiply(ratioPerc);
				toAdd=toAdd.abs();//rates can be negativer...
				limit=prev.add(toAdd);
			}
			
			if(limit.compareTo(rate)>=0) {
				//The last considered rate is eps-close to the current
				//do nothing
			}
			else {
				//The last considered rate is too far away.
				currentBlock=new ArrayList<>();
				reactionsWithEpsEquivalentRates.add(currentBlock);
			}
			currentBlock.add(r);
			prev=r.getRate();
		}
		
		for(ArrayList<ICRNReaction> block : reactionsWithEpsEquivalentRates) {
			BigDecimal mb=block.get(0).getRate();
			BigDecimal Mb=block.get(block.size()-1).getRate();
			for(ICRNReaction r:block) {
				m.put(r, mb);
				M.put(r, Mb);
				mMcenter.put(r, (mb.add(Mb)).divide(TWO));
			}
		}
		return reactionsWithEpsEquivalentRates;
	}

//	protected IPartition refine(Reduction red, ICRN crn, IPartition partition, Terminator terminator, MessageConsoleStream out, BufferedWriter bwOut, boolean extraTab, boolean print, double percentagePertCoRN, double absolutePertCoRN) {
//		return refine(red, crn, partition, terminator, out, bwOut, extraTab,print,percentagePertCoRN, absolutePertCoRN,null,null);
//	}

	//		protected static IPartition refine(Reduction red, ICRN crn, IPartition partition, Terminator terminator, MessageConsoleStream out, BufferedWriter bwOut, 
	//				boolean extraTab, boolean print, BigDecimal deltaHalf,HashMap<ICRNReaction, BigDecimal> reactionToRateToConsider) {
	//			return refine(red, crn, partition, terminator, out, bwOut, 
	//					extraTab, print, deltaHalf,reactionToRateToConsider,null,null);
	//		}

	/**
	 * 
	 * @param red 
	 * @param crn
	 * @param partition the partition to be refined. Note that the partition is modified, thus first invoke copy if you want to preserve it.
	 * @param multisetCoefficients 
	 * @param speciesCounters 
	 * @param terminator 
	 * @param bwOut 
	 * @param out 
	 * @param upperBoundFactorCoRN 
	 * @param lowerBoundFactorCoRN 
	 * @param computeMm 
	 */
	private IPartition refine(Reduction red, ICRN crn, IPartition partition, Terminator terminator, 
			MessageConsoleStream out, BufferedWriter bwOut, 
			boolean extraTab, boolean print
			/*, 
			double percentagePertCoRN, double absolutePertCoRN,
			HashMap<ICRNReaction, BigDecimal> m,HashMap<ICRNReaction, BigDecimal> M, double lowerBoundFactorCoRN, double upperBoundFactorCoRN, 
			HowToComputeMm computeMm*/
			//ArrayListOfReactions[] reactionsToConsiderForEachSpecies, HashMap<IComposite, BigDecimal> multisetCoefficients_DELETE
			) {
		String pref = "";
		if(extraTab) {
			pref="\t\t";
		}
		if(!(red.equals(Reduction.CoRN))) {
			CRNReducerCommandLine.printWarning(out,bwOut,"This method should be invoked with Reduction.CoRN as first parameter.");
		}
		//			if(speciesCounters!=null && speciesCountersHM!=null) {
		//				CRNReducerCommandLine.printWarning(out,bwOut,"Only one out of speciesCounters and speciesCountersHM should be NOT NULL.");
		//			}


		if(print) {
			CRNReducerCommandLine.print(out,bwOut,"\n"+pref+"\tScanning the reactions once to pre-compute informations for the partitioning ... ");
		}
		long begin = System.currentTimeMillis();		

		//			HashMap<ICRNReaction,Integer> consideredAtIteration=null;
		//			if(reactionsToConsiderForEachSpecies==null) {
		//				reactionsToConsiderForEachSpecies =  new ArrayListOfReactions[crn.getSpeciesSize()];
		//				if(red.equals(Reduction.CoRN)) {
		//					multisetCoefficients = new HashMap<IComposite, BigDecimal>();
		//					CRNBisimulationsNAry.addToReactionsWithNonZeroStoichiometryAndComputeMultisetCoefficients(crn.getReactions(), terminator, reactionsToConsiderForEachSpecies, multisetCoefficients, crn.getMaxArity());
		//				}
		//	//			else if(red.equals(Reduction.BE)) {
		//	//				CRNBisimulations.addToOutgoingReactionsOfReagents(crn.getReactions(), reactionsToConsiderForEachSpecies);
		//	//				consideredAtIteration = new HashMap<>(crn.getReactions().size());
		//	//			}
		//			}

		//For multisetCoefficients
		int[] factorials = CRNBisimulationsNAry.computeFactorials(crn.getMaxArity());
		HashMap<IComposite, BigDecimal> multisetCoefficients = new HashMap<IComposite, BigDecimal>();
		//For the algorithm
		HashMap<IComposite, ArrayList<ICRNReaction>> reagentsToReactions=new HashMap<>(crn.getSpeciesSize());
		HashMap<ISpecies, HashSet<IComposite>> speciesToReagentsContainingIt=new HashMap<>(crn.getSpeciesSize());
		LinkedHashMap<ICRNReaction, BigDecimal> reactionToVrFirstMult=new LinkedHashMap<>(crn.getReactions().size());
		LinkedHashMap<ICRNReaction, BigDecimal> reactionToCentreFirstMult=new LinkedHashMap<>(crn.getReactions().size());
		scanReactionsForPrecomputation(crn, factorials, multisetCoefficients, reagentsToReactions,
				speciesToReagentsContainingIt, reactionToVrFirstMult, reactionToCentreFirstMult
				/*,
				percentagePertCoRN,
				absolutePertCoRN,
				M,m,
				lowerBoundFactorCoRN,upperBoundFactorCoRN,
				computeMm*/);


		long end = System.currentTimeMillis();
		if(print) {
			CRNReducerCommandLine.print(out,bwOut,"completed in "+ String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0))+" (s)");
		}

		begin =System.currentTimeMillis();
		Integer iteration=1;
		if(print) {
			CRNReducerCommandLine.print(out,bwOut,"\n"+pref+"\tPerforming the actual "+red+" partition refinement ... ");
		}

		int prevSize = partition.size();
		do {
			prevSize=partition.size();
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			//partition.initializeAllBST();

			IPartition refinedPartition = new Partition(crn.getSpecies().size());
			IBlock current = partition.getFirstBlock();
			while(current!=null) {
				splitBlock(current, partition, refinedPartition, speciesToReagentsContainingIt,reagentsToReactions,reactionToVrFirstMult,reactionToCentreFirstMult);
				current=current.getNext();
			}

			//				if(speciesCountersHM!=null) {
			//					speciesCountersHM=new HashMap<>();
			//				}
			iteration++;
			partition=refinedPartition;
		}while(partition.size()!=prevSize);

		end =System.currentTimeMillis();
		if(print) {
			CRNReducerCommandLine.print(out,bwOut,iteration+" iterations of the while true performed ... ");
			CRNReducerCommandLine.print(out,bwOut,"completed in "+ String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0))+" (s)");
		}
		return partition;
	}

	private void scanReactionsForPrecomputation(ICRN crn, int[] factorials,
			HashMap<IComposite, BigDecimal> multisetCoefficients,
			HashMap<IComposite, ArrayList<ICRNReaction>> reagentsToReactions,
			HashMap<ISpecies, HashSet<IComposite>> speciesToReagentsContainingIt,
			LinkedHashMap<ICRNReaction, BigDecimal> reactionToVrFirstMult,
			LinkedHashMap<ICRNReaction, BigDecimal> reactionToCentreFirstMult
			/*, 
			double percentagePertCoRN, double absolutePertCoRN, HashMap<ICRNReaction, BigDecimal> M, HashMap<ICRNReaction, BigDecimal> m, double lowerBoundFactorCoRN, double upperBoundFactorCoRN, HowToComputeMm computeMm
			*/) {
		
		/*
		BigDecimal lowerBoundFactorCoRNBD=BigDecimal.valueOf(lowerBoundFactorCoRN);
		BigDecimal upperBoundFactorCoRNBD=BigDecimal.valueOf(upperBoundFactorCoRN);
		*/
		
		for(ICRNReaction r : crn.getReactions()) {
			CRNBisimulationsNAry.extractMultisetCoefficients(multisetCoefficients, factorials, r);
			IComposite reagents=r.getReagents();
			ArrayList<ICRNReaction> prev = reagentsToReactions.get(reagents);
			if(prev==null) {
				prev=new ArrayList<>();
				reagentsToReactions.put(reagents, prev);
			}
			prev.add(r);

			for(int s=0;s< reagents.getNumberOfDifferentSpecies();s++) {
				ISpecies reagent=reagents.getAllSpecies(s);
				HashSet<IComposite> prevReag = speciesToReagentsContainingIt.get(reagent);
				if(prevReag==null) {
					prevReag=new HashSet<>();
					speciesToReagentsContainingIt.put(reagent,prevReag);
				}
				prevReag.add(reagents);
			}

			computeVRandCentreFR_FirstMultiplicand(r, multisetCoefficients,reactionToVrFirstMult,reactionToCentreFirstMult
					/*,
					percentagePertCoRN,absolutePertCoRN,M,m,lowerBoundFactorCoRNBD,upperBoundFactorCoRNBD, computeMm
					*/);
			//reactionToVrFirstMult.put(r, computeVRFirstMultiplicand(r, multisetCoefficients));
			//reactionToCentreFirstMult.put(r, computeContreFRFirstMultiplicand(r, multisetCoefficients));

		}
		
	}

	private static void splitBlock(IBlock block, IPartition partition,IPartition newPartition,
			HashMap<ISpecies, HashSet<IComposite>> speciesToReagentsContainingIt,
			HashMap<IComposite, ArrayList<ICRNReaction>> reagentsToReactions,
			LinkedHashMap<ICRNReaction, BigDecimal> reactionToVrFirstMult, 
			LinkedHashMap<ICRNReaction, BigDecimal> reactionToCentreFirstMult) {
		SpeciesSplayBST<BigDecimal, IBlock> subblocks = block.getBST();
		for(ISpecies species : block.getSpecies()) {
			boolean found=false;
			int sb=1;
			for(IBlock subblock:subblocks.getBlocks()) {
				found=isEquation4True(species, subblock.getRepresentative(), partition, speciesToReagentsContainingIt, reagentsToReactions, reactionToVrFirstMult, reactionToCentreFirstMult);
				if(found) {
					//I add the new species to an existing subblock
					subblocks.put(BigDecimal.valueOf(sb), species, newPartition);
					break;
				}
				sb++;
			}
			if(!found) {
				//I add the species to a new subblock
				subblocks.put(BigDecimal.valueOf(sb), species, newPartition);
			}
		}
	}

	private static boolean isEquation4True(ISpecies Si,ISpecies Sj,IPartition partition, 
			HashMap<ISpecies, HashSet<IComposite>> speciesToReagentsContainingIt,
			HashMap<IComposite, ArrayList<ICRNReaction>> reagentsToReactions,
			LinkedHashMap<ICRNReaction, BigDecimal> reactionToVrFirstMult, 
			LinkedHashMap<ICRNReaction, BigDecimal> reactionToCentreFirstMult) {

		HashSet<IComposite> reagsWithSi=speciesToReagentsContainingIt.get(Si);
		HashSet<IComposite> reagsWithSj=speciesToReagentsContainingIt.get(Sj);
//		if( (Si.getName().equals("S14")&&Sj.getName().equals("S12"))||
//			(Si.getName().equals("S12")&&Sj.getName().equals("S14"))) {
//			System.out.println("Ciao");
//		}

		if(nullOrEmpty(reagsWithSi)&&nullOrEmpty(reagsWithSj)) {
			//Neither of them appear as reagents 
			return true;
		}
		else if(nullOrEmpty(reagsWithSi) || nullOrEmpty(reagsWithSj)) {
			//System.out.println("Precsely one among "+Si+" or "+Sj.getName()+" does not appear as reagents. I do not return false because the one with reactions might get fr=0");
			//One might think that I should return false because only one of them does not appear as reagents.
			//However, it might be that the reactions give fr=0.
			//DO NOTHING//return false;
		}

		HashSet<ILabel> consideredLabels=new HashSet<>();
		boolean isTrue=true;
		if(reagsWithSi!=null) {
			//First I extract all labels rho'' from reactions Si+rho'' and I compare fr(Si+rho'') vs fr(Sj+rho'')
			isTrue=checkEquation4OnAllRhosOfFirst(reagsWithSi, consideredLabels, partition, Si, Sj, reagentsToReactions, reactionToVrFirstMult, reactionToCentreFirstMult);
			if(!isTrue) {
				return false;
			}
		}
		if(reagsWithSj!=null) {
			isTrue=checkEquation4OnAllRhosOfFirst(reagsWithSj, consideredLabels, partition, Sj, Si, reagentsToReactions, reactionToVrFirstMult, reactionToCentreFirstMult);
		}


		//If I arrive here, then isTrue tells us whether Eq. 4 is satisfied by Si and Sj.
		return isTrue;

	}

	private static boolean checkEquation4OnAllRhosOfFirst(HashSet<IComposite> reagsWithSi,HashSet<ILabel> consideredLabels,IPartition partition,
			ISpecies Si,ISpecies Sj,
			HashMap<IComposite, ArrayList<ICRNReaction>> reagentsToReactions,
			LinkedHashMap<ICRNReaction, BigDecimal> reactionToVrFirstMult, 
			LinkedHashMap<ICRNReaction, BigDecimal> reactionToCentreFirstMult
			) {
		if(reagsWithSi!=null) {
			for(IComposite reagents:reagsWithSi) {
				if(reagents.isUnary()) {
					if(consideredLabels.contains(EmptySetLabel.EMPTYSETLABEL)) {
						//do nothing: I already considered rho=empty
					}
					else {
						consideredLabels.add(EmptySetLabel.EMPTYSETLABEL);
						//I have to compare fr(Si) with fr(Sj)
						IComposite rho = (IComposite)Si;
						IComposite rhop = (IComposite)Sj;
						boolean equal=areEqual_GeneratorselementwiseAbs_Centers(partition, reagentsToReactions, reactionToVrFirstMult,
								reactionToCentreFirstMult, rho, rhop);
						if(!equal) {
							return false;
						}
					}
				}
				else {
					NAryLabelBuilder nAryLabel = new NAryLabelBuilder((Composite)reagents);
					nAryLabel.setSpeciesToDecrease(reagents.getPosOfSpecies(Si));
					MutableNAryLabel label=(MutableNAryLabel)nAryLabel.getObtainedLabel();
					if(consideredLabels.contains(label)) {
						//do nothing
						nAryLabel.resetSpeciesToDecrease();
					}
					else {
						consideredLabels.add(label);
						IComposite rho  = label.toCompositeAddingSpecies(Si);
						IComposite rhop = label.toCompositeAddingSpecies(Sj);
						boolean equal=areEqual_GeneratorselementwiseAbs_Centers(partition, reagentsToReactions, reactionToVrFirstMult,
								reactionToCentreFirstMult, rho, rhop);
						nAryLabel.resetSpeciesToDecrease();
						if(!equal) {
							return false;
						}
					}

				}
			}
		}

		//I could not decide yet whether the two species satisfy Equation 4.
		return true;
	}

	private static boolean nullOrEmpty(Collection<IComposite> coll) {
		return coll==null || coll.size()==0;
	}

	private static boolean areEqual_GeneratorselementwiseAbs_Centers(IPartition partition,
			HashMap<IComposite, ArrayList<ICRNReaction>> reagentsToReactions,
			LinkedHashMap<ICRNReaction, BigDecimal> reactionToVrFirstMult,
			LinkedHashMap<ICRNReaction, BigDecimal> reactionToCentreFirstMult, IComposite rho, IComposite rhop) {

		ArrayList<ICRNReaction> Rrho = reagentsToReactions.get(rho);

		LinkedHashSet<ArrayList<BigDecimal>> generators_fr_rho = new LinkedHashSet<>(0);
		if(Rrho!=null && Rrho.size()>0) {
			generators_fr_rho = computeGeneratorsOfFR(rho ,Rrho , reactionToVrFirstMult, partition);
		}
		else {
			Rrho = new ArrayList<>(0);
		}
		ArrayList<ICRNReaction> Rrhop = reagentsToReactions.get(rhop);
		LinkedHashSet<ArrayList<BigDecimal>> generators_fr_rhop = new LinkedHashSet<>(0);
		if(Rrhop!=null && Rrhop.size()>0) {
			generators_fr_rhop = computeGeneratorsOfFR(rhop,Rrhop, reactionToVrFirstMult, partition);
		}
		else {
			Rrhop = new ArrayList<>(0);
		}

		boolean equal=areFRGeneratorsEqualUpToElementwiseAbs(generators_fr_rho,generators_fr_rhop);
		if(!equal) {
			return false;
		}
		else {
			//The generatars are equal. I now compare the centers.
			ArrayList<BigDecimal> c =computeCentreOfFR(rho , Rrho , reactionToCentreFirstMult, partition);
			ArrayList<BigDecimal> cp=computeCentreOfFR(rhop, Rrhop, reactionToCentreFirstMult, partition);
			boolean equalCenters=equalVectorsUpToElementwiseAbs(c,cp,false);
			if(!equalCenters) {
				return false;
			}
		}
		return true;
	}
	
	/*
	private static boolean areEqual_Centers_GeneratorselementwiseAbs(IPartition partition,
			HashMap<IComposite, ArrayList<ICRNReaction>> reagentsToReactions,
			LinkedHashMap<ICRNReaction, BigDecimal> reactionToVrFirstMult,
			LinkedHashMap<ICRNReaction, BigDecimal> reactionToCentreFirstMult, IComposite rho, IComposite rhop) {

		ArrayList<ICRNReaction> Rrho = reagentsToReactions.get(rho);
		ArrayList<ICRNReaction> Rrhop = reagentsToReactions.get(rhop);
		
		
		//I start comparing the centers (it takes less time than the generators
		//The generatars are equal. I now compare the centers.
		ArrayList<BigDecimal> c =computeCentreOfFR(rho , Rrho , reactionToCentreFirstMult, partition);
		ArrayList<BigDecimal> cp=computeCentreOfFR(rhop, Rrhop, reactionToCentreFirstMult, partition);
		boolean equalCenters=equalVectorsUpToElementwiseAbs(c,cp,false);
		if(!equalCenters) {
			//The centers are different. I can already return false	
			return false;
		}
		else {
			//The centers are equal. I have to compare the generators
			LinkedHashSet<ArrayList<BigDecimal>> generators_fr_rho = new LinkedHashSet<>(0);
			if(Rrho!=null && Rrho.size()>0) {
				generators_fr_rho = computeGeneratorsOfFR(rho ,Rrho , reactionToVrFirstMult, partition);
			}
			else {
				Rrho = new ArrayList<>(0);
			}
			
			LinkedHashSet<ArrayList<BigDecimal>> generators_fr_rhop = new LinkedHashSet<>(0);
			if(Rrhop!=null && Rrhop.size()>0) {
				generators_fr_rhop = computeGeneratorsOfFR(rhop,Rrhop, reactionToVrFirstMult, partition);
			}
			else {
				Rrhop = new ArrayList<>(0);
			}

			boolean equalGenerators=areFRGeneratorsEqualUpToElementwiseAbs(generators_fr_rho,generators_fr_rhop);
			return equalGenerators;
		}
	}
	*/

	private static boolean areFRGeneratorsEqualUpToElementwiseAbs(
			LinkedHashSet<ArrayList<BigDecimal>> generators_fr_1,
			LinkedHashSet<ArrayList<BigDecimal>> generators_fr_2) {
		if(generators_fr_1.size()<generators_fr_2.size()) {
			return false;
		}

		//This is very inefficient. 
		//I can 
		//	1) apply abs() to all their values
		//	2) transform vi and vpj in sets;
		//	3) use a data structure with hashcode to quickly compare the two sets? 
		for(ArrayList<BigDecimal> vi : generators_fr_1) {
			boolean found=false;
			for(ArrayList<BigDecimal> vpj : generators_fr_2) {
				if(equalVectorsUpToElementwiseAbs(vi,vpj,true)) {
					found=true;
					break;
				}
			}
			if(!found) {
				return false;
			}
		}
		//If I arrive here, then for all vectors in the first we found one equal up to element-wise abs in the second
		return true;
	}

	private static boolean equalVectorsUpToElementwiseAbs(ArrayList<BigDecimal> vi, ArrayList<BigDecimal> vpj,boolean considerMinus) {
		if(vi.size()!=vpj.size()) {
			return false;
		}
		for(int k=0;k<vi.size();k++) {
			BigDecimal vi_k=vi.get(k);
			BigDecimal vpj_k=vpj.get(k);
			boolean equal= cmpWithTol(vi_k, vpj_k) ==0;
			if(!equal) {
				if(considerMinus) {
					BigDecimal minusvpj_k=ZERO.subtract(vpj_k);
					equal= cmpWithTol(vi_k,minusvpj_k)==0;
					if(!equal) {
						return false;
					}
				}
				else {
					return false;
				}
			}
			//If I arrive here it means that the abs of the current elements of the vectors are equal
			//I move to the next entry
		}

		//If I arrive here, the two vectors are equal modulo abs
		return true;
	}


	private static LinkedHashSet<ArrayList<BigDecimal>> computeGeneratorsOfFR(
			IComposite rho,ArrayList<ICRNReaction> Rrho,
			LinkedHashMap<ICRNReaction, BigDecimal> reactionToVrFirstMult,IPartition partition) {
		//LinkedHashSet<LinkedHashMap<IBlock, BigDecimal>> generators = new LinkedHashSet<>();


		ArrayList<ArrayList<BigDecimal>>generatorsList=new ArrayList<>(Rrho.size());
		for(ICRNReaction r : Rrho) {
			//LinkedHashMap<IBlock, BigDecimal> vr 
			ArrayList<BigDecimal> vr = computeVR(r, reactionToVrFirstMult, partition);
			if(!allZeros(vr)) {
				generatorsList.add(vr);
			}
		}

		for(int i=0;i<generatorsList.size();i++) {
			ArrayList<BigDecimal> wi = generatorsList.get(i);
			for(int j=i+1;j<generatorsList.size();j++) {
				ArrayList<BigDecimal> wj = generatorsList.get(j);
				BigDecimal lambda = areLinearDependent(wi,wj);
				if(cmpWithTol(lambda, ZERO)!=0) {
					//linear dependent
					//1 replace wi with wi+l/abs(l) wj 
					wi=combine(wi,wj,lambda);
					generatorsList.set(i, wi);
					//2 replace wj with 0;
					generatorsList.remove(j);
					j--;//I don't want to change j. Therefore I decrease it here, and I increase it in the for
				}
				else {
					//linear independent
					//do nothing
				}
			}
		}

		LinkedHashSet<ArrayList<BigDecimal>> generators = new LinkedHashSet<>(generatorsList);


		return generators;

	}

	private static boolean allZeros(ArrayList<BigDecimal> vr) {
		for(BigDecimal bd : vr) {
			if(cmpWithTol(bd, ZERO)!=0) {
				return false;
			}
		}
		return true;
	}

	private static ArrayList<BigDecimal> combine(ArrayList<BigDecimal> wi, ArrayList<BigDecimal> wj,
			BigDecimal lambda) {
		boolean add=true;
		if(cmpWithTol(lambda, ZERO)<0) {
			add=false;
		}
		ArrayList<BigDecimal> wnew=new ArrayList<>(wi.size());
		for(int k=0;k<wi.size();k++) {
			if(add) {
				wnew.add(wi.get(k).add(wj.get(k)));
			}
			else {
				wnew.add(wi.get(k).subtract(wj.get(k)));
			}			
		}
		return wnew;
	}

	/**
	 * 
	 * @param wi
	 * @param wj
	 * @return a non-zero lambda if the two are dependent. Zero if they are independent
	 */
	private static BigDecimal areLinearDependent(ArrayList<BigDecimal> wi, ArrayList<BigDecimal> wj) {
		if(wi.size()!=wj.size() || wi.size()==0 || wj.size()==0) {
			throw new UnsupportedOperationException("wi and wj should have same size (of the current partition)");
		}

		//I first have to identify the correct lambda
		BigDecimal lambda=ZERO;
		int k=0;
		while(k<wi.size()){
			BigDecimal wik=wi.get(k);
			BigDecimal wjk=wj.get(k);
			if(cmpWithTol(wik, ZERO)==0 && cmpWithTol(wjk, ZERO)==0) {
				//the current entries are 0. I have to check the next one
			}
			else {
				if((cmpWithTol(wik, ZERO)!=0 && cmpWithTol(wjk, ZERO)==0) || 
						(cmpWithTol(wik, ZERO)==0 && cmpWithTol(wjk, ZERO)!=0)){
					//No such lambda exists!
					return ZERO;
				}
				else {
					lambda=divideWithScale(wik, wjk);
					break;
				}
			}
			k++;
		}
		if(k==wi.size()) {
			throw new UnsupportedOperationException("Both wi and wj contain only zeros. Is this supported?");
			//TODO if supported, I should just remove both before.
		}


		//Now I check if the ratio lambda holds for all entries. 
		for(;k<wi.size();k++) {
			BigDecimal wik=wi.get(k);
			BigDecimal wjk=wj.get(k);
			if(cmpWithTol(wik, ZERO)==0 && cmpWithTol(wjk, ZERO)==0) {
				//both entries have value 0. It should be just ok.
			}
			else if((cmpWithTol(wik, ZERO)==0 || cmpWithTol(wjk, ZERO)==0)){
				//Precisely one is 0. Lambda does not hold for this entry.
				return ZERO;
			}
			else {
				//Both values are non-zero
				BigDecimal lambda2 = divideWithScale(wik, wjk);;
				if(cmpWithTol(lambda, lambda2)!=0) {
					//Lambda does not hold for this entry.
					return ZERO;
				}
			}
		}

		return lambda;
	}

	private static ArrayList<BigDecimal> computeVR(ICRNReaction r,LinkedHashMap<ICRNReaction, BigDecimal> reactionToVrFirstMult, IPartition partition) {
		ArrayList<BigDecimal> vr=new ArrayList<>(partition.size());

		BigDecimal firstMultiplier = reactionToVrFirstMult.get(r);

		IBlock block = partition.getFirstBlock();
		while(block!=null) {
			BigDecimal secondMultiplier = BigDecimal.valueOf(computeRH(block, r));
			if(cmpWithTol(secondMultiplier, ZERO)==0) {
				vr.add(ZERO);	
			}
			else {
				BigDecimal vrH=firstMultiplier.multiply(secondMultiplier);
				//vr.put(block, vrH);
				vr.add(vrH);
			}
			block=block.getNext();
		}
		return vr;
	}

	private void computeVRandCentreFR_FirstMultiplicand(ICRNReaction r,HashMap<IComposite, BigDecimal> multisetCoefficients, 
			LinkedHashMap<ICRNReaction,BigDecimal> reactionToVrFirstMult, LinkedHashMap<ICRNReaction,BigDecimal> reactionToCentreFirstMult
			/*,
			double percentagePertCoRN, double absolutePertCoRN, 
			HashMap<ICRNReaction, BigDecimal> M, HashMap<ICRNReaction, BigDecimal> m,
			BigDecimal lowerBoundFactorCoRN, BigDecimal upperBoundFactorCoRN, HowToComputeMm computeMm
			*/){

		BigDecimal rhofact=BigDecimal.ONE;
		if(!r.isUnary()) {
			rhofact=multisetCoefficients.get(r.getReagents());
		}
		BigDecimal twoRhoFact=TWO.multiply(rhofact);	

//		boolean percentage=false;
//		if(percentagePertCoRN>=0) {
//			percentage=true;
//		}
		
		
		BigDecimal Mr=M.get(r);
		BigDecimal mr=m.get(r);
		/*
		if(computeMm.equals(HowToComputeMm.PERCPERT)) {
			//perturb by percentage DELTA
			//BigDecimal oneTenth=divideWithScale(r.getRate(), DELTA_PERCENTAGE);//here I add/remove 10%
			double perc=(percentagePertCoRN/2)/100.0;
			
			
			BigDecimal percentageRate=r.getRate().multiply(BigDecimal.valueOf(perc));//divideWithScale(r.getRate(), BigDecimal.valueOf(percentagePertCoRN));
			percentageRate=percentageRate.abs();//rates can be negative...
			Mr=r.getRate().add(percentageRate);
			mr=r.getRate().subtract(percentageRate);
		}
		else if(computeMm.equals(HowToComputeMm.ABSPERT)) {
			//perturb by absolute value DELTA
			BigDecimal absolutePert=BigDecimal.valueOf(absolutePertCoRN/2);
			Mr=r.getRate().add(absolutePert);
			mr=r.getRate().subtract(absolutePert);
		}
		else if(computeMm.equals(HowToComputeMm.LowerUpperBoundRatios)) {
			Mr=r.getRate().multiply(upperBoundFactorCoRN);
			mr=r.getRate().multiply(lowerBoundFactorCoRN);
		}
		else {
			//we have m and M.
			Mr=M.get(r);
			mr=m.get(r);
		}
		*/

		BigDecimal firstMultiplierVR= divideWithScale(Mr.subtract(mr), twoRhoFact);
		reactionToVrFirstMult.put(r, firstMultiplierVR);

		BigDecimal firstMultiplierCenterFR= divideWithScale(Mr.add(mr), twoRhoFact);
		reactionToCentreFirstMult.put(r, firstMultiplierCenterFR);
	}

	private static ArrayList<BigDecimal> computeCentreOfFR(IComposite rho, ArrayList<ICRNReaction> Rrho,
			LinkedHashMap<ICRNReaction, BigDecimal> reactionToCentreFirstMult, IPartition partition) {
		ArrayList<BigDecimal> centre = new ArrayList<>(partition.size());
		for(int b=0;b<partition.size();b++) {
			centre.add(ZERO);
		}

		for(ICRNReaction r : Rrho) {
			BigDecimal firstMultiplier = reactionToCentreFirstMult.get(r);
			IBlock block = partition.getFirstBlock();
			int b=0;
			while(block!=null) {
				BigDecimal secondMultiplier = BigDecimal.valueOf(computeRH(block, r));
				centre.set(b, centre.get(b).add(firstMultiplier.multiply(secondMultiplier)));
				b++;
				block=block.getNext();
			}
		}
		return centre;
	}

	private static int computeRH(IBlock block, ICRNReaction r) {
		int rH=0;
		for(ISpecies s: block.getSpecies()) {
			int netStoich = r.getProducts().getMultiplicityOfSpecies(s);
			netStoich -= r.getReagents().getMultiplicityOfSpecies(s);
			rH+=netStoich;
		}
		return rH;
	}
	
	private static int cmpWithTol(BigDecimal key1, BigDecimal key2){

		int cmp = key1.compareTo(key2);
		if(cmp==0){
			return cmp;
		}
		else{
			BigDecimal diff = key1.subtract(key2);
			diff=diff.abs();
			if(diff.compareTo(CRNBisimulationsNAry.getTolerance())<0){
				//System.out.println("equal up-to-tolerance: "+key1 +" "+key2);
				cmp=0;
			}    
			return cmp;
		}
	}

	private static BigDecimal divideWithScale(BigDecimal divisor, BigDecimal dividend){
		if(cmpWithTol(divisor, ZERO)==0 && cmpWithTol(dividend, ZERO)!=0) {
			return ZERO;
		}
		else {
			return divisor.divide(dividend, CRNBisimulationsNAry.getSCALE(),CRNBisimulationsNAry.RM);
		}
	}
//	private static BigDecimal multiplyWithScale(BigDecimal first, BigDecimal second){
//		if(cmpWithTol(first, ZERO)==0 || cmpWithTol(second, ZERO)==0) {
//			return ZERO;
//		}
//		else {
//			return first.multiply(second, MC);
//		}
//	}

}
