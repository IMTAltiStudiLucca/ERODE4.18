package it.imt.erode.crn.label;

import it.imt.erode.auxiliarydatastructures.IntegerAndSpecies;
import it.imt.erode.crn.implementations.Composite;
import it.imt.erode.crn.interfaces.ISpecies;

public class MutableNAryLabel implements ILabel {

	private int referredArity;
	//private Composite reagents;
	private int speciesToDecreaseMultiplicity;
	private ISpecies[] allSpecies;
	private int[] multiplicities;
	private int hc;

	public MutableNAryLabel(Composite reagents,int speciesToDecreaseMultiplicity, int referredArity) {
		//this.reagents=reagents;
		this.speciesToDecreaseMultiplicity=speciesToDecreaseMultiplicity;
		this.referredArity=referredArity;
		this.allSpecies=reagents.getAllSpecies();
		this.multiplicities=reagents.getMultiplicities();
		this.hc = computeHashCode();
	}

	
	public boolean isActuallyUnary() {
		return ((multiplicities.length==1 && multiplicities[0]==2) ||
				(multiplicities.length==2 && multiplicities[0]+multiplicities[1]==2) );
	}
	
	@Override
	public int getReferredArity() {
		return referredArity;
	}

	@Override
	public int getLabelID() {
		if(isActuallyUnary()){
			if(multiplicities.length==1 || (multiplicities.length==2 && speciesToDecreaseMultiplicity==1)){
				return allSpecies[0].getLabelID();
			} 
			else{
				return allSpecies[1].getLabelID();
			}
		}
		throw new UnsupportedOperationException("We can assign a label only to actually unary labels (i.e., which refer to binary reactions)");
	}
	
	public int getLabelIDOfFirstSpecies() {
		if(speciesToDecreaseMultiplicity==0 && multiplicities[0]==1){
			return allSpecies[1].getLabelID();
		}
		else{
			return allSpecies[0].getLabelID();
		}
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if(((ILabel)obj).getReferredArity()!=getReferredArity()){
			return false;
		}
		if(obj instanceof ISpecies){
			if(isActuallyUnary()){
				if(multiplicities.length==1 || (multiplicities.length==2 && speciesToDecreaseMultiplicity==1)){
					return allSpecies[0].equals((ISpecies)obj);
				} 
				else{
					return allSpecies[1].equals((ISpecies)obj);
				}
			}
			else{
				return false;
			}
		}
		
		MutableNAryLabel other = (MutableNAryLabel) obj;
		if(hashCode()!=other.hashCode()){
			return false;
		}
		//return  equalArraysConsideringOnePossiblyDecreasedMultiplicity(other.allSpecies, other.multiplicities, other.speciesToDecreaseMultiplicity);
		return deepCompare(other)==0;
	}
	
	/*private boolean equalArraysConsideringOnePossiblyDecreasedMultiplicity(ISpecies[] otherAllSpecies,int[] otherMultiplicities, int otherSpeciesToDecreaseMultiplicity) {

		int myi = 0;
		int otheri=0;
		while(myi<allSpecies.length&&otheri<otherAllSpecies.length){
			int myRealMult = multiplicities[myi];
			if(speciesToDecreaseMultiplicity==myi){
				myRealMult--;
				if(myRealMult==0){
					myi++;
					if(myi>=multiplicities.length){
						myRealMult=0;
					}
					else{
						myRealMult = multiplicities[myi];
					}
				}
			}
			int otherRealMult = otherMultiplicities[otheri];
			if(otherSpeciesToDecreaseMultiplicity==otheri){
				otherRealMult--;
				if(otherRealMult==0){
					otheri++;
					if(otheri>=otherMultiplicities.length){
						otherRealMult=0;
					}
					else{
						otherRealMult = otherMultiplicities[otheri];
					}
				}
			}

			if(myRealMult != otherRealMult){
				return false;
			}
			//If they are both 0 I don't care about the species
			else if(myRealMult!=0 && ! allSpecies[myi].equals(otherAllSpecies[otheri])){
				return false;
			}

			myi++;
			otheri++;
		}

		if(myi>=allSpecies.length && otheri>=otherAllSpecies.length){
			return true;
		}
		else if(myi>=allSpecies.length && otheri==otherAllSpecies.length-1 && otherSpeciesToDecreaseMultiplicity==otherAllSpecies.length-1){
				return true;
		}
		else if(myi==allSpecies.length-1 && speciesToDecreaseMultiplicity == allSpecies.length-1 && otheri>=otherAllSpecies.length){
			return true;
		}
		return false;
	}*/
	
	public int deepCompare(MutableNAryLabel mnl2) {
		/*if(hashCode()==mnl2.hashCode()){
			if(this.equals(mnl2)){
				return 0;
			}
		}*/
		
		IntegerAndSpecies myMultAndSpecies=new IntegerAndSpecies();
		IntegerAndSpecies otherMultAndSpecies=new IntegerAndSpecies();
		int pos=0;
		
		do{
			getNextSpeciesAndMultiplicity(myMultAndSpecies,pos);
			mnl2.getNextSpeciesAndMultiplicity(otherMultAndSpecies, pos);
			
			if(myMultAndSpecies.getSpecies()!=null && otherMultAndSpecies.getSpecies()!=null){
				int cmp = Integer.compare(myMultAndSpecies.getSpecies().getLabelID(), otherMultAndSpecies.getSpecies().getLabelID());
				if(cmp!=0){
					return cmp;
				}
				else{
					int cmpMult = Integer.compare(myMultAndSpecies.getMultiplicity(), otherMultAndSpecies.getMultiplicity());
					if(cmpMult!=0){
						return cmpMult;
					}
					else{
						//The current entry is equal. We have to iterate to the next
						pos++;
					}
				}
			}
			else if(myMultAndSpecies.getSpecies()==null && otherMultAndSpecies.getSpecies()==null){
				//We are equal, because we have both finised the species
				return 0;
			}
			else if(myMultAndSpecies.getSpecies()==null){
				//I'm smaller because I do not have more species, while the other has.
				return -1;
			}
			else{
				//I'm bigger because I do have more species, while the other don't.
				return 1;
			}
		}while(myMultAndSpecies.getSpecies()!=null && otherMultAndSpecies.getSpecies()!=null);
		
		return 0;
	}

	
	private void getNextSpeciesAndMultiplicity(IntegerAndSpecies multAndSpecies, int pos) {
		
		//If I have  C+F, and C is the one to skip, then F is in position 0.
		//If I have 2C+F, and C is the one to skip, then F is in position 1.
		//If I have  C+F, and F is the one to skip, then F is in position -1 (it is not here).
		//If I have  C+F, and NF is the one to skip, then F is in position 1 and has multiplicity N-1
		
		if(speciesToDecreaseMultiplicity>pos){
			setSpeciesAndMultiplicitiesIfinsideTheBounds(multAndSpecies, pos);
		}
		else if(speciesToDecreaseMultiplicity<pos){
			if(multiplicities[speciesToDecreaseMultiplicity]==1){
				//The species to be decreased has multiplicity 1. Hence it has to be ignored. If I search for someone in pos i, he will actually be  in position i+1
				pos++;
			}
			setSpeciesAndMultiplicitiesIfinsideTheBounds(multAndSpecies, pos);			
		}
		else if(speciesToDecreaseMultiplicity==pos){
			if(multiplicities[speciesToDecreaseMultiplicity]==1){
				//The species to be decreased has multiplicity 1. Hence it has to be ignored. If I search for someone in pos i, he will actually be  in position i+1
				pos++;
				setSpeciesAndMultiplicitiesIfinsideTheBounds(multAndSpecies, pos);
			}
			else{
				//The species to be decreased has multiplicity at least 2. Hence it does not have to be ignored, but I decrease it multiplicity
				multAndSpecies.setSpecies(allSpecies[pos]);
				multAndSpecies.setMultiplicity(multiplicities[pos]-1);
			}
		}
	}


	private void setSpeciesAndMultiplicitiesIfinsideTheBounds(
			IntegerAndSpecies multAndSpecies, int pos) {
		if(pos>=allSpecies.length){
			multAndSpecies.setSpecies(null);
			multAndSpecies.setMultiplicity(-1);
		}
		else{
			multAndSpecies.setSpecies(allSpecies[pos]);
			multAndSpecies.setMultiplicity(multiplicities[pos]);
		}
	}


	@Override
	public int hashCode() {
		return hc;
	}

	private int computeHashCode() {
		int hashCode = 1;
		if(isActuallyUnary()){
			if(multiplicities.length==1 || (multiplicities.length==2 && speciesToDecreaseMultiplicity==1)){
				hashCode=allSpecies[0].hashCode();
			} 
			else{
				hashCode=allSpecies[1].hashCode();
			} 
		}
		else{
			final int prime = 31;
			hashCode = 1;

			int hashCodeAllSpecies=1;
			int hashCodeMultiplicities=1;
			
			for(int i=0;i<allSpecies.length;i++){
				if(i!=speciesToDecreaseMultiplicity){
					hashCodeAllSpecies= 31 * hashCodeAllSpecies + allSpecies[i].hashCode();
					hashCodeMultiplicities = 31 * hashCodeMultiplicities + multiplicities[i];
				}
				else { //if(i==speciesToDecreaseMultiplicity){
					if(multiplicities[i]>1){
						hashCodeAllSpecies= 31 * hashCodeAllSpecies + allSpecies[i].hashCode();
						hashCodeMultiplicities = 31 * hashCodeMultiplicities + multiplicities[i]-1;
					}
				}
				
			}

			hashCode = prime * hashCode + hashCodeAllSpecies; 
			hashCode = prime * hashCode + hashCodeMultiplicities;
		}
		
		return hashCode;
	}
	
	
	
	
	@Override
	public String toString() {
		
		if(allSpecies.length==0){
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<allSpecies.length;i++){
			int realMult = multiplicities[i];
			if(i==speciesToDecreaseMultiplicity){
				realMult--;
			}
			if(realMult==1){
				sb.append(allSpecies[i]);
				sb.append("+");
			}
			else {
				sb.append(realMult);
				sb.append(allSpecies[i]);
				sb.append("+");
			}
		}
		sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}

}
