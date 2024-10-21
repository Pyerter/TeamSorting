package pyerter.squirrel;

import java.util.*;

public class TeamSortingInput {

    protected List<Member> members;
    protected Map<String, Integer> teamMap;
    protected Map<String, Integer> roleMap;
    protected String[] teams;
    protected String[] roles;
    protected String[] memberNames;
    protected int minMembersPerTeam;
    protected int[][] teamRoleRequirements;
    protected Map<Integer, Integer> augmentedTeamRoleIndex;
    protected int numbExplicitTeamRoles;
    protected Map<Integer, Integer> jToTeam;
    protected Map<Integer, Integer> jToRole;
    protected int[][] colsOfTeam;
    protected int[][] colsOfRole;
    protected int numbPreferences;

    public TeamSortingInput(List<Member> members, String[] teams, String[] roles, int numbPreferences, int[][] teamRoleRequirements, int[] minimumMemberCounts) {
        this.members = members;
        this.teamMap = new HashMap<>();
        for (int i = 0; i < teams.length; i++) {
            teamMap.put(teams[i], i);
        }
        this.roleMap = new HashMap<>();
        for (int i = 0; i < roles.length; i++) {
            roleMap.put(roles[i], i);
        }
        this.teams = teams;
        this.roles = roles;
        this.memberNames = calculateMemberNames();
        this.minMembersPerTeam = 5;
        this.teamRoleRequirements = teamRoleRequirements;
        colsOfRole = new int[roles.length][];
        colsOfTeam = new int[teams.length][];
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
        for (int i = 0; i < numbColsOfTeam.length; i++) {
            colsOfTeam[i] = new int[Math.max(numbColsOfTeam[i], minimumMemberCounts[i])];
            numbColsOfTeam[i] = 0; // now use as a counter to indicate next point in array while adding indexes
        }
        this.augmentedTeamRoleIndex = new HashMap<>();
        jToRole = new HashMap<>();
        jToTeam = new HashMap<>();
        int colIndex = 0;
        int[] teamMinMemberColumns = new int[teams.length];
        for (int i = 0; i < teamRoleRequirements.length; i++) {
            for (int j = 0; j < teamRoleRequirements[i].length; j++) {
                int teamRoleIndex = i * numbRoles() + j;
                augmentedTeamRoleIndex.put(teamRoleIndex, colIndex);
                for (int numbRole = 0; numbRole < teamRoleRequirements[i][j]; numbRole++) {
                    colsOfRole[j][numbColsOfRole[j]] = colIndex;
                    colsOfTeam[i][numbColsOfTeam[i]] = colIndex;
                    numbColsOfRole[j]++;
                    numbColsOfTeam[i]++;
                    jToTeam.put(colIndex, i);
                    jToRole.put(colIndex, j);
                    teamMinMemberColumns[i]++;
                    colIndex++;
                }
            }
        }
        for (int i = 0; i < teamMinMemberColumns.length; i++) {
            while (teamMinMemberColumns[i] < minimumMemberCounts[i]) {
                colsOfTeam[i][numbColsOfTeam[i]] = colIndex;
                numbColsOfTeam[i]++;
                jToTeam.put(colIndex, i);
                teamMinMemberColumns[i]++;
                colIndex++;
            }
        }
        this.numbExplicitTeamRoles = colIndex;
        this.numbPreferences = numbPreferences;
    }

    public int numbMembers() {
        return members.size();
    }

    public int numbTeams() {
        return teams.length;
    }

    public int numbRoles() {
        return roles.length;
    }

    public int numbAugmentedTeamRoles() {
        return numbExplicitTeamRoles;
    }

    public int numbAugmentedColumns() {
        return members.size();
    }

    public int numbRows() {
        return members.size();
    }

    public List<Member> getMembers() {
        return members;
    }

    public Member getMember(int i) {
        return members.get(i);
    }

    public Map<String, Integer> getTeamMap() {
        return teamMap;
    }

    public Map<String, Integer> getRoleMap() {
        return roleMap;
    }

    public String[] getTeams() {
        return teams;
    }

    public String[] getRoles() {
        return roles;
    }

    public int getJToTeam(int j) {
        Integer t = jToTeam.get(j);
        return t != null ? t : -1;
    }

    public int getJToRole(int j) {
        Integer r = jToRole.get(j);
        return r != null ? r : -1;
    }

    public int[] getColsOfTeam(int t) {
        return colsOfTeam[t];
    }

    public int[] getColsOfRole(int r) {
        return colsOfRole[r];
    }

    public int[] getColsOfTeamRole(int t, int r) {
        List<Integer> intersection = new ArrayList<>();
        int[] teamCols = colsOfTeam[t];
        int[] roleCols = colsOfRole[r];
        int currentT = 0;
        int currentR = 0;
        while (currentT < teamCols.length && currentR < roleCols.length) {
            if (teamCols[currentT] == roleCols[currentR]) {
                currentT++;
                currentR++;
                intersection.add(teamCols[currentT]);
            } else if (teamCols[currentT] < roleCols[currentR]) {
                currentR++;
            } else {
                currentT++;
            }
        }
        int[] result = new int[intersection.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = intersection.get(i);
        }
        return result;
    }

    public int getNumbPreferences() {
        return this.numbPreferences;
    }

    public String[] calculateMemberNames() {
        return members.stream().map(Member::getName).toArray(String[]::new);
    }

    public String[] getMemberNames() {
        return memberNames;
    }

    public String toPrintMemberNames() {
        return Arrays.toString(Arrays.stream(getMemberNames()).map(m -> "\"" + m + "\"").toArray(String[]::new)).replace(",", "");
    }

    public int[] getMemberPreferences(int member) {
        return members.get(member).getTeamEncodings(teamMap);
    }

    public int[][] getMemberPreferences() {
        return members.stream().map((m) -> m.getTeamEncodings(teamMap)).toArray(int[][]::new);
    }

    public String toPrintMemberPreferences() {
        return Arrays.toString(
                members.stream()
                        .map((m) -> m.getTeamEncodings(teamMap))
                        .map(m -> { // increment values by 1 for matlab output
                            for (int i = 0; i < m.length; i++)
                                m[i]++;
                            return m;
                        })
                        .map(Arrays::toString).toArray(String[]::new)
        ).replace(",", "");
    }

    public int[][] getMemberRoles() {
        return members.stream().map((m) -> m.getRoleEncodings(roleMap)).toArray(int[][]::new);
    }

    public int[] getMemberRoles(int member) {
        return members.get(member).getRoleEncodings(roleMap);
    }

    public int getTeamRoleRequiremenet(int t, int r) {
        return teamRoleRequirements[t][r];
    }

    public int getTeamMinimumMembers(int t) {
        return colsOfTeam[t].length;
    }

    public String toPrintMemberRoles() {
        return Arrays.toString(
                members.stream()
                        .map((m) -> m.getRoleEncodings(roleMap))
                        .map(m -> { // increment values by 1 for matlab output
                            for (int i = 0; i < m.length; i++)
                                m[i]++;
                            return m;
                        })
                        .map(Arrays::toString).toArray(String[]::new)
        ).replace(",", "");
    }

    public String toPrintTeams() {
        return Arrays.toString(Arrays.stream(teams).map(a -> "\"" + a + "\"").toArray(String[]::new)).replace(",", "");
    }

    public String toPrintRoles() {
        return Arrays.toString(Arrays.stream(roles).map(a -> "\"" + a + "\"").toArray(String[]::new)).replace(",", "");
    }

    public String toPrintTeamRoleRequirements() {
        String[] teamRoleReqStrings = new String[teamRoleRequirements.length];
        for (int i = 0; i < teamRoleReqStrings.length; i++) {
            teamRoleReqStrings[i] = Arrays.toString(Arrays.stream(teamRoleRequirements[i]).mapToObj(String::valueOf).map(a -> "\"" + a + "\"").toArray(String[]::new)).replace(",", "");
        }
        String returnString = Arrays.toString(teamRoleReqStrings);
        return "{" + returnString.substring(1, returnString.length() - 1).replace(",", "") + "}";
    }

    public int getMinMembersPerTeam() {
        return minMembersPerTeam;
    }

}
