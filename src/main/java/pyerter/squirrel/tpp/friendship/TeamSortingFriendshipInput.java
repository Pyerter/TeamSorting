package pyerter.squirrel.tpp.friendship;

import pyerter.squirrel.tpp.core.TeamSortingInput;
import pyerter.squirrel.tpp.core.Member;

import java.util.*;

/**
 * Author: Porter Squires
 * License: MIT License
 *
 *
 */
public class TeamSortingFriendshipInput extends TeamSortingInput {

    protected int o;
    protected int wildColumnCount;
    int[][] oColsOfTeam;

    public TeamSortingFriendshipInput(List<Member> members, String[] teams, String[] roles, int numbPreferences, int[][] teamRoleRequirements, int[] minimumMemberCounts) {
        this(members, teams, roles, numbPreferences, teamRoleRequirements, minimumMemberCounts, new Friendship[0]);
    }

    public TeamSortingFriendshipInput(List<Member> members, String[] teams, String[] roles, int numbPreferences, int[][] teamRoleRequirements, int[] minimumMemberCounts, Friendship[] friendships) {
        super(members, teams, roles, numbPreferences, teamRoleRequirements, minimumMemberCounts, friendships);
    }

    @Override
    protected void initializeColumnIndexes(int[] minimumMemberCounts) {
        int[] oOfTeams = new int[teams.length];
        colsOfRole = new int[roles.length][];
        colsOfTeam = new int[teams.length][];
        oColsOfTeam = new int[teams.length][];
        int[] numbColsOfRole = new int[numbRoles()];
        int[] numbColsOfTeam = new int[numbTeams()];
        for (int i = 0; i < teamRoleRequirements.length; i++) {
            for (int j = 0; j < teamRoleRequirements[i].length; j++) {
                numbColsOfTeam[i] += teamRoleRequirements[i][j];
                numbColsOfRole[j] += teamRoleRequirements[i][j];
            }
        }
        for (int i = 0; i < numbColsOfRole.length; i++) {
            colsOfRole[i] = new int[numbColsOfRole[i]];
            numbColsOfRole[i] = 0; // now use as a counter to indicate next point in array while adding indexes
        }
        this.o = 0;
        for (int i = 0; i < numbColsOfTeam.length; i++) {
            oOfTeams[i] = Math.max(numbColsOfTeam[i], minimumMemberCounts[i]);
            o += oOfTeams[i];
        }
        this.wildColumnCount = members.size() - o;
        for (int i = 0; i < numbColsOfTeam.length; i++) {
            colsOfTeam[i] = new int[oOfTeams[i] + this.wildColumnCount];
            oColsOfTeam[i] = new int[oOfTeams[i]];
            numbColsOfTeam[i] = 0; // now use as a counter to indicate next point in array while adding indexes
        }
        this.augmentedTeamRoleIndex = new HashMap<>();
        jToRole = new HashMap<>();
        jToTeam = new HashMap<>();
        int colIndex = 0;
        for (int i = 0; i < teamRoleRequirements.length; i++) {
            for (int j = 0; j < teamRoleRequirements[i].length; j++) {
                int teamRoleIndex = i * numbRoles() + j;
                augmentedTeamRoleIndex.put(teamRoleIndex, colIndex);
                for (int numbRole = 0; numbRole < teamRoleRequirements[i][j]; numbRole++) {
                    colsOfRole[j][numbColsOfRole[j]] = colIndex;
                    colsOfTeam[i][numbColsOfTeam[i]] = colIndex;
                    oColsOfTeam[i][numbColsOfTeam[i]] = colIndex;
                    numbColsOfRole[j]++;
                    numbColsOfTeam[i]++;
                    jToTeam.put(colIndex, i);
                    jToRole.put(colIndex, j);
                    colIndex++;
                }
            }
            while (numbColsOfTeam[i] < colsOfTeam[i].length) {
                colsOfTeam[i][numbColsOfTeam[i]] = colIndex;
                numbColsOfTeam[i]++;
                jToTeam.put(colIndex, i);
            }
        }
        this.numbExplicitTeamRoles = colIndex;
    }

    public int[] getOColsOfTeam(int t) {
        return oColsOfTeam[t];
    }

    public int getO() {
        return o;
    }

}
