package sbml.conversion.nodes.value;

import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.ReferenceToNodeUpdateFunction;
import sbml.conversion.nodes.elements.SBMLElement;

public class ValueWriter extends ValueASTConverter {

    private SBMLElement element;

    public ValueWriter(IUpdateFunction updateFunction) {
        super(updateFunction);
        this.element = new SBMLElement();
        this.convert();
    }

    @Override
    protected void convert() {
        Class<?> classType = updateFunction.getClass();
        if(classType.equals(ReferenceToNodeUpdateFunction.class))
            this.currentNode = element.reference(updateFunction);
        else
            this.currentNode = element.booleanConstant(updateFunction);
    }
}
