package it.imt.erode.crn.ui.handler;



import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IFile;
//import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
//import org.eclipse.core.runtime.CoreException;
//import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
//import org.eclipse.core.commands.common.NotDefinedException;
//import org.eclipse.core.resources.IFile;
//import org.eclipse.core.resources.IFolder;
//import org.eclipse.core.resources.IProject;
//import org.eclipse.core.resources.IWorkspace;
//import org.eclipse.core.resources.ResourcesPlugin;
//import org.eclipse.core.runtime.CoreException;
//import org.eclipse.core.runtime.IPath;
//import org.eclipse.core.runtime.NullProgressMonitor;
//import org.eclipse.core.runtime.Path;
//import org.eclipse.emf.common.util.URI;
//import org.eclipse.emf.ecore.EcorePackage;
//import org.eclipse.emf.ecore.resource.Resource;
//import org.eclipse.emf.ecore.resource.ResourceSet;
//import org.eclipse.emf.ecore.xml.type.SimpleAnyType;
//import org.eclipse.emf.ecore.xml.type.XMLTypeFactory;
//import org.eclipse.ui.IEditorInput;
//import org.eclipse.ui.IEditorPart;
//import org.eclipse.ui.IWorkbench;
//import org.eclipse.ui.IWorkbenchPage;
//import org.eclipse.ui.IWorkbenchWindow;
//import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
//import org.eclipse.xtext.builder.EclipseResourceFileSystemAccess2;
//import org.eclipse.ui.part.FileEditorInput;
//import org.eclipse.xtext.generator.IGenerator;
//import org.eclipse.xtext.generator.OutputConfiguration;
import org.eclipse.xtext.resource.IResourceDescriptions;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.ui.editor.utils.EditorUtils;
//import org.eclipse.xtext.resource.XtextResource;
//import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.ui.resource.IResourceSetProvider;
//import org.eclipse.xtext.util.concurrent.IUnitOfWork;

import com.google.common.util.concurrent.ExecutionError;
import com.google.inject.Inject;
//import com.google.inject.Provider;

public class RunHandlerFromPackageExplorer extends AbstractHandler implements IHandler {

	/*@Inject
    private IGenerator generator;*/
 
   /* @Inject
    private Provider<EclipseResourceFileSystemAccess2> fileAccessProvider;*/
     
    @Inject
    IResourceDescriptions resourceDescriptions;
     
    @Inject
    IResourceSetProvider resourceSetProvider;
	
    /*@Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
    	System.out.println("execute RunHandler");
    	  //used to save the command name (defined in extensions)
    	  String mode = "";
    	 
    	  try {
    	    mode = event.getCommand().getName();
    	  } catch (NotDefinedException e1) {
    	  // TODO Auto-generated catch block
    	    e1.printStackTrace();
    	  }
    	 
    	  // stuff to get the workbench and current file
    	  IWorkbench wb = PlatformUI.getWorkbench();
    	  IWorkbenchWindow window = wb.getActiveWorkbenchWindow();
    	  IWorkbenchPage page = window.getActivePage();
    	  IEditorPart editor = page.getActiveEditor();
    	  IEditorInput input = editor.getEditorInput();
    	  IPath path = ((FileEditorInput) input).getPath();
    	 
    	  String name = path.toString();
    	  File myfile = new File(name);
    	 
    	  IWorkspace workspace= ResourcesPlugin.getWorkspace();
    	  IPath location= Path.fromOSString(myfile.getAbsolutePath());
    	  IFile file= workspace.getRoot().getFileForLocation(location);
    	 
    	  IProject project = file.getProject();
    	  IFolder srcGenFolder = project.getFolder("src-gen");
    	  if (!srcGenFolder.exists()) {
    	    try {
    	      srcGenFolder.create(true, true, new NullProgressMonitor());
    	    } catch (CoreException e) {
    	    return null;
    	    }
    	  }
    	 
    	  final EclipseResourceFileSystemAccess2 fsa = fileAccessProvider.get();
    	 
    	  fsa.setProject(project);
    	 
    	  //same stuff
    	  //fsa.setOutputPath("src-gen");
    	  fsa.setOutputPath(srcGenFolder.getName().toString());
    	 
    	  fsa.setMonitor(new NullProgressMonitor());
    	  Map<String, OutputConfiguration> teste = fsa.getOutputConfigurations();
    	 
    	  Iterator<Entry<String, OutputConfiguration>> it = teste.entrySet().iterator();
    	 
    	  //make a new Outputconfiguration <- needed
    	  while(it.hasNext()){
    	 
    	    Entry<String, OutputConfiguration> next = it.next();
    	    OutputConfiguration out = next.getValue();
    	    out.isOverrideExistingResources();
    	    out.setCreateOutputDirectory(true); // <--- do not touch this
    	 
    	  }
    	  // ----->
    	  
    	  URI uri = URI.createPlatformResourceURI(file.getFullPath().toString(), true);
    	  ResourceSet rs = resourceSetProvider.get(project);
    	  Resource r = rs.getResource(uri, true);
    	 
    	  // to pass a String inside a resource i have to wrap it in a EOBject
    	  SimpleAnyType wrapper = XMLTypeFactory.eINSTANCE.createSimpleAnyType();
    	  wrapper.setInstanceType(EcorePackage.eINSTANCE.getEString());
    	  wrapper.setValue(mode);
    	  //
    	 
    	  // add string to resource
    	  r.getContents().add(wrapper);
    	 
    	  generator.doGenerate(r, fsa);
    	 
    	  return null;
    	 
    	}*/
    

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
    	//System.out.println("execute RunHandlerFromPackageExplorer");
    	ISelection selection = HandlerUtil.getCurrentSelection(event);
    	if (selection instanceof IStructuredSelection) {

    		IStructuredSelection structuredSelection = (IStructuredSelection) selection;
    		Object[] arraySelection = structuredSelection.toArray();
    		//ThreadBuffer<RunHandlerFromPackageExplorerWorker> threadsBuffer = new ThreadBuffer<>(arraySelection.length);
    		ThreadBuffer<MyCRNProgamExecutorWorker> threads = new ThreadBuffer<MyCRNProgamExecutorWorker>(arraySelection.length*2);
    		for(int i=0;i<arraySelection.length;i++) {
    			Object currentElement = arraySelection[i];
    			//Object firstElement = structuredSelection.getFirstElement();
    			if (currentElement instanceof IFile) {
//    				/threads.addWorker(new MyCRNProgamExecutorWorker(consoleOut, bwOut, project, console, threadsBuffer, mec, msgVisualizer, constraints, booleanUpdateFunctions, commandsReader, guidog));
    				IFile file = (IFile) currentElement;
    				
    				IProject project = file.getProject();
    				ResourceSet rs = resourceSetProvider.get(project);
    				
    				/*
    				Test to run files of interest
    				IWorkspace ws = file.getWorkspace();
    	    		IPath fp = file.getFullPath();
    	    		IPath fp2 = fp.removeLastSegments(1);
    	    		IPath fp3 = fp2.append("BIOMD0000000030.ode");
    	    		File file3 = fp3.toFile();
    	    		URI uri3 = URI.createPlatformResourceURI(fp3.toString(), true);
    	    		Resource r3 = rs.getResource(uri3, true);
    	    		*/
    				
    				
    				/*IFolder srcGenFolder = project.getFolder("src-gen");
                if (!srcGenFolder.exists()) {
                    try {
                        srcGenFolder.create(true, true,
                                new NullProgressMonitor());
                    } catch (CoreException e) {
                        return null;
                    }
                }

                final EclipseResourceFileSystemAccess2 fsa = fileAccessProvider.get();
                fsa.setOutputPath(srcGenFolder.getFullPath().toString());*/

    				URI uri = URI.createPlatformResourceURI(file.getFullPath().toString(), true);
    				

    				Resource r = rs.getResource(uri, true);
    				//generator.doGenerate(r, null);


    				boolean canSynchEditor=false;
    				//



    				XtextEditor xtextEditor = EditorUtils.getActiveXtextEditor();
    				IEditorInput editorInput=null;
    				if(xtextEditor==null){
    					editorInput = EditorUtils.createEditorInput(file);
    				}
    				else{
    					editorInput = xtextEditor.getEditorInput();
    				}

    				//IEditorInput editorInput = xtextEditor.getEditorInput();
    				if (editorInput instanceof IFileEditorInput)
    				{
    					IFile openFile = ((IFileEditorInput)editorInput).getFile();
    					if(openFile.equals(file)){
    						canSynchEditor=true;
    					}
    				}


    				IPath projectLocation = project.getLocation(); //file.getProject().getLocation();
    				IPath wsLocation = projectLocation.removeLastSegments(1);
    				//String projectName = projectLocation.lastSegment();
    				IPath erodeFileLocation = file.getLocation();
    				IPath erodeFileLocationRelativeToWS = erodeFileLocation.makeRelativeTo(wsLocation);
    				erodeFileLocationRelativeToWS=erodeFileLocationRelativeToWS.makeAbsolute();
    				IPath erodeFileParentLocationRelativeToWS = erodeFileLocationRelativeToWS.removeLastSegments(1);
    				ExecutorUnitOfWork execuw = new ExecutorUnitOfWork(canSynchEditor,file.getProject(),erodeFileParentLocationRelativeToWS,erodeFileLocationRelativeToWS,threads);
    				XtextResource xr = (XtextResource)r;
    				//RunHandlerFromPackageExplorerWorker worker = new RunHandlerFromPackageExplorerWorker(threadsBuffer,execuw,xr);
    				//worker.start();
    				
    				
    				try {
    					execuw.exec(xr);
    				} catch (Exception e) {
    					throw new ExecutionError(e.getMessage(), new Error(e.getCause()));
    				}
    				
    			}
    		}   
    	}
    	return null;
    }

}
