package it.imt.erode.importing.automaticallygeneratedmodels;

import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.ui.console.MessageConsoleStream;
import it.imt.erode.crn.implementations.CRN;
import it.imt.erode.crn.implementations.CRNReaction;
import it.imt.erode.crn.implementations.Command;
import it.imt.erode.crn.implementations.CommandParameter;
import it.imt.erode.crn.implementations.Composite;
import it.imt.erode.crn.implementations.Species;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.expression.evaluator.MathEval;

public class SIR_VaxOnGraph {
	
	public ICRN createSIR_VaxOnGraph(int N,double a,double b,double g,double eta, double kmigr, 
			double delta,
			String suffix,MessageConsoleStream out, BufferedWriter bwOut){	

		//Generate the CRN
		MathEval math = new MathEval();
		String name="SIR_VAXonGraph"+N+suffix;
		ICRN crn = new CRN(name, math, out, bwOut);

		//Create parameters
		crn.addParameter("a", a+"");
		//crn.addParameter("b", b+"");
		//crn.addParameter("g", g+"");
		//crn.addParameter("eta", eta+"");
		crn.addParameter("kmigr", kmigr+"");
		
		//Create species
		HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(N);
		int id=0;

		for(int i=1;i<=N;i++){
			String speciesName="S"+i;
			ISpecies species = new Species(speciesName, id, BigDecimal.ZERO, "0",false);
			crn.addSpecies(species);
			speciesNameToSpecies.put(speciesName, species);
			id++;
			
			speciesName="I"+i;
			species = new Species(speciesName, id, BigDecimal.ONE, "1",false);
			crn.addSpecies(species);
			speciesNameToSpecies.put(speciesName, species);
			id++;
			
			speciesName="R"+i;
			species = new Species(speciesName, id, BigDecimal.ZERO, "0",false);
			crn.addSpecies(species);
			speciesNameToSpecies.put(speciesName, species);
			id++;
		}
		
		{
			//{S1},{I1},{R1}
			ISpecies S1 = speciesNameToSpecies.get("S1");
			ISpecies I1 = speciesNameToSpecies.get("I1");
			ISpecies R1 = speciesNameToSpecies.get("R1");
			ArrayList<HashSet<ISpecies>> preserveZero=new ArrayList<>(3);
			HashSet<ISpecies> set=new HashSet<>(1);
			set.add(S1);
			preserveZero.add(set);
			set=new HashSet<>(1);
			set.add(I1);
			preserveZero.add(set);
			set=new HashSet<>(1);
			set.add(R1);
			preserveZero.add(set);
			crn.setUserDefinedPartition(preserveZero);
		}
		
		//Create reactions
		//Vaccination in a location
		//Si -> Ri , a
		for(int i=1;i<=N;i++){
			IComposite Si = (IComposite)speciesNameToSpecies.get("S"+i);
			IComposite Ri = (IComposite)speciesNameToSpecies.get("R"+i);
			ICRNReaction vaccination = new CRNReaction(BigDecimal.ONE , Si,Ri, "a","vax"+i);
			crn.addReaction(vaccination);
		}
		
		/*
		//Infection among same or different locations (star network imposes S0-All, All-I0)
		 //Sj + Ii -> Ij + Ii,b
		//Multi-compartment model
		//	a)Local/internal SIR in each compartment
		//	b)External infections among connected compartments: disabled because it prevents reduction
		//a)Local/internal SIR in each compartment
		for(int i=1;i<=N;i++){
			ISpecies Ii = speciesNameToSpecies.get("I"+i);
			ISpecies Si = speciesNameToSpecies.get("S"+i);
			
			IComposite reagents = new Composite(Si, Ii);
			IComposite products = new Composite(Ii, Ii);
			
			ICRNReaction infection = new CRNReaction(BigDecimal.ONE , reagents,products, b+"","infection_"+i);
			crn.addReaction(infection);
		}
//		//b External infections among connected compartments: disabled because it prevents reduction
//		ISpecies S1 = speciesNameToSpecies.get("S1");
//		ISpecies I1 = speciesNameToSpecies.get("I1");
//		for(int i=2;i<=N;i++){
//			ISpecies Ii = speciesNameToSpecies.get("I"+i);
//			ISpecies Si = speciesNameToSpecies.get("S"+i);
//			
//			//S1 + I{i>1} -> I1 + I{i>1},b
//			IComposite reagents = new Composite(S1, Ii);
//			IComposite products = new Composite(I1, Ii);
//			ICRNReaction infection = new CRNReaction(BigDecimal.ONE , reagents,products, b+"","infection1_"+i);
//			crn.addReaction(infection);
//			//S{i>1} + I1 -> I{i>1} + I1,b
//			reagents = new Composite(Si, I1);
//			products = new Composite(Ii, I1);
//			infection = new CRNReaction(BigDecimal.ONE , reagents,products, b+"","infection"+i+"_1");
//			crn.addReaction(infection);
//		}
		*/
		
		/*
		 * Bioinformatics & LICS Style + self-infections
		//Infection among same or different locations (star network imposes S0-All, All-I0)
		 //S1 + Ii -> I1 + Ii,b
		ISpecies S1 = speciesNameToSpecies.get("S1");
		ISpecies I1 = speciesNameToSpecies.get("I1");
		for(int i=1;i<=N;i++){
			ISpecies Ii = speciesNameToSpecies.get("I"+i);
			
			IComposite reagents = new Composite(S1, Ii);
			IComposite products = new Composite(I1, Ii);
			
			ICRNReaction infection = new CRNReaction(BigDecimal.ONE , reagents,products, "b","infection1_"+i);
			crn.addReaction(infection);
		}
		//Sj + I1 -> Ij + I1
		for(int j=1;j<=N;j++){
			ISpecies Sj = speciesNameToSpecies.get("S"+j);
			ISpecies Ij = speciesNameToSpecies.get("I"+j);
			
			IComposite reagents = new Composite(Sj, I1);
			IComposite products = new Composite(Ij, I1);
			
			ICRNReaction infection = new CRNReaction(BigDecimal.ONE , reagents,products, "b","infection"+j+"_1");
			crn.addReaction(infection);
		}
		*/
		
		
		
		//Bioinformatics & LICS Style
		//Infection among different locations (star network imposes S0-Others, Others-I0)
		 //S1 + I{i>1} -> I1 + I{i>1},b
		ISpecies S1 = speciesNameToSpecies.get("S1");
		ISpecies I1 = speciesNameToSpecies.get("I1");
		for(int i=2;i<=N;i++){
			ISpecies Ii = speciesNameToSpecies.get("I"+i);
			
			IComposite reagents = new Composite(S1, Ii);
			IComposite products = new Composite(I1, Ii);
			
			ICRNReaction infection = new CRNReaction(BigDecimal.ONE , reagents,products, "b","infection1_"+i);
			crn.addReaction(infection);
		}
		//S{j>1} + I1 -> I{j>1} + I1
		for(int j=2;j<=N;j++){
			ISpecies Sj = speciesNameToSpecies.get("S"+j);
			ISpecies Ij = speciesNameToSpecies.get("I"+j);
			
			IComposite reagents = new Composite(Sj, I1);
			IComposite products = new Composite(Ij, I1);
			
			ICRNReaction infection = new CRNReaction(BigDecimal.ONE , reagents,products, "b","infection"+j+"_1");
			crn.addReaction(infection);
		}
		

		
		//The recovery I -> R for each node
		//Ii -> Ri , g	
		for(int i=1;i<=N;i++){
			IComposite Ii = (IComposite)speciesNameToSpecies.get("I"+i);
			IComposite Ri = (IComposite)speciesNameToSpecies.get("R"+i);
			ICRNReaction recovery = new CRNReaction(BigDecimal.ONE , Ii,Ri, g+"","recovery"+i);
			crn.addReaction(recovery);
		}
		
		//Susceptibilization in a location
		//Ri -> Si , eta
		for(int i=1;i<=N;i++){
			IComposite Ri = (IComposite)speciesNameToSpecies.get("R"+i);
			IComposite Si = (IComposite)speciesNameToSpecies.get("S"+i);
			ICRNReaction susc = new CRNReaction(BigDecimal.ONE ,Ri, Si, eta+"","susc"+i);
			crn.addReaction(susc);
		}
		
		/*
		//Migration: recovered can travel
		//Ri -> Rj , kmigr
		//Due to the star topology, I can only move from center to external, and vice-versa
		IComposite R1 = (IComposite)speciesNameToSpecies.get("R1");
		for(int j=2;j<=N;j++){
			IComposite Rj = (IComposite)speciesNameToSpecies.get("R"+j);
			ICRNReaction migration = new CRNReaction(BigDecimal.ONE ,R1, Rj, kmigr+"","migr"+1+"_"+j);
			crn.addReaction(migration);
			migration = new CRNReaction(BigDecimal.ONE ,Rj, R1, kmigr+"","migr"+j+"_"+1);
			crn.addReaction(migration);
		}
		*/
		
		List<CommandParameter> params=new ArrayList<>();
		CommandParameter p;
		//p = new CommandParameter("reducedFile", "\"SIR_VAXonGrah"+N+"_SE.ode\"");
		//params.add(p);
		p = new CommandParameter("prePartition", "USER");
		params.add(p);
		p = new CommandParameter("csvFile", "\"SE.csv\"");
		params.add(p);
		Command seCommand = new Command("reduceSE", params);
		crn.addCommand(seCommand);
		
		params=new ArrayList<>();
		p = new CommandParameter("delta", delta+"");
		params.add(p);
		//p = new CommandParameter("reducedFile", "\"SIR_VAXonGrah"+N+"_USE.ode\"");
		//params.add(p);
		p = new CommandParameter("prePartition", "USER");
		params.add(p);
		p = new CommandParameter("csvFile", "\"USE.csv\"");
		params.add(p);
		Command useCommand = new Command("reduceUSE", params);
		crn.addCommand(useCommand);
		//String seCommand="reduceSE(reducedFile=\"SIR_VAXonGrah"+n+"_SE.ode\",prePartition=USER)";
		//String useCommand="reduceUSE(delta=0.1, prePartition=USER,reducedFile=\"SIR_VAXonGrah"+n+"_USE.ode\")";
		
		return crn;
	}

}
