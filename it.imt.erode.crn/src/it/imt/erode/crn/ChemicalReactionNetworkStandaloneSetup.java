/*
 * generated by Xtext 2.20.0
 */
package it.imt.erode.crn;


/**
 * Initialization support for running Xtext languages without Equinox extension registry.
 */
public class ChemicalReactionNetworkStandaloneSetup extends ChemicalReactionNetworkStandaloneSetupGenerated {

	public static void doSetup() {
		new ChemicalReactionNetworkStandaloneSetup().createInjectorAndDoEMFRegistration();
	}
}
