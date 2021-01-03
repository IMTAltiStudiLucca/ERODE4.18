/*

SBO Checklist built with information extracted from Systems Biology Ontology 
(http://www.ebi.ac.uk/sbo/main/)

Is used to check agains the SBO terms in the kineticLaws of the reactions 
to determine if the equation should be treeated as mass-action.

 */
package it.imt.erode.importing.sbml;

import java.util.HashMap;
import java.util.Map;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;



/**
 *
 * @author Isabel Cristina Perez-Verona
 */
public class SBOChecker {
    
    Map <String,String> terms ;
    Model model;
    
    public SBOChecker(){}
    
    public SBOChecker (Model model){
     this.model = model;
     terms = new HashMap<>();     
     initSBO_TermList();
    }
        
    public boolean isStringMassAction(String string){
        return terms.containsKey(string);}
    
    public void initSBO_TermList(){
        terms.put("SBO:0000562","mass action like rate law for second order irreversible reactions, one reactant, one essential stimulator");
        terms.put("SBO:0000563","mass action like rate law for second order irreversible reactions, one reactant, one essential stimulator, continuous scheme");
        terms.put("SBO:0000564","mass action like rate law for second order irreversible reactions, one reactant, one essential stimulator, discrete scheme");
        terms.put("SBO:0000012","mass action rate law");
        terms.put("SBO:0000080","mass action rate law for first order forward, first order reverse, reversible reactions, continuous scheme");
        terms.put("SBO:0000081","mass action rate law for first order forward, second order reverse, reversible reactions");
        terms.put("SBO:0000082","mass action rate law for first order forward, second order reverse, reversible reactions, one product, continuous scheme");
        terms.put("SBO:0000083","mass action rate law for first order forward, second order reverse, reversible reactions, two products, continuous scheme");
        terms.put("SBO:0000084","mass action rate law for first order forward, third order reverse, reversible reactions");
        terms.put("SBO:0000085","mass action rate law for first order forward, third order reverse, reversible reactions, one product, continuous scheme");
        terms.put("SBO:0000087","mass action rate law for first order forward, third order reverse, reversible reactions, three products, continuous scheme");
        terms.put("SBO:0000086","mass action rate law for first order forward, third order reverse, reversible reactions, two products, continuous scheme");
        terms.put("SBO:0000079","mass action rate law for first order forward, zeroth order reverse, reversible reactions, continuous scheme");
        terms.put("SBO:0000044","mass action rate law for first order irreversible reactions");
        terms.put("SBO:0000049","mass action rate law for first order irreversible reactions, continuous scheme");
        terms.put("SBO:0000141","mass action rate law for first order irreversible reactions, discrete scheme");
        terms.put("SBO:0000560","mass action rate law for first order irreversible reactions, single essential stimulator, continuous scheme");
        terms.put("SBO:0000561","mass action rate law for first order irreversible reactions, single essential stimulator, discrete scheme");
        terms.put("SBO:0000078","mass action rate law for first order reversible reactions");
        terms.put("SBO:0000041","mass action rate law for irreversible reactions");
        terms.put("SBO:0000163","mass action rate law for irreversible reactions, continuous scheme");
        terms.put("SBO:0000166","mass action rate law for irreversible reactions, discrete scheme");
        terms.put("SBO:0000042","mass action rate law for reversible reactions");
        terms.put("SBO:0000646","mass action rate law for reversible reactions, continuous schema");
        terms.put("SBO:0000091","mass action rate law for second order forward, first order reverse, reversible reactions, one reactant, continuous scheme");
        terms.put("SBO:0000101","mass action rate law for second order forward, first order reverse, reversible reactions, two reactants, continuous scheme");
        terms.put("SBO:0000089","mass action rate law for second order forward, reversible reactions, one reactant");
        terms.put("SBO:0000099","mass action rate law for second order forward, reversible reactions, two reactants");
        terms.put("SBO:0000092","mass action rate law for second order forward, second order reverse, reversible reactions, one reactant");
        terms.put("SBO:0000093","mass action rate law for second order forward, second order reverse, reversible reactions, one reactant, one product, continuous scheme");
        terms.put("SBO:0000094","mass action rate law for second order forward, second order reverse, reversible reactions, two products, continuous scheme");
        terms.put("SBO:0000102","mass action rate law for second order forward, second order reverse, reversible reactions, two reactants");
        terms.put("SBO:0000103","mass action rate law for second order forward, second order reverse, reversible reactions, two reactants, one product, continuous scheme");
        terms.put("SBO:0000104","mass action rate law for second order forward, second order reverse, reversible reactions, two reactants, two products, continuous scheme");
        terms.put("SBO:0000095","mass action rate law for second order forward, third order reverse, reversible reactions, one reactant");
        terms.put("SBO:0000096","mass action rate law for second order forward, third order reverse, reversible reactions, one reactant, one product, continuous scheme");
        terms.put("SBO:0000098","mass action rate law for second order forward, third order reverse, reversible reactions, one reactant, three products, continuous scheme");
        terms.put("SBO:0000097","mass action rate law for second order forward, third order reverse, reversible reactions, one reactant, two products, continuous scheme");
        terms.put("SBO:0000105","mass action rate law for second order forward, third order reverse, reversible reactions, two reactants");
        terms.put("SBO:0000106","mass action rate law for second order forward, third order reverse, reversible reactions, two reactants, one product, continuous scheme");
        terms.put("SBO:0000108","mass action rate law for second order forward, third order reverse, reversible reactions, two reactants, three products, continuous scheme");
        terms.put("SBO:0000107","mass action rate law for second order forward, third order reverse, reversible reactions, two reactants, two products, continuous scheme");
        terms.put("SBO:0000090","mass action rate law for second order forward, zeroth order reverse, reversible reactions, one reactant, continuous scheme");
        terms.put("SBO:0000100","mass action rate law for second order forward, zeroth order reverse, reversible reactions, two reactants, continuous scheme");
        terms.put("SBO:0000045","mass action rate law for second order irreversible reactions");
        terms.put("SBO:0000050","mass action rate law for second order irreversible reactions, one reactant");
        terms.put("SBO:0000052","mass action rate law for second order irreversible reactions, one reactant, continuous scheme");
        terms.put("SBO:0000142","mass action rate law for second order irreversible reactions, one reactant, discrete scheme");
        terms.put("SBO:0000053","mass action rate law for second order irreversible reactions, two reactants");
        terms.put("SBO:0000054","mass action rate law for second order irreversible reactions, two reactants, continuous scheme");
        terms.put("SBO:0000143","mass action rate law for second order irreversible reactions, two reactants, discrete scheme");
        terms.put("SBO:0000088","mass action rate law for second order reversible reactions");
        terms.put("SBO:0000132","mass action rate law for third order forward, first order reverse, reversible reactions, one reactant, continuous scheme");
        terms.put("SBO:0000122","mass action rate law for third order forward, first order reverse, reversible reactions, three reactants, continuous scheme");
        terms.put("SBO:0000112","mass action rate law for third order forward, first order reverse, reversible reactions, two reactants, continuous scheme");
        terms.put("SBO:0000130","mass action rate law for third order forward, reversible reactions, one reactant");
        terms.put("SBO:0000120","mass action rate law for third order forward, reversible reactions, three reactants");
        terms.put("SBO:0000110","mass action rate law for third order forward, reversible reactions, two reactants");
        terms.put("SBO:0000133","mass action rate law for third order forward, second order reverse, reversible reactions, one reactant");
        terms.put("SBO:0000134","mass action rate law for third order forward, second order reverse, reversible reactions, one reactant, one product, continuous scheme");
        terms.put("SBO:0000135","mass action rate law for third order forward, second order reverse, reversible reactions, one reactant, two products, continuous scheme");
        terms.put("SBO:0000123","mass action rate law for third order forward, second order reverse, reversible reactions, three reactants");
        terms.put("SBO:0000124","mass action rate law for third order forward, second order reverse, reversible reactions, three reactants, one product, continuous scheme");
        terms.put("SBO:0000125","mass action rate law for third order forward, second order reverse, reversible reactions, three reactants, two products, continuous scheme");
        terms.put("SBO:0000113","mass action rate law for third order forward, second order reverse, reversible reactions, two reactants");
        terms.put("SBO:0000114","mass action rate law for third order forward, second order reverse, reversible reactions, two reactants, one product, continuous scheme");
        terms.put("SBO:0000115","mass action rate law for third order forward, second order reverse, reversible reactions, two reactants, two products, continuous scheme");
        terms.put("SBO:0000136","mass action rate law for third order forward, third order reverse, reversible reactions, one reactant");
        terms.put("SBO:0000137","mass action rate law for third order forward, third order reverse, reversible reactions, one reactant, one product, continuous scheme");
        terms.put("SBO:0000139","mass action rate law for third order forward, third order reverse, reversible reactions, one reactant, three products, continuous scheme");
        terms.put("SBO:0000138","mass action rate law for third order forward, third order reverse, reversible reactions, one reactant, two products, continuous scheme");
        terms.put("SBO:0000126","mass action rate law for third order forward, third order reverse, reversible reactions, three reactants");
        terms.put("SBO:0000127","mass action rate law for third order forward, third order reverse, reversible reactions, three reactants, one product, continuous scheme");
        terms.put("SBO:0000129","mass action rate law for third order forward, third order reverse, reversible reactions, three reactants, three products, continuous scheme");
        terms.put("SBO:0000128","mass action rate law for third order forward, third order reverse, reversible reactions, three reactants, two products, continuous scheme");
        terms.put("SBO:0000116","mass action rate law for third order forward, third order reverse, reversible reactions, two reactants");
        terms.put("SBO:0000117","mass action rate law for third order forward, third order reverse, reversible reactions, two reactants, one product, continuous scheme");
        terms.put("SBO:0000119","mass action rate law for third order forward, third order reverse, reversible reactions, two reactants, three products, continuous scheme");
        terms.put("SBO:0000118","mass action rate law for third order forward, third order reverse, reversible reactions, two reactants, two products, continuous scheme");
        terms.put("SBO:0000131","mass action rate law for third order forward, zeroth order reverse, reversible reactions, one reactant, continuous scheme");
        terms.put("SBO:0000121","mass action rate law for third order forward, zeroth order reverse, reversible reactions, three reactants, continuous scheme");
        terms.put("SBO:0000111","mass action rate law for third order forward, zeroth order reverse, reversible reactions, two reactants, continuous scheme");
        terms.put("SBO:0000055","mass action rate law for third order irreversible reactions");
        terms.put("SBO:0000056","mass action rate law for third order irreversible reactions, one reactant");
        terms.put("SBO:0000057","mass action rate law for third order irreversible reactions, one reactant, continuous scheme");
        terms.put("SBO:0000144","mass action rate law for third order irreversible reactions, one reactant, discrete scheme");
        terms.put("SBO:0000060","mass action rate law for third order irreversible reactions, three reactants");
        terms.put("SBO:0000061","mass action rate law for third order irreversible reactions, three reactants, continuous scheme");
        terms.put("SBO:0000146","mass action rate law for third order irreversible reactions, three reactants, discrete scheme");
        terms.put("SBO:0000058","mass action rate law for third order irreversible reactions, two reactants");
        terms.put("SBO:0000059","mass action rate law for third order irreversible reactions, two reactants, continuous scheme");
        terms.put("SBO:0000145","mass action rate law for third order irreversible reactions, two reactants, discrete scheme");
        terms.put("SBO:0000109","mass action rate law for third order reversible reactions");
        terms.put("SBO:0000070","mass action rate law for zeroth order forward, first order reverse, reversible reactions, continuous scheme");
        terms.put("SBO:0000071","mass action rate law for zeroth order forward, second order reverse, reversible reactions, continuous scheme");
        terms.put("SBO:0000072","mass action rate law for zeroth order forward, second order reverse, reversible reactions, one product, continuous scheme");
        terms.put("SBO:0000073","mass action rate law for zeroth order forward, second order reverse, reversible reactions, two products, continuous scheme");
        terms.put("SBO:0000074","mass action rate law for zeroth order forward, third order reverse, reversible reactions, continuous scheme");
        terms.put("SBO:0000075","mass action rate law for zeroth order forward, third order reverse, reversible reactions, one product, continuous scheme");
        terms.put("SBO:0000077","mass action rate law for zeroth order forward, third order reverse, reversible reactions, three products, continuous scheme");
        terms.put("SBO:0000076","mass action rate law for zeroth order forward, third order reverse, reversible reactions, two products, continuous scheme");
        terms.put("SBO:0000043","mass action rate law for zeroth order irreversible reactions");
        terms.put("SBO:0000047","mass action rate law for zeroth order irreversible reactions, continuous scheme");
        terms.put("SBO:0000140","mass action rate law for zeroth order irreversible reactions, discrete scheme");
        terms.put("SBO:0000069","mass action rate law for zeroth order reversible reactions");
        terms.put("SBO:0000164","second order irreversible mass action kinetics, continuous scheme (obsolete term)");
        terms.put("SBO:0000165","third order irreversible mass action kinetics, continuous scheme (obsolete term)");
        terms.put("SBO:0000012","mass action rate law");
        terms.put("SBO:0000079","mass action rate law for first order forward, zeroth order reverse, reversible reactions, continuous scheme");
        terms.put("SBO:0000104","mass action rate law for second order forward, second order reverse, reversible reactions, two reactants, two products, continuous scheme");
        terms.put("SBO:0000125","mass action rate law for third order forward, second order reverse, reversible reactions, three reactants, two products, continuous scheme");

    }

    public boolean IsMassAction (){
    boolean flag= false;
    if(!model.getListOfReactions().isEmpty()){
        ListOf<Reaction> R = model.getListOfReactions();
        
        for(Reaction reaction : R){
        if(isStringMassAction(reaction.getKineticLaw().getSBOTermID()))
            flag=true;
            break;
            }
    
    
    }
    return flag;}

//    public static void main(String[] args) throws IOException, XMLStreamException{
//
//String path = "D:\\xmlCurated\\xmlCuratedurl";
//int count = 0;
//File[] files = new File(path).listFiles();
//            for (File file : files) {
//                if(file.isFile()){
//                    SBMLDocument doc = new SBMLReader().readSBML(file);
//                    Model model = doc.getModel();
//                    SBOChecker checker = new SBOChecker(model);
// 
//                    if(checker.IsMassAction()){
//                        System.out.println("            MASS ACTION "+doc.getModel().getName()+" NAME"+ file.getName() +"\n");
//                        count++;}
//
//                    else {
//                        System.out.println(file.getName()+" was analized \n");
//                    }
//                    
//
//                }
//            }
//            System.out.println("There are "+count+" MASS ACTION models");
//
//    }



}

