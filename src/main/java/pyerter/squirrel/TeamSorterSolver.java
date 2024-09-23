package pyerter.squirrel;

import com.google.ortools.Loader;
import com.google.ortools.init.OrToolsVersion;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

import java.util.Arrays;

public class TeamSorterSolver {

    public static final double INFINITY = Double.POSITIVE_INFINITY;
    public static final double NEGATIVE_INFINITY = Double.NEGATIVE_INFINITY;

    protected TeamSortingInput input;

    public TeamSorterSolver(TeamSortingInput input) {
        this.input = input;
    }

    public TeamSortingInput getInput() {
        return input;
    }

    public void setInput(TeamSortingInput input) {
        this.input = input;
    }

    public void solve() {
        Loader.loadNativeLibraries();

        System.out.println("Google OR-Tools version: " + OrToolsVersion.getVersionString());

        // Create the linear solver with the GLOP backend.
        MPSolver solver = MPSolver.createSolver("GLOP");
        if (solver == null) {
            System.out.println("Could not create solver GLOP");
            return;
        }

        // Create the variable matrix
        MPVariable[][] vars = createVariables(solver);

        // Create the constraints
        MPConstraint[] constraint1 = createConstraint1TeamRoleColumnSums(solver, vars);
        MPConstraint[] constraint2 = createConstraint2MemberRowSums(solver, vars);
        MPConstraint[] constraint3 = createConstraint3MemberRoleCapabilities(solver, vars);

        // Create the objective function
        MPObjective objective = createObjectiveFunction(solver, vars, input.getPreferenceMultipliers());
        objective.setMaximization(); // we maximize it

        System.out.println("Solving with " + solver.solverVersion());
        final MPSolver.ResultStatus resultStatus = solver.solve();

        System.out.println("Status: " + resultStatus);
        if (resultStatus != MPSolver.ResultStatus.OPTIMAL) {
            System.out.println("The problem does not have an optimal solution!");
            if (resultStatus == MPSolver.ResultStatus.FEASIBLE) {
                System.out.println("A potentially suboptimal solution was found");
            } else {
                System.out.println("The solver could not solve the problem.");
                return;
            }
        }

        System.out.println("Solution:");
        System.out.println("Objective value = " + objective.value());

        MemberAssignment[] memberAssignments = new MemberAssignment[input.numbRows()];
        for (int i = 0; i < memberAssignments.length; i++) {
            memberAssignments[i] = new MemberAssignment(input, i, vars[i]);
            System.out.println(memberAssignments[i].toPrintAssignment());
        }

        System.out.println("Advanced usage:");
        System.out.println("Problem solved in " + solver.wallTime() + " milliseconds");
        System.out.println("Problem solved in " + solver.iterations() + " iterations");
    }

    protected MPVariable[][] createVariables(MPSolver solver) {
        int rows = input.numbMembers();
        int cols = input.numbAugmentedColumns();
        MPVariable[][] vars = new MPVariable[rows][];
        for (int row = 0; row < rows; row++) {
            vars[row] = solver.makeNumVarArray(cols, 0, 1);
        }
        return vars;
    }

    protected MPConstraint[] createConstraint1TeamRoleColumnSums(MPSolver solver, MPVariable[][] vars) {
        MPConstraint[] constraints = new MPConstraint[input.numbAugmentedColumns()];
        int constraintIndex = 0;
        // Create constraints for all columns
        int beginExtraColumns = constraintIndex;
        while (constraintIndex < constraints.length) {
            // get team role mapping if it exists
            int t = input.getJToTeam(constraintIndex);
            int r = input.getJToRole(constraintIndex);
            MPConstraint constraint;
            if (t < 0 || r < 0)
                constraint = solver.makeConstraint(0, 1, String.format("colsumextra%dcolumn%d", constraintIndex - beginExtraColumns, constraintIndex));
            else
                constraint = solver.makeConstraint(0, 1, String.format("colsumteam%drole%dcolumn%d", t, r, constraintIndex));
            constraints[constraintIndex] = constraint;
            forEach(getColumns(vars, constraintIndex), (v, i, j) -> {
                constraint.setCoefficient(v, 1);
            });
            constraintIndex++;
        }
        return constraints;
    }

    protected MPConstraint[] createConstraint2MemberRowSums(MPSolver solver, MPVariable[][] vars) {
        MPConstraint[] constraints = new MPConstraint[input.numbRows()];
        for (int m = 0; m < constraints.length; m++) {
            MPConstraint constraint = solver.makeConstraint(0, 1, String.format("rowsummember%drow%d", m, m));;
            constraints[m] = constraint;
            forEach(getRows(vars, m), (v, i, j) -> {
                constraint.setCoefficient(v, 1);
            });
        }
        return constraints;
    }

    protected MPConstraint[] createConstraint3MemberRoleCapabilities(MPSolver solver, MPVariable[][] vars) {
        MPConstraint[] constraints = new MPConstraint[input.numbRows()];
        for (int m = 0; m < constraints.length; m++) {
            MPConstraint constraint = solver.makeConstraint(NEGATIVE_INFINITY, 0, String.format("membercapableroles%d", m));
            constraints[m] = constraint;
            int[] memberRoles = input.getMemberRoles()[m];
            forEach(getRows(vars, m), (v, i, j) -> {
                int role = input.getJToRole(j);
                boolean contained = false;
                for (int r = 0; r < memberRoles.length; r++) {
                    if (memberRoles[r] == role) {
                        contained = true;
                        break;
                    }
                }
                if (!contained) {
                    constraint.setCoefficient(v, 1);
                }
            });
        }
        return constraints;
    }

    protected MPObjective createObjectiveFunction(MPSolver solver, MPVariable[][] vars, int ... prefValues) {
        MPObjective objective = solver.objective();
        forEach(vars, (v, i, j) -> {
            Member m = input.getMember(i);
            String[] preferences = m.getPreferredTeams();
            int teamCol = input.getJToTeam(j);
            for (int p = 0; p < preferences.length; p++) {
                Integer currentPref = input.getTeamMap().get(preferences[p]);
                if (currentPref != null && currentPref == teamCol) {
                    if (p < prefValues.length)
                        objective.setCoefficient(v, prefValues[p]);
                    break;
                }
            }
        });
        return objective;
    }

    public MPVariable[][] getColumns(MPVariable[][] vars, int ... cols) {
        MPVariable[][] slice = new MPVariable[vars.length][];
        for (int i = 0; i < slice.length; i++) {
            slice[i] = new MPVariable[cols.length];
            for (int j = 0; j < cols.length; j++) {
                int targetCol = cols[j];
                slice[i][j] = vars[i][targetCol];
            }
        }
        return slice;
    }

    public MPVariable[][] getRows(MPVariable[][] vars, int ... rows) {
        MPVariable[][] slice = new MPVariable[rows.length][];
        for (int i = 0; i < slice.length; i++) {
            slice[i] = vars[rows[i]];
        }
        return slice;
    }

    public void forEach(MPVariable[][] vars, MatrixVariableConsumer consumer) {
        for (int i = 0; i < vars.length; i++) {
            for (int j = 0; j < vars[i].length; j++) {
                consumer.consume(vars[i][j], i, j);
            }
        }
    }

    public interface MatrixVariableConsumer {
        public void consume(MPVariable var, int i, int j);
    }

    public static class MemberAssignment {
        protected int member;
        protected Member m;
        protected double[] teamAssignments;
        protected int[] assignedTeams;
        protected String[] assignedTeamNames;

        public MemberAssignment(TeamSortingInput input, int member, MPVariable[] memberRow) {
            this.member = member;
            m = input.getMember(member);
            teamAssignments = new double[memberRow.length];
            int numbAssignedTeams = 0;
            for (int i = 0; i < memberRow.length; i++) {
                teamAssignments[i] = memberRow[i].solutionValue();
                if (teamAssignments[i] != 0)
                    numbAssignedTeams++;
            }
            assignedTeams = new int[numbAssignedTeams];
            assignedTeamNames = new String[numbAssignedTeams];
            int current = 0;
            for (int i = 0; i < teamAssignments.length; i++) {
                if (teamAssignments[i] != 0) {
                    assignedTeams[current] = i;
                    assignedTeamNames[current] = input.getTeams()[i];
                    current++;
                }
            }
        }

        public int getMemberIndex() {
            return member;
        }

        public double[] getValues() {
            return teamAssignments;
        }

        public double getValue(int col) {
            return teamAssignments[col];
        }

        public int[] getAssignedTeams() {
            return assignedTeams;
        }

        public Member getMember() {
            return m;
        }

        public String toPrintAssignment() {
            if (assignedTeams.length == 0) return String.format("Member %s (%d) assigned to no team!?", m.getName(), member);
            if (assignedTeams.length == 1) return String.format("Member %s (%d) assigned to team %s (%d) with value %f", m.getName(), member, assignedTeamNames[0], assignedTeams[0], teamAssignments[assignedTeams[0]]);
            String result = String.format("Member %s (%d) assigned to teams: ", m.getName(), member);
            String[] teams = new String[teamAssignments.length];
            for (int i = 0; i < teams.length; i++) {
                teams[i] = String.format("%s (%d) with value %f", assignedTeamNames[i], assignedTeams[i], teamAssignments[assignedTeams[i]]);
            }
            result += Arrays.toString(teams);
            return result;
        }
    }

}
