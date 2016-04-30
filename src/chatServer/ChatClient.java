package chatServer;

/**
 * Created by Satria on 4/28/2016.
 */
import java.net.*;
import java.io.*;
import org.json.*;

public class ChatClient implements Runnable
{
    private Socket socket              = null;
    private Thread thread              = null;
    private DataInputStream  console   = null;
    private DataOutputStream streamOut = null;
    private ChatClientThread client    = null;
    private UDPReceiver clientUDP      = null;
    private UDPTransmitter transmitterUDP = null;
    private Player currentPlayer;
    private Player[] players = new Player [ChatServer.PLAYER_SIZE];

    public ChatClient(String serverName, int serverPort) {
        System.out.println("Establishing connection. Please wait ...");
        try {
            socket = new Socket(serverName, serverPort);
            System.out.println("Connected: " + socket);
            String localIP = socket.getLocalAddress().getHostAddress();
            int localPort = socket.getLocalPort();
            currentPlayer = new Player(localIP, localPort);
            start();
        } catch(UnknownHostException uhe) {
            System.out.println("Host unknown: " + uhe.getMessage());
        } catch(IOException e) {
            System.out.println("Unexpected exception: " + e.getMessage()); }
    }

    public void run() {
        while (thread != null) {
            try {
                String read = console.readLine();
                JSONObject jsonObject = new JSONObject();
                if (read.equals("server")) {
                    try {
                        jsonObject.put("method", "get_server");
                        jsonObject.put("server", "127.0.0.1");
                        jsonObject.put("port", "9876");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    streamOut.writeUTF(jsonObject.toString());
                    streamOut.flush();
                } else if (read.contains("join")) {
                    if(read.length() > 5) {
                        String username = read.substring(5);
                        try {
                            jsonObject.put("method", "join");
                            jsonObject.put("username", username);
                            jsonObject.put("udp_address", currentPlayer.getAddrIp());
                            jsonObject.put("udp_port", currentPlayer.getAddrPort());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        currentPlayer.setUsername(username);
                        System.out.println(jsonObject);
                        streamOut.writeUTF(jsonObject.toString());
                        streamOut.flush();
                    } else {
                        System.out.println("Please input username");
                    }
                } else if (read.equals("leave")) {
                    try {
                        jsonObject.put("method", "leave");
                        jsonObject.put("udp_address", currentPlayer.getAddrPort());
                        jsonObject.put("udp_port", currentPlayer.getAddrIp());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println(jsonObject);
                    streamOut.writeUTF(jsonObject.toString());
                    streamOut.flush();
                } else if (read.contains("client_address")) {

                } else if (read.equals("ready")){
                    // Check if player has joined the game by checking player_id
                    if (currentPlayer.getId() != Player.ID_NOT_SET){
                        try {
                            jsonObject.put("method", "ready");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        streamOut.writeUTF(jsonObject.toString());
                        streamOut.flush();
                    } else {
                        System.out.println("You have not joined the game");
                    }
                } else if (read.equals("client_address")) {
                    try {
                        jsonObject.put("method", "client_address");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println(jsonObject);
                    streamOut.writeUTF(jsonObject.toString());
                    streamOut.flush();
                } else if (read.equals("toClient")){
                    System.out.println("ToClient");
                } else if (read.equals("prepare_proposal")) {
                    prepareProposal();
                } else if (read.equals("accept_proposal")) {
                    acceptProposal();
                } else if (read.equals("vote_werewolf")) {
                    voteWerewolf();
                } else if (read.equals("vote_civillian")) {
                    voteCivilian();
                }
            } catch(IOException e) {
                System.out.println("Sending error: " + e.getMessage());
                stop();
            }
        }
    }

    public void handle(String msg) {
        if (msg.equals(".bye")) {
            System.out.println("Good bye. Press RETURN to exit ...");
            stop();
        } else {
            System.out.println("Server message : " + msg);
            try {
                JSONObject jsonObject = new JSONObject(msg);

                if(jsonObject.has("status")){
                    // Handle every possibility
                    if(!jsonObject.getString("status").equalsIgnoreCase("ok")){
                        currentPlayer.setUsername("");
                    }
                    if(jsonObject.has("player_id")){
                        currentPlayer.setId(jsonObject.getInt("player_id"));
                    }
                    System.out.println("Status: " + jsonObject.getString("status"));
                }
                System.out.println("Current player: " + currentPlayer);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void start() throws IOException {
        console   = new DataInputStream(System.in);
        streamOut = new DataOutputStream(socket.getOutputStream());
        if (thread == null) {
            client = new ChatClientThread(this, socket);
            clientUDP = new UDPReceiver(this, socket);
            thread = new Thread(this);
            thread.start();
        }
    }

    public void stop() {
        if (thread != null) {
            thread.stop();
            thread = null;
        }

        try {
            if (console   != null)  console.close();
            if (streamOut != null)  streamOut.close();
            if (socket    != null)  socket.close();
        } catch(IOException e) {
            System.out.println("Error closing: " + e);
        }
        client.close();
        client.stop();
    }

    public static void main(String args[]) {
        ChatClient client = null;
        if (args.length != 2)
            System.out.println("Usage: java ChatClient host port");
        else
            client = new ChatClient(args[0], Integer.parseInt(args[1]));
    }


    /*-------------------------- Method Prepare Proposal Paxos---------------------------*/
    void prepareProposal(){
        if(this.currentPlayer.getStatusPaxos().equals("proposer")){
            System.out.println("I am proposer");
        } else if (this.currentPlayer.getStatusPaxos().equals("acceptor")) {
            System.out.println("I am acceptor");
        } else if (this.currentPlayer.getStatusPaxos().equals("leader")) {
            System.out.println("I am KPU leader");
        }
    }

    /*-------------------------- Method Accept Proposal Paxos---------------------------*/
    void acceptProposal(){
        if(this.currentPlayer.getStatusPaxos().equals("proposer")){
            System.out.println("I am proposer");
        } else if (this.currentPlayer.getStatusPaxos().equals("acceptor")) {
            System.out.println("I am acceptor");
        } else if (this.currentPlayer.getStatusPaxos().equals("leader")) {
            System.out.println("I am KPU leader");
        }

    }

    /*-------------------------- Method Vote Werewolf Paxos---------------------------*/
    void voteWerewolf(){
        if(this.currentPlayer.getStatusPaxos().equals("proposer")){
            System.out.println("I am proposer");
        } else if (this.currentPlayer.getStatusPaxos().equals("acceptor")) {
            System.out.println("I am acceptor");
        } else if (this.currentPlayer.getStatusPaxos().equals("leader")) {
            System.out.println("I am KPU leader");
        }

    }

    /*-------------------------- Method Vote Civillian Paxos---------------------------*/
    void voteCivilian(){
        if(this.currentPlayer.getStatusPaxos().equals("proposer")){
            System.out.println("I am proposer");
        } else if (this.currentPlayer.getStatusPaxos().equals("acceptor")) {
            System.out.println("I am acceptor");
        } else if (this.currentPlayer.getStatusPaxos().equals("leader")) {
            System.out.println("I am KPU leader");
        }

    }

}