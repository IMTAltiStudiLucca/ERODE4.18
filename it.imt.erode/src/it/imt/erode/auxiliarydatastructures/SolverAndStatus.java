package it.imt.erode.auxiliarydatastructures;

import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;


public class SolverAndStatus {

	private Solver solver;
	private Status status;

	public SolverAndStatus(Solver solver, Status status){
		this.solver = solver;
		this.status = status;
	}

	public Solver getSolver() {
		return solver;
	}

	public Status getStatus() {
		return status;
	}


}
