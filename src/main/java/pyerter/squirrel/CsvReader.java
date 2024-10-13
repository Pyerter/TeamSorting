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

        TeamSortingInput sortingInput = readProblemInputCsv(filePath, nameColumn, preferences, preferenceColumns, rolesColumn);
        if (sortingInput != null) {
            System.out.println("Members (M): " + sortingInput.toPrintMemberNames());
            System.out.println("Teams (T): " + sortingInput.toPrintTeams());
            System.out.println("Roles (R): " + sortingInput.toPrintRoles());
            System.out.println("Preferences (pm): " + sortingInput.toPrintMemberPreferences());
            System.out.println("Member Roles (rm): " + sortingInput.toPrintMemberRoles());
            System.out.println("Team role requirements (rt) format, where each inner array is length of roles, each number corresponds to the number of that role required: {[# # # #] [# # # #]}");
        }
    }

    public static TeamSortingInput readProblemInputCsv(String filePath, int nameColumn, int preferences, int[] preferenceColumns, int rolesColumn) {
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
            TeamSortingInput sortingInput = new TeamSortingInput(members, teams, roles, 3);
            return sortingInput;
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } catch (CsvValidationException e) {
            System.out.println("CsvValidationException: " + e.getMessage());
        }
        return null;
    }

}
