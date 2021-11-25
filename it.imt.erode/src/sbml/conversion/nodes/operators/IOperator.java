package sbml.conversion.nodes.operators;

import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.crn.symbolic.constraints.BasicConstraintComparator;

public interface IOperator<T> {

    T not(T x);

    T and(T x, T y);

    T or(T x, T y);

    T xor(T x, T y);

    T implies(T x, T y);

    T equals(T x, T y);

    T notEquals(T x, T y);
    
    T plus(T x, T y);
    T product(T x, T y);
    T comparison(T x, T y,BasicConstraintComparator cmp);

	IUpdateFunction bn_comparison(IUpdateFunction l, IUpdateFunction r, BasicConstraintComparator cmp);
}
