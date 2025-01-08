package it.imt.erode.auxiliarydatastructures;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.onthefly.Pair;

public class Matrix {

	//TODO: should I use BigDecimal rather than Dobule? I think so
	HashMap<Pair,Double> data;
	double defaultValue=0;
	//ICRN crn;
	List<ISpecies> allSpecies;
	
	public Set<Entry<Pair, Double>> getNonDefaultData(){
		return data.entrySet();
	}
	
	public Matrix(double defVal, List<ISpecies> allSpecies) {
		this.allSpecies=allSpecies;
		data = new LinkedHashMap<>(allSpecies.size());
		setDefaultValue(defVal);
	}

	private void setDefaultValue(double d) {
		defaultValue=d;
	}

	public void setValue(ISpecies i, ISpecies j, double d) {
		//Create an object representing the entry
		Pair ij = new Pair(i, j);
		
		if(d==defaultValue) {
			//if we are assigning the default value, we just remove the entry
			data.remove(ij);
		}
		else {
			//we assign explicitly any value that is not the default one
			data.put(ij, d);
		}
		
		
	}

	public double getValue(int i, int j) {
		//Create an object representing the entry
		Pair ij = new Pair(allSpecies.get(i), allSpecies.get(j));
		Double v = data.get(ij);
		if(v==null) {
			v=defaultValue;
		}
		return v;
	}
	
	public double[][] toDoubleMatrix(boolean round){
		double[][] ret = new double[allSpecies.size()][allSpecies.size()];
		
		if(defaultValue!=0) {
			for(int i=0;i<ret.length;i++) {
				for(int j=0;j<ret.length;j++) {
					ret[i][j]=defaultValue; 
				}
			}
		}
		
		for(Entry<Pair, Double> entry:data.entrySet()) {
			int i = entry.getKey().getFirst().getFirstReagent().getID();
			int j = entry.getKey().getSecond().getFirstReagent().getID();
			ret[i][j]=entry.getValue();
			if(round) {
				 BigDecimal bd = BigDecimal.valueOf(entry.getValue());
				 bd = bd.setScale(5, RoundingMode.HALF_UP);
				 ret[i][j]=bd.doubleValue();
			}
		}
		
		return ret;
	}

	public double getDefault() {
		return defaultValue;
	}
	
	

	@Override
	public String toString() {
		if(data==null) {
			return "empty";
		}
		else {
			return data.toString();
		}
		
	}
	
}
