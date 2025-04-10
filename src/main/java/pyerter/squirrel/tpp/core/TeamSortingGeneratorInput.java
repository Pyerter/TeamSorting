package pyerter.squirrel.tpp.core;

import pyerter.squirrel.tpp.friendship.Friendship;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public class TeamSortingGeneratorInput {

    public static boolean _comepletelyRandomFriends = true;
    public static boolean _uniformRandomMMinusQ = true;

    public static TeamSortingInput generateInput(int memberCount, int teamCount, int roleCount, int preferenceCount, int roleReqLB, int roleReqUB, int minCountLB, int minCountUB, int memberRoleLB, int memberRoleUB) {
        return generateInput(memberCount, teamCount, roleCount, preferenceCount, roleReqLB, roleReqUB, minCountLB, minCountUB, memberRoleLB, memberRoleUB, 0, 0, 0);
    }

    public static TeamSortingInput generateInput(int memberCount, int teamCount, int roleCount, int preferenceCount, int roleReqLB, int roleReqUB, int minCountLB, int minCountUB, int memberRoleLB, int memberRoleUB, int numbFriendships, int friendshipSizeLB, int friendshipSizeUB) {

        // Ensure input is valid
        if (minCountUB * teamCount > memberCount) {
            System.out.println("Cannot randomize input where minimum member count per team upper bound would cause infeasible solutions.");
            System.out.printf("minCountUB (%d) * teamCount (%d) > memberCount (%d)", minCountUB, teamCount, memberCount);
            return null;
        }
        if (minCountLB > minCountUB) {
            System.out.println("Minimum Count Lower Bound must be less than Minimum Count Upper Bound");
            return null;
        }
        if (roleReqLB > roleReqUB) {
            System.out.println("Role Requirement Lower Bound must be less than Role Requirement Upper Bound");
            return null;
        }
        if (memberRoleLB > memberRoleUB) {
            System.out.println("Member Role Lower Bound must be less than Member Role Upper Bound");
            return null;
        }
        if (memberRoleUB > roleCount) {
            System.out.println("Cannot randomize input where member role upper bound is greater than the total number of roles.");
            return null;
        }

        int mmq = -1;int maxTRR = teamCount * roleCount * roleReqUB;
        int maxTC = teamCount * minCountUB;
        int maxF = numbFriendships * friendshipSizeUB;
        int minTRR = teamCount * roleCount * roleReqLB;
        int minTC = teamCount * minCountLB;
        int minF = numbFriendships * friendshipSizeLB;
        int maxMMQ = Math.max(Math.max(maxTRR, maxTC), maxF);
        int minMMQ = Math.max(Math.max(minTRR, minTC), minF);
        if (_uniformRandomMMinusQ) {
            mmq = (int)(Math.random() * (maxMMQ - minMMQ + 1)) + minMMQ;
        }

        // create members list (will store members later)
        List<Member> members = new ArrayList<>(memberCount);
        String[] teams = new String[teamCount]; // teams by number
        for (int i = 0; i < teams.length; i++) {
            teams[i] = "t" + (i + 1);
        }
        String[] roles = new String[roleCount]; // roles by number
        for (int i = 0; i < roles.length; i++) {
            roles[i] = "r" + (i + 1);
        }
        int requiredMembers = 0;
        int[][] teamRoleRequirements = new int[teamCount][roleCount]; // create mapping for number of roles per team
        int randRange;
        if (mmq < 0) { // if randomly generating
            randRange = roleReqUB - roleReqLB + 1;
            for (int t = 0; t < teamCount; t++) {
                for (int r = 0; r < roleCount; r++) {
                    teamRoleRequirements[t][r] = (int) (Math.random() * randRange) + roleReqLB; // randomly generated
                    requiredMembers += teamRoleRequirements[t][r];
                }
            }
            if (requiredMembers > memberCount) {
                memberCount = requiredMembers;
            }
            requiredMembers = 0;
        } else { // randomly generating under mmq
            randRange = teamCount * roleCount;
            for (int t = 0; t < teamCount; t++) {
                for (int r = 0; r < roleCount; r++) {
                    teamRoleRequirements[t][r] = roleReqLB;
                    requiredMembers += roleReqLB;
                }
            }
            int remainingMMQ = (int)(Math.random() * (Math.min(mmq, maxTRR) - requiredMembers + 1));
            while (remainingMMQ > 0) {
                int index = (int)(Math.random() * randRange);
                int tIndex = index % teamCount;
                int rIndex = index / teamCount;
                if (teamRoleRequirements[tIndex][rIndex] < roleReqUB) {
                    teamRoleRequirements[tIndex][rIndex]++;
                    remainingMMQ--;
                }
            }
            requiredMembers = 0;
        }

        // set min team counts
        // max between number of required roles and a random min team requirement
        int[] minMemberCount = new int[teamCount];
        if (mmq < 0) {
            randRange = minCountUB - minCountLB + 1;
            for (int t = 0; t < teamCount; t++) {
                int totalRoleRequirement = Arrays.stream(teamRoleRequirements[t]).reduce(0, Integer::sum);
                minMemberCount[t] = Math.max(totalRoleRequirement, (int) (Math.random() * randRange) + minCountLB);
                requiredMembers += minMemberCount[t];
            }
            if (requiredMembers > memberCount) {
                memberCount = requiredMembers;
            }
            requiredMembers = 0;
        } else {
            randRange = teamCount;
            for (int t = 0; t < teamCount; t++) {
                minMemberCount[t] = minCountLB;
                requiredMembers += minCountLB;
            }
            int remainingMMQ = Math.min(mmq, maxTC) - requiredMembers;
            while (remainingMMQ > 0) {
                int index = (int)(Math.random() * randRange);
                if (minMemberCount[index] < minCountUB) {
                    minMemberCount[index]++;
                    remainingMMQ--;
                }
            }
            requiredMembers = 0;
        }

        int o = 0; // total team-specific assignments needed
        int t = 0; // current team
        int r = 0; // current role
        int c = 0; // current count of team assignments for team t
        randRange = memberRoleUB - memberRoleLB + 1;
        List<Integer> friendshipSplits = new ArrayList<>();
        for (int m = 0; m < memberCount; m++) {
            // randomly select number of roles for this member
            int numbRoles = (int)(Math.random() * randRange) + memberRoleLB;
            String[] mRoles; // roles for this member
            String[] mPrefs; // preferences for this member
            if (numbRoles > 0) { // shuffle the roles list then store number of roles into member's list
                List<String> list = new ArrayList<String>(List.of(roles));
                Collections.shuffle(list);
                mRoles = list.subList(0, numbRoles).toArray(String[]::new);
            } else {
                mRoles = new String[0]; // no roles!
            }
            List<String> list = new ArrayList<>(List.of(teams)); // shuffle teams for preferences
            Collections.shuffle(list);
            mPrefs = list.subList(0, preferenceCount).toArray(String[]::new); // select preferences for teams
            //System.out.printf("t(%d),r(%d),c(%d)%n", t, r, c);
            if (t < teamCount && teamRoleRequirements[t][r] > c) { // if we're still generating valid input for teams
                if (numbRoles > 0) { // confirm that the required role is filled by this member
                    boolean contained = false; // check if contains
                    for (int i = 0; i < mRoles.length; i++) {
                        if (mRoles[i].equalsIgnoreCase(roles[r])) {
                            contained = true;
                            break;
                        }
                    }
                    if (!contained) // if not contained, set 1 randomly as the role
                        mRoles[(int)(Math.random() * numbRoles)] = roles[r];
                } else { // if no roles, make sure valid role is assigned
                    numbRoles = 1;
                    mRoles = new String[]{ roles[r] };
                }
                boolean contained = false; // check if preferences contains the target team
                for (int i = 0; i < mPrefs.length; i++) {
                    if (mPrefs[i].equalsIgnoreCase(teams[t])) {
                        contained = true;
                        break;
                    }
                }
                if (!contained && preferenceCount > 0) // if not, randomly swap a preference for the team
                    mPrefs[(int)(Math.random() * preferenceCount)] = teams[t];
                c++;

                // update counters
                if (teamRoleRequirements[t][r] <= c) {
                    c = 0;
                    r++;
                    if (r >= roleCount) {
                        r = 0;
                        friendshipSplits.add(m + 1);
                        t++;
                    }
                }
            }
            Member member = new Member("m" + m, mRoles, mPrefs);
            members.add(member); // add member
        }
        Friendship[] friendships;
        if (numbFriendships > 0) {
            System.out.printf("Friendship splits: %s%n", Arrays.toString(friendshipSplits.toArray()));
            friendships = new Friendship[numbFriendships];
            int[] friendshipSizes = new int[numbFriendships];
            if (mmq < 0) {
                for (int f = 0; f < numbFriendships; f++) {
                    friendshipSizes[f] = (int)(Math.random()*(friendshipSizeUB - friendshipSizeLB + 1)) + friendshipSizeLB;
                }
            } else {
                randRange = numbFriendships;
                for (int f = 0; f < numbFriendships; f++) {
                    friendshipSizes[f] = friendshipSizeLB;
                    requiredMembers += friendshipSizeLB;
                }
                int remainingMMQ = (int)(Math.random() * (Math.min(mmq, maxF) - requiredMembers + 1));
                while (remainingMMQ > 0) {
                    int index = (int)(Math.random() * randRange);
                    if (friendshipSizes[index] < friendshipSizeUB) {
                        friendshipSizes[index]++;
                        remainingMMQ--;
                    }
                }
            }
            for (int i = 0; i < numbFriendships; i++) {
                int split = (int)(Math.random() * (friendshipSplits.size()));
                if (friendshipSplits.get(split) >= members.size()) split--;
                int splitStart;
                int splitEnd;
                if (_comepletelyRandomFriends) {
                    splitStart = 0;
                    splitEnd = memberCount;
                } else {
                    splitStart = split > 0 ? friendshipSplits.get(split - 1) : 0;
                    splitEnd = friendshipSplits.get(split);
                }
                Integer[] possibleValues = IntStream.rangeClosed(splitStart, splitEnd - 1).boxed().toArray( Integer[]::new );
                List<Integer> values = new ArrayList<>(List.of(possibleValues));
                if (!_comepletelyRandomFriends && split < friendshipSplits.size() - 1) {
                    Integer[] anyValues = IntStream.rangeClosed(friendshipSplits.get(friendshipSplits.size() - 1), members.size() - 1).boxed().toArray(Integer[]::new);
                    values.addAll(List.of(anyValues));
                }
                Collections.shuffle(values);
                int[] friends = new int[Math.min(friendshipSizes[i], values.size())];
                String[] friendNames = new String[friends.length];
                for (int f = 0; f < friends.length; f++) {
                    friends[f] = values.get(f);
                    friendNames[f] = members.get(friends[f]).getName();
                }
                boolean skip = false;
                for (int f = 0; f < i; f++) {
                    for (String m: friendships[f].getFriends()) {
                        for (int j = 0; j < friendNames.length; j++) {
                            if (m.equalsIgnoreCase(friendNames[j])) {
                                skip = true;
                                System.out.println("Got repeated friend name- recreating friendship.");
                                break;
                            }
                        }
                        if (skip) break;
                    }
                    if (skip) break;
                }
                if (skip) {
                    i--;
                    continue;
                }
                Friendship friendship = new Friendship(String.format("Friendship{%s}", Arrays.toString(friendNames)), friendNames);
                friendships[i] = friendship;
            }
        } else {
            friendships = new Friendship[0];
        }
        return new TeamSortingInput(members, teams, roles, preferenceCount, teamRoleRequirements, minMemberCount, friendships);
    }


}
