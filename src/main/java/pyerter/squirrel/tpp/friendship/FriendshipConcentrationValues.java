package pyerter.squirrel.tpp.friendship;

import pyerter.squirrel.tpp.core.Member;
import pyerter.squirrel.tpp.core.TeamSortingInput;

public class FriendshipConcentrationValues {

    protected int team;
    protected double objectiveValue;
    protected Friendship friendship;

    public FriendshipConcentrationValues(Friendship friendship, int t, TeamSortingInput input, double[][] solution) {
        team = t;
        this.friendship = friendship;
        int teamObjectiveValue = 0;
        int[] memberRows = new int[friendship.getFriends().length];
        for (int i = 0; i < memberRows.length; i++) {
            memberRows[i] = input.getMemberIndex(friendship.getFriends()[i]);
        }
        for (int m : memberRows) {
            for (int col : input.getColsOfTeam(t)) {
                objectiveValue += solution[m][col];
            }
        }
    }

    public int getTeam() {
        return team;
    }

    public double getObjectiveValue() {
        return objectiveValue;
    }

    public Friendship getFriendship() {
        return friendship;
    }

}
