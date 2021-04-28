package it.imt.erode.simulation.output;

import java.io.BufferedWriter;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.commandline.SimulationSolutions;
import it.imt.erode.crn.interfaces.ICRN;
import vesta.mc.InfoMultiQuery;

public interface IDataOutputHandler {

	void setShowLabels(boolean showLabels);

	void showPlots(boolean drawImages, String imagesName, boolean visualizeVars,boolean visualizeViews);

	void writeCSV(String csvFile/*, MessageConsoleStream out,IMessageDialogShower msgDialogShower*/);
	//static void writeCSV(String csvFile,String extension, String stepLabel, String valueLabel, List<Double> valuesAtPoint,MessageConsoleStream out,BufferedWriter bwOut);

	void setData(String minimalDescription, double[][] result, double[][] resultViews, boolean stepPerSpecies, double[] x, ICRN crn,boolean skipFirstPosOfResult, String command);

	void setData(String minimalDescription, double[][] result, boolean stepPerSpecies, double[] x, ICRN crn,boolean skipFirstPosOfResult, boolean covariances, boolean computeOnlyJacobian, String command);

	void setData(String minimalDescription, ICRN crn, InfoMultiQuery infoMultiQuery,double alpha, double delta, String command);

	MessageConsoleStream getOut();
	BufferedWriter getBWOut();

	IMessageDialogShower getMsgDialogShower();

	void setOut(MessageConsoleStream out);
	void setBWOut(BufferedWriter bwOut);

	void setMsgDialogShower(IMessageDialogShower msgDialogShower);

	SimulationSolutions getSimulationSolutions();

}
