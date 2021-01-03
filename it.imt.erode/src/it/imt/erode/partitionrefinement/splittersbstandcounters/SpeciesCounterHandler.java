package it.imt.erode.partitionrefinement.splittersbstandcounters;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;

import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.crn.label.ILabel;


public class SpeciesCounterHandler implements ISpeciesCounterHandler{
	
	private BigDecimal pr;
	private BigDecimal fr;
	private LinkedHashMap<ILabel, BigDecimal> smbCounterForEachLabel;
	private LinkedHashMap<ILabel, BigDecimal> prForEachLabel;
	private LinkedHashMap<ILabel, BigDecimal> frForEachLabel;
	private BigDecimal nr;
	private LinkedHashMap<ILabel, BigDecimal> crrForEachLabel;
	private BigDecimal smbCounter; // Used for SMB and EMSB
	private HashMap<IComposite, BigDecimal> cft;
	private LinkedHashMap<ILabel, BigDecimal> nrForEachLabel;
	
	public SpeciesCounterHandler() {
		initializeAllCounters();
	}
	
	@Override
	public void initializeAllCounters() {
		pr= BigDecimal.ZERO;
		fr= BigDecimal.ZERO;
		smbCounterForEachLabel = null;
		prForEachLabel = null;//prForEachLabel = new LinkedHashMap<ILabel,BigDecimal>();
		frForEachLabel = null;//new LinkedHashMap<ILabel,BigDecimal>();
		nrForEachLabel = null;//wprForEachLabel = new LinkedHashMap<ILabel,BigDecimal>();
		nr= BigDecimal.ZERO;
		crrForEachLabel = null; //new LinkedHashMap<ILabel,BigDecimal>();
		smbCounter = BigDecimal.ZERO;
		cft = null;//new HashMap<IComposite,BigDecimal>();
	}
	
	
	@Override
	public void addToCRRD(ILabel label, BigDecimal val) {
		if(crrForEachLabel==null){
			crrForEachLabel = new LinkedHashMap<ILabel, BigDecimal>(); 
		}
		BigDecimal previousValue = crrForEachLabel.get(label);
		if(previousValue == null){
			crrForEachLabel.put(label, val);
		}
		else {
			crrForEachLabel.put(label, previousValue.add(val));
		}
		
	}
	
	
	@Override
	public BigDecimal getCRRd(ILabel label) {
		if(crrForEachLabel==null){
			return BigDecimal.ZERO;
		}
		BigDecimal ret = crrForEachLabel.get(label);
		if(ret == null){
			ret= BigDecimal.valueOf(0);
		}
		
		return ret;
	}
	
	
	@Override
	public BigDecimal getPR() {
		return pr;
	}
	
	
	@Override
	public BigDecimal getFR() {
		return fr;
	}
	
	@Override
	public HashMap<ILabel, BigDecimal> getSMBCounterVector() {
		return smbCounterForEachLabel;
	}
	
	@Override
	public HashMap<ILabel, BigDecimal> getPRVector() {
		return prForEachLabel;
	}
	
	
	@Override
	public HashMap<ILabel, BigDecimal> getNRVector() {
		return nrForEachLabel;
	}
	
	
	@Override
	public HashMap<ILabel, BigDecimal> getFRVector() {
		return frForEachLabel;
	}
	
	
	@Override
	public HashMap<ILabel, BigDecimal> getCRRVector() {
		return crrForEachLabel;
	}
	
	public static void initSpeciesCounters(ISpeciesCounterHandler[] speciesCounters, ISpecies species) {
		if(speciesCounters[species.getID()]==null){
			speciesCounters[species.getID()] = new SpeciesCounterHandler();
		}	
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
		if(BigDecimal.ZERO.compareTo(val)!=0){
			if(prForEachLabel==null){
				prForEachLabel = new LinkedHashMap<ILabel, BigDecimal>();
			}
			BigDecimal previousValue = prForEachLabel.get(label);
			if(previousValue == null){
				prForEachLabel.put(label, val);
			}
			else {
				BigDecimal newVal = previousValue.add(val);
				if(newVal.compareTo(BigDecimal.ZERO)==0){
					prForEachLabel.remove(label);
				}
				else{
					prForEachLabel.put(label, newVal);
				}

			}
		}
		
	}
	
	
	@Override
	public void addToFR(ILabel label, BigDecimal val) {
		if(BigDecimal.ZERO.compareTo(val)!=0){
			if(frForEachLabel==null){
				frForEachLabel = new LinkedHashMap<ILabel, BigDecimal>();  
			}
			BigDecimal previousValue = frForEachLabel.get(label);
			if(previousValue == null){
				frForEachLabel.put(label, val);
			}
			else {
				BigDecimal newVal =previousValue.add(val);
				if(newVal.compareTo(BigDecimal.ZERO)==0){
					frForEachLabel.remove(label);
				}
				else{
					frForEachLabel.put(label, newVal);
				}
			}
		}
	}
	
	
	@Override
	public void addToNRWithScale(ILabel label, BigDecimal val,int scale, RoundingMode rm) {
		val = val.setScale(scale, rm);
		if(BigDecimal.ZERO.compareTo(val)!=0){
			if(nrForEachLabel==null){
				nrForEachLabel = new LinkedHashMap<ILabel, BigDecimal>();
			}
			BigDecimal previousValue = nrForEachLabel.get(label);
			if(previousValue == null){
				nrForEachLabel.put(label, val);
			}
			else {
				BigDecimal newVal = previousValue.add(val);
				if(newVal.compareTo(BigDecimal.ZERO)==0){
					nrForEachLabel.remove(label);
				}
				else{
					nrForEachLabel.put(label, newVal);
				}

			}
		}
		
	}
	
	
	@Override
	public void addToPR(BigDecimal val) {
		pr = pr.add(val);
	}
	
	
	@Override
	public void addToFR(BigDecimal val) {
		fr = fr.add(val);
	}
	
	
	@Override
	public BigDecimal getNR() {
		return nr;
	}
	
	
	@Override
	public void addToWPR(BigDecimal val) {
		nr = nr.add(val);
	}
	
	
	@Override
	public void addToNRWithScale(BigDecimal val,int scale, RoundingMode rm) {
		val = val.setScale(scale, rm);
		if(nr==null){
			nr=val;
		}
		else{
			nr = nr.add(val);
		}
	}
	


	
	@Override
	public BigDecimal getSMBCounter() {
		return smbCounter;
	}
	
	
	@Override
	public void addToSMBCounter(BigDecimal val) {
		smbCounter = smbCounter.add(val);
	}
	
	@Override
	public BigDecimal get(SpeciesCounterField keyField) {
		switch (keyField) {
		case SMBCOUNTER:
			return getSMBCounter();
		case PR:
			return getPR();
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
		BigDecimal val = rate.multiply(BigDecimal.valueOf(multPMinusMultR));
		if(cft==null){
			cft=new HashMap<IComposite, BigDecimal>();
		}
		BigDecimal previousValue = cft.get(rho);
		if(previousValue == null){
			cft.put(rho, val);
		}
		else {
			cft.put(rho, previousValue.add(val));
		}
	}

	
	@Override
	public BigDecimal getCumulativeFluxRate(Collection<IComposite> composites) {
		BigDecimal val = BigDecimal.ZERO;
		for (IComposite composite : composites) {
			val=val.add(get(SpeciesCounterField.CFT,composite));
		}
		return val;
	}

	
	@Override
	public BigDecimal get(SpeciesCounterField keyField, IComposite composite) {
		BigDecimal ret;
		switch (keyField) {
		case CFT:
			if(cft==null){
				return BigDecimal.ZERO;
			}
			ret = cft.get(composite);
			break;
		default:
			return null;
		}

		if(ret == null){
			ret= BigDecimal.ZERO;
		}
		return ret;
	}

	
	

}
