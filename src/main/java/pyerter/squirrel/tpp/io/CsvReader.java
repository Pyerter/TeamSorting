package pyerter.squirrel.tpp.io;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import pyerter.squirrel.tpp.core.TeamSortingInput;
import pyerter.squirrel.tpp.friendship.Friendship;
import pyerter.squirrel.tpp.core.Member;
import pyerter.squirrel.tpp.friendship.TeamSortingFriendshipInput;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Author: Porter Squires
 * License: MIT License
 *
 *
 */
public class CsvReader {

    public static void main(String[] args) {
        if (args.length < 1)  {
            System.out.println("No file input provided!");
            return;
        } else {
            System.out.println("File: " + args[0]);
        }
        String filePath = args[0];

        try {
            TeamSortingInput sortingInput = readProblemInputCsv(filePath);
            System.out.println("Members (M): " + sortingInput.toPrintMemberNames());
            System.out.println("Teams (T): " + sortingInput.toPrintTeams());
            System.out.println("Roles (R): " + sortingInput.toPrintRoles());
            System.out.println("Preferences (pm): " + sortingInput.toPrintMemberPreferences());
            System.out.println("Member Roles (rm): " + sortingInput.toPrintMemberRoles());
            System.out.println("Team role requirements (rt): " + sortingInput.toPrintTeamRoleRequirements());
        } catch (TeamSorterInputReadingException e) {
            System.out.println("Exception while reading csv file: " + e.getMessage());
            if (e.hasChildException()) {
                e.getChildException().printStackTrace();
            }
        }
    }

    public static TeamSortingInput readProblemInputCsv(String filePath) throws TeamSorterInputReadingException {
        return readProblemInputCsv(filePath, false);
    }

    public static TeamSortingInput readProblemInputCsv(String filePath, boolean createFriendshipInput) throws TeamSorterInputReadingException {
        // values depending on the layout of the csv
        int countsRow = 1;
        int teamReqRowStart = 3; // >countsRow
        int teamCount;
        int roleCount;
        int prefCount;
        int memberCount;
        int friendshipCount;
        String[] roles;
        String[] teams;
        int[] minimumMemberCounts;
        List<Member> members;
        Friendship[] friendships;

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] nextLine;

            // Read counts of teams, roles, prefs, members
            reader.readNext(); // 1
            nextLine = reader.readNext(); // 2
            teamCount = Integer.parseInt(nextLine[0]);
            roleCount = Integer.parseInt(nextLine[1]);
            prefCount = Integer.parseInt(nextLine[2]);
            memberCount = Integer.parseInt(nextLine[3]);
            try {
                friendshipCount = Integer.parseInt(nextLine[4]);
            } catch (Exception e) {
                friendshipCount = 0;
            }
            roles = new String[roleCount];
            teams = new String[teamCount];
            members = new ArrayList<>(memberCount);
            friendships = new Friendship[friendshipCount];
            minimumMemberCounts = new int[teamCount];

            // Read names of roles in row 4
            reader.readNext(); // 3
            nextLine = reader.readNext(); // 4
            for (int i = 0; i < roleCount; i++) {
                roles[i] = nextLine[i + 1]; // first column is label, so add 1
            }

            int[][] roleRequirements = new int[teamCount][roleCount];

            // Read role requirements of each team
            for (int i = 0; i < teamCount; i++) {
                nextLine = reader.readNext(); // 4 + i + 1
                teams[i] = nextLine[0];
                for (int j = 0; j < roleCount; j++) {
                    roleRequirements[i][j] = Integer.parseInt(nextLine[j + 1]); // first column is label
                }
                minimumMemberCounts[i] = Integer.parseInt(nextLine[roleCount + 2]);
            }
            // previous line: 4 + |T|

            // Read rows containing the members
            reader.readNext(); // 5 + |T|
            reader.readNext(); // 6 + |T| header row for member section
            for (int i = 0; i < memberCount; i++) {
                nextLine = reader.readNext(); // 6 + |T| + i + 1
                //System.out.println("Reading member: " + Arrays.toString(nextLine));

                String[] memberPrefs = new String[prefCount];
                System.arraycopy(nextLine, 1, memberPrefs, 0, prefCount);
                int memberRolesCounted = 0;
                for (int j = 1 + prefCount; j < 1 + prefCount + roleCount; j++) {
                    if (nextLine[j].equalsIgnoreCase("Y")) {
                        memberRolesCounted++;
                    }
                }
                String[] memberRoles = new String[memberRolesCounted];
                int r = 0;
                for (int j = 1 + prefCount; j < 1 + prefCount + roleCount; j++) {
                    if (nextLine[j].equalsIgnoreCase("Y")) {
                        int roleIndex = j - 1 - prefCount;
                        memberRoles[r] = roles[roleIndex];
                        r++;
                    }
                }
                Member m = new Member(nextLine[0], memberRoles, memberPrefs);
                members.add(m);
            }
            // previous line: 6 + |T| + |M|

            if (friendshipCount > 0) {
                try {
                    nextLine = reader.readNext(); // 7 + |T| + |M|
                    nextLine = reader.readNext(); // 8 + |T| + |M|: Header row for friendship section
                    for (int i = 0; i < friendshipCount; i++) {
                        nextLine = reader.readNext();
                        String friendshipName = nextLine[0];
                        int j = 1;
                        while (j < nextLine.length && !nextLine[j].isEmpty()) {
                            j++;
                        }
                        String[] friends = new String[j - 1];
                        j = 0;
                        while (j < friends.length) {
                            friends[j] = nextLine[j + 1];
                            j++;
                        }
                        friendships[i] = new Friendship(friendshipName, friends);
                    }
                } catch (Exception e) {
                    System.out.printf("Err - could not read %d friendship groups from csv, continuing without friendships.%n", friendshipCount);
                    friendships = new Friendship[0];
                    friendshipCount = 0;
                }
            }

            if (!createFriendshipInput) {
                TeamSortingInput sortingInput = new TeamSortingInput(members, teams, roles, prefCount, roleRequirements, minimumMemberCounts, friendships);
                return sortingInput;
            }
            TeamSortingInput sortingInput = new TeamSortingFriendshipInput(members, teams, roles, prefCount, roleRequirements, minimumMemberCounts, friendships);
            return sortingInput;
        } catch (IOException e) {
            throw new TeamSorterInputReadingException("IOException: " + e.getMessage(), e);
        } catch (CsvValidationException e) {
            throw new TeamSorterInputReadingException("CsvValidationException: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new TeamSorterInputReadingException("Other exception while parsing: " + e.getMessage(), e);
        }
    }

}
