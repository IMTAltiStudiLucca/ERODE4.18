package it.imt.erode.auxiliarydatastructures;
import com.microsoft.z3.Status;


public class DoubleAndStatus {

	private Status status;
	private double d;

	public DoubleAndStatus(Status solver,  double d){
		this.status = solver;
		this.d = d;
	}

	public Status getStatus() {
		return status;
	}

	public double getDouble() {
		return d;
	}


}
