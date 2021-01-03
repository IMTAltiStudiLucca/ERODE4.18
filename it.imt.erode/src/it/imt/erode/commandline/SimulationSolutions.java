package it.imt.erode.commandline;

public class SimulationSolutions {

	private double[] x;
	private double[][] plotsAll;
	private double[][] plotsViews;
	private String[] labelsAll;
	private String[] labelsViews;
	
	
	
	public SimulationSolutions(double[] x, double[][] plotsAll, double[][] plotsViews, String[] labelsAll,
			String[] labelsViews) {
		super();
		this.x = x;
		this.plotsAll = plotsAll;
		this.plotsViews = plotsViews;
		this.labelsAll = labelsAll;
		this.labelsViews = labelsViews;
	}

	public double[] getX(){
		return x;
	}

	public double[][] getPlotsAll(){
		return plotsAll;
	}

	public double[][] getPlotsViews(){
		return plotsViews;
	}
	
	public String[] getLabelsAll(){
		return labelsAll;
	}

	public String[] getLabelsViews(){
		return labelsViews;
	}
}
