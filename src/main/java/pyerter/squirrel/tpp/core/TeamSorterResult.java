package pyerter.squirrel.tpp.core;

import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import pyerter.squirrel.tpp.friendship.Friendship;
import pyerter.squirrel.tpp.friendship.FriendshipObjectiveValues;

import java.util.*;

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

    protected Map<String, Friendship> friendshipMap = new HashMap<>();
    protected int friendCount = 0;
    protected Map<Friendship, Integer> friendshipIndexMap = new HashMap<>();
    protected Friendship[] friendships;
    protected boolean calculatedFriendships = false;

    protected TeamSorterResult roundedResult = null;

    public TeamSorterResult(MPSolver solver, MPObjective objective, MPVariable[][] vars, TeamSortingInput input, final MPSolver.ResultStatus status, int[] preferenceMultipliers, MemberAssignment[] assignments) {
        this.solver = solver;
        this.objective = objective;
        this.vars = vars;
        this.input = input;
        this.assignments = assignments;
        assignmentMap = new HashMap<>();
        for (int i = 0; i < assignments.length; i++) {
            assignmentMap.put(input.getMember(i).getName(), assignments[i]);
        }
        this.status = status;
        this.preferenceMultipliers = preferenceMultipliers;
    }

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

    public TeamSorterResult(MPSolver solver, MPObjective objective, MPVariable[][] vars, TeamSortingInput input, final MPSolver.ResultStatus status, int[] preferenceMultipliers, TeamSorterResult roundedResult) {
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
        this.roundedResult = roundedResult;
    }

    public TeamSorterResult getRoundedResult() {
        return roundedResult;
    }

    public void setRoundedResult(TeamSorterResult result) {
        this.roundedResult = result;
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

    public String toPrintFinalFriendshipAssignments() {
        if (!isValidSolution()) {
            return "Solver could not find feasible solution.";
        }
        calculateFriendships();
        String out = "";
        for (int i = 0; i < friendships.length; i++) {
            String currentFriendship = String.format("Friendship %d: ", i);
            for (int t = 0; t < input.numbTeams(); t++) {
                LinkedList<String> membersInTeam = new LinkedList<>();
                for (String member : friendships[i].getFriends()) {
                    if (assignmentMap.get(member).getFinalTeamAssignment() == t) {
                        membersInTeam.add(member);
                    }
                }
                if (membersInTeam.size() > 0) {
                    currentFriendship += String.format("Team %d members %s, ", t, Arrays.toString(membersInTeam.toArray(String[]::new)));
                }
            }
            out += currentFriendship + "\n";
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
                    prefNumber == -1 ? 1 : preferenceMultipliers[prefNumber],
                    prefNumber, preferences[index]);
        }
        return out.substring(0, out.length() - 1);
    }

    public String toPrintFinalFriendshipObjective() {
        if (!isValidSolution()) {
            return "Solver could not find feasible solution.";
        }
        getObjectiveValue();
        String out = "Final Friendship Objective: " + getFinalFriendshipObjectiveValue();
        return out;
    }

    public void calculateFriendships() {
        getObjectiveValue();
        if (calculatedFriendships) return;

        friendshipMap = new HashMap<>();
        friendCount = 0;
        friendshipIndexMap = new HashMap<>();
        for (int i = 0; i < input.numbMembers(); i++) {
            Optional<Friendship> friendship = input.tryGetFriendship(input.getMember(i).getName());
            if (friendship.isPresent()) {
                if (!friendshipMap.containsValue(friendship.get())) {
                    friendshipIndexMap.put(friendship.get(), friendCount);
                    friendCount++;
                }
                friendshipMap.put(input.getMember(i).getName(), friendship.get());
            } else {
                Friendship singleFriendship = new Friendship(input.getMember(i).getName(), input.getMember(i).getName());
                singleFriendship.initialize(input);
                friendshipMap.put(input.getMember(i).getName(), singleFriendship);
                friendshipIndexMap.put(singleFriendship, friendCount);
                friendCount++;
            }
        }
        friendships = new Friendship[friendCount];
        for (Friendship f: friendshipMap.values()) {
            friendships[friendshipIndexMap.get(f)] = f;
        }
        calculatedFriendships = true;
    }

    public float getFinalFriendshipObjectiveValue() {
        getObjectiveValue();
        calculateFriendships();

        float totalObjective = 0;

        for (int fIndex = 0; fIndex < friendships.length; fIndex++) {
            Friendship f = friendships[fIndex];
            String[] friends = f.getFriends();
            int[] friendTeamCounts = new int[input.numbTeams()];
            for (String friend : friends) {
                friendTeamCounts[assignmentMap.get(friend).getFinalTeamAssignment()] += 1;
            }
            for (int t = 0; t < friendTeamCounts.length; t++) {
                float currentObjective = ((float)friendTeamCounts[t]) / friends.length;
                currentObjective *= currentObjective;
                totalObjective += currentObjective;
            }
        }

        return totalObjective;
    }

    public float getTheoreticalMaxFriendshipObjectiveValue() {
        getObjectiveValue();
        calculateFriendships();

        return friendCount;
    }

    public float getTheoreticalMaxObjectiveValue() {
        return preferenceMultipliers[0] * input.numbMembers();
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
            if (friendship != null && false) {
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

    public List<String[]> getCsvPrint() {
        if (!isValidSolution()) {
            return null;
        }

        // <preferences>, <preference multipliers>,
        // objective function, friendship objective function, max obj. func., max friendship obj. func.

        int prefColumns = input.getNumbPreferences() + 1;
        String[] headers = new String[prefColumns * 2 + 6];
        String[] values = new String[headers.length];
        for (int i = 0; i < preferenceMultipliers.length; i++) {
            headers[i] = "Preference " + i;
        }

        int[] preferences = getFinalPreferences();
        for (int i = 1; i <= input.getNumbPreferences() + 1; i++) {
            int index = (i % (input.getNumbPreferences() + 1));
            int prefNumber = index - 1;
            headers[index] = index == 0 ? "Not Preference" : "Preference " + index;
            headers[index + prefColumns] = index == 0 ? "P None Value" : "P " + index + " Value";
            values[index] = "" + preferences[index];
            values[index + prefColumns] = "" + (index == 0 ? 1 : preferenceMultipliers[prefNumber]);
        }
        int index = prefColumns * 2;
        headers[index] = "Objective";
        headers[index + 1] = "Friendship Objective";
        headers[index + 2] = "Max Objective";
        headers[index + 3] = "Max Friendship Objective";
        headers[index + 4] = "q";
        headers[index + 5] = "M";
        values[index] = "" + getObjectiveValue();
        values[index + 1] = "" + getFinalFriendshipObjectiveValue();
        values[index + 2] = "" + getTheoreticalMaxObjectiveValue();
        values[index + 3] = "" + getTheoreticalMaxFriendshipObjectiveValue();
        values[index + 4] = "" + input.getO();
        values[index + 5] = "" + input.numbMembers();


        List<String[]> list = new ArrayList<>();
        list.add(headers);
        list.add(values);
        list.add(new String[0]); // spacer row

        String[] assignmentHeaders = new String[]{"Member", "Team", "Role", "Preference"};
        list.add(assignmentHeaders);
        for (int i = 0; i < assignments.length; i++) {
            // += assignments[i].toPrintFinalAssignment() + "\n";
            String[] memberRow = new String[]{assignments[i].getMember().getName(),
                    assignments[i].getFinalTeamAssignmentName(),
                    assignments[i].getFinalTeamAssignmentRoleName(),
                    "" + (input.getMember(i).getPreference(assignments[i].getFinalTeamAssignmentName()) + 1)
            };
            list.add(memberRow);
        }
        return list;
    }

}
