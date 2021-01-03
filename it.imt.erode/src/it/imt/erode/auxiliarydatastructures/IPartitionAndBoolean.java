package it.imt.erode.auxiliarydatastructures;

import it.imt.erode.partition.interfaces.IPartition;

public class IPartitionAndBoolean {

	private IPartition partition;
	private boolean bool;
	public IPartition getPartition() {
		return partition;
	}
	public void setPartition(IPartition partition) {
		this.partition = partition;
	}
	public boolean getBool() {
		return bool;
	}
	public void setBool(boolean bool) {
		this.bool = bool;
	}
	public IPartitionAndBoolean(IPartition partition, boolean bool) {
		super();
		this.partition = partition;
		this.bool = bool;
	}
	
	
	
}
