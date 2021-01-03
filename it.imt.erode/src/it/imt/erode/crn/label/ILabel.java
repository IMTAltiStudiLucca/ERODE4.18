package it.imt.erode.crn.label;

public interface ILabel {

	/**
	 * 
	 * @return the arity of reactions this label refers to: 0 for emptyset, 1 for singleton species, the size of the multi-set for all other cases
	 */
	int getReferredArity();

	int getLabelID();

}
