package pyerter.squirrel.tpp.friendship;

import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import pyerter.squirrel.tpp.core.Member;
import pyerter.squirrel.tpp.core.MemberAssignment;
import pyerter.squirrel.tpp.core.TeamSortingInput;

import java.util.*;

public class TeamSorterFriendshipRounding {

    protected MPSolver solver;
    protected MPObjective objective;
    protected MPVariable[][] vars;
    protected TeamSortingInput input;
    protected final MPSolver.ResultStatus status;
    protected int[] preferenceMultipliers;
    protected MemberAssignment[] assignments;
    protected Map<String, MemberAssignment> assignmentMap;
    protected Map<String, Friendship> friendshipMap;
    protected Map<Friendship, Integer> friendshipIndexMap;
    protected Friendship[] friendships;
    protected int friendCount = 0;
    protected int[] currentFriendshipChecker;
    protected FriendshipObjectiveValues[][] friendshipObjectiveValues;

    public TeamSorterFriendshipRounding(MPSolver solver, MPObjective objective, MPVariable[][] vars,
                                        TeamSortingInput input, final MPSolver.ResultStatus status, int[] preferenceMultipliers) {
        this.solver = solver;
        this.vars = vars;
        this.input = input;
        this.status = status;
        this.preferenceMultipliers = preferenceMultipliers;
        assignments = new MemberAssignment[input.numbRows()];
        assignmentMap = new HashMap<>();
        for (int i = 0; i < assignments.length; i++) {
            assignments[i] = new MemberAssignment(input, i, vars[i], false);
            assignmentMap.put(input.getMember(i).getName(), assignments[i]);
        }
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
        friendshipObjectiveValues = new FriendshipObjectiveValues[friendCount][];
        currentFriendshipChecker = new int[friendCount];
        for (Friendship f: friendshipMap.values()) {
            friendships[friendshipIndexMap.get(f)] = f;
        }
        for (int i = 0; i < friendCount; i++) {
            boolean[] possibleTeams = new boolean[input.numbTeams()];
            for (int f = 0; f < friendships[i].size(); f++) {
                for (Member m: friendships[i].getMembers()) {
                    MemberAssignment assignment = assignments[input.getMemberIndex(m)];
                    int[] teams = assignment.getAssignedTeams();
                    for (int t = 0; t < teams.length; t++) {
                        int teamIndex = input.getJToTeam(teams[t]);
                        if (teamIndex < 0) teamIndex = input.convertTeamNameToIndex(m.getPreferredTeams()[0])[0];
                        possibleTeams[teamIndex] = true;
                    }
                }
            }
            int totalPossibleTeams = 0;
            for (int t = 0; t < possibleTeams.length; t++) {
                if (possibleTeams[t]) totalPossibleTeams++;
            }
            friendshipObjectiveValues[i] = new FriendshipObjectiveValues[totalPossibleTeams];
            totalPossibleTeams = 0;
            for (int t = 0; t < possibleTeams.length; t++) {
                if (possibleTeams[t]) {
                    FriendshipObjectiveValues friendshipObj = new FriendshipObjectiveValues(friendships[i], t, input, preferenceMultipliers);
                    friendshipObjectiveValues[i][totalPossibleTeams] = friendshipObj;
                    totalPossibleTeams++;
                }
            }
            //String[] friendshipNameArr = Arrays.stream(friendshipObjectiveValues[i]).map((f) -> String.format("Friendship %s to Team %d (%d)", f.getFriendship().getFriendshipName(), f.getTeam(), (int)f.getObjectiveValue())).toArray(String[]::new);
            //System.out.printf("Friendship array: %s%n", Arrays.toString(friendshipNameArr));
            System.out.flush();
            Arrays.sort(friendshipObjectiveValues[i], (f1, f2) -> (int)((-f1.getObjectiveValue() + f2.getObjectiveValue())*10000));
        }
    }

    public boolean canFitFriendship(Friendship f, int t, int[] assignments, int currentMemberCount) {
        int teamCapacity = input.getTeamMinimumMembers(t) + (input.numbMembers() - input.getO());
        if (f.size() + currentMemberCount <= teamCapacity) {
            return true;
        }
        return false;
    }

    public int[] CalculateAssignments() {
        int[] assignments = new int[input.numbMembers()];
        int[] teamMembers = new int[input.numbTeams()];

        Arrays.fill(assignments, -1);

        PriorityQueue<FriendshipObjectiveValues> friendshipQueue = new PriorityQueue<>(friendCount, (f1, f2) -> (int)((-f1.getObjectiveValue() + f2.getObjectiveValue())*10000));
        for (int f = 0; f < friendCount; f++) {
            friendshipQueue.add(friendshipObjectiveValues[f][0]);
        }

        String out = "";

        while (!friendshipQueue.isEmpty()) {
            FriendshipObjectiveValues fValues = friendshipQueue.poll();
            if (canFitFriendship(fValues.getFriendship(), fValues.getTeam(), assignments, teamMembers[fValues.getTeam()])) {
                for (Member m: fValues.getFriendship().getMembers()) {
                    int mIndex = input.getMemberIndex(m);
                    assignments[mIndex] = fValues.getTeam();
                    this.assignments[mIndex].setFinalTeamAssignment(fValues.getTeam(), input);
                    out += this.assignments[mIndex].toPrintFinalAssignment() + "\n";
                }
            } else {
                int f = friendshipIndexMap.get(fValues.getFriendship());
                currentFriendshipChecker[f] += 1;
                if (currentFriendshipChecker[f] >= friendshipObjectiveValues[f].length) {
                    System.out.printf("Err: friendship %s could not be assigned to a proper team!%n", fValues.getFriendship().getFriendshipName());
                    return assignments;
                }
                friendshipQueue.add(friendshipObjectiveValues[f][currentFriendshipChecker[f]]);
            }
        }

        System.out.printf("Final Assignments Rounded ---%n%s%n%s%n------%n%n", toPrintFinalPreferences(), out);


        return assignments;

    }

    public String toPrintFinalPreferences() {
        int[] preferences = new int[input.getNumbPreferences() + 1];
        for (int m = 0; m < input.numbMembers(); m++) {
            int pref = assignments[m].getMember().getPreference(assignments[m].getFinalTeamAssignmentName());
            preferences[pref+1] += 1;
        }
        String out = "Members with preferences:\n";
        int totalValue = 0;
        for (int i = 1; i <= input.getNumbPreferences() + 1; i++) {
            int index = (i % (input.getNumbPreferences() + 1));
            int prefNumber = index - 1;
            out += String.format("    Preference (%s%d) %d: %d%n",
                    index == 0 ? "" : "+",
                    prefNumber == -1 ? 0 : preferenceMultipliers[prefNumber],
                    prefNumber, preferences[index]);
            if (prefNumber >= 0) {
                totalValue += preferenceMultipliers[prefNumber] * preferences[index];
            }
        }
        out += String.format("    Total: %d%n", totalValue);
        return out.substring(0, out.length() - 1);
    }

}
