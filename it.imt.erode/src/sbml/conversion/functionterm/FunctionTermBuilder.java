package sbml.conversion.functionterm;

import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.ext.qual.FunctionTerm;

import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.MVUpdateFunctionByCases;
import sbml.configurations.SBMLConfiguration;

public class FunctionTermBuilder {
    private static final SBMLConfiguration CONFIG = SBMLConfiguration.getConfiguration();

    public FunctionTerm createFunctionTerm(int resultLevel, ASTNode astNode) {
        FunctionTerm f = new FunctionTerm(CONFIG.getLevel(), CONFIG.getVersion());
        f.setResultLevel(resultLevel);
        f.setMath(astNode);
        return f;
    }

    public FunctionTerm createDefaultTerm(IUpdateFunction updateFunction) {
    	FunctionTerm defaultTerm = new FunctionTerm();
    	if(updateFunction instanceof MVUpdateFunctionByCases) {
    		int oVal = ((MVUpdateFunctionByCases) updateFunction).getOtherwiseVal();
    		if(oVal!=-1) {
                defaultTerm.setDefaultTerm(true);
                defaultTerm.setResultLevel(oVal);
    		}
    		else {
    			 defaultTerm.setDefaultTerm(false);
    		}
    	}
    	else {
            defaultTerm.setDefaultTerm(true);
            defaultTerm.setResultLevel(0);
    	}
        
        return defaultTerm;
    }
}
