package bgu.spl.net.api.bidi;

import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.User;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class BidiMessagingProtocolImpl implements BidiMessagingProtocol<String>{
    private int connectionId;
    private ConnectionsImpl connections;

    @Override
    public void start(int connectionId, Connections<String> connections) {
        this.connectionId = connectionId;
        this.connections = (ConnectionsImpl) connections;
    }

    public void responseError(String opcode){
        String errorMsg = "11" + opcode + ";";
        connections.send(this.connectionId, errorMsg);
    }

    public void responseAck(String opcode, String message){
        String ackMsg = "10" + opcode + message + ";";
        connections.send(this.connectionId, ackMsg);
    }

    public void responseNotify(String ansToNotify, List<String> usersToSend){
        for (String userToSendName: usersToSend){
            User userToSend = connections.getDataBase().getUsers().get(userToSendName);
            if (userToSend.isLoggedIn()){
                connections.send(userToSend.getConnectionId(), ansToNotify + ";");
            }
            else{
                try {
                    userToSend.getWaitingMessagesQueue().put(ansToNotify + ";");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public synchronized void switchConnectionHandler(int oldConnectionId){
        ConnectionHandler newConnectionHandler= connections.getConnectionHandlers().get(this.connectionId);
        connections.getConnectionHandlers().remove(this.connectionId);
        connections.getConnectionHandlers().remove(oldConnectionId);
        connections.getConnectionHandlers().put(oldConnectionId,newConnectionHandler);
        this.connectionId = oldConnectionId;
    }

    public String filterMessage(String message){
	    connections.getDataBase().addBadWords();
        List<String> badWords = connections.getDataBase().getBadWords();
        message = " " + message + " ";
        for (String badWord: badWords){
            message = message.replaceAll(" " + badWord + ", ", " <filtered> ");
            message = message.replaceAll(" " + badWord + ". ", " <filtered> ");
            message = message.replaceAll(" " + badWord + "? ", " <filtered> ");
            message = message.replaceAll(" " + badWord + "! ", " <filtered> ");
            message = message.replaceAll(" " + badWord + " ", " <filtered> ");
        }
        if (message.charAt(0) == ' '){
            message = message.substring(1);
        }
        if (message.charAt(message.length() - 1) == ' '){
            message = message.substring(0, message.length() - 1);
        }
        return message;
    }

    @Override
    public void process(String message) {
        String opcode = message.substring(0,2);
        message = message.substring(2);

        //handle register request
        switch (opcode) {
            case "01": {
                String username = message.substring(0, message.indexOf('\0'));
                message = message.substring(username.length() + 1);
                if (connections.getDataBase().getUsers().get(username) != null) {
                    responseError(opcode);
                } else {
                    String password = message.substring(0, message.indexOf('\0'));
                    message = message.substring(password.length() + 1);
                    String birthday = message.substring(0, message.indexOf('\0'));
                    if (username.equals("") || password.equals("") || birthday.equals("")){
                        responseError(opcode);
                        break;
                    }
                    else {
                        User user = new User(username, password, birthday, connectionId);
                        boolean registerSuccess = true;
                        synchronized (connections.getConnectionIdToUser()){
                            if (connections.getConnectionIdToUser().get(connectionId) == null) {
                                connections.getConnectionIdToUser().put(connectionId, user);
                            }
                            else {
                                registerSuccess = false;
                                responseError(opcode);
                            }
                        }
                        synchronized (connections.getConnectionIdToUser()){
                            if (connections.getDataBase().getUsers().get(username) == null) {
                                connections.getDataBase().getUsers().put(username, user);
                            }
                            else if (registerSuccess) {
                                responseError(opcode);
                                registerSuccess = false;
                            }
                        }
                        if (registerSuccess){
                            responseAck(opcode, "");
                        }
                    }
                }
                break;
            }

            //handle Login request
            case "02": {
                String username = message.substring(0, message.indexOf('\0'));
                message = message.substring(username.length() + 1);
                String password = message.substring(0, message.indexOf('\0'));
                message = message.substring(password.length() + 1);
                String captcha = message;
                //checking if the captch is 0
                if (!captcha.equals("1")) {
                    responseError(opcode);
                }
                //checking if user didnt registered
                else if (connections.getDataBase().getUsers().get(username) == null) {
                    responseError(opcode);
                } else {
                    User user = connections.getDataBase().getUsers().get(username);
                    //checking if the password doesn't match
                    if (!user.getPassword().equals(password)) {
                        responseError(opcode);
                        break;
                    }
                    //checking if yhe user is already logged in
                    else if (user.isLoggedIn()) {
                        responseError(opcode);
                        break;
                    } else if (user.getConnectionId() != this.connectionId) {
                        switchConnectionHandler(user.getConnectionId());
                    }
                    user.setLoggedIn(true);
                    while (!user.getWaitingMessagesQueue().isEmpty()) {
                        String waitingMessage = user.getWaitingMessagesQueue().remove();
                            connections.send(user.getConnectionId(), waitingMessage);
                    }
                    responseAck(opcode, "");
                }
                break;
            }

            //handle Logout request
            case "03": {
                //checking if user didnt registered
                User user = connections.getConnectionIdToUser().get(connectionId);
                if (connections.getDataBase().getUsers().get(user.getName()) == null) {
                    responseError(opcode);
                } else if (!user.isLoggedIn()) {
                    responseError(opcode);
                }
                else {
                    user.setLoggedIn(false);
                    responseAck(opcode, "");
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    connections.disconnect(this.connectionId);
                }
                break;
            }

            //handle follow-unfollow request
            case "04": {
                String type = message.substring(0, 1);
                message = message.substring(1);
                String username = message;
                String ackResponse = username + '\0';
                User user = connections.getConnectionIdToUser().get(connectionId);
                if (connections.getDataBase().getUsers().get(user.getName()) == null) {
                    responseError(opcode);
                } else if (!user.isLoggedIn()) {
                    responseError(opcode);
                } else if (connections.getDataBase().getUsers().get(username) == null) {
                    responseError(opcode);
                } else {
                    User otherUser = connections.getDataBase().getUsers().get(username);

                    if (type.equals("0")) {
                        // check if already follow
                        if (user.getFollowing().contains(username) ||
                                otherUser.getBlockedUsers().contains(user.getName())) {
                            responseError(opcode);
                            break;
                        } else {
                            user.getFollowing().add(username);
                            otherUser.getFollowedBy().add(user.getName());
                        }
                    }
                    // case unfollow
                    else if (type.equals("1")) {
                        // check if already follow
                        if (!user.getFollowing().contains(username)) {
                            responseError(opcode);
                            break;
                        } else {
                            user.getFollowing().remove(username);
                            otherUser.getFollowedBy().remove(user.getName());
                        }
                    }
                    responseAck(opcode, ackResponse);
                }

                break;
            }

            //handle POST request
            case "05": {
                User user = connections.getConnectionIdToUser().get(connectionId);
                if (user == null || connections.getDataBase().getUsers().get(user.getName()) == null) {
                    responseError(opcode);
                } else if (!user.isLoggedIn()) {
                    responseError(opcode);
                } else {
                    String content = message.substring(0, message.length() - 1); //removing the '\0'
                    connections.getDataBase().getPostList().add(content);
                    List<String> usersToSendPost = new LinkedList<>();
                    usersToSendPost.addAll(user.getFollowedBy());
                    String returnContent = "";
                    while (content.indexOf('@') != -1) {
                        String username = content.substring(content.indexOf('@') + 1, content.indexOf(' ', content.indexOf('@')));
                        returnContent += content.substring(0, content.indexOf('@') + username.length() + 1);
                        content = content.substring(content.indexOf('@') + username.length() + 1);
                        if (connections.getDataBase().getUsers().get(username) != null) {
                            LinkedBlockingQueue<String> blockedUsers = connections.getDataBase().getUsers().get(username).getBlockedUsers();
                            if (!usersToSendPost.contains(username) && !blockedUsers.contains(user.getName())){
                                usersToSendPost.add(username);
                            }

                        }
                    }
                    returnContent += content;
                    String ans = "09" + '1' + user.getName() + '\0' + returnContent + '\0';
                    responseNotify(ans, usersToSendPost);
                    user.incNumOfMessagesPosts();
                    responseAck(opcode, "");
                }
                break;
            }

            // handle PM
            case "06": {
                User user = connections.getConnectionIdToUser().get(connectionId);
                if (user == null || connections.getDataBase().getUsers().get(user.getName()) == null) {
                    responseError(opcode);
                } else if (!user.isLoggedIn()) {
                    responseError(opcode);
                } else {
                    String userToSend = message.substring(0, message.indexOf('\0'));
                    if (connections.getDataBase().getUsers().get(userToSend) == null) {
                        responseError(opcode);
                    } else if (!user.getFollowing().contains(userToSend) ||
                            !connections.getDataBase().getUsers().get(userToSend).getFollowedBy().contains(user.getName())) {
                        responseError(opcode);
                    } else {
                        message = message.substring(message.indexOf('\0') + 1);
                        String content = message.substring(0, message.indexOf('\0'));
                        String filteredContent = filterMessage(content);
                        message = message.substring(message.indexOf('\0') + 1);
                        String timeAndDate = message.substring(0, message.indexOf('\0'));
                        timeAndDate = timeAndDate.substring(0, timeAndDate.length() - 5);
                        connections.getDataBase().getPmList().add(filteredContent);
                        List<String> usersToSendPost = new LinkedList<>();
                        usersToSendPost.add(userToSend);
                        String ans = "09" + '0' + user.getName() + '\0' + filteredContent + " " + timeAndDate + '\0';
                        responseNotify(ans, usersToSendPost);
                        responseAck(opcode, "");
                    }
                }
                break;
            }
            // handle LOGSTAT
            case "07": {
                User user = connections.getConnectionIdToUser().get(connectionId);
                if (user == null || connections.getDataBase().getUsers().get(user.getName()) == null) {
                    responseError(opcode);
                } else if (!user.isLoggedIn()) {
                    responseError(opcode);
                } else {
                    List<User> loggedInUsers = new LinkedList<>();
                    for (User userToCheck : connections.getDataBase().getUsers().values()) {
                        if (userToCheck.isLoggedIn() &&
                                !userToCheck.getBlockedUsers().contains(user.getName()) &&
                                !user.getBlockedUsers().contains(userToCheck.getName())) {
                            loggedInUsers.add(userToCheck);
                        }
                    }
                    String ackResult = "";
                    for (User loggedUser : loggedInUsers) {
                        ackResult += "ACK 7 " + loggedUser.getAge() + " " +
                                loggedUser.getNumOfMessagesPosts() + " " +
                                loggedUser.getFollowedBy().size() + " " +
                                loggedUser.getFollowing().size() + '\n';
                    }
                    responseAck(opcode, ackResult);
                }
                break;
            }
            // handle STAT
            case "08": {
                User user = connections.getConnectionIdToUser().get(connectionId);
                if (user == null || connections.getDataBase().getUsers().get(user.getName()) == null) {
                    responseError(opcode);
                } else if (!user.isLoggedIn()) {
                    responseError(opcode);
                } else {
                    List<User> loggedInUsers = new LinkedList<>();
                    boolean found = false;
                    // parse users list
                    while (!found && message.indexOf('|') != -1) {
                        String username = message.substring(0, message.indexOf('|'));
                        message = message.substring(message.indexOf('|') + 1);
                        if (connections.getDataBase().getUsers().get(username) != null) {
                            User otherUser = connections.getDataBase().getUsers().get(username);
                            if (otherUser.isLoggedIn()) {
                                if (otherUser.getBlockedUsers().contains(user.getName()) ||
                                        user.getBlockedUsers().contains(username)){
                                    loggedInUsers.clear();
                                    found = true;
                                }
                                else {
                                    loggedInUsers.add(otherUser);
                                }
                            }
                        }
                    }
                    if (!loggedInUsers.isEmpty()) {
                        String ackResult = "";
                        for (User loggedUser : loggedInUsers) {
                            ackResult += "ACK 8 " + loggedUser.getAge() + " " +
                                    loggedUser.getNumOfMessagesPosts() + " " +
                                    loggedUser.getFollowedBy().size() + " " +
                                    loggedUser.getFollowing().size() + "\n";
                        }
                        responseAck(opcode, ackResult);
                    }
                    else{
                        responseError(opcode);
                    }
                }
                break;
            }
            // handle BLOCKED
            case "12": {
                User user = connections.getConnectionIdToUser().get(connectionId);
                if (user == null || connections.getDataBase().getUsers().get(user.getName()) == null) {
                    responseError(opcode);
                } else if (!user.isLoggedIn()) {
                    responseError(opcode);
                } else {
                    String otherUserName = message.substring(0, message.length() - 1);
                    user.getFollowing().remove(otherUserName);
                    user.getFollowedBy().remove(otherUserName);
                    User otherUser = connections.getDataBase().getUsers().get(otherUserName);
                    if (otherUser != null) {
                        otherUser.getFollowing().remove(user.getName());
                        otherUser.getFollowedBy().remove(user.getName());
                        try {
                            user.getBlockedUsers().put(otherUserName);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    responseAck(opcode, "");
                }
                break;
            }
        }
    }

    @Override
    public boolean shouldTerminate() {
        return false;
    }
}
