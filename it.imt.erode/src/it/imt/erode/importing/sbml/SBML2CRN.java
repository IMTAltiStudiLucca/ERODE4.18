
package it.imt.erode.importing.sbml;



import java.util.ArrayList;
import java.util.List;

//import org.apache.commons.math.ode.DerivativeException;
import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ModifierSpeciesReference;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.util.Pair;
import org.sbml.jsbml.util.compilers.ASTNodeValue;
import org.sbml.jsbml.util.compilers.FormulaCompiler;

import it.imt.erode.importing.MutableValue;



/**
 * @author Isabel Cristina Perez-Verona
 * 			Fixed by Andrea Vandin
 */
public class SBML2CRN {
	public static final String[] FLUX_VALUES = new String[] {"FLUX_VALUE","FLUX"};

	public static final String SOURCESPECIES = "source";

	//0. VARIABLES ------------------------------------------------------------------------------------------------------------ 
	private Boolean noSBO = false; //Change to TRUE only if you are SUPER SURE that the model you are parsing is mass action.

	/**
	 * MODEL DATA
	 */
	Model model = new Model();
	ListOf<Parameter> Parameters = new ListOf<>();
	List<LocalParameter> localParameters = new ArrayList<>();
	ListOf<Species> Species = new ListOf<>();
	ArrayList<String> ConstantSpecies = new ArrayList<>();
	ListOf<Reaction> Reactions = new ListOf<>();
	boolean hasTime=false;
	boolean hasSink=false;
	boolean hasSource=false;
	int numberOfConstantSpecies = 0;
	
	protected ListOf<Parameter> getParameters(){
		return Parameters;
	}
	protected BannedNamesList getBannedNames() {
		return bannednames;
	}
	protected ListOf<Species> getSpecies(){
		return Species;
	}
	protected ListOf<Reaction> getReactions(){
		return Reactions;
	}
	

	/**
	 * STRUCTURE VALIDATORS
	 */
	BannedNamesList bannednames = new BannedNamesList();
	SBOChecker checkSBO;
	KineticFormulaParser parser;


	public SBML2CRN(Model model, boolean forceMassAction){
		this.model = model;
		this.parser = new KineticFormulaParser(model);
		this.checkSBO = new SBOChecker(model);
		this.noSBO=forceMassAction;
		
		//You need these three methods to load the parameters,species and reactions in Isabel's data structures
		//	readReactions only checks if we have to add source/sink species
		readParameters();
		readSpecies();
		readReactions();
	}


	//  1.0 HANDLING THE PARAMETERS--------------------------------------------------------------------------------------------

	public void readParameters() /*throws IOException*/{
		if(!model.getListOfParameters().isEmpty())
			Parameters = model.getListOfParameters();
		parser.parameterMapping();
	}


	//  2.0 HANDLING THE SPECIES------------------------------------------------------------------------------------------------

	public void readSpecies(){
		if(!model.getListOfSpecies().isEmpty()){
			Species = model.getListOfSpecies();
			parser.speciesMapping();

			for(Species s: Species)
				if(s.isConstant()){
					ConstantSpecies.add(s.getId());
					numberOfConstantSpecies++;
				}
		}
	}

	public boolean hasTimeSpecies() {
		return hasTime;
	}
	public boolean hasSinkSpecies() {
		return hasSink;
	}
	public boolean hasSourceSpecies() {
		return hasSource;
	}

	public String getSpeciesStochiometry(SpeciesReference species){
		double value = 1;
		if(!Double.isNaN(species.getStoichiometry()))
			value = species.getStoichiometry();

		if(value!=1){
			if((int)value!=0){return ((int)value)+"*";}
			else
				return "";
		}
		else
			return "";

	}





	//  3.0 HANDLING THE REACTIONS----------------------------------------------------------------------------------------------

	public void readReactions() //throws ModelOverdeterminedException//, DerivativeException
	{
		if(!model.getListOfReactions().isEmpty()){
			Reactions = model.getListOfReactions();

			for (Reaction reaction : Reactions) {
				//if(reaction.getKineticLaw().getFormula().contains("time")){
				if(reaction.getKineticLaw().getMath().toFormula().contains("time")){
					hasTime = true;  hasSource = true;}
				if(reaction.getListOfReactants().isEmpty()&&reaction.getListOfModifiers().isEmpty())
					hasSource = true;
				if(reaction.getListOfProducts().isEmpty()&&reaction.getListOfModifiers().isEmpty())
					hasSink = true;
			}
		}
	}

	public Pair<ArrayList<String>, ArrayList<String>> handleConstantSpecies (Reaction r){
		MutableValue<Boolean> isNullToSpecies=new MutableValue<Boolean>(false);
		return handleConstantSpecies(r, isNullToSpecies);
	}
	
	public Pair<ArrayList<String>, ArrayList<String>> handleConstantSpecies (Reaction r, MutableValue<Boolean> isNullToSpecies){
		Pair<ArrayList<String>, ArrayList<String>> pair  = new Pair<>();

		//Case 1: Complete reaction, i.e, the reaction has reactants and products.
		if(!r.getListOfReactants().isEmpty()&&!r.getListOfProducts().isEmpty()){
			ArrayList<String> reactants = new ArrayList<>(numberOfConstantSpecies+r.getListOfReactants().size());
			ArrayList<String> products = new ArrayList<>(numberOfConstantSpecies+r.getListOfProducts().size());
			int constantReactantSpecies = 0;    int constantProductSpecies = 0;

			for(SpeciesReference rc : r.getListOfReactants()){
				reactants.add(getSpeciesStochiometry(rc)+bannednames.verifySingleTerm(rc.getSpecies()));
				if(ConstantSpecies.contains(rc.getSpecies()))
					constantReactantSpecies++;
			}

			for(SpeciesReference p : r.getListOfProducts()){
				products.add(getSpeciesStochiometry(p)+bannednames.verifySingleTerm(p.getSpecies()));
				if(ConstantSpecies.contains(p.getSpecies()))
					constantProductSpecies++;
			}

			if(constantReactantSpecies>0)
				for(SpeciesReference rc : r.getListOfReactants())
					if(ConstantSpecies.contains(rc.getSpecies()))
						products.add(getSpeciesStochiometry(rc)+bannednames.verifySingleTerm(rc.getSpecies()));

			if(constantProductSpecies>0)
				for(SpeciesReference p : r.getListOfProducts())
					if(ConstantSpecies.contains(p.getSpecies()))
						reactants.add(getSpeciesStochiometry(p)+bannednames.verifySingleTerm(p.getSpecies()));

			pair  = new Pair<>(reactants, products);
		}

		else{

			//------ Case 2. The reaction has no reactant but has modifiers and products

			if(!r.getListOfProducts().isEmpty()&&r.getListOfReactants().isEmpty()&&!r.getListOfModifiers().isEmpty()){
				//System.out.println(" Case 2. The reaction has no reactant, but has modifiers and products");
				ArrayList<String> modifiers = new ArrayList<>(numberOfConstantSpecies+r.getListOfModifiers().size());
				ArrayList<String> products = new ArrayList<>(numberOfConstantSpecies+r.getListOfProducts().size());   
				int constantProductSpecies = 0;

				for(ModifierSpeciesReference m : r.getListOfModifiers())
					modifiers.add(bannednames.verifySingleTerm(m.getSpecies()));

				for(SpeciesReference p : r.getListOfProducts()){
					products.add(getSpeciesStochiometry(p)+bannednames.verifySingleTerm(p.getSpecies()));
					if(ConstantSpecies.contains(p.getSpecies()))
						constantProductSpecies++;
				}

				if(constantProductSpecies>0)
					for(SpeciesReference p : r.getListOfProducts())
						if(ConstantSpecies.contains(p.getSpecies()))
							modifiers.add(getSpeciesStochiometry(p)+bannednames.verifySingleTerm(p.getSpecies()));

				pair = new Pair<>(modifiers, products);
			}

			//------ Case 3. The reaction has no products but has reactants and modifiers

			if(r.getListOfProducts().isEmpty()&&!r.getListOfReactants().isEmpty()&&!r.getListOfModifiers().isEmpty()){
				//System.out.println(" Case 3. The reaction has no products, but has reactants and modifiers");
				ArrayList<String> modifiers = new ArrayList<>(numberOfConstantSpecies+r.getListOfModifiers().size());
				ArrayList<String> reactants = new ArrayList<>(numberOfConstantSpecies+r.getListOfReactants().size());
				int constantReactantSpecies = 0;

				for(ModifierSpeciesReference m : r.getListOfModifiers())
					modifiers.add(bannednames.verifySingleTerm(m.getSpecies()));

				for(SpeciesReference rc : r.getListOfReactants()){
					reactants.add(getSpeciesStochiometry(rc)+bannednames.verifySingleTerm(rc.getSpecies()));
					if(ConstantSpecies.contains(rc.getSpecies()))
						constantReactantSpecies++;
				}

				if(constantReactantSpecies>0)
					for(SpeciesReference rc : r.getListOfReactants())
						if(ConstantSpecies.contains(rc.getSpecies()))
							modifiers.add(getSpeciesStochiometry(rc)+bannednames.verifySingleTerm(rc.getSpecies()));

				pair = new Pair<>(reactants, modifiers);
			}

			if(r.getListOfModifiers().isEmpty()) {

				//------ Case 4. The reaction has no reactant, no modifiers but has product. Type: null -> Species

				if(!r.getListOfProducts().isEmpty()&&r.getListOfReactants().isEmpty()){
					isNullToSpecies.setValue(true);
					//I have to say that I have added the Species 'source'. It has to be added to the rate
					ArrayList<String> sourceSpecies = new ArrayList<>(); 
					sourceSpecies.add(SOURCESPECIES);
					//System.out.println("Case 4. The reaction has no reactant, no modifiers but has product. Type: null -> Species");

					ArrayList<String> products = new ArrayList<>();
					//System.out.println(products.size());
					int constantProductSpecies = 0;


					for(SpeciesReference p : r.getListOfProducts()){
						products.add(getSpeciesStochiometry(p)+bannednames.verifySingleTerm(p.getSpecies()));
						if(ConstantSpecies.contains(p.getSpecies()))
							constantProductSpecies++;
					}

					if(constantProductSpecies>0)
						for(SpeciesReference p : r.getListOfProducts())
							if(ConstantSpecies.contains(p.getSpecies()))
								sourceSpecies.add(getSpeciesStochiometry(p)+bannednames.verifySingleTerm(p.getSpecies()));


					products.add(SOURCESPECIES);// in case that the synthesis is constant

					pair = new Pair<>(sourceSpecies, products);

				}            

				//------ Case 5. The degradation of a reactant. The reaction has no modifiers, no product. Type: Species -> SINK

				else{
					//System.out.println("Case 5. The reaction a reactant being degradaded. Has no modifiers, no product. Type: Species -> SINK");
					ArrayList<String> Sink = new ArrayList<>();
					Sink.add("SINK");
					ArrayList<String> reactants = new ArrayList<>();
					int constantReactantSpecies = 0;


					for(SpeciesReference rc : r.getListOfReactants()){
						reactants.add(getSpeciesStochiometry(rc)+bannednames.verifySingleTerm(rc.getSpecies()));
						if(ConstantSpecies.contains(rc.getSpecies()))
							constantReactantSpecies++;
					}


					if(constantReactantSpecies>0)
						for(SpeciesReference rc : r.getListOfReactants())
							if(ConstantSpecies.contains(rc.getSpecies()))
								Sink.add(getSpeciesStochiometry(rc)+bannednames.verifySingleTerm(rc.getSpecies()));

					pair = new Pair<>(reactants, Sink);   

				}                        

			}

		}

		return pair;

	}

	public String writeCompleteReaction(Reaction reaction, Pair<ArrayList<String>, ArrayList<String>> pair){
		String rc = "";
		String pr = "";
		String m="";
		String sentence = "";
		int count = 0;

		rc =  pair.getKey().get(0);
		for(int i = 1; i< pair.getKey().size(); i++)
			rc = rc.concat(" + "+ pair.getKey().get(i));

		pr = pair.getValue().get(0);
		for(int i = 1; i< pair.getValue().size(); i++)
			pr = pr.concat(" + "+ pair.getValue().get(i));

		if(!reaction.getListOfModifiers().isEmpty()){
			String modif[]= new String[reaction.getNumModifiers()];

			while(count!=reaction.getListOfModifiers().size()){
				ModifierSpeciesReference s = reaction.getListOfModifiers().get(count);
				modif[count]= bannednames.verifySingleTerm(s.getSpecies());
				count++;
			}

			m = modif[0];
			for (int i = 1;i<modif.length;i++)
				m = m.concat(" + "+modif[i]);  

			sentence = sentence.concat(rc+" + "+m+" -> "+m+" + "+pr);     
		}

		else{
			sentence = sentence.concat(rc+" -> "+pr);
		}


		return sentence;}

	public String writeReverseOfCompleteReaction (Reaction reaction, Pair<ArrayList<String>, ArrayList<String>> pair){
		String rc = "";
		String pr = "";
		String m="";
		String sentence = "";
		int count = 0;

		rc =  pair.getKey().get(0);
		for(int i = 1; i< pair.getKey().size(); i++)
			rc = rc.concat(" + "+ pair.getKey().get(i));

		pr = pair.getValue().get(0);
		for(int i = 1; i< pair.getValue().size(); i++)
			pr = pr.concat(" + "+ pair.getValue().get(i));

		if(!reaction.getListOfModifiers().isEmpty()){
			String modif[]= new String[reaction.getNumModifiers()];

			while(count!=reaction.getListOfModifiers().size()){
				ModifierSpeciesReference s = reaction.getListOfModifiers().get(count);
				modif[count]= bannednames.verifySingleTerm(s.getSpecies());
				count++;
			}

			m = modif[0];
			for (int i = 1;i<modif.length;i++)
				m = m.concat(" + "+modif[i]);  

			sentence = sentence.concat(pr+" + "+m+" -> "+m+" + "+rc);     
		}

		else{
			sentence = sentence.concat(pr+" -> "+rc);
		}


		return sentence;}

	public String writeReactionWithNoReactants (Reaction reaction, Pair<ArrayList<String>, ArrayList<String>> pair){
		String pr = "";
		String m="";    String m0 = "";
		String sentence = "";

		pr = pair.getValue().get(0);
		for(int i = 1; i< pair.getValue().size(); i++)
			pr = pr.concat(" + "+ pair.getValue().get(i));

		if(!reaction.getListOfModifiers().isEmpty()){

			m =  pair.getKey().get(0);
			for(int i = 1; i< pair.getKey().size(); i++)
				m = m.concat(" + "+ pair.getKey().get(i));

			m0 = reaction.getListOfModifiers().get(0).getSpecies();
			for(int i = 1; i< reaction.getListOfModifiers().size(); i++)
				m0 = m0.concat(" + "+reaction.getListOfModifiers().get(i).getSpecies());

			sentence = sentence.concat(m+" -> "+pr + " + "+m0);
		}

		else{
			String source = pair.getKey().get(0);
			for(int i = 1; i< pair.getKey().size(); i++)
				source = source.concat(" + "+ pair.getKey().get(i));

			sentence = sentence.concat(source +" -> "+pr);
		}

		return sentence;}

	public String writeReverseOfReactionWithNoReactants (Reaction reaction, Pair<ArrayList<String>, ArrayList<String>> pair){
		String pr = "";
		String m="";    String m0 = "";
		String sentence = "";

		pr = pair.getValue().get(0);
		for(int i = 1; i< pair.getValue().size(); i++)
			pr = pr.concat(" + "+ pair.getValue().get(i));

		if(!reaction.getListOfModifiers().isEmpty()){

			m =  pair.getKey().get(0);
			for(int i = 1; i< pair.getKey().size(); i++)
				m = m.concat(" + "+ pair.getKey().get(i));

			m0 = reaction.getListOfModifiers().get(0).getSpecies();
			for(int i = 1; i< reaction.getListOfModifiers().size(); i++)
				m0 = m0.concat(" + "+reaction.getListOfModifiers().get(i).getSpecies());

			sentence = sentence.concat(pr+" + "+m0+" -> "+m);
		}

		else{
			String source = pair.getKey().get(0);;
			for(int i = 1; i< pair.getKey().size(); i++)
				source = source.concat(" + "+ pair.getKey().get(i));

			sentence = sentence.concat(pr +" ->"+source);
		}

		return sentence;}

	public String writeReactionWithNoProducts (Reaction reaction, Pair<ArrayList<String>, ArrayList<String>> pair){
		String rc = "";
		String m="";    String m0="";
		String sentence = "";

		if(pair.isSetKey()&&pair.isSetValue()){
			rc =  pair.getKey().get(0);
			for(int i = 1; i< pair.getKey().size(); i++)
				rc = rc.concat(" + "+ pair.getKey().get(i));

			if(!reaction.getListOfModifiers().isEmpty()){

				m =  pair.getValue().get(0);
				for(int i = 1; i< pair.getValue().size(); i++)
					m = m.concat(" + "+ pair.getValue().get(i));

				m0 = getBannedNames().verifySingleTerm(reaction.getListOfModifiers().get(0).getSpecies());
				for(int i = 1; i< reaction.getListOfModifiers().size(); i++)
					m0 = m0.concat(" + "+getBannedNames().verifySingleTerm(reaction.getListOfModifiers().get(i).getSpecies()));

				sentence = sentence.concat(rc+" + "+m0+" -> "+m);
			}
			else{
				String Sink = pair.getValue().get(0);;
				for(int i = 1; i< pair.getValue().size(); i++)
					Sink = Sink.concat(" + "+ pair.getValue().get(i));

				sentence = sentence.concat(Sink +" ->"+rc);
			}
		}

		return sentence;}

	public String writeReverseOfReactionWithNoProducts (Reaction reaction, Pair<ArrayList<String>, ArrayList<String>> pair){
		String rc = "";
		String m="";    String m0="";
		String sentence = "";

		rc =  pair.getKey().get(0);
		for(int i = 1; i< pair.getKey().size(); i++)
			rc = rc.concat(" + "+ pair.getKey().get(i));

		if(!reaction.getListOfModifiers().isEmpty()){

			m =  pair.getValue().get(0);
			for(int i = 1; i< pair.getValue().size(); i++)
				m = m.concat(" + "+ pair.getValue().get(i));

			m0 = reaction.getListOfModifiers().get(0).getSpecies();
			for(int i = 1; i< reaction.getListOfModifiers().size(); i++)
				m0 = m0.concat(" + "+reaction.getListOfModifiers().get(i).getSpecies());

			sentence = sentence.concat(m+" -> "+m0+" -> "+rc);
		}

		else{
			String Sink = pair.getValue().get(0);;
			for(int i = 1; i< pair.getValue().size(); i++)
				Sink = Sink.concat(" + "+ pair.getValue().get(i));

			Sink = Sink.replaceFirst(SOURCESPECIES, "SINK"); //***touched this
			sentence = sentence.concat(rc+" ->"+Sink);
		}

		return sentence;}

	public String writeEquation (Reaction reaction){
		MutableValue<Boolean> mut=new MutableValue<Boolean>(false);
		return writeEquation(reaction,mut);
	}
	
	public String writeEquation (Reaction reaction, MutableValue<Boolean> isNullToSpecies){
		Pair<ArrayList<String>, ArrayList<String>> pair = handleConstantSpecies(reaction,isNullToSpecies);
		String equation = "";

		if (!reaction.getListOfReactants().isEmpty()&&!reaction.getListOfProducts().isEmpty()){
			equation = writeCompleteReaction(reaction, pair);
		}
		if(reaction.getListOfReactants().isEmpty()){
			equation = writeReactionWithNoReactants(reaction, pair);
		}
		if(reaction.getListOfProducts().isEmpty()){
			equation = writeReactionWithNoProducts(reaction, pair);
		}

		return equation;
	}

	public String writeReverseEquation (Reaction reaction){
		String equation = "";
		Pair<ArrayList<String>, ArrayList<String>> pair = handleConstantSpecies(reaction);

		if (!reaction.getListOfReactants().isEmpty()&&!reaction.getListOfProducts().isEmpty()){
			equation = writeReverseOfCompleteReaction(reaction, pair);
		}
		if(reaction.getListOfReactants().isEmpty()){
			equation = writeReverseOfReactionWithNoReactants(reaction, pair);
		}
		if(reaction.getListOfProducts().isEmpty()){
			equation = writeReverseOfReactionWithNoProducts(reaction, pair);
		}

		return equation;}


	public ReversibleReaction handleReaction(Reaction reaction) throws FluxBalanceAnalysisModel {
		String equation = "";
		String reverseEquation = "";
		ASTNode rate = new ASTNode();
		List<ASTNode> rates = new ArrayList<>();
		ReversibleReaction reversibleReaction;

		//ListOf<SpeciesReference> reactants = reaction.getListOfReactants();
		//ListOf<SpeciesReference> products = reaction.getListOfProducts();
		//ListOf<ModifierSpeciesReference> modifiers = reaction.getListOfModifiers();


		ASTNode kineticLaw = new ASTNode();
		if(reaction.isSetKineticLaw()) {
			kineticLaw = reaction.getKineticLaw().getMath();
		}
		else{
			reaction.createKineticLaw();
			//reaction.getKineticLaw().setFormula("1");
			kineticLaw=new ASTNode(1);
			reaction.getKineticLaw().setMath(kineticLaw);//Changed to avoid the deprecated.
		}
		
		if(searchForFluxValue(kineticLaw)) {
			throw new FluxBalanceAnalysisModel(reaction.toString());
		}
		

		//Andrea: noSBO means that I am imposing to treat this reaction as mass-action even if it does not have an explicit sbo tag saying it  
		//	an SBO label tells you if it is mass action or not (either the whole model, or each reaction). 
		// noSBO= the reaction has no tag. If noSBO has been set to true by me, then I know it is mass-action
		if(noSBO){
			reversibleReaction = handleMassActionModelsWithMissingSBOLabbel(reaction, kineticLaw, rate, rates, equation, reverseEquation/*, bw*/);
			//imposeMassActionIgnoringSBOLabel(reaction, kineticLaw, bw);
		}
		else {//if(!noSBO){
			if(reaction.isReversible()){
				String forwS;
				String revS;

				//Andrea: if the label says that this reaction is mass-action
				if(checkSBO.isStringMassAction(reaction.getKineticLaw().getSBOTermID())){
					rate = parser.recursiveParsingFormulaTree(kineticLaw);
					rate = parser.replaceFormulaByMapping(rate);
					rates = parser.getReversibleEqRates(rate, reaction, bannednames);
					//Andrea: equation is a reaction in erode format, where I add source or sink if necessary 
					equation = writeEquation(reaction);
					reverseEquation = writeReverseEquation(reaction);
					if(!rates.contains(new ASTNode("singleRate"))){
						forwS=equation+", "+parser.termVerification(parser.evaluatingFormulaTerms(rates.get(0),reaction),bannednames);
						revS=reverseEquation+", "+parser.termVerification(parser.evaluatingFormulaTerms(rates.get(1),reaction),bannednames);
						//bw.write(forwS+"\n");
						//bw.write(revS+"\n");
					}
					else{ //if(rates.contains(new ASTNode("singleRate"))){
						rates.remove(new ASTNode("singleRate"));
						forwS=equation+", "+parser.termVerification(parser.evaluatingFormulaTerms(rates.get(0),reaction),bannednames);
						revS=reverseEquation+", "+parser.termVerification(parser.evaluatingFormulaTerms(rates.get(0),reaction),bannednames);
						//bw.write(forwS+"\n");
						//bw.write(revS+"\n");
					}
					reversibleReaction=new ReversibleReaction(forwS, revS);
				}
				else{
					rate = parser.kineticLawCompiler(reaction,bannednames);
					equation = writeEquation(reaction);

					if(rate.isInteger()) {
						forwS=equation+", "+rate;
						//bw.write(forwS+"\n");
					}
					else {
						FormulaCompiler fc = new MyFormulaCompilerForDoubleSBML();
						//FormulaCompiler fc = new MyFormulaCompilerForDouble();
						ASTNodeValue compiledRate = rate.compile(fc);
						//System.out.println(compiledRate.toString());
						forwS=equation+", arbitrary "+compiledRate.toString();
						//bw.write(forwS+"\n");
					}
					reversibleReaction=new ReversibleReaction(forwS);
				}
			}

			else{
				String forwS;

				if(checkSBO.isStringMassAction(reaction.getKineticLaw().getSBOTermID())){

					rate = parser.recursiveParsingFormulaTree(kineticLaw);
					rate = parser.replaceFormulaByMapping(rate);
					rate = parser.termVerification(parser.evaluatingFormulaTerms(parser.getSingleRate(rate, reaction, bannednames), reaction), bannednames);
					equation = writeEquation(reaction);
					forwS=equation+", "+rate;
					//bw.write(forwS+"\n");
				}
				else{
					rate = parser.kineticLawCompiler(reaction,bannednames);
					
					MutableValue<Boolean> isNullToSpecies = new MutableValue<Boolean>(false);
					equation = writeEquation(reaction,isNullToSpecies);
					
					if(isNullToSpecies.getValue()) {
						//.equation.rate. = "source *("+rate+")";
						ASTNode source = new ASTNode(SOURCESPECIES);
						ASTNode mult = new ASTNode('*');
						mult.addChild(source);
						mult.addChild(rate);
						rate=mult;
					}
					
					if(rate.isInteger()) {
						forwS=equation+", "+rate;
						//bw.write(forwS+"\n");
					}
					else {
						FormulaCompiler fc = new MyFormulaCompilerForDoubleSBML();
						//FormulaCompiler fc = new MyFormulaCompilerForDouble();
						ASTNodeValue compiledRate = rate.compile(fc);
						//System.out.println(compiledRate.toString());
						forwS=equation+", arbitrary "+compiledRate.toString();
						//forwS=equation+", arbitrary "+rate;
						//bw.write(forwS+"\n");
					}

				}
				reversibleReaction=new ReversibleReaction(forwS);
			}
		}
		
		if(reversibleReaction!=null) {
			reversibleReaction.setIDAndDescr(reaction.getId(),reaction.getName());
		}
		return reversibleReaction;
	}
	
	/**
	 * Andrea: method to detect flux-balance analysis models
	 * @param kineticLaw
	 * @return
	 */
	private boolean searchForFluxValue(ASTNode kineticLaw) {
		if(kineticLaw.isName()) {
			for(String f : FLUX_VALUES) {
				if(kineticLaw.getName().equalsIgnoreCase(f)) {
					return true;
				}
			}
		}
		if(kineticLaw.getChildren()!=null) {
			for(ASTNode child : kineticLaw.getChildren()) {
				boolean ret = searchForFluxValue(child);
				if(ret) {
					return true;
				}
			}
		}
		return false;
	}
	//Andrea: I don't think you need kineticLaw as a parameter
	public ReversibleReaction handleMassActionModelsWithMissingSBOLabbel(Reaction reaction, ASTNode kineticLaw, ASTNode rate, List<ASTNode> rates, String equation, String reverseEquation/*, BufferedWriter bw*/){
		if(reaction.isReversible()){
			String forwS;
			String revS;
			rate = parser.recursiveParsingFormulaTree(kineticLaw);
			rate = parser.replaceFormulaByMapping(rate);
			rates = parser.getReversibleEqRates(rate, reaction, bannednames);
			equation = writeEquation(reaction);
			reverseEquation = writeReverseEquation(reaction);
			if(!rates.contains(new ASTNode("singleRate"))){
				forwS = equation+", "+parser.termVerification(parser.evaluatingFormulaTerms(rates.get(0),reaction),bannednames);
				revS = reverseEquation+", "+parser.termVerification(parser.evaluatingFormulaTerms(rates.get(1),reaction),bannednames);
				//bw.write(forwS+"\n");
				//bw.write(revS+"\n");
			}
			else { //if(rates.contains(new ASTNode("singleRate"))){
				rates.remove(new ASTNode("singleRate"));
				forwS = equation+", "+parser.termVerification(parser.evaluatingFormulaTerms(rates.get(0),reaction),bannednames);
				revS = reverseEquation+", "+parser.termVerification(parser.evaluatingFormulaTerms(rates.get(0),reaction),bannednames);
				//bw.write(forwS+"\n");
				//bw.write(revS+"\n");
			}
			return new ReversibleReaction(forwS,revS);
		}
		else{
			String forwS;
			rate = parser.recursiveParsingFormulaTree(kineticLaw);
			rate = parser.replaceFormulaByMapping(rate);
			rate = parser.termVerification(parser.evaluatingFormulaTerms(parser.getSingleRate(rate, reaction, bannednames), reaction), bannednames);
			equation = writeEquation(reaction);
			forwS = equation+", "+rate;
			//bw.write(forwS+"\n");
			return new ReversibleReaction(forwS);
		}
		

	}

	public boolean containsSpeciesRule(String id) {
		if(parser!=null ) {
			if(parser.speciesbyRule!=null && parser.speciesbyRule.containsKey(id)) {
				return true;
			}
			if(parser.speciesRateRule!=null && parser.speciesRateRule.containsKey(id)) {
				return true;
			}
		}
		return false;
	}
	
	
	


}
