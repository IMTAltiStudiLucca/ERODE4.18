package it.imt.erode.auxiliarydatastructures;

import it.imt.erode.partition.interfaces.IPartition;

public class IntegerAndPartition {

	private int integer;
	private IPartition partition;
	public int getInteger() {
		return integer;
	}
	public void setInteger(int integer) {
		this.integer = integer;
	}
	public IPartition getPartition() {
		return partition;
	}
	public void setPartition(IPartition partition) {
		this.partition = partition;
	}
	public IntegerAndPartition(int integer, IPartition partition) {
		super();
		this.integer = integer;
		this.partition = partition;
	}
	
	
	
}
