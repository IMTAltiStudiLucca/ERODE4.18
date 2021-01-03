package it.imt.erode.onthefly.algorithm;

public interface IOnTheFly {

	public int getNumberOfUpdates();
	public int getNumberOfExpand();
	public int getTranspProblemsSolved();
	public int getTranspProblemsUnbalancedSkept();
	public int getNumberOfPairsAddedToAdj();
	public int getNumberOfPairsUpdatedInAdj();
	
}
