package it.imt.erode.crn;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.eclipse.emf.common.util.EList;

import it.imt.erode.crn.chemicalReactionNetwork.BoolExpr;
import it.imt.erode.crn.chemicalReactionNetwork.BooleanCommand;
import it.imt.erode.crn.chemicalReactionNetwork.BooleanImportFolder;
import it.imt.erode.crn.chemicalReactionNetwork.Command;
import it.imt.erode.crn.chemicalReactionNetwork.Import;
import it.imt.erode.crn.chemicalReactionNetwork.ImportFolder;
import it.imt.erode.crn.chemicalReactionNetwork.MVNodeDefinition;
import it.imt.erode.crn.chemicalReactionNetwork.NodeDefinition;

public class ModelElementsCollector {

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
	
	public ModelElementsCollector(String modelName,ArrayList<String> symbolicParameters, EList<BoolExpr> constraintsListXTEXT, ArrayList<ArrayList<String>> parameters, 
			ArrayList<LinkedHashMap<String,String>> reactions, ArrayList<LinkedHashMap<String,String>> algebraicConstraints,
			ArrayList<ArrayList<String>> views, ArrayList<ArrayList<String>> initialConcentrations,ArrayList<ArrayList<String>> initialAlgConcentrations,ArrayList<ArrayList<String>> userPartition,
			ArrayList<Command> commandsList, ArrayList<BooleanCommand> booleanCommandsList, String importString, Import importCommand, String importFolderString, ImportFolder importFolderCommand, BooleanImportFolder booleanImportFolderCommand, String importName,
			ModelDefKind modelDefKind, EList<NodeDefinition> booleanUpdateFunctionsXTEXT, EList<MVNodeDefinition> mvBooleanUpdateFunctionsXTEXT, boolean synchEditor, String absolutePath) {
		super();
		this.absolutePath=absolutePath;
		this.modelName = modelName;
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
		this.importString = importString;
		this.importCommand = importCommand;
		this.importName = importName;
		this.importFolderString = importFolderString;
		this.importFolderCommand = importFolderCommand;
		this.booleanImportFolderCommand = booleanImportFolderCommand;
		this.modelDefKind = modelDefKind;
		this.synchEditor=synchEditor;
		
		this.booleanUpdateFunctionsXTEXT=booleanUpdateFunctionsXTEXT;
		this.mvBooleanUpdateFunctionsXTEXT=mvBooleanUpdateFunctionsXTEXT;
	}

	public boolean isSyncEditor() {
		return synchEditor;
	}
	
	public ArrayList<String> getSymbolicParameters() {
		return symbolicParameters;
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
	
	public ArrayList<ArrayList<String>> getInitialAlgConcentrations() {
		return initialAlgConcentrations;
	}
	
	public ArrayList<ArrayList<String>> getUserPartition() {
		return userPartition;
	}

	public ArrayList<Command> getCommandsList() {
		return commandsList;
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

	public EList<NodeDefinition> getBooleanUpdateFunctionsXTEXT() {
		return booleanUpdateFunctionsXTEXT;
	}
	
	public EList<MVNodeDefinition> getMVBooleanUpdateFunctionsXTEXT() {
		return mvBooleanUpdateFunctionsXTEXT;
	}
	
}
