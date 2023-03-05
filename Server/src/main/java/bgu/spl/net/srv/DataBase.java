package bgu.spl.net.srv;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;


public class DataBase {
    private static class SingletonHolder{
        private static DataBase instance= new DataBase();
    }
    private ConcurrentHashMap<String, User> users;
    private BlockingQueue<String> pmList;
    private BlockingQueue<String> postList;
    private LinkedList<String> badWords;

    //constructors
    private DataBase() {
        this.users = new ConcurrentHashMap<String, User>();
        this.pmList = new LinkedBlockingQueue<String>();
        this.postList = new LinkedBlockingQueue<String>();
        this.badWords = new LinkedList<>();
    }

    public void addBadWords(){
        String[] arr = {"bad"}; // add bad words here
        badWords.addAll(Arrays.asList(arr));
    }
    public static DataBase getInstance(){
        return SingletonHolder.instance;
    }

    //getters
    public ConcurrentHashMap<String, User> getUsers() {
        return users;
    }

    public BlockingQueue<String> getPmList() {
        return pmList;
    }

    public BlockingQueue<String> getPostList() {
        return postList;
    }

    public LinkedList<String> getBadWords() {
        return badWords;
    }
}
