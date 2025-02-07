package pyerter.squirrel.tpp.friendship;

import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import pyerter.squirrel.tpp.core.Member;
import pyerter.squirrel.tpp.core.TeamSorterSolver;
import pyerter.squirrel.tpp.core.TeamSortingInput;

import java.util.Arrays;

/**
 * Author: Porter Squires
 * License: MIT License
 *
 *
 */
public class TeamSorterFriendshipSolverOld extends TeamSorterSolver {

    public TeamSorterFriendshipSolverOld(TeamSortingInput input) {
        this(input, false);
    }

    public TeamSorterFriendshipSolverOld(TeamSortingInput input, boolean useIntegralVariables) {
        super(input, false);
    }

    protected MPVariable[][] createVariables(MPSolver solver) {
        int rows = input.numbMembers();
        int cols = input.getO();
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
        MPConstraint[] constraints = new MPConstraint[input.getO()];
        int constraintIndex = 0;
        int colIndex = 0;
        // Create constraints for all columns
        int beginExtraColumns = constraintIndex;
        while (constraintIndex < constraints.length) {
            // get team role mapping if it exists
            int t = input.getJToTeam(colIndex);
            int r = input.getJToRole(colIndex);
            MPConstraint constraint = null;
            if (t >= 0) {
                if (r < 0) {
                    int[] colsOfTeam = input.getOColsOfTeam(t);
                    boolean contained = false;
                    for (int i = 0; i < colsOfTeam.length; i++) {
                        if (colsOfTeam[i] == colIndex) {
                            contained = true;
                            break;
                        }
                    }
                    if (contained)
                        constraint = solver.makeConstraint(1, 1, String.format("colsumteam%droleanydcolumn%d", t, colIndex));
                } else
                    constraint = solver.makeConstraint(1, 1, String.format("colsumteam%drole%dcolumn%d", t, r, colIndex));
            }
            if (constraint != null) {
                MPConstraint constraintDup = constraint;
                constraints[constraintIndex] = constraint;
                forEach(getColumns(vars, colIndex), (v, i, j) -> {
                    constraintDup.setCoefficient(v, 1);
                });
                constraintIndex++;
            }
            colIndex++;
        }
        return constraints;
    }

    protected MPConstraint[] createConstraint2MemberRowSums(MPSolver solver, MPVariable[][] vars) {
        MPConstraint[] constraints = new MPConstraint[input.numbRows()];
        for (int m = 0; m < constraints.length; m++) {
            MPConstraint constraint = solver.makeConstraint(1, 1, String.format("rowsummember%drow%d", m, m));;
            constraints[m] = constraint;
            // System.out.printf("Preferred teams for member %d: %s%n", m, Arrays.toString(input.getMember(m).getPreferredTeams()));
            // Remove need to filter only teams which a member prefers
            //int[] memberPrefs = input.getMemberPreferences(m);
            //Arrays.sort(memberPrefs);
            forEach(getRows(vars, m), (v, i, j) -> {
                //int t = input.getJToTeam(j);
                //if (t < 0) {
                //    constraint.setCoefficient(v, 1);
                //    return;
                //}
                //if (Arrays.binarySearch(memberPrefs, t) >= 0) {
                constraint.setCoefficient(v, 1);
                //}
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

    public interface MatrixVariableConsumer {
        public void consume(MPVariable var, int i, int j);
    }

}
