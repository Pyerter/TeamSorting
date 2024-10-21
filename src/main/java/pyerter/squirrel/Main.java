package pyerter.squirrel;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Expected one argument: filepath to .csv file.");
            return;
        }
        try {
            TeamSortingInput input = CsvReader.readProblemInputCsv(args[0]);
            TeamSorterSolver solver = new TeamSorterSolver(input);
            solver.solve();
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