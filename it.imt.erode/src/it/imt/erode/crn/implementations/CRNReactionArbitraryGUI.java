package it.imt.erode.crn.implementations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.ASTNode.Type;
import org.sbml.jsbml.text.parser.ParseException;

import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;

public class CRNReactionArbitraryGUI extends CRNReactionArbitraryAbstract {

	public CRNReactionArbitraryGUI(IComposite reagents, IComposite products,String body,String id) throws IOException {
		super(reagents, products, body,id);
	}
	
	@Override
	public ICRNReaction cloneReplacingProducts(Composite newProducts) throws IOException {
		return new CRNReactionArbitraryGUI(getReagents(), newProducts, getRateExpression(),getID());
	}

	/*public void replaceSpeciesWithRepresentativeInRate(IPartition partition,HashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,HashMap<String, ISpecies> speciesNameToSpecies) {
		replaceSpeciesWithRepresentativeInRate(partition,correspondenceBlock_ReducedSpecies,speciesNameToSpecies,false);
	}*/
	
	public void replaceSAndBWithMin(HashMap<String, ISpecies> speciesNameToSpecies){
		ASTNode reducedRateLaw = getRateLaw().clone();
		replaceVar(reducedRateLaw, 1,speciesNameToSpecies);
		setRateLaw(reducedRateLaw);
	}
	
	private void replaceVar(ASTNode node, int idIncrement, HashMap<String, ISpecies> speciesNameToSpecies) {
		//OLD COMMENT //A variable is actually a parameter, while a function (e.g., varsName(1)) is an ODE variable (with id 0).
		//A variable is actually a parameter or a species
		//System.out.println(node);
		if(node.isOperator()){
			for(int i=0;i<node.getChildCount();i++){
				replaceVar(node.getChild(i), idIncrement,speciesNameToSpecies);
			}
		}
		else if(node.isFunction() /*&& !node.getName().equals(varsName)*/){
			for(int i=0;i<node.getChildCount();i++){
				replaceVar(node.getChild(i), idIncrement,speciesNameToSpecies);
			}
		}
		/*else if(node.isFunction() && node.getName().equals(varsName)){
			int id = node.getListOfNodes().get(0).getInteger();
			ISpecies reducedSpecies = correspondenceBlock_ReducedSpecies.get(partition.getBlockOf(speciesIdToSpecies[id-idIncrement]));
			node.getListOfNodes().get(0).setValue(reducedSpecies.getID()+idIncrement);//varsName(i) is named varsNamei, but has id i-1;
		}*/
		else if(node.isVariable()){
			ISpecies species = speciesNameToSpecies.get(node.getName());
			if(species!=null){
				if(species.getName().startsWith("S")||species.getName().startsWith("B")){
					node.setType(Type.FUNCTION);
					node.setName("min");
					ASTNode spNode = new ASTNode(Type.NAME);
					spNode.setName(species.getName());
					ASTNode oneNode = new ASTNode(Type.INTEGER);
					oneNode.setValue(1);
					node.addChild(spNode);
					node.addChild(oneNode);
				}
				else{
					//do nothing
				}
			}
			/*for (ISpecies species : crn.getSpecies()) {
				if(node.getName().equals(species.getName())){
					int id = species.getID();
					ISpecies reducedSpecies = correspondenceBlock_ReducedSpecies.get(partition.getBlockOf(speciesIdToSpecies[id-idIncrement]));
					//node.getListOfNodes().get(0).setValue(reducedSpecies.getID()+idIncrement);//varsName(i) is named varsNamei, but has id i-1;
					node.getListOfNodes().get(0).setName(reducedSpecies.getName());
				}
			}*/
			else{
				//It can be either a species or parameter
				//DO NOTHING	
				//Pay attention: functions are considered variables  in isVariables()...
			}
		}
		else if(node.isNumber()){
			//DO NOTHING
		}
		else{
			throw new UnsupportedOperationException(node.toString());
		}
	}
	
	public void replaceSpeciesWithRepresentativeInRate(IPartition partition,HashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,HashMap<String, ISpecies> speciesNameToSpecies, boolean scaleBySizeOfBlock) {
		ASTNode reducedRateLaw = getRateLaw().clone();
		replaceVar(reducedRateLaw, partition, correspondenceBlock_ReducedSpecies, 1,speciesNameToSpecies, scaleBySizeOfBlock);
		setRateLaw(reducedRateLaw);
	}
	
	public static void replaceTwoSpeciesWithTwoExprs(ASTNode node, ISpecies firstSpecies, ASTNode factorForFirstSpecies,ISpecies secondSpecies, ASTNode factorForSecondSpecies,ASTNode commonFactor) {
		
		if(node.isOperator()){
			for(int i=0;i<node.getChildCount();i++){
				replaceTwoSpeciesWithTwoExprs(node.getChild(i), firstSpecies, factorForFirstSpecies,secondSpecies, factorForSecondSpecies,commonFactor);
			}
		}
		else if(node.isFunction() /*&& !node.getName().equals(varsName)*/){
			for(int i=0;i<node.getChildCount();i++){
				replaceTwoSpeciesWithTwoExprs(node.getChild(i), firstSpecies, factorForFirstSpecies,secondSpecies, factorForSecondSpecies,commonFactor);
			}
		}
		else if(node.isVariable()){
			if(node.getName().equals(firstSpecies.getName())){
				ASTNode clonedFirstFactor = factorForFirstSpecies.clone();
				ASTNode clonedSecondFactor = commonFactor.clone();
				node.setName(null);
				node.addChild(clonedFirstFactor);
				node.addChild(clonedSecondFactor);
				node.setType(Type.TIMES);
			}
			else if(node.getName().equals(secondSpecies.getName())){
				ASTNode clonedFirstFactor = factorForSecondSpecies.clone();
				ASTNode clonedSecondFactor = commonFactor.clone();
				node.setName(null);
				node.addChild(clonedFirstFactor);
				node.addChild(clonedSecondFactor);
				node.setType(Type.TIMES);
			}
			else{
				//It can be either another species or parameter
				//DO NOTHING	
				//Pay attention: functions are considered variables  in isVariables()...
			}
		}
		else if(node.isNumber()){
			//DO NOTHING
		}
		else{
			throw new UnsupportedOperationException(node.toString());
		}
	}
	
	public static void replaceNonRepWithZeroAndRepWithBlock(ASTNode node, IPartition partition, HashMap<String, ISpecies> speciesNameToSpecies){
		if(node.isOperator()){
			for(int i=0;i<node.getChildCount();i++){
				replaceNonRepWithZeroAndRepWithBlock(node.getChild(i), partition,speciesNameToSpecies);
			}
		}
		else if(node.isFunction() /*&& !node.getName().equals(varsName)*/){
			for(int i=0;i<node.getChildCount();i++){
				replaceNonRepWithZeroAndRepWithBlock(node.getChild(i), partition,speciesNameToSpecies);
			}
		}
		else if(node.isVariable()){
			ISpecies species = speciesNameToSpecies.get(node.getName());
			if(species!=null){
				if(partition.getBlockOf(species).getSpecies().size()==1){
					//do nothing
				}
				else if(partition.speciesIsRepresentativeOfItsBlock(species)){
					ASTNode sumOfBlock = computeSumOfNonSingletonBlock(partition.getBlockOf(species));
					node.setName(null);
					node.addChild(sumOfBlock.getLeftChild());
					node.addChild(sumOfBlock.getRightChild());
					node.setType(Type.PLUS);
				}
				else{
					//ASTNode zero = ASTNode.parseFormula("0");
					node.setName(null);
					node.setValue(0);
					/*node.addChild(zero.clone());
					node.addChild(zero.clone());
					node.setType(Type.TIMES);*/
				}
				
			}
			else{
				//It can be either another species or parameter
				//DO NOTHING	
				//Pay attention: functions are considered variables  in isVariables()...
			}
		}
		else if(node.isNumber()){
			//DO NOTHING
		}
		else{
			throw new UnsupportedOperationException(node.toString());
		}
		
	}
	
	private static ASTNode computeSumOfNonSingletonBlock(IBlock block) {
		ASTNode sum = new ASTNode();
		ASTNode currentPlusNode = sum;
		if(block.getSpecies().size()>1){
			int s=0;
			for (ISpecies species : block.getSpecies()) {
				ASTNode speciesNode;
				try {
					speciesNode = ASTNode.parseFormula(species.getName());
				} catch (ParseException e) {
					throw new UnsupportedOperationException("Problems in transforming a species in a parse tree node of an arithmetic expression: "+e.getMessage());
				}
				if(s==0){
					//this is the first species of the block
					sum.addChild(speciesNode);
					sum.setType(Type.PLUS);
					currentPlusNode=sum;
				}
				else if(s==block.getSpecies().size()-1){
					//this is the last species of the block
					currentPlusNode.addChild(speciesNode);
				}
				else{
					//this is neither first nor last
					ASTNode newSumNode = new ASTNode();
					newSumNode.setType(Type.PLUS);
					newSumNode.addChild(speciesNode);
					currentPlusNode.addChild(newSumNode);
					currentPlusNode = newSumNode;
				}
				s++;
			}
		}
		else{
			throw new UnsupportedOperationException("The method computeSumOfNonSingletonBlock should be invoked only for non singleton blocks.");
			//sum.setName(block.getRepresentative().getName());
		}
		
		return sum;
	}
	
	@Override
	public  ASTNode replaceVar(String variablePrefix,String variableSuffix,int idIncrement, HashMap<String, ISpecies> speciesNameToSpecies){
		ASTNode clone=getRateLaw().clone();
		replaceVar(clone, variablePrefix, variableSuffix, idIncrement, speciesNameToSpecies);
		return clone;
	}
	
	public static void replaceVar(ASTNode node,  String variablePrefix,String variableSuffix,int idIncrement, HashMap<String, ISpecies> speciesNameToSpecies) {
		if(node.isOperator()){
			for(int i=0;i<node.getChildCount();i++){
				replaceVar(node.getChild(i), variablePrefix,variableSuffix,idIncrement,speciesNameToSpecies);
			}
		}
		else if(node.isFunction() /*&& !node.getName().equals(varsName)*/){
			for(int i=0;i<node.getChildCount();i++){
				replaceVar(node.getChild(i), variablePrefix,variableSuffix,idIncrement,speciesNameToSpecies);
			}
		}
		else if(node.isVariable()){
			ISpecies species = speciesNameToSpecies.get(node.getName());
			if(species!=null){
				node.setName(variablePrefix+(species.getID()+1)+variableSuffix);
			}
			else{
				//It must be a parameter
				node.setName(node.getName()+variableSuffix);
			}
		}
		else if(node.isNumber()){
			//DO NOTHING
		}
		else{
			throw new UnsupportedOperationException(node.toString());
		}
	}
	
	private void replaceVar(ASTNode node,  IPartition partition,
			HashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies, int idIncrement, HashMap<String, ISpecies> speciesNameToSpecies, boolean scaleBySizeOfBlock) {
		//OLD COMMENT //A variable is actually a parameter, while a function (e.g., varsName(1)) is an ODE variable (with id 0).
		//A variable is actually a parameter or a species
		//System.out.println(node);
		if(node.isOperator()){
			for(int i=0;i<node.getChildCount();i++){
				replaceVar(node.getChild(i), partition, correspondenceBlock_ReducedSpecies,idIncrement,speciesNameToSpecies,scaleBySizeOfBlock);
			}
		}
		else if(node.isFunction() /*&& !node.getName().equals(varsName)*/){
			for(int i=0;i<node.getChildCount();i++){
				replaceVar(node.getChild(i), partition, correspondenceBlock_ReducedSpecies,idIncrement,speciesNameToSpecies,scaleBySizeOfBlock);
			}
		}
		/*else if(node.isFunction() && node.getName().equals(varsName)){
			int id = node.getListOfNodes().get(0).getInteger();
			ISpecies reducedSpecies = correspondenceBlock_ReducedSpecies.get(partition.getBlockOf(speciesIdToSpecies[id-idIncrement]));
			node.getListOfNodes().get(0).setValue(reducedSpecies.getID()+idIncrement);//varsName(i) is named varsNamei, but has id i-1;
		}*/
		else if(node.isVariable()){
			ISpecies species = speciesNameToSpecies.get(node.getName());
			if(species!=null){
				ISpecies reducedSpecies = correspondenceBlock_ReducedSpecies.get(partition.getBlockOf(species));
				//node.getListOfNodes().get(0).setName(reducedSpecies.getName());
				if(scaleBySizeOfBlock && partition.getBlockOf(species).getSpecies().size() != 1){
					ASTNode redSP = new ASTNode(Type.NAME);
					redSP.setName(reducedSpecies.getName());
					ASTNode factorNode = new ASTNode(partition.getBlockOf(species).getSpecies().size());
					node.setName(null);
					node.addChild(redSP);
					node.addChild(factorNode);
					node.setType(Type.DIVIDE);
				}
				else{
					node.setName(reducedSpecies.getName());
				}
			}
			/*for (ISpecies species : crn.getSpecies()) {
				if(node.getName().equals(species.getName())){
					int id = species.getID();
					ISpecies reducedSpecies = correspondenceBlock_ReducedSpecies.get(partition.getBlockOf(speciesIdToSpecies[id-idIncrement]));
					//node.getListOfNodes().get(0).setValue(reducedSpecies.getID()+idIncrement);//varsName(i) is named varsNamei, but has id i-1;
					node.getListOfNodes().get(0).setName(reducedSpecies.getName());
				}
			}*/
			else{
				//It can be either a species or parameter
				//DO NOTHING	
				//Pay attention: functions are considered variables  in isVariables()...
			}
		}
		else if(node.isNumber()){
			//DO NOTHING
		}
		else{
			throw new UnsupportedOperationException(node.toString());
		}
	}
	
	/**
	 * This method builds a String replacing the occurrences of a species S with the value y[S.getID()]
	 * @param node
	 * @param speciesNameToSpecies
	 * @param values
	 */
	public static void replaceVarWithValue(ASTNode node,  HashMap<String, ISpecies> speciesNameToSpecies, double[] values) {
		//OLD COMMENT //A variable is actually a parameter, while a function (e.g., varsName(1)) is an ODE variable (with id 0).
		//A variable is actually a parameter or a species
		//System.out.println(node);
		if(node.isOperator()){
			for(int i=0;i<node.getChildCount();i++){
				replaceVarWithValue(node.getChild(i),speciesNameToSpecies,values);
			}
		}
		else if(node.isFunction() /*&& !node.getName().equals(varsName)*/){
			for(int i=0;i<node.getChildCount();i++){
				replaceVarWithValue(node.getChild(i), speciesNameToSpecies,values);
			}
		}
		/*else if(node.isFunction() && node.getName().equals(varsName)){
			int id = node.getListOfNodes().get(0).getInteger();
			ISpecies reducedSpecies = correspondenceBlock_ReducedSpecies.get(partition.getBlockOf(speciesIdToSpecies[id-idIncrement]));
			node.getListOfNodes().get(0).setValue(reducedSpecies.getID()+idIncrement);//varsName(i) is named varsNamei, but has id i-1;
		}*/
		else if(node.isVariable()){
			ISpecies species = speciesNameToSpecies.get(node.getName());
			if(species!=null){
				node.setValue(values[species.getID()]);
				//node.setType(Type.REAL);
			}
			/*for (ISpecies species : crn.getSpecies()) {
				if(node.getName().equals(species.getName())){
					int id = species.getID();
					ISpecies reducedSpecies = correspondenceBlock_ReducedSpecies.get(partition.getBlockOf(speciesIdToSpecies[id-idIncrement]));
					//node.getListOfNodes().get(0).setValue(reducedSpecies.getID()+idIncrement);//varsName(i) is named varsNamei, but has id i-1;
					node.getListOfNodes().get(0).setName(reducedSpecies.getName());
				}
			}*/
			else{
				//It can be either a species or parameter
				//DO NOTHING	
				//Pay attention: functions are considered variables  in isVariables()...
			}
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
		getSpeciesInRateLaw(consideredRateLaw,speciesNodes,speciesNameToSpecies);
		return speciesNodes;
	}

	private static void getSpeciesInRateLaw(ASTNode node,Collection<ASTNode> speciesNodes,HashMap<String, ISpecies> speciesNameToSpecies) {
		if(node.isOperator()){
			for(int i=0;i<node.getChildCount();i++){
				getSpeciesInRateLaw(node.getChild(i),speciesNodes,speciesNameToSpecies);
			}
		}
		else if(node.isFunction() /*&& !node.getName().equals(varsName)*/){
			for(int i=0;i<node.getChildCount();i++){
				getSpeciesInRateLaw(node.getChild(i), speciesNodes,speciesNameToSpecies);
			}
		}
		else if(node.isVariable()){
			ISpecies species = speciesNameToSpecies.get(node.getName());
			if(species!=null){
				speciesNodes.add(node);
			}
			else{
				//It can be either a species or parameter
				//DO NOTHING	
				//Pay attention: functions are considered variables  in isVariables()...
			}
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
		getSpeciesIDsInRateLaw(consideredRateLaw,speciesIDs,speciesNameToSpecies);
		return speciesIDs;
	}
	
	private void getSpeciesIDsInRateLaw(ASTNode node,
			List<Integer> speciesIDs,HashMap<String, ISpecies> speciesNameToSpecies) {
		if(node.isOperator()){
			for(int i=0;i<node.getChildCount();i++){
				getSpeciesIDsInRateLaw(node.getChild(i),speciesIDs,speciesNameToSpecies);
			}
		}
		//else if(node.isFunction() //&& !node.getName().equals(varsName)
		  ){
		else if(node.isFunction() ){
			for(int i=0;i<node.getChildCount();i++){
				getSpeciesIDsInRateLaw(node.getChild(i), speciesIDs,speciesNameToSpecies);
			}
		}
		else if(node.isVariable()){
			ISpecies species = speciesNameToSpecies.get(node.getName());
			if(species!=null){
				speciesIDs.add(species.getID());
			}
			else{
				//It can be either a species or parameter
				//DO NOTHING	
				//Pay attention: functions are considered variables  in isVariables()...
			}
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
		ISpecies species = null;
		if(speciesNode.isVariable()){
			species = speciesNameToSpecies.get(speciesNode.getName());
		}
		return species;
	}


	
}
