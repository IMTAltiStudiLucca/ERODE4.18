package it.imt.erode.partitionrefinement.splittersbstandcounters;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;

import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.label.ILabel;

public interface ISpeciesCounterHandler {
	
	public void initializeAllCounters();
	
	
	public void addToCRRD(ILabel label, BigDecimal val);
	
	
	public BigDecimal getCRRd(ILabel label);
	
	
	public BigDecimal getPR() ;
	
	
	public BigDecimal getFR();
	
	public HashMap<ILabel, BigDecimal> getSMBCounterVector();
	
	public HashMap<ILabel, BigDecimal> getPRVector();
	
	
	public HashMap<ILabel, BigDecimal> getNRVector();
	
	
	public HashMap<ILabel, BigDecimal> getFRVector();
	
	
	public HashMap<ILabel, BigDecimal> getCRRVector();
		
	public void addToSMBCounter(ILabel label, BigDecimal val);
	
	public void addToPR(ILabel label, BigDecimal val);
	
	public void addToFR(ILabel label, BigDecimal val);
	
	
	public void addToNRWithScale(ILabel label, BigDecimal val,int scale, RoundingMode rm);
	
	
	public void addToPR(BigDecimal val);
	
	
	public void addToFR(BigDecimal val);
	
	
	public BigDecimal getNR();
	
	
	public void addToWPR(BigDecimal val);
	
	
	public void addToNRWithScale(BigDecimal val,int scale, RoundingMode rm);

	
	public BigDecimal getSMBCounter();
	
	
	public void addToSMBCounter(BigDecimal val);
	
	public BigDecimal get(SpeciesCounterField keyField);

	
	public void addToCFT(IComposite rho, int multPMinusMultR, BigDecimal rate);

	
	public BigDecimal getCumulativeFluxRate(Collection<IComposite> composites);

	
	public BigDecimal get(SpeciesCounterField keyField, IComposite composite);

	
	

}
