package it.imt.erode.auxiliarydatastructures;

import java.util.HashSet;

import it.imt.erode.crn.label.ILabel;

public class SetOfLabels {

	HashSet<ILabel> labels;
	public boolean add(ILabel l) {
		if(labels==null) {
			labels=new HashSet<>();
		}
		return labels.add(l);
	}
}
