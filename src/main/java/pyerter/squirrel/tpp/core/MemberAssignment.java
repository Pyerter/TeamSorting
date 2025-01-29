package pyerter.squirrel.tpp.core;

import com.google.ortools.linearsolver.MPVariable;

import java.util.Arrays;

/**
 * Author: Porter Squires
 * License: MIT License
 *
 *
 */
public class MemberAssignment {
    public static final float TOLERANCE = 0.0000001f;
    protected int member;
    protected Member m;
    protected double[] teamAssignments;
    protected int[] assignedTeams;
    protected String[] assignedTeamNames;
    protected String[] assignedRoleNames;
    protected int positiveTeams;
    protected String singleTeamName;
    protected String singleRoleName;
    protected int finalTeamAssignment = -1;
    protected int finalTeamAssignmentRole = -1;
    protected String finalTeamAssignmentName = "";
    protected String finalTeamAssignmentRoleName = "";

    public MemberAssignment(TeamSortingInput input, int member, MPVariable[] memberRow, boolean detailedPrinting) {
        this.member = member;
        m = input.getMember(member);
        teamAssignments = new double[memberRow.length];
        int numbAssignedTeams = 0;
        for (int i = 0; i < memberRow.length; i++) {
            teamAssignments[i] = memberRow[i].solutionValue();
            if (teamAssignments[i] >= TOLERANCE) {
                numbAssignedTeams++;
            }
        }
        assignedTeams = new int[numbAssignedTeams];
        assignedTeamNames = new String[numbAssignedTeams];
        assignedRoleNames = new String[numbAssignedTeams];
        int current = 0;
        positiveTeams = 0;
        for (int i = 0; i < teamAssignments.length; i++) {
            if (teamAssignments[i] >= TOLERANCE) {
                assignedTeams[current] = i;
                int teamNumber = input.getJToTeam(i);
                assignedTeamNames[current] = teamNumber >= 0 ? input.getTeams()[teamNumber] : "<Any>";
                if (teamNumber >= 0 || detailedPrinting) {
                    positiveTeams++;
                }
                assignedRoleNames[current] = input.getJToRole(i) >= 0 ? input.getRoles()[input.getJToRole(i)] : "<Any>";
                current++;
            }
        }
        if (!detailedPrinting) {
            if (assignedTeams.length == 1 && positiveTeams == 0) {
                singleRoleName = assignedRoleNames[0];
                singleTeamName = String.format("<Any, Prefer %s>", m.getPreferredTeams()[0]);
            }
            else if (positiveTeams == 1) {
                int teamNumber = 0;
                for (int i = 0; i < teamAssignments.length; i++) {
                    if (teamAssignments[i] < TOLERANCE) continue;
                    teamNumber = input.getJToTeam(i);
                    if (teamNumber >= 0) {
                        singleRoleName = input.getJToRole(i) >= 0 ? input.getRoles()[input.getJToRole(i)] : "Any";
                        singleTeamName = input.getTeams()[teamNumber];
                        break;
                    }
                }
            } else if (positiveTeams == 0) {
                singleRoleName = "<Any>";
                singleTeamName = "<Any>";
            }
        }
    }

    public int getMemberIndex() {
        return member;
    }

    public double[] getValues() {
        return teamAssignments;
    }

    public double getValue(int col) {
        return teamAssignments[col];
    }

    public int[] getAssignedTeams() {
        return assignedTeams;
    }

    public Member getMember() {
        return m;
    }

    public int getAssignedTeam(TeamSortingInput input) {
        return assignedTeams.length > 0 ? input.getJToTeam(assignedTeams[0]) : -1;
    }

    public int getAssignedRole(TeamSortingInput input) {
        return assignedTeams.length > 0 ? input.getJToRole(assignedTeams[0]) : -1;
    }

    public void setFinalTeamAssignment(int finalTeamAssignment, TeamSortingInput input) {
        this.finalTeamAssignment = finalTeamAssignment;
        this.finalTeamAssignmentRole = getAssignedRole(input);
        this.finalTeamAssignmentName = input.getTeams()[finalTeamAssignment];
        this.finalTeamAssignmentRoleName = finalTeamAssignmentRole >= 0 ? input.getRoles()[finalTeamAssignmentRole] : "<Any>";
    }

    public int getFinalTeamAssignment() {
        return finalTeamAssignment;
    }

    public int getFinalTeamAssignmentRole() {
        return finalTeamAssignmentRole;
    }

    public String getFinalTeamAssignmentName() {
        return finalTeamAssignmentName;
    }

    public String getFinalTeamAssignmentRoleName() {
        return finalTeamAssignmentRoleName;
    }

    public String toPrintFinalAssignment() {
        if (finalTeamAssignment >= 0) {
            return String.format("Member (%d) %s assigned to %s as %s: Preference %d",
                    member, m.getName(), finalTeamAssignmentName, finalTeamAssignmentRoleName,
                    m.getPreference(finalTeamAssignmentName));
        }
        return "No final assignment set yet.";
    }

    public double getFinalAssignmentObjectiveValue(int[] preferenceMultipliers) {
        if (finalTeamAssignment < 0) {
            return 0;
        }
        int preference = m.getPreference(finalTeamAssignmentName);
        if (preference >= 0) {
            return preferenceMultipliers[preference];
        }
        return 0;
        //return -preferenceMultipliers[0];
    }

    public String toPrintAssignment() {
        if (assignedTeams.length == 0) return String.format("Member (%d) %s assigned to no team", member, m.getName());
        if (assignedTeams.length == 1) return String.format("Member (%d) %s assigned to %s as %s (x_%d=%f): Preference %d", member, m.getName(), singleTeamName, singleRoleName, assignedTeams[0], teamAssignments[assignedTeams[0]], m.getPreference(assignedTeamNames[0]) + 1);
        String result = String.format("Member (%d) %s assigned to ", member, m.getName());
        if (positiveTeams == 0 || positiveTeams == 1) {
            String[] teamAssignments = new String[assignedTeams.length];
            for (int i = 0; i < teamAssignments.length; i++) {
                teamAssignments[i] = String.format("(x_%d=%f, P%d)", assignedTeams[i], this.teamAssignments[assignedTeams[i]], m.getPreference(assignedTeamNames[i]) + 1);
            }
            result += String.format("%s as %s, split: %s", singleTeamName, singleRoleName,
                    Arrays.toString(teamAssignments));
        } else {
            String[] teams = new String[assignedTeams.length];
            for (int i = 0; i < teams.length; i++) {
                teams[i] = String.format("%s as %s (x_%d=%f, P%d)", assignedTeamNames[i], assignedRoleNames[i], assignedTeams[i], teamAssignments[assignedTeams[i]], m.getPreference(assignedTeamNames[i]) + 1);
            }
            result += "many teams: " + Arrays.toString(teams);
        }
        return result;
    }
}