package it.imt.erode.auxiliarydatastructures;

import it.imt.erode.partition.interfaces.IPartition;

public class PartitionAndString {

	private IPartition partition;
	private String string;
	public PartitionAndString(IPartition partition, String string) {
		super();
		this.partition = partition;
		this.string = string;
	}
	public IPartition getPartition() {
		return partition;
	}
	public String getString() {
		return string;
	}
	
	
	
}
