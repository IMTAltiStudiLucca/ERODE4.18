package it.imt.erode.booleannetwork.auxiliarydatastructures;

import it.imt.erode.booleannetwork.interfaces.IBooleanNetwork;
import it.imt.erode.partition.interfaces.IPartition;

public class BNandPartition {

	private IBooleanNetwork bn;
	private IPartition partition;
	public IBooleanNetwork getBN() {
		return bn;
	}
	public void setBN(IBooleanNetwork bn) {
		this.bn = bn;
	}
	public IPartition getPartition() {
		return partition;
	}
	public void setPartition(IPartition partition) {
		this.partition = partition;
	}
	public BNandPartition(IBooleanNetwork bn, IPartition partition) {
		super();
		this.bn = bn;
		this.partition = partition;
	}
	
	
	
}
