package it.imt.erode.crn.ui.perspective.plot;

import java.io.BufferedWriter;

import org.eclipse.nebula.visualization.xygraph.figures.IXYGraph;
import org.eclipse.nebula.visualization.xygraph.util.XYGraphMediaFactory;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.simulation.output.DataOutputHandlerAbstract;
import it.imt.erode.simulation.output.MutableBoolean;

public class GUIDataOutputHandler extends DataOutputHandlerAbstract {

	//private PlotView plotView;
	private MessageConsole console;
	private int counter=0;
	
	public GUIDataOutputHandler(MessageConsole console, IMessageDialogShower msgVisualizer,MessageConsoleStream out,BufferedWriter bwOut) {
		this.console=console;
		setMsgDialogShower(msgVisualizer);
		setOut(out);
		setBWOut(bwOut);
	}

	@Override
	public void showPlots(boolean drawImages, String imagesName, boolean visualizeVars,boolean visualizeViews) {
		//devo solo fargli disegnare i plot che ho memorizzato nei field di DataOutputHandlerAbstract
		/*double[] xArray = new double[] { 10, 23, 34, 45, 56, 78, 88, 99 };
		double[] yArray = new double[] { 11, 44, 55, 45, 88, 98, 52, 23 };
		
		double[] xArray2 = new double[] { 10, 23, 34, 45, 56, 78, 88, 99 };
		double[] yArray2 = new double[] { 11+10, 44+10, 55+10, 45+10, 88+10, 98+10, 52+10, 23+10 };
		
		double[] xArray3 = new double[] { 10, 23, 34, 45, 56, 78, 88, 99 };
		double[] yArray3 = new double[] { 11+20, 44+20, 55+20, 45+20, 88+20, 98+20, 52+20, 23+20 };*/
		
		
		counter++;
		
		
				
		//Display.getDefault().asyncexec(
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
							
				/*PlotView plotView=null;
				try {
					plotView = (PlotView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("it.imt.erode.crn.ui.perspective.PlotView",console.getName(),org.eclipse.ui.IWorkbenchPage.VIEW_ACTIVATE);
				} catch (PartInitException e2) {
					e2.printStackTrace();
				}
								
				plotView.setPartName(console.getName());
				plotView.addTrace("My first trace", xArray, yArray);
				plotView.addTrace("My second trace", xArray2, yArray2);
				plotView.addTrace("My third trace", xArray3, yArray3);
				
				XYGraph xyg = plotView.getXYGraph();
				xyg.setShowLegend(showLabels);
				xyg.setTitle(minimalDescription+messageSuffix);
				xyg.getXAxisList().get(0).setTitle(xLabel);
				xyg.getYAxisList().get(0).setTitle(yLabel);*/
				
				
				//create plot for means and SMC variances
				PlotView plotViewMeans=null;
				PlotView plotViewSMCVariances=null;
				try {
					if(visualizeVars){
						plotViewMeans = (PlotView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("it.imt.erode.crn.ui.perspective.PlotView",console.getName()+counter,org.eclipse.ui.IWorkbenchPage.VIEW_ACTIVATE);
						//plotViewMeans.setPartProperty(key, value);
						plotViewMeans.setPartName(console.getName());//setPartName(plotViewMeans.setPartName(console.getName()+counter));
						IXYGraph xyg = plotViewMeans.getXYGraph();
						//xyg.setTitle(command+"\n"+minimalDescription+messageSuffix);
						setTitleAndLabelsAndOtherProperties(xyg, minimalDescription+messageSuffix,yLabel);
					}
					if(hasSMCVariances){
						plotViewSMCVariances = (PlotView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("it.imt.erode.crn.ui.perspective.PlotView",console.getName()+"SMCVar"+counter,org.eclipse.ui.IWorkbenchPage.VIEW_ACTIVATE);
						plotViewSMCVariances.setPartName(console.getName());//plotViewSMCVariances.setPartName(console.getName()+"SMCVar"+counter);
						IXYGraph xyg = plotViewSMCVariances.getXYGraph();
						//xyg.setTitle(command+"\n"+minimalDescription+". Variances estimations.");
						setTitleAndLabelsAndOtherProperties(xyg, minimalDescription+" - Variances estimations.","Variances estimations");
					}
				} catch (PartInitException e2) {
					e2.printStackTrace();
				}
				
				
				for(int species=0;species<labelsAll.length;species++){
					if(visualizeVars){
						// add a line 
						plotViewMeans.addTrace(labelsAll[species],x,plotsAll[species]);
					}
					if(hasSMCVariances){
						//Trace trace = new Trace(name, xAxis, yAxis, dataProvider)
						plotViewSMCVariances.addTrace(labelsAll[species],x,smcVariances[species]);
					}
				}
				
				if(visualizeVars){
					plotViewMeans.getXYGraph().performAutoScale();
				}
				
				if(hasSMCVariances){
					plotViewSMCVariances.getXYGraph().performAutoScale();
				}
				
				
				if(hasViews && visualizeViews){
					PlotView plotViewViews=null;
					
					try {
						plotViewViews = (PlotView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("it.imt.erode.crn.ui.perspective.PlotView",console.getName()+"views"+counter,org.eclipse.ui.IWorkbenchPage.VIEW_VISIBLE);//org.eclipse.ui.IWorkbenchPage.VIEW_ACTIVATE
						plotViewViews.setPartName(console.getName());//plotViewViews.setPartName(console.getName()+"views"+counter);
						IXYGraph xyg = plotViewViews.getXYGraph();
						//xyg.setTitle(command+"\n"+minimalDescription+"- VIEWS");
						setTitleAndLabelsAndOtherProperties(xyg,minimalDescription+" - VIEWS","Views");
					} catch (PartInitException e2) {
						e2.printStackTrace();
					}

					for(int view=0;view<labelsViews.length;view++){
						if((!covariances) && crn.getViewExpressionsUsesCovariances()[view]){
							continue;
						}
						else{
							plotViewViews.addTrace(labelsViews[view],x,plotsViews[view]);
						}
					}
					plotViewViews.getXYGraph().performAutoScale();
				}
				
				if(getComputeJacobian()){
					PlotView plotViewJacobian=null;

					try {
						plotViewJacobian = (PlotView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("it.imt.erode.crn.ui.perspective.PlotView",console.getName()+"Jacobian"+counter,org.eclipse.ui.IWorkbenchPage.VIEW_VISIBLE);//org.eclipse.ui.IWorkbenchPage.VIEW_ACTIVATE
						plotViewJacobian.setPartName(console.getName());//plotViewViews.setPartName(console.getName()+"views"+counter);
						IXYGraph xyg = plotViewJacobian.getXYGraph();
						//xyg.setTitle(command+"\n"+minimalDescription+"- VIEWS");
						setTitleAndLabelsAndOtherProperties(xyg,minimalDescription+" - Jacobian","Jacobian");
					} catch (PartInitException e2) {
						e2.printStackTrace();
					}
					
					//Build the jump vector (and its transpose) of each reaction. This is very space demanding: numberOfReactions*numberOfSpecies. Check if I can instead store an array of composites (the jvComposite).
					int[][] jumps = computeJumps(crn);
					double[][][] jacobiansAtStep = new double[x.length][][];
					MutableBoolean warned = new MutableBoolean();
					warned.setValue(true);
					for(int step=0;step<x.length;step++){
						double[][] je = computeCurrentJacobian(plotsAll, step, crn,jumps,getMsgDialogShower(),getOut(),getBWOut(),warned);
						jacobiansAtStep[step]=je;
					}
					int n2 = crn.getSpecies().size()*crn.getSpecies().size();
					double[][] plotsJacobian = new double[n2][];
					for(int jacobianentry=0;jacobianentry<n2;jacobianentry++){
						plotsJacobian[jacobianentry]=new double[x.length];
						for(int step=0;step<x.length;step++){
							int species1 = jacobianentry / labelsAll.length;
							int species2 = jacobianentry % labelsAll.length;
							plotsJacobian[jacobianentry][step]=jacobiansAtStep[step][species1][species2];
						}
					}

					for(int jacobianentry=0;jacobianentry<n2;jacobianentry++){
						int species1 = jacobianentry / labelsAll.length;
						int species2 = jacobianentry % labelsAll.length;
						//String label = "J("+(species1+1)+","+(species2+1)+")";
						String label = "J("+(labelsAll[species1])+","+(labelsAll[species2])+")";
						plotViewJacobian.addTrace(label,x,plotsJacobian[jacobianentry]);
					}
					plotViewJacobian.getXYGraph().performAutoScale();
				}

			}

			private void setTitleAndLabelsAndOtherProperties(IXYGraph xyg, String title, String yLabelToUse) {
				command=command.replace("=>", "=").replace("({", "(").replace("})",")");
				int spaces = command.length() - title.length();
				String s = "";
				if(spaces>=0){
					for(int i=0;i<spaces;i++){//spaces/2
						s+=" ";
					}
				}
				title = command +"\n"+s+title;
				//title = title + "\n["+ command +"]";
				xyg.setTitle(title);
				xyg.getPrimaryXAxis().setShowMajorGrid(true);
				xyg.getPrimaryYAxis().setShowMajorGrid(true);
				
				xyg.setShowLegend(showLabels);
				xyg.getXAxisList().get(0).setTitle(xLabel);
				xyg.getYAxisList().get(0).setTitle(yLabelToUse);
				
				xyg.setFont(XYGraphMediaFactory.getInstance().getFont(XYGraphMediaFactory.FONT_TAHOMA));
				/*xyg.primaryXAxis.setDateEnabled(true);
				xyg.primaryYAxis.setAutoScale(true);
				xyg.primaryXAxis.setAutoScale(true);
				xyg.primaryXAxis.setShowMajorGrid(true);
				xyg.primaryYAxis.setShowMajorGrid(true);
				xyg.primaryXAxis.setAutoScaleThreshold(0);
				
				xyg.setFocusTraversable(true);
				xyg.setRequestFocusEnabled(true);*/
			}
		});
	}

}
