package sbml.conversion.nodes.operators;

import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.ASTNode.Type;

import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.crn.symbolic.constraints.BasicConstraintComparator;

public class SBMLOperator implements IOperator<ASTNode> {

    private ASTNodeBuilder builder;

    public SBMLOperator() {
        this.builder = new ASTNodeBuilder();
    }

    @Override
    public ASTNode not(ASTNode x) {
        return builder.unary(x,ASTNode.Type.LOGICAL_NOT);
    }

    @Override
    public ASTNode and(ASTNode x, ASTNode y) {
        return builder.binary(x,y,ASTNode.Type.LOGICAL_AND);
    }

    @Override
    public ASTNode or(ASTNode x, ASTNode y) {
        return builder.binary(x,y,ASTNode.Type.LOGICAL_OR);
    }

    @Override
    public ASTNode xor(ASTNode x, ASTNode y) {
        return builder.binary(x,y,ASTNode.Type.LOGICAL_XOR);
    }

    @Override
    public ASTNode implies(ASTNode x, ASTNode y) {
        return builder.binary(x,y,ASTNode.Type.LOGICAL_IMPLIES);
    }

    @Override
    public ASTNode equals(ASTNode x, ASTNode y) {
        return builder.binary(x,y,ASTNode.Type.RELATIONAL_EQ);
    }

    @Override
    public ASTNode notEquals(ASTNode x, ASTNode y) {
        return builder.binary(x,y,ASTNode.Type.RELATIONAL_NEQ);
    }

	@Override
	public ASTNode plus(ASTNode x, ASTNode y) {
		return builder.binary(x,y,ASTNode.Type.SUM);
	}

	@Override
	public ASTNode product(ASTNode x, ASTNode y) {
		return builder.binary(x,y,ASTNode.Type.PRODUCT);
	}

	@Override
	public ASTNode comparison(ASTNode x, ASTNode y, BasicConstraintComparator cmp) {
		return builder.binary(x,y,toType(cmp));
	}

	public static Type toType(BasicConstraintComparator cmp) {
		switch (cmp) {
		case GEQ:
			return ASTNode.Type.RELATIONAL_GEQ;
		case GT:
			return ASTNode.Type.RELATIONAL_GT;
		case LEQ:
			return ASTNode.Type.RELATIONAL_LEQ;
		case LT:
			return ASTNode.Type.RELATIONAL_LT;
		default:
			return null;
		}
	}

	@Override
	public IUpdateFunction bn_comparison(IUpdateFunction l, IUpdateFunction r, BasicConstraintComparator cmp) {
		throw new UnsupportedOperationException("bn_comparison not implemented for SBMLOperator");
	}
}
