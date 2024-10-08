package it.imt.erode.importing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.auxiliarydatastructures.CRNandPartition;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.DialogType;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.crn.implementations.Species;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.utopic.MatlabODEPontryaginExporter;

public class C2E2Exporter {

	private static final int digits=3;

	public static double setNumberOfDigits(double toBeTruncated) {
		return setNumberOfDigits(toBeTruncated, digits);
	}
	
	public static double setNumberOfDigits(double toBeTruncated, int numberOfDigits) {
		Double truncatedDouble = BigDecimal.valueOf(toBeTruncated)
				.setScale(numberOfDigits, RoundingMode.HALF_UP)
				.doubleValue();
		return truncatedDouble.doubleValue();
	}
	
	public static void printCRNToC2E2File(ICRN crn,String name, Collection<String> preambleCommentLines, boolean verbose,MessageConsoleStream out,BufferedWriter bwOut,
			double timeHorizon,double kvalue,/*double delta,double taylororder,*/double timestep, 
			//LinkedHashSet<String> paramsToPerturb,
			HashMap<String, Integer> parameterToPerturbToItsPosition, String[] paramsToPerturb, double[] lows, double[] highs,
			String[] unsafeSetSplit, 
			IMessageDialogShower msgDialogShower) throws UnsupportedFormatException  {
		//expoand crn making parameters species and transforming all other parameters in numbers
		
		//if((!crn.isMassAction()) || crn.isSymbolic()){
		if(crn.isSymbolic()){
			CRNReducerCommandLine.printWarning(out,bwOut, true, msgDialogShower, "The C2E2 exporter can be created only for mass-action CRNs without symbolic parameters.", DialogType.Error);
			return;
		}
		
		
		if(!crn.isMassAction()){
			//CRNandSpecies crnAndSpecies = MatlabODEPontryaginExporter.computeRNEncoding(crn, out, bwOut, speciesNameToSpecies);
			CRNandPartition crnAndSpecies = MatlabODEPontryaginExporter.computeRNEncoding(crn, crn.getReactions(), out, bwOut, false,null,true);
			if(crnAndSpecies==null){
				return;
			}
			crn=crnAndSpecies.getCRN();
			if(crn==null){
				return;
			}
		}
		
		LinkedHashSet<String> pToPert = new LinkedHashSet<String>(paramsToPerturb.length);
		for(int p=0;p<paramsToPerturb.length;p++){
			pToPert.add(paramsToPerturb[p]);
		}
		
		HashMap<String,ISpecies> speciesNameToExpandedSpecies = new HashMap<>(crn.getSpecies().size());
		ICRN crnExpanded = MatlabODEsImporter.expandCRN(false,crn, pToPert, speciesNameToExpandedSpecies,false);
		
		String fileName = name;
		String fileName2=AbstractImporter.overwriteExtensionIfEnabled(fileName,"",true);
		String modelId=fileName2;
		if(modelId.contains(File.separator)){
			int lastSep=modelId.lastIndexOf(File.separator)+1;
			modelId=modelId.substring(lastSep);
		}
		modelId=modelId.replace("."+File.separator, "").replace(File.separator, "").replace("-", "_").replace("~", "_");//"_" is ok
		fileName=AbstractImporter.overwriteExtensionIfEnabled(fileName,".xml");
		if(verbose){
			CRNReducerCommandLine.print(out,bwOut,"Writing model in file "+fileName);
		}

		AbstractImporter.createParentDirectories(fileName);
		BufferedWriter br;
		try {
			br = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printCRNToHYXMLFile, exception raised while creating the filewriter for file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}

		try {
			
			br.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			//br.write("<?xml version='1.0' encoding='utf-8'?>\n");
			br.write("<!-- Created by "+CRNReducerCommandLine.TOOLNAME+" "+CRNReducerCommandLine.TOOLVERSION+" -->\n");
			if(preambleCommentLines!=null && preambleCommentLines.size()>0){
				for (String comment : preambleCommentLines) {
					br.write("<!--"+comment+"-->\n");
				}
				br.write("\n");
			}
			
			boolean ignoreI=true;
			
			br.write("<!DOCTYPE hyxml>\n");
			br.write("<hyxml type=\"Model\">\n");
			String tab="  ";
			br.write(tab+"<automaton name=\"default_automaton\">\n");
			for (ISpecies species : crnExpanded.getSpecies()) {
				//<variable scope="LOCAL_DATA" type="Real" name="x"/>
				if(ignoreI&&species.getName().equals(Species.I_SPECIESNAME)){
					continue;
				}
				else{
					br.write(tab+tab+"<variable scope=\"LOCAL_DATA\" type=\"Real\" name=\""+species.getName()+"\"/>\n");
				}
				
			}
			//<mode initial="True" id="0" name="Brussellator">
			br.write(tab+tab+"<mode initial=\"True\" id=\"0\" name=\""+modelId+"\">\n");
			
			
			HashMap<ISpecies, StringBuilder> speciesToDrift = GUICRNImporter.computeDrifts(crnExpanded, false,ignoreI,false);
			for (ISpecies species : crnExpanded.getSpecies()) {
				if(ignoreI&& species.getName().equals(Species.I_SPECIESNAME)){
					continue;
				}
				else{
					//<dai equation="x_dot = 1+x*x*y-2.5*x"/>
					StringBuilder driftSB = speciesToDrift.get(species);
					String drift="0";
					if(driftSB!=null&& driftSB.length()!=0){
						drift=driftSB.toString();
					}
					br.write(tab+tab+tab+"<dai equation=\""+species.getName()+"_dot = "+drift+"\"/>\n");
				}
			}
			for (ISpecies species : crnExpanded.getSpecies()) {
				if(ignoreI&& species.getName().equals(Species.I_SPECIESNAME)){
					continue;
				}
				else{
					//<dai equation="x_out = x"/>
					br.write(tab+tab+tab+"<dai equation=\""+species.getName()+"_out = "+species.getName()+"\"/>\n");
				}
			}
			br.write(tab+tab+"</mode>\n");
			br.write(tab+"</automaton>\n");
			br.write(tab+"<composition automata=\"default_automaton\"/>\n");
			
			/*
			 <property unsafeSet="x&gt;=10" type="0" name="Property1" initialSet="Brussellator: x&gt;=2.0&amp;&amp;x&lt;=3.0&amp;&amp;y&gt;=1.0&amp;&amp;y&lt;=1.5">
		       <parameters taylororder="10.0" timestep="0.01" timehorizon="10.0" delta="0.1"/>
		     </property>
		    */
			/*
			 <property initialSet="SlowDown: sx&gt;=-15.0&amp;&amp;sx&lt;=-14.95&amp;&amp;vx&gt;=3.25&amp;&amp;vx&lt;=3.3&amp;&amp;ax==0&amp;&amp;vy==0&amp;&amp;omega==0&amp;&amp;sy==0" name="SxUB1" type="0" unsafeSet="sx&gt;=50">
    			<parameters delta="0.01" taylororder="10.0" timehorizon="40.0" timestep="0.1"/>
  			 </property>
			 */
			
			br.write(tab+"<property initialSet=\""+modelId+": ");
			//ax==0&amp;&amp;vy==0
			StringBuilder sb = new StringBuilder();
			String andAnd="&amp;&amp;";
			boolean hasSpecies=false;
			for (ISpecies species : crn.getSpecies()) {
				if(ignoreI&& species.getName().equals(Species.I_SPECIESNAME)){
					continue;
				}
				else{
					sb.append(species.getName()+"=="+setNumberOfDigits(species.getInitialConcentration().doubleValue()));
					sb.append(andAnd);
					hasSpecies=true;
				}
				/*if(s<crn.getSpecies().size()-1){
					br.write("&amp;&amp;");
				}
				else{
					//only if last is not I
					if(paramsToPerturb!=null&&paramsToPerturb.size()>0){
						br.write("&amp;&amp;");
					}
				}
				s++;*/				
			}
			if(sb.length()>0){
				sb.delete(sb.length()-andAnd.length(), sb.length());
				br.write(sb.toString());
			}
			
			if(paramsToPerturb!=null&&paramsToPerturb.length>0){
				sb = new StringBuilder();
				if(hasSpecies){
					sb.append(andAnd);
				}
				for (int p=0;p<paramsToPerturb.length;p++){
					String param=paramsToPerturb[p];
					double paramLow=setNumberOfDigits(lows[p]);
					double paramHigh=setNumberOfDigits(highs[p]);
					sb.append(param+"&gt;="+paramLow+andAnd);
					sb.append(param+"&lt;="+paramHigh);
					sb.append(andAnd);
				}
				sb.delete(sb.length()-andAnd.length(), sb.length());
				br.write(sb.toString());
			}
			br.write("\"");
			//br.write(" name=\"Property1\" type=\"0\" unsafeSet=\""+crnExpanded.getSpecies().get(0)+"&gt;=50\"");
			br.write(" name=\"Property1\" type=\"0\"");
			if(unsafeSetSplit!=null&&unsafeSetSplit.length>0){
				sb = new StringBuilder();
				//  <property unsafeSet="x&gt;=10" type="0" name="Property1" initialSet="Brussellator: x&gt;=2.0&amp;&amp;x&lt;=3.0&amp;&amp;y&gt;=1.0&amp;&amp;y&lt;=1.5">
				br.write(" unsafeSet=\"");
				for(int i=0;i<unsafeSetSplit.length;i+=3){
					String species=unsafeSetSplit[i];
					String comp=unsafeSetSplit[i+1];
					String compXML=compToXML(comp);
					String expr=unsafeSetSplit[i+2];
					double exprEval= setNumberOfDigits(crn.getMath().evaluate(expr));
					sb.append(" "+species+compXML+exprEval);
					sb.append(andAnd);
				}
				sb.delete(sb.length()-andAnd.length(), sb.length());
				sb.append(" \"");
				br.write(sb.toString());
			}
			br.write(">\n");
			
			
			
			//br.write(tab+tab+"<parameters delta=\""+delta+"\" taylororder=\""+taylororder+"\" timehorizon=\""+timeHorizon+"\" timestep=\""+timestep+"\"/>\n");
		    //<parameters kvalue="2000.0" timehorizon="10.0" timestep="0.01" />
			br.write(tab+tab+"<parameters kvalue=\""+kvalue+"\" timehorizon=\""+timeHorizon+"\" timestep=\""+timestep+"\"/>\n");
			
			br.write(tab+"</property>\n");
			
			br.write("</hyxml>\n");

		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printCRNToHYXMLFile, exception raised while writing in the file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		finally{
			if(verbose){
				CRNReducerCommandLine.println(out,bwOut,"Writing model in file "+fileName+" completed");
			}
			try {
				br.close();
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in printCRNToHYXMLFile, exception raised while closing the bufferedwriter of the file: "+fileName);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}

	}

	private static String compToXML(String comp) throws UnsupportedFormatException {
		switch (comp) {
		case ">":
			return "&gt;";
		case ">=":
			return "&gt;=";
		case "<":
			return "&lt;";
		case "<=":
			return "&lt;=";
		case "=":
			return "==";	
		default:
			throw new UnsupportedFormatException("The comparator "+comp+" is not supported");
		}
	}
	
}

/*
<?xml version='1.0' encoding='utf-8'?>
<!DOCTYPE hyxml>
<hyxml>
  <automaton name="default_automaton">
    <variable scope="LOCAL_DATA" type="Real" name="x"/>
    <variable scope="LOCAL_DATA" type="Real" name="y"/>
    <mode initial="True" id="0" name="Brussellator">
      <dai equation="x_dot = 1+x*x*y-2.5*x"/>
      <dai equation="y_dot = 1.5*x-x*x*y"/>
      <dai equation="x_out = x"/>
      <dai equation="y_out = y"/>
      <annotation mode="Brussellator">
        <K value="2000"/>
        <gamma value="0"/>
        <type string="exponential" value="1"/>
      </annotation>
    </mode>
  </automaton>
  <composition automata="default_automaton"/>
  <property unsafeSet="x&gt;=10" type="0" name="Property1" initialSet="Brussellator: x&gt;=2.0&amp;&amp;x&lt;=3.0&amp;&amp;y&gt;=1.0&amp;&amp;y&lt;=1.5">
    <parameters taylororder="10.0" timestep="0.01" timehorizon="10.0" delta="0.1"/>
  </property>
</hyxml>
*/
