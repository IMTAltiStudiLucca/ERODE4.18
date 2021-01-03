package it.imt.erode.partitionrefinement.splittersbstandcounters;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import it.imt.erode.crn.implementations.Composite;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.expression.parser.IMonomial;

/**
 * This class represents the monomials of the drift of a species. 
 * It associates a coefficient (a BigDecimal) to each product of species (represented as a composite). 
 * It offers a method to check if two drifts are at epsilon distance
 * @author Andrea Vandin
 *
 */
public class VectorOfCoefficientsForEachMonomial /*implements Comparable<VectorOfCoefficientsForEachMonomial> */{

	protected final IComposite[] keys;
	protected final HashMap<IComposite, BigDecimal> varaiblesToCoefficients;
	//private static final ILabelComparator lc = new ILabelComparator();

	protected IComposite[] getKeys() {
		return keys;
	}
	
	protected BigDecimal getCoefficient(IComposite variables) {
		return varaiblesToCoefficients.get(variables);
	}
	
	public VectorOfCoefficientsForEachMonomial() {
		varaiblesToCoefficients = new HashMap<>(0);
		keys = new IComposite[0];
	}
	public VectorOfCoefficientsForEachMonomial(HashMap<HashMap<ISpecies, Integer>, BigDecimal> hmVariablesToCoefficients) {
		varaiblesToCoefficients = new HashMap<>(hmVariablesToCoefficients.size());
		for (Entry<HashMap<ISpecies, Integer>, BigDecimal> entry : hmVariablesToCoefficients.entrySet()) {
			HashMap<ISpecies, Integer> hm = entry.getKey();
			IComposite composite = new Composite(hm);
			varaiblesToCoefficients.put(composite, entry.getValue());
		}
		keys=varaiblesToCoefficients.keySet().toArray(new IComposite[0]);
		Arrays.sort(keys);
	}
	public VectorOfCoefficientsForEachMonomial(ArrayList<IMonomial> monomials) {
		varaiblesToCoefficients = new HashMap<>(monomials.size());
		for (IMonomial monomial : monomials) {
			HashMap<ISpecies, Integer> hm = monomial.getOrComputeSpecies();
			IComposite composite = SpeciesOrComposite(hm);
			BigDecimal current = monomial.getOrComputeCoefficient();
			BigDecimal prev = varaiblesToCoefficients.put(composite, current);
			if(prev!=null) {
				varaiblesToCoefficients.put(composite, current.add(prev));
			}
		}
		keys=varaiblesToCoefficients.keySet().toArray(new IComposite[0]);
		Arrays.sort(keys);
	}
	
	public VectorOfCoefficientsForEachMonomial(LinkedHashMap<IComposite, BigDecimal> varToCoeffs) {
		varaiblesToCoefficients = varToCoeffs;
		keys=varaiblesToCoefficients.keySet().toArray(new IComposite[0]);
		Arrays.sort(keys);
	}

	@Override
	public String toString() {
		return varaiblesToCoefficients.toString();
	}

	public boolean atEpsilonDistance(VectorOfCoefficientsForEachMonomial o, BigDecimal epsilon){
		BigDecimal totalDistance = computeDistanceUpToEpsilon(o, epsilon);
		if(totalDistance==null || totalDistance.compareTo(epsilon)>0){
			return false;
		}
		else{
			return true;
		}
		/*BigDecimal totalDistance = BigDecimal.ZERO;
		
		IComposite[] myKeys = getKeys();
		IComposite[] oKeys = o.getKeys();
		
		int myi=0;
		int oi=0;
		while(myi<myKeys.length || oi<oKeys.length){
			int cmp = 0;
			
			//Check if in pos myi and oi we have same label or not.
			if(myi<myKeys.length && oi<oKeys.length){
				cmp = myKeys[myi].compareTo(oKeys[oi]);
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
				IComposite variables = myKeys[myi];//which is equal to oKeys[i] 
				BigDecimal bd = getCoefficient(variables);
				BigDecimal obd = o.getCoefficient(variables);
				BigDecimal diff = bd.subtract(obd).abs();
				totalDistance=totalDistance.add(diff);
				if(totalDistance.compareTo(epsilon)>0){
				//if(diff.compareTo(epsilon)>0){
		        	return false;
		        }
				myi++;
				oi++;
			}
			else if(cmp<=0){
				//my label is smaller. I.e., I have a label that the other does not have. If my value is epsilon, we are in the epsilon distance for this label. Otherwise we are not.
				IComposite variables = myKeys[myi];
				BigDecimal bd = getCoefficient(variables).abs();
				totalDistance=totalDistance.add(bd);
				if(totalDistance.compareTo(epsilon)>0){
				//if(bd.compareTo(epsilon)>0){
					return false;
				}
				myi++;
			}
			else{ //cmp>0
				//the label of o is smaller. I.e., the other has a label that I do not have. If the value of the other is epsilon, we are in the epsilon distance for this label. Otherwise we are not.
				IComposite variables = oKeys[oi];
				BigDecimal obd = o.getCoefficient(variables).abs();
				totalDistance=totalDistance.add(obd);
				if(totalDistance.compareTo(epsilon)>0){
				//if(obd.compareTo(epsilon)>0){
					return false;
				}
				oi++;
			}
		}
		
		return true;
		*/
	}

	/**
	 * Compute the distance between this object and o, if the distance is smaller than eps. If the distance is bigger than eps, then null is returned. 
	 * @param o
	 * @param epsilon
	 * @return
	 */
	public BigDecimal computeDistanceUpToEpsilon(VectorOfCoefficientsForEachMonomial o, BigDecimal epsilon) {

		BigDecimal totalDistance = BigDecimal.ZERO;
		
		IComposite[] myKeys = getKeys();
		//IComposite[] oKeys = o.getKeys();
		IComposite[] oKeys;
		if(o==null) {
			oKeys = new IComposite[0];
		}
		else {
			oKeys = o.getKeys();
		}
		
		int myi=0;
		int oi=0;
		while(myi<myKeys.length || oi<oKeys.length){
			int cmp = 0;
			
			//Check if in pos myi and oi we have same label or not.
			if(myi<myKeys.length && oi<oKeys.length){
				cmp = myKeys[myi].compareTo(oKeys[oi]);
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
				IComposite variables = myKeys[myi];//which is equal to oKeys[i] 
				BigDecimal bd = getCoefficient(variables);
				BigDecimal obd = o.getCoefficient(variables);
				BigDecimal diff = bd.subtract(obd).abs();
				totalDistance=totalDistance.add(diff);
				if(totalDistance.compareTo(epsilon)>0){
				//if(diff.compareTo(epsilon)>0){
		        	return null;
		        }
				myi++;
				oi++;
			}
			else if(cmp<=0){
				//my label is smaller. I.e., I have a label that the other does not have. If my value is epsilon, we are in the epsilon distance for this label. Otherwise we are not.
				IComposite variables = myKeys[myi];
				BigDecimal bd = getCoefficient(variables).abs();
				totalDistance=totalDistance.add(bd);
				if(totalDistance.compareTo(epsilon)>0){
				//if(bd.compareTo(epsilon)>0){
					return null;
				}
				myi++;
			}
			else{ //cmp>0
				//the label of o is smaller. I.e., the other has a label that I do not have. If the value of the other is epsilon, we are in the epsilon distance for this label. Otherwise we are not.
				IComposite variables = oKeys[oi];
				BigDecimal obd = o.getCoefficient(variables).abs();
				totalDistance=totalDistance.add(obd);
				if(totalDistance.compareTo(epsilon)>0){
				//if(obd.compareTo(epsilon)>0){
					return null;
				}
				oi++;
			}
		}
		
		return totalDistance;
	}


	/*
	@Override
	public int compareTo(VectorOfCoefficientsForEachMonomial o) {
		ILabel[] myKeys = getKeys();
		ILabel[] oKeys = o.getKeys();
		
		//Lexicographical order wrt order of the labels given by their IDs.
		for(int i=0;i<Math.min(myKeys.length, oKeys.length);i++){
			int cmp = lc.compare(myKeys[i], oKeys[i]);
			//If the current positions havesame labels, I have to check their values
			if(cmp==0){
				ILabel label = myKeys[i];//which is equal to oKeys[i] 
				BigDecimal bd = getBigDecimal(label);
				BigDecimal obd = o.getBigDecimal(label);
				cmp = bd.compareTo(obd);
				
				if(CRNBisimulationsNAry.USETOLERANCE && cmp!=0){
					BigDecimal diff = bd.subtract(obd).abs();
					if(diff.compareTo(CRNBisimulationsNAry.TOLERANCE)<=0){
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
	*/

	
	protected CoefficientToMonomialsPosNeg splitPositiveAndNegativeCoefficients() {
		LinkedHashMap<IComposite, BigDecimal> varaiblesToCoefficientsPos=new LinkedHashMap<IComposite, BigDecimal>(varaiblesToCoefficients.size());
		LinkedHashMap<IComposite, BigDecimal> varaiblesToCoefficientsNeg=new LinkedHashMap<IComposite, BigDecimal>(varaiblesToCoefficients.size());
		for(Entry<IComposite, BigDecimal> entry : varaiblesToCoefficients.entrySet()) {
			if(entry.getValue().compareTo(BigDecimal.ZERO)>0) {
				varaiblesToCoefficientsPos.put(entry.getKey(), entry.getValue());
			}
			else if(entry.getValue().compareTo(BigDecimal.ZERO)<0) {
				varaiblesToCoefficientsNeg.put(entry.getKey(), entry.getValue().abs());
			}
		}
		VectorOfCoefficientsForEachMonomial vPos = new VectorOfCoefficientsForEachMonomial(varaiblesToCoefficientsPos);
		VectorOfCoefficientsForEachMonomial vNeg = new VectorOfCoefficientsForEachMonomial(varaiblesToCoefficientsNeg);
		return new CoefficientToMonomialsPosNeg(vPos,vNeg);
	}
	
	private IComposite SpeciesOrComposite(HashMap<ISpecies, Integer> hm) {
		if(hm.size()==1 && hm.values().iterator().next()==1) {
			return (IComposite) hm.keySet().iterator().next();
		}
		return new Composite(hm);
	}

	public VectorOfCoefficientsForEachMonomial sum(VectorOfCoefficientsForEachMonomial o) {
		LinkedHashMap<IComposite, BigDecimal> varToCoeffs = new LinkedHashMap<>(varaiblesToCoefficients.size());
		if(o==null) {
			return this;
		}
		
		IComposite[] myKeys = getKeys();
		IComposite[] oKeys = o.getKeys();
		
		int myi=0;
		int oi=0;
		
		while(myi<myKeys.length || oi<oKeys.length){
			int cmp = 0;
			
			//Check if in pos myi and oi we have same label or not.
			if(myi<myKeys.length && oi<oKeys.length){
				cmp = myKeys[myi].compareTo(oKeys[oi]);
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
				IComposite variables = myKeys[myi];//which is equal to oKeys[i] 
				BigDecimal bd = getCoefficient(variables);
				BigDecimal obd = o.getCoefficient(variables);
				putNonZero(varToCoeffs,variables,bd.add(obd));//varToCoeffs.put(variables, bd.add(obd));
				myi++;
				oi++;
			}
			else if(cmp<=0){
				//my label is smaller. I.e., I have a label that the other does not have.
				IComposite variables = myKeys[myi];
				BigDecimal bd = getCoefficient(variables);
				putNonZero(varToCoeffs, variables, bd);//varToCoeffs.put(variables, bd);
				myi++;
			}
			else{ //cmp>0
				//the label of o is smaller. I.e., the other has a label that I do not have.
				IComposite variables = oKeys[oi];
				BigDecimal obd = o.getCoefficient(variables);
				putNonZero(varToCoeffs,variables,obd);//varToCoeffs.put(variables, obd);
				oi++;
			}
		}
		
		return new VectorOfCoefficientsForEachMonomial(varToCoeffs);
	}

	private void putNonZero(LinkedHashMap<IComposite, BigDecimal> varToCoeffs, IComposite variables, BigDecimal bd) {
		if(bd.compareTo(BigDecimal.ZERO)!=0) {
			varToCoeffs.put(variables, bd);
		}
	}

	public int numberOfMonomials() {
		return varaiblesToCoefficients.size();
	}

	public IComposite getMonomial(int i) {
		return keys[i];
	}
	public BigDecimal getCoefficient(int i) {
		return getCoefficient(getMonomial(i));
	}

	public double[] toCoefficients() {
		double[] ret = new double[numberOfMonomials()];
		for(int i=0;i<numberOfMonomials();i++) {
			ret[i]=getCoefficient(i).doubleValue();
		}
		return ret;
	}
	public BigDecimal[] toCoefficientsBD() {
		BigDecimal[] ret = new BigDecimal[numberOfMonomials()];
		for(int i=0;i<numberOfMonomials();i++) {
			ret[i]=getCoefficient(i);
		}
		return ret;
	}
	
	public BigDecimal sumCoefficientsBD() {
		BigDecimal ret = BigDecimal.ZERO;
		for(int i=0;i<numberOfMonomials();i++) {
			ret.add(getCoefficient(i));
		}
		return ret;
	}
}
