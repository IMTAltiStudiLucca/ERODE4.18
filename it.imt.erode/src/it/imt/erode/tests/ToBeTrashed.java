package it.imt.erode.tests;

import java.math.BigDecimal;
import java.util.Collection;

import it.imt.erode.crn.implementations.SpeciesCompact;
import it.imt.erode.crn.interfaces.ISpecies;

public class ToBeTrashed {
	
	static void ciao (Collection<ISpecies> c) {
		for(ISpecies s : c) {
			System.out.println(s.getName());
		}
	}
	
	public static void main(String[] args) {
		ISpecies[] c = new ISpecies[2];
		c[0] = new SpeciesCompact(0, BigDecimal.ONE);
		c[1] = new SpeciesCompact(1, BigDecimal.ONE);
		//ciao(c);
	}

}
