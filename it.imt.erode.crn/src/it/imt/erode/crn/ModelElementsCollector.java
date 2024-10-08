package it.imt.erode.crn;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.emf.common.util.EList;

import it.imt.erode.crn.chemicalReactionNetwork.BoolExpr;
import it.imt.erode.crn.chemicalReactionNetwork.BooleanCommand;
import it.imt.erode.crn.chemicalReactionNetwork.BooleanImportFolder;
import it.imt.erode.crn.chemicalReactionNetwork.Command;
import it.imt.erode.crn.chemicalReactionNetwork.Import;
import it.imt.erode.crn.chemicalReactionNetwork.ImportBoolean;
import it.imt.erode.crn.chemicalReactionNetwork.ImportFolder;
import it.imt.erode.crn.chemicalReactionNetwork.MVNodeDefinition;
import it.imt.erode.crn.chemicalReactionNetwork.NodeDefinition;
import it.imt.erode.crn.chemicalReactionNetwork.ProbCommand;

public class ModelElementsCollector {

	private ArrayList<String> probProgParameters = new ArrayList<String>(0);
	private ArrayList<String> symbolicParameters = new ArrayList<String>(0);
	private ArrayList<ArrayList<String>> parameters = new ArrayList<ArrayList<String>>(0);
	private ArrayList<LinkedHashMap<String,String>> reactions = new ArrayList<>(0);
	private ArrayList<LinkedHashMap<String,String>> algebraicConstraints = new ArrayList<>(0);
	private ArrayList<ArrayList<String>> views = new ArrayList<ArrayList<String>>(0);
	private ArrayList<ArrayList<String>> userPartition = new ArrayList<ArrayList<String>>(0);
	private ArrayList<ArrayList<String>> initialConcentrations = new ArrayList<ArrayList<String>>(0);
	private ArrayList<ArrayList<String>> initialAlgConcentrations = new ArrayList<ArrayList<String>>(0);
	private ArrayList<Command> commandsList = new ArrayList<>();
	private ArrayList<BooleanCommand> booleanCommandsList = new ArrayList<>();
	private ArrayList<ProbCommand> probCommandsList = new ArrayList<>();
	private String importString=null;
	private Import importCommand=null;
	private String importFolderString=null;
	private ImportFolder importFolderCommand=null;
	private BooleanImportFolder booleanImportFolderCommand=null;
	private String importName = null;
	private ModelDefKind modelDefKind=null;
	private String modelName;
	private String absolutePath;
	private boolean synchEditor;
	private EList<BoolExpr> constraintsListXTEXT;
	private EList<NodeDefinition> booleanUpdateFunctionsXTEXT;
	private EList<MVNodeDefinition> mvBooleanUpdateFunctionsXTEXT;
	private List<ArrayList<String>> parsedConditionsProbProg;
	private boolean realSortMVNet=false;
	private ImportBoolean importBooleanCommand;
	private ArrayList<ArrayList<LinkedHashMap<String, String>>> reactionsProbProg;
	
	
	public ModelElementsCollector(String modelName,ArrayList<String> symbolicParameters,ArrayList<String> probProgParameters, EList<BoolExpr> constraintsListXTEXT, ArrayList<ArrayList<String>> parameters, 
			ArrayList<LinkedHashMap<String,String>> reactions, ArrayList<LinkedHashMap<String,String>> algebraicConstraints,
			ArrayList<ArrayList<String>> views, ArrayList<ArrayList<String>> initialConcentrations,ArrayList<ArrayList<String>> initialAlgConcentrations,ArrayList<ArrayList<String>> userPartition,
			ArrayList<Command> commandsList, ArrayList<BooleanCommand> booleanCommandsList, ArrayList<ProbCommand> probCommandsList, String importString, Import importCommand, ImportBoolean importBooleanCommand, String importFolderString, ImportFolder importFolderCommand, BooleanImportFolder booleanImportFolderCommand, String importName,
			ModelDefKind modelDefKind, 
			ArrayList<ArrayList<String>> parsedConditionsOfAllClauses,//List<String> parsedConditions,	//EList<Expression> conditions,
			ArrayList<ArrayList<LinkedHashMap<String, String>>> reactionsOfAllClauses, 
			EList<NodeDefinition> booleanUpdateFunctionsXTEXT, EList<MVNodeDefinition> mvBooleanUpdateFunctionsXTEXT, boolean synchEditor, String absolutePath) {
		super();
		this.absolutePath=absolutePath;
		this.modelName = modelName;
		this.probProgParameters=probProgParameters;
		this.symbolicParameters=symbolicParameters;
		this.setConstraintsListXTEXT(constraintsListXTEXT);
		this.parameters = parameters;
		this.reactions = reactions;
		this.algebraicConstraints=algebraicConstraints;
		this.views = views;
		this.initialConcentrations = initialConcentrations;
		this.initialAlgConcentrations = initialAlgConcentrations;
		this.userPartition=userPartition;
		this.commandsList = commandsList;
		this.booleanCommandsList=booleanCommandsList;
		this.probCommandsList = probCommandsList;
		this.importString = importString;
		this.importCommand = importCommand;
		this.importBooleanCommand = importBooleanCommand;
		this.importName = importName;
		this.importFolderString = importFolderString;
		this.importFolderCommand = importFolderCommand;
		this.booleanImportFolderCommand = booleanImportFolderCommand;
		this.modelDefKind = modelDefKind;
		this.synchEditor=synchEditor;
		
		this.parsedConditionsProbProg=parsedConditionsOfAllClauses;
		this.reactionsProbProg=reactionsOfAllClauses;
		
		this.booleanUpdateFunctionsXTEXT=booleanUpdateFunctionsXTEXT;
		this.mvBooleanUpdateFunctionsXTEXT=mvBooleanUpdateFunctionsXTEXT;
	}

	public boolean isSyncEditor() {
		return synchEditor;
	}
	
	public ArrayList<String> getSymbolicParameters() {
		return symbolicParameters;
	}
	public ArrayList<String> getProbProgParameters() {
		return probProgParameters;
	}
	
	public ArrayList<ArrayList<String>> getParameters() {
		return parameters;
	}

	public ArrayList<LinkedHashMap<String,String>> getReactions() {
		return reactions;
	}
	
	public ArrayList<LinkedHashMap<String,String>> getAlgebraicConstraints() {
		return algebraicConstraints;
	}

	public ArrayList<ArrayList<String>> getViews() {
		return views;
	}

	public ArrayList<ArrayList<String>> getInitialConcentrations() {
		return initialConcentrations;
	}
	public void setInitialConcentrations(ArrayList<ArrayList<String>> initialConcentrations) {
		this.initialConcentrations=initialConcentrations;
	}
	
	public ArrayList<ArrayList<String>> getInitialAlgConcentrations() {
		return initialAlgConcentrations;
	}
	
	public ArrayList<ArrayList<String>> getUserPartition() {
		return userPartition;
	}
	public void setUserPartition(ArrayList<ArrayList<String>> userPartition) {
		this.userPartition=userPartition;
	}

	public ArrayList<Command> getCommandsList() {
		return commandsList;
	}
	public ArrayList<ProbCommand> getProbCommandsList() {
		return probCommandsList;
	}
	
	public ArrayList<BooleanCommand> getBooleanCommandsList() {
		return booleanCommandsList;
	}

	public String getImportString() {
		return importString;
	}

	public Import getImportCommand() {
		return importCommand;
	}
	public ImportBoolean getImportBooleanCommand() {
		return importBooleanCommand;
	}
	
	public ImportFolder getImportFolderCommand() {
		return importFolderCommand;
	}
	public BooleanImportFolder getBooleanImportFolderCommand() {
		return booleanImportFolderCommand;
	}
	
	public String getImportFolderString() {
		return importFolderString;
	}

	public String getImportName() {
		return importName;
	}

	public ModelDefKind getModelDefKind() {
		return modelDefKind;
	}

	public String getModelName() {
		return modelName;
	}
	public String getAbsolutePath() {
		return absolutePath;
	}

	public EList<BoolExpr> getConstraintsListXTEXT() {
		return constraintsListXTEXT;
	}

	public void setConstraintsListXTEXT(EList<BoolExpr> constraintsListXTEXT) {
		this.constraintsListXTEXT = constraintsListXTEXT;
	}

	public List<ArrayList<String>> getParsedConditionsProbProg() {
		return parsedConditionsProbProg;
	}
	public ArrayList<ArrayList<LinkedHashMap<String, String>>> getReactionsProbProg() {
		return reactionsProbProg;
	}
	
	public EList<NodeDefinition> getBooleanUpdateFunctionsXTEXT() {
		return booleanUpdateFunctionsXTEXT;
	}
	
	public EList<MVNodeDefinition> getMVBooleanUpdateFunctionsXTEXT() {
		return mvBooleanUpdateFunctionsXTEXT;
	}

	public void setRealSortMVNet(boolean b) {
		this.realSortMVNet=b;		
	}
	public boolean getRealSortMVNet() {
		return realSortMVNet;
	}
	
}
