package sbml.conversion.species;

import java.math.BigDecimal;

import it.imt.erode.crn.implementations.Species;
import it.imt.erode.crn.interfaces.ISpecies;

class ErodeSpeciesBuilder {


    public ISpecies createSpecies(int id, String name, int initialLevel) {
    	String startValue = String.valueOf(initialLevel);
        return new Species(name,id,new BigDecimal(startValue),startValue, false);
//        switch (initialLevel) {
//            case 0:
//                return new Species(name,id, BigDecimal.ZERO,"false", false);
//            case 1:
//                return new Species(name, id, BigDecimal.ONE, "true", false);
//            default:
//                throw new IllegalArgumentException("The value of the given species is outside the Boolean Domain");
//                //Example code for multi-valued species:
//                /*String startValue = String.valueOf(initialValue);
//                return new Species(species.getId(),id,new BigDecimal(startValue),startValue, false);*/
//        }
    }
    
    public void setBooleanICExprs(ISpecies sp) {
    	String icExpr="false";
    	BigDecimal ic=sp.getInitialConcentration();
        if(sp.getInitialConcentration().compareTo(BigDecimal.ONE)==0) {
            icExpr="true";
        }
        sp.setInitialConcentration(ic, icExpr);
    }
}
