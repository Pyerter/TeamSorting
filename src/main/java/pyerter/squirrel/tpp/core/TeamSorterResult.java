package pyerter.squirrel.tpp.core;

import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import pyerter.squirrel.tpp.friendship.Friendship;

import java.util.HashMap;
import java.util.Map;

/**
 * Author: Porter Squires
 * License: MIT License
 *
 *
 */
public class TeamSorterResult {

    protected MPSolver solver;
    protected MPObjective objective;
    protected MPVariable[][] vars;
    protected TeamSortingInput input;
    protected boolean detailedPrinting = false;
    protected MemberAssignment[] assignments;
    protected Map<String, MemberAssignment> assignmentMap;
    protected final MPSolver.ResultStatus status;
    protected int[] preferenceMultipliers;

    public TeamSorterResult(MPSolver solver, MPObjective objective, MPVariable[][] vars, TeamSortingInput input, final MPSolver.ResultStatus status, int[] preferenceMultipliers) {
        this.solver = solver;
        this.objective = objective;
        this.vars = vars;
        this.input = input;
        assignments = new MemberAssignment[input.numbRows()];
        assignmentMap = new HashMap<>();
        for (int i = 0; i < assignments.length; i++) {
            assignments[i] = new MemberAssignment(input, i, vars[i], detailedPrinting);
            assignmentMap.put(input.getMember(i).getName(), assignments[i]);
        }
        this.status = status;
        this.preferenceMultipliers = preferenceMultipliers;
    }

    public boolean isValidSolution() {
        return status == MPSolver.ResultStatus.FEASIBLE || status == MPSolver.ResultStatus.OPTIMAL;
    }

    public String toPrintAssignments() {
        if (!isValidSolution()) {
            return "Solver could not find feasible solution.";
        }
        String out = "";
        for (int i = 0; i < assignments.length; i++) {
            out += assignments[i].toPrintAssignment() + "\n";
        }
        return out.substring(0, out.length() - 1);
    }

    public String toPrintFinalAssignments() {
        if (!isValidSolution()) {
            return "Solver could not find feasible solution.";
        }
        getObjectiveValue();
        String out = "";
        for (int i = 0; i < assignments.length; i++) {
            out += assignments[i].toPrintFinalAssignment() + "\n";
        }
        return out.substring(0, out.length() - 1);
    }

    public int[] getFinalPreferences() {
        int[] preferences = new int[input.getNumbPreferences() + 1];
        getObjectiveValue();
        for (int i = 0; i < input.numbMembers(); i++) {
            preferences[input.getMember(i).getPreference(assignments[i].getFinalTeamAssignmentName()) + 1]++;
        }
        return preferences;
    }

    public String toPrintFinalPreferences() {
        if (!isValidSolution()) {
            return "Solver could not find feasible solution.";
        }
        int[] preferences = getFinalPreferences();
        String out = "Members with preferences:\n";
        for (int i = 1; i <= input.getNumbPreferences() + 1; i++) {
            int index = (i % (input.getNumbPreferences() + 1));
            int prefNumber = index - 1;
            out += String.format("    Preference (%s%d) %d: %d%n",
                    index == 0 ? "" : "+",
                    prefNumber == -1 ? 0 : preferenceMultipliers[prefNumber],
                    prefNumber, preferences[index]);
        }
        return out.substring(0, out.length() - 1);
    }

    public MPSolver getSolver() {
        return solver;
    }

    public MPObjective getObjective() {
        return objective;
    }

    public MPVariable[][] getVars() {
        return vars;
    }

    public TeamSortingInput getInput() {
        return input;
    }

    public String toPrintStats() {
        String out = "";
        out += "Status: " + status + "\n";
        if (status != MPSolver.ResultStatus.OPTIMAL) {
            out += "The problem does not have an optimal solution!\n";
            if (status == MPSolver.ResultStatus.FEASIBLE) {
                out += "A potentially suboptimal solution was found\n";
            } else {
                out += "The solver could not solve the problem.";
                return out;
            }
        }

        out += "Objective value = " + objective.value() + "\n";
        out += "Assigned objective value = " + getObjectiveValue() + "\n";

        out += "Advanced usage:\n";
        out += "    Problem solved in " + solver.wallTime() + " milliseconds\n";
        out += "    Problem solved in " + solver.iterations() + " iterations";
        return out;
    }

    public double getObjectiveValue() {
        double obj = 0;
        boolean[] assigned = new boolean[input.numbMembers()];
        for (int i = 0; i < assigned.length; i++) {
            if (assigned[i] || assignments[i].getFinalTeamAssignment() >= 0) {
                obj += assignments[i].getFinalAssignmentObjectiveValue(preferenceMultipliers);
                assigned[i] = true;
                continue;
            }
            int currentAssignedTeam = assignments[i].getAssignedTeam(input);
            if (currentAssignedTeam >= 0) {
                assignments[i].setFinalTeamAssignment(currentAssignedTeam, input);
                obj += assignments[i].getFinalAssignmentObjectiveValue(preferenceMultipliers);
                assigned[i] = true;
                continue;
            }
            Friendship friendship = input.getNameToFriends().get(input.getMember(i).getName());
            if (friendship != null) {
                Member[] friends = friendship.getMembers();
                for (int f = 0; f < friends.length; f++) {
                    int currentAssignment = assignmentMap.get(friends[f].getName()).getAssignedTeam(input);
                    if (currentAssignment >= 0) {
                        for (int f2 = 0; f2 < friends.length; f2++) {
                            int mIndex = input.getMemberIndex(friends[f2]);
                            assignments[mIndex].setFinalTeamAssignment(currentAssignment, input);
                            assigned[mIndex] = true;
                        }
                        break;
                    }
                }
                if (assigned[i]) {
                    obj += assignments[i].getFinalAssignmentObjectiveValue(preferenceMultipliers);
                    continue;
                }
                int[] friendTeamVotes = new int[input.numbTeams()];
                int max = -1;
                for (int f = 0; f < friends.length; f++) {
                    int desiredTeam = input.getTeamMap().get(friends[f].getPreferredTeams()[0]);
                    friendTeamVotes[desiredTeam]++;
                    if (max == -1 || friendTeamVotes[desiredTeam] > friendTeamVotes[max]) {
                        max = desiredTeam;
                    }
                }
                for (int f = 0; f < friends.length; f++) {
                    int mIndex = input.getMemberIndex(friends[f]);
                    assignments[mIndex].setFinalTeamAssignment(max, input);
                    assigned[mIndex] = true;
                }
                obj += assignments[i].getFinalAssignmentObjectiveValue(preferenceMultipliers);
                continue;
            } else {
                try {
                    assignments[i].setFinalTeamAssignment(input.getTeamMap().get(input.getMember(i).getPreferredTeams()[0]), input);
                    obj += assignments[i].getFinalAssignmentObjectiveValue(preferenceMultipliers);
                    assigned[i] = true;
                } catch (Exception e) {
                    System.out.printf("Error while assigning member %s to their first preference: %s", input.getMember(i).getName(), e.getMessage());
                }
            }
        }
        return obj;
    }



}
