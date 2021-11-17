package sbml.conversion.functionterm;

import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.ext.qual.FunctionTerm;

import sbml.configurations.SBMLConfiguration;

public class FunctionTermBuilder {
    private static final SBMLConfiguration CONFIG = SBMLConfiguration.getConfiguration();

    public FunctionTerm createFunctionTerm(int resultLevel, ASTNode astNode) {
        FunctionTerm f = new FunctionTerm(CONFIG.getLevel(), CONFIG.getVersion());
        f.setResultLevel(resultLevel);
        f.setMath(astNode);
        return f;
    }

    public FunctionTerm createDefaultTerm() {
        FunctionTerm defaultTerm = new FunctionTerm();
        defaultTerm.setDefaultTerm(true);
        defaultTerm.setResultLevel(0);
        return defaultTerm;
    }
}
