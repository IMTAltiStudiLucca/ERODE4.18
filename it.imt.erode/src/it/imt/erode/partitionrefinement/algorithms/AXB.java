package it.imt.erode.partitionrefinement.algorithms;

import java.util.LinkedHashSet;

public class AXB {

	private double[] b;
	private double[][] A;
	private LinkedHashSet<String> columns;
	private double[] solution;
	private boolean solutionComputed=false;
	
	public AXB(double[] b, double[][] A, LinkedHashSet<String> labelsOfColumns) {
		this.b =b;
		this.A=A;
		this.columns = labelsOfColumns;
	}

	public double[] getB() {
		return b;
	}

	public double[][] getA() {
		return A;
	}

	public LinkedHashSet<String> getColumns() {
		return columns;
	}
	
	public void setSolution(double[] sol) {
		this.solution=sol;
		solutionComputed=true;
	}
	public boolean isSolutionComputed() {
		return solutionComputed;
	}
	public double[] getSolution() {
		return solution;
	}

}
