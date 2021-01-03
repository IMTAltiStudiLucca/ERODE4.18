package it.imt.erode.simulation.output;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.math.plot.Plot2DPanel;
import org.math.plot.plotObjects.BaseLabel;

import it.imt.erode.tests.TabbedPlots;
//import it.imt.erode.simulation.output.DataOutputHandlerAbstract;

public class DataOutputHandler extends DataOutputHandlerAbstract{

	private Plot2DPanel graphicalPlotAll;
	private Plot2DPanel graphicalPlotViews;
	private Plot2DPanel graphicalPlotSMCVariances;

	private static final int TITLE_PLOT_SIZE = 13;
	private static final int TEXT_PLOT_SIZE = 11;

	private void initPlot(){
		graphicalPlotAll = new Plot2DPanel();
		graphicalPlotAll.addPlotToolBar("NORTH");
		graphicalPlotAll.setPreferredSize(new Dimension(600, 600));
		if(showLabels){
			// define the legend position
			graphicalPlotAll.addLegend("EAST");
		}
		BaseLabel title = new BaseLabel(minimalDescription+messageSuffix, Color.BLUE, 0.5, 1.1);
		title.setFont(new Font("Courier", Font.BOLD, TITLE_PLOT_SIZE));
		graphicalPlotAll.addPlotable(title);
		graphicalPlotAll.setFont(new Font("Courier", Font.LAYOUT_LEFT_TO_RIGHT, TEXT_PLOT_SIZE));
		graphicalPlotAll.setAxisLabel(0, xLabel);
		graphicalPlotAll.setAxisLabel(1, yLabel);
		
		if(hasSMCVariances){
			graphicalPlotSMCVariances = new Plot2DPanel();
			graphicalPlotSMCVariances.addPlotToolBar("NORTH");
			graphicalPlotSMCVariances.setPreferredSize(new Dimension(600, 600));
			if(showLabels){
				// define the legend position
				graphicalPlotSMCVariances.addLegend("EAST");
			}
			title = new BaseLabel(minimalDescription+". Variances estimations.", Color.BLUE, 0.5, 1.1);
			title.setFont(new Font("Courier", Font.BOLD, TITLE_PLOT_SIZE));
			graphicalPlotSMCVariances.addPlotable(title);
			graphicalPlotSMCVariances.setFont(new Font("Courier", Font.LAYOUT_LEFT_TO_RIGHT, TEXT_PLOT_SIZE));
			graphicalPlotSMCVariances.setAxisLabel(0, xLabel);
			graphicalPlotSMCVariances.setAxisLabel(1, "Variances estimations");
		}
		
		//means
		for(int species=0;species<labelsAll.length;species++){
			// add a line plot to the PlotPanel
			graphicalPlotAll.addLinePlot(labelsAll[species], x, plotsAll[species]);
			if(hasSMCVariances){
				graphicalPlotSMCVariances.addLinePlot(labelsAll[species],x,smcVariances[species]);
			}
		}
		/*//covariances
		boolean showAllCovariances=false;
		for(int covariance=labelsAll.length;covariance<plotsAll.length;covariance++){
			int cov=(covariance-labelsAll.length);//this is the cov-th covariance. I.e., the C_{species1,species2}
			int species1 = cov / labelsAll.length;
			int species2 = cov % labelsAll.length;
			if(species1==species2){//with this if I only print the variances
				graphicalPlotAll.addLinePlot("V_"+ labelsAll[species1], x, plotsAll[covariance]);
			}
			else{
				if(showAllCovariances){
					graphicalPlotAll.addLinePlot("C_"+ labelsAll[species1]+"-"+labelsAll[species2], x, plotsAll[covariance]);
				}
			}
		}*/
		
		if(hasViews){
			graphicalPlotViews = new Plot2DPanel();
			graphicalPlotViews.setPreferredSize(new Dimension(600, 600));

			graphicalPlotViews.addPlotToolBar("NORTH");
			if(showLabels){
				// define the legend position
				graphicalPlotViews.addLegend("SOUTH");
			}
			BaseLabel titleViews = new BaseLabel(minimalDescription+"- VIEWS", Color.BLUE, 0.5, 1.1);
			titleViews.setFont(new Font("Courier", Font.BOLD, TITLE_PLOT_SIZE));
			graphicalPlotViews.addPlotable(titleViews);
			graphicalPlotViews.setFont(new Font("Courier", Font.LAYOUT_LEFT_TO_RIGHT, TEXT_PLOT_SIZE));
			graphicalPlotViews.setAxisLabel(0, xLabel);
			graphicalPlotViews.setAxisLabel(1, "Views concentrations");

			for(int view=0;view<labelsViews.length;view++){
				if((!covariances) && crn.getViewExpressionsUsesCovariances()[view]){
					continue;
				}
				else{
					graphicalPlotViews.addLinePlot(labelsViews[view], x, plotsViews[view]);
				}
			}
		}
	}

	/**
	 * Create the GUI and show it.  For thread safety,
	 * this method should be invoked from
	 * the event dispatch thread.
	 * @param imagesName 
	 * @param drawImages 
	 */
	private void createAndShowGUI(boolean drawImages, String imagesName, String name,boolean visualizeVars,boolean visualizeViews){
		//Create the plots
		initPlot();

		//show them
		//Create and set up the window.
		JFrame frame = new JFrame(name);
		//frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setResizable(true);

		//Add content to the window.
		TabbedPlots plots = new TabbedPlots();
		if(visualizeVars){
			plots.addPlot(mainTabLabel,graphicalPlotAll,0,iconAll);
		}
		if(hasViews&&visualizeViews){
			plots.addPlot("Concentrations of the views",graphicalPlotViews,1,iconViews);
		}
		if(hasSMCVariances){
			int k= (hasViews)?2:1;
			plots.addPlot("Variances estimations", graphicalPlotSMCVariances, k, iconAll);
		}

		frame.add(plots, BorderLayout.CENTER);
		frame.setExtendedState(frame.getExtendedState()|JFrame.MAXIMIZED_BOTH );

		//Display the window.
		frame.pack();
		frame.setVisible(true);

		if(drawImages && imagesName !=null && (!imagesName.equals(""))){
			try {
				graphicalPlotAll.toGraphicFile(new File(imagesName+".png"));
				if(hasViews){
					graphicalPlotViews.toGraphicFile(new File(imagesName+"views.png"));
				}
			} catch (IOException e) {
				//CRNReducerCommandLine.println(out,"Plot image creation failed.");
			}
		}	
	}

	@Override
	public void showPlots(final boolean drawImages, final String imagesName,final boolean visualizeVars,final boolean visualizeViews){
		//Schedule a job for the event dispatch thread:
		//creating and showing this application's GUI.
		//SwingUtilities.invokeAndWait(
		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						//Turn off metal's use of bold fonts
						//UIManager.put("swing.boldMetal", Boolean.FALSE);
						createAndShowGUI(drawImages,imagesName,crn.getName(),visualizeVars,visualizeViews);
					}
				});
	}


}
