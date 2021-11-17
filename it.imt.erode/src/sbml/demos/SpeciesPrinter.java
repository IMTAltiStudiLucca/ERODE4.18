package sbml.demos;

import java.io.File;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.ext.qual.QualModelPlugin;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;

public class SpeciesPrinter {

    public static void main(String[] args) throws IOException, XMLStreamException {
        String path = "D:/Repositories/SBML-Converter-for-ERODE/src/main/resources/sbml/demos/Trp_reg.sbml";
        SBMLDocument tree = SBMLReader.read(new File(path));
        SpeciesPrinter printer = new SpeciesPrinter();
        ListOf<QualitativeSpecies> species = printer.getListOfSpecies(tree);
        printer.printAllSpecies(species);
    }

    private ListOf<QualitativeSpecies> getListOfSpecies(SBase tree) {
        QualModelPlugin qual = (QualModelPlugin) tree.getModel().getExtension("qual");
        return qual.getListOfQualitativeSpecies();
    }

    private void printSpecies(QualitativeSpecies q) {
        System.out.println("Name: " + q.getName());
        System.out.println("Compartment: " + q.getCompartment());
        System.out.println("Constant: " + q.getConstant());
        System.out.println("Id: " + q.getId());
        System.out.println("Max Level: " + q.getMaxLevel()+ "\n\n");
    }

    private void printAllSpecies(ListOf<QualitativeSpecies> species) {
        for(QualitativeSpecies q : species) {
            this.printSpecies(q);
        }
    }
}
