package it.imt.erode.causalgraph;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.ui.console.MessageConsoleStream;
import org.sbml.jsbml.ASTNode;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.crn.implementations.CRNReactionArbitraryGUI;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.importing.AbstractImporter;

public class CausalGraphCreator {
	
	private LinkedHashMap<ISpecies,LinkedHashSet<ISpecies>> causalGraph;
	private int edges=0;

	private void writeInfluencesOfTarget(Entry<ISpecies, LinkedHashSet<ISpecies>> targetToInfluencing,BufferedWriter bw) throws IOException {
		bw.write("Variables affecting "+targetToInfluencing.getKey().getName()+"\n");
		for(ISpecies source : targetToInfluencing.getValue()) {
			bw.write("\t"+source.getName()+"\n");
		}
	}
	
	public void writeCausalGraph(String fileName, String modelName, List<ISpecies> species,MessageConsoleStream out, BufferedWriter bwOut,boolean verbose) {
		CRNReducerCommandLine.print(out,bwOut,"Writing the causal graph in file "+ fileName+" ...");
		AbstractImporter.createParentDirectories(fileName);
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in writeCausalGraph, exception raised while creating the filewriter for file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}

		try {
			bw.write("Causal graph automatically generated from "+modelName+"\n");
			bw.write("\t"+causalGraph.size()+" nodes and "+edges+" edges.\n");
			bw.write("\n\n");
			
			for(Entry<ISpecies, LinkedHashSet<ISpecies>> targetToInfluencing : causalGraph.entrySet()) {
				//bw.write(influence.toString());
				writeInfluencesOfTarget(targetToInfluencing,bw);
				bw.write("\n");
			}
			
			
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in writeCausalGraph, exception raised while writing in the file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		finally{
			if(verbose){
				CRNReducerCommandLine.println(out,bwOut,"Writing in file "+fileName+" completed");
			}
			try {
				bw.flush();
				bw.close();
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in writeCausalGraph, exception raised while closing the bufferedwriter of the file: "+fileName);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}
		
		CRNReducerCommandLine.println(out,bwOut," completed");
	}
	
	public void writeDOTCausalGraph(String fileName, String modelName, List<ISpecies> species,MessageConsoleStream out, BufferedWriter bwOut,boolean verbose) {
		CRNReducerCommandLine.print(out,bwOut,"Writing the causal graph in DOT file "+ fileName+" ...");
		AbstractImporter.createParentDirectories(fileName);
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in writeDOTCausalGraph, exception raised while creating the filewriter for file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}

		try {
			bw.write("/* Open in your favourite Graphviz viewer, e.g.\n" + 
					" * https://dreampuf.github.io/GraphvizOnline/\n" + 
					" * https://edotor.net/\n" + 
					" */\n");
			bw.write("/* Causal graph automatically generated from "+modelName+"\n");
			bw.write(" * "+causalGraph.size()+" nodes and "+edges+" edges.\n");
			bw.write(" */\n\n");
			
			bw.write("digraph "+modelName+" {\n");
			String space="  ";
			//bw.write(space+"subgraph «diagram.attacker.name» {\n");
			writeNodes(bw,space);
			writeTransitions(bw,space);
			bw.write("}");

			
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in writeDOTCausalGraph, exception raised while writing in the file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		finally{
			if(verbose){
				CRNReducerCommandLine.println(out,bwOut,"Writing in file "+fileName+" completed");
			}
			try {
				bw.flush();
				bw.close();
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in writeDOTCausalGraph, exception raised while closing the bufferedwriter of the file: "+fileName);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}
		
		CRNReducerCommandLine.println(out,bwOut," completed");
	}
	
	private void writeNodes(BufferedWriter bw,String space) throws IOException {
		bw.write(space+space+"//States\n" + 
				space+space+"node [shape=ellipse style=rounded color=blue penwidth=1.0]\n");
		for(ISpecies node : causalGraph.keySet()) {
			bw.write(space+space+node.getName()+"\n");
		}
		bw.write("\n");
	}
	private void writeTransitions(BufferedWriter bw,String space) throws IOException {
		bw.write(space+space+"//Transitions\n");
		bw.write(space+space+"edge [color=blue penwidth=1.0]\n");
		for(Entry<ISpecies, LinkedHashSet<ISpecies>> targetToInfluencing : causalGraph.entrySet()) {
			ISpecies target = targetToInfluencing.getKey();
			for(ISpecies source : targetToInfluencing.getValue()) {
				bw.write(space+space+source.getName()+" -> "+target.getName()+"[label=\"\"]\n");
			}
		}
	}

	public void createCausalGraphTrivial(ICRN crn, MessageConsoleStream out, BufferedWriter bwOut,boolean verbose) {
		CRNReducerCommandLine.print(out,bwOut,"Creating the causal graph with trivial approach for model "+ crn.getName()+" ... ");
		long begin = System.currentTimeMillis();
		
		HashMap<String, ISpecies> nameToSpecies = new HashMap<>(crn.getSpeciesSize());
		for(ISpecies species : crn.getSpecies()) {
			nameToSpecies.put(species.getName(), species);
		}
		
		initCausalGraph(crn);
		
		for(ICRNReaction r : crn.getReactions()) {
			CRNReactionArbitraryGUI reaction = (CRNReactionArbitraryGUI)r;
			ISpecies target = reaction.getReagents().getFirstReagent();
			List<ASTNode> l =reaction.getSpeciesInRateLaw(reaction.getRateLaw(), nameToSpecies);
			LinkedHashSet<ISpecies> speciesAffectingTarget = new LinkedHashSet<ISpecies>(l.size());
			for(ASTNode n : l) {
				speciesAffectingTarget.add(nameToSpecies.get(n.getName()));
				edges++;
			}
			causalGraph.put(target, speciesAffectingTarget);
//			String one="1";
//			for(ISpecies source : involvedSpecies) {
//				ICRNReaction influence = new CRNReaction(BigDecimal.ONE, (IComposite)source, (IComposite)target, one, null);
//				causalGraph.add(influence);
//			}
		}
		
		long end = System.currentTimeMillis();
		CRNReducerCommandLine.print(out,bwOut,causalGraph.size()+" nodes and "+edges+" edges ...");
		CRNReducerCommandLine.println(out,bwOut," completed in "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s).");
		
	}

	private void initCausalGraph(ICRN crn) {
		edges=0;
		causalGraph = new LinkedHashMap<>(crn.getSpeciesSize());
	}

}
