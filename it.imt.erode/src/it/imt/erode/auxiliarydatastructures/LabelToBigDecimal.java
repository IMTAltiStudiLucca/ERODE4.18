package it.imt.erode.auxiliarydatastructures;

import java.math.BigDecimal;
import java.util.LinkedHashMap;

import it.imt.erode.crn.label.ILabel;

public class LabelToBigDecimal {

	private LinkedHashMap<ILabel, BigDecimal> map;
	
	public void add(ILabel label, BigDecimal n) {
		if(map==null) {
			map= new LinkedHashMap<ILabel, BigDecimal>();
			map.put(label, n);
		}
		else {
			BigDecimal current = map.get(label);
			if(current==null) {
				map.put(label, n);
			}
			else {
				map.put(label, n.add(current));
			}
		}
	}
	
	public BigDecimal getValue(ILabel label) {
		if(map==null) {
			return BigDecimal.ZERO;
		}
		BigDecimal bd = map.get(label);
		if(bd==null) {
			return BigDecimal.ZERO;
		}
		return bd;
		
	}
	
}
