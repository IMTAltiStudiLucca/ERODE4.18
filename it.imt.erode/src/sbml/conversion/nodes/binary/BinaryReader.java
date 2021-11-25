package sbml.conversion.nodes.binary;

import org.sbml.jsbml.ASTNode;

import it.imt.erode.booleannetwork.updatefunctions.FalseUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.NotBooleanUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.ReferenceToNodeUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.TrueUpdateFunction;
import it.imt.erode.crn.symbolic.constraints.BasicConstraintComparator;
import sbml.conversion.nodes.NodeManager;
import sbml.conversion.nodes.operators.ErodeOperator;

public class BinaryReader extends BinaryASTConverter {

    private ErodeOperator operator;

    public BinaryReader(ASTNode node,boolean mv) {
        super(node,mv);
        this.leftChild = NodeManager.create(currentNode.getChild(0),mv);
        this.rightChild = NodeManager.create(currentNode.getChild(1),mv);
        this.operator = new ErodeOperator(mv);
        this.convert();
    }

    
    
    @Override
    protected void convert() {
        ASTNode.Type type = currentNode.getType();
        IUpdateFunction l=leftChild.getUpdateFunction();
		IUpdateFunction r=rightChild.getUpdateFunction();
        switch (type) {
            case LOGICAL_AND:
                this.updateFunction = operator.and(l,r);
                break;
            case LOGICAL_OR:
                this.updateFunction = operator.or(l,r);
                break;
            case LOGICAL_XOR:
                this.updateFunction = operator.xor(l,r);
                break;
            case LOGICAL_IMPLIES:
                this.updateFunction = operator.implies(l,r);
                break;
            case RELATIONAL_EQ:
            	if(mv) {
            		this.updateFunction =operator.comparison(l, r, BasicConstraintComparator.EQ);
            		//this.updateFunction = operator.equals(l, r);
            	}
            	else {
            		if(l instanceof ReferenceToNodeUpdateFunction && r instanceof TrueUpdateFunction) {
            			this.updateFunction=l;
            		}
            		else if(r instanceof ReferenceToNodeUpdateFunction && l instanceof TrueUpdateFunction) {
            			this.updateFunction=r;
            		}
            		else if(r instanceof ReferenceToNodeUpdateFunction && l instanceof FalseUpdateFunction) {
            			this.updateFunction= new NotBooleanUpdateFunction(r);
            		} 
            		else if(l instanceof ReferenceToNodeUpdateFunction && r instanceof FalseUpdateFunction) {
            			this.updateFunction= new NotBooleanUpdateFunction(l);
            		}  
            		else {
            			this.updateFunction = operator.equals(l,r);
            		}
            	}
                break;
            case RELATIONAL_NEQ:
            	if(mv) {
            		//this.updateFunction = operator.notEquals(l,r);
            		this.updateFunction =operator.comparison(l, r, BasicConstraintComparator.NOTEQ);
            	}
            	else {
            		if(l instanceof ReferenceToNodeUpdateFunction && r instanceof FalseUpdateFunction) {
            			this.updateFunction=l;
            		}
            		else if(r instanceof ReferenceToNodeUpdateFunction && l instanceof FalseUpdateFunction) {
            			this.updateFunction=r;
            		}
            		else {
            			this.updateFunction = operator.notEquals(l,r);
            		}
            	}
                //this.updateFunction = operator.notEquals(leftChild.getUpdateFunction(), rightChild.getUpdateFunction());
                break;
            case RELATIONAL_GEQ:
            	if(mv) {
            		this.updateFunction = operator.comparison(l,r,BasicConstraintComparator.GEQ);
            	}
            	else {
            		this.updateFunction = operator.bn_comparison(l, r, BasicConstraintComparator.GEQ);
            		//throw new IllegalArgumentException("Comparisons are supported only for multivalued networks.");
            	}
                break;
            case RELATIONAL_GT:
            	if(mv) {
            		this.updateFunction = operator.comparison(l,r,BasicConstraintComparator.GT);
            	}
            	else {
            		this.updateFunction = operator.bn_comparison(l, r, BasicConstraintComparator.GT);
            		//throw new IllegalArgumentException("Comparisons are supported only for multivalued networks.");
            	}
                break;
            case RELATIONAL_LEQ:
            	if(mv) {
            		this.updateFunction = operator.comparison(l,r,BasicConstraintComparator.LEQ);
            	}
            	else {
            		this.updateFunction = operator.bn_comparison(l, r, BasicConstraintComparator.LEQ);
            		//throw new IllegalArgumentException("Comparisons are supported only for multivalued networks.");
            	}
                break;
            case RELATIONAL_LT:
            	if(mv) {
            		this.updateFunction = operator.comparison(l,r,BasicConstraintComparator.LT);
            	}
            	else {
            		this.updateFunction = operator.bn_comparison(l, r, BasicConstraintComparator.LT);
            		//throw new IllegalArgumentException("Comparisons are supported only for multivalued networks.");
            	}
                break;
            case SUM:
            	if(!mv) {
            		throw new IllegalArgumentException("Arithmetic expressions are supported only of multivalued networks");
            	}
            	this.updateFunction = operator.plus(l,r);
            	break;
            case PRODUCT:
            	if(!mv) {
            		throw new IllegalArgumentException("Arithmetic expressions are supported only of multivalued networks");
            	}
            	this.updateFunction = operator.product(l,r);
            	break;
            case TIMES:
            	if(!mv) {
            		throw new IllegalArgumentException("Arithmetic expressions are supported only of multivalued networks");
            	}
            	this.updateFunction = operator.product(l,r);
            	break;	
            default:
                throw new IllegalArgumentException("Invalid type name");
        }
    }
}
