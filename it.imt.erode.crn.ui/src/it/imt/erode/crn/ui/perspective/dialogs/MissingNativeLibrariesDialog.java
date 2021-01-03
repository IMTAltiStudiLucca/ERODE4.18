package it.imt.erode.crn.ui.perspective.dialogs;

import org.eclipse.swt.widgets.Label;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class MissingNativeLibrariesDialog extends Dialog {

	private String path32;
	private String path64;
	private String path32Short;
	private String path64Short;
	private ArrayList<String> paths;
	private String OS;

	public MissingNativeLibrariesDialog(Shell parentShell, String link32, String link64, String link32Short, String link64Short, ArrayList<String> paths, String OS) {
		super(parentShell);
		this.path32=link32;
		this.path64=link64;
		this.path32Short=link32Short;
		this.path64Short=link64Short;
		this.paths=paths;
		this.OS=OS;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
		/*createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);*/
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);

		Label messageLabel= new Label(container, SWT.WRAP);
		//messageLabel.setFont(parent.getFont());
		//messageLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		//messageLabel.setText("Please download one of the two following archives:");
		//msg = "Please download the archive \n   "+link32 +" or \n   "+link64+"\n"+"and add its files to one of the following locations:";


		if(path32!=null || path32Short!=null) {
			messageLabel.setText("Please download one of the two following archives:");
			Link link32 = new Link(container, SWT.NONE);
			link32.setText(createLink(path32,path32Short));
			link32.setToolTipText("For 32 bit Java versions");
			link32.addSelectionListener(new MyWebSelectionListener());
		}
		else {
			messageLabel.setText("Please download one the archive given in the console:");
		}
		Link link64 = new Link(container, SWT.NONE);
		link64.setText(createLink(path64,path64Short));
		link64.setToolTipText("For 64 bit Java versions");
		//link.setSize(400, 100);

		link64.addSelectionListener(new MyWebSelectionListener());

		Label messageLabel2= new Label(container, SWT.WRAP);
		//messageLabel.setFont(parent.getFont());
		//messageLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		messageLabel2.setText("And add the files in the archive to one of the following locations:");

		for (String path : paths) {
			/*Label pathLabel = new Label(container, SWT.WRAP);
			pathLabel.setText("\t"+path);*/
			Link pathLink = new Link(container, SWT.NONE);
			//pathLink.setText(createLink("file:///"+path));
			
			if(OS.equalsIgnoreCase("Linux")){
				if(!path.endsWith(File.separator)){
					path=path+File.separator;
				}
				pathLink.setText(path);
			}
			else if(OS.contains("windows")||OS.contains("Windows")){
				pathLink.setText(createLink(path));
				pathLink.addSelectionListener(new MyLocalSelectionListener());
				File f=new File(path);
				if(!f.isDirectory()){
					pathLink.setEnabled(false);
				}
			}
		}

		/*Button button = new Button(container, SWT.PUSH);
    button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false,
        false));
    button.setText("Press me");
    button.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        System.out.println("Pressed");
      }
    });*/

		return container;
	}

	private String createLink(String path) {
		return createLink(path,path);
	}
	
	private String createLink(String path,String pathToShow) {
		return "\t<a href=\""+path+"\">"+pathToShow+"</a>";
	}


	// overriding this methods allows you to set the
	// title of the custom dialog
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Libraries missing.");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(450, 300);
	}
	
	private final class MyWebSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			//System.out.println("You have selected: "+e.text);
			try {
				//  Open default external browser 
				PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(e.text));
			} 
			catch (PartInitException ex) {
				// TODO Auto-generated catch block
				ex.printStackTrace();
			} 
			catch (MalformedURLException ex) {
				// TODO Auto-generated catch block
				ex.printStackTrace();
			}
		}
	}

	private final class MyLocalSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			//System.out.println("You have selected: "+e.text);
			File file = new File(e.text+"/");
			boolean fileisDir = file.isDirectory();
			boolean exists=file.exists();
			System.out.println(e.text + " is directory: "+fileisDir);
			System.out.println(e.text + " exists: "+exists);
			if(file.isDirectory()){
				Desktop desktop = Desktop.getDesktop();
				try {
					desktop.open(file);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					//e1.printStackTrace();
				} catch(java.lang.IllegalArgumentException e2){
					//TODO Auto-generated catch block
					e2.printStackTrace();
				}
			}
			
		}
	}

} 
