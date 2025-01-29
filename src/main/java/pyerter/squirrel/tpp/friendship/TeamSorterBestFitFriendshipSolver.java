package pyerter.squirrel.tpp.friendship;

import pyerter.squirrel.tpp.core.Member;
import pyerter.squirrel.tpp.core.TeamSortingInput;

import java.util.*;

public class TeamSorterBestFitFriendshipSolver {

    protected TeamSortingInput input;

    public TeamSorterBestFitFriendshipSolver(TeamSortingInput input) {
        this.input = input;
    }

    public TeamSortingInput getInput() {
        return input;
    }

    public void setInput(TeamSortingInput input) {
        this.input = input;
    }

    public int[][] solve() {
        // i = member, j=0 for team assignment, j=1 for role assignment
        int[][] assignments = new int[input.numbMembers()][2];

        int friendshipCount = 0;
        Map<Friendship, Integer> friendshipMap = new HashMap<>();
        int[] friendshipIndexes = new int[input.numbMembers()];
        for (int i = 0; i < input.numbMembers(); i++) {
            String memberName = input.getMember(i).getName();
            Optional<Friendship> friendship = input.tryGetFriendship(memberName);
            if (friendship.isEmpty() || !friendshipMap.containsKey(friendship.get())) {
                friendshipIndexes[i] = friendshipCount;
                if (friendship.isPresent()) {
                    friendshipMap.put(friendship.get(), friendshipCount);
                }
                friendshipCount++;
            } else {
                friendshipIndexes[i] = friendshipMap.get(friendship.get());
            }
        }

        int[] preferenceMultipliers = new int[input.getNumbPreferences()];
        for (int i = 0; i < preferenceMultipliers.length; i++) {
            int val = preferenceMultipliers.length - i;
            preferenceMultipliers[i] = val * val;
        }

        int[][] memberPreferenceScores = new int[input.numbMembers()][input.numbTeams()];
        for (int i = 0; i < input.numbMembers(); i++) {
            Member m = input.getMember(i);
            String[] preferences = m.getPreferredTeams();
            int[] prefNumbers = new int[preferences.length];
            for (int j = 0; j < prefNumbers.length; j++) {
                prefNumbers[j] = input.getTeamMap().get(preferences[j]);
            }
            for (int j = 0; j < input.numbTeams(); j++) {
                int teamCol = input.getJToTeam(j);
                if (teamCol < 0) continue;
                for (int p = 0; p < preferences.length; p++) {
                    int currentPref = prefNumbers[p];
                    if (currentPref == teamCol) {
                        if (p < preferenceMultipliers.length) {
                            memberPreferenceScores[i][j] = preferenceMultipliers[p];
                        }
                        break;
                    }
                }
            }
        }

        float[][] friendshipScore = new float[friendshipCount][input.numbTeams()];
        int[] membersPerFriendship = new int[friendshipCount];
        for (int i = 0; i < input.numbMembers(); i++) {
            int friendship = friendshipIndexes[i];
            for (int j = 0; j < input.numbTeams(); j++) {
                friendshipScore[friendship][j] += memberPreferenceScores[i][j];
            }
            membersPerFriendship[friendship]++;
        }
        int[][] memberIndexes = new int[friendshipCount][];
        for (int i = 0; i < friendshipCount; i++) {
            memberIndexes[i] = new int[membersPerFriendship[i]];
            Arrays.fill(memberIndexes[i], -1);
            for (int j = 0; j < input.numbTeams(); j++)
                friendshipScore[i][j] /= membersPerFriendship[i];
        }
        for (int i = 0; i < input.numbMembers(); i++) {
            int friendship = friendshipIndexes[i];
            for (int j = 0; j < memberIndexes[friendship].length; j++) {
                if (memberIndexes[friendship][j] < 0) {
                    memberIndexes[friendship][j] = i;
                    break;
                }
            }
        }

        float maxScore = 0;
        PriorityQueue<FriendshipIndexScore> friendshipQueue = new PriorityQueue<>(new FriendshipIndexComparator());
        for (int i = 0; i < friendshipCount; i++) {
            int maxIndex = 0;
            for (int j = 1; j < input.numbTeams(); j++) {
                if (friendshipScore[i][j] > friendshipScore[i][maxIndex]) {
                    maxIndex = j;
                }
            }
            maxScore += friendshipScore[i][maxIndex];
            friendshipQueue.add(new FriendshipIndexScore(i, friendshipScore[i][maxIndex], maxIndex));
        }

        int teamExcess = input.numbMembers() - input.getO();
        int[] teamVacancies = new int[input.numbTeams()];
        float[] finalFriendshipScores = new float[friendshipCount];
        for (int i = 0; i < teamVacancies.length; i++) {
            int teamCapacity = input.getOColsOfTeam(i).length + teamExcess;
            teamVacancies[i] = teamCapacity;
        }
        while (!friendshipQueue.isEmpty()) {
            FriendshipIndexScore friendship = friendshipQueue.poll();
            int[] members = memberIndexes[friendship.getIndex()];
            int teamAssignment = friendship.getMaxIndex();
            if (members.length > teamVacancies[teamAssignment]) {
                PriorityQueue<FriendshipIndexScore> teamAssignments = new PriorityQueue<>(new FriendshipIndexComparator());
                for (int i = 0; i < input.numbTeams(); i++) {
                    teamAssignments.add(new FriendshipIndexScore(i, friendshipScore[friendship.getIndex()][i], 0));
                }
                FriendshipIndexScore assignment = teamAssignments.poll();
                while (assignment != null && members.length > teamVacancies[assignment.getIndex()]) {
                    assignment = teamAssignments.poll();
                }
                teamAssignment = assignment != null ? assignment.getIndex() : -1;
            }
            for (int i = 0; i < members.length; i++) {
                assignments[members[i]][0] = teamAssignment;
            }
            teamVacancies[teamAssignment] -= members.length;
            finalFriendshipScores[friendship.getIndex()] = teamAssignment >= 0 ? friendshipScore[friendship.getIndex()][teamAssignment] : -1;
        }

        System.out.printf("Best case score is %.2f%n", maxScore);
        float finalScore = 0;
        for (int i = 0; i < friendshipCount; i++) {
            finalScore += finalFriendshipScores[i];
        }
        System.out.printf("Final score is %.2f%n", finalScore);
        for (int i = 0; i < friendshipCount; i++) {
            System.out.printf("Friendship %d assigned to team %d, score %.2f%n", i, assignments[i][0], finalFriendshipScores[i]);
            for (int j = 0; j < memberIndexes[i].length; j++) {
                System.out.printf("  Member %d%n", memberIndexes[i][j]);
            }
        }

        return assignments;
    }

    protected static class FriendshipIndexScore {

        protected int index;
        protected float score;
        protected int maxIndex;
        public FriendshipIndexScore(int index, float score, int maxIndex) {
            this.index = index;
            this.score = score;
            this.maxIndex = maxIndex;
        }
        public int getIndex() {
            return index;
        }
        public float getScore() {
            return score;
        }
        public int getMaxIndex() {
            return maxIndex;
        }
    }

    protected static class FriendshipIndexComparator implements Comparator<FriendshipIndexScore> {

        @Override
        public int compare(FriendshipIndexScore o1, FriendshipIndexScore o2) {
            return Float.compare(o1.getScore(), o2.getScore());
        }
    }

}
