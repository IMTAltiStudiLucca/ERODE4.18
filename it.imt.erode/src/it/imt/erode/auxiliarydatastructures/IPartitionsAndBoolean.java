package it.imt.erode.auxiliarydatastructures;

import it.imt.erode.partition.interfaces.IPartition;

public class IPartitionsAndBoolean extends IPartitionAndBoolean{

	private IPartition secondPartition;

	public IPartition getSecondPartition() {
		return secondPartition;
	}
	public void setSecondPartition(IPartition partition) {
		this.secondPartition = partition;
	}
	public IPartitionsAndBoolean(IPartition partition, boolean bool, IPartition secondPartition) {
		super(partition,bool);
		this.secondPartition=secondPartition;
	}
	
	
	
}
