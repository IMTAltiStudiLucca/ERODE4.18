package it.imt.erode.importing;

import it.imt.erode.commandline.CRNReducerCommandLine;

public class InfoCRNImporting {

	private int readCRNReactions;
	private int readParameters;
	private int readSpecies;
	private String fileName;
	private int readProducts;
	private int readReagents;
	private boolean loadedCRN;
	@SuppressWarnings("unused")
	private int loadedCommands;
	private boolean loadedCRNIsInfluenceNetwork=false;
	private boolean loadingCRNFailed=false;
	private long requiredMS;
	private ODEorNET format;
	
	/*private InfoCRNImporting(){
		
	}*/
	
	public InfoCRNImporting(int readCRNReactions, int readParameters, int readSpecies, String fileName, long msNecessary) {
		super();
		this.readCRNReactions = readCRNReactions;
		this.readParameters = readParameters;
		this.readSpecies = readSpecies;
		this.fileName = fileName;
		readProducts=-1;
		readReagents=-1;
		this.loadedCRN=false;
		this.loadedCommands=0;
		this.requiredMS=msNecessary;
	}
	
	public InfoCRNImporting(int readCRNReactions, int readParameters, int readSpecies, String fileName, int readProducts,long msNecessary) {
		this(readCRNReactions, readParameters, readSpecies, fileName,msNecessary);
		this.readProducts=readProducts;
	}
	
	public String getFileName() {
		return fileName;
	}

	public int getReadProducts() {
		return readProducts;
	}
	
	public int getReadReagents() {
		return readReagents;
	}
	
	public void setReadProducts(int readProducts) {
		this.readProducts=readProducts;
	}
	
	public void setReadReagents(int readReagents) {
		this.readReagents=readReagents;
	}
	
	public void setReadCRNReactions(int readCRNReactions) {
		this.readCRNReactions = readCRNReactions;
	}
	
	public void setReadParameters(int readParameters) {
		this.readParameters = readParameters;
	}

	public void setReadSpecies(int readSpecies) {
		this.readSpecies = readSpecies;
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public void setLoadedCRN(boolean b) {
		this.loadedCRN=b;
		
	}

	public void increaseCommandsCounter() {
		this.loadedCommands++;
		
	}

	public void setLoadedCRNIsInlfuenceNetwork(boolean isInfluenceNetwork) {
		this.loadedCRNIsInfluenceNetwork=isInfluenceNetwork;
		
	}

	public void setLoadingCRNFailed() {
		this.loadingCRNFailed=true;
	}
	


	public int getReadCRNReactions() {
		return readCRNReactions;
	}

	public int getReadParameters() {
		return readParameters;
	}

	public int getReadSpecies() {
		return readSpecies;
	}

	public String getIdOrFileName() {
		return fileName;
	}

	@Override
	public String toString(){
		//String out = "";
		StringBuilder sb = new StringBuilder();
		if(loadedCRN){
//			if(loadedCRNIsInfluenceNetwork){
//				out+="Influence network. ";
//			}
			String speciesOrVars="species";
			if(format!=null && format.equals(ODEorNET.ODE)) {
				speciesOrVars="variables";
			}
			sb.append("\tRead parameters: "+getReadParameters()+", read "+speciesOrVars+": " + getReadSpecies());
			if(format==null) {
				sb.append(", read reactions: "+getReadCRNReactions());
			}
			else if(format!=null && !format.equals(ODEorNET.ODE)) {
				sb.append(", read reactions: "+getReadCRNReactions());
			}
			//out+="\tRead parameters: "+getReadParameters()+", read species: " + getReadSpecies()+", read reactions: "+getReadCRNReactions();
			if(getReadReagents()>0){
				//out+=", read reagents: "+getReadReagents()+"";\
				sb.append(", read reagents: "+getReadReagents());
			}
			if(getReadProducts()>0){
				sb.append(", read products: "+getReadProducts());
				//out+=", read products: "+getReadProducts();
			}
			if(requiredMS>0) {
				sb.append(" in "+ String.format( CRNReducerCommandLine.MSFORMAT, (requiredMS/1000.0) )+ " (s). ");
				//out+=" in "+ String.format( CRNReducerCommandLine.MSFORMAT, (requiredMS/1000.0) )+ " (s). ";
			}
			
		}
		else{
			sb.append("No model defined in the input file. ");
		}
		//out+="Loaded commands: "+loadedCommands;
		
		return sb.toString();
	}
	
	public String toStringShort(){
		StringBuilder sb = new StringBuilder();
		//String out = "";
		if(loadedCRN){
			String speciesOrVars="Species";
			if(format.equals(ODEorNET.ODE)) {
				speciesOrVars="Variables";
			}
			if(loadedCRNIsInfluenceNetwork){
				//out+="Influence network. ";
				sb.append("Influence network. ");
			}
			sb.append("\tParameters: "+getReadParameters()+"\n\t"+speciesOrVars+": " + getReadSpecies());
			if(!format.equals(ODEorNET.ODE)) {
				sb.append("\n\tReactions: "+getReadCRNReactions());
			}
			//out+="\tParameters: "+getReadParameters()+"\n\tSpecies: " + getReadSpecies()+"\n\tReactions: "+getReadCRNReactions();
			/*if(getReadReagents()>0){
				out+=", read reagents: "+getReadReagents()+"";
			}
			if(getReadProducts()>0){
				out+=", read products: "+getReadProducts();
			}*/
			sb.append(" in "+ String.format( CRNReducerCommandLine.MSFORMAT, (requiredMS/1000.0) )+ " (s). ");
			//out+=" in "+ String.format( CRNReducerCommandLine.MSFORMAT, (requiredMS/1000.0) )+ " (s). ";
		}
		else{
			sb.append("No model defined in the input file. ");
		}
		//out+="Loaded commands: "+loadedCommands;
		
		return sb.toString();
	}
	
	public boolean getLoadingCRNFailed(){
		return loadingCRNFailed;
	}

	public void setRequiredMS(long ms) {
		this.requiredMS=ms;
		
	}

	public void setLoadedCRNFormat(ODEorNET format) {
		this.format = format;
		
	}
	
	
}
