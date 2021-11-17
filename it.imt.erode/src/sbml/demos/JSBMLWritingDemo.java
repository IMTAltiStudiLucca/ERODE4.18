package sbml.demos;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.ext.qual.QualModelPlugin;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Transition;

public class JSBMLWritingDemo {

	private static final int LEVEL = 3;
	private static final int VERSION = 1;

	public static void main(String[] args) throws IOException, XMLStreamException {

		//Demo: Adding an extension to an existing SBML Document

		//Initialisations
		SBMLDocument sbmlDocument = new SBMLDocument(LEVEL, VERSION);
		Model model = new Model(LEVEL, VERSION);
		QualModelPlugin qualModel = new QualModelPlugin(model);

		//Create model content
		ListOf<QualitativeSpecies> species = new ListOf<>(LEVEL, VERSION);
		ListOf<Transition> transitions = new ListOf<>(LEVEL, VERSION);

		//Add model to the SBML representation
		sbmlDocument.setModel(model);

		//Add SBML-qual extension
		model.addExtension("qual", qualModel);

		//Add model content to the extesion model
		species.setParent(qualModel.getModel());
		qualModel.setListOfQualitativeSpecies(species);

		transitions.setParent(qualModel.getModel());
		qualModel.setListOfTransitions(transitions);

	}
}