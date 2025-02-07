package pyerter.squirrel.tpp.friendship;

import pyerter.squirrel.tpp.TeamSortingLogger;
import pyerter.squirrel.tpp.core.TeamSorterResult;
import pyerter.squirrel.tpp.core.TeamSorterSolver;
import pyerter.squirrel.tpp.core.TeamSortingInput;

public class TeamSorterFriendshipSolver extends TeamSorterSolver {
    public TeamSorterFriendshipSolver(TeamSortingInput input) {
        super(input);
        setIgnoreFriendships(false);
    }

    public TeamSorterFriendshipSolver(TeamSortingInput input, boolean useIntegral) {
        super(input, useIntegral);
        setIgnoreFriendships(false);
    }

    public TeamSorterResult solve(TeamSortingLogger logger) {
        TeamSorterResult result = super.solve(logger);



        return result;
    }
}
