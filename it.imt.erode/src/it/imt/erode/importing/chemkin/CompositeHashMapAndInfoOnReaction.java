package it.imt.erode.importing.chemkin;

import java.util.HashMap;

import it.imt.erode.crn.interfaces.ISpecies;

public class CompositeHashMapAndInfoOnReaction {

	private HashMap<ISpecies, Integer> compositeHM;
	
	private ChemKinReactionNote note;
	
	protected HashMap<ISpecies, Integer> getCompositeHM() {
		return compositeHM;
	}

	protected ChemKinReactionNote getNote() {
		return note;
	}

	public CompositeHashMapAndInfoOnReaction(
			HashMap<ISpecies, Integer> compositeHM,
			ChemKinReactionNote note) {
		super();
		this.compositeHM = compositeHM;
		this.note=note;
	}
	
	@Override
	public String toString() {
		return note.toString()+" "+compositeHM.toString();
	}
	
	
}
