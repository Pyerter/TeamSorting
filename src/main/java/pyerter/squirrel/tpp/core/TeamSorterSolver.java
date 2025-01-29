package pyerter.squirrel.tpp.core;

import com.google.ortools.Loader;
import com.google.ortools.init.OrToolsVersion;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import pyerter.squirrel.tpp.TeamSortingLogger;
import pyerter.squirrel.tpp.friendship.Friendship;

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
    protected boolean useIntegralVariables = false;
    protected boolean useFriendships = true;
    protected boolean useHardPreferenceObjectiveFunction = false;
    protected boolean ignoreFriendships = false;

    public TeamSorterSolver(TeamSortingInput input) {
        this(input, false);
    }

    public TeamSorterSolver(TeamSortingInput input, boolean useIntegralVariables) {
        this.input = input;
        this.useIntegralVariables = useIntegralVariables;
    }

    public TeamSortingInput getInput() {
        return input;
    }

    public boolean isUseFriendships() {
        return useFriendships;
    }

    public void setUseFriendships(boolean useFriendships) {
        this.useFriendships = useFriendships;
    }

    public void setInput(TeamSortingInput input) {
        this.input = input;
    }

    public boolean isUseHardPreferenceObjectiveFunction() {
        return useHardPreferenceObjectiveFunction;
    }

    public void setUseHardPreferenceObjectiveFunction(boolean useHardPref) {
        useHardPreferenceObjectiveFunction = useHardPref;
    }

    public boolean isIgnoreFriendships() {
        return ignoreFriendships;
    }

    public void setIgnoreFriendships(boolean ignoreFriendships) {
        this.ignoreFriendships = ignoreFriendships;
    }

    public TeamSorterResult solve(TeamSortingLogger logger) {
        Loader.loadNativeLibraries();

        logger.log("Google OR-Tools version: " + OrToolsVersion.getVersionString());

        // Create the linear solver with the GLOP backend.
        String solverID = useIntegralVariables ? "SCIP" : "GLOP";
        MPSolver solver = MPSolver.createSolver(solverID);
        if (solver == null) {
            logger.log(String.format("Could not create solver %s", solverID));
            return null;
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
        logger.log(String.format("%dx%d Matrix: %d variables", input.numbRows(), input.numbAugmentedColumns(), input.numbRows() * input.numbAugmentedColumns()));
        logger.log(String.format("Created %d constraints for column sums.", constraintCounts[0]));
        logger.log(String.format("Created %d constraints for row sums.", constraintCounts[1]));
        logger.log(String.format("Created %d constraints for member role assignments.", constraintCounts[2]));
        logger.log(String.format("Created %d constraints for team size requirements.", constraintCounts[3]));
        logger.log(String.format("Created %d constraints for friendship requirements.", constraintCounts[4]));

        // Create the objective function
        int[] preferenceMultipliers = new int[input.getNumbPreferences()];
        for (int i = 0; i < preferenceMultipliers.length; i++) {
            int val = preferenceMultipliers.length - i;
            preferenceMultipliers[i] = val * val;
        }
        MPObjective objective = createObjectiveFunction(solver, vars, preferenceMultipliers);
        objective.setMaximization(); // we maximize it

        logger.log("Solving with " + solver.solverVersion());
        final MPSolver.ResultStatus resultStatus = solver.solve();

        TeamSorterResult result = new TeamSorterResult(solver, objective, vars, input, resultStatus, preferenceMultipliers);

        return result;
    }

    protected MPVariable[][] createVariables(MPSolver solver) {
        int rows = input.numbMembers();
        int cols = input.numbAugmentedColumns();
        MPVariable[][] vars = new MPVariable[rows][];
        if (useIntegralVariables) System.out.printf("Using integral variables");
        for (int row = 0; row < rows; row++) {
            if (!useIntegralVariables)
                vars[row] = solver.makeNumVarArray(cols, 0, 1);
            else
                vars[row] = solver.makeIntVarArray(cols, 0, 1);
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
            Arrays.sort(memberPrefs);
            forEach(getRows(vars, m), (v, i, j) -> {
                int t = input.getJToTeam(j);
                if (t < 0) {
                    constraint.setCoefficient(v, 1);
                    return;
                }
                if (useHardPreferenceObjectiveFunction || Arrays.binarySearch(memberPrefs, t) >= 0) {
                    constraint.setCoefficient(v, 1);
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
            Arrays.sort(memberRoles);
            forEach(getRows(vars, m), (v, i, j) -> {
                int role = input.getJToRole(j);
                if (role == -1) return;
                if (Arrays.binarySearch(memberRoles, role) < 0) {
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
        if (ignoreFriendships) {
            constraints = new MPConstraint[0][0];
            return constraints;
        }
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
        int objAdder = useHardPreferenceObjectiveFunction ? input.numbMembers() * (input.getNumbPreferences() + 2) : 0;
        System.out.printf("Adding flat value to preference objective function: %d%n", objAdder);
        System.out.printf("Should use hard objective function: %b", useHardPreferenceObjectiveFunction);
        forEach(vars, (v, i, j) -> {
            Member m = input.getMember(i);
            String[] preferences = m.getPreferredTeams();
            int teamCol = input.getJToTeam(j);
            if (teamCol < 0) return;
            for (int p = 0; p < preferences.length; p++) {
                Integer currentPref = input.getTeamMap().get(preferences[p]);
                if (currentPref != null && currentPref == teamCol) {
                    if (p < prefValues.length)
                        objective.setCoefficient(v, prefValues[p] + objAdder);
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

}
