package it.sssa.erode.or_tools;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

/** MIP example that solves an assignment problem. 
 * From https://developers.google.com/optimization/assignment/assignment_example
 * */
public class AssignmentMip {
  public static void main(String[] args) {
    Loader.loadNativeLibraries();
    // Data
    double[][] costs = {
        {90, 80, 75, 70},
        {35, 85, 55, 65},
        {125, 95, 90, 95},
        {45, 110, 95, 115},
        {50, 100, 90, 100},
    };
    int numWorkers = costs.length;
    int numTasks = costs[0].length;

    // Solver
    // Create the linear solver with the SCIP backend.
    MPSolver solver = MPSolver.createSolver("SCIP");
    if (solver == null) {
      System.out.println("Could not create solver SCIP");
      return;
    }

    // Variables
    // x[i][j] is an array of 0-1 variables, which will be 1
    // if worker i is assigned to task j.
    MPVariable[][] x = new MPVariable[numWorkers][numTasks];
    for (int i = 0; i < numWorkers; ++i) {
      for (int j = 0; j < numTasks; ++j) {
        x[i][j] = solver.makeIntVar(0, 1, "");
      }
    }

    // Constraints
    // Each worker is assigned to at most one task.
    for (int i = 0; i < numWorkers; ++i) {
      MPConstraint constraint = solver.makeConstraint(0, 1, "");
      for (int j = 0; j < numTasks; ++j) {
        constraint.setCoefficient(x[i][j], 1);
      }
    }
    // Each task is assigned to exactly one worker.
    for (int j = 0; j < numTasks; ++j) {
      MPConstraint constraint = solver.makeConstraint(1, 1, "");
      for (int i = 0; i < numWorkers; ++i) {
        constraint.setCoefficient(x[i][j], 1);
      }
    }

    // Objective
    MPObjective objective = solver.objective();
    for (int i = 0; i < numWorkers; ++i) {
      for (int j = 0; j < numTasks; ++j) {
        objective.setCoefficient(x[i][j], costs[i][j]);
      }
    }
    objective.setMinimization();

    // Solve
    MPSolver.ResultStatus resultStatus = solver.solve();

    // Print solution.
    // Check that the problem has a feasible solution.
    if (resultStatus == MPSolver.ResultStatus.OPTIMAL
        || resultStatus == MPSolver.ResultStatus.FEASIBLE) {
      System.out.println("Total cost: " + objective.value() + "\n");
      for (int i = 0; i < numWorkers; ++i) {
        for (int j = 0; j < numTasks; ++j) {
          // Test if x[i][j] is 0 or 1 (with tolerance for floating point
          // arithmetic).
          if (x[i][j].solutionValue() > 0.5) {
            System.out.println(
                "Worker " + i + " assigned to task " + j + ".  Cost = " + costs[i][j]);
          }
        }
      }
    } else {
      System.err.println("No solution found.");
    }
  }

  private AssignmentMip() {}
}