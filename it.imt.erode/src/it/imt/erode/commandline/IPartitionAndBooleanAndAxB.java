package it.imt.erode.commandline;

import it.imt.erode.auxiliarydatastructures.IPartitionAndBoolean;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.algorithms.AXB;

public class IPartitionAndBooleanAndAxB extends IPartitionAndBoolean {

	private AXB axb;
	private double[] P0;
	
	public IPartitionAndBooleanAndAxB(IPartition partition, boolean bool, AXB axb,double[] P0) {
		super(partition, bool);
		this.axb=axb;
		this.P0=P0;
	}
	
	public AXB getAxb() {
		return axb;
	}
	
	public double[] getP0() {
		return P0;
	}

}
