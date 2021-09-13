package it.imt.erode.commandline;

import java.io.IOException;

import it.imt.erode.importing.UnsupportedFormatException;


public class EntryPointDifferentialHull extends EntryPointForMatlabAbstract{

	
	
	public EntryPointDifferentialHull() {
		super(false,false);
	}
	
	public void computeDifferentialHull(String fOut, boolean strict, double delta) throws UnsupportedFormatException, IOException {
		String command="computeDifferentialHull({fileOut=>"+fOut+",strict=>"+strict+",delta=>"+delta+"})";
		erode.handleCreateDifferentialHullCommand(command, out, bwOut);
	}

	public static void main(String[] args) {
		
		///computeDifferentialHull({fileOut=>/Users/andrea/OneDrive - Scuola Superiore Sant'Anna/runtimes/runtime-ERODE.product(4)/giuseppe/differentialHull/x123MRNHullaa.ode,strict=>true,delta=>0.01})
		
		
		EntryPointDifferentialHull entry = new EntryPointDifferentialHull();
		String fileIn="/Users/andrea/OneDrive - Scuola Superiore Sant'Anna/runtimes/runtime-ERODE.product(4)/giuseppe/differentialHull/MI.ode";
		entry.load(fileIn);
		
		try {
			String fOut="/Users/andrea/OneDrive - Scuola Superiore Sant'Anna/runtimes/runtime-ERODE.product(4)/giuseppe/differentialHull/MIhullaaaaaa.ode";
			boolean strict=true;
			double delta=0.01;
			entry.computeDifferentialHull(fOut, strict, delta);
		} catch (UnsupportedFormatException | IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

}
