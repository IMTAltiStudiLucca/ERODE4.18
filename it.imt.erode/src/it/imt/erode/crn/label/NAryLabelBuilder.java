package it.imt.erode.crn.label;

import java.util.HashMap;

import it.imt.erode.crn.implementations.Composite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.partition.interfaces.IPartition;

public class NAryLabelBuilder {

	private Composite reagents;
	private int referredArity;
	private boolean isSetSpeciesToDecreaseMultiplicity=false;
	private int speciesToDecreaseMultiplicity=-1;

	public NAryLabelBuilder(Composite reagents) {
		this.reagents= reagents;
		this.referredArity=reagents.getTotalMultiplicity();
	}

	public void setSpeciesToDecrease(int posOfSpecies) {
		if(isSetSpeciesToDecreaseMultiplicity){
			throw new UnsupportedOperationException("Before setting the species whose multiplicity has to be decreased you have to reset the previous one");
		}
		else{
			isSetSpeciesToDecreaseMultiplicity=true;
			speciesToDecreaseMultiplicity=posOfSpecies;
			//reagents.getMultiplicities()[speciesToDecreaseMultiplicity]=reagents.getMultiplicities()[speciesToDecreaseMultiplicity]-1;
		}
	}
	
	public void resetSpeciesToDecrease() {
		if(!isSetSpeciesToDecreaseMultiplicity){
			throw new UnsupportedOperationException("You tried to reset the species whose multiplicity has to be decreased without having actually set one");
		}
		else{
			isSetSpeciesToDecreaseMultiplicity=false;
			//reagents.getMultiplicities()[speciesToDecreaseMultiplicity]=reagents.getMultiplicities()[speciesToDecreaseMultiplicity]+1;
			speciesToDecreaseMultiplicity=-1;
		}
	}

	public ILabel getObtainedLabel() {
		if(!isSetSpeciesToDecreaseMultiplicity){
			throw new UnsupportedOperationException("Before using nAry labels you have to set the species whose multiplicity you want to decrease by one");
		}
		else{
			return new MutableNAryLabel(reagents,speciesToDecreaseMultiplicity,referredArity);
		}
	}

	public ILabel getObtainedBlockLabel(IPartition partition) {
		if(!isSetSpeciesToDecreaseMultiplicity){
			throw new UnsupportedOperationException("Before using nAry labels you have to set the species whose multiplicity you want to decrease by one");
		}
		else{
			HashMap<ISpecies, Integer> blockLabelHM = new HashMap<ISpecies, Integer>(reagents.getAllSpecies().length); 
			for(int i=0;i<reagents.getAllSpecies().length;i++){
				ISpecies species = reagents.getAllSpecies()[i];
				int multiplicity = reagents.getMultiplicities()[i];
				if(speciesToDecreaseMultiplicity==i){
					multiplicity--;
				}
				if(multiplicity!=0){
					ISpecies speciesRep = partition.getBlockOf(species).getRepresentative();
					Integer prevMult = blockLabelHM.get(speciesRep);
					if(prevMult!=null){
						multiplicity=multiplicity+prevMult;
					}
					blockLabelHM.put(speciesRep, multiplicity);
				}
			}
			return new NAryBlockLabel(blockLabelHM,referredArity);
		}
	}

}
