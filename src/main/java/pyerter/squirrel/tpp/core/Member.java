package pyerter.squirrel.tpp.core;

import java.util.Map;

/**
 * Author: Porter Squires
 * License: MIT License
 *
 *
 */
public class Member {

    protected String name;
    protected String[] preferredTeams;
    protected String[] roles;

    public Member(String name, String[] roles, String ... preferredTeams) {
        this.name = name;
        this.preferredTeams = preferredTeams;
        this.roles = roles;
    }

    public String getName() {
        return name;
    }

    public String[] getPreferredTeams() {
        return preferredTeams;
    }

    public String[] getRoles() {
        return roles;
    }

    public int[] getTeamEncodings(Map<String, Integer> mapping) {
        int[] encodings = new int[preferredTeams.length];
        for (int i = 0; i < encodings.length; i++) {
            encodings[i] = mapping.get(preferredTeams[i]);
        }
        return encodings;
    }

    public int[] getRoleEncodings(Map<String, Integer> mapping) {
        int[] encodings = new int[roles.length];
        for (int i = 0; i < encodings.length; i++) {
            encodings[i] = mapping.get(roles[i]);
        }
        return encodings;
    }

    public int getPreference(String team) {
        for (int i = 0; i < preferredTeams.length; i++) {
            if (preferredTeams[i].equalsIgnoreCase(team)) {
                return i;
            }
        }
        return -1;
    }

}
