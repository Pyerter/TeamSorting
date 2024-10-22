package pyerter.squirrel;

import java.util.Arrays;
import java.util.List;

/**
 * Author: Porter Squires
 * License: MIT License
 *
 * 
 */
public class Friendship {

    protected String friendshipName;
    protected String[] friends;
    protected Member[] members;

    public Friendship(String friendshipName, String ... friends) {
        this.friendshipName = friendshipName;
        this.friends = friends;
        Arrays.sort(this.friends);
    }

    public String[] getFriends() {
        return friends;
    }

    public String getFriendshipName() {
        return friendshipName;
    }

    public Member[] getMembers() {
        return getMembers(null);
    }

    public int size() {
        return members.length;
    }

    public Member[] getMembers(TeamSortingInput input) {
        if (members == null) {
            if (input == null) return null;
            initialize(input);
        }
        return members;
    }

    public void initialize(TeamSortingInput input) {
        if (this.members != null) return;
        List<Member> memberList = input.getMembers();
        this.members = memberList.stream()
                .filter(m -> Arrays.binarySearch(this.friends, m.getName()) >= 0)
                .toArray(Member[]::new);
    }

    public boolean equals(Friendship friendship) {
        return friendshipName.equalsIgnoreCase(friendship.friendshipName) &&
                Arrays.equals(friends, friendship.friends);
    }

    public boolean isInitialized() {
        return members != null;
    }

    public static Friendship merge(Friendship f1, Friendship f2, String name) {
        String[] friends = new String[f1.getFriends().length + f2.getFriends().length];
        System.arraycopy(f1.getFriends(), 0, friends, 0, f1.getFriends().length);
        System.arraycopy(f2.getFriends(), 0, friends, f1.getFriends().length, f2.getFriends().length);
        return new Friendship(name, friends);
    }

}
