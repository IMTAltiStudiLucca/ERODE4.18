package it.imt.erode.crn.ui.perspective.console;

import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.IPageSite;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;

public class ConsoleActions implements IConsolePageParticipant {

    private IPageBookViewPage page;
    //private Action remove;
    private Action stop;
    private IActionBars bars;
    private IConsole console;
    
    private boolean terminated=false;
    
    /*public ConsoleActions() {
		System.out.println("ciao");
	}*/

    @Override
    public void init(final IPageBookViewPage page, final IConsole console) {
        this.console = console;
        
        
        
        this.page = page;
        IPageSite site = page.getSite();
        this.bars = site.getActionBars();

        createTerminateAllButton();
        //createRemoveButton();

        bars.getMenuManager().add(new Separator());
        //bars.getMenuManager().add(remove);

        IToolBarManager toolbarManager = bars.getToolBarManager();

        toolbarManager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, stop);
        //toolbarManager.appendToGroup(IConsoleConstants.LAUNCH_GROUP,remove);

        bars.updateActionBars();

        if(console instanceof MyMessageConsole){
        	((MyMessageConsole)console).setConsoleActions(this);
        }
        
    }

    private void createTerminateAllButton() {
    	
        ImageDescriptor imageDescriptor = ImageDescriptor.createFromFile(getClass(), "/icons/stop_all_active.gif");
        this.stop = new Action("Terminate", imageDescriptor) {

			public void run() {
            	//System.out.println("Terminate button pressed. console name="+console.getName());
            	if(console instanceof MyMessageConsole){
            		((MyMessageConsole) console).setTerminationFlag();
            		terminated = true;
            		stop.setEnabled(false);
            	}
            }
        };

    }

    /*private void createRemoveButton() {
        ImageDescriptor imageDescriptor = ImageDescriptor.createFromFile(getClass(), "/icons/remove_active.gif");
        this.remove= new Action("Remove console", imageDescriptor) {
            public void run() {
                //code to execute when button is pressed
            	System.out.println("Remove button pressed");
            }
        };
    }*/

    @Override
    public void dispose() {
        //remove= null;
        stop = null;
        bars = null;
        page = null;
    }

    /*@Override
    public Object getAdapter(Class<T> adapter) {
        return null;
    }*/

    @Override
    public void activated() {
        updateVis();
    }

    @Override
    public void deactivated() {
        updateVis();
    }

    private void updateVis() {

        if (page == null)
            return;
        boolean isEnabled = true;
        stop.setEnabled(isEnabled && (!terminated));
        //remove.setEnabled(isEnabled);
        bars.updateActionBars();
    }

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		// TODO Auto-generated method stub
		return null;
	}

	public void disableButtonProgrammatically() {
		terminated = true;
		stop.setEnabled(false);
	}

}
