package it.imt.erode.importing.sbml;


import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.util.compilers.FormulaCompiler;

public class MyFormulaCompilerForDoubleSBML extends FormulaCompiler {

	/**
	   * Creates brackets if needed.
	   *
	   * @param node
	   * @return
	   * @throws SBMLException
	   */
	  @Override
	  protected String checkBrackets(ASTNode node) throws SBMLException {
	    String term = node.compile(this).toString();

	    if (node.isSum() || node.isDifference() || node.isUMinus() 
	        || node.isRelational() || node.isLogical()) 
	    {
	      term = brackets(term).toString();
	    } else if (node.isReal()) {
	      if (    (node.getReal() < 0d) ||   
	    		  (node.getExponent()!=1 && node.getExponent()!=0) ||
	    		  (term.contains("E"))
	    		  ) {
	        term = brackets(term).toString();
	      }
	    }

	    return term;
	  }
	
}
