package it.imt.erode.partition.interfaces;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.crn.label.ILabel;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SplittersGenerator;

public interface IPartition {

	public int size();

	public IBlock getFirstBlock();
	
	public IBlock getLastBlock();

	public void add(IBlock block);

	public boolean remove(IBlock block);
	
	public IBlock getBlockOf(ISpecies species);
	
	/**
	 * This method is used to remove the block toBeRemoved, and to put aliasBlock in its position. 
	 * @param aliasBlock
	 * @param splittedBlock
	 */
	public void substituteAndDecreaseSize(IBlock aliasBlock, IBlock toBeRemoved);

	boolean remove(IBlock blockToRemove, boolean notifySplitterGenerator);

	public IPartition copy();
	
	public boolean speciesIsRepresentativeOfItsBlock(ISpecies species);
	
	//boolean speciesIsRepresentativeOfItsBlock(ISpecies species,boolean computeRepresentativeByMin);
	
	/**
	 * This method updates the array used to store the correspondence from species to its block. It is assumed that a species is stored in the position getId().
	 * @param species
	 * @param block
	 */
	public void updateSpeciesToBlockMapping(ISpecies species, IBlock block);
	
	
	public void initializeAllBST();
//	
//	/**
//	 * 
//	 * @param labels
//	 * @return a new splitter generator, or the previously created one. As a consequence, invoking this method with differen labels does not create a new splitter generator for the new labels, but the old one is returned
//	 */
//	public SplittersGenerator getOrCreateSplitterGenerator(List<ILabel> labels);
//	
	/**
	 * Creates a new splittergenerator, replacing the previous one, if any
	 * @param labels
	 * @return
	 */
	public SplittersGenerator createSplitterGenerator(List<ILabel> labels);

	public void splitBlock(Collection<IBlock> splittedBlocks,ISpecies species, IBlock block, BigDecimal val);
	
	public boolean splitBlockOnlyIfKeyIsNotZero(Collection<IBlock> splittedBlocks,ISpecies species, IBlock block, BigDecimal val);

	public void setMinAsRepresentative();

	public void prepareForMoreSpecies(int size);

}
