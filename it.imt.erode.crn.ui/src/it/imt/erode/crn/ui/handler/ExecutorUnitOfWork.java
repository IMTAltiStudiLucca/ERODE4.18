package it.imt.erode.crn.ui.handler;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.Issue;

import it.imt.erode.commandline.DialogType;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.crn.chemicalReactionNetwork.ModelDefinition;
import it.imt.erode.crn.chemicalReactionNetwork.Precision;
import it.imt.erode.crn.chemicalReactionNetwork.Settings;
import it.imt.erode.crn.ui.perspective.dialogs.MessageDialogShower;
import it.imt.erode.partitionrefinement.algorithms.CRNBisimulationsNAry;

public class ExecutorUnitOfWork implements IUnitOfWork<Boolean, XtextResource> {
	private final boolean canSynchEditor;
	private final IProject project;
	private IPath erodeFileParentLocationRelativeToWS;
	private IPath erodeFileLocationRelativeToWS;
	private ThreadBuffer<MyCRNProgamExecutorWorker> threads;

	public ExecutorUnitOfWork(boolean canSynchEditor, IProject project, IPath erodeFileParentLocationRelativeToWS, IPath erodeFileLocationRelativeToWS,ThreadBuffer<MyCRNProgamExecutorWorker> threads) {
		//erodeFileParentLocationRelativeToWS,erodeFileLocationRelativeToWS
		super();
		this.canSynchEditor=canSynchEditor;
		this.project=project;
		this.erodeFileParentLocationRelativeToWS=erodeFileParentLocationRelativeToWS;
		this.erodeFileLocationRelativeToWS=erodeFileLocationRelativeToWS;
		this.threads=threads;
	}
	public ExecutorUnitOfWork(boolean canSynchEditor, IProject project, IPath erodeFileParentLocationRelativeToWS, IPath erodeFileLocationRelativeToWS) {
		this(canSynchEditor, project, erodeFileParentLocationRelativeToWS, erodeFileLocationRelativeToWS,null);
	}
	
	private void applySettings(Settings content){
//		if(content instanceof UniqueReagents) {
//			CRNReducerCommandLine.univoqueReagents = (((UniqueReagents) content).isUniqueReagents()); 
//		}
//		else
		if(content instanceof Precision) {
			CRNBisimulationsNAry.setSCALE(((Precision) content).getPrecision()); 
		}
	}
	
	private void resetDefaultSettings() {
		//CRNReducerCommandLine.univoqueReagents = CRNReducerCommandLine.univoqueReagentsDefault;
		CRNBisimulationsNAry.setSCALE(CRNBisimulationsNAry.SCALEDefault);
	}

	@Override
	public Boolean exec(XtextResource state)
			throws Exception {
		try {
			IWorkbench workbench = PlatformUI.getWorkbench();
			workbench.showPerspective("ErodePerspective.perspective1",workbench.getActiveWorkbenchWindow());
		} catch (WorkbenchException e) {
			e.printStackTrace();
		}

		//List<Issue> issues = state.getResourceServiceProvider().getResourceValidator().validate(state, CheckMode.ALL, CancelIndicator.NullImpl);
		List<Issue> issues = state.getResourceServiceProvider().getResourceValidator().validate(state, CheckMode.ALL, CancelIndicator.NullImpl);
		int errors=0;
		if(issues!=null && issues.size()>0){
			for (Issue issue : issues) {
				if(issue.isSyntaxError() || issue.getSeverity().equals(Severity.ERROR)){
					errors++;
				}
			}
		}
		if(errors>0){
			IMessageDialogShower msgVisualizer = new MessageDialogShower(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
			String msg = "Please, fix the "+errors+" errors.";
			if(errors==1){
				msg = "Please, fix the error.";
			}
			msgVisualizer.openSimpleDialog(msg, DialogType.Error);
			return false;
		}else if(state.getErrors().size()>0){
			IMessageDialogShower msgVisualizer = new MessageDialogShower(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
			String msg = "Please, fix the "+state.getErrors().size()+" errors.";
			if(state.getErrors().size()==1){
				msg = "Please, fix the error.";
			}
			msgVisualizer.openSimpleDialog(msg, DialogType.Error);
			return false;
		}else{
			List<ModelDefinition> modelDefinitions = new ArrayList<ModelDefinition>();
			TreeIterator<EObject> contents = state.getAllContents();
			
			resetDefaultSettings();
			//System.out.println("#######"+CRNReducerCommandLine.univoqueReagents);
			while(contents.hasNext()){
				EObject content = contents.next();
				if(content instanceof Settings) {
					applySettings((Settings)content);
				}
				else if(content instanceof ModelDefinition){
					modelDefinitions.add((ModelDefinition)content);
				}
			}

			if(threads==null) {
				threads = new ThreadBuffer<MyCRNProgamExecutorWorker>(modelDefinitions.size());
			}
			for (ModelDefinition modelDef : modelDefinitions) {
				MyCRNProgramExecutor myExecutor = new MyCRNProgramExecutor();
				//myExecutor.readAndExecute(modelDef,canSynchEditor);
				myExecutor.readAndExecuteMultiThreaded(modelDef,canSynchEditor,project,erodeFileParentLocationRelativeToWS,erodeFileLocationRelativeToWS,threads);
			}


			/*
			//Discard run invocations if already running
			ModelDefinition modelDef = null;
			TreeIterator<EObject> contents = state.getAllContents();
			while(contents.hasNext()&&modelDef==null){
				EObject content = contents.next();
				if(content instanceof ModelDefinition){
					modelDef=(ModelDefinition)content;
				}
			}
			if(modelDef!=null){
				MyCRNProgramExecutor myExecutor = new MyCRNProgramExecutor();
				//myExecutor.readAndExecute(modelDef,canSynchEditor);
				myExecutor.readAndExecuteMultiThreaded(modelDef,canSynchEditor,project);
			}
			 */
			return true;//true;
		}
	}
}
