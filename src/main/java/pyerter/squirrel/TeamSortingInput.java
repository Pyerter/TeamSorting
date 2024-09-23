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
    protected int[] preferenceMultipliers;

    public TeamSortingInput(List<Member> members, String[] teams, String[] roles, int[] preferenceMultipliers) {
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
        this.teamRoleRequirements = new int[numbRoles()][numbTeams()];
        colsOfRole = new int[numbRoles()][];
        colsOfTeam = new int[numbTeams()][];
        int[] numbColsOfRole = new int[numbRoles()];
        int[] numbColsOfTeam = new int[numbTeams()];
        for (int i = 0; i < teamRoleRequirements.length; i++) {
            Arrays.fill(teamRoleRequirements[i], 1);
            for (int j = 0; j < teamRoleRequirements[i].length; j++) {
                for (int req = 0; req < teamRoleRequirements[i][j]; req++) {
                    numbColsOfTeam[i]++;
                    numbColsOfRole[j]++;
                }
            }
        }
        for (int i = 0; i < numbColsOfRole.length; i++) {
            colsOfRole[i] = new int[numbColsOfRole[i]];
            numbColsOfRole[i] = 0; // now use as a counter to indicate next point in array while adding indexes
        }
        for (int i = 0; i < numbColsOfTeam.length; i++) {
            colsOfTeam[i] = new int[numbColsOfTeam[i]];
            numbColsOfTeam[i] = 0; // now use as a counter to indicate next point in array while adding indexes
        }
        this.augmentedTeamRoleIndex = new HashMap<>();
        jToRole = new HashMap<>();
        jToTeam = new HashMap<>();
        int colIndex = 0;
        for (int i = 0; i < teamRoleRequirements.length; i++) {
            for (int j = 0; j < teamRoleRequirements[i].length; j++) {
                int teamRoleIndex = i * numbRoles() + j;
                for (int numbRole = 0; numbRole < teamRoleRequirements[i][j]; numbRole++) {
                    augmentedTeamRoleIndex.put(teamRoleIndex, colIndex);
                    colIndex++;
                    colsOfRole[j][numbColsOfRole[j]] = colIndex;
                    colsOfTeam[i][numbColsOfTeam[i]] = colIndex;
                }
                jToTeam.put(teamRoleIndex, i);
                jToRole.put(teamRoleIndex, j);
            }
        }
        this.numbExplicitTeamRoles = colIndex;
        this.preferenceMultipliers = preferenceMultipliers;
        this.numbPreferences = preferenceMultipliers.length;
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

    public int[] getPreferenceMultipliers() {
        return this.preferenceMultipliers;
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

    public int getMinMembersPerTeam() {
        return minMembersPerTeam;
    }

}
