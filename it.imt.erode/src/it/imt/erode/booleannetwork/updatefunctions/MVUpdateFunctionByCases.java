package it.imt.erode.booleannetwork.updatefunctions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Z3Exception;

import it.imt.erode.booleannetwork.interfaces.IBooleanNetwork;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.algorithms.booleannetworks.FBEAggregationFunctions;

public class MVUpdateFunctionByCases implements IUpdateFunction {

	private LinkedHashMap<Integer, IUpdateFunction> casesNoOtherwise;
	private ArrayList<Integer> valuesNoOtherwise;
	private int otherwiseVal=-1; 
	private int max=-1;
	
	public MVUpdateFunctionByCases(LinkedHashMap<Integer, IUpdateFunction> cases, int mx) {
		this.casesNoOtherwise= new LinkedHashMap<>();// cases;
		if(mx!=-1) {
			this.max=Math.max(1, mx);//by default the smallest max is 1.
			//but if -1 is given, it means I don't care about it because everything is given explicitly
		}
		valuesNoOtherwise=new ArrayList<>(cases.size());
		for(Entry<Integer, IUpdateFunction> cur_case : cases.entrySet()) {
			if(cur_case.getValue() instanceof Otherwise) {
				otherwiseVal=cur_case.getKey();
			}
			else {
				valuesNoOtherwise.add(cur_case.getKey());
				this.casesNoOtherwise.put(cur_case.getKey(),cur_case.getValue());
			}
		}
		if(max==-1) {
			if(otherwiseVal==-1) {
				throw new UnsupportedOperationException("No otherwise value");
			}
		}
		else {
			if(otherwiseVal==-1 ) {
				if(valuesNoOtherwise.size()!=max) {
					throw new UnsupportedOperationException("The otherwise case can be omitted only if all other cases are present");
				}
				else {
					for(int i=0;i<=max;i++) {
						if(!valuesNoOtherwise.contains(i)) {
							otherwiseVal=i;
							break;
						}
					}
					if(otherwiseVal==-1) {
						throw new UnsupportedOperationException("No otherwise value");
					}
				}
			}
			if(valuesNoOtherwise.size()>max) {
				throw new UnsupportedOperationException("Wrong number of cases. You should have"+max+" explicit ones, plus one (possibly implicit) otherwise");
			}
		}
	}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		//sb.append("\n");
		sb.append("(");
		for(Entry<Integer, IUpdateFunction> cur_case : casesNoOtherwise.entrySet()) {
			//sb.append("\t");
			sb.append(cur_case.getKey());
			sb.append(" if "+cur_case.getValue()+"; ");
			//sb.append("\n");
		}
		//sb.append("\t");
		sb.append(otherwiseVal+" otherwise)");
		return sb.toString();
	}
	
	
	public Expr casesToITE(BoolExpr[] cases_z3, int c,Context ctx) {
		if(c==valuesNoOtherwise.size()) {
			return ctx.mkNumeral(otherwiseVal, ctx.getIntSort());
		}
		else {
			return  ctx.mkITE(cases_z3[c], 
						ctx.mkInt(valuesNoOtherwise.get(c)), 
						casesToITE(cases_z3, c+1, ctx)	);
		}
	}
	
	@Override
	public Expr toZ3(Context ctx, HashMap<String, ISpecies> speciesNameToSpecies,
			HashMap<ISpecies, Expr> speciesToSpeciesVariable) throws Z3Exception {
		BoolExpr[] cases_z3=new BoolExpr[casesNoOtherwise.size()];
		int c=0;
		for(Entry<Integer, IUpdateFunction> cur_case : casesNoOtherwise.entrySet()) {
			cases_z3[c]=(BoolExpr)cur_case.getValue().toZ3(ctx, speciesNameToSpecies, speciesToSpeciesVariable);
			c++;
		}
		return casesToITE(cases_z3, 0, ctx);
	}
	@Override
	public IUpdateFunction cloneReplacingWithRepresentative(IPartition partition,
			LinkedHashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,
			HashMap<String, ISpecies> speciesNameToOriginalSpecies) {
		
		LinkedHashMap<Integer, IUpdateFunction> clonedCases = new LinkedHashMap<Integer, IUpdateFunction>(casesNoOtherwise.size());
		for(Entry<Integer, IUpdateFunction> cur_case : casesNoOtherwise.entrySet()) {
			clonedCases.put(cur_case.getKey(), 
							cur_case.getValue().cloneReplacingWithRepresentative(partition, correspondenceBlock_ReducedSpecies, speciesNameToOriginalSpecies));
		}
		clonedCases.put(otherwiseVal, new Otherwise());
		return new MVUpdateFunctionByCases(clonedCases, max);
	}
	@Override
	public IUpdateFunction cloneReplacingNorRepresentativeWithNeutral(IPartition partition,
			LinkedHashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,
			HashMap<String, ISpecies> speciesNameToOriginalSpecies, FBEAggregationFunctions aggregationFunction
			, IBooleanNetwork bn) {
		LinkedHashMap<Integer, IUpdateFunction> clonedCases = new LinkedHashMap<Integer, IUpdateFunction>(casesNoOtherwise.size());
		for(Entry<Integer, IUpdateFunction> cur_case : casesNoOtherwise.entrySet()) {
			clonedCases.put(cur_case.getKey(), 
							cur_case.getValue().cloneReplacingNorRepresentativeWithNeutral(partition, correspondenceBlock_ReducedSpecies, speciesNameToOriginalSpecies, aggregationFunction,bn));
		}
		clonedCases.put(otherwiseVal, new Otherwise());
		return new MVUpdateFunctionByCases(clonedCases, max);
	}
	
	@Override
	public boolean seemsInputSpecies(String sp) {
		int ncases=0;
		if(otherwiseVal!=-1) {
			ncases++;
		}
		if(casesNoOtherwise!=null) {
			ncases+=casesNoOtherwise.size();
		}
		
		if(ncases==1) {
			if(otherwiseVal!=-1) {
				return true;
			}
			else  {
				return casesNoOtherwise.values().iterator().next().seemsInputSpecies(sp);
			}
		}
		return false;
	}
	@Override
	public void dropNonOutputSpecies(String sp, HashSet<String> guessedOutputs) {
		for(IUpdateFunction cur_case:casesNoOtherwise.values()) {
			cur_case.dropNonOutputSpecies(sp, guessedOutputs);
		}
	}

}
