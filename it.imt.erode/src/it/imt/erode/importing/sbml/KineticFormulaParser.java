/*
@version 2017.12.06
 */
package it.imt.erode.importing.sbml;


//import parsers.*;
//import backups.readLibSBML20171012;
//import java.io.File;
//import java.io.IOException;
import java.util.ArrayList;
//import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//import java.util.Set;
//import java.util.stream.Collectors;
//import javax.swing.tree.TreeNode;
//import javax.xml.stream.XMLStreamException;
//import jdk.nashorn.internal.ir.annotations.Ignore;
//import org.apache.commons.math.ode.DerivativeException;
import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.ExplicitRule;
import org.sbml.jsbml.FunctionDefinition;
import org.sbml.jsbml.InitialAssignment;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Rule;
//import org.sbml.jsbml.SBMLDocument;
//import org.sbml.jsbml.SBMLException;
//import org.sbml.jsbml.SBMLReader;
//import org.sbml.jsbml.SBase;
import org.sbml.jsbml.Species;
//import org.sbml.jsbml.SpeciesReference;
//import org.sbml.jsbml.math.parser.FormulaParser;
//import org.sbml.jsbml.math.parser.FormulaParserTokenManager;
//import org.sbml.jsbml.math.test.RecursionTest;
//import org.sbml.jsbml.text.parser.ParseException;
//import org.sbml.jsbml.validator.ModelOverdeterminedException;
//import org.simulator.math.odes.AbstractDESSolver;
//import org.simulator.math.odes.MultiTable;
//import org.simulator.math.odes.RosenbrockSolver;
//import org.simulator.sbml.SBMLinterpreter;


/**
 *
 * @author Isabel Cristina Perez-Verona
 * @VERSION 2018.02.04
 * 
 */
public class KineticFormulaParser {
    
//0. VARIABLES ------------------------------------------------------------------------------------------------------------     
    Model model = new Model ();
    String parsedFormula = "";
    
/**
 * Description
 * formulaMapping : Structure to map the terms while performing substitutions.
 * speciesRateRule : Pairs the specie with its rate rule (if any)
 * speciesbyRule : Pairs the species, with the mathematical expression of is assignment rule, if any.
 * parameterByRule : Pairs the parameters and their Assignment Rules(if any). At the moment this is collecting the parameters and the Assignment Rules over such parameters.
 * 
 */
    Map<ASTNode, ASTNode> formulaMapping = new HashMap<>();
    Map<String, ASTNode> speciesbyRule = new HashMap<>();   
    Map<String, Rule> speciesRateRule = new HashMap<>();    
    Map<String, ASTNode> parameterByRule = new HashMap<>(); 
   

    
    public KineticFormulaParser(Model model){
        this.model = model;    
    }

/**
 * This methods makes a mapping <Parameter, Rule> for those parameters that are affected by rules.
 * It updates the variable parameterByRule
 */
    public void parameterMapping(){
        if(!model.getListOfParameters().isEmpty()&&!model.getListOfRules().isEmpty()){
            for(Parameter p: model.getListOfParameters())
                if(!p.isConstant()){
                    for (Rule rule : model.getListOfRules())
                           if(rule.isAssignment())
                            if(p.getId().equals(((ExplicitRule)rule).getVariable())){
                                parameterByRule.put(p.getId(),rule.getMath());
                            }
                }
        }
        else{
            if(!model.getListOfParameters().isEmpty()&&!model.getListOfInitialAssignments().isEmpty()){
                 for(Parameter p: model.getListOfParameters())
                        for (InitialAssignment init : model.getListOfInitialAssignments())
                            if(p.getId().equals(init.getVariable())){
                                    parameterByRule.put(p.getId(),init.getMath());
                                }
            }
        }
    }
    
    
/**
 * This method replaces in the equation the parameter with it's corresponding rule if the parameter is affected by a rule, 
 * else it returns the parameter
 * @param parameter
 * @return ASTNode parameter
 */
   private ASTNode recursiveParameterParser(ASTNode parameter) {
        boolean glaf = false;
        ASTNode newParameter = parameter.clone();

        if(newParameter.isFunction()&&isFD(newParameter))
            glaf = true;
        
        if(newParameter.isVariable()&&(model.getListOfParameters().get(newParameter.toString())!=null)&&!newParameter.isConstant()){
            Parameter p = model.getListOfParameters().get(newParameter.toString());
            if(parameterByRule.containsKey(p.getId())){
                ASTNode holder = parameterByRule.get(p.getId());
                holder = recursiveParameterParser(holder);
                newParameter = holder;

                glaf = true;
            }
        }                    
    
        if(!glaf){
        while(!newParameter.getChildren().isEmpty())
            newParameter.removeChild(0);
        
            for (ASTNode v : parameter.getChildren()) {
                ASTNode node = recursiveParameterParser(v);
                newParameter.addChild(node);
            }
        }
          
    return newParameter;
    }
  
   
/**
 * This methods makes a mapping <Species, Rule> for those species that are affected by rules,
 * it updates the variables speciesbyRule and speciesRateRule
 */
    public void speciesMapping (){
        if(!model.getListOfSpecies().isEmpty()&&!model.getListOfRules().isEmpty()){
            for(Species specie: model.getListOfSpecies()){
                for (Rule rule : model.getListOfRules())
                   if(rule.isAssignment()||rule.isRate())
                        if(specie.getId().equals(((ExplicitRule)rule).getVariable())){
                            if(rule.isAssignment())
                            speciesbyRule.put(specie.getId(),rule.getMath());
                                if(rule.isRate())
                                    speciesRateRule.put(specie.getId(),rule);

                        }
                }
        }
        else{
            if(!model.getListOfSpecies().isEmpty()&&!model.getListOfInitialAssignments().isEmpty()){
                for(Species specie: model.getListOfSpecies()){
                    for (InitialAssignment init : model.getListOfInitialAssignments())
                        //if(specie.getId().equals(init.getSymbol())){ // fixed to correct deprecated Andrea.
                        if(specie.getId().equals(init.getVariable())){
                                speciesbyRule.put(specie.getId(),init.getMath());
                            }
                    }
            }
        }
    }

    
/**
 * This method replaces in the equation each species with it's corresponding rule (if applies),
 * else it returns the node
 * @param kineticLaw
 * @return ASTNode node
 */
    private ASTNode recursiveSpeciesParser(ASTNode kineticLaw) {
    
    //System.out.println("\n-------------------Entered to the RECURSIVE  recursiveSpeciesParser\n");
        boolean glaf = false;
        ASTNode kineticLaw_copy = kineticLaw.clone();

        if(kineticLaw_copy.isFunction()&&isFD(kineticLaw_copy))
            glaf = true;

        if(kineticLaw_copy.isVariable()&&(model.getListOfSpecies().get(kineticLaw_copy.toString())!=null)){
        Species specie  = model.getListOfSpecies().get(kineticLaw_copy.toString());
                
        if((!specie.isConstant() && specie.isBoundaryCondition())||(!specie.isConstant() &&!specie.isBoundaryCondition()))    
            { 
            Species species = model.getListOfSpecies().get(kineticLaw_copy.toString());
             
                if(speciesbyRule.containsKey(species.getId())){
                    ASTNode holder = speciesbyRule.get(species.getId());
                    holder = recursiveSpeciesParser(holder);
                    kineticLaw_copy = holder;
                
                glaf = true;
                }
        }}                    
    
 
         
        if(!glaf){
            while(!kineticLaw_copy.getChildren().isEmpty())
                kineticLaw_copy.removeChild(0);


            for (ASTNode v : kineticLaw.getChildren()) {
                ASTNode node = recursiveSpeciesParser(v);
                kineticLaw_copy.addChild(node);
            }
        
        }
        
    return kineticLaw_copy;}

    
public boolean looksLikeMA(ASTNode kineticLaw){
    boolean MA = false;
    
            
    //check condition of MA.
    
    
    
return MA;}    
    
    
    
/**
 * This method is called when the equation is mass action. It splits the kinetic formula and returns a list with two positions containing the rates.
 * @param kineticLaw
 * @param reaction  
 * @param bannednames
 * @return List<ASTNode> contains the rate for the direct reaction in the first position, and the rate for the reverse reaction in the second position.
 */
    public List<ASTNode> getReversibleEqRates(ASTNode kineticLaw, Reaction reaction, BannedNamesList bannednames) {
        //System.out.println("_______entering into REVERSIBLE getReversibleEqRates");
        ASTNode node = kineticLaw.getChildren().get(0);
        ASTNode kineticLaw_copy = kineticLaw.clone();

        if(!node.isOperator()&&!node.isNumber()&&node.isVariable()){
            for(Compartment c : model.getListOfCompartments())
                if(c.getId().equals(node.toString())){
                    kineticLaw_copy.removeChild(0);}
        }

        ASTNode holder = kineticLaw_copy;
        if(kineticLaw_copy.getListOfNodes().size()==1){
            holder = kineticLaw_copy.getListOfNodes().get(0); }

        
        List<ASTNode> rates = new ArrayList<>(kineticLaw_copy.getChildCount());

        for(int i = 0; i < holder.getChildCount();i++){
            ASTNode no = (getSingleRate(holder.getChild(i), reaction, bannednames));
           rates.add(no);
        }
    return rates;
    }

    
/**
 * This method calculates a rate. 
 * @param kineticLaw
 * @param reaction
 * @param bannednames
 * @return an ASTNode containing a rate law.
 */
    public ASTNode getSingleRate (ASTNode kineticLaw, Reaction reaction, BannedNamesList bannednames) {
       
        //System.out.println("__________entering into SINGLE RATE");
        List<ASTNode> termList = kineticLaw.getListOfNodes();
        ASTNode kineticLaw_clon = kineticLaw.clone();
        ASTNode flag = new ASTNode("singleRate"); 
        
        if(!termList.isEmpty()){
            for(ASTNode node : termList){
                if(!node.isOperator()&&!node.isNumber()&&node.isVariable()){
                    //this can eliminate the compartment
                    for(Compartment c : model.getListOfCompartments())
                        if(c.getId().equals(node.toString())){ 
                            kineticLaw_clon.getListOfNodes().remove(node);}

                    for(Species species : model.getListOfSpecies())
                        if(species.getId().equals(node.toString())) { 
                            kineticLaw_clon.getListOfNodes().remove(node);}
                }
            }
          
          if(kineticLaw_clon.getListOfNodes().isEmpty())
              kineticLaw_clon = flag;
        }
        
        else{
            if(kineticLaw_clon.isVariable()){
                for(Compartment c : model.getListOfCompartments())
                    if(c.getId().equals(kineticLaw_clon.toString())){ 
                        kineticLaw_clon = flag;}

                for(Species species : model.getListOfSpecies())
                    if(species.getId().equals(kineticLaw_clon.toString())) { 
                        kineticLaw_clon = flag;}
                }
           }
    kineticLaw = kineticLaw_clon;
    
    
    return kineticLaw;
    }

    
/**
 * This method is called when there are no reactions. The rates are given directly by Rate Rules over parameters or species
 * @param specie
 * @param bannednames
 * @return 
 */
    public ASTNode directRateRuleCompiler(Species specie, BannedNamesList bannednames) {
        
        ASTNode n =fetchRateEquation(specie);
        ASTNode node1 =recursiveParameterParser(n);
        ASTNode node2 = recursiveParsingFormulaTree(node1);
        ASTNode node3 = evaluatingFormulaTerms2(node2, new Reaction("synthetic"));
        ASTNode node4 = termVerification(node3, bannednames);
        formulaMapping.clear();
    
        return node4;
    }
  
    
/**
 * This method parses the kineticLaw field of the reaction, 
 * to substitute functions by its definitions and variables by their mappings.
 * @param kineticLaw
 * @return ASTNode kineticLaw_copy
 */
    public ASTNode recursiveParsingFormulaTree(ASTNode kineticLaw) {
        //System.out.println("\n-------------------Entered to the RECURSIVE  parseFormulaTree\n");
        ASTNode kineticLaw_copy = kineticLaw.clone();
        boolean glaf = false;

        if(kineticLaw_copy.isVariable()&&(model.getListOfParameters().get(kineticLaw_copy.toString())!=null)&&!kineticLaw_copy.isConstant()){
            Parameter p = model.getListOfParameters().get(kineticLaw_copy.toString());
            if(parameterByRule.containsKey(p.getId())){
                ASTNode holder = recursiveParameterParser(kineticLaw_copy);
                kineticLaw_copy = holder;
            }
        }

        if(kineticLaw_copy.isFunction()&&isFD(kineticLaw_copy)){
            FunctionDefinition function  = getFunctionDefinitionMentionedInFormula(kineticLaw_copy);
                        
            for(int i=0;i<kineticLaw_copy.getChildCount();i++){
                formulaMapping.put(kineticLaw.getChild(i), function.getArgument(i));}   
           
            ASTNode holder = function.getBody();
            holder = recursiveParsingFormulaTree(holder);
            kineticLaw_copy = holder;
            
            glaf = true;
        }
            
        
        if(!glaf){
            while(!kineticLaw_copy.getChildren().isEmpty()){
                kineticLaw_copy.removeChild(0);}

            for (ASTNode v : kineticLaw.getChildren()) {
                ASTNode node = recursiveParsingFormulaTree(v);
                kineticLaw_copy.addChild(node);}
        }
        return kineticLaw_copy;
    }
   
    
/**
 * This method evaluated the terms in the formula
 * @param kineticLaw
 * @param reaction
 * @return 
 */
    public ASTNode evaluatingFormulaTerms(ASTNode kineticLaw, Reaction reaction){
        ASTNode kineticLaw_clon = kineticLaw.clone();         boolean glaf = false;
        
        if(kineticLaw_clon.isVariable()){
            for(Compartment comp : model.getListOfCompartments()){
                if (comp.getId().equals(kineticLaw.toFormula())){
                    ASTNode no = new ASTNode(comp.getSize());
                    kineticLaw_clon = no;
                    break;
                }
            }
            if(formulaMapping.containsValue(kineticLaw)){
                for(ASTNode o: formulaMapping.keySet()){
                    if(formulaMapping.get(o).equals(kineticLaw_clon)){
                        kineticLaw_clon = o.clone();
                        glaf = true; 
                    }
                }
            }
        
            for(Parameter p : model.getListOfParameters()){
                if(p.getId().equals(kineticLaw_clon.toString())&&!p.isConstant()){
                    ASTNode n = recursiveParameterParser(kineticLaw_clon);
                    kineticLaw_clon = n;
                    glaf = true;
                }
            }
            
            
            
            for(LocalParameter p : reaction.getKineticLaw().getListOfLocalParameters()){
                if (p.getId().equals(kineticLaw.toString())){
                	double val = p.getValue();
                	/*
                	String valStr = String.valueOf(val);
                	ASTNode no=null;
                	if(valStr.toLowerCase().contains("e-")) {
                		BigDecimal bdVal = new BigDecimal(val);
                		valStr = bdVal.toPlainString();
                		try {
    						no = ASTNode.parseFormula(valStr);
    					} catch (ParseException e) {
    						// ANDREA: there cannot be an exception here
    						e.printStackTrace();
    					}
                	}
                	else {
                		no = new ASTNode(val);
                	}
                	*/
                	ASTNode no = new ASTNode(val);
                    kineticLaw_clon = no;
                    break;
                }
            }
            
        } 
        
        if(!glaf){
            while(!kineticLaw_clon.getChildren().isEmpty())
                kineticLaw_clon.removeChild(0);

            for (ASTNode v : kineticLaw.getChildren()) {
                ASTNode node = evaluatingFormulaTerms(v,reaction);
                kineticLaw_clon.addChild(node);
            }
        }
        
    return kineticLaw_clon; 
    }
    
    
/**
 * Direct method to obtain a rate law. Used when it is possible to write the CRN.
 * @param reaction
 * @param name
 * @return rate law
 */
    public ASTNode kineticLawCompiler (Reaction reaction, BannedNamesList name) {
        
        ASTNode kineticLaw = reaction.getKineticLaw().getMath();
        ASTNode node1 =recursiveSpeciesParser(kineticLaw);
        ASTNode node11 =recursiveParameterParser(node1);
        ASTNode node2 = recursiveParsingFormulaTree(node11);
        ASTNode node3 = evaluatingFormulaTerms2(node2, reaction);
        ASTNode node4 = termVerification(node3, name);
        formulaMapping.clear();
    
    return node4;
    }
    
    
/**
 * This method fetches the mathematical expression corresponding to the rate of a species.
 * It is used for models with no reactions where the rate equations are obtained trough rate rules.
 * @param specie
 * @return 
 */
    private ASTNode fetchRateEquation(Species specie) {
        return (speciesRateRule.get(specie.getId())).getMath();
    }

    
/**
 * This method verifies every term against the list of banned terms, 
 * if the term is banned, then modifies it by adding a suffix.
 * @param formula
 * @param banned
 * @return cleaned term
 */    
    public ASTNode termVerification(ASTNode formula, BannedNamesList banned){
        //System.out.println(" entered to the termVerification");
        ASTNode clon = formula.clone();
        boolean glaf = false;

        if(banned.isBanned(clon.toFormula())){
            ASTNode holder = new ASTNode(banned.verifySingleTerm(formula.toString()));
            holder = termVerification(holder, banned);
            clon = holder;
            glaf = true;
        } 


        if(!glaf){
            
            while(!clon.getChildren().isEmpty())
                        clon.removeChild(0);
            
            for (ASTNode v : formula.getChildren()) {
                    ASTNode node = termVerification(v,banned);
                    clon.addChild(node);
            }
        } 

    return clon;
    }
    
    
/**
 * This method returns true if the given node encodes a FunctionDefinition, false otherwise.
 * @param formulaTerm
 * @return 
 */
    private boolean isFD(ASTNode formulaTerm) {
        ListOf<FunctionDefinition> FD = model.getListOfFunctionDefinitions();
        boolean flag=false;
        for (FunctionDefinition functionDefinition : FD)
            if (formulaTerm.toFormula().contains(functionDefinition.getId()))
                flag =true;
    return flag;
    }
    
    
/**
 * this method fetches the FunctionDefinition encoded in a ASTNode
 * @param formulaTerm
 * @return 
 */    
    private FunctionDefinition getFunctionDefinitionMentionedInFormula(ASTNode formulaTerm) {
      return  new FunctionDefinition(model.getListOfFunctionDefinitions().get(formulaTerm.getName()));
    }

    
/**
 * This method is the same than evaluatingFormulaTerms, with the difference that compares against the variable.toString
 * @param kineticLaw
 * @param reaction
 * @return 
 */
    private ASTNode evaluatingFormulaTerms2(ASTNode kineticLaw, Reaction reaction) {
        ASTNode kineticLaw_clon = kineticLaw.clone();
        boolean glaf = false;
        if(kineticLaw_clon.isVariable()){
            for(Compartment comp : model.getListOfCompartments()){
                if (comp.getId().equals(kineticLaw.toString())){
                    ASTNode no = new ASTNode(comp.getSize());
                    kineticLaw_clon = no;
                    break;
                    }
                }

            if(formulaMapping.containsValue(kineticLaw)){
                for(ASTNode o: formulaMapping.keySet()){
                    if(formulaMapping.get(o).equals(kineticLaw_clon)){
                        kineticLaw_clon = o.clone();
                        glaf = true; //break;
                    }
                }
            }
            if(!reaction.getId().equals("synthetic")){
                for(LocalParameter p : reaction.getKineticLaw().getListOfLocalParameters()){
                    if (p.getId().equals(kineticLaw.toString())){
                        ASTNode no = new ASTNode(p.getValue());
                        kineticLaw_clon = no;
                        break;
                    }
                }
            }

        } 
        
        if(!glaf){
            while(!kineticLaw_clon.getChildren().isEmpty())
                kineticLaw_clon.removeChild(0);

            for (ASTNode v : kineticLaw.getChildren()) {
                ASTNode node = evaluatingFormulaTerms(v,reaction);
                kineticLaw_clon.addChild(node);
            }
        }

        return kineticLaw_clon;
    }

    public ASTNode replaceFormulaByMapping (ASTNode kineticLaw){
        ASTNode kineticLaw_clon = kineticLaw.clone();
        boolean glaf = false;
       
        if(kineticLaw_clon.isVariable()&&formulaMapping.containsValue(kineticLaw)){
            for(ASTNode o: formulaMapping.keySet()){
                if(formulaMapping.get(o).equals(kineticLaw_clon)){
                    kineticLaw_clon = o.clone();
                    glaf = true; 
                }
            }
        }
        
        if(!glaf){
        while(!kineticLaw_clon.getChildren().isEmpty())
            kineticLaw_clon.removeChild(0);

        for (ASTNode v : kineticLaw.getChildren()) {
            ASTNode node = replaceFormulaByMapping(v);
            kineticLaw_clon.addChild(node);
        }}

    return kineticLaw_clon;
    }

}
