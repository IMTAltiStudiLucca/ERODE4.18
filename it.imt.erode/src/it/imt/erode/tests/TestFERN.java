package it.imt.erode.tests;

import java.io.File;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.jdom.JDOMException;

import fern.network.FeatureNotSupportedException;
import fern.network.Network;
//import plot.WindowEventHandler;
import fern.network.fernml.FernMLNetwork;
import fern.simulation.Simulator;
import fern.simulation.algorithm.GibsonBruckSimulator;
import fern.simulation.algorithm.GillespieSimple;
import fern.simulation.observer.AmountIntervalObserver;
import fern.simulation.observer.IntervalObserver;
import fern.tools.gnuplot.GnuPlot;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.expression.evaluator.MathEval;
import it.imt.erode.importing.AbstractImporter;
import it.imt.erode.importing.CRNImporter;
import it.imt.erode.importing.ImporterOfSupportedNetworks;
import it.imt.erode.importing.SupportedFormats;
import it.imt.erode.importing.UnsupportedFormatException;
import it.imt.erode.simulation.stochastic.fern.FernNetworkFromLoadedCRN;

public class TestFERN {

	public static void main(String[] args){
		//this is the example taken from FERN website

		double maxTime = 5;
		double interval = 5.0/49.0;
		
		/*try {
			//simulateSBML("BioNetGen_CCP.xml",maxTime,interval);
			//simulateFERNML("mm.xml",maxTime,interval);
			//simulateFERNML("mm2.xml",maxTime,interval);
		} catch (FeatureNotSupportedException e) {
			// TODO Auto-generated catch block
			CRNReducerCommandLine.printStackTrace(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			CRNReducerCommandLine.printStackTrace(e);
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			CRNReducerCommandLine.printStackTrace(e);
		}*/
		
		
		
		try {
			//String fileName=BioNetGenImporter.BNGNetworksFolder+"BioNetGen_CCP.net";
			//String fileName=BioNetGenImporter.BNGNetworksFolder+"dsbReducedBioNetGen_CCP.net";
			//String fileName=BioNetGenImporter.BNGNetworksFolder+"dsbReducedmmc1.net";
			String fileName=  CRNImporter.CRNNetworksFolder+"amSameRates.crn";
			
			simulateLoadedCRN(fileName, maxTime,interval);
		} catch (UnsupportedFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//exampleFromFERNWebsite();
		

	}
	
	
	private static void simulateLoadedCRN(String fileName, double maxTime, double interval) throws UnsupportedFormatException, IOException, JDOMException, XMLStreamException{
		SupportedFormats format = SupportedFormats.BNG;
		if(fileName.endsWith(".crn")){
			format=SupportedFormats.CRN;
		}
		
		AbstractImporter importer = new ImporterOfSupportedNetworks().importSupportedNetwork(fileName, true, false,format,true,null,null,null,false,false);
		ICRN crn = importer.getCRN();		
		Network net = new FernNetworkFromLoadedCRN(crn,0);
		String plottedSpecies[] = ((FernNetworkFromLoadedCRN)net).getIndexToSpeciesId();
		String imagesName = fileName.substring(fileName.lastIndexOf(File.separator)+1);
		int extensionPos = imagesName.lastIndexOf('.');
		if(extensionPos!=-1){
			imagesName = imagesName.substring(0, extensionPos);
		}
		
		simulate(net,plottedSpecies,imagesName,maxTime,interval,crn,importer.getMath());
	}

	@SuppressWarnings("unused")
	private static void simulateFERNML(String fileName, double maxTime,double interval) throws FeatureNotSupportedException, IOException, JDOMException{
		Network net = new FernMLNetwork(new File("FERNMLNetworks"+File.separator+fileName));
		String allSpecies[] = new String[net.getNumSpecies()];
		for(int i=0;i<net.getNumSpecies();i++){
			allSpecies[i]=net.getSpeciesName(i);
		}
		
		/*String plottedSpecies[] = new String[6];
		plottedSpecies[0]="E0*";
		plottedSpecies[1]="E1*";
		plottedSpecies[2]="E2*";
		plottedSpecies[3]="E3*";
		plottedSpecies[4]="E4*";
		plottedSpecies[5]="E5*";*/
		
		//IntervalObserver obs = (IntervalObserver) sim.addObserver(new AmountIntervalObserver(sim,10,"E0*","E1*","E2*","E3*","E4*","E5*"));
		
		simulate(net,allSpecies,fileName.replace(".xml",""),maxTime,interval);
	}
	
	private static void simulate(Network net, String[] plottedSpecies, String imagesName, double maxTime, double interval, ICRN crn, MathEval mathEval) throws IOException {
		//Simulator sim = new GibsonBruckSimulator(net);
		Simulator sim = new GillespieSimple(net);
		IntervalObserver amountObs = (IntervalObserver) sim.addObserver(new AmountIntervalObserver(sim,interval,plottedSpecies));
		//IntervalObserver viewsObs = (IntervalObserver) sim.addObserver(new ViewsIntervalObserver(sim, interval, crn.getViewNames(), crn.getViewExpressionsSupportedByMathEval(), mathEval));
		
		
		GnuPlot amountgp = new GnuPlot();
		amountgp.setDefaultStyle("with lines");
		//GnuPlot viewsgp = new GnuPlot();
		//viewsgp.setDefaultStyle("with lines");

		for (int i=1; i<=10000; i++) {
			sim.start(maxTime);
			//if (i<=10 || i%10==0) {
			if (i%1000==0) {
				amountgp.getCommands().clear();
				amountgp.addCommand("set title \""+imagesName+" time course after "+i+" repeats");

				amountObs.toGnuplot(amountgp);
				amountgp.plot();
				//System.out.println(obs);
				
				/*gp.setVisible(true);
				// put the PlotPanel in a JFrame, as a JPanel
				JFrame frame = new JFrame("a plot panel");
				//frame.addWindowListener(new WindowEventHandler());
				frame.setContentPane(gp);
				frame.setSize(600, 600);
				frame.setExtendedState( frame.getExtendedState()|JFrame.MAXIMIZED_BOTH );
				frame.setVisible(true);*/

				amountgp.saveImage(new File(String.format("images"+File.separator+imagesName+"%03d.png",i)));
				amountgp.saveData(new File(String.format("images"+File.separator+imagesName+"%03d.txt",i)));
				amountgp.clearData();
				
				
				/*viewsgp.getCommands().clear();
				viewsgp.addCommand("set title \""+imagesName+" time course after "+i+" repeats");
				viewsObs.toGnuplot(viewsgp);
				viewsgp.plot();
				viewsgp.saveImage(new File(String.format("images"+File.separator+"views"+imagesName+"%03d.png",i)));
				viewsgp.clearData();*/
			}
		}
		
	}
	
	private static void simulate(Network net, String[] plottedSpecies, String fileName, double maxTime, double interval) throws IOException {
		Simulator sim = new GibsonBruckSimulator(net);
		IntervalObserver obs = (IntervalObserver) sim.addObserver(new AmountIntervalObserver(sim,interval,plottedSpecies));
		
		GnuPlot gp = new GnuPlot();
		gp.setDefaultStyle("with lines");

		for (int i=1; i<=100; i++) {
			sim.start(maxTime);
			if (i<=10 || i%10==0) {
				gp.getCommands().clear();
				gp.addCommand("set title \""+fileName+" time course after "+i+" repeats");

				obs.toGnuplot(gp);
				gp.plot();
				//System.out.println(obs);
				
				/*gp.setVisible(true);
				// put the PlotPanel in a JFrame, as a JPanel
				JFrame frame = new JFrame("a plot panel");
				//frame.addWindowListener(new WindowEventHandler());
				frame.setContentPane(gp);
				frame.setSize(600, 600);
				frame.setExtendedState( frame.getExtendedState()|JFrame.MAXIMIZED_BOTH );
				frame.setVisible(true);*/

				gp.saveImage(new File(String.format("images/"+fileName+"%03d.png",i)));
				gp.clearData();
				
			}
		}
		
	}

	//@SuppressWarnings("unused")
	/**
	 * This does not work because I have first to install libsbml
	 * @param fileName
	 * @throws FeatureNotSupportedException
	 * @throws IOException
	 */
	/*private static void simulateSBML(String fileName) throws FeatureNotSupportedException, IOException{
		Network net = new NewSBMLNetwork(new File("SBMLNetworks"+File.separator+fileName)); 
		String allSpecies[] = new String[net.getNumSpecies()];
		for(int i=0;i<net.getNumSpecies();i++){
			allSpecies[i]=net.getSpeciesName(i);
		}
		simulate(net,allSpecies,fileName);
	}*/
	
	@SuppressWarnings("unused")
	private static void exampleFromFERNWebsite(){
		Network net;
		try {
			//String fileName = "fernMLExampleFromGuide.xml"; //mapk.xml
			String fileName = "mapk.xml";
			net = new FernMLNetwork(new File("examples"+File.separator+fileName));

			Simulator sim = new GibsonBruckSimulator(net);
			IntervalObserver obs = (IntervalObserver) sim.addObserver(new AmountIntervalObserver(sim,10,"E0*","E1*","E2*","E3*","E4*","E5*"));
			/*String[] species = new String[4];
			species[0]="S";
			species[1]="E";
			species[2]="ES";
			species[3]="P";
			IntervalObserver obs = (IntervalObserver) sim.addObserver(new AmountIntervalObserver(sim,10,species));*/

			GnuPlot gp = new GnuPlot();
			gp.setDefaultStyle("with lines");

			for (int i=1; i<=100; i++) {
				sim.start(1000);
				if (i<=10 || i%10==0) {
					gp.getCommands().clear();
					gp.addCommand("set title \""+fileName+" time course after "+i+" repeats");

					obs.toGnuplot(gp);
					gp.plot();

					gp.saveImage(new File(String.format("images/"+fileName+"%03d.png",i)));
					gp.clearData();
				}
			}
		} catch (IOException | JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

	

