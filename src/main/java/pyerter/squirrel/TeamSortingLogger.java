package pyerter.squirrel;

/**
 * Author: Porter Squires
 * License: MIT License
 *
 *
 */
public class TeamSortingLogger {

    protected int verbose;

    public TeamSortingLogger() {
        this(1);
    }

    public TeamSortingLogger(int verbose) {
        this.verbose = verbose;
    }

    public void log(String out, int verbosity) {
        if (verbosity <= verbose) {
            System.out.println(out);
        }
    }

    public void log(String out) {
        log(out, 2);
    }

    public void writeResult(String filename) {

    }

}
