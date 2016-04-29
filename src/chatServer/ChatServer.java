package chatServer;

/**
 * Created by Satria on 4/28/2016.
 */
import java.net.*;
import java.io.*;
import org.json.*;

public class ChatServer implements Runnable {
    private ChatServerThread clients[] = new ChatServerThread[50];
    private ServerSocket server = null;
    private Thread       thread = null;

    private final int PLAYER_SIZE = 50;

    private int clientCount = 0;
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
                    default:
                        break;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

//            String msg = new String(String.valueOf(jsonObj));
//            clients[findClient(ID)].send(msg);
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
    // Join
    void joinClient(String username, String udpAddress, int udpPort){
        System.out.println("Username: " + username);
        System.out.println("Address: " + udpAddress);
        System.out.println("Port: " + udpPort);

        int currentClient = findClient(udpPort);
        int i = 0;
        boolean userExists = false;
        while(i<clientCount && !userExists){
            if(players[i].getUsername() != null) {
                userExists = players[i].getUsername().equals(username);
            }
            i++;
        }

        if(userExists){
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("status", "fail");
                jsonObject.put("description", "user exists");

                String msg = new String(String.valueOf(jsonObject));
                clients[findClient(udpPort)].send(msg);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            // user does not exist
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("status", "ok");
                jsonObject.put("player_id", 0);

                String msg = new String(String.valueOf(jsonObject));
                clients[findClient(udpPort)].send(msg);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
