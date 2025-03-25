package pyerter.squirrel;

import pyerter.squirrel.tpp.*;
import pyerter.squirrel.tpp.core.TeamSortingGeneratorInput;
import pyerter.squirrel.tpp.io.CsvResultWriter;
import pyerter.squirrel.tpp.io.TeamSorterInputReadingException;
import pyerter.squirrel.tpp.core.TeamSorterResult;
import pyerter.squirrel.tpp.core.TeamSorterSolver;
import pyerter.squirrel.tpp.core.TeamSortingInput;
import pyerter.squirrel.tpp.io.CsvReader;
import pyerter.squirrel.tpp.friendship.TeamSorterFriendshipSolverOld;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Author: Porter Squires
 * License: MIT License
 *
 *
 */
public class MainFriendship {
    static boolean useFriendship = true;

    public static void main(String[] args) {
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


            TeamSorterSolver solver = new TeamSorterSolver(input, false);
            solver.setUseHardPreferenceObjectiveFunction(false);
            solver.setIgnoreFriendships(false);
            solver.setRoundFriendships(true);
            TeamSorterResult result;
            TeamSorterResult roundedResult;
            boolean caughtFailure = false;
            String failureMessage = "";
            try {
                logger = new TeamSortingLogger(2);
                result = solver.solve(logger);
                roundedResult = result.getRoundedResult();
            } catch (Exception e) {
                solver.setUseHardPreferenceObjectiveFunction(false);
                solver.setIgnoreFriendships(true);
                solver.setRoundFriendships(true);
                logger = new TeamSortingLogger(3);
                result = solver.solve(logger);
                roundedResult = result.getRoundedResult();
                caughtFailure = true;
                failureMessage = e.getMessage();
                //e.printStackTrace();
            }

            boolean usingRounded = roundedResult != null;

            logger.log("Direct Result - - - Assignment Values\n" + result.toPrintAssignments(), 3);
            logger.log("Direct Result - - -\n" + result.toPrintFinalAssignments(), 1);
            if (usingRounded) logger.log("Rounded Result - - -\n" + roundedResult.toPrintFinalAssignments(), 1);
            logger.log("Direct Result - - -\n" + result.toPrintFinalFriendshipAssignments(), 1);
            if (usingRounded) logger.log("Rounded Result - - -\n" + roundedResult.toPrintFinalFriendshipAssignments(), 1);
            logger.log("Direct Result - - -\n" + result.toPrintStats());
            logger.log(result.toPrintFinalPreferences());
            logger.log(result.toPrintFinalFriendshipObjective());
            if (usingRounded) logger.log("Rounded Result - - -\n" + roundedResult.toPrintStats());
            if (usingRounded) logger.log(roundedResult.toPrintFinalPreferences());
            if (usingRounded) logger.log(roundedResult.toPrintFinalFriendshipObjective());
            if (usingRounded) {
                logger.log(String.format("Rounding improvement (friendship objective): %.3f", (roundedResult.getFinalFriendshipObjectiveValue() - result.getFinalFriendshipObjectiveValue())));
                logger.log(String.format("Rounding improvement   (standard objective): %.3f", (roundedResult.getObjectiveValue() - result.getObjectiveValue())));
                logger.log(String.format("Maximum value        (friendship objective): %.3f", (result.getTheoreticalMaxFriendshipObjectiveValue())));
                logger.log(String.format("Maximum value          (standard objective): %.3f", (result.getTheoreticalMaxObjectiveValue())));
            }
            if (caughtFailure) {
                System.out.println("--------- Original attempt failed, retried with rounding algorithm on LP without friendship constraints");
                System.out.printf("Failure Message: %s%n", failureMessage);
            }

            CsvResultWriter.targetResultDirectoryName = "program_output";
            String outputFile = CsvResultWriter.writeResultToFile(result, "result");
            if (usingRounded) CsvResultWriter.writeResultToFile(roundedResult, "result_rounded");
            CsvReader.writeProblemInputCsv(input, "result_input");
            System.out.println("Wrote result to file: " + outputFile);
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