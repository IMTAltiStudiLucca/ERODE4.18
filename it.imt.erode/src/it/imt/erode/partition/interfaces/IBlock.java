package it.imt.erode.partition.interfaces;

import java.math.BigDecimal;
import java.util.Collection;

import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SpeciesSplayBST;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SpeciesSplayBSTWithTolerance;
import it.imt.erode.partitionrefinement.splittersbstandcounters.VectorOfBigDecimalsForEachLabel;

public interface IBlock {

	public SpeciesSplayBST<BigDecimal, IBlock> getBST();

	public void throwAwayBST();
	
	public IBlock getPrev();
	
	public IBlock getNext();
	
	public void setPrev(IBlock block);
	
    public void setNext(IBlock block);
	
	public Collection<ISpecies> getSpecies();

	public void addSpecies(ISpecies species);

	public boolean isEmpty();

	public void removeSpecies(ISpecies species);

	public ISpecies getRepresentative();
	
	ISpecies getRepresentative(boolean getMinSpecies);

	public BigDecimal getBlockConcentration();

	public String toStringSeparatedSpeciesNames(String sep);

	public void setPartition(IPartition partition);

	public IBlock copyAndAddToPartition(IPartition partition);

	public Collection<String> computeBlockComment();

	public String computeBlockConcentrationExpr();

	void updatePartition(IPartition partition);

	public SpeciesSplayBSTWithTolerance<BigDecimal, IBlock> getBST(BigDecimal tolerance);

	void throwAwayBSTWithTolerance();

	SpeciesSplayBST<VectorOfBigDecimalsForEachLabel, IBlock> getBSTForVectors();

	void throwAwayBSTForVectors();

	public void setCanBeUsedAsSplitter(boolean val);
	
	public boolean canBeUsedAsSplitter();

	public void setHasBeenAlreadyUsedAsSplitter(boolean val);

	boolean hasBeenAlreadyUsedAsSplitter();

	public void setMinAsRepresentative();
}
