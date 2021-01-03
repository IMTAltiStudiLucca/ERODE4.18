package it.imt.erode.importing.chemkin;

import java.io.IOException;
import java.math.BigDecimal;

public class ThermoDynamicInfoChemKin {

	private BigDecimal lowTOfSpecies;
	private BigDecimal highTOfSpecies;
	private BigDecimal commonTOfSpecies;
	
	BigDecimal[] upperIntervalCoefficients;
	BigDecimal[] lowerIntervalCoefficients;
	
	public ThermoDynamicInfoChemKin(BigDecimal lowTOfSpecies,
			BigDecimal highTOfSpecies, BigDecimal commonTOfSpecies,
			BigDecimal[] upperIntervalCoefficients,
			BigDecimal[] lowerIntervalCoefficients) throws IOException {
		super();
		this.lowTOfSpecies = lowTOfSpecies;
		this.highTOfSpecies = highTOfSpecies;
		this.commonTOfSpecies = commonTOfSpecies;
	 	this.upperIntervalCoefficients = upperIntervalCoefficients;
		this.lowerIntervalCoefficients = lowerIntervalCoefficients;
		
		if(lowerIntervalCoefficients.length!=7 || upperIntervalCoefficients.length!=7){
			throw new IOException("The lower and upper interval coefficients should hav 7 entries each. Instead they have, respectively:" + lowerIntervalCoefficients.length + " and " + upperIntervalCoefficients.length);
		}
	}

	protected BigDecimal getLowTOfSpecies() {
		return lowTOfSpecies;
	}

	protected BigDecimal getHighTOfSpecies() {
		return highTOfSpecies;
	}

	protected BigDecimal getCommonTOfSpecies() {
		return commonTOfSpecies;
	}

	protected BigDecimal[] getUpperIntervalCoefficients() {
		return upperIntervalCoefficients;
	}

	protected BigDecimal[] getLowerIntervalCoefficients() {
		return lowerIntervalCoefficients;
	}
	
	protected BigDecimal[] getAppropriateIntervalCoefficients(BigDecimal T) {
		//If T <= commonTOfSpecies return lowerIntervalCoefficients. Otherwise return the upper ones.
		if(T.compareTo(commonTOfSpecies)<=0){
			return lowerIntervalCoefficients;
		}
		else{
			return upperIntervalCoefficients;
		}
	}
	
	
	
	
}
