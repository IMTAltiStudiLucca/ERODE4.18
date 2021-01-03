package it.imt.erode.crn.ui.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
//import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.IPath;
//import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
//import org.eclipse.emf.ecore.EObject;
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
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
/*import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;*/
//import org.eclipse.ui.IWorkbench;
//import org.eclipse.ui.IWorkbenchPage;
//import org.eclipse.ui.IWorkbenchWindow;
//import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
//import org.eclipse.ui.part.FileEditorInput;
//import org.eclipse.xtext.builder.EclipseResourceFileSystemAccess2;
//import org.eclipse.xtext.generator.IGenerator;
//import org.eclipse.xtext.generator.OutputConfiguration;
//import org.eclipse.xtext.resource.IResourceDescriptions;
//import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.XtextEditor;
//import org.eclipse.xtext.ui.editor.model.IXtextDocument;
//import org.eclipse.xtext.ui.resource.IResourceSetProvider;
//import org.eclipse.xtext.util.concurrent.IUnitOfWork;
//import org.eclipse.xtext.xbase.lib.IterableExtensions;

//import com.google.inject.Inject;

//import it.imt.erode.crn.chemicalReactionNetwork.ModelDefinition;
//import com.google.inject.Provider;

public class RunHandler extends AbstractHandler implements IHandler {

	/*@Inject
    private IGenerator generator;*/
 
    /*@Inject
    private Provider<EclipseResourceFileSystemAccess2> fileAccessProvider;*/
     
    /*@Inject
    IResourceDescriptions resourceDescriptions;*/
     
    /*@Inject
    IResourceSetProvider resourceSetProvider;*/
	
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
    	//System.out.println("execute runHandler");
    	/*try {
            IWorkbench workbench = PlatformUI.getWorkbench();
            workbench.showPerspective("ErodePerspective.perspective1",
                    workbench.getActiveWorkbenchWindow());
        } catch (WorkbenchException e) {
            e.printStackTrace();
        }*/

    	IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
    	if (activeEditor instanceof XtextEditor) {
    		activeEditor.doSave(new NullProgressMonitor());
    		IFileEditorInput input = (IFileEditorInput) (activeEditor.getEditorInput());
    		IPath projectLocation = input.getFile().getProject().getLocation();
    		IPath wsLocation = projectLocation.removeLastSegments(1);
    		//String projectName = projectLocation.lastSegment();
    		IPath erodeFileLocation = input.getFile().getLocation();
    		IPath erodeFileLocationRelativeToWS = erodeFileLocation.makeRelativeTo(wsLocation);
    		erodeFileLocationRelativeToWS=erodeFileLocationRelativeToWS.makeAbsolute();
    		IPath erodeFileParentLocationRelativeToWS = erodeFileLocationRelativeToWS.removeLastSegments(1);
    		//IPath fullPath = input.getFile().getFullPath();
    		//IPath fullPathOfParent = input.getFile().getParent().getFullPath();
    		//IPath fullPathOfParent = fullPath.removeLastSegments(1);
    		ExecutorUnitOfWork euow = new ExecutorUnitOfWork(true,input.getFile().getProject(),erodeFileParentLocationRelativeToWS,erodeFileLocationRelativeToWS);
    		((XtextEditor)activeEditor).getDocument().readOnly(euow);
    		//((XtextEditor)activeEditor).getDocument().modify(new ExecutorUnitOfWork(true,input.getFile().getProject()));
    		/*Old version where we were not modifying the editor (now instead we can replace an import with the crn specification)*/
    		/*((XtextEditor)activeEditor).getDocument().readOnly(new IUnitOfWork<Boolean, XtextResource>() {

    			@Override
    			public Boolean exec(XtextResource state)
    					throws Exception {
    				generator.doGenerate(state, null);
    				return Boolean.TRUE;
    			}
    		});*/
    	}
    	return null;
    }
    
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
    
}
