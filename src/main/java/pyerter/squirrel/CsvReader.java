package pyerter.squirrel;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class CsvReader {

    public static void main(String[] args) {
        if (args.length < 1)  {
            System.out.println("No file input provided!");
            return;
        } else {
            System.out.println("File: " + args[0]);
        }
        String filePath = args[0];
        int nameColumn;
        int preferences;
        int[] preferenceColumns;
        int rolesColumn;
        if (args.length > 4) {
            nameColumn = Integer.parseInt(args[1]);
            preferences = Integer.parseInt(args[2]);
            preferenceColumns = new int[preferences];
            rolesColumn = Integer.parseInt(args[args.length - 1]);
            if (args.length == 5) {
                preferenceColumns[0] = Integer.parseInt(args[3]);
                for (int i = 1; i < preferences; i++) {
                    preferenceColumns[i] = preferenceColumns[0] + i;
                }
            } else if (args.length == 4 + preferences) {
                for (int i = 0; i < preferences; i++) {
                    preferenceColumns[i] = Integer.parseInt(args[i + 3]);
                }
            } else {
                System.out.println("Not enough preference columns given! Expected 1 or " + preferences + " but got " + (args.length - 4));
            }
        } else {
            System.out.println("Not enough arguments! Expecting at least 5 arguments [String: file, int: nameColumn, int: numbPreferences, [int: preferenceColumns], int: rolesColumn");
            System.out.println("Argument numbPreferences can either be a single number (representing the first column of preferences) or a number of separate int arguments equal to number of preferences");
            return;
        }

        Map<String, Integer> roleMap = new HashMap<>();
        Map<String, Integer> teamMap = new HashMap<>();
        String[] roles;
        String[] teams;
        List<Member> members = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] nextLine;

            String[] rolesRead = reader.readNext();
            String[] teamsRead = reader.readNext();
            int rolesEndIndex = 0;
            int teamsEndIndex = 0;
            for (int i = 0; i < rolesRead.length; i++) {
                if (!rolesRead[i].isEmpty()) rolesEndIndex++;
                else break;
            }
            for (int i = 0; i < teamsRead.length; i++) {
                if (!teamsRead[i].isEmpty()) teamsEndIndex++;
                else break;
            }
            roles = Arrays.copyOfRange(rolesRead, 1, rolesEndIndex);
            teams = Arrays.copyOfRange(teamsRead, 1, teamsEndIndex);
            for (int i = 0; i < roles.length; i++) {
                roleMap.put(roles[i], i + 1);
            }
            for (int i = 0; i < teams.length; i++) {
                teamMap.put(teams[i], i + 1);
            }

            String[] headerRow = reader.readNext();

            while ((nextLine = reader.readNext()) != null) {
                String name = nextLine[nameColumn];
                String[] preferredTeams = new String[preferences];
                int i = 0;
                for (int j: preferenceColumns) {
                    preferredTeams[i] = nextLine[j];
                    i++;
                }
                String[] mRoles = nextLine[rolesColumn].split(", ");
                Member m = new Member(name, mRoles, preferredTeams);
                members.add(m);
            }

            String[] memberNames = members.stream().map(Member::getName).map(m -> "\"" + m + "\"").toArray(String[]::new);
            //int[][] teamPreferences = members.stream().map((m) -> m.getTeamEncodings(teamMap)).toArray(int[][]::new);
            //int[][] memberRoles = members.stream().map((m) -> m.getRoleEncodings(roleMap)).toArray(int[][]::new);
            String[] teamPreferences = members.stream().map((m) -> m.getTeamEncodings(teamMap)).map(Arrays::toString).toArray(String[]::new);
            String[] memberRoles = members.stream().map((m) -> m.getRoleEncodings(roleMap)).map(Arrays::toString).toArray(String[]::new);
            System.out.println("Members: " + Arrays.toString(memberNames).replace(",", ""));
            System.out.println("Teams: " + Arrays.toString(Arrays.stream(teams).map(a -> "\"" + a + "\"").toArray(String[]::new)).replace(",", ""));
            System.out.println("Roles: " + Arrays.toString(Arrays.stream(roles).map(a -> "\"" + a + "\"").toArray(String[]::new)).replace(",", ""));
            System.out.println("Preferences (pm): " + Arrays.toString(teamPreferences).replace(",", ""));
            System.out.println("Member Roles (rm): " + Arrays.toString(memberRoles).replace(",", ""));
            System.out.println("Team role requirements (rt) format, where each inner array is length of roles, each number corresponds to the number of that role required: {[# # # #] [# # # #]}");
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } catch (CsvValidationException e) {
            System.out.println("CsvValidationException: " + e.getMessage());
        }
    }

}
