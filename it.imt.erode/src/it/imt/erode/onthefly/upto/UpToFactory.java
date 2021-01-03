package it.imt.erode.onthefly.upto;

public class UpToFactory {

	public static IUpToMembershipChecker buildUpToMembershipChecker(UpToType upTo){
		switch (upTo) {
		case NO:
			return new IdentityUpToMembershipChecker();
			
		case Reflexivity:
			return new ReflexivityUpToMembershipChecker();
		case Symmetry:
			return new SymmetryUpToMembershipChecker();
		case Transitivity:
			return new TransitiviyUpToMembershipChecker();
			
		case ReflexivitySymmetry:
			return new ReflexivitySymmetryUpToMembershipChecker();
		
		case Equivalence:
			return new EquivalenceUpToMembershipChecker();
		
		default:
			throw new UnsupportedOperationException("Upto type "+upTo+" not supported.");
		}
	}
	
	public static UpToType stringToUpToType(String s) {
		for(UpToType upTo : UpToType.values()) {
			if(upTo.name().equalsIgnoreCase(s)) {
				return upTo;
			}
		}
		return null;
	}
	
}
