package sbml.conversion.functionterm;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.ext.qual.FunctionTerm;

import it.imt.erode.booleannetwork.updatefunctions.BooleanUpdateFunctionExpr;
import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.MVUpdateFunctionByCases;
import it.imt.erode.booleannetwork.updatefunctions.ValUpdateFunction;
import it.imt.erode.crn.symbolic.constraints.BooleanConnector;
import sbml.configurations.SBMLConfiguration;
import sbml.conversion.nodes.INodeConverter;
import sbml.conversion.nodes.NodeManager;
import sbml.conversion.nodes.binary.BinaryASTConverter;

public class FunctionTermWriter {
    private static final SBMLConfiguration CONFIG = SBMLConfiguration.getConfiguration();

    private FunctionTermBuilder functionTermBuilder;

    public FunctionTermWriter() {
        super();
        this.functionTermBuilder = new FunctionTermBuilder();
    }

    public ListOf<FunctionTerm> convert(IUpdateFunction updateFunction, int maxLevel,boolean mv) {
    	ListOf<FunctionTerm> functionTerms = new ListOf<>(CONFIG.getLevel(),CONFIG.getVersion());
    	HashMap<Integer, ASTNode> cases = this.convertUpdateFunction(updateFunction,mv);
    	//ASTNode astNode = this.convertUpdateFunction(updateFunction);
    	if(cases.size()==1 && cases.keySet().iterator().next()==-1 && cases.values().iterator().next().isNumber()) {
    		BooleanUpdateFunctionExpr trivial = new BooleanUpdateFunctionExpr(new ValUpdateFunction(0), new ValUpdateFunction(0), BooleanConnector.EQ);
    		BinaryASTConverter a = BinaryASTConverter.create(trivial);
    		ASTNode trivialAST = a.getExpressionAST();
    		//do nothing, add the case value as default term
    		Entry<Integer, ASTNode> entry = cases.entrySet().iterator().next();
    		int val = entry.getValue().getInteger();
    		FunctionTerm functionTerm = functionTermBuilder.createFunctionTerm(val, trivialAST);
			functionTerms.add(functionTerm);
    	}
    	else {
    		for(Entry<Integer, ASTNode> entry:cases.entrySet()) {
    			int i=entry.getKey();
    			ASTNode astNode =entry.getValue();
    			//for(int i = maxLevel; i > 0; i--) {
    			FunctionTerm functionTerm = functionTermBuilder.createFunctionTerm(i, astNode);
    			functionTerms.add(functionTerm);
    		}

    		FunctionTerm defaultTerm = functionTermBuilder.createDefaultTerm(updateFunction);
    		if(defaultTerm!=null) {
    			functionTerms.add(defaultTerm);
    		}
    	}
        return functionTerms;
    }

    private HashMap<Integer, ASTNode> convertUpdateFunction(IUpdateFunction updateFunction,boolean mv) {
    	HashMap<Integer, ASTNode> cases;
    	if(updateFunction instanceof MVUpdateFunctionByCases) {
    		MVUpdateFunctionByCases updFuncCases = (MVUpdateFunctionByCases)updateFunction;
    		cases=new LinkedHashMap<Integer, ASTNode>(updFuncCases.getCasesNoOtherwise().size()+1);
    		for(Entry<Integer, IUpdateFunction> entry : updFuncCases.getCasesNoOtherwise().entrySet()) {
    			INodeConverter nodeConverter = NodeManager.create(entry.getValue());
    			cases.put(entry.getKey(), nodeConverter.getExpressionAST());
    		}
    		//The default term is handled elsewhere
    	}
    	else {
    		cases=new LinkedHashMap<Integer, ASTNode>(1);
    		INodeConverter nodeConverter = NodeManager.create(updateFunction);
    		if(mv) {
    			cases.put(-1, nodeConverter.getExpressionAST());
    		}
    		else {
    			cases.put(1, nodeConverter.getExpressionAST());
    		}
    	}
    	return cases;
    }
}
