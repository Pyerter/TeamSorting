package pyerter.squirrel;

import pyerter.squirrel.tpp.*;
import pyerter.squirrel.tpp.io.TeamSorterInputReadingException;
import pyerter.squirrel.tpp.core.TeamSorterResult;
import pyerter.squirrel.tpp.core.TeamSorterSolver;
import pyerter.squirrel.tpp.core.TeamSortingInput;
import pyerter.squirrel.tpp.io.CsvReader;
import pyerter.squirrel.tpp.friendship.TeamSorterBestFitFriendshipSolver;
import pyerter.squirrel.tpp.friendship.TeamSorterFriendshipSolver;

/**
 * Author: Porter Squires
 * License: MIT License
 *
 *
 */
public class Main {
    static boolean useFriendship = false;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Expected one argument: filepath to .csv file.");
            return;
        }
        try {
            TeamSortingInput input;
            TeamSortingLogger logger = new TeamSortingLogger(3);
            if (args[0].equalsIgnoreCase("random")) {
                input = TeamSortingInput.generateInput(Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]),
                        Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]),
                        Integer.parseInt(args[7]), Integer.parseInt(args[8]), Integer.parseInt(args[0]), Integer.parseInt(args[10]));
            } else {
                input = CsvReader.readProblemInputCsv(args[0]);
            }
            if (false) {
                TeamSorterBestFitFriendshipSolver solver2 = new TeamSorterBestFitFriendshipSolver(input);
                solver2.solve();
                return;
            }
            boolean useIntegral = false;//input.numbFriendships() > 0;
            TeamSorterSolver solver = !useFriendship ? new TeamSorterSolver(input, useIntegral) : new TeamSorterFriendshipSolver(input, useIntegral);
            solver.setUseHardPreferenceObjectiveFunction(true);
            solver.setIgnoreFriendships(true);
            TeamSorterResult result = solver.solve(logger);

            logger.log(result.toPrintAssignments(), 3);
            if (useIntegral && !useFriendship) {
                TeamSorterSolver fractionalSolver = new TeamSorterSolver(input, false);
                TeamSorterResult fractResult = fractionalSolver.solve(logger);
                logger.log("Fractional " + fractResult.toPrintStats());
                logger.log(fractResult.toPrintFinalPreferences());
            }
            logger.log("Result " + result.toPrintStats());
            logger.log(result.toPrintFinalPreferences());
            logger.log(result.toPrintFinalAssignments(), 1);
        } catch (TeamSorterInputReadingException e) {
            System.out.println(e.getMessage());
            if (args.length > 1 && args[1].equalsIgnoreCase("--debug=true")) {
                e.getChildException().printStackTrace();
            } else {
                System.out.println("To print exception, run with --debug=True");
            }
        }
    }
}