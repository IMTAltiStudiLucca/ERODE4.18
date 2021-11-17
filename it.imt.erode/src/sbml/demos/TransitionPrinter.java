package sbml.demos;

import java.io.File;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.ext.qual.Input;
import org.sbml.jsbml.ext.qual.Output;
import org.sbml.jsbml.ext.qual.QualModelPlugin;
import org.sbml.jsbml.ext.qual.Transition;

public class TransitionPrinter {
    public static void main(String[] args) throws IOException, XMLStreamException {
        String path = "D:/Repositories/SBML-Converter-for-ERODE/src/main/resources/sbml/demos/Trp_reg.sbml";
        SBase tree = SBMLReader.read(new File(path));
        TransitionPrinter printer = new TransitionPrinter();
        ListOf<Transition> transitions = printer.getListOfTransitions(tree);
        printer.printAllTransitions(transitions);
    }

    private ListOf<Transition> getListOfTransitions(SBase tree) {
        QualModelPlugin qual = (QualModelPlugin) tree.getModel().getExtension("qual");
        return qual.getListOfTransitions();
    }

    private void printInput(Input input) {
        System.out.println("  Input ID: " + input.getId());
        System.out.println("  Input Species: " + input.getQualitativeSpecies());
        System.out.println("  Input Sign: " + input.getSign());
        System.out.println("  Transition Effect: " + input.getTransitionEffect());
        System.out.println();
    }

    private void printTransition(Transition t) {
        System.out.println("Transition ID: " + t.getId());
        ListOf<Input> inputs = t.getListOfInputs();
        ListOf<Output> outputs = t.getListOfOutputs();

        for(Input i : inputs)
            this.printInput(i);
        for(Output o : outputs)
            this.printOutput(o);
        System.out.println();
    }

    private void printOutput(Output o) {
        System.out.println("  Output ID: " + o.getId());
        System.out.println("  Output Species: " + o.getQualitativeSpecies());
        System.out.println("  Transition Effect: " + o.getTransitionEffect());
        System.out.println();
    }

    private void printAllTransitions(ListOf<Transition> transitions) {
        for(Transition t : transitions) {
            this.printTransition(t);
        }
    }
}
