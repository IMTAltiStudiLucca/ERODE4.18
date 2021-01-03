package it.imt.erode.crn.ui.perspective.wizards;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

import it.imt.erode.importing.GUICRNImporter;

public class WizardNewERODEFileCreationPage extends WizardNewFileCreationPage {

	private Combo combo;
	public static final String RNTYPE = "Yes, with reactions";
	public static final String ODEsTYPE = "Yes, with ODEs";
	public static final String DAEsTYPE = "Yes, with DAEs";
	public static final String BNTYPE = "Yes, a Boolean network";
	public static final String NOTEMPLATETYPE = "No";
	
	public WizardNewERODEFileCreationPage(IStructuredSelection selection) {
		super("New ERODE file", selection);
		setTitle("ERODE file");
		setDescription("Create a new ERODE file");
        setFileExtension("ode");
	}
	
	@Override
	protected void createAdvancedControls(Composite parent) {
		Label label1 = new Label(parent, SWT.NONE);
		label1.setText("Would you like an initial template file?");
		
		combo = new Combo(parent, SWT.READ_ONLY);
		//combo.add("System of Ordinary Differential Equations");
		//combo.add("Reaction Network");
		combo.add(NOTEMPLATETYPE);
		combo.add(RNTYPE);
		combo.add(ODEsTYPE);
		combo.add(DAEsTYPE);
		combo.add(BNTYPE);
		combo.select(0);
		combo.setSize(2, combo.getSize().y);
		super.createAdvancedControls(new Composite(parent, 1));
	}
	
	public String getType(){
		return combo.getText();
	}
		
	@Override
	protected InputStream getInitialContents() {
		String modelName = getFileName();
		//if(modelName.endsWith(".crn")||modelName.endsWith(".ode")){
		if(modelName.endsWith(".ode")){
				modelName=modelName.substring(0, modelName.length()-4);
		}
		modelName= GUICRNImporter.getOnlyAlphaNumeric(modelName);//.replace('-', '_').replace("(", "").replace(")", "");
		if(!getType().equals(WizardNewERODEFileCreationPage.NOTEMPLATETYPE)){
			String preDefModel = getPredefModel(getType(), modelName);
			return new ByteArrayInputStream(preDefModel.getBytes());
		}
		else{
			return null;
		}
	}

	
	/*public static String getOnlyDigits(String s) {
	    Pattern pattern = Pattern.compile("[^0-9]");
	    Matcher matcher = pattern.matcher(s);
	    String number = matcher.replaceAll("");
	    return number;
	 }
	 public static String getOnlyStrings(String s) {
	    Pattern pattern = Pattern.compile("[^a-z A-Z]");
	    Matcher matcher = pattern.matcher(s);
	    String number = matcher.replaceAll("");
	    return number;
	 }*/
	
	private String getPredefModel(String type, String modelName) {
		StringBuilder sb = new StringBuilder();
		if(type.equals(BNTYPE)){
			sb.append("begin Boolean network ");
			sb.append(modelName);
			sb.append("\n");
			
			sb.append(" /*\n"+
					"  * List of variables (or species) and their initial Boolean value. Each variable is specified as:\n"+
					"  * 	variableName [= IC]\n"+
					"  * with \"IC\" being either true or false.\n"+ 
					"  * IC is optional, with default value false\n"+   
					"  */\n"+
					" begin init\n"+
					"  //s1 = true\n"+
					"  //s2\n"+
					" end init\n");

			sb.append(" /*\n"+
					"  * An optional partition of species. It can be used as initial partition when applying any of the supported reductions. \n"+
					"  * Unspecified species belong to a default block. \n"+   
					"  */\n"+
					" begin partition\n"+
					"  //{s1}\n"+
					" end partition\n");
			
			sb.append("\n"); 
			sb.append(" begin update functions\n");
			sb.append(" //s1 = s1 | s2\n");
			sb.append(" //If you have a boolfunctions specification, just copy it here\n");
			sb.append(" end update functions\n");
			
			sb.append("end Boolean network");
		}
		else{
			sb.append("begin model ");
			sb.append(modelName);
			sb.append("\n");
			sb.append(" /*\n");
			sb.append("  * Optional list of parameters. Each parameter is specified as:\n");
			sb.append("  * \t parameterName = expression\n");
			sb.append("  * with \"expression\" being an arithmetic expression of reals and parameters.\n"); 
			sb.append("  * Supported arithmetic operations: +, -, *, /, ^, max, min, abs\n");
			sb.append("  */\n");
			sb.append(" begin parameters\n");
			sb.append("  p1 = 1.0\n");
			sb.append("  p2 = 2*p1\n");
			sb.append(" end parameters\n");

			if(type.equals(WizardNewERODEFileCreationPage.RNTYPE)||type.equals(WizardNewERODEFileCreationPage.ODEsTYPE)){
				sb.append(" /*\n"+
						"  * List of variables (or species) and their initial concentration. Each variable is specified as:\n"+
						"  * 	variableName [= IC]\n"+
						"  * with \"IC\" being an arithmetic expression of reals and parameters.\n"+ 
						"  * IC is optional, with default value 0\n"+   
						"  */\n"+
						" begin init\n"+
						"  s1 = 1\n"+ 
						"  s2 = 2.0\n"+
						"  s3\n"+
						" end init\n");
			}
			else {
				sb.append(" /*\n"+
						"  * List of variables (or species) and their initial concentration. Each variable is specified as:\n"+
						"  * 	variableName [= IC]\n"+
						"  * with \"IC\" being an arithmetic expression of reals and parameters.\n"+ 
						"  * IC is optional, with default value 0\n"+   
						"  */\n"+
						" begin init\n"+
						"  s1 = 1\n"+ 
						"  s2 = 2.0\n"+
						" end init\n");
				sb.append(" /*\n"+
						"  * List of algebraic variables (or species) and their initial concentration. Each algebraic variable is specified as:\n"+
						"  * 	variableName [= IC]\n"+
						"  * with \"IC\" being an arithmetic expression of reals and parameters.\n"+ 
						"  * IC is optional, with default value 0\n"+   
						"  */\n"+
						" begin alginit\n"+
						"  s3\n"+
						" end alginit\n");
			}

			sb.append(" /*\n"+
					"  * An optional partition of species. It can be used as initial partition when applying any of the supported reductions. \n"+
					"  * Unspecified species belong to a default block. \n"+   
					"  */\n"+
					" begin partition\n"+
					"  {s1,s2}\n"+ 
					" end partition\n");

			if(type.equals(WizardNewERODEFileCreationPage.RNTYPE)){
				sb.append(	"/*\n"+
						" * List of reactions. Each reaction is specified as:\n"+ 
						" * 	reagents -> products , rate\n"+
						" * where \"reagents\" and \"products\" are multisets of variables, while \"rate\" can be \n"+
						" * 	1) an arithmetic expression of reals and parameters. In which case we have a mass action reaction, and the rate is its kinetic constant\n"+
						" * 	2) \"arbitrary\", followed by an arithmetic expression of reals, parameters and variables. The rate is the actual firing rate of this arbitrary reaction\n"+
						" * 	3) \"Hill\", followed by three real parameters K, R1, and R2, and two naturals n1 and n2. This is a reaction with Hill kinetics.\n"+ 
						" * 	 \n"+
						" */\n"+
						"begin reactions\n"+
						" s1 + s2 -> s3 + s2 , p1\n"+  
						"end reactions\n");
			}
			else if(type.equals(WizardNewERODEFileCreationPage.ODEsTYPE)) {
				sb.append(
						" /*\n"+
								"  * List of ODEs. Each ODE is specified as:\n"+ 
								"  * d(variable) = drift\n"+
								"  * where \"drift\" is an arithmetic expression of reals, parameters and variables.\n"+
								"  */\n"
						);
				sb.append(	" begin ODE\n"+
						"  d(s1) = -s1*s2*p1\n"+ 
						"  d(s2) = 0\n"+
						"  d(s3) =  s1*s2*p1\n"+
						" end ODE\n");
			}
			else if(type.equals(WizardNewERODEFileCreationPage.DAEsTYPE)) {
				sb.append(
						" /*\n"+
								"  * List of ODEs. Each ODE is specified as:\n"+ 
								"  * d(variable) = drift\n"+
								"  * where \"drift\" is an arithmetic expression of reals, parameters and variables.\n"+
								"  */\n"
						);
				sb.append(	" begin ODE\n"+
						"  d(s1) = -s1*s2*p1\n"+ 
						"  d(s2) = 0\n"+
						" end ODE\n");
				
				sb.append(
						" /*\n"+
								"  * List of algebraic constraints for the algebraic variables. Each constraint is specified as:\n"+ 
								"  * variable = constraint\n"+
								"  * where \"constraint\" is an arithmetic expression of reals, parameters and variables.\n"+
								"  */\n"
						);
				sb.append(	" begin algebraic\n"+
						"  s3 = 1 - s1\n"+ 
						" end algebraic\n");
			}
			sb.append(
					" /*\n"+
							" * Optional list of views (or observables) to be plot. Each view is given in a line of this form:\n"+
							" * 	 viewName = value\n"+
							" * with value being an arithmetic expression of reals, parameters and variables.\n"+ 
							//" * The special operations var(variable) and covar(variable) are also supported, which tell the ODE solver to compute also the (co)variance of that variable, rather than just its mean\n"+  
					" */\n");
			sb.append(	" begin views\n"+
					"  v1 = s1 + s2\n"+
					" end views\n"
					);
			sb.append(
					" /*\n"+
							" * A number of commands can be provided, including:\n"+
							" *  simulateODE and simulateDAE, to simulate the model and plot the result \n"+
							" *  reduceBDE and reduceBE, to reduce the model according to Backward Equivalence using an SMT-based or a Bisimulation-based algorithm, respectively \n"+
							" *  reduceFDE and reduceFE, to reduce the model according to Forward Equivalence using an SMT-based or a Bisimulation-based algorithm, respectively. \n"+
							" * Reductions can be preceded by \"this=\", in which case the model is replaced with its reduction.\n"+
					" */\n");
			
			if(type.equals(WizardNewERODEFileCreationPage.DAEsTYPE)) {
				sb.append(" simulateDAE(tEnd=1.0 , viewPlot=VARS&VIEWS , viewLabels=true)\n");
				sb.append(" reduceBDE(reducedFile=\""+modelName+"BDE.ode\")\n");
				sb.append(" reduceBE(reducedFile=\""+modelName+"BE.ode\")\n");
			}
			else {
				sb.append(" simulateODE(tEnd=1.0 , viewPlot=VARS&VIEWS , viewLabels=true , library=SUNDIALS)\n");
				sb.append(" simulateODE(tEnd=1.0 , viewPlot=VARS&VIEWS , viewLabels=true , library=APACHE)\n");
				sb.append(" reduceBDE(reducedFile=\""+modelName+"BDE.ode\")\n");
				sb.append(" reduceFDE(reducedFile=\""+modelName+"FDE.ode\")\n");
			}
			if(type.equals(WizardNewERODEFileCreationPage.RNTYPE)){
				sb.append(" reduceBE(reducedFile=\""+modelName+"BE.ode\")\n");
				sb.append(" reduceFE(reducedFile=\""+modelName+"FE.ode\")\n");
			}
			/*sb.append(" simulateODE(tEnd=1.0 viewPlot=true viewLabels=true)\n");*/
			sb.append("end model");
		}
		
		return sb.toString();
	}
	
	/*private String getPredefModel(String type, String modelName) {
		StringBuilder sb = new StringBuilder();
		sb.append("begin model ");
		sb.append(modelName);
		sb.append("\n");
		sb.append(" /*\n");
		sb.append("  * Optional list of parameters. Each parameter is specified as:\n");
		sb.append("  * \t name = expression\n");
		sb.append("  * with \"expression\" being an arithmetic expression of reals or parameters.\n"); 
		sb.append("  * Supported arithmetic operations: +, -, *, /, ^, max, min, abs\n");
		sb.append("  *\n");
		sb.append(" begin parameters\n");
		sb.append("  p1 = 1.0\n");
		sb.append("  p2 = 2*p1\n");
		sb.append(" end parameters\n");
		if(type.equals(WizardNewERODEFileCreationPage.RNTYPE)){
			sb.append(	" begin net\n"+
						"  /*\n"+
						"   * List of species. Each species is specified as:\n"+
						"   * 	speciesName [= IC]\n"+
						"   * with \"IC\" being an arithmetic expression of reals and parameters.\n"+ 
						"   * IC is the optional initial concentration of the species, with default value 0\n"+   
						"   *\n"+
						"  begin species\n"+
						"   s1 = 1\n"+ 
						"   s2 = 2.0\n"+
						"   s3\n"+
						" end species\n"+
						" /*\n"+
						"  * List of reactions. Each reaction is specified as:\n"+ 
						"  * 	reagents -> products , rate\n"+
						"  * where \"reagents\" and \"products\" are multisets of species, while \"rate\" can be \n"+
						"  * 	1) an arithmetic expression of reals and parameters. In which case we have a mass action reaction, and the rate is its kinetic constant\n"+
						"  * 	2) \"arbitrary\", followed by an arithmetic expression of reals, parameters and species. The rate is the actual firing rate of this arbitrary reaction\n"+
						"  * 	3) \"Hill\", followed by three real parameters K, R1, and R2, and two naturals n1 and n2. This is a reaction with Hill kinetics.\n"+ 
						"  * 	 \n"+
						"  *\n"+
						" begin reactions\n"+
						"  s1 + s2 -> s3 + s2 , p1\n"+  
						" end reactions\n"+
						"end net\n");
		}
		else if(type.equals(WizardNewERODEFileCreationPage.ODEsTYPE)) {
			sb.append(
					" /*\n"+
							"  * List of ODEs. Each ODE is specified as:\n"+ 
							"  * d(speciesName) = drift\n"+
							"  * where \"drift\" is an arithmetic expression of reals, parameters and species.\n"+
							"  *\n"
					);
			sb.append(	" begin ODE\n"+
						"  d(s1) = -s1*s2*p1\n"+ 
						"  d(s2) = 0\n"+
						"  d(s3) =  s1*s2*p1\n"+
						" /*\n"+
						"   * Optional list of initial concentrations. Each is specified as:\n"+
						"   * 	speciesName = IC\n"+
						"   * with \"IC\" being an arithmetic expression of reals and parameters.\n"+ 
						"   * IC is the initial concentration of the species. The default initial concentration is 0\n"+   
						"   *\n"+
						" begin initialConcentrations\n"+
						"  s1 = 1\n"+
						"  s2 = 2\n"+
						" end initialConcentrations\n"+
						"end ODE\n");
		}
		sb.append(
				" /*\n"+
				" * Optional list of views to be plot. Each view is given in a line of this form:\n"+
				" * 	 name = value\n"+
				" * with value being an arithmetic expression of reals, parameters or species.\n"+ 
				" * The special operations var(species) and covar(species) are also supported, which tell the solver to compute also the (co)variance of that species, rather than just its mean\n"+  
				" *\n");
		sb.append(	" begin views\n"+
					"  v1 = s1 + s2\n"+
					" end views\n"
				);
		sb.append(
				" /*\n"+
				" * A number of commands can be provided, including:\n"+
				" *  simulateODE, to simulate the model and plot the result \n"+
				" *  reduceBDE and reduceBB, to reduce the model according to Backward Differential Equivalence and Backward Bisimilarity \n"+
				" *  reduceBDE and reduceBB, to reduce the model according to Forward Differential Equivalence and Forward Bisimilarity \n"+
				" *\n");
		sb.append(" simulateODE({tEnd=1.0 , viewPlot=true , viewLabels=true})\n");
		sb.append(" this=reduceBDE({})\n");
		//sb.append(" simulateODE({tEnd=1.0 viewPlot=true viewLabels=true})\n");
		sb.append("end model");
		return sb.toString();
	}*/

}
