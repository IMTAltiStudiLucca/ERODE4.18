package it.imt.erode.auxiliarydatastructures;

import it.imt.erode.partition.interfaces.IPartition;

public class PartitionAndStringAndBoolean extends PartitionAndString {

	private boolean bool;

	public PartitionAndStringAndBoolean(IPartition partition, String string, boolean bool) {
		super(partition, string);
		this.bool=bool;
	}
	
	public boolean booleanValue() {
		return bool;
	}

}
