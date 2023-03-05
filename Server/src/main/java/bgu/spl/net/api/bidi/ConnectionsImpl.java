package bgu.spl.net.api.bidi;

import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.DataBase;
import bgu.spl.net.srv.User;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionsImpl implements Connections<String>{
    private static class SingletonHolder{
        private static ConnectionsImpl instance= new ConnectionsImpl();
    }
    private DataBase dataBase;
    private ConcurrentHashMap<Integer, ConnectionHandler<String>> connectionHandlers;
    private ConcurrentHashMap<Integer, User> connectionIdToUser;
    private int nextConnectionId;

    //contractures
    private ConnectionsImpl(){
        this.dataBase = DataBase.getInstance();
        this.connectionHandlers = new ConcurrentHashMap<Integer, ConnectionHandler<String>>();
        this.connectionIdToUser =new ConcurrentHashMap<Integer, User>();
        this.nextConnectionId = 0;
    }

    public static ConnectionsImpl getInstance(){
        return SingletonHolder.instance;
    }

    //getters
    public DataBase getDataBase() {
        return dataBase;
    }

    public ConcurrentHashMap<Integer, ConnectionHandler<String>> getConnectionHandlers() {
        return connectionHandlers;
    }

    public ConcurrentHashMap<Integer, User> getConnectionIdToUser() {
        return connectionIdToUser;
    }

    public int getNextConnectionId() {
        return nextConnectionId;
    }


    public void insertConnectionHandler(ConnectionHandler<String> connectionHandler) {
        this.connectionHandlers.put(nextConnectionId,connectionHandler);
        this.nextConnectionId++;
    }

    @Override
    public boolean send(int connectionId, String msg) {
        ConnectionHandler<String> userConnectionHandler = this.connectionHandlers.get(connectionId);
        try {
            userConnectionHandler.send(msg);
        }catch (Exception e){
            return false;
        }
        return true;

    }

    @Override
    public void broadcast(String msg) {
        for (ConnectionHandler<String> connectionHandler: this.connectionHandlers.values()){
            connectionHandler.send(msg);
        }
    }

    @Override
    public void disconnect(int connectionId) {
        ConnectionHandler<String> userConnectionHandler = this.connectionHandlers.get(connectionId);
        try {
            userConnectionHandler.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
