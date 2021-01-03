package it.imt.erode.importing;

import it.imt.erode.commandline.CRNReducerCommandLine;

public class InfoBooleanNetworkImporting {

	private int readNodes;
	private String fileName;
	private boolean loadedBN;
	@SuppressWarnings("unused")
	private int loadedCommands;
	private boolean loadingBNFailed=false;
	private long requiredMS;
	
	/*private InfoCRNImporting(){
		
	}*/
	
	public InfoBooleanNetworkImporting(int readNodes, String fileName, long msNecessary) {
		super();
		this.readNodes = readNodes;
		this.fileName = fileName;
		this.loadedBN=false;
		this.loadedCommands=0;
		this.requiredMS=msNecessary;
	}
	
	public String getFileName() {
		return fileName;
	}

	public void setReadNodes(int readNodes) {
		this.readNodes = readNodes;
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public void setLoadedBooleanNetwork(boolean b) {
		this.loadedBN=b;
		
	}

	public void increaseCommandsCounter() {
		this.loadedCommands++;
		
	}

	public void setLoadingCRNFailed() {
		this.loadingBNFailed=true;
	}
	


	public int getReadNodes() {
		return readNodes;
	}

	public String getIdOrFileName() {
		return fileName;
	}

	@Override
	public String toString(){
		String out = "";
		if(loadedBN){
			out+="\tRead nodes: " + getReadNodes();
			out+=" in "+ String.format( CRNReducerCommandLine.MSFORMAT, (requiredMS/1000.0) )+ " (s). ";
		}
		else{
			out+="No model defined in the input file. ";
		}
		//out+="Loaded commands: "+loadedCommands;
		
		return out;
	}
	
	public String toStringShort(){
		String out = "";
		if(loadedBN){
			out+="\tSpecies: " + getReadNodes();
			out+=" in "+ String.format( CRNReducerCommandLine.MSFORMAT, (requiredMS/1000.0) )+ " (s). ";
		}
		else{
			out+="No model defined in the input file. ";
		}
		//out+="Loaded commands: "+loadedCommands;
		
		return out;
	}
	
	public boolean getLoadingBNFailed(){
		return loadingBNFailed;
	}

	public void setRequiredMS(long ms) {
		this.requiredMS=ms;
		
	}
	
	
}
