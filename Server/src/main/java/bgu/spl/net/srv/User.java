package bgu.spl.net.srv;

import java.time.LocalDate;
import java.time.Period;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class User {
    //users data
    private String name;
    private String password;
    private String birthday;

    private int connectionId;
    private boolean loggedIn;
    private LinkedBlockingQueue<String> followedBy;
    private LinkedBlockingQueue<String> following;
    private BlockingQueue<String> waitingMessagesQueue;
    private LinkedBlockingQueue<String> blockedUsers;
    private int numOfMessagesPosts;


    public User(String name, String password, String birthday, int connectionId) {
        this.name = name;
        this.password = password;
        this.birthday = birthday;
        this.connectionId = connectionId;
        this.loggedIn = false;
        this.followedBy = new LinkedBlockingQueue<String>();
        this.following = new LinkedBlockingQueue<String>();
        this.waitingMessagesQueue = new LinkedBlockingQueue<String>();
        this.blockedUsers = new LinkedBlockingQueue<>();
        this.numOfMessagesPosts = 0;
    }

    //getters
    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public String getBirthday() {
        return birthday;
    }

    public int getConnectionId() {
        return connectionId;
    }

    public boolean isLoggedIn() {
        return this.loggedIn;
    }

    public LinkedBlockingQueue<String> getFollowedBy() {
        return followedBy;
    }

    public LinkedBlockingQueue<String> getFollowing() {
        return following;
    }

    public BlockingQueue<String> getWaitingMessagesQueue() {
        return waitingMessagesQueue;
    }

    public LinkedBlockingQueue<String> getBlockedUsers() {
        return blockedUsers;
    }

    public int getNumOfMessagesPosts() {
        return numOfMessagesPosts;
    }

    public String getAge(){
        int year = Integer.parseInt(this.birthday.substring(6));
        int month = Integer.parseInt(this.birthday.substring(3,5));
        int day = Integer.parseInt(this.birthday.substring(0,2));
        LocalDate birthday = LocalDate.of(year, month, day);
        LocalDate now = LocalDate.now();
        Period period = Period.between(birthday, now);
        return String.valueOf(period.getYears());
    }

    //setters
    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public void incNumOfMessagesPosts() {
        this.numOfMessagesPosts++;
    }

    public void setConnectionId(int connectionId) {
        this.connectionId = connectionId;
    }

}
