package it.imt.erode.importing.automaticallygeneratedmodels;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;

import org.eclipse.ui.console.MessageConsoleStream;

import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.importing.BioNetGenImporter;

public class RandomCNF {

	public static void main(String[] args) {
		int numberOfVars=8; 
		int numberOfClauses=20;//50;
		int minSizeOfCaluses=4;
		int maxSizeOfCaluses=4;
		String path="/Users/andrea/Documents/runtimes/runtime-ERODE.product/probPrograms/cnf/randomlyGenerated_nVars"+numberOfVars+"_nClauses"+numberOfClauses+"_minClause"+minSizeOfCaluses+"_maxClause"+maxSizeOfCaluses+"/cnf";
		
		for(int model=1;model<100;model++) {
			createAndWriteRandomCNF(path+"_"+model, numberOfVars, numberOfClauses, minSizeOfCaluses, maxSizeOfCaluses, null, null);
		}
		
		
	}
	
	public static void createAndWriteRandomCNF(String path,int numberOfVars, int numberOfClauses, int minSizeOfCaluses,int maxSizeOfCaluses,
			MessageConsoleStream out,BufferedWriter bwOut) {
		String fileName=path+"rnd_V"+numberOfVars+"_C"+numberOfClauses+"_MinCS"+minSizeOfCaluses+"_MaxCS"+maxSizeOfCaluses+".cnf";
		
		CRNReducerCommandLine.println(out,bwOut,"Creating random CNF with " +numberOfVars+" vars, "+ numberOfClauses+" clauses, "+minSizeOfCaluses+" min size of clauses, and " +maxSizeOfCaluses+" max size of clauses. It will be written in: "+fileName);
		
		BioNetGenImporter.createParentDirectories(fileName);
		
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printRandomCRNToNetFile, exception raised while creating the filewriter for file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		
		try {
			Date d = new Date();
			//Date d2 = new Date();
			//int cmp = d.compareTo(d2);
			RandomEngine randomGenerator = new MersenneTwister(d);
			
			
			bw.write("c\nc Created by ERODE on "+d+"\nc\n\n");
			bw.write("c\nc Random CNF with " +numberOfVars+" vars, "+ numberOfClauses+" clauses, "+minSizeOfCaluses+" min size of clauses, and " +maxSizeOfCaluses+" max size of clauses.\nc\n\n");
			
			
			//p cnf 90 300
			bw.write("p cnf "+numberOfVars+" "+numberOfClauses+"\n");
			for(int clause=0;clause<numberOfClauses;clause++) {
				int clauseSize=minSizeOfCaluses;
				clauseSize+=RandomBNG.nextInt(randomGenerator, maxSizeOfCaluses-minSizeOfCaluses);
				
				HashSet<Integer> variablesOfThisClause=new HashSet<>(clauseSize);
				for(int var=0;var<clauseSize;var++) {
					//-7 8 0
					int current=RandomBNG.nextInt(randomGenerator, numberOfVars-1)+1;
					while(variablesOfThisClause.contains(current)) {
						current=RandomBNG.nextInt(randomGenerator, numberOfVars-1)+1;
					}
					variablesOfThisClause.add(current);
					
					if(RandomBNG.nextBoolean(randomGenerator, 0.5)) {
						//negated
						bw.write("-");
					}
					bw.write(current+" ");
				}
				bw.write("0\n");
			}
			
			
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in createAndWriteRandomCNF, exception raised while writing in the file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		finally{
			try {
				bw.close();
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in createAndWriteRandomCNF, exception raised while closing the bufferedwriter of the file: "+fileName);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}
		
	}
	
}
