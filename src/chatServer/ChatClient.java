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
    private String Time = "day";
    private int numberWerewolf = numberPlayer/3;
    private int numberCivilian = numberPlayer * 2/3;
    private int tryKill = 0;
    private int currentlyPlaying;
    private boolean once = true;

    public Player getCurrentPlayer(){
        return currentPlayer;
    }

    public void setPlayers(Player[] _players){
        players = _players;
        currentPlayer = players[currentPlayer.getId()];
    }

    public Player[] getPlayers(){
        return players;
    }

    public int getNumberPlayer(){
        return numberPlayer;
    }

    public int getNumberWerewolf(){
        return numberWerewolf;
    }

    public int getNumberCivilian() { return numberCivilian; }

    public int getCurrentlyPlaying() { return currentlyPlaying; }

    public String getTime() { return Time; }

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
                JSONObject jsonObject = new JSONObject();
                switch (read) {
                    case "server":
                        jsonObject.put("method", "get_server");
                        jsonObject.put("server", "127.0.0.1");
                        jsonObject.put("port", "9876");
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
                        break;
                    case "leave":
                        jsonObject.put("method", "leave");
                        break;
                    case "ready":
                        if (currentPlayer.getId() != Player.ID_NOT_SET) {
                            jsonObject.put("method", "ready");
                        } else {
                            System.out.println("You have not joined the game");
                        }
                        break;
                    case "client_address":
                        jsonObject.put("method", "client_address");
                        break;
                    case "vote_civilian":
                        if (Time.equals("day") && (!currentPlayer.getRolePlayer().equals("dead"))){
                            scanner = new Scanner(System.in);
                            System.out.print("Pilih ID pemain yang akan dibunuh: ");
                            int index = scanner.nextInt();
                            voteCivilian(index);
                        } else {
                            System.out.println("Hari sedang malam, gunakan vote_werewolf");
                        }
                        break;
                    case "vote_werewolf":
                        if (Time.equals("night") && currentPlayer.getRolePlayer().equals("werewolf")) {
                            scanner = new Scanner(System.in);
                            System.out.print("Pilih ID pemain non werewolf yang akan dibunuh: ");
                            int index = scanner.nextInt();
                            voteWerewolf(index);
                        } else {
                            System.out.println("Hari sedang siang, gunakan vote_civilian atau anda bukan werewolf");
                        }
                        break;
                    case "change_phase":
                        System.out.println("waktu saat ini : " + Time);
                        break;
                    case "status":
                        System.out.println("Status Paxos saat ini : " + currentPlayer.getStatusPaxos());
                        System.out.println("Status Role saat ini : " + currentPlayer.getRolePlayer());
                        System.out.println("Status Pemimpin saat ini : " + currentLeader);
                        System.out.println("Currently playing is " + currentlyPlaying);
                        System.out.println("Number of werewolf " + numberWerewolf);
                        System.out.println("Number of civilian " + numberCivilian);
                        System.out.println("Try kill " + tryKill);
                        break;
                    default:
                        jsonObject.put("status","error");
                        jsonObject.put("description","wrong request");
                        break;
                }
                System.out.println(jsonObject.toString());
                streamOut.writeUTF(jsonObject.toString());
                streamOut.flush();

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
                                int i;
                                for (i = 0; i < jsonArray.length(); i++) {
                                    JSONObject json = jsonArray.getJSONObject(i);
                                    players[i].setId(json.getInt("player_id"));
                                    players[i].setAlive(json.getInt("is_alive"));
                                    //if there is player dead check is it werewolf or civilian
                                    if(  players[i].getAlive() == 0) {
                                        currentlyPlaying--;
                                        //set the dead player into dead
                                        if ( currentPlayer.getId() == players[i].getId()  ) {
                                            currentPlayer.setRolePlayer("dead");
                                        }
                                        if (json.getString("role").equals("werewolf")){
                                            numberWerewolf--;
                                        } else {
                                            numberCivilian--;
                                        }
                                        currentlyPlaying = numberWerewolf + numberCivilian;
                                    }
                                    players[i].setAddrIp(json.getString("address"));
                                    players[i].setAddrPort(json.getInt("port"));
                                    players[i].setUsername(json.getString("username"));
                                }
                                numberPlayer = i;
                                if (once){
                                    currentlyPlaying = numberPlayer;
                                    numberWerewolf = numberPlayer/3;
                                    numberCivilian = numberPlayer * 2 /3;
                                    once = false;
                                }


                                if(currentPlayer.getId() >= numberPlayer-2) {
                                    currentPlayer.setStatusPaxos("proposer");
                                    prepareProposal();

                                    System.out.println("Selesai Prepare Proposal");
                                } else {
                                    currentPlayer.setStatusPaxos("acceptor");
                                }

                                break;
                            default:
                                break;
                        }
                    }
                    if(jsonObject.has("player_id")){
                        currentPlayer.setId(jsonObject.getInt("player_id"));
                    }
//                    System.out.println("Status: " + jsonObject.getString("status"));
                } else if (jsonObject.has("method")){
                    if(jsonObject.getString("method").equals("start")){
                        currentPlayer.setRolePlayer(jsonObject.getString("role"));
                        Time = jsonObject.getString("time");
                    } else if(jsonObject.getString("method").equals("kpu_selected")) {
                        JSONObject json = new JSONObject();
                        json.put("status", "ok");
                        streamOut.writeUTF(json.toString());
                        streamOut.flush();
                        System.out.println("I have seen the leader is " + jsonObject.getInt("kpu_id"));
                        if (currentLeader == Player.ID_NOT_SET){
                            System.out.println("selected leader " + jsonObject.getInt("kpu_id"));
                            currentLeader = jsonObject.getInt("kpu_id");
                        }else {
                            previousLeader = currentLeader;
                            currentLeader = jsonObject.getInt("kpu_id");
                        }
                        UDPReceiver.finishElection = true;
                    } else if (jsonObject.getString("method").equals("vote_now")){
                        JSONObject json = new JSONObject();
                        json.put("status", "ok");
                        streamOut.writeUTF(json.toString());
                        streamOut.flush();
                        Time = jsonObject.getString("phase");
                        VoteNow(currentLeader);
                    } else if ( jsonObject.getString("method").equals("change_phase")) {
                        Time = jsonObject.getString("time");
                        System.out.println("waktu saat ini : " + Time);
                    }
                }
                System.out.println("Current player: " + currentPlayer);

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
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
        if (UDPReceiver.finishElection){
            System.out.println("Election finished");
            this.currentPlayer.getStatusPaxos().equals("acceptor");

        } else {
            System.out.println("Election not finished");
        }

        if(this.currentPlayer.getStatusPaxos().equals("proposer") && !UDPReceiver.finishElection ){
            System.out.println("I am proposer");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method","prepare_proposal");
            jsonObject.put("proposal_id", "["+ proposalID + ","+ currentPlayer.getId() + "]");
            if (previousLeader != Player.ID_NOT_SET){
                jsonObject.put("previous_accepted", previousLeader);
            }

            for (int i = 0; i < numberPlayer; i++ ){
                System.out.println("loop :" + players[i].getAddrIp() + " : "+ players[i].getAddrPort());
                transmitterUDP = new UDPTransmitter(this, players[i].getAddrIp(), players[i].getAddrPort(), socket.getLocalPort());
                transmitterUDP.send(jsonObject.toString());
            }

            //Wait for timeout
            Thread wt = new Thread(new WaitingThread(clientUDP, this));
            wt.start();


        } else if (this.currentPlayer.getStatusPaxos().equals("acceptor")) {
            System.out.println("I am acceptor");
        } else if (this.currentPlayer.getStatusPaxos().equals("leader")) {
            System.out.println("I am KPU leaders");
        }
    }

    /*-------------------------- Method Accept Proposal Paxos---------------------------*/
    public void acceptProposal(int candidateLeader) throws InterruptedException, JSONException {
        if (currentPlayer.getStatusPaxos().equals("proposer")) {
            System.out.println("I am accept proposer");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "accept_proposal");
            int numberPlayer = getNumberPlayer();
            int proposalID = getProposalID();
            jsonObject.put("proposal_id", "[" + proposalID + "," + currentPlayer.getId() + "]");
            jsonObject.put("kpu_id", candidateLeader);
            for (int i = 0; i < numberPlayer; i++) {
                System.out.println("loop :" + players[i].getAddrIp() + " : " + players[i].getAddrPort());
                UDPTransmitter transmitterUDP = new UDPTransmitter(this, players[i].getAddrIp(), players[i].getAddrPort(), socket.getLocalPort());
                transmitterUDP.send(jsonObject.toString());
            }
        } else if (this.currentPlayer.getStatusPaxos().equals("acceptor")) {
            System.out.println("I am acceptor");
        }
    }

    /*-------------------------- Method Vote Werewolf Paxos---------------------------*/
    public void voteWerewolf(int index){
        System.out.println("My leader is " + currentLeader);
        if(this.currentPlayer.getRolePlayer().equals("werewolf")){
            System.out.println("I am werewolf");
            System.out.println("ID telah dimasukkan");

            transmitterUDP = new UDPTransmitter(this, players[currentLeader].getAddrIp(), players[currentLeader].getAddrPort(), socket.getLocalPort());
            JSONObject jsonObject = new JSONObject();

            try {
                jsonObject.put("method", "vote_werewolf");
                jsonObject.put("player_id", index);
                transmitterUDP.send(jsonObject.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } else if (this.currentPlayer.getRolePlayer().equals("civilian")) {
            System.out.println("I am civilian waiting for the night");
        } else if (this.currentPlayer.getRolePlayer().equals("dead")) {
            System.out.println("I am dead waiting for the night");
        }
    }

    public void voteResultWerewolf(int[] listVote){
        System.out.println("My leader is " + currentLeader);
        JSONObject jsonObject = new JSONObject();
        if(this.currentPlayer.getStatusPaxos().equals("leader")){
            int i;
            for (i = 0; i < numberPlayer; i++){
                if (listVote[i] > numberWerewolf / 2){
                    try {
                        jsonObject.put("method", "vote_result_werewolf");
                        jsonObject.put("vote_status", "1");
                        jsonObject.put("player_killed", i);
                        JSONArray jsonArray = new JSONArray();
                        for (int j = 0; j < numberPlayer; j++){
                            JSONObject tempJsonObject = new JSONObject();
                            try {
                                tempJsonObject.put(String.valueOf(j), listVote[j]);
                                jsonArray.put(tempJsonObject);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        jsonObject.put("vote_result", jsonArray);
                        //send result to the server

                        streamOut.writeUTF(jsonObject.toString());
                        streamOut.flush();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }

            }

            //check if there is no break in the loop means no majority
            if (i == numberPlayer){
                System.out.println("No Majority Werewolf needs to redo choosing");

                try {
                    jsonObject.put("method", "vote_result");
                    jsonObject.put("vote_status", "-1");
                    JSONArray jsonArray = new JSONArray();
                    for (int j = 0; j < numberPlayer; j++){
                        JSONObject tempJsonObject = new JSONObject();
                        try {
                            tempJsonObject.put(String.valueOf(j), listVote[j]);
                            jsonArray.put(tempJsonObject);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        listVote[j] = 0;
                    }
                    jsonObject.put("vote_result", jsonArray);

                    //send result to the server

                    streamOut.writeUTF(jsonObject.toString());
                    streamOut.flush();
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

    }

    /*-------------------------- Method Vote Civillian Paxos---------------------------*/
    public void voteCivilian(int index){
        System.out.println("My leader is " + currentLeader);
        if(this.currentPlayer.getRolePlayer().equals("werewolf")){
            System.out.println("I am werewolf");

            transmitterUDP = new UDPTransmitter(this, players[currentLeader].getAddrIp(), players[currentLeader].getAddrPort(), socket.getLocalPort());
            JSONObject jsonObject = new JSONObject();

            try {
                jsonObject.put("method", "vote_civilian");
                jsonObject.put("player_id", index);
                transmitterUDP.send(jsonObject.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else if (this.currentPlayer.getRolePlayer().equals("civilian")) {

            transmitterUDP = new UDPTransmitter(this, players[currentLeader].getAddrIp(), players[currentLeader].getAddrPort(), socket.getLocalPort());
            JSONObject jsonObject = new JSONObject();

            try {
                jsonObject.put("method", "vote_civilian");
                jsonObject.put("player_id", index);
                transmitterUDP.send(jsonObject.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else if (this.currentPlayer.getStatusPaxos().equals("dead")) {
            System.out.println("I am dead waiting for the day");
        }

    }

    public void voteResultCivilian(int[] listVote){
        System.out.println("My leader is " + currentLeader);
        JSONObject jsonObject = new JSONObject();
        if (tryKill <= 2) {
            if (this.currentPlayer.getStatusPaxos().equals("leader")) {
                int i;
                for (i = 0; i < numberPlayer; i++) {
                    if (listVote[i] > currentlyPlaying / 2) {
                        try {
                            jsonObject.put("method", "vote_result_civilian");
                            jsonObject.put("vote_status", "1");
                            jsonObject.put("player_killed", i);
                            JSONArray jsonArray = new JSONArray();
                            for (int j = 0; j < numberPlayer; j++) {
                                JSONObject tempJsonObject = new JSONObject();
                                try {
                                    tempJsonObject.put(String.valueOf(j), listVote[j]);
                                    jsonArray.put(tempJsonObject);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            jsonObject.put("vote_result", jsonArray);
                            //send result to the server

                            streamOut.writeUTF(jsonObject.toString());
                            streamOut.flush();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    }

                }

                //check if there is no break in the loop means no majority
                if (i == numberPlayer) {
                    System.out.println("No Majority player needs to redo choosing");

                    try {
                        jsonObject.put("method", "vote_result");
                        jsonObject.put("vote_status", "-1");
                        JSONArray jsonArray = new JSONArray();
                        for (int j = 0; j < numberPlayer; j++) {
                            JSONObject tempJsonObject = new JSONObject();
                            try {
                                tempJsonObject.put(String.valueOf(j), listVote[j]);
                                jsonArray.put(tempJsonObject);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            //listVote[j] = 0;
                        }
                        jsonObject.put("vote_result", jsonArray);

                        //send result to the server

                        streamOut.writeUTF(jsonObject.toString());
                        streamOut.flush();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    tryKill++;

                }
            }
        } else {
            tryKill = 0;
        }
    }

    public void VoteNow(int Leader){
        if (currentPlayer.getId() == currentLeader){
            currentPlayer.setStatusPaxos("leader");
        }
        if(Time.equals("day")){
            System.out.println("It is day invoke method vote_civilian");
        } else if (Time.equals("night")){
            System.out.println("It is night invoke method vote_werewolf");
            UDPReceiver.finishElection = false;
        }
    }

    public static void main(String args[]) {
        ChatClient client = null;
        if (args.length != 2)
            System.out.println("Usage: java ChatClient host port");
        else
            client = new ChatClient(args[0], Integer.parseInt(args[1]));
    }

    /*-------------------------- Method KPU Selected---------------------------*/
    void KPUSelected(int playerId){
        JSONObject jsonObject = new JSONObject();

        if (this.currentPlayer.getStatusPaxos().equals("leader")) {
            System.out.println("I am KPU leader");
            currentLeader = currentPlayer.getId();
            if(Time.equals("day")){
                System.out.println("It is day invoke method vote_civilian");
            } else if (Time.equals("night")){
                System.out.println("It is day invoke method vote_werewolf");
            }
        } else {
            try {
                jsonObject.put("method", "accepted_proposal");
                jsonObject.put("kpu_id", playerId);
                jsonObject.put("description", "Kpu is selected");
                System.out.println(jsonObject.toString());
                streamOut.writeUTF(jsonObject.toString());
                streamOut.flush();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}