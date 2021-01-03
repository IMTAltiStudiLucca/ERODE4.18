package it.imt.erode.dot;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.ui.console.MessageConsoleStream;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.importing.AbstractImporter;

public class FigureCreator {

	public static void generateFigure(String dot, String figure, MessageConsoleStream out, BufferedWriter bwOut, boolean verbose) throws IOException {
		CRNReducerCommandLine.print(out,bwOut,"Drawing the causal graph in file "+ figure+" ...");
		AbstractImporter.createParentDirectories(figure);
		
		FileInputStream fis = new FileInputStream(dot);
		MutableGraph loadedDot = new Parser().read(fis);
		fis.close();
		
		//Graphviz.fromGraph(loadedDot).width(700).render(Format.SVG).toOutputStream(os);
		//Graphviz.fromGraph(loadedDot).render(Format.SVG).toOutputStream(os);
		
		FileOutputStream fos = new FileOutputStream(figure+".png");
		Graphviz.fromGraph(loadedDot).render(Format.PNG).toOutputStream(fos);
		fos.close();
		
		fos = new FileOutputStream(figure+".svg");
		Graphviz.fromGraph(loadedDot).render(Format.SVG).toOutputStream(fos);
		fos.close();
		
		
		
		CRNReducerCommandLine.println(out,bwOut," completed");
	}
	
}
