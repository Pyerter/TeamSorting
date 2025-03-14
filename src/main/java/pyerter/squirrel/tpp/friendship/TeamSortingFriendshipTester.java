package pyerter.squirrel.tpp.friendship;

import pyerter.squirrel.tpp.TeamSortingLogger;
import pyerter.squirrel.tpp.core.TeamSorterResult;
import pyerter.squirrel.tpp.core.TeamSorterSolver;
import pyerter.squirrel.tpp.core.TeamSortingGeneratorInput;
import pyerter.squirrel.tpp.core.TeamSortingInput;

import java.util.Arrays;
import java.util.stream.IntStream;

public class TeamSortingFriendshipTester {

    public static void main(String[] args) {
        int memberCount = 25;
        int teamCount = 5;
        int roleCount = 3;
        int preferenceCount = 3;
        // Role requirements for each team
        int roleReqLB = 1;
        int roleReqUB = 2;
        // Minimum team sizes
        int minTeamCountLB = 4;
        int minTeamCountUB = 5;
        // Number of roles members have
        int memberRoleCountLB = 1;
        int memberRoleCountUB = 2;
        //if (memberCount < roleCount * roleReqUB * teamCount) memberCount = roleCount * roleReqUB * teamCount;
        //if (memberCount < minTeamCountUB * teamCount) memberCount = minTeamCountUB * teamCount;
        int numbFriendships = 4;
        int friendshipSizeLB = 2;
        int friendshipSizeUB = 5;

        TeamSortingInput input = TeamSortingGeneratorInput.generateInput(memberCount, teamCount, roleCount, preferenceCount,
                roleReqLB, roleReqUB, minTeamCountLB, minTeamCountUB, memberRoleCountLB, memberRoleCountUB,
                numbFriendships, friendshipSizeLB, friendshipSizeUB);
        System.out.printf("Members (%d), Teams (%d), Roles (%d), Preferences (%d)%n", memberCount, teamCount, roleCount, preferenceCount);
        System.out.printf("Role Requirements (%d-%d), Min Team Counts (%d-%d), Member Role Counts (%d-%d)%n",
                roleReqLB, roleReqUB, minTeamCountLB, minTeamCountUB, memberRoleCountLB, memberRoleCountUB);
        if (input == null) {
            System.out.printf("Err: Input object returned as null- ran into some error?%n");
            return;
        }

        if (memberCount != input.numbMembers()) {
            System.out.printf("Note -- member count corrected to %d to ensure a solution is possible.", input.numbMembers());
        }

        System.out.printf("Teams: %s%n", input.toPrintTeams());
        System.out.printf("Roles: %s%n", input.toPrintRoles());
        System.out.printf("Members: %s%n", input.toPrintMemberNames());
        System.out.printf("Member Roles: %s%n", input.toPrintMemberRoles());
        System.out.printf("Member Preferences: %s%n", input.toPrintMemberPreferences());
        System.out.printf("Team Role Requirements: %s%n", input.toPrintTeamRoleRequirements());
        System.out.printf("Minimum Team Counts: %s%n", Arrays.toString(IntStream.rangeClosed(0, teamCount - 1).map(input::getTeamMinimumMembers).toArray()).replace(",", ""));


        System.out.printf("%n----- Solving Team Sorting -----%n%n");

        TeamSorterSolver solver = new TeamSorterSolver(input, false);
        solver.setUseHardPreferenceObjectiveFunction(false);
        solver.setIgnoreFriendships(false);
        solver.setRoundFriendships(true);
        TeamSortingLogger logger;
        TeamSorterResult result;
        TeamSorterResult roundedResult;
        boolean caughtFailure = false;
        String failureMessage = "";
        try {
            logger = new TeamSortingLogger(3);
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
            logger.log(String.format("Improvement by rounding function:%n    %.3f", (roundedResult.getFinalFriendshipObjectiveValue() - result.getFinalFriendshipObjectiveValue())));
            logger.log(String.format("Maximum (theoretical objective function:%n    %.3f", (result.getTheoreticalMaxFriendshipObjectiveValue())));
        }
        if (caughtFailure) {
            System.out.println("--------- Original attempt failed, retried with rounding algorithm on LP without friendship constraints");
            System.out.printf("Failure Message: %s%n", failureMessage);
        }
    }

}
