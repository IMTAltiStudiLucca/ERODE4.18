/*
A copy of the JSBMLvisualizer class in the official jsbml repository
@ https://github.com/sbmlteam/jsbml/blob/master/doc/user_guide/src/org/sbml/jsbml/gui/JSBMLvisualizer.java
Only used for experimentation purposes

The class has a slightly modified code in order to improve the visualization gui.
 */

//License from original class
/*
 * ----------------------------------------------------------------------------
 * This file is part of JSBML. Please visit <http://sbml.org/Software/JSBML>
 * for the latest version of JSBML and more information about SBML.
 *
 * Copyright (C) 2009-2018 jointly by the following organizations:
 * 1. The University of Tuebingen, Germany
 * 2. EMBL European Bioinformatics Institute (EBML-EBI), Hinxton, UK
 * 3. The California Institute of Technology, Pasadena, CA, USA
 * 4. The University of California, San Diego, La Jolla, CA, USA
 * 5. The Babraham Institute, Cambridge, UK
 * 6. Marquette University, Milwaukee, WI, USA
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation. A copy of the license agreement is provided
 * in the file named "LICENSE.txt" included with this software distribution
 * and also available online as <http://sbml.org/Software/JSBML/License>.
 * ----------------------------------------------------------------------------
 */

package sbml.demos;

import java.awt.Font;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.UIManager;

import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBase;

/**
 * @author Andreas Dr&auml;ger
 * @since 1.0
 */
public class JSBMLvisualizer extends JFrame {

    /**
     * Generated serial version identifier.
     */
    private static final long serialVersionUID = 6864318867423022411L;

    /**
     * @param tree
     *        The SBML root node of an SBML file
     */
    public JSBMLvisualizer(SBase tree) {
        super("SBML Structure Visualization");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JTree jTree = new JTree(tree);
        Font f = new Font("Courier", Font.ITALIC,16);
        jTree.setFont(f);
        getContentPane().add(new JScrollPane(jTree));
        pack();
        setAlwaysOnTop(true);
        setLocationRelativeTo(null);
        setVisible(true);
        setFont(f);
    }

    /**
     * Main. Note: this doesn't perform error checking, but should. It is an illustration only.
     *
     * @param args path to an SBML file.
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        String path = "D:/Repositories/SBML-Converter-for-ERODE/src/main/resources/sbml/demos/DemoNetwork.sbml";
        SBase tree = SBMLReader.read(new File(path));
        new JSBMLvisualizer(tree);
    }
}