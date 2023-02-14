package it.imt.erode.importing;

import java.util.ArrayList;

import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;

public class CNFClauses {

	private int nVars;
	private int nClauses;
	//Each entry of the outer array is a list of  variables or not variables appearing in a CNF clause. 
	//The whole out array represents the cnf clauses in AND
	private ArrayList<ArrayList<IUpdateFunction>> cnfClauses;
	
	public CNFClauses(int nVars,int nClauses) {
		this.nVars=nVars;
		this.nClauses=nClauses;
		cnfClauses=new ArrayList<>(this.nClauses);
	}
	
	public int getnVars() {
		return nVars;
	}
	public int getnClauses() {
		return nClauses;
	}
	
	public ArrayList<ArrayList<IUpdateFunction>> getCnfClauses() {
		return cnfClauses;
	}

	public void add(ArrayList<IUpdateFunction> currentClause) {
		cnfClauses.add(currentClause);
	}
}
