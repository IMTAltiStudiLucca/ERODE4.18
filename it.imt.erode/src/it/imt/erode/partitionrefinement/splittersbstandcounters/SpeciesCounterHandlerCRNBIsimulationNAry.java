package it.imt.erode.partitionrefinement.splittersbstandcounters;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;

import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.label.ILabel;


public class SpeciesCounterHandlerCRNBIsimulationNAry implements ISpeciesCounterHandler{
	
	private BigDecimal frOrNR;
	private LinkedHashMap<ILabel, BigDecimal> frOrNrForEachLabel;
	//private BigDecimal nr;
	//private LinkedHashMap<ILabel, BigDecimal> nrForEachLabel;
	
	public SpeciesCounterHandlerCRNBIsimulationNAry() {
		initializeAllCounters();
	}
	
	@Override
	public void initializeAllCounters() {
		frOrNR= BigDecimal.ZERO;
		frOrNrForEachLabel = null;//new LinkedHashMap<ILabel,BigDecimal>();
		//nrForEachLabel = null;//wprForEachLabel = new LinkedHashMap<ILabel,BigDecimal>();
		//nr= BigDecimal.ZERO;
	}
	
	
	@Override
	public void addToCRRD(ILabel label, BigDecimal val) {
		throw new UnsupportedOperationException("This class has limited functionalities");
		
	}
	
	
	@Override
	public BigDecimal getCRRd(ILabel label) {
		throw new UnsupportedOperationException("This class has limited functionalities");
	}
	
	
	@Override
	public BigDecimal getPR() {
		throw new UnsupportedOperationException("This class has limited functionalities");
	}
	
	
	@Override
	public BigDecimal getFR() {
		//throw new UnsupportedOperationException("This class has limited functionalities");
		return frOrNR;
	}
	
	@Override
	public HashMap<ILabel, BigDecimal> getSMBCounterVector() {
		throw new UnsupportedOperationException("This class has limited functionalities");
	}
	
	@Override
	public HashMap<ILabel, BigDecimal> getPRVector() {
		throw new UnsupportedOperationException("This class has limited functionalities");
	}
	
	
	@Override
	public HashMap<ILabel, BigDecimal> getNRVector() {
		//return nrForEachLabel;
		return frOrNrForEachLabel;
	}
	
	
	@Override
	public HashMap<ILabel, BigDecimal> getFRVector() {
		return frOrNrForEachLabel;
	}
	
	
	@Override
	public HashMap<ILabel, BigDecimal> getCRRVector() {
		throw new UnsupportedOperationException("This class has limited functionalities");
	}
	
	@Override
	public void addToSMBCounter(ILabel label, BigDecimal val) {
		throw new UnsupportedOperationException("This class has limited functionalities");
	}
	
	@Override
	public void addToPR(ILabel label, BigDecimal val) {
		throw new UnsupportedOperationException("This class has limited functionalities");
	}
	
	
	@Override
	public void addToFR(ILabel label, BigDecimal val) {
		if(BigDecimal.ZERO.compareTo(val)!=0){
			if(frOrNrForEachLabel==null){
				frOrNrForEachLabel = new LinkedHashMap<ILabel, BigDecimal>();  
			}
			BigDecimal previousValue = frOrNrForEachLabel.get(label);
			if(previousValue == null){
				frOrNrForEachLabel.put(label, val);
			}
			else {
				BigDecimal newVal =previousValue.add(val);
				if(newVal.compareTo(BigDecimal.ZERO)==0){
					frOrNrForEachLabel.remove(label);
				}
				else{
					frOrNrForEachLabel.put(label, newVal);
				}
			}
		}
	}
	
	
	@Override
	public void addToNRWithScale(ILabel label, BigDecimal val,int scale, RoundingMode rm) {
		val = val.setScale(scale, rm);
		if(BigDecimal.ZERO.compareTo(val)!=0){
			if(frOrNrForEachLabel==null){
				frOrNrForEachLabel = new LinkedHashMap<ILabel, BigDecimal>();
			}
			BigDecimal previousValue = frOrNrForEachLabel.get(label);
			if(previousValue == null){
				frOrNrForEachLabel.put(label, val);
			}
			else {
				BigDecimal newVal = previousValue.add(val);
				if(newVal.compareTo(BigDecimal.ZERO)==0){
					frOrNrForEachLabel.remove(label);
				}
				else{
					frOrNrForEachLabel.put(label, newVal);
				}

			}
		}
		
	}
	
	
	@Override
	public void addToPR(BigDecimal val) {
		throw new UnsupportedOperationException("This class has limited functionalities");
	}
	
	
	@Override
	public void addToFR(BigDecimal val) {
		frOrNR = frOrNR.add(val);
	}
	
	
	@Override
	public BigDecimal getNR() {
		return frOrNR;
	}
	
	
	@Override
	public void addToWPR(BigDecimal val) {
		frOrNR = frOrNR.add(val);
	}
	
	
	@Override
	public void addToNRWithScale(BigDecimal val,int scale, RoundingMode rm) {
		val = val.setScale(scale, rm);
		if(frOrNR==null){
			frOrNR=val;
		}
		else{
			frOrNR = frOrNR.add(val);
		}
	}
	


	
	@Override
	public BigDecimal getSMBCounter() {
		throw new UnsupportedOperationException("This class has limited functionalities");
	}
	
	
	@Override
	public void addToSMBCounter(BigDecimal val) {
		throw new UnsupportedOperationException("This class has limited functionalities");
	}
	
	@Override
	public BigDecimal get(SpeciesCounterField keyField) {
		switch (keyField) {
		case NR:
			return getNR();
		case FR:
			return getFR();
		default:
			throw new UnsupportedOperationException(keyField.toString());
		}
	}

	
	@Override
	public void addToCFT(IComposite rho, int multPMinusMultR, BigDecimal rate) {
		throw new UnsupportedOperationException("This class has limited functionalities");
	}

	
	@Override
	public BigDecimal getCumulativeFluxRate(Collection<IComposite> composites) {
		throw new UnsupportedOperationException("This class has limited functionalities");
	}

	
	@Override
	public BigDecimal get(SpeciesCounterField keyField, IComposite composite) {
		throw new UnsupportedOperationException("This class has limited functionalities");
	}

	
	

}
