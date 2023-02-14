package it.imt.erode.partitionrefinement.splittersbstandcounters;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import it.imt.erode.crn.implementations.Species;
import it.imt.erode.crn.label.ILabel;
import it.imt.erode.crn.label.MutableNAryLabel;
import it.imt.erode.crn.label.NAryBlockLabel;
import it.imt.erode.partitionrefinement.algorithms.CRNBisimulationsNAry;

public class VectorOfBigDecimalsForEachLabel implements Comparable<VectorOfBigDecimalsForEachLabel> {

	private final ILabel[] keys;
	private final HashMap<ILabel, BigDecimal> labelAndBigDecimals;
	private static final ILabelComparator lc = new ILabelComparator();

	protected ILabel[] getKeys() {
		return keys;
	}
	
	protected BigDecimal getBigDecimal(ILabel label) {
		return labelAndBigDecimals.get(label);
	}


	public VectorOfBigDecimalsForEachLabel(HashMap<ILabel, BigDecimal> labelAndBigDecimals) {
		this.labelAndBigDecimals=labelAndBigDecimals;

		if(labelAndBigDecimals!=null) {
			keys = new ILabel[labelAndBigDecimals.keySet().size()]; 
			labelAndBigDecimals.keySet().toArray(keys);
			Arrays.sort(keys, new ILabelComparator());
		}
		else {
			keys = new ILabel[0];
		}
	}

	@Override
	public String toString() {
		return labelAndBigDecimals.toString();
	}

	/*
	public boolean atEpsilonDistance(VectorOfBigDecimalsForEachLabel o){
		ILabel[] myKeys = getKeys();
		ILabel[] oKeys = o.getKeys();
		
		int myi=0;
		int oi=0;
		while(myi<myKeys.length || oi<oKeys.length){
			int cmp = 0;
			
			//Check if in pos myi and oi we have same label or not.
			if(myi<myKeys.length && oi<oKeys.length){
				cmp = lc.compare(myKeys[myi], oKeys[oi]);
			}
			else{
				if(myi>=myKeys.length){
					cmp=1;
				}
				if(oi>=oKeys.length){
					cmp=-1;
				}
			}
					
			//If the current positions have same labels, I have to check their values
			if(cmp==0){
				ILabel label = myKeys[myi];//which is equal to oKeys[i] 
				BigDecimal bd = getBigDecimal(label);
				BigDecimal obd = o.getBigDecimal(label);
				BigDecimal diff = bd.subtract(obd).abs();
				if(diff.compareTo(CRNBisimulationsNAry.EPSILON)>0){
		        	return false;
		        }
				myi++;
				oi++;
			}
			else if(cmp<=0){
				//my label is smaller. I.e., I have a label that the other does not have. If my value is epsilon, we are in the epsilon distance for this label. Otherwise we are not.
				ILabel label = myKeys[myi];
				BigDecimal bd = getBigDecimal(label).abs();
				if(bd.compareTo(CRNBisimulationsNAry.EPSILON)>0){
					return false;
				}
				myi++;
			}
			else{ //cmp>0
				//the label of o is smaller. I.e., the other has a label that I do not have. If the value of the other is epsilon, we are in the epsilon distance for this label. Otherwise we are not.
				ILabel label = oKeys[oi];
				BigDecimal obd = o.getBigDecimal(label).abs();
				if(obd.compareTo(CRNBisimulationsNAry.EPSILON)>0){
					return false;
				}
				oi++;
			}
		}
		
//		for(int i=0;i<Math.min(myKeys.length, oKeys.length);i++){
//			int cmp = lc.compare(myKeys[i], oKeys[i]);
//			
//			//If the current positions have same labels, I have to check their values
//			if(cmp==0){
//				ILabel label = myKeys[i];//which is equal to oKeys[i] 
//				BigDecimal bd = getBigDecimal(label);
//				BigDecimal obd = o.getBigDecimal(label);
//				
//				BigDecimal diff = bd.subtract(obd).abs();
//				if(diff.compareTo(CRNBisimulationsNAry.EPSILON)>0){
//		        	return false;
//		        }
//			}
//			else{
//				
//			}
//		}
		
		
		return true;
	}
	
	*/
	/*
	public int compareToIgnoringEpsilonEntries(VectorOfBigDecimalsForEachLabel o) {
		ILabel[] myKeys = getKeys();
		ILabel[] oKeys = o.getKeys();
		//ILabelComparator lc = new ILabelComparator();
		
		int myi=0;
		int oi=0;
		while(myi<myKeys.length || oi<oKeys.length){
			//Skip epsilon-entries: entries with |values| < epsilon.
			if(myi<myKeys.length && getBigDecimal(myKeys[myi]).abs().compareTo(CRNBisimulationsNAry.EPSILON)<0){
				myi++;
				continue;
			}
			if(oi<oKeys.length && o.getBigDecimal(oKeys[oi]).abs().compareTo(CRNBisimulationsNAry.EPSILON)<0){
				oi++;
				continue;
			}
			
			
			int cmp = 0;
			
			//Check if in pos myi and oi we have same label or not.
			if(myi<myKeys.length && oi<oKeys.length){
				cmp = lc.compare(myKeys[myi], oKeys[oi]);
			}
			else{
				if(myi>=myKeys.length){
					cmp=1;
				}
				if(oi>=oKeys.length){
					cmp=-1;
				}
			}
			
			//If the current positions have same labels, I have to check their values
			if(cmp==0){
				ILabel label = myKeys[myi];//which is equal to oKeys[i] 
				BigDecimal bd = getBigDecimal(label);
				BigDecimal obd = o.getBigDecimal(label);
				BigDecimal diff = bd.subtract(obd).abs();
				if(diff.compareTo(CRNBisimulationsNAry.EPSILON)>0){
		        	return bd.compareTo(obd);
		        }
				myi++;
				oi++;
			}
			else if(cmp<=0){
				//my label is smaller. I.e., I have a label that the other does not have. If my value is epsilon, we are in the epsilon distance for this label. Otherwise we are not.
				ILabel label = myKeys[myi];
				BigDecimal bd = getBigDecimal(label).abs();
				if(bd.compareTo(CRNBisimulationsNAry.EPSILON)>0){
					return -1;
				}
				myi++;
			}
			else{ //cmp>0
				//the label of o is smaller. I.e., the other has a label that I do not have. If the value of the other is epsilon, we are in the epsilon distance for this label. Otherwise we are not.
				ILabel label = oKeys[oi];
				BigDecimal obd = o.getBigDecimal(label).abs();
				if(obd.compareTo(CRNBisimulationsNAry.EPSILON)>0){
					return 1;
				}
				oi++;
			}
		}

		//I scanned both arrays, and all entries were epsilon-equal 
		return 0;
	}
	*/
	
	@Override
	public int compareTo(VectorOfBigDecimalsForEachLabel o) {
		ILabel[] myKeys = getKeys();
		ILabel[] oKeys = o.getKeys();
		//ILabelComparator lc = new ILabelComparator();

		//Lexicographical order wrt order of the labels given by their IDs.
		for(int i=0;i<Math.min(myKeys.length, oKeys.length);i++){
			int cmp = lc.compare(myKeys[i], oKeys[i]);
			//If the current positions havesame labels, I have to check their values
			if(cmp==0){
				ILabel label = myKeys[i];//which is equal to oKeys[i] 
				/*if(getBigDecimal(label)==null){
					CRNReducerCommandLine.println("ciao");
				}
				if(o==null){
					CRNReducerCommandLine.println("ciao");
				}
				if(o.getBigDecimal(label)==null){
					CRNReducerCommandLine.println("ciao");
					o.getBigDecimal(label);
				}*/
				BigDecimal bd = getBigDecimal(label);
				BigDecimal obd = o.getBigDecimal(label);
				cmp = bd.compareTo(obd);
				
				if(CRNBisimulationsNAry.USETOLERANCE && cmp!=0){
					BigDecimal diff = bd.subtract(obd).abs();
					if(diff.compareTo(CRNBisimulationsNAry.getTolerance())<=0){
			        	//CRNReducerCommandLine.println("equal up-to-tolerance "+CRNBisimulationsNAry.TOLERANCE+": "+bd +" "+obd);
			        	cmp=0;
			        }
				}
			}
			if(cmp!=0){
				return cmp;
			}
		}
		//I completed to scan the arrays. All scanned entries have same labels and values. If the arrays have same size, the vectors are equal. Otherwise the shortest is the smallest
		return Integer.compare(myKeys.length, oKeys.length);
	}

	static class ILabelComparator implements Comparator<ILabel>
	{            
		public int compare(ILabel l1, ILabel l2)
		{
			if( canAssignAnID(l1) && canAssignAnID(l2) ){
				return Integer.compare(l1.getLabelID(), l2.getLabelID());
			}
			else{
				//Otherwise at least one of the two labels is a mutable nary label
				return deepCompare(l1, l2);
			}
		}
		
		/**
		 * This method is invoked when comparing one MutableNAryLabel with another label (empty, species, or another mutableNAryLabel)
		 * @param l1
		 * @param l2
		 * @return
		 */
		private int deepCompare(ILabel l1, ILabel l2){
			//Three ifs to cover the cases in which one of the two is the emptyset (no need for the first one)
			if(l1.getReferredArity()==1 && l2.getReferredArity()==1){
				return 0;
			}
			else if(l1.getReferredArity()==1 && l2.getReferredArity()>1){
				return -1;
			}
			else if(l1.getReferredArity()>1 && l2.getReferredArity()==1){
				return 1;
			}
			
			
			////////////////////////////////////////////////////////////////////////////
			//THIS SEQUENCE OF IF IS USED WHEN REDUCING UP TO NARY FORWARD BISIMULATIONS
			////////////////////////////////////////////////////////////////////////////
			//Two ifs to cover the cases in which one of the two is a species, and the other a not-actually unary MutableNAryLabel
			if(l1 instanceof Species && l2 instanceof MutableNAryLabel){
				//I know that I cannot assign an id to l2. Hence it is a not-actually unary MutableNAryLabel (refers to arity at most 3). I do lexicographical ordering. l2<>l1 if the first species of l2<> than l1. If the two are equal, then l1 is smaller.
				MutableNAryLabel mnl2=(MutableNAryLabel)l2;
				int cmp = Integer.compare(l1.getLabelID(), mnl2.getLabelIDOfFirstSpecies());
				if(cmp==0){
					return -1;
				}
				return cmp;
			}
			else if(l1 instanceof MutableNAryLabel && l2 instanceof Species){
				//I know that I cannot assign an id to l1. Hence it is a not-actually unary MutableNAryLabel (refers to arity at most 3). I do lexicographical ordering. l1<>l2 if the first species of l1<> than l2. If the two are equal, then l2 is smaller.
				MutableNAryLabel mnl1=(MutableNAryLabel)l1;
				int cmp = Integer.compare(mnl1.getLabelIDOfFirstSpecies(),l2.getLabelID());
				if(cmp==0){
					return 1;
				}
				return cmp;
			}
			else if(l1 instanceof MutableNAryLabel && l2 instanceof MutableNAryLabel){
				//I'm finally in the case in which l1 and l2 are both two not-actually unary MutableNAryLabel
				MutableNAryLabel mnl1=(MutableNAryLabel)l1;
				MutableNAryLabel mnl2=(MutableNAryLabel)l2;
				return mnl1.deepCompare(mnl2);
			}
			
			
			/////////////////////////////////////////////////////////////////////////////
			//THIS SEQUENCE OF IF IS USED WHEN REDUCING UP TO NARY BACKWARD BISIMULATIONS
			/////////////////////////////////////////////////////////////////////////////
			//Two ifs to cover the cases in which one of the two is a species, and the other a not-actually unary NAryBlockLabel
			if(l1 instanceof Species && l2 instanceof NAryBlockLabel){
				//I know that I cannot assign an id to l2. Hence it is a not-actually unary NAryBlockLabel (refers to arity at most 3). I do lexicographical ordering. l2<>l1 if the first species of l2<> than l1. If the two are equal, then l1 is smaller.
				NAryBlockLabel bl2=(NAryBlockLabel)l2;
				int cmp = Integer.compare(l1.getLabelID(), bl2.getLabelIDOfFirstSpecies());
				if(cmp==0){
					return -1;
				}
				return cmp;
			}
			else if(l1 instanceof NAryBlockLabel && l2 instanceof Species){
				//I know that I cannot assign an id to l1. Hence it is a not-actually unary NBlockAryLabel (refers to arity at most 3). I do lexicographical ordering. l1<>l2 if the first species of l1<> than l2. If the two are equal, then l2 is smaller.
				NAryBlockLabel bl1=(NAryBlockLabel)l1;
				int cmp = Integer.compare(bl1.getLabelIDOfFirstSpecies(),l2.getLabelID());
				if(cmp==0){
					return 1;
				}
				return cmp;
			}
			else if(l1 instanceof NAryBlockLabel && l2 instanceof NAryBlockLabel){
				//I'm finally in the case in which l1 and l2 are both two not-actually unary MutableNAryLabel
				NAryBlockLabel bl1=(NAryBlockLabel)l1;
				NAryBlockLabel bl2=(NAryBlockLabel)l2;
				return bl1.deepCompare(bl2);
			}
			
			//CRNReducerCommandLine.println("problem");
			
			throw new UnsupportedOperationException("Unsupported combination of labels. l1 ("+l1.getClass()+"):"+l1.toString()+", l2 ("+l2.getClass()+"):"+l2.toString());
			
			
		}
		
		public boolean canAssignAnID(ILabel l1){
			return l1.getReferredArity()==1 || l1.getReferredArity()==2;
			
			/*if(l1 instanceof EmptySetLabel || l1 instanceof Species){
				return true;
			}
			else if(l1 instanceof MutableNAryLabel){
				MutableNAryLabel mnl = (MutableNAryLabel)l1;
				if(mnl.isActuallyUnary()){
					return true;
				}
				else{
					return false;
				}
			}
			else if(l1 instanceof NAryBlockLabel){
				NAryBlockLabel bl = (NAryBlockLabel)l1;
				if(bl.getReferredArity()<3){
					return true;
				}
				else{
					return false;
				}
			}
			else{
				throw new UnsupportedOperationException("UnsupportedLabel");
			}*/
		}
	}

}
