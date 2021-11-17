package sbml.conversion.nodes.elements;

public interface IElement<T,T2> {

    T2 reference(T node);

    T2 constant(T node,boolean mv);

    T2 booleanConstant(T node);
}
