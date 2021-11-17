package sbml.conversion.species;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;

import it.imt.erode.crn.interfaces.ISpecies;
import sbml.configurations.SBMLConfiguration;

abstract class SpeciesConverter implements ISpeciesConverter {
    protected static final SBMLConfiguration CONFIG = SBMLConfiguration.getConfiguration();

    protected ListOf<QualitativeSpecies> sbmlSpecies;
    protected List<ISpecies> erodeSpecies;
    protected HashMap<String,Integer> maxValues;
    protected LinkedHashSet<String> constantSpecies;
    protected boolean multivalued=false;

    public SpeciesConverter(@NotNull ListOf<QualitativeSpecies> listOfQualitativeSpecies) {
        this.sbmlSpecies = listOfQualitativeSpecies;
    }

    public SpeciesConverter(@NotNull List<ISpecies> species) {
        this.erodeSpecies = species;
    }

    @Override
    public List<ISpecies> getErodeSpecies() {
        return this.erodeSpecies;
    }
    
    @Override
    public HashMap<String,Integer> getMaxValues() {
        return maxValues;
    }
    @Override
	public HashSet<String> getConstantSpecies() {
		return constantSpecies;
	}
    
    protected void setMaxValue(String name,int m) {
    	if(m>1) {
    		multivalued=true;
    	}
    	maxValues.put(name,m);
    }
    
    

    @Override
    public ListOf<QualitativeSpecies> getSbmlSpecies() {
        return sbmlSpecies;
    }
    
    @Override
    public boolean isMultiValued() {
        return multivalued;
    }
}
