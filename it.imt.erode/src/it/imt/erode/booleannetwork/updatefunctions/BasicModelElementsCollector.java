package it.imt.erode.booleannetwork.updatefunctions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import it.imt.erode.importing.booleannetwork.GuessPrepartitionBN;

public class BasicModelElementsCollector {

	private GuessPrepartitionBN guessPrep = null;
	private LinkedHashMap<String, IUpdateFunction> booleanUpdateFunctions;
	//name,ic,[origname] -> name,[origname],ic
	private ArrayList<ArrayList<String>> initialConcentrations;
	
	private ArrayList<ArrayList<String>> userPartition;

	
	public BasicModelElementsCollector(GuessPrepartitionBN guessPrep, LinkedHashMap<String, IUpdateFunction> parsed) {
		this.booleanUpdateFunctions=parsed;
		this.guessPrep=guessPrep;
		
		initialConcentrations = new ArrayList<ArrayList<String>>(booleanUpdateFunctions.size());
		for(String sp : booleanUpdateFunctions.keySet()) {
			ArrayList<String> cur = new ArrayList<>(2);
			cur.add(sp);
			cur.add("false");
			initialConcentrations.add(cur);
		}
		userPartition=initializeUserPartition(guessPrep,booleanUpdateFunctions); //computeUserPrepString(parsed, null, false);
		if(userPartition==null) {
			userPartition=new ArrayList<ArrayList<String>>(0);
		}
		
	}
	
	public static ArrayList<ArrayList<String>> initializeUserPartition(GuessPrepartitionBN guessPrepartitionOnInputs, LinkedHashMap<String, IUpdateFunction> updateFunctions) {
		if(guessPrepartitionOnInputs.equals(GuessPrepartitionBN.INPUTS)) {
			return guessInputs(updateFunctions,false);
        }
        else if(guessPrepartitionOnInputs.equals(GuessPrepartitionBN.OUTPUTS)) {
        	return guessOutputs(updateFunctions,false);
        }
        else if(guessPrepartitionOnInputs.equals(GuessPrepartitionBN.INPUTSONEBLOCK)) {
        	return guessInputs(updateFunctions,true);
        }
        else if(guessPrepartitionOnInputs.equals(GuessPrepartitionBN.OUTPUTSONEBLOCK)) {
        	return guessOutputs(updateFunctions,true);
        }
		return null;
    }

    public static ArrayList<String> guessInputs(LinkedHashMap<String, IUpdateFunction> updateFunctions){
    	ArrayList<String> guessedInputs = new ArrayList<String>();
		for(Entry<String, IUpdateFunction> pair:updateFunctions.entrySet()) {
			boolean guessedInput=pair.getValue().seemsInputSpecies(pair.getKey());
			if(guessedInput) {
				guessedInputs.add(pair.getKey());
			}
		}
		return guessedInputs;
    }
    
	public static ArrayList<ArrayList<String>> guessInputs(LinkedHashMap<String, IUpdateFunction> updateFunctions,boolean oneBlock) {
		ArrayList<String> guessedInputs=guessInputs(updateFunctions);
		return computeUserPrepString(updateFunctions, guessedInputs, oneBlock);
	}

	
    public static HashSet<String> guessOutputs(LinkedHashMap<String, IUpdateFunction> updateFunctions){
    	//HashSet<String> guessedOutputs = new HashSet<String>(booleanNetwork.getSpecies().size());
//		for(ISpecies sp:booleanNetwork.getSpecies()) {
//			guessedOutputs.add(sp.getName());
//		}
    	HashSet<String> guessedOutputs = new HashSet<String>(updateFunctions.keySet().size());
    	for(String sp:updateFunctions.keySet()) {
			guessedOutputs.add(sp);
		}
		
		//ArrayList<String> guessedInputs = new ArrayList<String>();
		for(Entry<String, IUpdateFunction> pair:updateFunctions.entrySet()) {
			pair.getValue().dropNonOutputSpecies(pair.getKey(),guessedOutputs);
		}
		return guessedOutputs;
    }	
	
	public static ArrayList<ArrayList<String>> guessOutputs(LinkedHashMap<String, IUpdateFunction> updateFunctions,boolean oneBlock) {
		HashSet<String> guessedOutputs=guessOutputs(updateFunctions);
		//setUserPrep(booleanNetwork,guessedOutputs,oneBlock);
		return computeUserPrepString(updateFunctions, guessedOutputs, oneBlock);
	}
	
	
	// userPartition
		public static ArrayList<ArrayList<String>> computeUserPrepString(LinkedHashMap<String, IUpdateFunction> updateFunctions, Collection<String> singletonSpecies,boolean oneBlock) {
			ArrayList<ArrayList<String>> userPartition=null;
			if(singletonSpecies.size()>0) {
				userPartition=new ArrayList<>(singletonSpecies.size());
				if(oneBlock) {
					userPartition=new ArrayList<>(1);
					ArrayList<String> block = new ArrayList<>(singletonSpecies.size());
					userPartition.add(block);
					for(String name : singletonSpecies) {
						block.add(name);
					}
				}
				else {
					userPartition=new ArrayList<>(singletonSpecies.size());
					for(String name : singletonSpecies) {
						ArrayList<String> block = new ArrayList<>(1);
						block.add(name);
						userPartition.add(block);
					}
				}
				
				
			}
			else {
				userPartition = new ArrayList<>(0);
			}
			return userPartition;
		}

	public LinkedHashMap<String, IUpdateFunction> getBooleanUpdateFunctions() {
		return booleanUpdateFunctions;
	}

	public GuessPrepartitionBN getGuessPrep() {
		return guessPrep;
	}
	public ArrayList<ArrayList<String>> getInitialConcentrations() {
		return initialConcentrations;
	}

	public ArrayList<ArrayList<String>> getUserPartition() {
		return userPartition;
	}
		
}
