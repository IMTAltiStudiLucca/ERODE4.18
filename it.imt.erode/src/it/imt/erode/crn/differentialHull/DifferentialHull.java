package it.imt.erode.crn.differentialHull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.ui.console.MessageConsoleStream;
import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.text.parser.ParseException;

import it.imt.erode.auxiliarydatastructures.CRNandPartition;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.EvaluatedParametersComparator;
import it.imt.erode.crn.implementations.CRN;
import it.imt.erode.crn.implementations.CRNReactionArbitraryGUI;
import it.imt.erode.crn.implementations.Composite;
import it.imt.erode.crn.implementations.Species;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.importing.ODEorNET;
import it.imt.erode.importing.SupportedFormats;
import it.imt.erode.partition.implementations.Block;
import it.imt.erode.partition.implementations.Partition;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;

public class DifferentialHull {

	public static CRNandPartition computeHull(ICRN crn, boolean strict, double delta, boolean hasDelta, SupportedFormats format,
			MessageConsoleStream out,BufferedWriter bwOut, ODEorNET crnGUIFormat, String command) throws IOException, ParseException {
		
		boolean ignoreI=false;
		
		HashMap<String, EvaluatedParameter> paramNameToEvaluatedParameter = partitionParametersUpToDeltaClosure(crn,delta, hasDelta);
		
		IBlock uniqueBlock = new Block();
		IPartition partition = new Partition(uniqueBlock, crn.getSpecies().size()*2);
		
		ICRN hullCRN = new CRN(crn.getName()+"DHull",crn.getSymbolicParameters(),crn.getConstraints(),crn.getParameters(),crn.getMath(),out,bwOut);
		
		LinkedHashMap<ISpecies, ISpecies> speciesToUnderline = new LinkedHashMap<>(crn.getSpecies().size());
		LinkedHashMap<ISpecies, ISpecies> speciesToOverline = new LinkedHashMap<>(crn.getSpecies().size());
		
		int id=0;
		for (ISpecies species : crn.getSpecies()) {
			ISpecies uSpecies = new Species("u"+species.getName(), null, id, species.getInitialConcentration(), species.getInitialConcentrationExpr(), "u"+species.getNameAlphanumeric(),species.isAlgebraic());
			hullCRN.addSpecies(uSpecies);
			speciesToUnderline.put(species, uSpecies);
			uniqueBlock.addSpecies(uSpecies);
			id++;	
			ISpecies oSpecies  = new Species("o"+species.getName(), null,id, species.getInitialConcentration(), species.getInitialConcentrationExpr(), "o"+species.getNameAlphanumeric(),species.isAlgebraic());
			hullCRN.addSpecies(oSpecies);
			speciesToOverline.put(species, oSpecies);
			uniqueBlock.addSpecies(oSpecies);
			id++;
		}
		
		HashMap<ISpecies, StringBuilder> speciesToOverlinePositiveDrift = new LinkedHashMap<ISpecies, StringBuilder>(hullCRN.getSpecies().size());
		HashMap<ISpecies, StringBuilder> speciesToOverlineNegativeDrift = new LinkedHashMap<ISpecies, StringBuilder>(hullCRN.getSpecies().size());
		HashMap<ISpecies, StringBuilder> speciesToUnderlinePositiveDrift = new LinkedHashMap<ISpecies, StringBuilder>(hullCRN.getSpecies().size());
		HashMap<ISpecies, StringBuilder> speciesToUnderlineNegativeDrift = new LinkedHashMap<ISpecies, StringBuilder>(hullCRN.getSpecies().size());
		for (ICRNReaction reaction : crn.getReactions()) {
			if(reaction.hasArbitraryKinetics()){
				CRNReducerCommandLine.println(out,bwOut,"Only hulls for mass action CRNs can be computed at the moment.");
				CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
				return null;
			}
			else{
				BigDecimal rate = reaction.getRate();
				IComposite net = reaction.computeProductsMinusReagents();
				for(int s=0;s<net.getNumberOfDifferentSpecies();s++){
					ISpecies species = net.getAllSpecies(s);
					int mult = net.getMultiplicities(s);
					boolean positive = true;
					HashMap<ISpecies, StringBuilder> speciesToOverlineDrift;
					HashMap<ISpecies, StringBuilder> speciesToUnderlineDrift;
					if((mult>0 && rate.compareTo(BigDecimal.ZERO)>0) || (mult<0 && rate.compareTo(BigDecimal.ZERO)<0)){
						speciesToOverlineDrift = speciesToOverlinePositiveDrift;
						speciesToUnderlineDrift = speciesToUnderlinePositiveDrift;
					}
					else{
						speciesToOverlineDrift = speciesToOverlineNegativeDrift;
						speciesToUnderlineDrift = speciesToUnderlineNegativeDrift;
						positive=false;
					}
					StringBuilder driftOverline = speciesToOverlineDrift.get(species);
					StringBuilder driftUnderline = speciesToUnderlineDrift.get(species);
					mult = Math.abs(mult);
					rate = rate.abs();
					
					String firingRateOverline = computeRateExpression(reaction.getRateExpression(),positive,true,paramNameToEvaluatedParameter) +"*";
					String firingRateUnderline = computeRateExpression(reaction.getRateExpression(),positive,false,paramNameToEvaluatedParameter)+"*";
					if(positive){
						firingRateOverline  += reaction.getReagents().getMassActionExpression(false,"o",ignoreI);
						firingRateUnderline += reaction.getReagents().getMassActionExpression(false,"u",ignoreI);
					}
					else{
						//mult * rate is negative
						if(strict){
							firingRateOverline  += reaction.getReagents().getMassActionExpression(false,"u",species,"o",ignoreI);
							firingRateUnderline += reaction.getReagents().getMassActionExpression(false,"o",species,"u",ignoreI);
						}
						else{
							firingRateOverline  += reaction.getReagents().getMassActionExpression(false,"u",ignoreI);
							firingRateUnderline += reaction.getReagents().getMassActionExpression(false,"o",ignoreI);
						}
					}
					
					if(driftOverline==null){
						driftOverline=new StringBuilder();
						speciesToOverlineDrift.put(species, driftOverline);
					}
					else{
						driftOverline.append(" + ");
					}
					if(driftUnderline==null){
						driftUnderline=new StringBuilder();
						speciesToUnderlineDrift.put(species, driftUnderline);
					}
					else{
						driftUnderline.append(" + ");
					}
					
					if(mult==1){
						driftOverline.append(firingRateOverline);
						driftUnderline.append(firingRateUnderline);
					}
					else {
						driftOverline.append(String.valueOf(mult));
						driftOverline.append("*(");
						driftOverline.append(firingRateOverline);
						driftOverline.append(")");
						
						driftUnderline.append(String.valueOf(mult));
						driftUnderline.append("*(");
						driftUnderline.append(firingRateUnderline);
						driftUnderline.append(")");
					}
				}
			}
		}
		
		
		for (ISpecies species : crn.getSpecies()) {
			String oRate="";
			if(speciesToOverlinePositiveDrift.get(species)!=null){
				oRate = speciesToOverlinePositiveDrift.get(species).toString();
			}
			if(speciesToOverlineNegativeDrift.get(species)!=null){
				oRate = oRate + "-("+speciesToOverlineNegativeDrift.get(species)+")";
			}
			if(!oRate.equals("")){
				ISpecies oSpecies =speciesToOverline.get(species);
				ICRNReaction oReaction = new CRNReactionArbitraryGUI((IComposite)oSpecies, new Composite(oSpecies, oSpecies), oRate,null);
				hullCRN.addReaction(oReaction);
			}
			
			String uRate="";
			if(speciesToUnderlinePositiveDrift.get(species)!=null){
				uRate = speciesToUnderlinePositiveDrift.get(species).toString();
			}
			if(speciesToUnderlineNegativeDrift.get(species)!=null){
				uRate = uRate + "-("+speciesToUnderlineNegativeDrift.get(species)+")";
			}
			if(!uRate.equals("")){
				ISpecies uSpecies =speciesToUnderline.get(species);
				ICRNReaction uReaction = new CRNReactionArbitraryGUI((IComposite)uSpecies, new Composite(uSpecies, uSpecies), uRate,null);
				hullCRN.addReaction(uReaction);
			}
		}
		
		return new CRNandPartition(hullCRN, partition);
	}

	private static HashMap<String, EvaluatedParameter> partitionParametersUpToDeltaClosure(ICRN crn, double delta,boolean hasDelta) {
		
		HashMap<String, EvaluatedParameter> paramNameToEvaluatedParameter = new HashMap<String, EvaluatedParameter>();
		List<EvaluatedParameter> evaluatedParameters = new ArrayList<EvaluatedParameter>(0);
		if(hasDelta){
			evaluatedParameters = new ArrayList<EvaluatedParameter>(crn.getParameters().size());
			for (String param : crn.getParameters()) {
				int space = param.indexOf(' ');
				String paramName = param.substring(0, space);
				String paramExpr = param.substring(space,param.length());
				EvaluatedParameter eParam = new EvaluatedParameter(paramName, crn.getMath().evaluate(paramExpr)); 
				evaluatedParameters.add(eParam);
				paramNameToEvaluatedParameter.put(paramName, eParam);
			}
			
			Collections.sort(evaluatedParameters, new EvaluatedParametersComparator());
			
			ParametersBlock currentBlock = new ParametersBlock();
			double lastVal=evaluatedParameters.get(0).getValue();
			for (EvaluatedParameter evaluatedParameter : evaluatedParameters) {
				double currentVal = evaluatedParameter.getValue();
				if(lastVal+delta< currentVal){
					currentBlock = new ParametersBlock();
				}
				currentBlock.addParameter(evaluatedParameter);
				lastVal=currentVal;
			}
		}
		return paramNameToEvaluatedParameter;
	}

	private static String computeRateExpression(String rateExpression, boolean positive, boolean overline,
			HashMap<String, EvaluatedParameter> paramNameToEvaluatedParameter) throws ParseException {
		
		boolean takeMax=false;
		if((positive && overline) || ((!positive) && (!overline))){
			takeMax=true;
		}
		
		EvaluatedParameter ePar = paramNameToEvaluatedParameter.get(rateExpression);
		if(ePar!=null){
			//It is just a parameter
			return takeMax?ePar.getBlock().getMax().getName():ePar.getBlock().getMin().getName();
		}
		else{
			ASTNode rateLaw = ASTNode.parseFormula(rateExpression);
			replaceParameterWithMinOrMaxOfItsBlock(rateLaw, paramNameToEvaluatedParameter, takeMax);
			return rateLaw.toFormula();
		}
	}
	
	/**
	 * This method builds a String replacing the occurrences of a species S with the value y[S.getID()]
	 * @param node
	 * @param speciesNameToSpecies
	 * @param values
	 */
	private static void replaceParameterWithMinOrMaxOfItsBlock(ASTNode node,  HashMap<String, EvaluatedParameter> paramNameToEvaluatedParameter, boolean takeMax) {
		//A variable is actually a parameter or a species
		if(node.isOperator()){
			for(int i=0;i<node.getChildCount();i++){
				replaceParameterWithMinOrMaxOfItsBlock(node.getChild(i),paramNameToEvaluatedParameter,takeMax);
			}
		}
		else if(node.isFunction()){
			for(int i=0;i<node.getChildCount();i++){
				replaceParameterWithMinOrMaxOfItsBlock(node.getChild(i),paramNameToEvaluatedParameter,takeMax);
			}
		}
		else if(node.isVariable()){
			EvaluatedParameter ePar = paramNameToEvaluatedParameter.get(node.getName());
			if(ePar!=null){
				node.setName(takeMax?ePar.getBlock().getMax().getName():ePar.getBlock().getMin().getName());
			}
			else{
				//It is a species
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
	
	
}
