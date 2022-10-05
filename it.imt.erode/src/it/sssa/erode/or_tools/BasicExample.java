package it.sssa.erode.or_tools;
/**
 * Based on https://developers.google.com/optimization/introduction/java
 * @param args
 */

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

/** Minimal Linear Programming example to showcase calling the solver. */
public final class BasicExample {
	public static void main(String[] args) {
		myBasicExample();
	}

	public static String myBasicExample() {
		StringBuffer sb = new StringBuffer();  
		sb.append("\n\n########################");
		sb.append("\n");
		sb.append(    "#BASIC EXAMPLE OR-TOOLS#");
		sb.append("\n");
		sb.append(    "########################");
		sb.append("\n");
		Loader.loadNativeLibraries();
		// Create the linear solver with the GLOP backend.
		MPSolver solver = MPSolver.createSolver("GLOP");

		// Create the variables x and y.
		MPVariable x = solver.makeNumVar(0.0, 1.0, "x");
		MPVariable y = solver.makeNumVar(0.0, 2.0, "y");

		sb.append("Number of variables = " + solver.numVariables());
		sb.append("\n");

		// Create a linear constraint, 0 <= x + y <= 2.
		MPConstraint ct = solver.makeConstraint(0.0, 2.0, "ct");
		ct.setCoefficient(x, 1);
		ct.setCoefficient(y, 1);

		// Andrea: x >= y: Create a linear constraint, 0.0 <= x - y.
		double infinity = java.lang.Double.POSITIVE_INFINITY;
		MPConstraint xgty = solver.makeConstraint(0.0, infinity, "xgty");
		xgty.setCoefficient(x, 1);
		xgty.setCoefficient(y, -1);

		// Andrea: x >= y +0.1: Create a linear constraint, 0.1 <= x - y.
		MPConstraint xgty2 = solver.makeConstraint(0.1, infinity, "xgty2");
		xgty2.setCoefficient(x, 1);
		xgty2.setCoefficient(y, -1);



		sb.append("Number of constraints = " + solver.numConstraints());
		sb.append("\n");

		// Create the objective function, 3 * x + y.
		MPObjective objective = solver.objective();
		objective.setCoefficient(x, 3);
		objective.setCoefficient(y, 1);
		objective.setMaximization();
		//objective.setMinimization();

		solver.solve();


		sb.append("Solution:\n");
		sb.append("Objective value = " + objective.value()+"\n");
		sb.append("x = " + x.solutionValue()+"\n");
		sb.append("y = " + y.solutionValue()+"\n");
		return sb.toString();
	}

	private BasicExample() {}
}