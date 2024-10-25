package pyerter.squirrel;

/**
 * Author: Porter Squires
 * License: MIT License
 *
 *
 */
public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Expected one argument: filepath to .csv file.");
            return;
        }
        try {
            TeamSortingLogger logger = new TeamSortingLogger(2);
            TeamSortingInput input = CsvReader.readProblemInputCsv(args[0]);
            boolean useIntegral = input.numbFriendships() > 0;
            TeamSorterSolver solver = new TeamSorterSolver(input, useIntegral);
            TeamSorterResult result = solver.solve(logger);
            logger.log(result.toPrintAssignments(), 3);
            if (useIntegral) {
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