package it.imt.erode.partition.implementations;

import java.util.Comparator;

import it.imt.erode.crn.implementations.CRNReaction;
import it.imt.erode.crn.implementations.CRNReactionArbitraryAbstract;
import it.imt.erode.crn.interfaces.ICRNReaction;

public class ICRNReactionComparatorToCollapse implements Comparator<ICRNReaction> {

	@Override
	public int compare(ICRNReaction o1, ICRNReaction o2) {
		if(o1 instanceof CRNReaction && o2 instanceof CRNReaction){
			if(o1.getRate()!=null && o2.getRate()!=null){
				return o1.compareTo(o2);
			}
			else{
				return compareUsingRateExpressionString(o1, o2);
			}
			
//			CRNReaction o1a = (CRNReaction)o1;
//			CRNReaction o2a = (CRNReaction)o2;
//			int cmp = o1a.compareReagentsAndProducts(o2a);
//			if(cmp!=0){
//				return cmp;
//			}
//			else{
//				//I don't care about the actual ordering of rates. We just want to sort according to the reagents and products, and then we sum the rates.
//				return o1a.getRateExpression().compareTo(o2a.getRateExpression());
//				cmp = Integer.compare(o1a.getRateExpression().length(), o2a.getRateExpression().length());
//				if(cmp!=0){
//					return cmp;
//				}
//				else{
//					return o1a.getRateExpression().compareTo(o2a.getRateExpression());
//				}
//				 
//			}
			
		}
		else if(o1 instanceof CRNReactionArbitraryAbstract && o2 instanceof CRNReactionArbitraryAbstract ){
			CRNReactionArbitraryAbstract o1a = (CRNReactionArbitraryAbstract)o1;
			CRNReactionArbitraryAbstract o2a = (CRNReactionArbitraryAbstract)o2;
			return compareUsingRateExpressionString(o1a, o2a);
		}
		else if(o1 instanceof CRNReactionArbitraryAbstract && ! (o2 instanceof CRNReactionArbitraryAbstract)){
			return 1;
		}
		else if((!(o1 instanceof CRNReactionArbitraryAbstract)) && o2 instanceof CRNReactionArbitraryAbstract){
			return -1;
		}
		else{
			throw new UnsupportedOperationException("Unsupported reactions: "+o1.toString()+" and "+o2.toString());
		}
	}

	private int compareUsingRateExpressionString(ICRNReaction o1, ICRNReaction o2) {
		int cmp = o1.compareReagentsAndProducts(o2);
		if(cmp!=0){
			return cmp;
		}
		else{
			//I don't care about the actual ordering of rates. We just want to sort according to the reagents and products, and then we sum the rates.
			return o1.getRateExpression().compareTo(o2.getRateExpression());
			/*cmp = Integer.compare(o1a.getRateExpression().length(), o2a.getRateExpression().length());
			if(cmp!=0){
				return cmp;
			}
			else{
				return o1a.getRateExpression().compareTo(o2a.getRateExpression());
			}*/
			 
		}
	}

}
