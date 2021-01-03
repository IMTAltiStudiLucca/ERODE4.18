package it.imt.erode.tests;

/*
 * This is a modification of the file TabbedPaneDemo, 
 * https://docs.oracle.com/javase/tutorial/uiswing/examples/components/TabbedPaneDemoProject/src/components/TabbedPaneDemo.java
 * 
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 

/*
 * TabbedPaneDemo.java requires one additional file:
 *   images/middle.gif.
 */

import java.awt.GridLayout;
import java.awt.event.KeyEvent;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.math.plot.Plot2DPanel;

public class TabbedPlots extends JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3735902216539001040L;
	private JTabbedPane tabbedPane;
	
    public TabbedPlots() {
        super(new GridLayout(1, 1));
        
        tabbedPane = new JTabbedPane();
        
        //Add the tabbed pane to this panel.
        add(tabbedPane);
        
        //The following line enables to use scrolling tabs.
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    }
        
    ///** Returns an ImageIcon, or null if the path was invalid. */
    /*protected static ImageIcon createImageIcon(String path) {
    	//return new ImageIcon(path);
    	return new ImageIcon(TabbedPlots.class.getResource(path));
    }*/
    
	public void addPlot(String title, Plot2DPanel graphicalPlot, int mnemonic, ImageIcon icon) {
        tabbedPane.addTab(title, icon, graphicalPlot, title);
        if(mnemonic==0){
        	tabbedPane.setMnemonicAt(mnemonic, KeyEvent.VK_1);
        }
        else if(mnemonic==1){
        	tabbedPane.setMnemonicAt(mnemonic, KeyEvent.VK_2);
        }
        else if(mnemonic==2){
        	tabbedPane.setMnemonicAt(mnemonic, KeyEvent.VK_3);
        } 
        else if(mnemonic==3){
        	tabbedPane.setMnemonicAt(mnemonic, KeyEvent.VK_4);
        }
        else if(mnemonic==4){
        	tabbedPane.setMnemonicAt(mnemonic, KeyEvent.VK_5);
        } 
	}
}