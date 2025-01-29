package pyerter.squirrel.tpp.io;

import java.util.HashMap;
import java.util.Map;

/**
 * Author: Porter Squires
 * License: MIT License
 *
 *
 */
public class ArgumentReader {

    protected Map<String, String> argMap;

    public ArgumentReader() {
        argMap = new HashMap<>();
    }

    public void addArgument(String name, int index) {

    }

    public void parseArgs(String[] args) {
        argMap = new HashMap<>();
    }

    public class Argument {
        public Argument(String name, int index) {

        }
    }

}
