package it.imt.erode.partitionrefinement.splittersbstandcounters;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;

import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.label.ILabel;


public class SpeciesCounterHandlerSENAry implements ISpeciesCounterHandler{
	
	private LinkedHashMap<ILabel, BigDecimal> smbCounterForEachLabel;
	private BigDecimal smbCounter;
	
	public SpeciesCounterHandlerSENAry() {
		initializeAllCounters();
	}
	
	@Override
	public void initializeAllCounters() {
		smbCounter= BigDecimal.ZERO;
		smbCounterForEachLabel = null;//new LinkedHashMap<ILabel,BigDecimal>();
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
		throw new UnsupportedOperationException("This class has limited functionalities");
	}
	
	@Override
	public HashMap<ILabel, BigDecimal> getSMBCounterVector() {
		return smbCounterForEachLabel;
	}
	
	@Override
	public HashMap<ILabel, BigDecimal> getPRVector() {
		throw new UnsupportedOperationException("This class has limited functionalities");
	}
	
	
	@Override
	public HashMap<ILabel, BigDecimal> getNRVector() {
		throw new UnsupportedOperationException("This class has limited functionalities");
	}
	
	
	@Override
	public HashMap<ILabel, BigDecimal> getFRVector() {
		throw new UnsupportedOperationException("This class has limited functionalities");
	}
	
	
	@Override
	public HashMap<ILabel, BigDecimal> getCRRVector() {
		throw new UnsupportedOperationException("This class has limited functionalities");
	}
	
	@Override
	public void addToSMBCounter(ILabel label, BigDecimal val) {
		if(BigDecimal.ZERO.compareTo(val)!=0){
			if(smbCounterForEachLabel==null){
				smbCounterForEachLabel = new LinkedHashMap<ILabel, BigDecimal>();
			}
			BigDecimal previousValue = smbCounterForEachLabel.get(label);
			if(previousValue == null){
				smbCounterForEachLabel.put(label, val);
			}
			else {
				BigDecimal newVal = previousValue.add(val);
				if(newVal.compareTo(BigDecimal.ZERO)==0){
					smbCounterForEachLabel.remove(label);
				}
				else{
					smbCounterForEachLabel.put(label, newVal);
				}

			}
		}
		
	}
	
	@Override
	public void addToPR(ILabel label, BigDecimal val) {
		throw new UnsupportedOperationException("This class has limited functionalities");
	}
	
	
	@Override
	public void addToFR(ILabel label, BigDecimal val) {
		throw new UnsupportedOperationException("This class has limited functionalities");
	}
	
	
	@Override
	public void addToNRWithScale(ILabel label, BigDecimal val,int scale, RoundingMode rm) {
		throw new UnsupportedOperationException("This class has limited functionalities");
		
	}
	
	
	@Override
	public void addToPR(BigDecimal val) {
		throw new UnsupportedOperationException("This class has limited functionalities");
	}
	
	
	@Override
	public void addToFR(BigDecimal val) {
		throw new UnsupportedOperationException("This class has limited functionalities");
	}
	
	
	@Override
	public BigDecimal getNR() {
		throw new UnsupportedOperationException("This class has limited functionalities");
	}
	
	
	@Override
	public void addToWPR(BigDecimal val) {
		throw new UnsupportedOperationException("This class has limited functionalities");
	}
	
	
	@Override
	public void addToNRWithScale(BigDecimal val,int scale, RoundingMode rm) {
		throw new UnsupportedOperationException("This class has limited functionalities");
	}
	


	
	@Override
	public BigDecimal getSMBCounter() {
		return smbCounter;
	}
	
	
	@Override
	public void addToSMBCounter(BigDecimal val) {
		smbCounter=smbCounter.add(val);
	}
	
	@Override
	public BigDecimal get(SpeciesCounterField keyField) {
		switch (keyField) {
		case SMBCOUNTER:
			return getSMBCounter();
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
