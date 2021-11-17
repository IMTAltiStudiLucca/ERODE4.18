package sbml.demos;

import java.io.File;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.ext.qual.FunctionTerm;
import org.sbml.jsbml.ext.qual.Input;
import org.sbml.jsbml.ext.qual.Output;
import org.sbml.jsbml.ext.qual.QualModelPlugin;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Transition;

public class JSBMLReadingDemo {
	
	@SuppressWarnings("unused")
	public static void main(String[] args) throws IOException, XMLStreamException {
		String path = "Insert path to sbml file here";

		//Read the SBML file and retrieve the data representation
		SBase sbmlEntity = SBMLReader.read(new File(path));
		SBMLDocument sbmlDocument = (SBMLDocument) sbmlEntity;

		//Retrieve the model contained in the document
		Model model = sbmlDocument.getModel();

		//Retrieve the SBML-qual extension
		QualModelPlugin qualModel = (QualModelPlugin) model.getExtension("qual");

		//Retrieve the model's species
		ListOf<QualitativeSpecies> species = qualModel.getListOfQualitativeSpecies();

		//Retrieve the model's transitions
		ListOf<Transition> transitions = qualModel.getListOfTransitions();

		//Retrieve a specific transition
		Transition transition = transitions.get(0);

		//Retrieve inputs
		ListOf<Input> inputs = transition.getListOfInputs();

		//Retrieve outputs
		ListOf<Output> outputs = transition.getListOfOutputs();

		//Retrieve function terms
		ListOf<FunctionTerm> functionTerms = transition.getListOfFunctionTerms();

		//Retrieve a specific function term
		FunctionTerm functionTerm = functionTerms.get(0);

		//Retrieve the AST for the mathematical expression contained in the function term
		ASTNode root = functionTerm.getMath();

	}
}