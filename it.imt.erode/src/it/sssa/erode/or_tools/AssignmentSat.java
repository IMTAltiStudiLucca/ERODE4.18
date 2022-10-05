package it.sssa.erode.or_tools;

import com.google.ortools.Loader;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.LinearExprBuilder;
import com.google.ortools.sat.Literal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/** Assignment problem. 
 * From https://developers.google.com/optimization/assignment/assignment_example
 * It does not work
 * */
public class AssignmentSat {
  public static void main(String[] args) {
    Loader.loadNativeLibraries();
    // Data
    int[][] costs = {
        {90, 80, 75, 70},
        {35, 85, 55, 65},
        {125, 95, 90, 95},
        {45, 110, 95, 115},
        {50, 100, 90, 100},
    };
    final int numWorkers = costs.length;
    final int numTasks = costs[0].length;

    final int[] allWorkers = IntStream.range(0, numWorkers).toArray();
    final int[] allTasks = IntStream.range(0, numTasks).toArray();

    // Model
    CpModel model = new CpModel();

    // Variables
    Literal[][] x = new Literal[numWorkers][numTasks];
    for (int worker : allWorkers) {
      for (int task : allTasks) {
        x[worker][task] = model.newBoolVar("x[" + worker + "," + task + "]");
      }
    }

    // Constraints
    // Each worker is assigned to at most one task.
    for (int worker : allWorkers) {
      List<Literal> tasks = new ArrayList<>();
      for (int task : allTasks) {
        tasks.add(x[worker][task]);
      }
      model.addAtMostOne(tasks);
    }

    // Each task is assigned to exactly one worker.
    for (int task : allTasks) {
      List<Literal> workers = new ArrayList<>();
      for (int worker : allWorkers) {
        workers.add(x[worker][task]);
      }
      model.addExactlyOne(workers);
    }

    // Objective
    LinearExprBuilder obj = LinearExpr.newBuilder();
    for (int worker : allWorkers) {
      for (int task : allTasks) {
        obj.addTerm(x[worker][task], costs[worker][task]);
      }
    }
    model.minimize(obj);

    // Solve
    CpSolver solver = new CpSolver();
    CpSolverStatus status = solver.solve(model);

    // Print solution.
    // Check that the problem has a feasible solution.
    if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
      System.out.println("Total cost: " + solver.objectiveValue() + "\n");
      for (int i = 0; i < numWorkers; ++i) {
        for (int j = 0; j < numTasks; ++j) {
          if (solver.booleanValue(x[i][j])) {
            System.out.println(
                "Worker " + i + " assigned to task " + j + ".  Cost: " + costs[i][j]);
          }
        }
      }
    } else {
      System.err.println("No solution found.");
    }
  }

  private AssignmentSat() {}
}