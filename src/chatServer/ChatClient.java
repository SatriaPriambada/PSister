package chatServer;

/**
 * Created by Satria on 4/28/2016.
 */

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

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
    private int proposalID = 0;
    private int currentLeader = Player.ID_NOT_SET;
    private int previousLeader = Player.ID_NOT_SET;
    private int numberPlayer;

    public Player getCurrentPlayer(){
        return currentPlayer;
    }

    public Player[] getPlayers(){
        return players;
    }

    public int getNumberPlayer(){
        return numberPlayer;
    }

    public int getProposalID(){
        return proposalID;
    }

    public ChatClient(String serverName, int serverPort) {
        System.out.println("Establishing connection. Please wait ...");
        for (int i = 0; i < ChatServer.PLAYER_SIZE; i++){
            players[i] = new Player();
        }
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
                System.out.println("Read: " + read);
                JSONObject jsonObject = new JSONObject();
                switch (read) {
                    case "server":
                        jsonObject.put("method", "get_server");
                        jsonObject.put("server", "127.0.0.1");
                        jsonObject.put("port", "9876");
                        System.out.println(jsonObject.toString());
                        streamOut.writeUTF(jsonObject.toString());
                        streamOut.flush();
                        break;
                    case "join":
                        Scanner scanner = new Scanner(System.in);
                        System.out.print("Username: ");
                        String username = scanner.next();
                        jsonObject.put("method", "join");
                        jsonObject.put("username", username);
                        jsonObject.put("udp_address", currentPlayer.getAddrIp());
                        jsonObject.put("udp_port", currentPlayer.getAddrPort());
                        currentPlayer.setUsername(username);
                        System.out.println(jsonObject.toString());
                        streamOut.writeUTF(jsonObject.toString());
                        streamOut.flush();
                        break;
                    case "leave":
                        jsonObject.put("method", "leave");
                        System.out.println(jsonObject.toString());
                        streamOut.writeUTF(jsonObject.toString());
                        streamOut.flush();
                        break;
                    case "ready":
                        if (currentPlayer.getId() != Player.ID_NOT_SET) {
                            jsonObject.put("method", "ready");
                        } else {
                            System.out.println("You have not joined the game");
                        }
                        System.out.println(jsonObject.toString());
                        streamOut.writeUTF(jsonObject.toString());
                        streamOut.flush();
                        break;
                    case "client_address":
                        jsonObject.put("method", "client_address");
                        System.out.println(jsonObject.toString());
                        streamOut.writeUTF(jsonObject.toString());
                        streamOut.flush();
                        break;
                    case "vote_werewolf":
                        voteWerewolf();
                        break;
                    case "accept_proposal":
                        acceptProposal();
                        break;
                    case "vote_civilian":
                        voteCivilian();
                        break;
                    default:
                        jsonObject.put("status","error");
                        jsonObject.put("description","wrong request");
                        break;
                }

            } catch (JSONException j){
                j.printStackTrace();
            } catch (IOException e) {
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
                        //currentPlayer.setUsername("");
                    } else if (jsonObject.has("description")){
                        String desc = jsonObject.getString("description");
                        switch (desc) {
                            case "list of clients retrieved":
                                JSONArray jsonArray = new JSONArray(jsonObject.get("clients").toString());
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject json = jsonArray.getJSONObject(i);
                                    players[i].setId(json.getInt("player_id"));
                                    players[i].setAlive(json.getInt("is_alive"));
                                    players[i].setAddrIp(json.getString("address"));
                                    players[i].setAddrPort(json.getInt("port"));
                                    players[i].setUsername(json.getString("username"));
                                    System.out.println(players[i].toString());
                                }
                                numberPlayer = jsonArray.length();
                                //Set player ID N and N-1 as proposer and other as acceptor
                                if (currentPlayer.getId() >= numberPlayer - 2){
                                    currentPlayer.setStatusPaxos("proposer");
                                } else {
                                    currentPlayer.setStatusPaxos("acceptor");
                                }
                                prepareProposal();
                                break;
                        }
                    }
                    if(jsonObject.has("player_id")){
                        currentPlayer.setId(jsonObject.getInt("player_id"));
                    }
                    System.out.println("Status: " + jsonObject.getString("status"));
                }
                System.out.println("Current player: " + currentPlayer);

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
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


    /*-------------------------- Method Prepare Proposal Paxos---------------------------*/
    public void prepareProposal() throws JSONException, InterruptedException {
        System.out.println(currentPlayer.getStatusPaxos().toString());
        if(this.currentPlayer.getStatusPaxos().equals("proposer")){
            System.out.println("I am proposer");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method","prepare_proposal");
            jsonObject.put("proposal_id", "["+ proposalID + ","+ currentPlayer.getId() + "]");
            for (int i = 0; i < numberPlayer; i++ ){
                System.out.println("loop :" + players[i].getAddrIp() + " : "+ players[i].getAddrPort());
                transmitterUDP = new UDPTransmitter(this, players[i].getAddrIp(), players[i].getAddrPort(), socket.getLocalPort());
                transmitterUDP.send(jsonObject.toString());
            }
        } else if (this.currentPlayer.getStatusPaxos().equals("acceptor")) {
            System.out.println("I am acceptor");
        } else if (this.currentPlayer.getStatusPaxos().equals("leader")) {
            System.out.println("I am KPU leaders");
        }
    }

    /*-------------------------- Method Accept Proposal Paxos---------------------------*/
    public void acceptProposal(){
        if(this.currentPlayer.getStatusPaxos().equals("proposer")){
            System.out.println("I am proposer");
        } else if (this.currentPlayer.getStatusPaxos().equals("acceptor")) {
            System.out.println("I am acceptor");
        } else if (this.currentPlayer.getStatusPaxos().equals("leader")) {
            System.out.println("I am KPU leader");
        }

    }

    /*-------------------------- Method Vote Werewolf Paxos---------------------------*/
    public void voteWerewolf(){
        if(this.currentPlayer.getStatusPaxos().equals("proposer")){
            System.out.println("I am proposer");
        } else if (this.currentPlayer.getStatusPaxos().equals("acceptor")) {
            System.out.println("I am acceptor");
        } else if (this.currentPlayer.getStatusPaxos().equals("leader")) {
            System.out.println("I am KPU leader");
        }
    }

    /*-------------------------- Method Vote Civillian Paxos---------------------------*/
    public void voteCivilian(){
        if(this.currentPlayer.getStatusPaxos().equals("proposer")){
            System.out.println("I am proposer");
        } else if (this.currentPlayer.getStatusPaxos().equals("acceptor")) {
            System.out.println("I am acceptor");
        } else if (this.currentPlayer.getStatusPaxos().equals("leader")) {
            System.out.println("I am KPU leader");
        }

    }


    public static void main(String args[]) {
        ChatClient client = null;
        if (args.length != 2)
            System.out.println("Usage: java ChatClient host port");
        else
            client = new ChatClient(args[0], Integer.parseInt(args[1]));
    }

}