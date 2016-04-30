package chatServer;

/**
 * Created by Satria on 4/28/2016.
 */
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ChatServer implements Runnable {
    private ChatServerThread clients[] = new ChatServerThread[50];
    private ServerSocket server = null;
    private Thread       thread = null;

    public static final int PLAYER_SIZE = 50;

    // a number of clients who connects to server
    private int clientCount = 0;

    // a number of clients who has joined the game
    private int playerCount = 0;

    private Player[] players = new Player[PLAYER_SIZE];
//    private int[] listIsAlive = new int[50];
//    private String[] listIP = new String[50];
//    private int[] listPort = new int[50];
//    private String[] listUsername = new String[50];
//    private int[] listID = new int[50];

    public ChatServer(int port) {
        try {
            System.out.println("Binding to port " + port + ", please wait  ...");
            server = new ServerSocket(port);
            System.out.println("Server started: " + server);
            start();
        } catch(IOException ioe) {
            System.out.println("Can not bind to port " + port + ": " + ioe.getMessage());
        }
    }

    public void run() {
        while (thread != null) {
            try {
                System.out.println("Waiting for a client ...");
                addThread(server.accept());
            } catch(IOException ioe) {
                System.out.println("Server accept error: " + ioe); stop();
            }
        }
    }

    public void start() {
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }

    public void stop() {
        if (thread != null) {
            thread.stop();
            thread = null;
        }
    }

    private int findClient(int ID) {
        for (int i = 0; i < clientCount; i++)
        if (clients[i].getID() == ID) {
            return i;
        }
        return -1;
    }

    public synchronized void handle(int ID, String input) {
        if (input.equals(".bye")) {
            clients[findClient(ID)].send(".bye");
            remove(ID);
        } else {
            System.out.println(input);
            try {
                JSONObject jsonObject = new JSONObject(input);
                String method = jsonObject.getString("method");

                switch (method){
                    case "join":
                        // Join Client
                        String clientUname = jsonObject.getString("username");
                        String clientAddr = jsonObject.getString("udp_address");
                        int clientPort = jsonObject.getInt("udp_port");
                        joinClient(clientUname, clientAddr, clientPort);

                        break;
                    case "get_server":
                        break;
                    case "leave":
                        leaveClient(ID);
                        break;
                    case "client_address":
                        clientAddress();
                        break;
                    case "ready":
                        ready(ID);
                        break;
                    default:
                        break;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void remove(int ID) {
        int pos = findClient(ID);
        if (pos >= 0) {
            ChatServerThread toTerminate = clients[pos];
            System.out.println("Removing client thread " + ID + " at " + pos);
            if (pos < clientCount-1) {
                for (int i = pos + 1; i < clientCount; i++) {
                    clients[i - 1] = clients[i];
                }
            }
            clientCount--;
            try {
                toTerminate.close();
            } catch(IOException ioe) {
                System.out.println("Error closing thread: " + ioe);
            }
            toTerminate.stop();
        }
    }

    private void addThread(Socket socket) {
        if (clientCount < clients.length) {
            System.out.println("Client accepted: " + socket);
            clients[clientCount] = new ChatServerThread(this, socket);
            try {
                String clientIp = socket.getInetAddress().getHostAddress();
                int clientPort = socket.getPort();
                players[clientCount] = new Player(clientIp, clientPort);

                System.out.println(players[clientCount]);
                clients[clientCount].open();
                clients[clientCount].start();
                clientCount++;
                System.out.println(clientCount);
            } catch(IOException ioe) {
                System.out.println("Error opening thread: " + ioe); }
        }
    else
        System.out.println("Client refused: maximum " + clients.length + " reached.");
    }

    public static void main(String args[])
    {
        ChatServer server = null;
        if (args.length != 1)
            System.out.println("Usage: java ChatServer port");
        else
            server = new ChatServer(Integer.parseInt(args[0]));
    }

    // Method di handle

    /*-------------------------- Method Join ---------------------------*/

    void joinClient(String username, String udpAddress, int udpPort){
        System.out.println("Username: " + username);
        System.out.println("Address: " + udpAddress);
        System.out.println("Port: " + udpPort);

        int i = 0;
        boolean userExists = false;
        while(i<playerCount && !userExists){
            if(players[i].getUsername() != null) {
                userExists = players[i].getUsername().equals(username);
            }
            i++;
        }


        // Check if port existed
        boolean portExists = false;
        i = 0;
        while(i<playerCount && !portExists){
            portExists = (players[i].getAddrPort() == udpPort);
            i++;
        }

        JSONObject jsonObject = new JSONObject();
        if(userExists){
            try {
                jsonObject.put("status", "fail");
                jsonObject.put("description", "user exists");

                String msg = new String(String.valueOf(jsonObject));
                clients[findClient(udpPort)].send(msg);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (portExists){
            try {
                jsonObject.put("status", "fail");
                jsonObject.put("description", "player has already joined");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            // user and port do not exist
            players[playerCount].setUsername(username);
            try {
                jsonObject.put("status", "ok");
                jsonObject.put("player_id", playerCount);

                String msg = new String(String.valueOf(jsonObject));
                clients[findClient(udpPort)].send(msg);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            playerCount++;
        }
    }

    /*-------------------------- Method Leave ---------------------------*/

    void leaveClient(int ID){
        int i = 0;
        boolean found = false;
        while(i<playerCount && !found){
            if(players[i].getUsername() != null) {
                found = players[i].getAddrPort() == ID;
            }
            i++;
        }

        if(!found){
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("status", "fail");
                jsonObject.put("description", "you have not join the game");

                String msg = new String(String.valueOf(jsonObject));
                clients[findClient(ID)].send(msg);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            i--;
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("status", "ok");

                String msg = new String(String.valueOf(jsonObject));
                clients[findClient(ID)].send(msg);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            // Geser player
            while (i+1 < playerCount) {
                players[i].setUsername(players[i + 1].getUsername());
                players[i].setAddrIp(players[i + 1].getAddrIp());
                players[i].setAddrPort(players[i + 1].getAddrPort());
                players[i].setAlive(players[i + 1].getAlive());
                players[i].setId(players[i + 1].getId());

                i++;
            }
            playerCount--;
        }
    }

    /*-------------------------- Method Client Address ---------------------------*/
    void clientAddress(){
        JSONArray jsonArray = new JSONArray();
        for (int j = 0; j < playerCount; j++){
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("player_id", j);
                jsonObject.put("is_alive", players[j].getAlive());
                jsonObject.put("address", players[j].getAddrIp());
                jsonObject.put("port", players[j].getAddrPort());
                jsonObject.put("username", players[j].getUsername());

                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < playerCount; i++){
            JSONObject json = new JSONObject();
            try {
                json.put("status", "ok");
                json.put("clients", jsonArray);
                json.put("description", "list of clients retrieved");
                String msg = new String(String.valueOf(json));
                clients[findClient(players[i].getAddrPort())].send(msg);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    /*-------------------------- Method Ready ---------------------------*/
    void ready(int ID){
        int i = 0;
        boolean found = false;
        while(i<playerCount && !found){
            if(players[i].getUsername() != null) {
                found = players[i].getAddrPort() == ID;
            }
            i++;
        }

        if(found){
            i--;
            JSONObject jsonObject = new JSONObject();
            if(players[i].isReady()){
                try {
                    jsonObject.put("status", "fail");
                    jsonObject.put("description", "You were ready");
                    String msg = new String(String.valueOf(jsonObject));
                    clients[findClient(players[i].getAddrPort())].send(msg);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    jsonObject.put("status", "ok");
                    jsonObject.put("description", "waiting for other player to start");
                    players[i].setReady(true);
                    String msg = new String(String.valueOf(jsonObject));
                    clients[findClient(players[i].getAddrPort())].send(msg);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

