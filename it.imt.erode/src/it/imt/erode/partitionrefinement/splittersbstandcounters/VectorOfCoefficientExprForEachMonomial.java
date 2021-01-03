package it.imt.erode.partitionrefinement.splittersbstandcounters;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import it.imt.erode.crn.implementations.Composite;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;


/**
 * This class represents the monomials of the drift of a species.
 * It associates a coefficient expression (a String) to each product of species (represented as a composite).
 * It offers a method to compute the equation which imposes constraints to make two drifts equal. 
 * @author Andrea Vandin
 *
 */
public class VectorOfCoefficientExprForEachMonomial {

	private final IComposite[] keys;
	//TODO: I should not use String, but IMonomial. 
	private final HashMap<IComposite, String> variablesToCoefficientExprs;

	protected IComposite[] getKeys() {
		return keys;
	}
	
	protected String getCoefficientExpr(IComposite variables) {
		return variablesToCoefficientExprs.get(variables);
	}

	public VectorOfCoefficientExprForEachMonomial(HashMap<HashMap<ISpecies, Integer>, String> hmVariablesToCoefficientExprs) {
		variablesToCoefficientExprs = new HashMap<>(hmVariablesToCoefficientExprs.size());
		keys = new IComposite[hmVariablesToCoefficientExprs.size()];
		int k=0;
		for (Entry<HashMap<ISpecies, Integer>, String> entry : hmVariablesToCoefficientExprs.entrySet()) {
			HashMap<ISpecies, Integer> hm = entry.getKey();
			IComposite composite = new Composite(hm);
			keys[k]=composite;
			k++;
			variablesToCoefficientExprs.put(composite, entry.getValue());
		}
		Arrays.sort(keys);
	}

	@Override
	public String toString() {
		return variablesToCoefficientExprs.toString();
	}

	public void computeLinearEquations(VectorOfCoefficientExprForEachMonomial o, List<String> linearEquations) {
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
			
			String equation = "";
			//If the current positions have same labels, I have to add an equation having the two coefficients
			if(cmp==0){
				IComposite variables = myKeys[myi];//which is equal to oKeys[i]
				equation = getCoefficientExpr(variables) + " - ("+o.getCoefficientExpr(variables)+")";
				myi++;
				oi++;
			}
			else if(cmp<=0){
				//my label is smaller. I.e., I have a label that the other does not have. I have to add an equation having my coefficient only, equal to zero. 
				IComposite variables = myKeys[myi];
				equation = getCoefficientExpr(variables);
				myi++;
			}
			else{ //cmp>0
				//the label of o is smaller. I.e., the other has a label that I do not have. I have to add an equation having the other coefficient only, equal to zero.
				IComposite variables = oKeys[oi];
				equation = " - ("+o.getCoefficientExpr(variables)+")";
				oi++;
			}
			linearEquations.add(equation);
		}
	}
}

