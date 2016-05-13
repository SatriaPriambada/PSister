package chatServer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Vector;

public class UDPReceiver extends Thread
{
	/**
	 * Contoh kode program untuk node yang menerima paket. Idealnya dalam paxos
	 * balasan juga dikirim melalui Unreliable Sender.
	 */


	private ChatClient       client    = null;
	private Socket socket    = null;
	private int              ID        = -1;
	private DataInputStream streamIn  =  null;
	private DataOutputStream streamOut = null;
    public static int counter = 0;
    private int[] listVote = new int[ChatServer.PLAYER_SIZE];

    private Player currentPlayer;
    private Player[] players = new Player [ChatServer.PLAYER_SIZE];
    private String IPReturn;
    private int portReturn;
    private UDPTransmitter udpTransmitter;
    private boolean accept = false;
    private boolean acceptLeader = false;
    private boolean finishElection = false;
    private int currentLeader = Player.ID_NOT_SET;
    private int previousLeader = Player.ID_NOT_SET;
    private String Time = "day";
    private int countVote = 0;
    private Vector<Integer> voteResult = new Vector<>();

    public int getCurrentLeader(){
        return currentLeader;
    }

    public int getPreviousLeader(){
        return previousLeader;
    }

	public UDPReceiver(ChatClient _client, Socket _socket) {
		super();
		client = _client;
		socket = _socket;
		ID     = socket.getLocalPort();
        String localIP = socket.getLocalAddress().getHostAddress();
        currentPlayer = new Player(localIP, ID);
		start();
        for (int q = 0; q < client.getNumberPlayer(); q++) {
            voteResult.set(q,0);
            listVote[q] = 0;
        }

	}

	public void run()
	{
		int listenPort = ID;
		try {
			DatagramSocket ListenSocket = new DatagramSocket(listenPort);
			byte[] receiveData = new byte[1024];
            while(true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                ListenSocket.receive(receivePacket);
                currentPlayer = client.getCurrentPlayer();
                players = client.getPlayers();
                Time = client.getTime();

                String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println("RECEIVED from client: " + receivePacket.getSocketAddress() + sentence);
                IPReturn = receivePacket.getAddress().toString().substring(1);
                portReturn = receivePacket.getPort() - 1000;
                //System.out.println("IF FOUND RETURN TO " + IPReturn + ":" + portReturn);
                JSONObject jsonObject = new JSONObject(sentence);
                if (jsonObject.has("method")) {
                    //handle method this is 1st time request
                    String method = jsonObject.getString("method");
                    if (method.equals("prepare_proposal")) {
                        prepareProposalResponse();
                    } else if (method.equals("accept_proposal")) {
                        acceptProposalResponse(jsonObject.getInt("kpu_id"));
                    } else if (method.equals("vote_werewolf")) {
                        voteWerewolfResponse(jsonObject.getInt("player_id"));
                    } else if (method.equals("vote_civilian")) {
                        voteCivilianResponse(jsonObject.getInt("player_id"));
                    } else {
                        System.out.println(method + "does not exist");
                    }
                } else if (jsonObject.has("status")){
                    //handle object with status this is response from other client
                    if(jsonObject.getString("status").equalsIgnoreCase("ok")){
                        if(jsonObject.has("description")){
                            if(jsonObject.getString("description").equalsIgnoreCase("accepted")){
                                counter++;
                                //check for majority
                                if (counter > client.getCurrentlyPlaying() / 2){
                                    int leaderCandidate = currentPlayer.getId();
                                    //when there is no accepted leader start the accept protocol
                                    if(!acceptLeader) {
                                        System.out.println("candidate : " + leaderCandidate);
                                        accept = false;
                                        counter = 0;
                                        client.acceptProposal(leaderCandidate);
                                        acceptLeader = true;
                                    } else {
                                        System.out.println("LEADER IS SELECTED : " + leaderCandidate);
                                        currentPlayer.setStatusPaxos("leader");
                                        //first selection
                                        if (currentLeader == Player.ID_NOT_SET) {
                                            currentLeader = leaderCandidate;
                                        } else {
                                            previousLeader = currentLeader;
                                            currentLeader = leaderCandidate;
                                        }
                                        finishElection = true;

                                        for(int i = 0; i < client.getNumberCivilian(); i++) {
                                            players[i].setFinish(finishElection);
                                            System.out.println("Set client " + i + " to " + finishElection);
                                            System.out.println(players[i]);
                                        }

                                        System.out.println("ALL FINISHED");
                                        client.KPUSelected(currentLeader);
                                    }
                                } else {
                                    accept = false;
                                    client.prepareProposal();
                                }
                            } else {
                                System.out.println(jsonObject);
                            }
                        } else {
                            System.out.println(jsonObject);
                        }
                    } else {
                        System.out.println(jsonObject);
                    }

                }
            }

		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }



    /*-------------------------- Method Prepare Proposal Paxos---------------------------*/
    void prepareProposalResponse() throws JSONException, InterruptedException {
        JSONObject jsonObject = new JSONObject();

        //each node can only reply once
        if(!accept) {
            jsonObject.put("status", "ok");
            jsonObject.put("description", "accepted");
            if (currentPlayer.getStatusPaxos().equals("proposer")) {
                udpTransmitter = new UDPTransmitter(client, IPReturn, portReturn, ID);
                udpTransmitter.reply(jsonObject.toString());
            } else if (currentPlayer.getStatusPaxos().equals("acceptor")) {
                System.out.println("I am acceptor");
                udpTransmitter = new UDPTransmitter(client, IPReturn, portReturn, ID);
                udpTransmitter.reply(jsonObject.toString());
            }
            accept = true;
        }  else {
            System.out.println("Already accepted other proposal");
            jsonObject.put("status", "failed");
            jsonObject.put("description","rejected");

            udpTransmitter = new UDPTransmitter(client, IPReturn, portReturn, ID);
            udpTransmitter.reply(jsonObject.toString());
        }
    }

    /*-------------------------- Method Accept Proposal Paxos---------------------------*/
    void acceptProposalResponse(int candidateLeader) throws JSONException, InterruptedException {
        JSONObject jsonObject = new JSONObject();
        accept = false;
        if(!accept) {
            jsonObject.put("status", "ok");
            jsonObject.put("description", "accepted");
            if (currentPlayer.getStatusPaxos().equals("proposer")) {
//                System.out.println("I am accept proposer");

                udpTransmitter = new UDPTransmitter(client, IPReturn, portReturn, ID);
                udpTransmitter.reply(jsonObject.toString());

            } else if (currentPlayer.getStatusPaxos().equals("acceptor")) {
//                System.out.println("I am accept acceptor");

                udpTransmitter = new UDPTransmitter(client, IPReturn, portReturn, ID);
                udpTransmitter.reply(jsonObject.toString());
            }
        }else {
            System.out.println("Already accepted other accept proposal");
            jsonObject.put("status", "failed");
            jsonObject.put("description","rejected");

            udpTransmitter = new UDPTransmitter(client, IPReturn, portReturn, ID);
            udpTransmitter.reply(jsonObject.toString());
        }


        //wait for signal assumtion status ok will always be sent to leader
        //System.out.println("My ID "+currentPlayer.getId() + "candidate " + candidateLeader);
        if(currentPlayer.getId() != candidateLeader) {
            System.out.println("ALL FINISHED");
            client.KPUSelected(candidateLeader);
        }

    }

    /*-------------------------- Method Vote Werewolf Paxos---------------------------*/
    void voteWerewolfResponse(int killPlayer){
        if (currentPlayer.getStatusPaxos().equals("leader")) {
            boolean once = true;
            System.out.println("I am KPU leader werewolf response");
            JSONObject jsonObject = new JSONObject();
            Time = "night";

            try {
                //each node can only vote once
                if(once) {
                    jsonObject.put("status", "ok");
                    jsonObject.put("description", "");
                    once = false;
                    int before = listVote[killPlayer];
                    listVote[killPlayer] = before + 1;
                    for (int i = 0; i < client.getNumberPlayer(); i++) {
                        System.out.print(listVote[i] + " ");
                    }
                    countVote++;
                } else {
                    jsonObject.put("status", "fail");
                    jsonObject.put("description", "");
                }

                udpTransmitter = new UDPTransmitter(client, IPReturn, portReturn, ID);
                udpTransmitter.reply(jsonObject.toString());

                System.out.println("vote count = " + countVote);
                for (int i = 0; i < client.getNumberPlayer(); i++) {
                    System.out.print(listVote[i] + " ");
                }

                if (countVote == (client.getNumberWerewolf() ) )  {
                    if (Time.equals("day")) {

                        client.voteResultCivilian(listVote);
                        for (int i = 0; i < client.getNumberPlayer(); i++) {
                            System.out.print(listVote[i] + " ");
                            listVote[i] = 0;
                        }
                        countVote = 0;
                    } else if (Time.equals("night")) {

                        client.voteResultWerewolf(listVote);
                        for (int i = 0; i < client.getNumberPlayer(); i++) {
                            System.out.print(listVote[i] + " ");
                            listVote[i] = 0;
                        }
                        countVote = 0;
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    /*-------------------------- Method Vote Civillian Paxos---------------------------*/
    void voteCivilianResponse(int killPlayer){
        //System.out.println("Leader is " );
        if (currentPlayer.getStatusPaxos().equals("leader")) {
            boolean once = true;
            System.out.println("I am KPU leader civilian response");
            JSONObject jsonObject = new JSONObject();
            Time = "day";

            try {
                //each node can only vote once
                if(once) {
                    jsonObject.put("status", "ok");
                    jsonObject.put("description", "");
                    once = false;
                    int before = listVote[killPlayer];
                    listVote[killPlayer] = before + 1;
                    for (int i = 0; i < client.getNumberPlayer(); i++) {
                        System.out.print(listVote[i] + " ");
                    }
                    countVote++;
                } else {
                    jsonObject.put("status", "fail");
                    jsonObject.put("description", "");
                }

                udpTransmitter = new UDPTransmitter(client, IPReturn, portReturn, ID);
                udpTransmitter.reply(jsonObject.toString());
                System.out.println("vote count = " + countVote);

                if (countVote == (client.getCurrentlyPlaying()) ) {
                    if (Time.equals("day")) {

                        client.voteResultCivilian(listVote);
                        for (int i = 0; i < client.getNumberPlayer(); i++) {
                            System.out.print(listVote[i] + " ");
                            listVote[i] = 0;
                        }
                        countVote = 0;
                    } else if (Time.equals("night")) {

                        client.voteResultWerewolf(listVote);
                        for (int i = 0; i < client.getNumberPlayer(); i++) {
                            System.out.print(listVote[i] + " ");
                            listVote[i] = 0;
                        }
                        countVote = 0;
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
