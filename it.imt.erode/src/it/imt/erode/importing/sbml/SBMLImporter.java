package it.imt.erode.importing.sbml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.eclipse.ui.console.MessageConsoleStream;
import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.text.parser.ParseException;

import com.eteks.parser.CompilationException;
import com.eteks.parser.CompiledFunction;
import com.eteks.parser.FunctionParser;
import com.eteks.parser.Interpreter;
import com.eteks.parser.MathMLInterpreter;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.crn.implementations.CRNReaction;
import it.imt.erode.crn.implementations.Composite;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.expression.evaluator.MathEval;
import it.imt.erode.importing.AbstractImporter;
import it.imt.erode.importing.CRNImporter;
import it.imt.erode.importing.InfoCRNImporting;
import it.imt.erode.importing.ODEorNET;
import it.imt.erode.importing.UnsupportedFormatException;

/**
 * 
 * @author Isabel Cristina Perez-Verona, Andrea Vandin
 * This class is used to import reaction networks written in SBML format
 */
public class SBMLImporter extends AbstractImporter {

	public SBMLImporter(String fileName,MessageConsoleStream out,BufferedWriter bwOut,IMessageDialogShower msgDialogShower){
		super(fileName,out,bwOut,msgDialogShower);
	}
	public SBMLImporter(MessageConsoleStream out,BufferedWriter bwOut,IMessageDialogShower msgDialogShower) {
		super(out,bwOut,msgDialogShower);
	}
	//private int numberOfConstantSpecies=0;
	private ISpecies timeSpecies;
	//private ISpecies sinkSpecies;
	private ISpecies sourceSpecies;
	
	public InfoCRNImporting importSBMLNetwork(boolean printInfo, boolean printCRN,boolean print,boolean forceMassAction) throws FileNotFoundException, IOException, XMLStreamException, FluxBalanceAnalysisModel, NonIntegerStoichiometryException, NegativeStoichiometryException{
		//numberOfConstantSpecies=0;
		if(print){
			CRNReducerCommandLine.println(out,bwOut,"Importing: "+getFileName());
		}

		long begin = System.currentTimeMillis();
		
		//initInfoImporting();
		//initMath();
		//initCRN();

		initInfoImporting();
		initCRNAndMath();
		getInfoImporting().setLoadedCRN(true);
		
		getCRN().setMdelDefKind(ODEorNET.RN);
		getInfoImporting().setLoadedCRNFormat(ODEorNET.RN);

		File file = new File(getFileName());
		if(file.isFile()) {
			String modelName = file.getName().replace("_url.xml", "");
			modelName = modelName.replace(".xml", "");
			SBMLDocument doc = new SBMLReader().readSBML(getFileName());
			SBML2CRN converter = new SBML2CRN(doc.getModel(),forceMassAction);			
			
			HashMap<String, ISpecies> speciesStoredInHashMap = new HashMap<String, ISpecies>(converter.getSpecies().size());
			addParameters(converter);
			addSpecies(converter,speciesStoredInHashMap);
 			addReactions(converter,speciesStoredInHashMap);
			
//			IBlock uniqueBlock = new Block();
//			setInitialPartition(new Partition(uniqueBlock,getCRN().getSpecies().size()));
//			for (ISpecies species : getCRN().getSpecies()) {
//				uniqueBlock.addSpecies(species);
//			}
			createInitialPartition();
		}
		else {
			CRNReducerCommandLine.println(out,bwOut,"The file does not exist. Loading failed.");
			getInfoImporting().setLoadedCRN(false);
			getInfoImporting().setLoadingCRNFailed();
			return getInfoImporting();
		}
		
		getInfoImporting().setReadCRNReactions(getCRN().getReactions().size());
		getInfoImporting().setReadParameters(getCRN().getParameters().size());
		getInfoImporting().setReadSpecies(getCRN().getSpecies().size());
		getInfoImporting().setRequiredMS(System.currentTimeMillis()-begin);
		
		if(printInfo&&print){
			CRNReducerCommandLine.println(out,bwOut,getInfoImporting().toString());
		}
		
		return getInfoImporting();
	}
	
	public void addParameters(SBML2CRN converter) {
		BannedNamesList bannednames = converter.getBannedNames();
		ListOf<Parameter> Parameters = converter.getParameters();
		if(!Parameters.isEmpty()){
			for (Parameter parameter : Parameters){
				String parameterName;
				String parameterExpression;
				parameterName=bannednames.verifySingleTerm(parameter.getId());
				if(!Double.isNaN(parameter.getValue())){
					//bw.write((bannednames.verifySingleTerm(parameter.getId()))+" = "+parameter.getValue()+"\n");
					parameterExpression=String.valueOf(parameter.getValue());
				} 
				else {
					//ANDREA: I removed this check. I should add a comment to the model.  
					//				if(parser.parameterByRule.containsKey(parameter.getId()))
					//					bw.write((bannednames.verifySingleTerm(parameter.getId()))+" = 0\n");
					//				else
					//					bw.write((bannednames.verifySingleTerm(parameter.getId()))+" = 0        // We are setting this value to zero, but initial conditions in the model should be rewied \n");
					parameterExpression="0";
				}
				addParameter(parameterName, parameterExpression);
			}
		}
	}
	
	private void addSpecies(SBML2CRN converter,HashMap<String, ISpecies> speciesStoredInHashMap) {
		ListOf<Species> Species = converter.getSpecies();
		BannedNamesList bannednames = converter.getBannedNames();
		//addSpecies(String name,String originalName,String icExpr, HashMap<String, ISpecies> speciesStoredInHashMap)
		if(!Species.isEmpty()){
			for (Species species : Species) {
				boolean skipSpecies=false;
				String name=bannednames.verifySingleTerm(species.getId());
				String originalName = species.getName();
				if(originalName==null || originalName.length()==0 || originalName.equals("null") || originalName.equals(name)) {
					originalName=null;
				}
				String icExpr;
				if(!Double.isNaN(species.getInitialConcentration())){
					icExpr = String.valueOf(species.getInitialConcentration());
					//bw.write( bannednames.verifySingleTerm(species.getId())+" = "+species.getInitialConcentration()+"\n");
				}

				else if(!Double.isNaN(species.getInitialAmount())){
					//bw.write( bannednames.verifySingleTerm(species.getId())+" = "+species.getInitialAmount()+"\n");
					icExpr = String.valueOf(species.getInitialAmount());
				}
				else{
					if(converter.containsSpeciesRule(species.getId())) {
						//bw.write( bannednames.verifySingleTerm(species.getId())+" = 0\n");
						icExpr = "0";
					}
					else {
						icExpr=null;
						name=null;
						skipSpecies=true;
					}
				}
				if(!skipSpecies) {
					addSpecies(name, originalName, icExpr, speciesStoredInHashMap);
				}

//				if(species.isConstant()) {
//					numberOfConstantSpecies++;
//				}
			}
		}

		//Adding dummy species Time, Source (describes the synthesis of species), SINK (describes the degradation of species)
		if(converter.hasTimeSpecies()) {
			//bw.write( "Time = 0\n");
			timeSpecies = addSpecies("time", null, "0", speciesStoredInHashMap);
		}
		if(converter.hasSinkSpecies()) {
			//bw.write( "SINK = 0\n");
			//sinkSpecies=
			addSpecies("SINK", null, "0", speciesStoredInHashMap);
		}
		if(converter.hasSourceSpecies()){ 
			//bw.write( "source = 1\n");
			sourceSpecies=addSpecies(SBML2CRN.SOURCESPECIES, null, "1", speciesStoredInHashMap);
		}
	}
	
	private void addReactions(SBML2CRN converter,HashMap<String, ISpecies> speciesStoredInHashMap) throws IOException, FluxBalanceAnalysisModel, NonIntegerStoichiometryException, NegativeStoichiometryException {
		if(converter.hasTimeSpecies()) {
			//bw.write("source ->time, 1\n");
			//Andrea: I change it in source ->time + source
			ICRNReaction crnReaction = new CRNReaction(BigDecimal.ONE, (IComposite)sourceSpecies, new Composite(timeSpecies,sourceSpecies),"1","r_time");
			crnReaction.addCommentLine("reaction the encodes passing of time");
			getCRN().addReaction(crnReaction);
		}
		//int r=0;
		for (Reaction reaction : converter.getReactions()) {
			ReversibleReaction reactions = converter.handleReaction(reaction);
			
			for(int r=0;r<reaction.getReactantCount();r++) {
				double stoich = reaction.getReactant(r).getStoichiometry();
				double rounded = Math.rint(stoich);
				if(stoich!=rounded) {
					throw new NonIntegerStoichiometryException("Reagents of "+reaction.toString());
				}
				if(stoich<0) {
					throw new NegativeStoichiometryException("Reagents of "+reaction.toString());
				}
			}
			for(int p=0;p<reaction.getProductCount();p++) {
				double stoich = reaction.getProduct(p).getStoichiometry();
				double rounded = Math.rint(stoich);
				if(stoich!=rounded) {
					throw new NonIntegerStoichiometryException("Products of "+reaction.toString());
				}
				if(stoich<0) {
					throw new NegativeStoichiometryException("Products of "+reaction.toString());
				}
			}
			
			CRNImporter.parseAndAddReaction(speciesStoredInHashMap, reactions.getForwardWithID(), getCRN(), out, getMath(),reactions.getDescr());
			if(reactions.isRevervible()) {
				String descr = reactions.getDescr();
				if(descr!=null && descr.length()>0) {
					descr+= " - reverse";
				}
				CRNImporter.parseAndAddReaction(speciesStoredInHashMap, reactions.getReverseWithID(), getCRN(), out, getMath(),descr);
			}
		}
	}
	
	public static void printCRNToSBMLFile(ICRN crn,String name, Collection<String> preambleCommentLines, boolean verbose,MessageConsoleStream out,BufferedWriter bwOut) throws CompilationException /*, ParseException*/, UnsupportedFormatException {
		String fileName = name;
		String fileName2=AbstractImporter.overwriteExtensionIfEnabled(fileName,"",true);
		String modelId=fileName2.replace("."+File.separator, "").replace(File.separator, "").replace("-", "_").replace("~", "_");//"_" is ok
		fileName=AbstractImporter.overwriteExtensionIfEnabled(fileName,".xml");
		if(verbose){
			CRNReducerCommandLine.print(out,bwOut,"Writing model in file "+fileName);
		}

		AbstractImporter.createParentDirectories(fileName);
		BufferedWriter br;
		try {
			br = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printCRNToSBMLFile, exception raised while creating the filewriter for file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}

		try {
			
			br.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			br.write("<!-- Created by "+CRNReducerCommandLine.TOOLNAME+" "+CRNReducerCommandLine.TOOLVERSION+" -->\n");
			if(preambleCommentLines!=null && preambleCommentLines.size()>0){
				for (String comment : preambleCommentLines) {
					br.write("<!--"+comment+"-->\n");
				}
				br.write("\n");
			}
			br.write("<sbml xmlns=\"http://www.sbml.org/sbml/level2\" level=\"2\" version=\"1\">\n");
			/*String modelId=crn.getName();
			if(modelId==null||modelId.equals("")){
				SimpleDateFormat sdfDate = new SimpleDateFormat("dd-MM-yyyy-HH:mm:ss.SSS");
				modelId=sdfDate.format(new Date());
			}*/
			br.write("  <model id=\""+modelId+"\">\n");
			
			
			br.write("    <listOfCompartments>\n");
			br.write("      <compartment id=\"cell\" size=\"1\"/>\n");
			br.write("    </listOfCompartments>\n");  
			
			//HashMap<String, Integer> speciesNameToSBMLId = new HashMap<String, Integer>(crn.getSpecies().size());
			HashMap<String, Integer> speciesNameSupportedByMathEvalToSBMLId = new HashMap<String, Integer>(crn.getSpecies().size());
			br.write("    <listOfSpecies>\n");
			int s=1;
			for (ISpecies species : crn.getSpecies()) {
				String originalName = species.getOriginalName();
				if(originalName==null || originalName.length()==0 || originalName.equals("null")) {
					originalName=species.getName();
				}
				//String n = "S"+s;
				String n=species.getName();
				br.write("      <species id=\""+n+"\" compartment=\"cell\" initialConcentration=\""+species.getInitialConcentration()+"\" name=\""+originalName+"\"/>\n");
				//speciesNameToSBMLId.put(species.getName(), s);
				speciesNameSupportedByMathEvalToSBMLId.put(species.getNameAlphanumeric(), s);
				s++;
			}
			br.write("    </listOfSpecies>\n");

			MathEval math = crn.getMath();
			br.write("    <listOfParameters>\n");
			for (String param : crn.getParameters()) {
				String paramName=param.substring(0, param.indexOf(' '));
				br.write("      <parameter id=\""+paramName+"\" value=\""+math.evaluate(paramName)+"\"/>\n");
			}
			br.write("      <!-- Views -->\n");
			String[] viewNames = crn.getViewNames();
			if(viewNames!=null&&viewNames.length>0){
				for(int v=0;v<viewNames.length;v++){
					br.write("      <parameter id=\""+viewNames[v]+"\" constant=\"false\"/>\n");
				}
			}
			br.write("    </listOfParameters>\n");
			
			if(viewNames!=null && viewNames.length>0){
				String[] viewExpStrings = crn.getViewExpressionsSupportedByMathEval();
				FunctionParser parser = new FunctionParser();
				Interpreter interpreter = new MathMLInterpreter();
				br.write("    <listOfRules>\n");
				br.write("      <!-- Views -->\n");
				for(int v=0;v<viewNames.length;v++){
					br.write("      <assignmentRule variable=\""+viewNames[v]+"\">\n");
					br.write("          <math xmlns=\"http://www.w3.org/1998/Math/MathML\">\n");
					//Define a "function" representing the view expression (e.g., f(species1,species2)=species1+species2, if the view sums species1 and species2)
					List<String> viewVariablesList = new ArrayList<String>(math.getVariablesWithin(viewExpStrings[v]));
					String viewVariables = viewVariablesList.toString();
					viewVariables=viewVariables.substring(1, viewVariables.length()-1);
					String function = "f("+viewVariables+") = " + viewExpStrings[v];
					//Now obtain a string S1,S2, where Si is the id assigned to speciesi in the SBML
					List<String> viewVariablesListSBMLId = new ArrayList<String>(viewVariablesList.size());
					for(String var : viewVariablesList){
						Integer id = speciesNameSupportedByMathEvalToSBMLId.get(var); 
						if(id!=null){
							viewVariablesListSBMLId.add("S"+id);
						}
						else{
							viewVariablesListSBMLId.add(var);
						}
					}
					CompiledFunction function1 = parser.compileFunction(function);
					 Object[] viewVariablesSBMLId = new String[viewVariablesListSBMLId.size()];
					 for(int p=0;p<viewVariablesSBMLId.length;p++){
						 viewVariablesSBMLId[p]=viewVariablesListSBMLId.get(p);
					 }
					 //Finally, rename the species names in the expression with the corresponding "S1", and obtain the MathML code corresponding to the expression.
					 String mathml=function1.computeFunction(interpreter, viewVariablesSBMLId).toString().replace("\n", "");
					br.write("            "+mathml+"\n");
					br.write("          </math>\n");
					br.write("      </assignmentRule>\n");
				}
				br.write("    </listOfRules>\n");
			}
			
			int r=1;
			br.write("    <listOfReactions>\n");
			for (ICRNReaction reaction : crn.getReactions()) {
				br.write("      <reaction id=\"R"+r+"\" reversible=\"false\">\n");
				br.write("        <listOfReactants>\n");
				//ISpecies[] reagents = reaction.getReagents().getAllSpecies();
				//int[] multiplicities = reaction.getReagents().getMultiplicities();
				for(s=0;s<reaction.getReagents().getNumberOfDifferentSpecies();s++){
					for(int m=0;m<reaction.getReagents().getMultiplicities(s);m++){
						//br.write("          <speciesReference species=\"S"+speciesNameToSBMLId.get(reaction.getReagents().getAllSpecies(s).getName())+"\"/>\n");
						br.write("          <speciesReference species=\""+reaction.getReagents().getAllSpecies(s).getName()+"\"/>\n");
					}
				}
				br.write("        </listOfReactants>\n");
				br.write("        <listOfProducts>\n");
				//ISpecies[] products = reaction.getProducts().getAllSpecies();
				//multiplicities = reaction.getProducts().getMultiplicities();
				for(s=0;s<reaction.getProducts().getNumberOfDifferentSpecies();s++){
					for(int m=0;m<reaction.getProducts().getMultiplicities(s);m++){
						//br.write("          <speciesReference species=\"S"+speciesNameToSBMLId.get(reaction.getProducts().getAllSpecies(s).getName())+"\"/>\n");
						br.write("          <speciesReference species=\""+reaction.getProducts().getAllSpecies(s).getName()+"\"/>\n");
					}
				}
				br.write("        </listOfProducts>\n");
				ASTNode kineticLaw=null;
				try{
					kineticLaw = ASTNode.parseFormula(reaction.getRateExpression());
				}catch(ParseException pe){
					throw new UnsupportedFormatException("Problems in parsing the rate expression of reaction: "+reaction.toString());
				}
				String mathML =kineticLaw.toMathML();
				if(mathML.startsWith("<?xml version=")) {
					int nl=mathML.indexOf("\n");
					mathML=mathML.substring(nl+1);
					//mathML=mathML.replace("<?xml version='1.0' encoding='UTF-8'?>", "");
				}
				br.write("        <kineticLaw>\n");
				br.write(mathML+"\n");
				/*
				br.write("          <math xmlns=\"http://www.w3.org/1998/Math/MathML\">\n");
				br.write("            <apply>\n");
				br.write("              <times/>\n");
				//multiplicities = reaction.getReagents().getMultiplicities();
				for(s=0;s<reaction.getReagents().getNumberOfDifferentSpecies();s++){
					for(int m=0;m<reaction.getReagents().getMultiplicities(s);m++){
						br.write("              <ci> S"+speciesNameToSBMLId.get(reaction.getReagents().getAllSpecies(s).getName())+" </ci>\n");
					}
				}
				String[] factors = reaction.getRateExpression().split("\\*");
				for(int f=0;f<factors.length;f++){
					br.write("              <ci> "+factors[f].trim() +"</ci>\n");
				}
				br.write("            </apply>\n");
				br.write("          </math>\n");
				*/
				br.write("        </kineticLaw>\n");
				br.write("      </reaction>\n");
				r++;
			}
			br.write("    </listOfReactions>\n");
			br.write("  </model>\n");
			br.write("</sbml>\n");

		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printCRNToCRNFile, exception raised while writing in the file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		finally{
			if(verbose){
				CRNReducerCommandLine.println(out,bwOut,"Writing model in file "+fileName+" completed");
			}
			try {
				br.close();
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in printCRNToCRNFile, exception raised while closing the bufferedwriter of the file: "+fileName);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}

	}
	
}

/*
<?xml version="1.0" encoding="UTF-8"?>
<!-- Created by BioNetGen 2.2.5  -->
<sbml xmlns="http://www.sbml.org/sbml/level2" level="2" version="1">
  <model id="BioNetGen_Fig3">
    <listOfCompartments>
      <compartment id="cell" size="1"/>
    </listOfCompartments>
    <listOfSpecies>
      <species id="S1" compartment="cell" initialConcentration="0" name="F6P(C1~0,C2~0,C3~0,C4~0,C5~0,C6~0)"/>
      <species id="S2" compartment="cell" initialConcentration="0" name="F16P(C1~0,C2~0,C3~0,C4~0,C5~0,C6~0)"/>
      <species id="S3" compartment="cell" initialConcentration="0" name="DHAP(C1~0,C2~0,C3~0)"/>
      <species id="S4" compartment="cell" initialConcentration="0" name="T3P(C1~0,C2~0,C3~0)"/>
      <species id="S5" compartment="cell" initialConcentration="1" name="I()"/>
      <species id="S6" compartment="cell" initialConcentration="0" name="NULL()"/>
      <species id="S7" compartment="cell" initialConcentration="0" name="F6P(C1~1,C2~0,C3~0,C4~0,C5~0,C6~0)"/>
      <species id="S8" compartment="cell" initialConcentration="0" name="F16P(C1~1,C2~0,C3~0,C4~0,C5~0,C6~0)"/>
      <species id="S9" compartment="cell" initialConcentration="0" name="T3P(C1~0,C2~1,C3~0)"/>
      <species id="S10" compartment="cell" initialConcentration="0" name="DHAP(C1~0,C2~1,C3~0)"/>
      <species id="S11" compartment="cell" initialConcentration="0" name="F16P(C1~0,C2~1,C3~0,C4~0,C5~0,C6~0)"/>
      <species id="S12" compartment="cell" initialConcentration="0" name="F16P(C1~1,C2~1,C3~0,C4~0,C5~0,C6~0)"/>
    </listOfSpecies>
    <listOfParameters>
      <!-- Independent variables -->
      <parameter id="v_PFK_f" value="1"/>
      <parameter id="v_FBA_f" value="0.5"/>
      <parameter id="v_FBA_b" value="0.5"/>
      <parameter id="v_TPI_f" value="0.5"/>
      <parameter id="v_TPI_b" value="0.5"/>
      <parameter id="vi_unlabeled" value="2"/>
      <parameter id="vi_labeled" value="0"/>
      <parameter id="vs_F6P" value="1"/>
      <parameter id="vs_T3P" value="1"/>
      <!-- Dependent variables -->
      <!-- Observables -->
      <parameter id="F6P" constant="false"/>
      <parameter id="F16P" constant="false"/>
      <parameter id="DHAP" constant="false"/>
      <parameter id="T3P" constant="false"/>
      <!-- Global functions -->
    </listOfParameters>
    <listOfRules>
      <!-- Dependent variables -->
      <!-- Observables -->
      <assignmentRule variable="F6P">
          <math xmlns="http://www.w3.org/1998/Math/MathML">
            <apply>
              <plus/>
              <ci> S1 </ci>
              <ci> S7 </ci>
            </apply>
          </math>
      </assignmentRule>
      <assignmentRule variable="F16P">
          <math xmlns="http://www.w3.org/1998/Math/MathML">
            <apply>
              <plus/>
              <ci> S2 </ci>
              <ci> S8 </ci>
              <ci> S11 </ci>
              <ci> S12 </ci>
            </apply>
          </math>
      </assignmentRule>
      <assignmentRule variable="DHAP">
          <math xmlns="http://www.w3.org/1998/Math/MathML">
            <apply>
              <plus/>
              <ci> S3 </ci>
              <ci> S10 </ci>
            </apply>
          </math>
      </assignmentRule>
      <assignmentRule variable="T3P">
          <math xmlns="http://www.w3.org/1998/Math/MathML">
            <apply>
              <plus/>
              <ci> S4 </ci>
              <ci> S9 </ci>
            </apply>
          </math>
      </assignmentRule>
      <!-- Global functions -->
    </listOfRules>
    <listOfReactions>
      <reaction id="R1" reversible="false">
        <listOfReactants>
          <speciesReference species="S5"/>
        </listOfReactants>
        <listOfProducts>
          <speciesReference species="S1"/>
          <speciesReference species="S5"/>
        </listOfProducts>
        <kineticLaw>
          <math xmlns="http://www.w3.org/1998/Math/MathML">
            <apply>
              <times/>
              <ci> vi_unlabeled </ci>
              <ci> S5 </ci>
            </apply>
          </math>
        </kineticLaw>
      </reaction>
      <reaction id="R2" reversible="false">
        <listOfReactants>
          <speciesReference species="S5"/>
        </listOfReactants>
        <listOfProducts>
          <speciesReference species="S5"/>
          <speciesReference species="S7"/>
        </listOfProducts>
        <kineticLaw>
          <math xmlns="http://www.w3.org/1998/Math/MathML">
            <apply>
              <times/>
              <ci> vi_labeled </ci>
              <ci> S5 </ci>
            </apply>
          </math>
        </kineticLaw>
      </reaction>
      <reaction id="R3" reversible="false">
        <listOfReactants>
          <speciesReference species="S1"/>
        </listOfReactants>
        <listOfProducts>
          <speciesReference species="S2"/>
        </listOfProducts>
        <kineticLaw>
          <math xmlns="http://www.w3.org/1998/Math/MathML">
            <apply>
              <times/>
              <ci> v_PFK_f </ci>
              <ci> S1 </ci>
            </apply>
          </math>
        </kineticLaw>
      </reaction>
      <reaction id="R4" reversible="false">
        <listOfReactants>
          <speciesReference species="S2"/>
        </listOfReactants>
        <listOfProducts>
          <speciesReference species="S3"/>
          <speciesReference species="S4"/>
        </listOfProducts>
        <kineticLaw>
          <math xmlns="http://www.w3.org/1998/Math/MathML">
            <apply>
              <times/>
              <ci> v_FBA_f </ci>
              <ci> S2 </ci>
            </apply>
          </math>
        </kineticLaw>
      </reaction>
      <reaction id="R5" reversible="false">
        <listOfReactants>
          <speciesReference species="S3"/>
          <speciesReference species="S4"/>
        </listOfReactants>
        <listOfProducts>
          <speciesReference species="S2"/>
        </listOfProducts>
        <kineticLaw>
          <math xmlns="http://www.w3.org/1998/Math/MathML">
            <apply>
              <times/>
              <ci> v_FBA_b </ci>
              <ci> S3 </ci>
              <ci> S4 </ci>
            </apply>
          </math>
        </kineticLaw>
      </reaction>
      <reaction id="R6" reversible="false">
        <listOfReactants>
          <speciesReference species="S4"/>
        </listOfReactants>
        <listOfProducts>
          <speciesReference species="S3"/>
        </listOfProducts>
        <kineticLaw>
          <math xmlns="http://www.w3.org/1998/Math/MathML">
            <apply>
              <times/>
              <ci> v_TPI_f </ci>
              <ci> S4 </ci>
            </apply>
          </math>
        </kineticLaw>
      </reaction>
      <reaction id="R7" reversible="false">
        <listOfReactants>
          <speciesReference species="S3"/>
        </listOfReactants>
        <listOfProducts>
          <speciesReference species="S4"/>
        </listOfProducts>
        <kineticLaw>
          <math xmlns="http://www.w3.org/1998/Math/MathML">
            <apply>
              <times/>
              <ci> v_TPI_b </ci>
              <ci> S3 </ci>
            </apply>
          </math>
        </kineticLaw>
      </reaction>
      <reaction id="R8" reversible="false">
        <listOfReactants>
          <speciesReference species="S1"/>
        </listOfReactants>
        <listOfProducts>
          <speciesReference species="S6"/>
        </listOfProducts>
        <kineticLaw>
          <math xmlns="http://www.w3.org/1998/Math/MathML">
            <apply>
              <times/>
              <ci> vs_F6P </ci>
              <ci> S1 </ci>
            </apply>
          </math>
        </kineticLaw>
      </reaction>
      <reaction id="R9" reversible="false">
        <listOfReactants>
          <speciesReference species="S4"/>
        </listOfReactants>
        <listOfProducts>
          <speciesReference species="S6"/>
        </listOfProducts>
        <kineticLaw>
          <math xmlns="http://www.w3.org/1998/Math/MathML">
            <apply>
              <times/>
              <ci> vs_T3P </ci>
              <ci> S4 </ci>
            </apply>
          </math>
        </kineticLaw>
      </reaction>
      <reaction id="R10" reversible="false">
        <listOfReactants>
          <speciesReference species="S7"/>
        </listOfReactants>
        <listOfProducts>
          <speciesReference species="S8"/>
        </listOfProducts>
        <kineticLaw>
          <math xmlns="http://www.w3.org/1998/Math/MathML">
            <apply>
              <times/>
              <ci> v_PFK_f </ci>
              <ci> S7 </ci>
            </apply>
          </math>
        </kineticLaw>
      </reaction>
      <reaction id="R11" reversible="false">
        <listOfReactants>
          <speciesReference species="S7"/>
        </listOfReactants>
        <listOfProducts>
          <speciesReference species="S6"/>
        </listOfProducts>
        <kineticLaw>
          <math xmlns="http://www.w3.org/1998/Math/MathML">
            <apply>
              <times/>
              <ci> vs_F6P </ci>
              <ci> S7 </ci>
            </apply>
          </math>
        </kineticLaw>
      </reaction>
      <reaction id="R12" reversible="false">
        <listOfReactants>
          <speciesReference species="S8"/>
        </listOfReactants>
        <listOfProducts>
          <speciesReference species="S3"/>
          <speciesReference species="S9"/>
        </listOfProducts>
        <kineticLaw>
          <math xmlns="http://www.w3.org/1998/Math/MathML">
            <apply>
              <times/>
              <ci> v_FBA_f </ci>
              <ci> S8 </ci>
            </apply>
          </math>
        </kineticLaw>
      </reaction>
      <reaction id="R13" reversible="false">
        <listOfReactants>
          <speciesReference species="S3"/>
          <speciesReference species="S9"/>
        </listOfReactants>
        <listOfProducts>
          <speciesReference species="S8"/>
        </listOfProducts>
        <kineticLaw>
          <math xmlns="http://www.w3.org/1998/Math/MathML">
            <apply>
              <times/>
              <ci> v_FBA_b </ci>
              <ci> S3 </ci>
              <ci> S9 </ci>
            </apply>
          </math>
        </kineticLaw>
      </reaction>
      <reaction id="R14" reversible="false">
        <listOfReactants>
          <speciesReference species="S9"/>
        </listOfReactants>
        <listOfProducts>
          <speciesReference species="S10"/>
        </listOfProducts>
        <kineticLaw>
          <math xmlns="http://www.w3.org/1998/Math/MathML">
            <apply>
              <times/>
              <ci> v_TPI_f </ci>
              <ci> S9 </ci>
            </apply>
          </math>
        </kineticLaw>
      </reaction>
      <reaction id="R15" reversible="false">
        <listOfReactants>
          <speciesReference species="S9"/>
        </listOfReactants>
        <listOfProducts>
          <speciesReference species="S6"/>
        </listOfProducts>
        <kineticLaw>
          <math xmlns="http://www.w3.org/1998/Math/MathML">
            <apply>
              <times/>
              <ci> vs_T3P </ci>
              <ci> S9 </ci>
            </apply>
          </math>
        </kineticLaw>
      </reaction>
      <reaction id="R16" reversible="false">
        <listOfReactants>
          <speciesReference species="S4"/>
          <speciesReference species="S10"/>
        </listOfReactants>
        <listOfProducts>
          <speciesReference species="S11"/>
        </listOfProducts>
        <kineticLaw>
          <math xmlns="http://www.w3.org/1998/Math/MathML">
            <apply>
              <times/>
              <ci> v_FBA_b </ci>
              <ci> S4 </ci>
              <ci> S10 </ci>
            </apply>
          </math>
        </kineticLaw>
      </reaction>
      <reaction id="R17" reversible="false">
        <listOfReactants>
          <speciesReference species="S9"/>
          <speciesReference species="S10"/>
        </listOfReactants>
        <listOfProducts>
          <speciesReference species="S12"/>
        </listOfProducts>
        <kineticLaw>
          <math xmlns="http://www.w3.org/1998/Math/MathML">
            <apply>
              <times/>
              <ci> v_FBA_b </ci>
              <ci> S9 </ci>
              <ci> S10 </ci>
            </apply>
          </math>
        </kineticLaw>
      </reaction>
      <reaction id="R18" reversible="false">
        <listOfReactants>
          <speciesReference species="S10"/>
        </listOfReactants>
        <listOfProducts>
          <speciesReference species="S9"/>
        </listOfProducts>
        <kineticLaw>
          <math xmlns="http://www.w3.org/1998/Math/MathML">
            <apply>
              <times/>
              <ci> v_TPI_b </ci>
              <ci> S10 </ci>
            </apply>
          </math>
        </kineticLaw>
      </reaction>
      <reaction id="R19" reversible="false">
        <listOfReactants>
          <speciesReference species="S11"/>
        </listOfReactants>
        <listOfProducts>
          <speciesReference species="S4"/>
          <speciesReference species="S10"/>
        </listOfProducts>
        <kineticLaw>
          <math xmlns="http://www.w3.org/1998/Math/MathML">
            <apply>
              <times/>
              <ci> v_FBA_f </ci>
              <ci> S11 </ci>
            </apply>
          </math>
        </kineticLaw>
      </reaction>
      <reaction id="R20" reversible="false">
        <listOfReactants>
          <speciesReference species="S12"/>
        </listOfReactants>
        <listOfProducts>
          <speciesReference species="S9"/>
          <speciesReference species="S10"/>
        </listOfProducts>
        <kineticLaw>
          <math xmlns="http://www.w3.org/1998/Math/MathML">
            <apply>
              <times/>
              <ci> v_FBA_f </ci>
              <ci> S12 </ci>
            </apply>
          </math>
        </kineticLaw>
      </reaction>
    </listOfReactions>
  </model>
</sbml>
*/


