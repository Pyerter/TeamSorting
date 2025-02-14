package pyerter.squirrel.tpp.friendship;

import pyerter.squirrel.tpp.core.Member;
import pyerter.squirrel.tpp.core.TeamSortingInput;

public class FriendshipObjectiveValues {

    protected int team;
    protected double objectiveValue;
    protected Friendship friendship;

    public FriendshipObjectiveValues(Friendship friendship, int t, TeamSortingInput input, int[] preferenceMultipliers) {
        team = t;
        this.friendship = friendship;
        int teamObjectiveValue = 0;
        for (Member m: friendship.getMembers()) {
            int pref = m.getPreference(input.getTeams()[t]); // 0 is best, input.numbPreferences() - 1 is worst
            if (pref >= 0) teamObjectiveValue += preferenceMultipliers[pref];
        }
        objectiveValue = (double)teamObjectiveValue / friendship.size();
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
