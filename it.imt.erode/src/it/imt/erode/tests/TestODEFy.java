package it.imt.erode.tests;

import java.io.File;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.ASTNode.Type;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SpeciesReference;

import it.imt.erode.importing.BoolCubeImporter;

public class TestODEFy {

	public static void main(String[] args) throws XMLStreamException, IOException {
		/*
		% ODE
		ydot = zeros(3,1);
		ydot(A) = 0;
		ydot(B) = (cvals(A)-cvals(B)) / params(1);
		ydot(C) = (cvals(A)*(1-cvals(B))-cvals(C)) / params(2);
		*/
		/*String exprString = "(cvals(A)-cvals(B)) / params(1)";
		exprString=exprString.replaceAll("cvals(A)", "A");
		MathEval math = new MathEval();
		Expression expr = new Expression(exprString);
		Iterator<String> tokenizer = expr.getExpressionTokenizer();
		while(tokenizer.hasNext()){
			CRNReducerCommandLine.println(tokenizer.next());
		}*/
		
		//importModel("./BoolCubeSBMLNetworks/feedForwardBooleCube.sbml");
		BoolCubeImporter importer = new BoolCubeImporter("./BoolCubeSBMLNetworks/feedForwardBooleCube.sbml",null,null,null);
		importer.importBoolCubeSBMLNetwork(false, false, true);
	}
	
	public static void importModel(String fileName) throws XMLStreamException, IOException{
		SBMLDocument document = SBMLReader.read(new File(fileName));
		Model model = document.getModel();
		int reactionsNumber = model.getNumReactions();
		for(int r=0;r<reactionsNumber;r++){
			org.sbml.jsbml.Reaction sbmlReaction = model.getReaction(r);
			System.out.println();
			System.out.println("Reaction: "+sbmlReaction.toString());
			SpeciesReference product = sbmlReaction.getProduct(0);
			System.out.println("ODE of: "+product);

			ASTNode rateLaw = sbmlReaction.getKineticLaw().getMath();

			if(rateLaw.isOperator() && rateLaw.getType().equals(Type.DIVIDE)){
				rateLaw=rateLaw.getLeftChild();
			}

			/*ArrayList<IMonomial> monomials=addCorrespondingReactionsNavigatingTree(sbmlReaction,rateLaw);
			for (IMonomial monomial : monomials) {
				CRNReducerCommandLine.print(monomial+" + ");
			}
			CRNReducerCommandLine.println("\ndone");*/

		}

		System.out.println("");
		System.out.println("");
	}

	/*private static ArrayList<IMonomial> addCorrespondingReactionsNavigatingTree(Reaction sbmlReaction,ASTNode numerator) {
		
		if(numerator.isOperator()){
			Type type = numerator.getType();
			ArrayList<IMonomial> monomialsLeft = addCorrespondingReactionsNavigatingTree(sbmlReaction,numerator.getLeftChild());
			ArrayList<IMonomial> monomialsRight = addCorrespondingReactionsNavigatingTree(sbmlReaction,numerator.getRightChild());
			if(type.equals(Type.TIMES)){	
				//combine the two
				ArrayList<IMonomial> products = new ArrayList<>(monomialsLeft.size()*monomialsRight.size());
				for (IMonomial left : monomialsLeft) {
					for (IMonomial right : monomialsRight) {
						products.add(new ProductMonomial(left, right));
					}
				}
				return products;
			}
			else if(type.equals(Type.MINUS) ){
				for (IMonomial right : monomialsRight) {
					monomialsLeft.add(new MinusMonomial(right));
				}
				return monomialsLeft;
			}
			else if(type.equals(Type.PLUS) ){
				monomialsLeft.addAll(monomialsRight);
				return monomialsLeft;
			}
		}
		else if(numerator.isVariable()){
			ArrayList<IMonomial> ret = new ArrayList<IMonomial>(1);
			ret.add(new SpeciesMonomial(numerator.getName()));
			return ret; 
		}
		else if(numerator.isNumber()){
			ArrayList<IMonomial> ret = new ArrayList<IMonomial>(1);
			ret.add(new NumberMonomial(numerator.getInteger()));
			return ret;
		}
		
		throw new UnsupportedOperationException(numerator.toString());
		
	}*/
	
	/*
	private static Collection<IDrift> addCorrespondingReactions(Reaction sbmlReaction,ASTNode numerator) {
		CRNReducerCommandLine.println(numerator);
		if(numerator.isOperator()){
			Type type = numerator.getType();
			if(type.equals(Type.TIMES)){
				Collection<IDrift> lefts = addCorrespondingReactions(sbmlReaction, numerator.getListOfNodes().get(0));
				Collection<IDrift> rights = addCorrespondingReactions(sbmlReaction, numerator.getListOfNodes().get(1));
				Collection<IDrift> products = new ArrayList<IDrift>(lefts.size()+rights.size());
				for (IDrift left : lefts) {
					for (IDrift right : rights) {
						products.add(new Product(left, right));
					}
				}
				return products;
			}
			else if(type.equals(Type.MINUS) ){
				Collection<IDrift> lefts = addCorrespondingReactions(sbmlReaction, numerator.getListOfNodes().get(0));
				Collection<IDrift> rights = addCorrespondingReactions(sbmlReaction, numerator.getListOfNodes().get(1));
				lefts.addAll(rights);
			}
			else if(type.equals(Type.PLUS)){
				Collection<IDrift> lefts = addCorrespondingReactions(sbmlReaction, numerator.getListOfNodes().get(0));
				Collection<IDrift> rights = addCorrespondingReactions(sbmlReaction, numerator.getListOfNodes().get(1));
				lefts.addAll(rights);
			}

		}
		else if(numerator.isConstant()){
			CRNReducerCommandLine.println("species:"+numerator);
		}
		else if(numerator.isNumber()){
			CRNReducerCommandLine.println("number:"+numerator);
		}
		return null;
	}
	*/
}
