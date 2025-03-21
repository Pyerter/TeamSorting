package pyerter.squirrel.tpp.friendship;

import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import pyerter.squirrel.tpp.core.Member;
import pyerter.squirrel.tpp.core.MemberAssignment;
import pyerter.squirrel.tpp.core.TeamSortingInput;

import java.util.*;

public class TeamSorterFriendshipRoundingConstrained {

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
    protected FriendshipConcentrationValues[][] friendshipConcentrationValues;
    protected double[][] solution;
    protected boolean[][] finalizedValues;

    public TeamSorterFriendshipRoundingConstrained(MPSolver solver, MPObjective objective, MPVariable[][] vars,
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

        solution = new double[vars.length][];
        finalizedValues = new boolean[vars.length][];
        for (int i = 0; i < vars.length; i++) {
            solution[i] = new double[vars[i].length];
            finalizedValues[i] = new boolean[vars[i].length];
            Arrays.fill(finalizedValues[i], false);
            for (int j = 0; j < vars[i].length; j++) {
                solution[i][j] = vars[i][j].solutionValue();
            }
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
        friendshipConcentrationValues = new FriendshipConcentrationValues[friendCount][];
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
            friendshipConcentrationValues[i] = new FriendshipConcentrationValues[totalPossibleTeams];
            totalPossibleTeams = 0;
            for (int t = 0; t < possibleTeams.length; t++) {
                if (possibleTeams[t]) {
                    FriendshipObjectiveValues friendshipObj = new FriendshipObjectiveValues(friendships[i], t, input, preferenceMultipliers);
                    friendshipObjectiveValues[i][totalPossibleTeams] = friendshipObj;
                    FriendshipConcentrationValues friendshipConcentration = new FriendshipConcentrationValues(friendships[i], t, input, solution);
                    friendshipConcentrationValues[i][totalPossibleTeams] = friendshipConcentration;
                    totalPossibleTeams++;
                }
            }
            //String[] friendshipNameArr = Arrays.stream(friendshipObjectiveValues[i]).map((f) -> String.format("Friendship %s to Team %d (%d)", f.getFriendship().getFriendshipName(), f.getTeam(), (int)f.getObjectiveValue())).toArray(String[]::new);
            //System.out.printf("Friendship array: %s%n", Arrays.toString(friendshipNameArr));
            //System.out.flush();
            Arrays.sort(friendshipObjectiveValues[i], (f1, f2) -> (int)((-f1.getObjectiveValue() + f2.getObjectiveValue())*10000));
            Arrays.sort(friendshipConcentrationValues[i], (f1, f2) -> (int)((-f1.getObjectiveValue() + f2.getObjectiveValue())*10000));
        }
    }

    // From a team with a target member on the fromTeam, check if any members can be swapped
    // on the toTeam.
    public boolean findAndSwapTarget(int toTeam, int withMember, int fromColumn) {
        if (solution[withMember][fromColumn] < 0.0001) return false;
        int[] searchCols = input.getColsOfTeam(toTeam);
        for (int m = 0; m < solution.length; m++) { // member
            for (int s = 0; s < searchCols.length; s++) { // search indexes
                int c = searchCols[s]; // column
                if (!finalizedValues[m][c]) { // if is not yet finalized, can check
                    if (canSwap(withMember, m, fromColumn, c)) { // swap em!
                        double temp = solution[withMember][fromColumn];
                        solution[m][fromColumn] = solution[m][c];
                        solution[withMember][c] = temp;
                        finalizedValues[withMember][c] = true;
                    }
                }
            }
        }
        return false;
    }

    public boolean memberCanFillColumn(int member, int col) {
        // if member does not prefer the team, return false
        if (input.getMember(member).getPreference(input.getTeams()[input.getJToTeam(col)]) < 0) {
            return false;
        }
        int[] memberRoles = input.getMemberRoles(member);
        int roleNumb = input.getJToRole(col);
        if (roleNumb < 0) return true;
        for (int i = 0; i < memberRoles.length; i++) {
            if (memberRoles[i] == roleNumb) return true;
        }
        return false;
    }

    public boolean canSwap(int member1, int member2, int col1, int col2) {
        return memberCanFillColumn(member1, col2) && memberCanFillColumn(member2, col1);
    }

    public int getColumnIndex(int member) {
        for (int i = 0; i < solution[member].length; i++) {
            if (solution[member][i] > 0) return i;
        }
        return -1;
    }

    public MemberAssignment[] calculateAssignments() {

        PriorityQueue<FriendshipConcentrationValues> friendshipQueue = new PriorityQueue<>(friendCount, (f1, f2) -> (int)((-f1.getObjectiveValue() + f2.getObjectiveValue())*10000));
        for (int f = 0; f < friendCount; f++) {
            friendshipQueue.add(friendshipConcentrationValues[f][0]);
        }

        int[] assignments = new int[input.numbMembers()];

        int[][] friendshipAssignmentSlots = new int[friendCount][];
        for (int i = 0; i < friendshipAssignmentSlots.length; i++) {
            Friendship friendship = this.friendships[i];
            friendshipAssignmentSlots[i] = new int[friendship.size()];
            for (int m = 0; m < friendship.size(); m++) {
                MemberAssignment assignment = assignmentMap.get(friendship.getFriends()[m]);
                if (assignment.getAssignedTeams().length > 1) {
                    System.out.printf("Err: Member %s (%d) has more than one assignments: %s%n", assignment.getMember().getName(), m, Arrays.toString(assignment.getAssignedTeams()));
                    return null;
                }
                try {
                    friendshipAssignmentSlots[i][m] = input.getJToTeam(assignment.getAssignedTeams()[0]);
                } catch (Exception e) {
                    System.out.println("Err: tries to access assignment and none present!");
                    return null;
                }
            }
        }

        for (int fIndex = 0; fIndex < friendCount; fIndex++) {
            System.out.printf("Friendship %d Assignment slots: %s%n", fIndex, Arrays.toString(friendshipAssignmentSlots[fIndex]));
        }

        boolean[][] scannedFriendshipTeams = new boolean[friendCount][input.numbTeams()];

        while (!friendshipQueue.isEmpty()) {
            FriendshipConcentrationValues fValues = friendshipQueue.poll();
            Friendship friendship = fValues.getFriendship();
            int fIndex = friendshipIndexMap.get(fValues.getFriendship());
            int currentSearchTeam = fValues.getTeam();
            boolean[] searchTeams = new boolean[input.numbTeams()];
            boolean[] searchableMembers = new boolean[input.numbMembers()];

            for (int i = 0; i < searchTeams.length; i++) {
                searchTeams[i] = !(currentSearchTeam == i || scannedFriendshipTeams[fIndex][i]);
            }

            Arrays.fill(searchableMembers, true);
            for (int i = 0; i < friendship.size(); i++) {
                searchableMembers[input.getMemberIndex(friendship.getMembers()[i])] = false;
            }

            for (int fMember = 0; fMember < friendship.size(); fMember++) {
                Member targetMember = friendship.getMembers()[fMember];
                int memberRow = input.getMemberIndex(targetMember);
                int memberColumn = getColumnIndex(memberRow);
                int memberTeam = input.getJToTeam(memberColumn);
                if (memberTeam < 0) {
                    if (targetMember.getPreference(input.getTeams()[currentSearchTeam]) >= 0) {
                        finalizedValues[memberRow][memberColumn] = true;
                        assignments[memberRow] = currentSearchTeam;
                        this.assignments[memberRow].setFinalTeamAssignment(currentSearchTeam, input);
                        friendshipAssignmentSlots[fIndex][fMember] = currentSearchTeam;
                    }
                    continue;
                }
                if (!searchTeams[memberTeam]) {
                    finalizedValues[memberRow][memberColumn] = true;
                    assignments[memberRow] = memberTeam;
                    this.assignments[memberRow].setFinalTeamAssignmentColumn(memberColumn, input);
                    friendshipAssignmentSlots[fIndex][fMember] = currentSearchTeam;
                    continue;
                }
                boolean swapped = false;
                for (int victimMember = 0; victimMember < searchableMembers.length; victimMember++) {
                    if (!searchableMembers[victimMember]) continue;
                    for (int victimCol : input.getColsOfTeam(currentSearchTeam)) {
                        if (solution[victimMember][victimCol] > 0 && !finalizedValues[victimMember][victimCol] && canSwap(memberRow, victimMember, memberColumn, victimCol)) {
                            double temp = solution[memberRow][memberColumn];
                            solution[victimMember][memberColumn] = solution[victimMember][victimCol];
                            solution[memberRow][victimCol] = temp;
                            finalizedValues[memberRow][victimCol] = true;
                            this.assignments[memberRow].setFinalTeamAssignmentColumn(victimCol, input);
                            assignments[memberRow] = currentSearchTeam;
                            swapped = true;
                            friendshipAssignmentSlots[fIndex][fMember] = currentSearchTeam;
                            Friendship victimFriendship = this.friendshipMap.get(input.getMember(victimMember).getName());
                            if (victimFriendship != null) {
                                int friendshipMemberIndex = -1;
                                String[] victimFriends = victimFriendship.getFriends();
                                for (int i = 0; i < victimFriends.length; i++) {
                                    int currentIndex = input.getMemberIndex(victimFriends[i]);
                                    if (currentIndex == victimMember) {
                                        friendshipMemberIndex = i;
                                        break;
                                    }
                                }
                                if (friendshipMemberIndex >= 0) {
                                    //System.out.printf("Debug - - - - fIndex %d, memberIndex %d%n", friendshipIndexMap.get(victimFriendship), friendshipMemberIndex);
                                    //System.out.printf("Debug - - - - friendship: %s%n", Arrays.toString(victimFriendship.getFriends()));
                                    friendshipAssignmentSlots[friendshipIndexMap.get(victimFriendship)][friendshipMemberIndex] = memberTeam;
                                } else {
                                    System.out.printf("Err: could not alter member assignment of member %d (friend index %d) because not in friendship%n", victimMember, friendshipMemberIndex);
                                }
                            }
                            break;
                        }
                    }
                    if (swapped) break;
                }
            }

            scannedFriendshipTeams[fIndex][currentSearchTeam] = true;

            currentFriendshipChecker[fIndex] += 1;
            if (currentFriendshipChecker[fIndex] < friendshipConcentrationValues[fIndex].length) {
                friendshipQueue.add(friendshipConcentrationValues[fIndex][currentFriendshipChecker[fIndex]]);
            }

        }

        return this.assignments;
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
