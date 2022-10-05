package or_tools;
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
    Loader.loadNativeLibraries();
    // Create the linear solver with the GLOP backend.
    MPSolver solver = MPSolver.createSolver("GLOP");

    // Create the variables x and y.
    MPVariable x = solver.makeNumVar(0.0, 1.0, "x");
    MPVariable y = solver.makeNumVar(0.0, 2.0, "y");

    System.out.println("Number of variables = " + solver.numVariables());

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
   
    

    System.out.println("Number of constraints = " + solver.numConstraints());

    // Create the objective function, 3 * x + y.
    MPObjective objective = solver.objective();
    objective.setCoefficient(x, 3);
    objective.setCoefficient(y, 1);
    objective.setMaximization();
    //objective.setMinimization();

    solver.solve();

    System.out.println("Solution:");
    System.out.println("Objective value = " + objective.value());
    System.out.println("x = " + x.solutionValue());
    System.out.println("y = " + y.solutionValue());
  }

  private BasicExample() {}
}