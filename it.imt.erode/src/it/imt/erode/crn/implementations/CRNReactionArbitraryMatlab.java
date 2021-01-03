package it.imt.erode.crn.implementations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.ASTNode.Type;

import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;

public class CRNReactionArbitraryMatlab extends CRNReactionArbitraryAbstract {

	private String varsName;
	
	public CRNReactionArbitraryMatlab(IComposite reagents, IComposite products,String body, String varsName, String id) throws IOException {
		super(reagents, products, body,id);
		this.varsName=varsName;
	}

	public CRNReactionArbitraryMatlab(IComposite reagents, IComposite products,String body,ASTNode rateLaw, String varsName,String id) throws IOException {
		super(reagents, products, body,rateLaw,id);
		this.varsName=varsName;
	}
	
	@Override
	public ICRNReaction cloneReplacingProducts(Composite newProducts) throws IOException {
		return new CRNReactionArbitraryMatlab(getReagents(), newProducts, getRateExpression(), getVarsName(),getID());
	}
	
	public String getVarsName() {
		return varsName;
	}

	/*public void replaceSpeciesWithRepresentativeInRate(IPartition partition,HashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,ISpecies[] speciesIdToSpecies) {
		replaceSpeciesWithRepresentativeInRate(partition,correspondenceBlock_ReducedSpecies,speciesIdToSpecies,false);
	}*/
	
	public void replaceSpeciesWithRepresentativeInRate(IPartition partition,HashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,ISpecies[] speciesIdToSpecies, boolean scaleBySizeOfBlock) {
		ASTNode reduceRateLaw = getRateLaw().clone();
		replaceVar(reduceRateLaw, getVarsName(), partition, correspondenceBlock_ReducedSpecies, 1,speciesIdToSpecies,scaleBySizeOfBlock);
		setRateLaw(reduceRateLaw);
	}
	
	@Override
	public  ASTNode replaceVar(String variablePrefix,String variableSuffix,int idIncrement, HashMap<String, ISpecies> speciesNameToSpecies){
		ASTNode clone=getRateLaw().clone();
		replaceVar(clone, variablePrefix, variableSuffix, idIncrement, speciesNameToSpecies);
		return clone;
	}
	
	private void replaceVar(ASTNode node,  String variablePrefix,String variableSuffix,int idIncrement, HashMap<String, ISpecies> speciesNameToSpecies) {
		//A variable is actually a parameter, while a function (e.g., varsName(1)) is an ODE variable (with id 0).
				//System.out.println(node);
				if(node.isOperator()){
					for(int i=0;i<node.getChildCount();i++){
						replaceVar(node.getChild(i), variablePrefix,variableSuffix,idIncrement,speciesNameToSpecies);
					}
				}
				else if(node.isFunction() && !node.getName().equals(varsName)){
					for(int i=0;i<node.getChildCount();i++){
						replaceVar(node.getChild(i), variablePrefix,variableSuffix,idIncrement,speciesNameToSpecies);
					}
				}
				else if(node.isFunction() && node.getName().equals(varsName)){
					int id = node.getListOfNodes().get(0).getInteger();
					while(node.getChildCount()>0){
						node.removeChild(node.getChildCount()-1);
					}
					node.setName(variablePrefix+id+variableSuffix);
					node.setType(Type.NAME);
				}
				else if(node.isVariable()){
					//It is a parameter	
					node.setName(node.getName()+variableSuffix);
				}
				else if(node.isNumber()){
					//DO NOTHING
				}
				else{
					throw new UnsupportedOperationException(node.toString());
				}
	}
	
	private void replaceVar(ASTNode node,  String varsName, IPartition partition,
			HashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies, int idIncrement, ISpecies[] speciesIdToSpecies, boolean scaleBySizeOfBlock) {
		//A variable is actually a parameter, while a function (e.g., varsName(1)) is an ODE variable (with id 0).
		//System.out.println(node);
		if(node.isOperator()){
			for(int i=0;i<node.getChildCount();i++){
				replaceVar(node.getChild(i), varsName, partition, correspondenceBlock_ReducedSpecies,idIncrement,speciesIdToSpecies,scaleBySizeOfBlock);
			}
		}
		else if(node.isFunction() && !node.getName().equals(varsName)){
			for(int i=0;i<node.getChildCount();i++){
				replaceVar(node.getChild(i), varsName, partition, correspondenceBlock_ReducedSpecies,idIncrement,speciesIdToSpecies,scaleBySizeOfBlock);
			}
		}
		else if(node.isFunction() && node.getName().equals(varsName)){
			int id = node.getListOfNodes().get(0).getInteger();
			ISpecies reducedSpecies = correspondenceBlock_ReducedSpecies.get(partition.getBlockOf(speciesIdToSpecies[id-idIncrement]));
			if((!scaleBySizeOfBlock) || partition.getBlockOf(speciesIdToSpecies[id-idIncrement]).getSpecies().size() == 1){
				node.getListOfNodes().get(0).setValue(reducedSpecies.getID()+idIncrement);//varsName(i) is named varsNamei, but has id i-1;
			}
			else{
				ASTNode redSP = new ASTNode();
				redSP.setName(varsName);
				redSP.addChild(new ASTNode(reducedSpecies.getID()+idIncrement));
				redSP.setType(Type.FUNCTION);
				ASTNode factorNode = new ASTNode(partition.getBlockOf(speciesIdToSpecies[id-idIncrement]).getSpecies().size());
				node.setName(null);
				node.removeChild(0);
				node.addChild(redSP);
				node.addChild(factorNode);
				node.setType(Type.DIVIDE);
			}
			
			
		}
		else if(node.isVariable()){
			//DO NOTHING	
			//Pay attention: functions are considered variables  in isVariables()...
		}
		else if(node.isNumber()){
			//DO NOTHING
		}
		else{
			throw new UnsupportedOperationException(node.toString());
		}

	}
	
	public static void replaceVarWithValue(ASTNode node,  String varsName, ISpecies[] speciesIdToSpecies, double[] values) {
		int idIncrement = 1;
		//A variable is actually a parameter, while a function (e.g., varsName(1)) is an ODE variable (with id 0).
		//System.out.println(node);
		if(node.isOperator()){
			for(int i=0;i<node.getChildCount();i++){
				replaceVarWithValue(node.getChild(i), varsName, speciesIdToSpecies,values);
			}
		}
		else if(node.isFunction() && !node.getName().equals(varsName)){
			for(int i=0;i<node.getChildCount();i++){
				replaceVarWithValue(node.getChild(i), varsName, speciesIdToSpecies,values);
			}
		}
		else if(node.isFunction() && node.getName().equals(varsName)){
			int id = node.getListOfNodes().get(0).getInteger();
			int actualId = id-idIncrement;
			node.setValue(values[actualId]);
		}
		else if(node.isVariable()){
			//DO NOTHING	
			//Pay attention: functions are considered variables  in isVariables()...
		}
		else if(node.isNumber()){
			//DO NOTHING
		}
		else{
			throw new UnsupportedOperationException(node.toString());
		}

	}

	@Override
	public List<ASTNode> getSpeciesInRateLaw(ASTNode consideredRateLaw, HashMap<String, ISpecies> speciesNameToSpecies) {
		List<ASTNode> speciesNodes = new ArrayList<ASTNode>();
		getSpeciesInRateLaw(consideredRateLaw,speciesNodes);
		return speciesNodes;
	}

	private void getSpeciesInRateLaw(ASTNode node,List<ASTNode> speciesNodes) {
		//A variable is actually a parameter, while a function (e.g., varsName(1)) is an ODE variable (with id 0).
		//System.out.println(node);
		if(node.isOperator()){
			for(int i=0;i<node.getChildCount();i++){
				getSpeciesInRateLaw(node.getChild(i), speciesNodes);
			}
		}
		else if(node.isFunction() && !node.getName().equals(varsName)){
			for(int i=0;i<node.getChildCount();i++){
				getSpeciesInRateLaw(node.getChild(i), speciesNodes);
			}
		}
		else if(node.isFunction() && node.getName().equals(varsName)){
			speciesNodes.add(node);
		}
		else if(node.isVariable()){
			//DO NOTHING	
			//Pay attention: functions are considered variables  in isVariables()...
		}
		else if(node.isNumber()){
			//DO NOTHING
		}
		else{
			throw new UnsupportedOperationException(node.toString());
		}
		
	}

	/*@Override
	public List<Integer> getSpeciesIDsInRateLaw(ASTNode consideredRateLaw, HashMap<String, ISpecies> speciesNameToSpecies) {
		List<Integer> speciesIDs = new ArrayList<Integer>();
		getSpeciesIDsInRateLaw(consideredRateLaw,speciesIDs);
		return speciesIDs;
	}
	
	private void getSpeciesIDsInRateLaw(ASTNode node,List<Integer> speciesIDs) {
		int idIncrement=1;
		//A variable is actually a parameter, while a function (e.g., varsName(1)) is an ODE variable (with id 0).
		//System.out.println(node);
		if(node.isOperator()){
			for(int i=0;i<node.getChildCount();i++){
				getSpeciesIDsInRateLaw(node.getChild(i), speciesIDs);
			}
		}
		else if(node.isFunction() && !node.getName().equals(varsName)){
			for(int i=0;i<node.getChildCount();i++){
				getSpeciesIDsInRateLaw(node.getChild(i), speciesIDs);
			}
		}
		else if(node.isFunction() && node.getName().equals(varsName)){
			int id = node.getListOfNodes().get(0).getInteger();
			int actualId = id-idIncrement;
			speciesIDs.add(actualId);
		}
		else if(node.isVariable()){
			//DO NOTHING	
			//Pay attention: functions are considered variables  in isVariables()...
		}
		else if(node.isNumber()){
			//DO NOTHING
		}
		else{
			throw new UnsupportedOperationException(node.toString());
		}
		
	}*/

	@Override
	public ISpecies getSpecies(ASTNode speciesNode,HashMap<String, ISpecies> speciesNameToSpecies, ISpecies[] speciesIdToSpecies) {
		int idIncrement = 1;
		ISpecies species = null;
		if(speciesNode.isFunction() && speciesNode.getName().equals(varsName)){
			int id = speciesNode.getListOfNodes().get(0).getInteger();
			int actualId = id-idIncrement;
			species = speciesIdToSpecies[actualId];
		}
		return species;
	}
	
}
