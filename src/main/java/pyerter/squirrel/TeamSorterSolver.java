package pyerter.squirrel;

import com.google.ortools.Loader;
import com.google.ortools.init.OrToolsVersion;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

import java.util.Arrays;

/**
 * Author: Porter Squires
 * License: MIT License
 *
 *
 */
public class TeamSorterSolver {

    public static final double INFINITY = Double.POSITIVE_INFINITY;
    public static final double NEGATIVE_INFINITY = Double.NEGATIVE_INFINITY;

    protected TeamSortingInput input;

    protected boolean detailedPrinting = false;

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
        int[] constraintCounts = new int[5];
        MPConstraint[] constraint1 = createConstraint1TeamRoleColumnSums(solver, vars);
        MPConstraint[] constraint2 = createConstraint2MemberRowSums(solver, vars);
        MPConstraint[] constraint3 = createConstraint3MemberRoleCapabilities(solver, vars);
        MPConstraint[] constraint4 = createConstraint4TeamSizeRequirements(solver, vars);
        MPConstraint[][] constraint5 = createConstraint5FriendshipRequirements(solver, vars);
        constraintCounts[0] = constraint1.length;
        constraintCounts[1] = constraint2.length;
        constraintCounts[2] = constraint3.length;
        constraintCounts[3] = constraint4.length;
        constraintCounts[4] = 0;
        for (int i = 0; i < constraint5.length; i++)
            constraintCounts[4] += constraint5[i].length;
        System.out.printf("Created %d constraints for column sums.%n", constraintCounts[0]);
        System.out.printf("Created %d constraints for row sums.%n", constraintCounts[1]);
        System.out.printf("Created %d constraints for member role assignments.%n", constraintCounts[2]);
        System.out.printf("Created %d constraints for team size requirements.%n", constraintCounts[3]);
        System.out.printf("Created %d constraints for friendship requirements.%n", constraintCounts[4]);

        // Create the objective function
        int[] preferenceMultipliers = new int[input.getNumbPreferences()];
        for (int i = 0; i < preferenceMultipliers.length; i++) {
            int val = preferenceMultipliers.length - i;
            preferenceMultipliers[i] = val * val;
        }
        MPObjective objective = createObjectiveFunction(solver, vars, preferenceMultipliers);
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
            memberAssignments[i] = new MemberAssignment(input, i, vars[i], detailedPrinting);
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
            if (t >= 0) {
                if (r < 0)
                    constraint = solver.makeConstraint(1, 1, String.format("colsumteam%droleanydcolumn%d", t, constraintIndex));
                else
                    constraint = solver.makeConstraint(1, 1, String.format("colsumteam%drole%dcolumn%d", t, r, constraintIndex));
            } else {
                constraint = solver.makeConstraint(1, 1, String.format("colsumextra%dcolumn%d", constraintIndex - beginExtraColumns, constraintIndex));
            }
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
            MPConstraint constraint = solver.makeConstraint(1, 1, String.format("rowsummember%drow%d", m, m));;
            constraints[m] = constraint;
            // System.out.printf("Preferred teams for member %d: %s%n", m, Arrays.toString(input.getMember(m).getPreferredTeams()));
            int[] memberPrefs = input.getMemberPreferences(m);
            forEach(getRows(vars, m), (v, i, j) -> {
                int t = input.getJToTeam(j);
                if (t < 0) {
                    constraint.setCoefficient(v, 1);
                    return;
                }
                for (int team = 0; team < memberPrefs.length; team++) {
                    if (memberPrefs[team] == t) {
                        constraint.setCoefficient(v, 1);
                        break;
                    }
                }
            });
        }
        return constraints;
    }

    protected MPConstraint[] createConstraint3MemberRoleCapabilities(MPSolver solver, MPVariable[][] vars) {
        MPConstraint[] constraints = new MPConstraint[input.numbRows()];
        for (int m = 0; m < constraints.length; m++) {
            MPConstraint constraint = solver.makeConstraint(NEGATIVE_INFINITY, 0, String.format("membercapableroles%d", m));
            constraints[m] = constraint;
            int[] memberRoles = input.getMemberRoles(m);
            forEach(getRows(vars, m), (v, i, j) -> {
                int role = input.getJToRole(j);
                if (role == -1) return;
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

    protected MPConstraint[] createConstraint4TeamSizeRequirements(MPSolver solver, MPVariable[][] vars) {
        MPConstraint[] constraints = new MPConstraint[input.numbTeams()];
        int constraintIndex = 0;
        for (int t = 0; t < input.numbTeams(); t++) {
            int[] tColumns = input.getColsOfTeam(t);
            MPConstraint constraint = solver.makeConstraint(input.getTeamMinimumMembers(t), INFINITY, String.format("team%dminmembers", t));
            constraints[constraintIndex] = constraint;
            forEach(getColumns(vars, tColumns), (v, i, j) -> {
                constraint.setCoefficient(v, 1);
            });
            constraintIndex++;
        }
        return constraints;
    }

    protected MPConstraint[][] createConstraint5FriendshipRequirements(MPSolver solver, MPVariable[][] vars) {
        // For each friendship, there needs to be a set of constraints for each team
        // For each friendship-team combo, there needs to be a constraint for each member where
        // their assignment to the team is |F_i| and all other member's assignments are -1.
        // Then, the constraint is only met if they sum to be 0.

        Friendship[] friendships = input.getFriendships();
        MPConstraint[][] constraints = new MPConstraint[friendships.length][];
        for (int f = 0; f < constraints.length; f++) {
            Member[] members = friendships[f].getMembers();
            int[] memberIndexes = Arrays.stream(members).mapToInt(m -> input.getMemberIndex(m)).toArray();
            Arrays.sort(memberIndexes);
            int friendshipSize = members.length - 1;
            MPConstraint[] friendshipConstraints = new MPConstraint[members.length * input.numbTeams()];
            constraints[f] = friendshipConstraints;
            int constraintIndex = 0;
            for (int m = 0; m < members.length; m++) {
                for (int t = 0; t < input.numbTeams(); t++) {
                    //System.out.printf("Created friendship constraint for member %d on team %d%n", m, t);
                    MPConstraint constraint = solver.makeConstraint(0, 0, String.format("member%dfriendshipteam%d", m, t));
                    friendshipConstraints[constraintIndex] = constraint;
                    int[] teamCols = input.getColsOfTeam(t);
                    int currentMember = m;
                    forEach(vars, (v, i, j) -> {
                        if ((Arrays.binarySearch(teamCols, j) >= 0 || input.getJToTeam(j) < 0) && Arrays.binarySearch(memberIndexes, i) >= 0) {
                            if (i == memberIndexes[currentMember]) {
                                //System.out.printf("Added coefficient to row, column %d, %d%n", i, j);
                                constraint.setCoefficient(v, friendshipSize);
                            } else {
                                constraint.setCoefficient(v, -1);
                            }
                        }
                    });
                    constraintIndex++;
                }
            }
        }
        return constraints;
    }

    protected MPObjective createObjectiveFunction(MPSolver solver, MPVariable[][] vars, int ... prefValues) {
        MPObjective objective = solver.objective();
        forEach(vars, (v, i, j) -> {
            Member m = input.getMember(i);
            String[] preferences = m.getPreferredTeams();
            int teamCol = input.getJToTeam(j);
            if (teamCol < 0) return;
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
        protected String[] assignedRoleNames;
        protected int positiveTeams;
        protected String singleTeamName;
        protected String singleRoleName;

        public MemberAssignment(TeamSortingInput input, int member, MPVariable[] memberRow, boolean detailedPrinting) {
            this.member = member;
            m = input.getMember(member);
            teamAssignments = new double[memberRow.length];
            int numbAssignedTeams = 0;
            for (int i = 0; i < memberRow.length; i++) {
                teamAssignments[i] = memberRow[i].solutionValue();
                if (teamAssignments[i] != 0) {
                    numbAssignedTeams++;
                }
            }
            assignedTeams = new int[numbAssignedTeams];
            assignedTeamNames = new String[numbAssignedTeams];
            assignedRoleNames = new String[numbAssignedTeams];
            int current = 0;
            positiveTeams = 0;
            for (int i = 0; i < teamAssignments.length; i++) {
                if (teamAssignments[i] != 0) {
                    assignedTeams[current] = i;
                    int teamNumber = input.getJToTeam(i);
                    assignedTeamNames[current] = teamNumber >= 0 ? input.getTeams()[teamNumber] : "<Any>";
                    if (teamNumber >= 0 || detailedPrinting) {
                        positiveTeams++;
                    }
                    assignedRoleNames[current] = input.getJToRole(i) >= 0 ? input.getRoles()[input.getJToRole(i)] : "<Any>";
                    current++;
                }
            }
            if (!detailedPrinting) {
                if (positiveTeams == 1) {
                    int teamNumber = 0;
                    for (int i = 0; i < teamAssignments.length; i++) {
                        teamNumber = input.getJToTeam(i);
                        if (teamNumber >= 0) {
                            singleRoleName = input.getJToRole(i) >= 0 ? input.getRoles()[input.getJToRole(i)] : "Any";
                            singleTeamName = input.getTeams()[teamNumber];
                        }
                    }
                } else if (positiveTeams == 0) {
                    singleRoleName = "<Any>";
                    singleTeamName = "<Any>";
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
            if (assignedTeams.length == 0) return String.format("Member (%d) %s assigned to no team", member, m.getName());
            if (assignedTeams.length == 1) return String.format("Member (%d) %s assigned to %s as %s (%d) (x=%f): Preference %d", member, m.getName(), assignedTeamNames[0], assignedRoleNames[0], assignedTeams[0], teamAssignments[assignedTeams[0]], m.getPreference(assignedTeamNames[0]) + 1);
            String result = String.format("Member (%d) %s assigned to ", member, m.getName());
            if (positiveTeams == 0 || positiveTeams == 1) {
                String[] teamAssignments = new String[assignedTeams.length];
                for (int i = 0; i < teamAssignments.length; i++) {
                    teamAssignments[i] = String.format("(%d, x=%f, P%d)", assignedTeams[i], this.teamAssignments[assignedTeams[i]], m.getPreference(assignedTeamNames[i]) + 1);
                }
                result += String.format("%s as %s %s", singleTeamName, singleRoleName,
                        Arrays.toString(teamAssignments));
            } else {
                String[] teams = new String[assignedTeams.length];
                for (int i = 0; i < teams.length; i++) {
                    teams[i] = String.format("%s as %s (%d) (x=%f): Preference %d", assignedTeamNames[i], assignedRoleNames[i], assignedTeams[i], teamAssignments[assignedTeams[i]], m.getPreference(assignedTeamNames[i]) + 1);
                }
                result += "teams: " + Arrays.toString(teams);
            }
            return result;
        }
    }

}
