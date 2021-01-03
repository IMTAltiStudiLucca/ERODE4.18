package it.imt.erode.commandline;

public class Terminator {

	private boolean terminate;
	
	public Terminator() {
		terminate=false;
	}
	
	public synchronized void setTerminationFlag(){
		terminate=true;
	}
	public synchronized boolean hasToTerminate(){
		return terminate;
	}
	
	public static boolean hasToTerminate(Terminator terminator){
		if(terminator==null){
			return false;
		}
		else{
			return terminator.hasToTerminate();
		}
	}

}
