package it.imt.erode.auxiliarydatastructures;

import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.partition.interfaces.IPartition;

public class CRNandPartition {

	private ICRN crn;
	private IPartition partition;
	public ICRN getCRN() {
		return crn;
	}
	public void setCrn(ICRN crn) {
		this.crn = crn;
	}
	public IPartition getPartition() {
		return partition;
	}
	public void setPartition(IPartition partition) {
		this.partition = partition;
	}
	public CRNandPartition(ICRN crn, IPartition partition) {
		super();
		this.crn = crn;
		this.partition = partition;
	}
	
	
	
}
