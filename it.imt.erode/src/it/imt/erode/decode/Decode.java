package it.imt.erode.decode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.ui.console.MessageConsoleStream;
import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.ASTNode.Type;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.text.parser.ParseException;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.DialogType;
//import it.imt.erode.commandline.DialogType;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.commandline.Terminator;
import it.imt.erode.crn.implementations.CRN;
import it.imt.erode.crn.implementations.CRNReaction;
import it.imt.erode.crn.implementations.CRNReactionArbitraryAbstract;
import it.imt.erode.crn.implementations.CRNReactionArbitraryGUI;
import it.imt.erode.crn.implementations.Composite;
import it.imt.erode.crn.implementations.Species;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
//import it.imt.erode.partition.implementations.Block;
//import it.imt.erode.partition.implementations.Partition;
//import it.imt.erode.partition.interfaces.IBlock;
//import it.imt.erode.partition.interfaces.IPartition;
//import it.imt.erode.partitionrefinement.algorithms.CRNBisimulationsNAry;

public class Decode { 
	
	private ICRN crn;
	private ISpecies[] species;
	HashMap<String,ISpecies> speciesNameToSpecies;
	
	/**
	 * change matrix for all reactions. Size [reactions][species]
	 */
	//int[][] reacMatrix;
	
	/**
	 * decompressed states, in order.
	 */
	StateSet decompStates; //LinkedList
	
	/**
	 * same length as crn.species: Sum term for total population per species.
	 */
	//LinkedList<Term> totalPopTerms;
	
	/**
	 * The matrix with the core equations of the decompressed system.
	 */
	//HashMap<String, Sum> eqMatrix;
	//Sum[] compStateEqs;
	
	/**
	 * rate terms, already adapted to whether explicit or stochiometric
	 */
	//Term[] rates;
	
	/**
	 * max number of elements per species in decomp states
	 */
	int[] max;
	
	/**
	 * initially decompressed elements
	 */
	private int[] ini;
	
	/**
	 * if true: treat catalysts like educts/products, move from pool to dec if used. 
	 * Not moving catalysts could prevent unwanted behavior sometimes but is work in progress for the general case
	 */
	//private boolean catEducts = true;

	public ICRN decode(ICRN crn, String fileName, boolean verbose, MessageConsoleStream out, BufferedWriter bwOut,
			IMessageDialogShower messageDialogShower, Terminator terminator, HashMap<String, Integer> speciesToLimit) {
		CRNReducerCommandLine.print(out,bwOut,":\n");
		if(verbose){
			CRNReducerCommandLine.print(out,bwOut,"Decompressing the model, and writing it in file "+fileName +"\n");
		}
		
		
		this.crn = crn;
		/*
		reacMatrix = new int[crn.size()][crn.getSpecies().size()];
		rates = new Term[crn.size()];
		for(int i =0; i<crn.size();i++){
			Reaction r = crn.get(i);
			reacMatrix[i] = r.totalChange.get();
			/*
			for(String s: r.educts) reacMatrix[i][crn.species.indexOf(s)]-=1;
			for(String s: r.products) reacMatrix[i][crn.species.indexOf(s)]+=1;
			* /
			
			rates[i] = r.rate;	
		}
		*/
		
		//initialize comp state eqs as empty
//		compStateEqs = new Sum[crn.getSpecies().size()];
//		for(int i = 0; i<compStateEqs.length; i++){
//			compStateEqs[i] = new Sum();
//		}
		
		
		
		/*
		if(crn.isSymbolic()){
			CRNReducerCommandLine.printWarning(out,bwOut, true, messageDialogShower, "Decompression can only be done for models without symbolic parameters.", DialogType.Error);
			return;
		}
		*/
		
		this.speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpecies().size());
		for (ISpecies s : crn.getSpecies()) {
			speciesNameToSpecies.put(s.getName(), s);
		}
		
		this.species=new ISpecies[crn.getSpecies().size()];
		this.ini = new int[species.length];
		this.max = new int[species.length];
		int i=0;
		for (ISpecies s : crn.getSpecies()) {
			species[i]=s;
			if(speciesToLimit.containsKey(s.getName()))
				max[i] = speciesToLimit.get(s.getName());
			ini[i] = Math.min(max[i], s.getInitialConcentration().intValue());
			i++;
		}
		CRNReducerCommandLine.print(out,bwOut,"\tPreparing State Space...");
		generateStates();
		CRNReducerCommandLine.print(out,bwOut," done\n");
		
		ICRN decompressedCRN = null;
		try {
			decompressedCRN = generateCRN(out, bwOut, terminator);
		} catch (SBMLException | ParseException | IOException | IllegalStateException e) {
			String m = e.toString()+":\n"+e.getMessage();
			CRNReducerCommandLine.printWarning(out,bwOut, true, messageDialogShower, m, DialogType.Error);
			return null;
		}
		
		//crn.getMath().evaluate("k1+3+k2");
		

		 		
		//newSpecies = new Species(name, longname,IDENTIFIER, ic,string representation of the IC,false);
		/* 
		 //Default block for all reduced species
		IBlock uniqueBlock = new Block();
		IPartition trivialPartition = new Partition(uniqueBlock,reducedCRN.getSpecies().size());
		for(ISpecies reducedSpecies : reducedCRN.getSpecies()){
			uniqueBlock.addSpecies(reducedSpecies);
		}
		 */
		
		
		return decompressedCRN;
		//return crn;
		
		
		
	}
	
	/**
	 * generates states for decompression and saves them in attribute.
	 * This one does traditional state space exploration.
	 * 
	 * @param species starting decomp state (length of species.size)
	 * @param max max number of decompressed elements per species (length of species.size)
	 * @throws Exception 
	 */
	private void generateStates(){
		
		this.decompStates = new StateSet();
		LinkedList<StateId> active = new LinkedList<StateId>();
		LinkedList<StateId> neu = new LinkedList<StateId>();
		
		active.add(new StateId(ini));
		while(true){
			for(StateId a: active){
				for(ICRNReaction r : crn.getReactions()){
					
					if(r.hasArbitraryKinetics()){
						StateId reac = maxChangeFrom(a,r);
						StateId x = a.add(reac);
						for(int i = 0; i<x.size(); i++){ //enforce max limit
							if(x.get(i)>max[i])
								x.set(i, max[i]);
						}
						if(!decompStates.keySet().contains(x) && !active.contains(x) && !neu.contains(x))
							neu.add(x);
					}
					
					else  {
						IComposite eductSet = r.getReagents();
						IComposite productSet = r.getProducts();
						HashSet<StateId> partials= changeFrom(a,eductSet); 
						for(StateId preac: partials){
							StateId x = a.add(preac);
							for(int i = 0; i<species.length; i++) {
								x.change(i, productSet.getMultiplicityOfSpecies(species[i]));
							}
//							StateId x = a.add(preac).add(r.plus); //was originally
//							if(catEducts) //TODO catalysts
//								x = x.add(r.catalysts);
							for(int i = 0; i<x.size(); i++){ //enforce max limit
								if(x.get(i)>max[i])
									x.set(i, max[i]);
							}
							if(!decompStates.keySet().contains(x) && !active.contains(x) && !neu.contains(x))
								neu.add(x); 
							//thought about testing for x.isEmpty, but in case of degeneration having the Empty state is fine
							//some models might even start in the empty state
						}
					}
				}
			}
			decompStates.addAll(active);
			if(neu.isEmpty()) break;
			active = neu;
			neu = new LinkedList<StateId>();
		}
		//forall: assign number (i++)
		int n = 0;
		for(StateId s: decompStates.keySet()){
			s.setNumber(n);
			n++;
		}
	}
	
	/**
	 * computes binomial k out of n
	 * if int overflows, change to:
	 * 
	 * static BigInteger binomial(final int N, final int K) {
	 * BigInteger ret = BigInteger.ONE;
	 * for (int k = 0; k < K; k++) {
	 * ret = ret.multiply(BigInteger.valueOf(N-k)).divide(BigInteger.valueOf(k+1));
	 * }
	 * return ret;
	 * }
	 * 
	 * @param N the big number
	 * @param K choose k
	 * @return
	 */
	public static int binomial(final int N, final int K) {
	    int ret = 1;
	    int L = Math.min(N-K, K);
	    for (int k = 0; k < L; k++) {
	        ret = ret*(N-k)/(k+1);
	    }
	    return ret;
	}
	
	public static int factorial(int n){
		if(n<=1) return 1;
		else return n*factorial(n-1);
	}
	
	/**
	 * For CRN output.
	 * Generates term for decompressed mass action rates, 
	 * keeping them mass action. For total rate multiply with 
	 * population of decompressed state and binomial of pool educts.
	 * 
	 * @param i decomp educt
	 * @param r reaction no.
	 * @param p decomp part
	 * @param decCat decomp part of catalysts (currently not in use)
	 * @return
	 */
	private String crnMassActEq(StateId i, ICRNReaction r, StateId p, StateId decCat) {
		String result = r.getRateExpression();
		int factor=1;
		//int div = 1;
		
		for(int s=0; s<i.size();s++){//for species
			//int totalre = Math.max(0,-crn.get(r).totalChange.get(s));
			//int totalcat = crn.get(r).catalysts.get(s);
			
			//int re = -p.get(s); // dec elements reacting
			//int used = re; //dec elements used, including cat
			//if(!catEducts)
			//	used +=decCat.get(s);
			int n = binomial(i.get(s), -p.get(s)); //-p.get(s)=used
			factor = factor*n;	
			
//			if(!catEducts && totalcat>0){ //TODO catalysts
//				int parts = binomial(totalre+totalcat, used);
//				int count = binomial(totalcat,decCat.get(s))*binomial(totalre, re);
//				
//				if(parts!=count){
//					div = div* parts;
//					factor = factor*count;
//				}
//			}
			//factor = factor*n;
		}
		
//		if(div>1){
//			result.addTerm(new Div(new Num(factor),new Num(div)));
//		}
//		else 
		if(factor>1)
			result = factor + " * " +result;
		return result;
	}
	
	/**
	 * creates rate equation term for reactions with arbitrary rates, 
	 * i.e. no binomial coefficients
	 * simple replacement of species name by (comp+decomp)
	 * 
	 * @param educt
	 * @param r
	 * @param curPop population terms for all species in educt dec state (dec+comp)
	 * @return
	 * @throws ParseException 
	 */
	private String arbRateEquation(StateId educt, CRNReactionArbitraryAbstract r) throws ParseException{
		//String oldrate = r.getRateExpression();
		ASTNode mod = ASTNode.parseFormula(r.getRateExpression()).clone(); 
		replaceSpecVar(mod, educt); //was: CRNReactionArbitraryGUI.replaceVar(...)
		ASTNode result = new ASTNode(Type.TIMES);
		result.addChild(new ASTNode("Dec"+educt.getNumber()));
		result.addChild(mod);
		return result.toFormula();
		
		//String t = rates[r].replace(crn.getSpecies(), educt);
		//if(outputType == OutputType.ERODE)
			//return "new Mult(new Var(Dec+educt.getNumber()), t)";
		//else
		//	return new Mult(xRef(educt.getNumber()), t);
	}
	
	/**
	 * to be used on a cloned copy. modifies node.
	 * @param node
	 * @param educt
	 * @return
	 */
	private void replaceSpecVar(ASTNode node, StateId educt) { //make static?
		if(node.isOperator()){
			for(int i=0;i<node.getChildCount();i++){
				replaceSpecVar(node.getChild(i), educt);
			}
		}
		else if(node.isFunction() /*&& !node.getName().equals(varsName)*/){
			for(int i=0;i<node.getChildCount();i++){
				replaceSpecVar(node.getChild(i), educt);
			}
		}
		else if(node.isVariable()){
			ISpecies s = speciesNameToSpecies.get(node.getName());
			if(s!=null){
				if(educt.get(s.getID())>0){
					ASTNode n = node.clone();
					ASTNode num = new ASTNode(educt.get(s.getID()));
					num.setType(Type.INTEGER);
					node.addChild(n);
					node.addChild(num);
					node.setType(Type.PLUS);
					return;
				}
				else {
					return;
				}
			}
			else{
				//It must be a parameter
				return;
			}
		}
		else if(node.isNumber()){
			return;
		}
		else{
			throw new UnsupportedOperationException(node.toString());
			
		}
	}
	
	private void replaceSpecVar(ASTNode node) {
		if(node.isOperator()){
			for(int i=0;i<node.getChildCount();i++){
				replaceSpecVar(node.getChild(i));
			}
		}
		else if(node.isFunction() /*&& !node.getName().equals(varsName)*/){
			for(int i=0;i<node.getChildCount();i++){
				replaceSpecVar(node.getChild(i));
			}
		}
		else if(node.isVariable()){
			ISpecies s = speciesNameToSpecies.get(node.getName());
			if(s!=null){
				if(max[s.getID()]>0) {
					int i = 0;
					ASTNode clone = node.clone();
					for(StateId dec: decompStates.keySet()){
						if(dec.get(s.getID())>0) {
							String name="Dec"+dec.getNumber();
							ASTNode d = new ASTNode(name);
							d.setType(Type.NAME);
							d.setName(name);//Andrea fix
							if(dec.get(s.getID())>1) {
								ASTNode num = new ASTNode(dec.get(s.getID()));
								num.setType(Type.INTEGER);
								ASTNode mal = new ASTNode(Type.TIMES);
								mal.addChild(num);
								mal.addChild(d);
								node.addChild(mal);
							}
							else
								node.addChild(d);
							i++; //counts the children that were added to the original node
						}
					}
					if(i>0) { //might not be true if despite limit>0 all states with s>0 are unreachable
						node.addChild(clone);
						node.setType(Type.PLUS);
					}
					return;
				}
			}
			else{
				//It must be a parameter
				return;
			}
		}
		else if(node.isNumber()){
			return;
		}
		else{
			throw new UnsupportedOperationException(node.toString());
			
		}
	}

	/**
	 * 
	 * @param product preliminary product, is modified if max is exceeded
	 * @return overflow
	 * @throws IllegalStateException 
	 */
	private StateId handleOverflow(StateId product) throws IllegalStateException{
		StateId overflow = new StateId(product.size());
		if(!decompStates.containsKey(product)){ //product == -1
			for(int x = 0; x<max.length; x++){ //enforce max limit
				if(product.get(x)>max[x]){
					overflow.set(x, product.get(x)-max[x]);
					product.set(x, max[x]);
				}	
			}
			//product = decompStates.indexOf(productState); 
			if(!decompStates.containsKey(product)) //if still not found
				throw new IllegalStateException("Nonexistent state created in reaction!");
			//String id = "R("+(i+1)+","+(product+1)+")";
		}
		return overflow;
	}
	
	/**
	 * CRN to CRN transformation
	 * Make sure the original CRN does not have species named "Dec"+i
	 * @param bwOut 
	 * @param out 
	 * @param terminator 
	 * @throws ParseException 
	 * @throws IOException 
	 * @throws SBMLException 
	 */
	private ICRN generateCRN(MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator) throws ParseException, SBMLException, IOException, IllegalStateException {
		
		String decompModelName = crn.getName()+"_decODE";
		//create empty crn
		ICRN decompressedCRN = new CRN(decompModelName,crn.getSymbolicParameters() //
				,crn.getConstraints() //
				,crn.getParameters() //same parameters as original
				,crn.getMath() // parser for terms, has access to parameters
				,out,bwOut);
	
		CRNReducerCommandLine.print(out,bwOut,"\tGenerating Species...");
		ISpecies[] decSpecies = new ISpecies[decompStates.size()+species.length];
		//create/add decompressed set species
		for(StateId s: decompStates.keySet()){
			BigDecimal ic;
			String ics;
			if(s.equals(ini)) {
				ic = BigDecimal.ONE;
				ics = "1";
			}
			else{
				ic = BigDecimal.ZERO;
				ics = "0";
			}
			
			ISpecies newSpecies = new Species("Dec"+s.getNumber(), s.toString(),s.getNumber(), ic,ics,false);
			decompressedCRN.addSpecies(newSpecies);
			decSpecies[s.getNumber()] = newSpecies;
		}
		//add atomic species, adjust initial concentration & id
		for(ISpecies s: species) {
			BigDecimal ic = s.getInitialConcentration();
			String ics = s.getInitialConcentrationExpr();
			if(ini[s.getID()]!=0) {
				ic = ic.subtract(BigDecimal.valueOf(ini[s.getID()]));
				ics = ics + "-" + ini[s.getID()];
			}
			ISpecies newSpecies = new Species(s.getName(), s.getOriginalName(),s.getID()+decompStates.size(), ic,ics,false);
			decompressedCRN.addSpecies(newSpecies);
			decSpecies[s.getID()+decompStates.size()] = newSpecies;
			
		}
		/*
		//Default block for all reduced species
		IBlock uniqueBlock = new Block();
		IPartition trivialPartition = new Partition(uniqueBlock,decompressedCRN.getSpecies().size());
		for(ISpecies s : decompressedCRN.getSpecies()){
			uniqueBlock.addSpecies(s);
		}
		*/
		CRNReducerCommandLine.print(out,bwOut," done\n");
		
		CRNReducerCommandLine.print(out,bwOut,"\tGenerating Reactions...");
		List<ICRNReaction> decompReactions = new ArrayList<ICRNReaction>();
		
		for(ICRNReaction r : crn.getReactions()){ //for each reaction r
			if(isPoolReaction(r)) {
				//copy Reaction as-is
				HashMap<ISpecies, Integer> eductHM = new HashMap<ISpecies, Integer>();
				HashMap<ISpecies, Integer> productHM = new HashMap<ISpecies, Integer>();
				for(ISpecies s: species){
					if(r.getReagents().contains(s))
						eductHM.put(decSpecies[decompStates.size()+s.getID()], r.getReagents().getMultiplicityOfSpecies(s));
					if(r.getProducts().contains(s))
						productHM.put(decSpecies[decompStates.size()+s.getID()], r.getProducts().getMultiplicityOfSpecies(s));
				}
				Composite reagents = new Composite(eductHM);
				Composite products = new Composite(productHM);
				ICRNReaction newRea; // = r.cloneReplacingProducts(products); //nope need to separate arbitrary and ma again, create new.
				if(r.hasArbitraryKinetics()){
					ASTNode rate = ASTNode.parseFormula(r.getRateExpression()).clone();
					replaceSpecVar(rate);
					newRea = new CRNReactionArbitraryGUI(reagents, products, rate.toFormula(),r.getID());
				}
				else {
					newRea = new CRNReaction(r.getRate(), reagents, products, r.getRateExpression(),r.getID());
				}
				decompReactions.add(newRea);
			}
			else for(StateId i: decompStates.keySet()){ //for each decompressed state i
			
				if(Terminator.hasToTerminate(terminator)){
					break;
				}
				//reaction with arbitrary rates
				if(r.hasArbitraryKinetics()){
					HashMap<ISpecies, Integer> eductHM = new HashMap<ISpecies, Integer>();
					HashMap<ISpecies, Integer> productHM = new HashMap<ISpecies, Integer>();
				
					StateId decompChange = maxChangeFrom(i,r);
					StateId product = i.add(decompChange);

					
					StateId overflow = handleOverflow(product);
					product = decompStates.get(product); //to have the original id for it
					
					//get pool reactants, products and catalysts
					for(int x = 0; x<max.length; x++){ //for species
						int ed = r.getReagents().getMultiplicityOfSpecies(species[x])-i.get(x);
						int prod = r.getProducts().getMultiplicityOfSpecies(species[x])-product.get(x);
						prod = Math.max(prod, overflow.get(x));
						if(ed>0) {
							eductHM.put(decSpecies[x+decompStates.size()], ed);
						}
						if(prod>0) {
							productHM.put(decSpecies[x+decompStates.size()], prod);
						}
					}
					
					eductHM.put(decSpecies[i.getNumber()], 1);
					productHM.put(decSpecies[product.getNumber()], 1);

					String rateExpression = arbRateEquation(i,(CRNReactionArbitraryAbstract) r); //casting is okay because rate is confirmed arbitrary

					IComposite reagents = new Composite(eductHM);
					IComposite products = new Composite(productHM);
					if(!reagents.equals(products)){
						String reacID = r.getID();
						if(reacID!=null)
							reacID = reacID+"_"+i.getNumber();
						CRNReactionArbitraryGUI newReaction = new CRNReactionArbitraryGUI(reagents, products, rateExpression, reacID);
						decompReactions.add(newReaction);
					}


				}

				//mass-action with binomial coefficient and multiple divisions of reactions	
				else 
				{
					HashSet<StateId> partials= changeFrom(i, r.getReagents()); //i == decompStates.get(i)
					if(!partials.isEmpty()){
						for(StateId p: partials){//for all possible splittings p of reaction 
							StateId product = i.add(p);
							for(int s = 0; s<species.length; s++) {
								product.change(s, r.getProducts().getMultiplicityOfSpecies(species[s]));
							}
//							if(catEducts)//TODO catalysts
//								product = product.add(crn.get(r).catalysts);
							
							StateId overflow = handleOverflow(product);
							
							product = decompStates.get(product); //to have the original id for it
							
							HashMap<ISpecies, Integer> eductHM = new HashMap<ISpecies, Integer>();
							HashMap<ISpecies, Integer> productHM = new HashMap<ISpecies, Integer>();
							eductHM.put(decSpecies[i.getNumber()], 1);
							productHM.put(decSpecies[product.getNumber()], 1);
							for(int s=0; s<species.length; s++) {
								if(overflow.get(s)>0)
									productHM.put(decSpecies[decompStates.size()+s], overflow.get(s));
								int totalEd = r.getReagents().getMultiplicityOfSpecies(species[s]);
								if(totalEd>0) {
									int poolEd = totalEd + p.get(s);
									if(poolEd>0)
										eductHM.put(decSpecies[decompStates.size()+s], poolEd);
								}
							}
							
								
								String rateExpression = crnMassActEq(i,r,p, null); // i:decomp educt; r:reaction; p:split with permutation number
								BigDecimal rate = BigDecimal.valueOf(decompressedCRN.getMath().evaluate(rateExpression));//reaction.getRate();
								//String rateExpression = reaction.getRateExpression();
								if(BigDecimal.ZERO.compareTo(rate)!=0) {
									//Andrea:Double check how reagents/products are added to CRNs in outer parts of code (flags and routines)
									IComposite reagents = new Composite(eductHM);
									IComposite products = new Composite(productHM);
									if(!reagents.equals(products)){
										String reacID = r.getID();
										if(reacID!=null)
											reacID = reacID+"_"+i.getNumber();
										ICRNReaction newReaction = new CRNReaction(rate, reagents, products, rateExpression,reacID);
										decompReactions.add(newReaction);
									}
								}
							
							/*else{ TODO catalysts //if not catEducts
								
								HashSet<StateId> cataDiv = splitCats(i,p,crn.get(r).catalysts);
							
								for(StateId decCat: cataDiv){
									cat = new int[species.length]; //clear cat
									//dec state is cat?
									if(product.equals(i)) //this is the else branch
										cat[product.getNumber()] = 1;
									//input pool catalysts
									for(int x = 0; x<max.length; x++){ 
										cat[decompStates.size()+x] = crn.get(r).catalysts.get(x)-decCat.get(x);
									}
									
									rate = crnMassActEq(i,r,p, decCat); // i:decomp educt; r:reaction; p:split with permutation number
							
									decCrn.add(new Reaction(cat,rea,rate,false));
								}
							}
							*/
							
							
							
						}
					}
				}
				
			}
		}
		CRNReducerCommandLine.print(out,bwOut," done\n");
		
		CRNReducerCommandLine.print(out,bwOut,"\tCreating Views...");
		String[] viewNames = new String[species.length];
		String[] viewExprs = new String[species.length];
		boolean[] viewExpressionsUsesCovariances = new boolean[species.length];
		for(int i=0; i<species.length; i++) {
			viewNames[i] = "V_"+species[i].getName();
			viewExprs[i] = species[i].getName();
		}
		for(StateId s: decompStates.keySet()) {
			for(int i=0; i<species.length; i++) {
				if(s.get(i)>0) {
					String plus = " + ";
					if(s.get(i)>1)
						plus = plus + s.get(i) + "*";
					plus = plus + "Dec"+s.getNumber();
					viewExprs[i] = viewExprs[i] + plus;
				}
			}
		}
		decompressedCRN.setViews(viewNames, viewExprs, viewExprs, viewExpressionsUsesCovariances);
		CRNReducerCommandLine.print(out,bwOut," done\n");
		
		CRNReducerCommandLine.print(out,bwOut,"\tSumming-up reactions with same reagents and products...");
		CRN.collapseAndCombineAndAddReactions(decompressedCRN,decompReactions,out,bwOut);
		CRNReducerCommandLine.print(out,bwOut," done\n");
		return decompressedCRN;
	}
	
	/**
	 * returns true if all of a reaction's educts and products have decompression bound 0, i.e. are completely in the pool.
	 * If true, splitting up the reaction into all decompressed states is unnecessary.
	 * @param r
	 * @return
	 */
	private boolean isPoolReaction(ICRNReaction r) {
		for(ISpecies s: species) {
			if(max[s.getID()]>0) {
				if(r.getReagents().contains(s) || r.getProducts().contains(s))
					return false;
			}
		}
		return true;
	}

	/**
	 * returns all possible partial reactions of this that can be performed on the given state
	 * now only contains educt partitions, as all products go to decomp by default. 
	 * result StateIds contain negative numbers.
	 * 
	 * ---old description--- 
	 * Partial reaction StateId objects contain the number of permutations leading to the same reaction pattern.
	 * (starts with minimal reactions, step by step adding more to get all possibilities)
	 * 
	 * @param c
	 * @param educts Multiset of Educts
	 * @return
	 */
	private HashSet<StateId> changeFrom(StateId c,IComposite educts){
		HashSet<StateId> list = new HashSet<StateId>();
		//int partNum = r.minus.size();
		list.add(new StateId(c.size()));
		//if(sym<0) partNum--; //handle last partial reaction separately
		
		for(int i =0; i<species.length ;i++){ //species
			int eds =educts.getMultiplicityOfSpecies(species[i]); 
			//if(!catEducts) eds -= r.catalysts.get(i); TODO catalysts
			for(int j=0; j<eds; j++){
				//StateId change = new StateId(c.size(), i, -1);
				if(c.get(i)>0){ //>j?
					HashSet<StateId> adds = new HashSet<StateId>();
					//adds.add(change);
					for(StateId old: list){
						StateId neu = old.add(i,-1);
						if(c.canReact(neu))
							adds.add(neu);
					}

				list.addAll(adds); 

				}
			}
			
		}
		return list;
		
	}
	
	/**
	 * returns the maximal possible partial reaction from a decomp state d.
	 * Only number of educts is adjusted, products are assumed to be decompressed completely.
	 * Overflow handling should follow.
	 * 
	 * @param d
	 * @return 
	 */
	private StateId maxChangeFrom(StateId d, ICRNReaction r){
		IComposite eductSet = r.getReagents();
		IComposite productSet = r.getProducts();
		StateId result = new StateId(d.size());
		for(int i=0; i<d.size();i++){
			int ch = productSet.getMultiplicityOfSpecies(species[i]) - eductSet.getMultiplicityOfSpecies(species[i]);
			if((d.get(i)+ch) < 0)
				result.set(i, -d.get(i));
			else
				result.set(i, ch);
		}
		return result;
	}
	

}
