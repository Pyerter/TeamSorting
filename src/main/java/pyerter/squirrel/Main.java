package pyerter.squirrel;

import pyerter.squirrel.tpp.TeamSortingLogger;
import pyerter.squirrel.tpp.core.TeamSorterResult;
import pyerter.squirrel.tpp.core.TeamSorterSolver;
import pyerter.squirrel.tpp.core.TeamSortingGeneratorInput;
import pyerter.squirrel.tpp.core.TeamSortingInput;
import pyerter.squirrel.tpp.io.CsvReader;
import pyerter.squirrel.tpp.io.TeamSorterInputReadingException;

import java.util.Arrays;
import java.util.stream.IntStream;

// This runs team sorting without the use of friendships
public class Main {

    static boolean useFriendship = false;
    static boolean useIntegral = false;

    public static void main(String[] args) {
        System.out.printf("---Running Team Sorting, not using friendships---%n");
        if (args.length < 1) {
            System.out.println("Expected one argument: filepath to .csv file.");
            return;
        }
        try {
            TeamSortingInput input;
            TeamSortingLogger logger = new TeamSortingLogger(2);
            if (args[0].equalsIgnoreCase("random") || args[0].equalsIgnoreCase("r")) {
                System.out.printf("Attempting to generate random input...%n");
                try {
                    input = TeamSortingGeneratorInput.generateInput(Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]),
                            Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]),
                            Integer.parseInt(args[7]), Integer.parseInt(args[8]), Integer.parseInt(args[9]), Integer.parseInt(args[10]));
                } catch (Exception e) {
                    System.out.printf("Random input should have the following arguments:%n" +
                            "<int:member count> <int:team count> <int:role count> <int: preference count> <int:members per role per team lower bound> <int:members per role per team upper bound> " +
                            "<int:min team count lower bound> <int:min team count upper bound> <int:roles per member lower bound> <int:roles per member upper bound>");
                    return;
                }
            } else {
                input = CsvReader.readProblemInputCsv(args[0]);
            }

            if (input == null) {
                System.out.printf("Err - when creating input, something errored. Input is null.%n");
                return;
            }

            System.out.printf("Teams: %s%n", input.toPrintTeams());
            System.out.printf("Roles: %s%n", input.toPrintRoles());
            System.out.printf("Members: %s%n", input.toPrintMemberNames());
            System.out.printf("Member Roles: %s%n", input.toPrintMemberRoles());
            System.out.printf("Member Preferences: %s%n", input.toPrintMemberPreferences());
            System.out.printf("Team Role Requirements: %s%n", input.toPrintTeamRoleRequirements());
            System.out.printf("Minimum Team Counts: %s%n", Arrays.toString(IntStream.rangeClosed(0, input.numbTeams() - 1).map(input::getTeamMinimumMembers).toArray()).replace(",", ""));

            logger.log(String.format("%nRunning solver..."), 0);

            TeamSorterSolver solver = new TeamSorterSolver(input, useIntegral);
            solver.setUseHardPreferenceObjectiveFunction(false);
            solver.setIgnoreFriendships(true);
            TeamSorterResult result = solver.solve(logger);

            logger.log(result.toPrintAssignments(), 3);

            logger.log("Result " + result.toPrintStats());
            logger.log(result.toPrintFinalPreferences());
            logger.log(result.toPrintFinalAssignments(), 1);
        } catch (TeamSorterInputReadingException e) {
            System.out.println(e.getMessage());
            if (args.length > 1 && args[args.length - 1].equalsIgnoreCase("--debug=true")) {
                e.getChildException().printStackTrace();
            } else {
                System.out.println("To print exception, run with --debug=True");
            }
        }
    }

}
